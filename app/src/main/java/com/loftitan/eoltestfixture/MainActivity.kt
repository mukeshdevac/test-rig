package com.loftitan.eoltestfixture

import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loftitan.eoltestfixture.ui.components.AppDestination
import com.loftitan.eoltestfixture.ui.components.BottomNavigationBarView
import com.loftitan.eoltestfixture.ui.components.HardwareConnectionDialog
import com.loftitan.eoltestfixture.ui.components.NavigationRailView
import com.loftitan.eoltestfixture.ui.components.OperatorAuthDialog
import com.loftitan.eoltestfixture.ui.components.TopStatusBar
import com.loftitan.eoltestfixture.ui.screens.DashboardScreen
import com.loftitan.eoltestfixture.ui.screens.HardwareScreen
import com.loftitan.eoltestfixture.ui.screens.ManualDiagnosticsScreen
import com.loftitan.eoltestfixture.ui.screens.SettingsScreen
import com.loftitan.eoltestfixture.ui.screens.TestHistoryQueueScreen
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.LOFTitanEOLTestFixtureTheme
import com.loftitan.eoltestfixture.ui.viewmodel.TestFixtureViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: TestFixtureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LOFTitanEOLTestFixtureTheme {
                MainContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent?.action) {
            viewModel.initializeHardware()
        }
    }
}

@Composable
fun MainContent(viewModel: TestFixtureViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestination.RUN_TEST) }
    var isRailExpanded by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showHardwareDialog by remember { mutableStateOf(false) }

    val operatorId by viewModel.operatorId.collectAsState()
    val isOperatorLoggedIn by viewModel.isOperatorLoggedIn.collectAsState()
    val activeSerialNumber by viewModel.activeSerialNumber.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val pcbConnectionState by viewModel.pcbConnectionState.collectAsState()
    val attachedUsbDevices by viewModel.attachedUsbDevices.collectAsState()
    val scanMessage by viewModel.scanMessage.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

    if (showAuthDialog) {
        OperatorAuthDialog(
            isCurrentlyLoggedIn = isOperatorLoggedIn,
            currentOperatorId = operatorId,
            onDismiss = { showAuthDialog = false },
            onLogin = { id, pin -> viewModel.loginOperator(id, pin) },
            onLogout = { viewModel.logoutOperator() }
        )
    }

    if (showHardwareDialog) {
        HardwareConnectionDialog(
            connectionState = connectionState,
            attachedDevices = attachedUsbDevices,
            scanMessage = scanMessage,
            onScanAndConnect = { viewModel.scanAndConnectHardware() },
            onDisconnect = { viewModel.disconnectHardware() },
            onDismiss = { showHardwareDialog = false }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
    ) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopStatusBar(
                    operatorId = operatorId,
                    isOperatorLoggedIn = isOperatorLoggedIn,
                    activeSerialNumber = activeSerialNumber,
                    connectionState = connectionState,
                    pcbConnectionState = pcbConnectionState,
                    onOperatorClick = { showAuthDialog = true },
                    onRigClick = { currentDestination = AppDestination.HARDWARE }
                )
                Box(modifier = Modifier.weight(1f)) {
                    ScreenSwitcher(currentDestination, viewModel, onOpenHardwareDialog = { currentDestination = AppDestination.HARDWARE })
                }
                BottomNavigationBarView(
                    currentDestination = currentDestination,
                    onDestinationSelected = { currentDestination = it },
                    pendingQueueCount = pendingSyncCount
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRailView(
                    currentDestination = currentDestination,
                    onDestinationSelected = { currentDestination = it },
                    isExpanded = isRailExpanded,
                    onToggleExpand = { isRailExpanded = !isRailExpanded },
                    pendingQueueCount = pendingSyncCount
                )
                Column(modifier = Modifier.fillMaxSize()) {
                    TopStatusBar(
                        operatorId = operatorId,
                        isOperatorLoggedIn = isOperatorLoggedIn,
                        activeSerialNumber = activeSerialNumber,
                        connectionState = connectionState,
                        pcbConnectionState = pcbConnectionState,
                        onOperatorClick = { showAuthDialog = true },
                        onRigClick = { currentDestination = AppDestination.HARDWARE }
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        ScreenSwitcher(currentDestination, viewModel, onOpenHardwareDialog = { currentDestination = AppDestination.HARDWARE })
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenSwitcher(
    destination: AppDestination,
    viewModel: TestFixtureViewModel,
    onOpenHardwareDialog: () -> Unit = {}
) {
    when (destination) {
        AppDestination.RUN_TEST -> {
            val serialNumber by viewModel.activeSerialNumber.collectAsState()
            val testStatus by viewModel.testStatus.collectAsState()
            val connectionState by viewModel.connectionState.collectAsState()
            val pcbConnectionState by viewModel.pcbConnectionState.collectAsState()
            val lastCompletedResult by viewModel.lastCompletedResult.collectAsState()
            val allRecords by viewModel.allTestRecords.collectAsState()
            val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
            val currentTelemetry by viewModel.currentTelemetry.collectAsState()
            val waveformPoints by viewModel.waveformPoints.collectAsState()
            val powerDiagnostic by viewModel.powerDiagnostic.collectAsState()
            val motorStatuses by viewModel.motorStatuses.collectAsState()
            val commStatuses by viewModel.commStatuses.collectAsState()
            val sensorStatuses by viewModel.sensorStatuses.collectAsState()
            val indicatorStatuses by viewModel.indicatorStatuses.collectAsState()

            DashboardScreen(
                serialNumber = serialNumber,
                onSerialNumberChange = { viewModel.setSerialNumber(it) },
                onStartTest = { viewModel.startTest() },
                onAbortTest = { viewModel.abortTest() },
                testStatus = testStatus,
                connectionState = connectionState,
                pcbConnectionState = pcbConnectionState,
                lastCompletedResult = lastCompletedResult,
                allRecords = allRecords,
                pendingSyncCount = pendingSyncCount,
                onSyncNow = { viewModel.triggerSyncNow() },
                telemetryPoint = currentTelemetry,
                waveformPoints = waveformPoints,
                powerDiagnostic = powerDiagnostic,
                motorStatuses = motorStatuses,
                commStatuses = commStatuses,
                sensorStatuses = sensorStatuses,
                indicatorStatuses = indicatorStatuses,
                onOpenHardwareDialog = onOpenHardwareDialog
            )
        }
        AppDestination.HARDWARE -> {
            val connectionState by viewModel.connectionState.collectAsState()
            val pcbConnectionState by viewModel.pcbConnectionState.collectAsState()
            val attachedDevices by viewModel.attachedUsbDevices.collectAsState()
            val scanMessage by viewModel.scanMessage.collectAsState()
            val recentLogs by viewModel.recentSerialLogs.collectAsState()
            val provisioningStatus by viewModel.provisioningStatus.collectAsState()

            HardwareScreen(
                connectionState = connectionState,
                pcbConnectionState = pcbConnectionState,
                attachedDevices = attachedDevices,
                scanMessage = scanMessage,
                recentLogs = recentLogs,
                provisioningStatus = provisioningStatus,
                onScanAndConnect = { viewModel.scanAndConnectHardware() },
                onDisconnect = { viewModel.disconnectHardware() },
                onSendPing = { viewModel.sendHardwarePing() },
                onRequestPermission = { devName -> viewModel.requestUsbPermission(devName) },
                onAutoProvision = { viewModel.autoProvisionTestPcb() }
            )
        }
        AppDestination.DIAGNOSTICS -> {
            val liveButtons by viewModel.liveButtons.collectAsState()
            val liveSensors by viewModel.liveSensors.collectAsState()
            val liveI2cScan by viewModel.liveI2cScan.collectAsState()
            val isLiveMonitoring by viewModel.isLiveMonitoring.collectAsState()

            ManualDiagnosticsScreen(
                liveButtons = liveButtons,
                liveSensors = liveSensors,
                liveI2cScan = liveI2cScan,
                isLiveMonitoring = isLiveMonitoring,
                onToggleLiveMonitoring = { viewModel.toggleLiveMonitoring() },
                onPollButtons = { viewModel.pollButtons() },
                onPollSensors = { viewModel.pollSensors() },
                onTriggerI2cScan = { viewModel.triggerI2cScan() },
                onTestPin = { gpio -> viewModel.manualPinTest(gpio) },
                onPulseLed = { idx -> viewModel.pulseLed(idx) },
                onToggleLed = { idx, st -> viewModel.setLedState(idx, st) },
                onBeepBuzzer = { viewModel.beepBuzzer() },
                onToggleBuzzer = { st -> viewModel.setBuzzerState(st) }
            )
        }
        AppDestination.HISTORY_QUEUE -> {
            val allRecords by viewModel.allTestRecords.collectAsState()
            val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()

            TestHistoryQueueScreen(
                records = allRecords,
                pendingCount = pendingSyncCount,
                onForceSync = { viewModel.triggerSyncNow() },
                onClearHistory = { viewModel.clearHistory() }
            )
        }
        AppDestination.SETTINGS -> {
            val googleScriptUrl by viewModel.googleScriptUrl.collectAsState()

            SettingsScreen(
                googleScriptUrl = googleScriptUrl,
                onSaveGoogleScriptUrl = { viewModel.saveGoogleScriptUrl(it) }
            )
        }
    }
}
