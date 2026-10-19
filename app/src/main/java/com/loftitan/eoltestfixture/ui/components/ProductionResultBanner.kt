package com.loftitan.eoltestfixture.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.CompleteTestResult
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.PcbConnectionState
import com.loftitan.eoltestfixture.domain.model.TestStatus
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

/**
 * Poka-Yoke (Mistake-Proof) Giant Visual Indicator Banner.
 * Gives line operators unmistakable, high-contrast Pass / Fail / Docking feedback.
 */
@Composable
fun ProductionResultBanner(
    testStatus: TestStatus,
    lastResult: CompleteTestResult?,
    connectionState: ConnectionState,
    pcbConnectionState: PcbConnectionState,
    serialNumber: String,
    busVoltageV: Float,
    quiescentMa: Float,
    modifier: Modifier = Modifier
) {
    val isRigConnected = connectionState == ConnectionState.CONNECTED
    val isPcbDocked = isRigConnected && pcbConnectionState == PcbConnectionState.DOCKED

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    when {
        // 1. Live Test Running
        testStatus == TestStatus.RUNNING -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A2239), RoundedCornerShape(14.dp))
                    .border(2.dp, Color(0xFF00A3FF), RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Color(0xFF00A3FF),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "AUTOMATED TEST IN PROGRESS...",
                            color = Color(0xFF00A3FF),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Verifying power, motor drivers, buttons & sensors for $serialNumber",
                            color = BambuTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 2. Recent Test Completed: PASS
        lastResult != null && lastResult.overallStatus == TestStatus.PASS && testStatus != TestStatus.RUNNING -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B2B16), RoundedCornerShape(14.dp))
                    .border(2.5.dp, BambuAccentGreen, RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "PASS",
                        tint = BambuAccentGreen,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PASS",
                                color = BambuAccentGreen,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "— CLEARED FOR ASSEMBLY",
                                color = BambuAccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Board ${lastResult.serialNumber.ifBlank { serialNumber }} passed all tests in ${lastResult.durationMs}ms. Remove and insert next PCB.",
                            color = BambuTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 3. Recent Test Completed: FAIL
        lastResult != null && lastResult.overallStatus == TestStatus.FAIL && testStatus != TestStatus.RUNNING -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(Color(0xFF330C0D), RoundedCornerShape(14.dp))
                    .border(2.5.dp, BambuErrorRed, RoundedCornerShape(14.dp))
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "FAIL",
                        tint = BambuErrorRed,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FAIL",
                                color = BambuErrorRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "— REJECT BOARD",
                                color = BambuErrorRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Defect detected on ${lastResult.serialNumber.ifBlank { serialNumber }}. Set aside in REJECT bin for diagnosis.",
                            color = BambuTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // 4. Standby: Board Docked & Ready
        isPcbDocked -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(14.dp))
                    .border(2.dp, BambuAccentGreen.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(BambuAccentGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = BambuAccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TEST PCB DOCKED & READY",
                                color = BambuAccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = serialNumber.ifBlank { "LOF_TITAN_S3" },
                                color = BambuTextPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = String.format("Power: %.2fV  |  Standby: %.1f mA  |  Ready for 1-click test", busVoltageV, quiescentMa),
                            color = BambuTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 5. Standby: Waiting for Board Insertion
        isRigConnected -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .alpha(pulseAlpha)
                    .background(BambuWarningYellow.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .border(2.dp, BambuWarningYellow.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = null,
                    tint = BambuWarningYellow,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "READY FOR NEXT PCB — PLACE BOARD ON TEST FIXTURE",
                        color = BambuWarningYellow,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Clamp PCB onto pogo-pins. Board will automatically sync and enable the START button.",
                        color = BambuTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 6. Test Rig Disconnected
        else -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(BambuErrorRed.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .border(2.dp, BambuErrorRed.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Usb,
                    contentDescription = null,
                    tint = BambuErrorRed,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "TEST RIG DISCONNECTED — PLUG USB-C CABLE",
                        color = BambuErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Connect tablet USB-C OTG cable to the ESP32 Test Rig mainboard to proceed.",
                        color = BambuTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
