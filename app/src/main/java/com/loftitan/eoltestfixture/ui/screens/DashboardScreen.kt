package com.loftitan.eoltestfixture.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.data.local.TestRecordEntity
import com.loftitan.eoltestfixture.domain.model.CommPortStatus
import com.loftitan.eoltestfixture.domain.model.CompleteTestResult
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.IndicatorStatus
import com.loftitan.eoltestfixture.domain.model.MotorStatus
import com.loftitan.eoltestfixture.domain.model.PcbConnectionState
import com.loftitan.eoltestfixture.domain.model.PowerDiagnostic
import com.loftitan.eoltestfixture.domain.model.SensorPortStatus
import com.loftitan.eoltestfixture.domain.model.TelemetryPoint
import com.loftitan.eoltestfixture.domain.model.TestStatus
import com.loftitan.eoltestfixture.ui.components.OscilloscopeWaveform
import com.loftitan.eoltestfixture.ui.components.ProductionChecklistGrid
import com.loftitan.eoltestfixture.ui.components.ProductionResultBanner
import com.loftitan.eoltestfixture.ui.components.ProductionStatsBanner
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

/**
 * Remapped Factory Production Line Dashboard.
 * Engineered for less-skilled line operators: 1-touch testing, giant Poka-Yoke result indicators,
 * real-time shift yield tracking, and an optional expandable Tech Mode for engineering diagnostics.
 */
@Composable
fun DashboardScreen(
    serialNumber: String,
    onSerialNumberChange: (String) -> Unit,
    onStartTest: () -> Unit,
    onAbortTest: () -> Unit,
    testStatus: TestStatus,
    connectionState: ConnectionState,
    pcbConnectionState: PcbConnectionState,
    lastCompletedResult: CompleteTestResult?,
    allRecords: List<TestRecordEntity>,
    pendingSyncCount: Int,
    onSyncNow: () -> Unit,
    telemetryPoint: TelemetryPoint,
    waveformPoints: List<TelemetryPoint>,
    powerDiagnostic: PowerDiagnostic,
    motorStatuses: List<MotorStatus>,
    commStatuses: List<CommPortStatus>,
    sensorStatuses: List<SensorPortStatus>,
    indicatorStatuses: List<IndicatorStatus>,
    onOpenHardwareDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isTechMode by remember { mutableStateOf(false) }

    val isRigConnected = connectionState == ConnectionState.CONNECTED
    val isPcbDocked = isRigConnected && pcbConnectionState == PcbConnectionState.DOCKED

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
    ) {
        val screenWidth = maxWidth
        val isCompactWidth = screenWidth < 600.dp
        val checklistColumns = if (isCompactWidth) 1 else if (screenWidth < 900.dp) 2 else 3

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Shift KPIs & Cloud Sync Strip
            item {
                ProductionStatsBanner(
                    allRecords = allRecords,
                    pendingSyncCount = pendingSyncCount,
                    isTechMode = isTechMode,
                    onToggleTechMode = { isTechMode = !isTechMode },
                    onSyncNow = onSyncNow
                )
            }

            // 2. Giant Poka-Yoke Result & Docking Banner
            item {
                ProductionResultBanner(
                    testStatus = testStatus,
                    lastResult = lastCompletedResult,
                    connectionState = connectionState,
                    pcbConnectionState = pcbConnectionState,
                    serialNumber = serialNumber,
                    busVoltageV = powerDiagnostic.busVoltageV,
                    quiescentMa = powerDiagnostic.quiescentCurrentMa
                )
            }

            // 3. Massive 1-Tap Operator Action Bar
            item {
                OperatorActionBar(
                    testStatus = testStatus,
                    isRigConnected = isRigConnected,
                    isPcbDocked = isPcbDocked,
                    serialNumber = serialNumber,
                    onStartTest = onStartTest,
                    onAbortTest = onAbortTest,
                    onOpenHardwareDialog = onOpenHardwareDialog
                )
            }

            // 4. Human-Friendly Hardware Checklist
            item {
                ProductionChecklistGrid(
                    powerDiagnostic = powerDiagnostic,
                    motorStatuses = motorStatuses,
                    commStatuses = commStatuses,
                    sensorStatuses = sensorStatuses,
                    indicatorStatuses = indicatorStatuses,
                    isPcbDocked = isPcbDocked,
                    testStatus = testStatus,
                    columns = checklistColumns
                )
            }

            // 5. Expandable Tech Mode (Oscilloscope & Diagnostics for Engineers)
            item {
                AnimatedVisibility(
                    visible = isTechMode,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BambuCardSurface, RoundedCornerShape(12.dp))
                            .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ENGINEERING OSCILLOSCOPE & CURRENT PROFILE",
                                color = BambuTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format("Peak: %.1f mA | Live: %.1f mA", powerDiagnostic.peakCurrentMa, telemetryPoint.currentMa),
                                color = BambuAccentGreen,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        OscilloscopeWaveform(
                            points = waveformPoints,
                            currentMa = telemetryPoint.currentMa,
                            busVoltageV = telemetryPoint.busVoltageV,
                            peakMa = powerDiagnostic.peakCurrentMa,
                            connectionState = connectionState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OperatorActionBar(
    testStatus: TestStatus,
    isRigConnected: Boolean,
    isPcbDocked: Boolean,
    serialNumber: String,
    onStartTest: () -> Unit,
    onAbortTest: () -> Unit,
    onOpenHardwareDialog: () -> Unit
) {
    if (testStatus == TestStatus.RUNNING) {
        Button(
            onClick = onAbortTest,
            colors = ButtonDefaults.buttonColors(
                containerColor = BambuErrorRed,
                contentColor = BambuTextPrimary
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "ABORT RUNNING TEST",
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    } else {
        val (btnText, btnColor, canClick) = when {
            !isRigConnected -> Triple("PLUG TEST RIG USB TO BEGIN", BambuErrorRed, false)
            !isPcbDocked -> Triple("INSERT TEST PCB TO START", BambuWarningYellow, false)
            else -> Triple("▶ START AUTOMATED TEST", BambuSlateBackground, true)
        }

        Button(
            onClick = {
                if (!isRigConnected) onOpenHardwareDialog()
                else if (isPcbDocked) onStartTest()
            },
            enabled = canClick || !isRigConnected,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canClick) BambuAccentGreen else BambuCardSurface,
                contentColor = btnColor,
                disabledContainerColor = BambuCardSurface,
                disabledContentColor = btnColor
            ),
            border = if (!canClick) androidx.compose.foundation.BorderStroke(1.dp, btnColor.copy(alpha = 0.5f)) else null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Icon(
                imageVector = when {
                    !isRigConnected -> Icons.Default.Usb
                    !isPcbDocked -> Icons.Default.HourglassTop
                    else -> Icons.Default.PlayArrow
                },
                contentDescription = null,
                tint = btnColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = btnText,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = btnColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
