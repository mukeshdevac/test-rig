package com.loftitan.eoltestfixture.domain.orchestrator

import android.util.Log
import com.loftitan.eoltestfixture.data.local.TestRecordEntity
import com.loftitan.eoltestfixture.data.repository.TestFixtureRepository
import com.loftitan.eoltestfixture.domain.model.CommPortStatus
import com.loftitan.eoltestfixture.domain.model.CompleteTestResult
import com.loftitan.eoltestfixture.domain.model.IndicatorStatus
import com.loftitan.eoltestfixture.domain.model.MotorStatus
import com.loftitan.eoltestfixture.domain.model.PcbConnectionState
import com.loftitan.eoltestfixture.domain.model.PinMapping
import com.loftitan.eoltestfixture.domain.model.PowerDiagnostic
import com.loftitan.eoltestfixture.domain.model.SensorPortStatus
import com.loftitan.eoltestfixture.domain.model.TelemetryPoint
import com.loftitan.eoltestfixture.domain.model.TestStatus
import com.loftitan.eoltestfixture.hardware.HeartbeatStateMachine
import com.loftitan.eoltestfixture.hardware.ISerialPortManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

private const val TAG = "TestSuiteRunner"
private const val MAX_WAVEFORM_POINTS = 150

class TestSuiteRunner(
    private val scope: CoroutineScope,
    private val repository: TestFixtureRepository,
    private val getActiveSerialManager: () -> ISerialPortManager,
    private val heartbeatStateMachine: HeartbeatStateMachine
) {

    private val _testStatus = MutableStateFlow(TestStatus.IDLE)
    val testStatus: StateFlow<TestStatus> = _testStatus.asStateFlow()

    // Target Test PCB Sensed on Rig Fixture Bed
    private val _pcbConnectionState = MutableStateFlow(PcbConnectionState.NOT_DETECTED)
    val pcbConnectionState: StateFlow<PcbConnectionState> = _pcbConnectionState.asStateFlow()

    private val _currentTelemetry = MutableStateFlow(TelemetryPoint(0L, 0f, 0f))
    val currentTelemetry: StateFlow<TelemetryPoint> = _currentTelemetry.asStateFlow()

    private val _waveformPoints = MutableStateFlow<List<TelemetryPoint>>(emptyList())
    val waveformPoints: StateFlow<List<TelemetryPoint>> = _waveformPoints.asStateFlow()

    private val _powerDiagnostic = MutableStateFlow(PowerDiagnostic())
    val powerDiagnostic: StateFlow<PowerDiagnostic> = _powerDiagnostic.asStateFlow()

    private val _motorStatuses = MutableStateFlow(defaultMotorStatuses())
    val motorStatuses: StateFlow<List<MotorStatus>> = _motorStatuses.asStateFlow()

    private val _commStatuses = MutableStateFlow(defaultCommStatuses())
    val commStatuses: StateFlow<List<CommPortStatus>> = _commStatuses.asStateFlow()

    private val _sensorStatuses = MutableStateFlow(defaultSensorStatuses())
    val sensorStatuses: StateFlow<List<SensorPortStatus>> = _sensorStatuses.asStateFlow()

    private val _indicatorStatuses = MutableStateFlow(defaultIndicatorStatuses())
    val indicatorStatuses: StateFlow<List<IndicatorStatus>> = _indicatorStatuses.asStateFlow()

    private val _lastCompletedResult = MutableStateFlow<CompleteTestResult?>(null)
    val lastCompletedResult: StateFlow<CompleteTestResult?> = _lastCompletedResult.asStateFlow()

    private var activeSerialNumber: String = ""
    private var activeOperatorId: String = ""
    private var testStartTime: Long = 0L
    private var serialListenJob: Job? = null

    init {
        bindSerialFrames()
    }

    fun bindSerialFrames() {
        serialListenJob?.cancel()
        serialListenJob = scope.launch(Dispatchers.Default) {
            val serialManager = getActiveSerialManager()
            serialManager.incomingFrames.collect { frame ->
                parseIncomingFrame(frame)
            }
        }
    }

    private suspend fun parseIncomingFrame(frame: String) {
        try {
            val json = JSONObject(frame)

            // Heartbeat response
            if (json.has("resp") && json.optString("resp") == "PONG") {
                heartbeatStateMachine.onPongReceived()
                return
            }

            val type = json.optString("type")

            // Target Test PCB Docking & Seating Detection from Test Rig ESP32 (Hardwired UART / In-Circuit Sense)
            if (type == "DUT_STATUS" || type == "PCB_STATUS") {
                val isDocked = json.optBoolean("connected", false) ||
                        json.optBoolean("seated", false) ||
                        json.optBoolean("docked", false) ||
                        json.optBoolean("uart_synced", false) ||
                        json.optString("uart_status").equals("ONLINE", ignoreCase = true)
                _pcbConnectionState.value = if (isDocked) PcbConnectionState.DOCKED else PcbConnectionState.NOT_DETECTED

                // Automatically bind the unique Bluetooth / Device name (e.g. LOF_TITAN_5F14)
                val detectedName = json.optString("device_name").takeIf { it.isNotEmpty() && it != "NOT_DETECTED" && it != "DISCONNECTED" }
                    ?: json.optString("ble_name").takeIf { it.isNotEmpty() }
                if (detectedName != null && isDocked) {
                    activeSerialNumber = detectedName
                }

                val voltage = json.optDouble("voltage_v", 0.0).toFloat()
                val quiescentMa = json.optDouble("quiescent_ma", 0.0).toFloat()
                if (isDocked && (voltage > 0f || quiescentMa > 0f)) {
                    _powerDiagnostic.value = _powerDiagnostic.value.copy(
                        busVoltageV = voltage,
                        quiescentCurrentMa = quiescentMa,
                        status = TestStatus.IDLE
                    )
                } else if (!isDocked && _testStatus.value != TestStatus.RUNNING) {
                    _powerDiagnostic.value = PowerDiagnostic()
                }
                return
            }

            // Real-time current/voltage telemetry
            if (type == "TELEMETRY") {
                val currentMa = json.optDouble("current_ma", 0.0).toFloat()
                val voltageV = json.optDouble("voltage_v", 0.0).toFloat()
                val timestampMs = json.optLong("timestamp_ms", System.currentTimeMillis() - testStartTime)

                if (voltageV > 3.0f || currentMa > 0.5f) {
                    _pcbConnectionState.value = PcbConnectionState.DOCKED
                }


                val point = TelemetryPoint(timestampMs, currentMa, voltageV)
                _currentTelemetry.value = point

                // Keep sliding window for waveform oscilloscope
                val currentList = _waveformPoints.value.toMutableList()
                currentList.add(point)
                if (currentList.size > MAX_WAVEFORM_POINTS) {
                    currentList.removeAt(0)
                }
                _waveformPoints.value = currentList

                // Update peak current if test is active
                if (_testStatus.value == TestStatus.RUNNING) {
                    val currentPower = _powerDiagnostic.value
                    if (currentMa > currentPower.peakCurrentMa) {
                        _powerDiagnostic.value = currentPower.copy(
                            peakCurrentMa = currentMa,
                            busVoltageV = voltageV
                        )
                    }
                }
                return
            }

            // Complete Test Result
            if (type == "TEST_COMPLETE") {
                handleTestComplete(json)
                return
            }

            // Test Aborted
            if (type == "TEST_ABORTED") {
                _testStatus.value = TestStatus.FAIL
                return
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming serial frame: ${e.message} (Frame: $frame)")
        }
    }

    suspend fun startAutomatedTest(serialNumber: String, operatorId: String): Boolean {
        if (_testStatus.value == TestStatus.RUNNING) return false

        activeSerialNumber = serialNumber.trim()
        activeOperatorId = operatorId.trim()
        testStartTime = System.currentTimeMillis()

        // Reset UI diagnostic states
        _testStatus.value = TestStatus.RUNNING
        _waveformPoints.value = emptyList()
        _powerDiagnostic.value = PowerDiagnostic(status = TestStatus.RUNNING)
        _motorStatuses.value = defaultMotorStatuses().map { it.copy(status = TestStatus.RUNNING, details = "Testing...") }
        _commStatuses.value = defaultCommStatuses().map { it.copy(status = TestStatus.RUNNING, details = "Verifying...") }
        _sensorStatuses.value = defaultSensorStatuses().map { it.copy(status = TestStatus.RUNNING) }
        _indicatorStatuses.value = defaultIndicatorStatuses()

        val startCommand = JSONObject().apply {
            put("cmd", "START_TEST")
            put("serial_number", activeSerialNumber)
            put("operator_id", activeOperatorId)
        }.toString()

        val sent = getActiveSerialManager().sendCommand(startCommand)
        if (!sent) {
            _testStatus.value = TestStatus.FAIL
            return false
        }
        return true
    }

    suspend fun abortTest() {
        val abortCommand = JSONObject().apply {
            put("cmd", "ABORT_TEST")
        }.toString()
        getActiveSerialManager().sendCommand(abortCommand)
        _testStatus.value = TestStatus.FAIL
    }

    private suspend fun handleTestComplete(json: JSONObject) {
        val durationMs = json.optLong("duration_ms", System.currentTimeMillis() - testStartTime)
        val overallResultStr = json.optString("overall_result", "FAIL")
        val overallStatus = if (overallResultStr.equals("PASS", ignoreCase = true)) TestStatus.PASS else TestStatus.FAIL
        _testStatus.value = overallStatus

        val diagJson = json.optJSONObject("diagnostics")

        // Parse Power
        val powerObj = diagJson?.optJSONObject("power")
        val quiescentMa = powerObj?.optDouble("quiescent_ma", 0.0)?.toFloat() ?: 0f
        val peakMa = powerObj?.optDouble("peak_ma", 0.0)?.toFloat() ?: _powerDiagnostic.value.peakCurrentMa
        val powerStatus = if (powerObj?.optString("status") == "PASS") TestStatus.PASS else TestStatus.FAIL
        _powerDiagnostic.value = PowerDiagnostic(
            quiescentCurrentMa = quiescentMa,
            peakCurrentMa = peakMa,
            busVoltageV = _currentTelemetry.value.busVoltageV,
            status = powerStatus
        )

        // Parse Motors
        val motorsObj = diagJson?.optJSONObject("motors")
        _motorStatuses.value = defaultMotorStatuses().map { motor ->
            val mObj = motorsObj?.optJSONObject(motor.id.lowercase())
            val statusStr = mObj?.optString("status", "FAIL")
            val status = if (statusStr.equals("PASS", ignoreCase = true)) TestStatus.PASS else TestStatus.FAIL
            motor.copy(status = status, details = if (status == TestStatus.PASS) "Driver OK" else "Fault / Open")
        }

        // Parse Communication (Hardwired UART between Rig ESP32 and Test PCB, SPI, I2C)
        val uartObj = diagJson?.optJSONObject("uart")
            ?: diagJson?.optJSONObject("hardwired_uart")
            ?: diagJson?.optJSONObject("uart_loopback")
        val spiObj = diagJson?.optJSONObject("spi_loopback") ?: diagJson?.optJSONObject("spi")
        val i2cObj = diagJson?.optJSONObject("i2c_scan") ?: diagJson?.optJSONObject("i2c")

        val isUartPass = uartObj?.optString("status")?.equals("PASS", ignoreCase = true) == true
        val uartDetails = when {
            isUartPass -> uartObj?.optString("details", "115200 Baud Synced (Bi-directional)") ?: "115200 Baud Synced (Bi-directional)"
            uartObj?.has("details") == true -> uartObj.optString("details")
            else -> "UART Timeout / No Handshake Response"
        }

        _commStatuses.value = listOf(
            CommPortStatus(
                portName = "Hardwired UART",
                pins = PinMapping.UART.DESCRIPTION,
                status = if (isUartPass) TestStatus.PASS else TestStatus.FAIL,
                details = uartDetails
            ),
            CommPortStatus(
                portName = "SPI Loopback",
                pins = "SCK(35), MOSI(36), MISO(37), CS(38)",
                status = if (spiObj?.optString("status") == "PASS") TestStatus.PASS else TestStatus.FAIL,
                details = if (spiObj?.optString("status") == "PASS") "Full Duplex OK" else "MISO Disconnected"
            ),
            CommPortStatus(
                portName = "I2C Bus Scan",
                pins = "SCL (8), SDA (7)",
                status = if (i2cObj?.optString("status") == "PASS") TestStatus.PASS else TestStatus.FAIL,
                details = if (i2cObj?.optString("status") == "PASS") "Devices: 0x3C, 0x68" else "Bus NACK / Missing Pullups"
            )
        )

        // Parse Sensors
        val sensorsObj = diagJson?.optJSONObject("sensors")
        _sensorStatuses.value = defaultSensorStatuses().map { sensor ->
            val key = when (sensor.portName) {
                "Ultrasonic S1" -> "ultrasonic_s1"
                "Ultrasonic S2" -> "ultrasonic_s2"
                else -> sensor.portName.lowercase()
            }
            val sObj = sensorsObj?.optJSONObject(key)
            val status = if (sObj?.optString("status") == "PASS") TestStatus.PASS else TestStatus.FAIL
            val logic = sObj?.optInt("logic", 0) == 1
            sensor.copy(status = status, logicLevelHigh = logic)
        }

        // Parse Indicators
        val indObj = diagJson?.optJSONObject("indicators")
        _indicatorStatuses.value = defaultIndicatorStatuses().map { ind ->
            val obj = indObj?.optJSONObject(ind.name.lowercase())
            val verified = obj != null && obj.optString("status") != "FAIL"
            ind.copy(verified = verified)
        }

        val testResult = CompleteTestResult(
            testId = UUID.randomUUID().toString(),
            serialNumber = activeSerialNumber,
            operatorId = activeOperatorId,
            timestamp = System.currentTimeMillis(),
            overallStatus = overallStatus,
            durationMs = durationMs,
            power = _powerDiagnostic.value,
            motors = _motorStatuses.value,
            comms = _commStatuses.value,
            sensors = _sensorStatuses.value,
            indicators = _indicatorStatuses.value,
            waveformPoints = _waveformPoints.value
        )
        _lastCompletedResult.value = testResult

        // Persist to local offline queue
        val entity = TestRecordEntity(
            testId = testResult.testId,
            timestamp = testResult.timestamp,
            operatorId = testResult.operatorId,
            serialNumber = testResult.serialNumber,
            overallResult = if (overallStatus == TestStatus.PASS) "PASS" else "FAIL",
            durationMs = durationMs,
            quiescentCurrentMa = _powerDiagnostic.value.quiescentCurrentMa,
            peakCurrentMa = _powerDiagnostic.value.peakCurrentMa,
            busVoltageV = _powerDiagnostic.value.busVoltageV,
            diagnosticsJson = json.toString()
        )
        repository.saveTestRecord(entity)
        Log.i(TAG, "Test completed and queued for offline sync: ${testResult.serialNumber} -> ${testResult.overallStatus}")
    }

    private fun defaultMotorStatuses(): List<MotorStatus> {
        return PinMapping.Motors.getAllDrivers().map { (id, gpios) ->
            MotorStatus(id = id, gpioPins = gpios, status = TestStatus.IDLE, details = "Standby")
        }
    }

    private fun defaultCommStatuses(): List<CommPortStatus> {
        return listOf(
            CommPortStatus("Hardwired UART", PinMapping.UART.DESCRIPTION, TestStatus.IDLE, "Standby"),
            CommPortStatus("SPI Loopback", PinMapping.SPI.DESCRIPTION, TestStatus.IDLE, "Standby"),
            CommPortStatus("I2C Bus Scan", PinMapping.I2C.DESCRIPTION, TestStatus.IDLE, "Standby")
        )
    }

    private fun defaultSensorStatuses(): List<SensorPortStatus> {
        return PinMapping.Sensors.getAllSensors().map { (name, gpio) ->
            SensorPortStatus(portName = name, gpio = gpio, status = TestStatus.IDLE)
        }
    }

    private fun defaultIndicatorStatuses(): List<IndicatorStatus> {
        return listOf(
            IndicatorStatus("LED1", PinMapping.Indicators.LED1_GPIO, false),
            IndicatorStatus("LED2", PinMapping.Indicators.LED2_GPIO, false),
            IndicatorStatus("Buzzer", PinMapping.Indicators.BUZZER_GPIO, false)
        )
    }
}
