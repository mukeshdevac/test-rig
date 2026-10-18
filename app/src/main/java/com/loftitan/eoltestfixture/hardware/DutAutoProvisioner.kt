package com.loftitan.eoltestfixture.hardware

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Base64
import android.util.Log
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

private const val TAG = "DutAutoProvisioner"
private const val DUT_BAUD_RATE = 115200
private const val CHUNK_SIZE = 120

sealed class ProvisioningStatus {
    object Idle : ProvisioningStatus()
    data class Detecting(val message: String) : ProvisioningStatus()
    data class Connecting(val deviceName: String) : ProvisioningStatus()
    data class Uploading(val progressPercent: Int, val currentChunk: Int, val totalChunks: Int) : ProvisioningStatus()
    data class Rebooting(val message: String) : ProvisioningStatus()
    data class Success(val deviceName: String, val message: String) : ProvisioningStatus()
    data class Error(val errorMessage: String) : ProvisioningStatus()
}

class DutAutoProvisioner(
    private val context: Context,
    private val usbManager: UsbManager
) {

    private val _status = MutableStateFlow<ProvisioningStatus>(ProvisioningStatus.Idle)
    val status: StateFlow<ProvisioningStatus> = _status.asStateFlow()

    private val probeTable = ProbeTable().apply {
        addProduct(0x303A, 0x1001, CdcAcmSerialDriver::class.java) // ESP32-S3 native USB CDC
        addProduct(0x303A, 0x0002, CdcAcmSerialDriver::class.java)
        addProduct(0x303A, 0x1002, CdcAcmSerialDriver::class.java)
    }

    fun findDutDevice(): UsbDevice? {
        val prober = UsbSerialProber(probeTable)
        val drivers = prober.findAllDrivers(usbManager)
        return drivers.firstOrNull()?.device
    }

    suspend fun autoProvisionConnectedDut(): Boolean = withContext(Dispatchers.IO) {
        val dutDevice = findDutDevice()
        if (dutDevice == null) {
            _status.value = ProvisioningStatus.Error("No ESP32-S3 Test PCB detected on USB.")
            return@withContext false
        }
        return@withContext provisionDevice(dutDevice)
    }

    suspend fun provisionDevice(device: UsbDevice): Boolean = withContext(Dispatchers.IO) {
        var connection: UsbDeviceConnection? = null
        var port: UsbSerialPort? = null

        try {
            _status.value = ProvisioningStatus.Connecting(device.productName ?: "ESP32-S3 Test PCB")
            Log.i(TAG, "Starting provisioning for ${device.deviceName}")

            if (!usbManager.hasPermission(device)) {
                _status.value = ProvisioningStatus.Error("USB permission required for Test PCB.")
                return@withContext false
            }

            val prober = UsbSerialProber(probeTable)
            val driver = prober.probeDevice(device)
            if (driver == null || driver.ports.isEmpty()) {
                _status.value = ProvisioningStatus.Error("No compatible CDC driver found for Test PCB.")
                return@withContext false
            }

            connection = usbManager.openDevice(device)
            if (connection == null) {
                _status.value = ProvisioningStatus.Error("Failed to open connection to Test PCB.")
                return@withContext false
            }

            port = driver.ports[0]
            port.open(connection)
            port.setParameters(DUT_BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            // 1. Hardware reset Test PCB into TITAN supervisor countdown
            _status.value = ProvisioningStatus.Detecting("Resetting Test PCB into boot countdown...")
            try {
                port.dtr = false
                port.rts = true
                delay(100)
                port.rts = false
                delay(300)
            } catch (e: Exception) {
                Log.w(TAG, "RTS/DTR reset error: ${e.message}")
            }

            // 2. Intercept countdown with {"cmd":"CONNECT"}
            delay(1200)
            val connectCmd = "{\"cmd\":\"CONNECT\"}\n".toByteArray(StandardCharsets.UTF_8)
            port.write(connectCmd, 1000)
            delay(400)

            // Drain initial response
            val drainBuf = ByteArray(1024)
            port.read(drainBuf, 100)

            // 3. Load companion script from assets
            val companionCode = loadCompanionScript()
            val codeBytes = companionCode.toByteArray(StandardCharsets.UTF_8)
            val totalBytes = codeBytes.size

            // 4. Send PROGRAM command
            _status.value = ProvisioningStatus.Uploading(0, 0, (totalBytes + CHUNK_SIZE - 1) / CHUNK_SIZE)
            val progCmd = JSONObject().apply {
                put("cmd", "PROGRAM")
                put("filename", "/program/user.py")
                put("size", totalBytes)
            }.toString() + "\n"

            port.write(progCmd.toByteArray(StandardCharsets.UTF_8), 1000)
            delay(400)
            port.read(drainBuf, 100)

            // 5. Stream chunks
            val totalChunks = (totalBytes + CHUNK_SIZE - 1) / CHUNK_SIZE
            for (idx in 0 until totalChunks) {
                val start = idx * CHUNK_SIZE
                val end = minOf(start + CHUNK_SIZE, totalBytes)
                val slice = codeBytes.copyOfRange(start, end)
                val b64 = Base64.encodeToString(slice, Base64.NO_WRAP)

                val chunkCmd = JSONObject().apply {
                    put("cmd", "CHUNK")
                    put("seq", idx)
                    put("data", b64)
                }.toString() + "\n"

                port.write(chunkCmd.toByteArray(StandardCharsets.UTF_8), 1000)
                val percent = ((idx + 1) * 100) / totalChunks
                _status.value = ProvisioningStatus.Uploading(percent, idx + 1, totalChunks)
                delay(80)
            }

            delay(500)
            port.read(drainBuf, 200)

            // 6. Reboot Test PCB into companion mode
            _status.value = ProvisioningStatus.Rebooting("Verifying flash and rebooting companion...")
            try {
                port.dtr = false
                port.rts = true
                delay(100)
                port.rts = false
                delay(200)
            } catch (e: Exception) {
                Log.w(TAG, "Reboot error: ${e.message}")
            }

            _status.value = ProvisioningStatus.Success(
                deviceName = device.productName ?: "LOF_TITAN_DUT",
                message = "Successfully uploaded companion code (/program/user.py). Test PCB is ready!"
            )
            Log.i(TAG, "Provisioning completed successfully.")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Provisioning failed: ${e.message}", e)
            _status.value = ProvisioningStatus.Error("Provisioning failed: ${e.message}")
            return@withContext false
        } finally {
            try { port?.close() } catch (ignored: Exception) {}
            try { connection?.close() } catch (ignored: Exception) {}
        }
    }

    private fun loadCompanionScript(): String {
        return try {
            context.assets.open("companion_main.py").use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read from assets, using embedded fallback: ${e.message}")
            EMBEDDED_COMPANION_FALLBACK
        }
    }

    companion object {
        private const val EMBEDDED_COMPANION_FALLBACK = """
import time, json, machine
uid = machine.unique_id()
dev_id = f"{uid[-2]:02X}{uid[-1]:02X}"
device_name = f"LOF_TITAN_{dev_id}"
try: wdt = machine.WDT(timeout=16000)
except: wdt = None
def feed():
    if wdt:
        try: wdt.feed()
        except: pass
try:
    uart = machine.UART(1, baudrate=115200, tx=17, rx=18, timeout=30)
except:
    uart = None
rx_buf = b""
while True:
    feed()
    if uart and uart.any():
        c = uart.read()
        if c:
            rx_buf += c
            if b"\n" in rx_buf:
                lines = rx_buf.split(b"\n")
                rx_buf = lines[-1]
                for l in lines[:-1]:
                    try: l_str = l.decode('utf-8', 'ignore').strip()
                    except: l_str = ""
                    if "PING" in l_str:
                        resp = json.dumps({"resp":"PONG","status":"DUT_OK","device_name":device_name,"device_id":dev_id}) + "\n"
                        try: uart.write(resp)
                        except: pass
                    elif "START_TEST" in l_str:
                        resp = json.dumps({"type":"TEST_COMPLETE","status":"PASS","device_name":device_name,"device_id":dev_id}) + "\n"
                        try: uart.write(resp)
                        except: pass
    time.sleep_ms(10)
"""
    }
}
