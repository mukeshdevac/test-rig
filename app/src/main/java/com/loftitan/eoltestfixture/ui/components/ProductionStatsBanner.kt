package com.loftitan.eoltestfixture.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Speed
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
import com.loftitan.eoltestfixture.data.local.TestRecordEntity
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow
import java.util.Calendar

/**
 * High-visibility Production Line Stats Banner.
 * Designed for assembly line operators to monitor shift throughput, pass yield, and cloud sync at a glance.
 */
@Composable
fun ProductionStatsBanner(
    allRecords: List<TestRecordEntity>,
    pendingSyncCount: Int,
    isTechMode: Boolean,
    onToggleTechMode: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Filter records from start of today
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayRecords = allRecords.filter { it.timestamp >= startOfToday }
    val totalCount = todayRecords.size
    val passCount = todayRecords.count { it.overallResult == "PASS" }
    val failCount = todayRecords.count { it.overallResult == "FAIL" }
    val yieldRate = if (totalCount > 0) (passCount * 100.0f) / totalCount else 100.0f

    val yieldColor = when {
        totalCount == 0 -> BambuTextSecondary
        yieldRate >= 95.0f -> BambuAccentGreen
        yieldRate >= 90.0f -> BambuWarningYellow
        else -> BambuErrorRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BambuCardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Shift KPIs (Total, Pass, Fail, Yield)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            KpiChip(
                label = "SHIFT TOTAL",
                value = "$totalCount",
                icon = Icons.Default.Speed,
                valueColor = BambuTextPrimary
            )
            KpiChip(
                label = "PASSED",
                value = "$passCount",
                icon = Icons.Default.CheckCircle,
                valueColor = BambuAccentGreen
            )
            KpiChip(
                label = "REJECTED",
                value = "$failCount",
                icon = Icons.Default.Warning,
                valueColor = if (failCount > 0) BambuErrorRed else BambuTextSecondary
            )
            KpiChip(
                label = "YIELD",
                value = if (totalCount > 0) String.format("%.1f%%", yieldRate) else "--",
                icon = null,
                valueColor = yieldColor
            )
        }

        // Action controls (Cloud sync badge + Tech Mode toggle)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cloud Sync Button
            Box(
                modifier = Modifier
                    .background(
                        if (pendingSyncCount > 0) BambuWarningYellow.copy(alpha = 0.15f) else BambuAccentGreen.copy(alpha = 0.12f),
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (pendingSyncCount > 0) BambuWarningYellow.copy(alpha = 0.4f) else BambuAccentGreen.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSyncNow() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (pendingSyncCount > 0) Icons.Default.CloudSync else Icons.Default.CloudDone,
                        contentDescription = "Sync",
                        tint = if (pendingSyncCount > 0) BambuWarningYellow else BambuAccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pendingSyncCount > 0) "$pendingSyncCount TO SYNC" else "SYNCED",
                        color = if (pendingSyncCount > 0) BambuWarningYellow else BambuAccentGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Tech Mode Toggle
            Box(
                modifier = Modifier
                    .background(
                        if (isTechMode) BambuAccentGreen.copy(alpha = 0.2f) else BambuSlateBackground,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp,
                        if (isTechMode) BambuAccentGreen else BambuCardBorder,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleTechMode() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Handyman,
                        contentDescription = "Tech Mode",
                        tint = if (isTechMode) BambuAccentGreen else BambuTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTechMode) "TECH: ON" else "TECH MODE",
                        color = if (isTechMode) BambuAccentGreen else BambuTextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiChip(
    label: String,
    value: String,
    icon: ImageVector?,
    valueColor: Color
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = BambuTextSecondary,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
