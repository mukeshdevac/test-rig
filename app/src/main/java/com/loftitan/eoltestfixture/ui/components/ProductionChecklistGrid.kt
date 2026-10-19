package com.loftitan.eoltestfixture.ui.components

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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.CommPortStatus
import com.loftitan.eoltestfixture.domain.model.IndicatorStatus
import com.loftitan.eoltestfixture.domain.model.MotorStatus
import com.loftitan.eoltestfixture.domain.model.PowerDiagnostic
import com.loftitan.eoltestfixture.domain.model.SensorPortStatus
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
 * Clean, Human-Friendly Production Hardware Checklist.
 * Breaks down testing into 6 clear hardware modules with plain-English status indicators for assembly line workers.
 */
@Composable
fun ProductionChecklistGrid(
    powerDiagnostic: PowerDiagnostic,
    motorStatuses: List<MotorStatus>,
    commStatuses: List<CommPortStatus>,
    sensorStatuses: List<SensorPortStatus>,
    indicatorStatuses: List<IndicatorStatus>,
    isPcbDocked: Boolean,
    testStatus: TestStatus,
    columns: Int = 3,
    modifier: Modifier = Modifier
) {
    // 1. Power Status
    val powerStatus = if (isPcbDocked) powerDiagnostic.status else TestStatus.IDLE
    val powerSummary = if (isPcbDocked && powerDiagnostic.status != TestStatus.IDLE) {
        String.format("%.1fV | %.0fmA Standby", powerDiagnostic.busVoltageV, powerDiagnostic.quiescentCurrentMa)
    } else {
        "High-Side Voltage & Current"
    }

    // 2. Controller & UART
    val uartStatus = if (isPcbDocked) {
        commStatuses.find { it.portName.contains("UART", ignoreCase = true) }?.status ?: TestStatus.IDLE
    } else TestStatus.IDLE
    val uartSummary = if (isPcbDocked) "Microcontroller Synced" else "Rig UART Handshake"

    // 3. Motor Drivers (M1 - M4)
    val motorOverall = when {
        !isPcbDocked -> TestStatus.IDLE
        motorStatuses.any { it.status == TestStatus.FAIL } -> TestStatus.FAIL
        motorStatuses.any { it.status == TestStatus.RUNNING } -> TestStatus.RUNNING
        motorStatuses.all { it.status == TestStatus.PASS } -> TestStatus.PASS
        else -> TestStatus.IDLE
    }
    val motorSummary = when (motorOverall) {
        TestStatus.PASS -> "All 4 Channels (M1–M4) Pass"
        TestStatus.FAIL -> "Channel Fault Detected"
        else -> "M1–M4 H-Bridge Drivers"
    }

    // 4. Push Buttons
    val buttonStatus = when {
        !isPcbDocked -> TestStatus.IDLE
        testStatus == TestStatus.RUNNING -> TestStatus.RUNNING
        testStatus == TestStatus.PASS -> TestStatus.PASS
        else -> TestStatus.IDLE
    }
    val buttonSummary = if (buttonStatus == TestStatus.PASS) "Buttons 1–4 Pull-Ups OK" else "On-Board Buttons 39–42"

    // 5. Sensors & Ultrasonic
    val sensorOverall = when {
        !isPcbDocked -> TestStatus.IDLE
        sensorStatuses.any { it.status == TestStatus.FAIL } -> TestStatus.FAIL
        sensorStatuses.any { it.status == TestStatus.RUNNING } -> TestStatus.RUNNING
        sensorStatuses.all { it.status == TestStatus.PASS } -> TestStatus.PASS
        else -> TestStatus.IDLE
    }
    val sensorSummary = if (sensorOverall == TestStatus.PASS) "S1–S5 & Ultrasonic OK" else "Digital Sensor Ports"

    // 6. Indicators (LEDs & Buzzer)
    val indicatorOverall = when {
        !isPcbDocked -> TestStatus.IDLE
        indicatorStatuses.any { !it.verified && testStatus == TestStatus.FAIL } -> TestStatus.FAIL
        indicatorStatuses.all { it.verified } -> TestStatus.PASS
        testStatus == TestStatus.RUNNING -> TestStatus.RUNNING
        else -> TestStatus.IDLE
    }
    val indicatorSummary = if (indicatorOverall == TestStatus.PASS) "Red/Green LEDs & Buzzer OK" else "Visual & Audio Alerts"

    val checklistItems = listOf(
        SubsystemData("1. Power & Supply", powerSummary, powerStatus, Icons.Default.ElectricBolt),
        SubsystemData("2. Controller & Sync", uartSummary, uartStatus, Icons.Default.Memory),
        SubsystemData("3. Motor Channels", motorSummary, motorOverall, Icons.Default.PrecisionManufacturing),
        SubsystemData("4. Push Buttons", buttonSummary, buttonStatus, Icons.Default.SmartButton),
        SubsystemData("5. Sensors & Ranging", sensorSummary, sensorOverall, Icons.Default.Sensors),
        SubsystemData("6. Alerts & Sound", indicatorSummary, indicatorOverall, Icons.Default.Campaign)
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        checklistItems.chunked(columns.coerceAtLeast(1)).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowItems.forEach { item ->
                    SubsystemCard(
                        title = item.title,
                        summary = item.summary,
                        status = item.status,
                        icon = item.icon,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining cells in the row if row is not full
                val remainder = columns - rowItems.size
                if (remainder > 0) {
                    repeat(remainder) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class SubsystemData(
    val title: String,
    val summary: String,
    val status: TestStatus,
    val icon: ImageVector
)

@Composable
private fun SubsystemCard(
    title: String,
    summary: String,
    status: TestStatus,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val (statusLabel, statusColor, bgColor) = when (status) {
        TestStatus.PASS -> Triple("PASS", BambuAccentGreen, BambuAccentGreen.copy(alpha = 0.12f))
        TestStatus.FAIL -> Triple("FAIL", BambuErrorRed, BambuErrorRed.copy(alpha = 0.15f))
        TestStatus.RUNNING -> Triple("TESTING", Color(0xFF00A3FF), Color(0xFF00A3FF).copy(alpha = 0.12f))
        TestStatus.WARNING -> Triple("WARN", BambuWarningYellow, BambuWarningYellow.copy(alpha = 0.15f))
        TestStatus.IDLE -> Triple("READY", BambuTextSecondary, BambuSlateBackground)
        else -> Triple("READY", BambuTextSecondary, BambuSlateBackground)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BambuCardSurface, RoundedCornerShape(10.dp))
            .border(
                1.dp,
                if (status == TestStatus.PASS) BambuAccentGreen.copy(alpha = 0.5f)
                else if (status == TestStatus.FAIL) BambuErrorRed.copy(alpha = 0.6f)
                else BambuCardBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(bgColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = BambuTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = summary,
                    color = BambuTextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        // Status Badge
        Box(
            modifier = Modifier
                .background(bgColor, RoundedCornerShape(6.dp))
                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = statusLabel,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
