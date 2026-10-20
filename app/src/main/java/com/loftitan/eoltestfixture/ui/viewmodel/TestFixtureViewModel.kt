package com.loftitan.eoltestfixture.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loftitan.eoltestfixture.data.local.TestRecordEntity
import com.loftitan.eoltestfixture.data.repository.TestFixtureRepository
import com.loftitan.eoltestfixture.domain.model.CommPortStatus
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.IndicatorStatus
import com.loftitan.eoltestfixture.domain.model.MotorStatus
import com.loftitan.eoltestfixture.domain.model.PcbConnectionState
import com.loftitan.eoltestfixture.domain.model.PowerDiagnostic
import com.loftitan.eoltestfixture.domain.model.SensorPortStatus
import com.loftitan.eoltestfixture.domain.model.TelemetryPoint
import com.loftitan.eoltestfixture.domain.model.TestStatus
import com.loftitan.eoltestfixture.domain.orchestrator.TestSuiteRunner
import com.loftitan.eoltestfixture.hardware.HeartbeatStateMachine
import com.loftitan.eoltestfixture.hardware.UsbSerialManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class TestFixtureViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TestFixtureRepository(application)

    // Direct Hardwired USB OTG Hardware Manager (Physical ESP32 Rig Only)
    val usbSerialManager = UsbSerialManager(application, viewModelScope)

    private val heartbeatStateMachine: HeartbeatStateMachine = HeartbeatStateMachine(
        scope = viewModelScope,
        getActiveSerialManager = { usbSerialManager }
    )

    private val testSuiteRunner: TestSuiteRunner = TestSuiteRunner(
        scope = viewModelScope,
        repository = repository,
        getActiveSerialManager = { usbSerialManager },
        heartbeatStateMachine = heartbeatStateMachine
    )

    // Automated Test PCB (DUT) Provisioner for factory flashing & auto-upload
    val dutAutoProvisioner = com.loftitan.eoltestfixture.hardware.DutAutoProvisioner(
        application,
        application.getSystemService(android.content.Context.USB_SERVICE) as android.hardware.usb.UsbManager
    )
    val provisioningStatus: StateFlow<com.loftitan.eoltestfixture.hardware.ProvisioningStatus> = dutAutoProvisioner.status

    // Operator Authentication (Defaulted to LINE-1 for smooth factory throughput)
    private val _operatorId = MutableStateFlow("LINE-1")
    val operatorId: StateFlow<String> = _operatorId.asStateFlow()

    private val _isOperatorLoggedIn = MutableStateFlow(true)
    val isOperatorLoggedIn: StateFlow<Boolean> = _isOperatorLoggedIn.asStateFlow()

    // Test Control States
    private val _activeSerialNumber = MutableStateFlow("")
    val activeSerialNumber: StateFlow<String> = _activeSerialNumber.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = heartbeatStateMachine.connectionState
    val pcbConnectionState: StateFlow<PcbConnectionState> = testSuiteRunner.pcbConnectionState

    val testStatus: StateFlow<TestStatus> = testSuiteRunner.testStatus
    val currentTelemetry: StateFlow<TelemetryPoint> = testSuiteRunner.currentTelemetry
    val waveformPoints: StateFlow<List<TelemetryPoint>> = testSuiteRunner.waveformPoints
    val powerDiagnostic: StateFlow<PowerDiagnostic> = testSuiteRunner.powerDiagnostic
    val motorStatuses: StateFlow<List<MotorStatus>> = testSuiteRunner.motorStatuses
    val commStatuses: StateFlow<List<CommPortStatus>> = testSuiteRunner.commStatuses
    val sensorStatuses: StateFlow<List<SensorPortStatus>> = testSuiteRunner.sensorStatuses
    val indicatorStatuses: StateFlow<List<IndicatorStatus>> = testSuiteRunner.indicatorStatuses
    val manualButtonStates: StateFlow<Map<Int, Int>> = testSuiteRunner.manualButtonStates
    val manualSensorStates: StateFlow<Map<Int, Int>> = testSuiteRunner.manualSensorStates
    val manualSensorVoltages: StateFlow<Map<Int, Float>> = testSuiteRunner.manualSensorVoltages
    val lastCompletedResult: StateFlow<com.loftitan.eoltestfixture.domain.model.CompleteTestResult?> = testSuiteRunner.lastCompletedResult

    // Persistence & Google Sheets Queue
    val allTestRecords: StateFlow<List<TestRecordEntity>> = repository.allRecordsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingCountFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _googleScriptUrl = MutableStateFlow(repository.getGoogleScriptUrl())
    val googleScriptUrl: StateFlow<String> = _googleScriptUrl.asStateFlow()

    private val _attachedUsbDevices = MutableStateFlow<List<com.loftitan.eoltestfixture.domain.model.UsbDeviceStatus>>(emptyList())
    val attachedUsbDevices: StateFlow<List<com.loftitan.eoltestfixture.domain.model.UsbDeviceStatus>> = _attachedUsbDevices.asStateFlow()

    val scanMessage: StateFlow<String?> = usbSerialManager.scanMessage
    val recentSerialLogs: StateFlow<List<String>> = usbSerialManager.recentLogs

    init {
        initializeHardware()
        viewModelScope.launch {
            testSuiteRunner.detectedSerialNumber.collect { detected ->
                if (detected.isNotBlank() && _activeSerialNumber.value.isBlank()) {
                    _activeSerialNumber.value = detected
                }
            }
        }
    }

    fun initializeHardware() {
        viewModelScope.launch {
            scanUsbDevices()
            val ok = usbSerialManager.connect()
            if (ok) {
                heartbeatStateMachine.start()
                testSuiteRunner.bindSerialFrames()
            }
            scanUsbDevices()
        }
    }

    fun scanUsbDevices() {
        _attachedUsbDevices.value = usbSerialManager.getScanReport()
    }

    fun scanAndConnectHardware() {
        initializeHardware()
    }

    fun disconnectHardware() {
        viewModelScope.launch {
            usbSerialManager.disconnect()
            scanUsbDevices()
        }
    }

    fun sendHardwarePing() {
        viewModelScope.launch {
            usbSerialManager.sendPing()
        }
    }

    fun requestUsbPermission(deviceName: String) {
        usbSerialManager.requestPermissionForDeviceByName(deviceName)
        scanUsbDevices()
    }

    fun loginOperator(id: String, pin: String): Boolean {
        if (id.trim().isNotEmpty() && pin.trim().isNotEmpty()) {
            _operatorId.value = id.trim().uppercase()
            _isOperatorLoggedIn.value = true
            return true
        }
        return false
    }

    fun logoutOperator() {
        _operatorId.value = ""
        _isOperatorLoggedIn.value = false
    }

    fun setSerialNumber(sn: String) {
        _activeSerialNumber.value = sn.trim().uppercase()
    }

    fun startTest() {
        if (_activeSerialNumber.value.isBlank()) {
            val detected = testSuiteRunner.detectedSerialNumber.value
            _activeSerialNumber.value = if (detected.isNotBlank()) detected else "LOF-" + System.currentTimeMillis().toString().takeLast(6)
        }
        val op = _operatorId.value.ifBlank { "LINE-1" }
        _operatorId.value = op
        _isOperatorLoggedIn.value = true

        viewModelScope.launch {
            testSuiteRunner.startAutomatedTest(_activeSerialNumber.value, op)
        }
    }

    fun abortTest() {
        viewModelScope.launch {
            testSuiteRunner.abortTest()
        }
    }

    fun saveGoogleScriptUrl(url: String) {
        _googleScriptUrl.value = url.trim()
        repository.saveGoogleScriptUrl(url.trim())
    }

    fun triggerSyncNow() {
        repository.triggerImmediateSync()
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun readButtons() {
        viewModelScope.launch {
            testSuiteRunner.queryButtons()
        }
    }

    fun readSensors() {
        viewModelScope.launch {
            testSuiteRunner.querySensors()
        }
    }

    fun pulseMotor(motorId: String, direction: String = "FWD", speedPct: Int = 100, durationMs: Int = 250) {
        viewModelScope.launch {
            testSuiteRunner.sendMotorCommand(motorId, "PULSE", direction, speedPct, durationMs)
        }
    }

    fun setMotorRun(motorId: String, direction: String = "FWD", speedPct: Int = 100) {
        viewModelScope.launch {
            testSuiteRunner.sendMotorCommand(motorId, "RUN", direction, speedPct)
        }
    }

    fun stopMotor(motorId: String) {
        viewModelScope.launch {
            testSuiteRunner.sendMotorCommand(motorId, "STOP")
        }
    }

    fun stopAllMotors() {
        viewModelScope.launch {
            testSuiteRunner.stopAllMotors()
        }
    }

    fun manualPinTest(gpio: Int) {
        viewModelScope.launch {
            when (gpio) {
                com.loftitan.eoltestfixture.domain.model.PinMapping.Indicators.LED1_GPIO -> pulseLed(1)
                com.loftitan.eoltestfixture.domain.model.PinMapping.Indicators.LED2_GPIO -> pulseLed(2)
                com.loftitan.eoltestfixture.domain.model.PinMapping.Indicators.BUZZER_GPIO -> beepBuzzer()
                com.loftitan.eoltestfixture.domain.model.PinMapping.Buttons.BUTTON_1_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Buttons.BUTTON_2_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Buttons.BUTTON_3_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Buttons.BUTTON_4_GPIO -> readButtons()
                com.loftitan.eoltestfixture.domain.model.PinMapping.Sensors.S1_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Sensors.S2_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Sensors.S3_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Sensors.S4_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Sensors.S5_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Sensors.ULTRASONIC_S1_GPIO,
                com.loftitan.eoltestfixture.domain.model.PinMapping.Sensors.ULTRASONIC_S2_GPIO -> readSensors()
                else -> {
                    val cmd = JSONObject().apply {
                        put("cmd", "PIN_TEST")
                        put("gpio", gpio)
                    }.toString()
                    usbSerialManager.sendCommand(cmd)
                }
            }
        }
    }

    fun pulseLed(ledIndex: Int) {
        viewModelScope.launch {
            usbSerialManager.sendCommand("LED${ledIndex}_ON")
            kotlinx.coroutines.delay(400)
            usbSerialManager.sendCommand("LED${ledIndex}_OFF")
        }
    }

    fun setLedState(ledIndex: Int, on: Boolean) {
        viewModelScope.launch {
            usbSerialManager.sendCommand(if (on) "LED${ledIndex}_ON" else "LED${ledIndex}_OFF")
        }
    }

    fun beepBuzzer() {
        viewModelScope.launch {
            usbSerialManager.sendCommand("BEEP")
        }
    }

    fun setBuzzerState(on: Boolean) {
        viewModelScope.launch {
            usbSerialManager.sendCommand(if (on) "BUZZER_ON" else "BUZZER_OFF")
        }
    }

    fun autoProvisionTestPcb() {
        viewModelScope.launch {
            val ok = dutAutoProvisioner.autoProvisionConnectedDut()
            if (ok) {
                // Poll Rig to verify DUT status
                kotlinx.coroutines.delay(12000)
                usbSerialManager.sendCommand("{\"cmd\":\"CHECK_DUT\"}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        heartbeatStateMachine.stop()
        viewModelScope.launch {
            usbSerialManager.disconnect()
        }
    }
}
