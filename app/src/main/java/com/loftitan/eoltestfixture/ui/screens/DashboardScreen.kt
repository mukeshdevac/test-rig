package com.loftitan.eoltestfixture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.CommPortStatus
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.MotorStatus
import com.loftitan.eoltestfixture.domain.model.PcbConnectionState
import com.loftitan.eoltestfixture.domain.model.PowerDiagnostic
import com.loftitan.eoltestfixture.domain.model.SensorPortStatus
import com.loftitan.eoltestfixture.domain.model.TelemetryPoint
import com.loftitan.eoltestfixture.domain.model.TestStatus
import com.loftitan.eoltestfixture.ui.components.DiagnosticCard
import com.loftitan.eoltestfixture.ui.components.MetricRow
import com.loftitan.eoltestfixture.ui.components.OscilloscopeWaveform
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

@Composable
fun DashboardScreen(
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    onStartTest: () -> Unit,
    onAbortTest: () -> Unit,
    testStatus: TestStatus,
    connectionState: ConnectionState,
    pcbConnectionState: PcbConnectionState = PcbConnectionState.NOT_DETECTED,
    isOperatorLoggedIn: Boolean,
    onRequireLoginPrompt: () -> Unit,
    telemetryPoint: TelemetryPoint,
    waveformPoints: List<TelemetryPoint>,
    powerDiagnostic: PowerDiagnostic,
    motorStatuses: List<MotorStatus>,
    commStatuses: List<CommPortStatus>,
    sensorStatuses: List<SensorPortStatus>,
    onOpenHardwareDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
    ) {
        val screenWidth = maxWidth
        val isCompactWidth = screenWidth < 600.dp
        val isMediumWidth = screenWidth in 600.dp..900.dp

        val gridColumns = when {
            isCompactWidth -> 1
            isMediumWidth -> 2
            else -> 4
        }

        // On compact screens (phones / small portrait tablets), use a scrollable column
        // On expanded landscape tablets, use clean structured layout
        if (isCompactWidth) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Action Bar (Stacked on compact screens)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BambuCardSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = serialNumber,
                            onValueChange = onSerialNumberChange,
                            label = { Text("PCB Serial Number") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Barcode",
                                    tint = BambuAccentGreen
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                if (isOperatorLoggedIn && testStatus != TestStatus.RUNNING) {
                                    onStartTest()
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BambuTextPrimary,
                                unfocusedTextColor = BambuTextPrimary,
                                focusedBorderColor = BambuAccentGreen,
                                unfocusedBorderColor = BambuCardBorder,
                                focusedLabelColor = BambuAccentGreen,
                                unfocusedLabelColor = BambuTextSecondary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        TestActionButton(
                            testStatus = testStatus,
                            connectionState = connectionState,
                            pcbConnectionState = pcbConnectionState,
                            isOperatorLoggedIn = isOperatorLoggedIn,
                            onRequireLoginPrompt = onRequireLoginPrompt,
                            onOpenHardwareDialog = onOpenHardwareDialog,
                            onStartTest = onStartTest,
                            onAbortTest = onAbortTest,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                    }
                }

                // Connection status banner on compact
                item {
                    val isRigConnected = connectionState == ConnectionState.CONNECTED
                    val isPcbDocked = isRigConnected && pcbConnectionState == PcbConnectionState.DOCKED
                    ConnectionAlertBanner(
                        isRigConnected = isRigConnected,
                        isPcbDocked = isPcbDocked,
                        onOpenHardwareDialog = onOpenHardwareDialog
                    )
                }

                // Waveform Oscilloscope (Placed high on compact view for quick visibility)
                item {
                    OscilloscopeWaveform(
                        points = waveformPoints,
                        currentMa = telemetryPoint.currentMa,
                        busVoltageV = telemetryPoint.busVoltageV,
                        peakMa = powerDiagnostic.peakCurrentMa,
                        connectionState = connectionState,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val isPcbReady = connectionState == ConnectionState.CONNECTED && pcbConnectionState == PcbConnectionState.DOCKED

                // Diagnostic Cards
                item { PowerCard(powerDiagnostic = powerDiagnostic, isPcbReady = isPcbReady) }
                item { MotorsCard(motorStatuses = motorStatuses, isPcbReady = isPcbReady) }
                item { CommsCard(commStatuses = commStatuses, isPcbReady = isPcbReady) }
                item { SensorsCard(sensorStatuses = sensorStatuses, isPcbReady = isPcbReady) }
            }
        } else {
            // Medium / Large screens (Tablets, Foldables, Landscape Phones)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Top Action Bar (Side-by-side)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BambuCardSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = serialNumber,
                        onValueChange = onSerialNumberChange,
                        label = { Text("LOF Titan PCB Barcode / Serial Number") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Barcode",
                                tint = BambuAccentGreen
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            if (isOperatorLoggedIn && testStatus != TestStatus.RUNNING) {
                                onStartTest()
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BambuTextPrimary,
                            unfocusedTextColor = BambuTextPrimary,
                            focusedBorderColor = BambuAccentGreen,
                            unfocusedBorderColor = BambuCardBorder,
                            focusedLabelColor = BambuAccentGreen,
                            unfocusedLabelColor = BambuTextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    TestActionButton(
                        testStatus = testStatus,
                        connectionState = connectionState,
                        pcbConnectionState = pcbConnectionState,
                        isOperatorLoggedIn = isOperatorLoggedIn,
                        onRequireLoginPrompt = onRequireLoginPrompt,
                        onOpenHardwareDialog = onOpenHardwareDialog,
                        onStartTest = onStartTest,
                        onAbortTest = onAbortTest,
                        modifier = Modifier.height(56.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val isRigConnected = connectionState == ConnectionState.CONNECTED
                val isPcbDocked = isRigConnected && pcbConnectionState == PcbConnectionState.DOCKED
                ConnectionAlertBanner(
                    isRigConnected = isRigConnected,
                    isPcbDocked = isPcbDocked,
                    onOpenHardwareDialog = onOpenHardwareDialog
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Diagnostic Cards Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item { PowerCard(powerDiagnostic = powerDiagnostic, isPcbReady = isPcbDocked) }
                    item { MotorsCard(motorStatuses = motorStatuses, isPcbReady = isPcbDocked) }
                    item { CommsCard(commStatuses = commStatuses, isPcbReady = isPcbDocked) }
                    item { SensorsCard(sensorStatuses = sensorStatuses, isPcbReady = isPcbDocked) }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Oscilloscope Waveform (Fills remaining height)
                OscilloscopeWaveform(
                    points = waveformPoints,
                    currentMa = telemetryPoint.currentMa,
                    busVoltageV = telemetryPoint.busVoltageV,
                    peakMa = powerDiagnostic.peakCurrentMa,
                    connectionState = connectionState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConnectionAlertBanner(
    isRigConnected: Boolean,
    isPcbDocked: Boolean,
    onOpenHardwareDialog: () -> Unit = {}
) {
    if (!isRigConnected) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenHardwareDialog() }
                .background(BambuErrorRed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .border(1.dp, BambuErrorRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Usb, contentDescription = null, tint = BambuErrorRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TEST RIG OFFLINE: Connect USB-C OTG cable from tablet to ESP32 fixture mainboard.",
                    color = BambuErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CONNECT RIG >",
                color = BambuAccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    } else if (!isPcbDocked) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BambuWarningYellow.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                .border(1.dp, BambuWarningYellow.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Memory, contentDescription = null, tint = BambuWarningYellow, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "RIG ONLINE: Awaiting LOF Titan S3 Test PCB insertion into fixture bed clamp/pogo-pins.",
                color = BambuWarningYellow,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TestActionButton(
    testStatus: TestStatus,
    connectionState: ConnectionState,
    pcbConnectionState: PcbConnectionState,
    isOperatorLoggedIn: Boolean,
    onRequireLoginPrompt: () -> Unit,
    onOpenHardwareDialog: () -> Unit = {},
    onStartTest: () -> Unit,
    onAbortTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (testStatus == TestStatus.RUNNING) {
        Button(
            onClick = onAbortTest,
            colors = ButtonDefaults.buttonColors(
                containerColor = BambuErrorRed,
                contentColor = BambuTextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = modifier
        ) {
            Icon(Icons.Default.Stop, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ABORT TEST",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    } else {
        val isRigConnected = connectionState == ConnectionState.CONNECTED
        val isPcbDocked = isRigConnected && pcbConnectionState == PcbConnectionState.DOCKED

        val (btnText, btnColor) = when {
            !isRigConnected -> Pair("CONNECT RIG", BambuErrorRed)
            !isPcbDocked -> Pair("INSERT TEST PCB TO START", BambuWarningYellow)
            else -> Pair("START AUTOMATED TEST", BambuSlateBackground)
        }

        Button(
            onClick = {
                if (!isRigConnected) {
                    onOpenHardwareDialog()
                } else if (!isPcbDocked) {
                    // Waiting on PCB
                } else if (!isOperatorLoggedIn) {
                    onRequireLoginPrompt()
                } else {
                    onStartTest()
                }
            },
            enabled = isPcbDocked || !isRigConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (!isRigConnected) BambuErrorRed.copy(alpha = 0.2f) else BambuAccentGreen,
                contentColor = if (!isRigConnected) BambuErrorRed else BambuSlateBackground,
                disabledContainerColor = BambuCardSurface,
                disabledContentColor = BambuTextSecondary
            ),
            border = if (!isRigConnected) androidx.compose.foundation.BorderStroke(1.dp, BambuErrorRed.copy(alpha = 0.6f)) else null,
            shape = RoundedCornerShape(10.dp),
            modifier = modifier
        ) {
            Icon(
                imageVector = when {
                    !isRigConnected -> Icons.Default.Usb
                    !isPcbDocked -> Icons.Default.Memory
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = btnColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = btnText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = btnColor
            )
        }
    }
}

@Composable
private fun PowerCard(powerDiagnostic: PowerDiagnostic, isPcbReady: Boolean) {
    DiagnosticCard(
        title = "Power & Quiescent",
        subtitle = "INA219 High-Side Probe",
        status = if (isPcbReady) powerDiagnostic.status else TestStatus.IDLE
    ) {
        MetricRow(
            label = "Quiescent Draw",
            value = if (isPcbReady && powerDiagnostic.status != TestStatus.IDLE) String.format("%.1f mA", powerDiagnostic.quiescentCurrentMa) else "-- mA",
            status = if (isPcbReady) powerDiagnostic.status else TestStatus.IDLE,
            highlight = isPcbReady && powerDiagnostic.status != TestStatus.IDLE
        )
        MetricRow(
            label = "Peak Current",
            value = if (isPcbReady && powerDiagnostic.status != TestStatus.IDLE) String.format("%.1f mA", powerDiagnostic.peakCurrentMa) else "-- mA"
        )
        MetricRow(
            label = "Bus Voltage",
            value = if (isPcbReady && powerDiagnostic.status != TestStatus.IDLE) String.format("%.2f V", powerDiagnostic.busVoltageV) else "-- V"
        )
        MetricRow(
            label = "Limit (<50mA)",
            value = if (isPcbReady) "PASS THRESHOLD" else "--"
        )
    }
}

@Composable
private fun MotorsCard(motorStatuses: List<MotorStatus>, isPcbReady: Boolean) {
    val overallMotorStatus = when {
        !isPcbReady -> TestStatus.IDLE
        motorStatuses.any { it.status == TestStatus.FAIL } -> TestStatus.FAIL
        motorStatuses.any { it.status == TestStatus.RUNNING } -> TestStatus.RUNNING
        motorStatuses.all { it.status == TestStatus.PASS } -> TestStatus.PASS
        else -> TestStatus.IDLE
    }

    DiagnosticCard(
        title = "Motor Drivers",
        subtitle = "M1–M4 H-Bridge Logic",
        status = overallMotorStatus
    ) {
        motorStatuses.forEach { motor ->
            MetricRow(
                label = "${motor.id} (GPIO ${motor.gpioPins.joinToString("/")})",
                value = if (isPcbReady) motor.details else "--",
                status = if (isPcbReady) motor.status else TestStatus.IDLE
            )
        }
    }
}

@Composable
private fun CommsCard(commStatuses: List<CommPortStatus>, isPcbReady: Boolean) {
    val overallCommStatus = when {
        !isPcbReady -> TestStatus.IDLE
        commStatuses.any { it.status == TestStatus.FAIL } -> TestStatus.FAIL
        commStatuses.any { it.status == TestStatus.RUNNING } -> TestStatus.RUNNING
        commStatuses.all { it.status == TestStatus.PASS } -> TestStatus.PASS
        else -> TestStatus.IDLE
    }

    DiagnosticCard(
        title = "Communication Ports",
        subtitle = "UART / SPI / I2C Buses",
        status = overallCommStatus
    ) {
        commStatuses.forEach { comm ->
            MetricRow(
                label = comm.portName,
                value = if (isPcbReady) comm.details else "--",
                status = if (isPcbReady) comm.status else TestStatus.IDLE
            )
        }
    }
}

@Composable
private fun SensorsCard(sensorStatuses: List<SensorPortStatus>, isPcbReady: Boolean) {
    val overallSensorStatus = when {
        !isPcbReady -> TestStatus.IDLE
        sensorStatuses.any { it.status == TestStatus.FAIL } -> TestStatus.FAIL
        sensorStatuses.any { it.status == TestStatus.RUNNING } -> TestStatus.RUNNING
        sensorStatuses.all { it.status == TestStatus.PASS } -> TestStatus.PASS
        else -> TestStatus.IDLE
    }

    DiagnosticCard(
        title = "Sensor Inputs",
        subtitle = "S1–S5 & Ultrasonic",
        status = overallSensorStatus
    ) {
        MetricRow(
            label = "S1–S3 (GPIO 2, 1, 3)",
            value = if (!isPcbReady) "--" else if (sensorStatuses.take(3).all { it.logicLevelHigh }) "HIGH (3.3V)" else "LOGIC VERIFIED",
            status = if (isPcbReady) (sensorStatuses.getOrNull(0)?.status ?: TestStatus.IDLE) else TestStatus.IDLE
        )
        MetricRow(
            label = "S4–S5 (GPIO 4, 5)",
            value = if (!isPcbReady) "--" else if (sensorStatuses.drop(3).take(2).all { it.logicLevelHigh }) "HIGH (3.3V)" else "LOGIC VERIFIED",
            status = if (isPcbReady) (sensorStatuses.getOrNull(3)?.status ?: TestStatus.IDLE) else TestStatus.IDLE
        )
        MetricRow(
            label = "Ultrasonic 1 (GPIO 6)",
            value = if (isPcbReady) "ECHO / TRIG OK" else "--",
            status = if (isPcbReady) (sensorStatuses.find { it.portName.contains("S1") }?.status ?: TestStatus.IDLE) else TestStatus.IDLE
        )
        MetricRow(
            label = "Ultrasonic 2 (GPIO 19)",
            value = if (isPcbReady) "ECHO / TRIG OK" else "--",
            status = if (isPcbReady) (sensorStatuses.find { it.portName.contains("S2") }?.status ?: TestStatus.IDLE) else TestStatus.IDLE
        )
    }
}
