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

    // Operator Authentication
    private val _operatorId = MutableStateFlow("")
    val operatorId: StateFlow<String> = _operatorId.asStateFlow()

    private val _isOperatorLoggedIn = MutableStateFlow(false)
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
        if (!_isOperatorLoggedIn.value) return
        if (_activeSerialNumber.value.isBlank()) return

        viewModelScope.launch {
            testSuiteRunner.startAutomatedTest(_activeSerialNumber.value, _operatorId.value)
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

    fun manualPinTest(gpio: Int) {
        viewModelScope.launch {
            val cmd = JSONObject().apply {
                put("cmd", "PIN_TEST")
                put("gpio", gpio)
            }.toString()
            usbSerialManager.sendCommand(cmd)
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
