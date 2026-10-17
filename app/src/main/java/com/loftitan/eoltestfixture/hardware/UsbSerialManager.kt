package com.loftitan.eoltestfixture.hardware

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver
import com.hoho.android.usbserial.driver.FtdiSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.UsbDeviceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private const val TAG = "UsbSerialManager"
private const val BAUD_RATE = 115200
private const val READ_WAIT_MILLIS = 200
private const val BUFFER_SIZE = 4096
const val ACTION_USB_PERMISSION = "com.loftitan.eoltestfixture.USB_PERMISSION"

class UsbSerialManager(
    private val context: Context,
    private val externalScope: CoroutineScope
) : ISerialPortManager {

    private val usbManager: UsbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingFrames = MutableSharedFlow<String>(extraBufferCapacity = 64)
    override val incomingFrames: SharedFlow<String> = _incomingFrames.asSharedFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<String>>(listOf("[SYSTEM] Serial Link Subsystem Initialized (115200 Baud)"))
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    private var usbConnection: UsbDeviceConnection? = null
    private var usbSerialPort: UsbSerialPort? = null
    private var readJob: Job? = null
    private val lineBuilder = StringBuilder()

    init {
        registerUsbReceiver()
    }

    fun logEvent(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val entry = "[$timestamp] $msg"
        val updated = (_recentLogs.value + entry).takeLast(60)
        _recentLogs.value = updated
    }

    private fun registerUsbReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_USB_PERMISSION -> {
                        val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        Log.i(TAG, "USB Permission callback result: granted=$granted")
                        logEvent("[USB] Permission callback: granted=$granted")
                        if (granted) {
                            _scanMessage.value = "Permission granted. Connecting..."
                            externalScope.launch { connect() }
                        } else {
                            _scanMessage.value = "USB permission denied by operator."
                            _connectionState.value = ConnectionState.DISCONNECTED
                        }
                    }
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        Log.i(TAG, "USB Device Attached broadcast received.")
                        logEvent("[USB] Device physically attached to port")
                        _scanMessage.value = "Hardware detected. Connecting..."
                        externalScope.launch { connect() }
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        Log.w(TAG, "USB Device Detached broadcast received.")
                        logEvent("[USB] Device physically detached from port")
                        _scanMessage.value = "Hardware detached."
                        notifyDisconnect()
                    }
                }
            }
        }
        try {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            Log.i(TAG, "USB BroadcastReceiver registered successfully with RECEIVER_EXPORTED.")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register USB BroadcastReceiver: ${e.message}")
        }
    }

    override fun isConnected(): Boolean = _connectionState.value == ConnectionState.CONNECTED

    fun getAvailableDrivers(): List<UsbSerialDriver> {
        val defaultDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (defaultDrivers.isNotEmpty()) return defaultDrivers

        // Custom fallback probe table for all ESP32 & USB bridge variants
        val customTable = ProbeTable().apply {
            addProduct(0x303A, 0x1001, CdcAcmSerialDriver::class.java) // ESP32-S2 / ESP32-S3 CDC
            addProduct(0x303A, 0x0002, CdcAcmSerialDriver::class.java)
            addProduct(0x1A86, 0x7523, Ch34xSerialDriver::class.java)  // CH340 / CH341
            addProduct(0x1A86, 0x5523, Ch34xSerialDriver::class.java)
            addProduct(0x10C4, 0xEA60, Cp21xxSerialDriver::class.java) // CP2102 / CP2104
            addProduct(0x10C4, 0xEA70, Cp21xxSerialDriver::class.java)
            addProduct(0x0403, 0x6001, FtdiSerialDriver::class.java)   // FTDI FT232R
            addProduct(0x0403, 0x6015, FtdiSerialDriver::class.java)
        }
        return UsbSerialProber(customTable).findAllDrivers(usbManager)
    }

    fun getScanReport(): List<UsbDeviceStatus> {
        val drivers = getAvailableDrivers()
        val driverDevices = drivers.map { it.device }
        return usbManager.deviceList.values.map { dev ->
            UsbDeviceStatus(
                deviceName = dev.deviceName,
                vendorId = dev.vendorId,
                productId = dev.productId,
                manufacturerName = dev.manufacturerName,
                productName = dev.productName,
                hasPermission = usbManager.hasPermission(dev),
                hasSupportedDriver = driverDevices.contains(dev)
            )
        }
    }

    fun requestPermissionForDevice(device: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(context.packageName)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val permissionIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        usbManager.requestPermission(device, permissionIntent)
    }

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            val availableDrivers = getAvailableDrivers()
            if (availableDrivers.isEmpty()) {
                val rawDevices = usbManager.deviceList.values
                if (rawDevices.isNotEmpty()) {
                    val dev = rawDevices.first()
                    _scanMessage.value = "Device found: ${dev.productName ?: dev.deviceName}. Requesting permission..."
                    if (!usbManager.hasPermission(dev)) {
                        requestPermissionForDevice(dev)
                    }
                } else {
                    _scanMessage.value = "No USB devices detected on tablet Type-C port."
                }
                Log.d(TAG, "No USB serial drivers detected.")
                _connectionState.value = ConnectionState.DISCONNECTED
                return@withContext false
            }

            val driver: UsbSerialDriver = availableDrivers[0]
            val device = driver.device

            if (!usbManager.hasPermission(device)) {
                Log.w(TAG, "Permission not granted for ${device.deviceName}. Requesting permission from user...")
                _scanMessage.value = "Prompting USB permission on tablet screen..."
                _connectionState.value = ConnectionState.RECONNECTING
                requestPermissionForDevice(device)
                return@withContext false
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                Log.w(TAG, "UsbManager.openDevice returned null.")
                _scanMessage.value = "Cannot open USB connection. Check cable."
                _connectionState.value = ConnectionState.RECONNECTING
                return@withContext false
            }

            val port = driver.ports[0]
            port.open(connection)
            port.setParameters(
                BAUD_RATE,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            try {
                port.dtr = true
            } catch (e: Exception) {
                Log.w(TAG, "DTR error: ${e.message}")
            }

            usbConnection = connection
            usbSerialPort = port
            _connectionState.value = ConnectionState.CONNECTED
            _scanMessage.value = "Test Rig Online (@ 115200 Baud)"
            logEvent("[CONNECT] Test Rig Connected (@ 115200 Baud)")
            Log.i(TAG, "USB Serial connected successfully: ${driver.device.deviceName}")

            startReadLoop(port, connection)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to USB Serial: ${e.message}", e)
            logEvent("[ERR] Connect error: ${e.message}")
            _scanMessage.value = "Connection error: ${e.message}"
            disconnect()
            false
        }
    }

    private fun startReadLoop(port: UsbSerialPort, connection: UsbDeviceConnection) {
        stopReadLoop()
        val endpoint = port.readEndpoint ?: run {
            Log.e(TAG, "No read endpoint available for USB serial port")
            return
        }

        readJob = externalScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(BUFFER_SIZE)
            while (isActive && isConnected()) {
                try {
                    // Critical: bulkTransfer with timeout=0 blocks indefinitely waiting for data or port close.
                    // 1) timeout=0 eliminates URB cancellations -> completely prevents MediaTek musb_urb_dequeue panic!
                    // 2) Bypasses UsbRequest.queue() -> completely eliminates "Queueing USB request failed"!
                    val bytesRead = connection.bulkTransfer(endpoint, buffer, buffer.size, 0)
                    if (bytesRead > 0) {
                        val chunk = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        for (ch in chunk) {
                            if (ch == '\n') {
                                val line = lineBuilder.toString().trim()
                                if (line.isNotEmpty()) {
                                    logEvent("[RX] $line")
                                    externalScope.launch { _incomingFrames.emit(line) }
                                }
                                lineBuilder.clear()
                            } else if (ch != '\r') {
                                lineBuilder.append(ch)
                            }
                        }
                    } else if (bytesRead < 0) {
                        Log.w(TAG, "bulkTransfer returned $bytesRead (device closed or detached)")
                        if (isActive && isConnected()) {
                            notifyDisconnect()
                        }
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "USB read loop exception: ${e.message}")
                    if (isActive && isConnected()) {
                        notifyDisconnect()
                    }
                    break
                }
            }
        }
    }

    private fun stopReadLoop() {
        try {
            readJob?.cancel()
        } catch (ignored: Exception) {}
        readJob = null
    }

    override suspend fun sendCommand(jsonCommand: String): Boolean = withContext(Dispatchers.IO) {
        val port = usbSerialPort ?: return@withContext false
        try {
            val trimmed = jsonCommand.trim()
            logEvent("[TX] $trimmed")
            val payload = (trimmed + "\n").toByteArray(Charsets.UTF_8)
            port.write(payload, 2000)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send command over USB serial: ${e.message}")
            logEvent("[ERR] Failed to send: ${e.message}")
            notifyDisconnect()
            false
        }
    }

    suspend fun sendPing(): Boolean {
        return sendCommand("{\"cmd\":\"PING\",\"timestamp\":${System.currentTimeMillis()}}")
    }

    fun requestPermissionForDeviceByName(deviceName: String) {
        val dev = usbManager.deviceList.values.find { it.deviceName == deviceName }
        if (dev != null) {
            logEvent("[USB] Requesting OS permission for ${dev.deviceName}")
            requestPermissionForDevice(dev)
        }
    }

    fun notifyDisconnect() {
        _connectionState.value = ConnectionState.RECONNECTING
        stopReadLoop()
        try {
            usbSerialPort?.close()
        } catch (ignored: Exception) {}
        try {
            usbConnection?.close()
        } catch (ignored: Exception) {}
        usbSerialPort = null
        usbConnection = null
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.DISCONNECTED
        _scanMessage.value = "Disconnected from hardware."
        stopReadLoop()
        try {
            usbSerialPort?.close()
        } catch (ignored: Exception) {}
        try {
            usbConnection?.close()
        } catch (ignored: Exception) {}
        usbSerialPort = null
        usbConnection = null
    }
}
