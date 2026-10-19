package com.loftitan.eoltestfixture.ui.viewmodel

import com.loftitan.eoltestfixture.domain.model.LiveButtonStatus
import com.loftitan.eoltestfixture.domain.model.LiveI2cScanStatus
import com.loftitan.eoltestfixture.domain.model.LiveSensorStatus
import com.loftitan.eoltestfixture.hardware.UsbSerialManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

class ManualDiagnosticsController(
    private val scope: CoroutineScope,
    private val serialManager: UsbSerialManager
) {
    private val _liveButtons = MutableStateFlow(defaultButtons())
    val liveButtons: StateFlow<List<LiveButtonStatus>> = _liveButtons.asStateFlow()

    private val _liveSensors = MutableStateFlow(defaultSensors())
    val liveSensors: StateFlow<List<LiveSensorStatus>> = _liveSensors.asStateFlow()

    private val _i2cScan = MutableStateFlow(LiveI2cScanStatus())
    val i2cScan: StateFlow<LiveI2cScanStatus> = _i2cScan.asStateFlow()

    private val _isLiveMonitoring = MutableStateFlow(false)
    val isLiveMonitoring: StateFlow<Boolean> = _isLiveMonitoring.asStateFlow()

    private var pollingJob: Job? = null

    init {
        scope.launch {
            serialManager.incomingFrames.collect { frame ->
                handleIncomingFrame(frame)
            }
        }
    }

    fun toggleLiveMonitoring() {
        val next = !_isLiveMonitoring.value
        _isLiveMonitoring.value = next
        if (next) startPolling() else stopPolling()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive && _isLiveMonitoring.value) {
                pollButtons()
                delay(120)
                pollSensors()
                delay(150)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun pollButtons() {
        scope.launch { serialManager.sendCommand("BUTTON") }
    }

    fun pollSensors() {
        scope.launch { serialManager.sendCommand("SENSOR") }
    }

    fun triggerI2cScan() {
        _i2cScan.value = _i2cScan.value.copy(isScanning = true)
        scope.launch {
            serialManager.sendCommand("I2C")
        }
    }

    fun handleIncomingFrame(frame: String) {
        try {
            val clean = frame.trim().let { raw ->
                val start = raw.indexOf('{')
                val end = raw.lastIndexOf('}')
                if (start in 0 until end) raw.substring(start, end + 1) else raw
            }
            val json = JSONObject(clean)
            val resp = json.optString("resp")

            if (resp == "BUTTONS_STATE" || json.has("buttons")) {
                val btnsObj = json.optJSONObject("buttons") ?: return
                val current = _liveButtons.value.toMutableList()
                for (i in current.indices) {
                    val b = current[i]
                    val btnData = btnsObj.optJSONObject(b.id)
                    if (btnData != null) {
                        val isPressed = btnData.optBoolean("pressed", false) || btnData.optInt("raw", 1) == 0
                        val raw = btnData.optInt("raw", if (isPressed) 0 else 1)
                        current[i] = b.copy(isPressed = isPressed, rawValue = raw)
                    }
                }
                _liveButtons.value = current
            } else if (resp == "SENSORS_STATE" || json.has("sensors")) {
                val sensorsObj = json.optJSONObject("sensors") ?: return
                val current = _liveSensors.value.toMutableList()
                for (i in current.indices) {
                    val s = current[i]
                    val sData = sensorsObj.optJSONObject(s.id)
                    if (sData != null) {
                        val raw = sData.optInt("raw", 0)
                        val v = sData.optDouble("voltage", (raw / 4095.0) * 3.3).toFloat()
                        val dig = sData.optInt("digital", if (raw > 2000) 1 else 0) == 1
                        current[i] = s.copy(rawAdc = raw, voltageV = v, isDigitalHigh = dig)
                    }
                }
                _liveSensors.value = current
            } else if (resp == "I2C_SCAN_OK" || json.has("i2c_devices")) {
                val devs = mutableListOf<String>()
                val arr = json.optJSONArray("devices") ?: json.optJSONArray("i2c_devices")
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        devs.add(arr.getString(i))
                    }
                }
                _i2cScan.value = LiveI2cScanStatus(
                    devices = devs,
                    isScanning = false,
                    lastScanTimestamp = System.currentTimeMillis()
                )
            }
        } catch (_: Exception) {}
    }

    companion object {
        fun defaultButtons() = listOf(
            LiveButtonStatus("btn_1", "BUTTON 1", 39),
            LiveButtonStatus("btn_2", "BUTTON 2", 40),
            LiveButtonStatus("btn_3", "BUTTON 3", 41),
            LiveButtonStatus("btn_4", "BUTTON 4", 42)
        )

        fun defaultSensors() = listOf(
            LiveSensorStatus("s1", "SENSOR S1 (ADC1_CH1)", 2),
            LiveSensorStatus("s2", "SENSOR S2 (ADC1_CH0)", 1),
            LiveSensorStatus("s3", "SENSOR S3 (ADC1_CH2)", 3),
            LiveSensorStatus("s4", "SENSOR S4 (ADC1_CH3)", 4),
            LiveSensorStatus("s5", "SENSOR S5 (ADC1_CH4)", 5),
            LiveSensorStatus("us1", "ULTRASONIC S1 (TRIG/ECHO)", 6),
            LiveSensorStatus("us2", "ULTRASONIC S2 (ECHO/TRIG)", 19)
        )
    }
}
