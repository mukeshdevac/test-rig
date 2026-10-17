package com.loftitan.eoltestfixture.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.data.local.SyncStatus
import com.loftitan.eoltestfixture.data.local.TestRecordEntity
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestHistoryQueueScreen(
    records: List<TestRecordEntity>,
    pendingCount: Int,
    onForceSync: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
    ) {
        val isNarrow = maxWidth < 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            // Queue Header Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OFFLINE TEST QUEUE",
                        color = BambuTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Box(
                        modifier = Modifier
                            .background(
                                if (pendingCount > 0) BambuWarningYellow.copy(alpha = 0.2f) else BambuAccentGreen.copy(alpha = 0.2f),
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (pendingCount > 0) BambuWarningYellow else BambuAccentGreen,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (pendingCount > 0) "$pendingCount QUEUED" else "ALL SYNCED",
                            color = if (pendingCount > 0) BambuWarningYellow else BambuAccentGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${records.size} local records",
                        color = BambuTextSecondary,
                        fontSize = 11.sp
                    )

                    Row {
                        OutlinedButton(
                            onClick = onClearHistory,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = BambuTextSecondary, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CLEAR", color = BambuTextSecondary, fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onForceSync,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BambuAccentGreen,
                                contentColor = BambuSlateBackground
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FORCE SYNC", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Queue List
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BambuCardSurface, RoundedCornerShape(12.dp))
                        .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No test records found. Completed tests will appear here.",
                        color = BambuTextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(records, key = { it.testId }) { record ->
                        RecordRowItem(record = record, isNarrow = isNarrow)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordRowItem(record: TestRecordEntity, isNarrow: Boolean = false) {
    val dateStr = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))
    val isPass = record.overallResult.equals("PASS", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BambuCardSurface, RoundedCornerShape(10.dp))
            .border(1.dp, BambuCardBorder, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isPass) BambuAccentGreen.copy(alpha = 0.2f) else BambuErrorRed.copy(alpha = 0.2f),
                            RoundedCornerShape(6.dp)
                        )
                        .border(1.dp, if (isPass) BambuAccentGreen else BambuErrorRed, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = record.overallResult,
                        color = if (isPass) BambuAccentGreen else BambuErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = record.serialNumber,
                        color = BambuTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "$dateStr | OP: ${record.operatorId}",
                        color = BambuTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Sync Status Badge
            when (record.syncStatus) {
                SyncStatus.SYNCED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(BambuAccentGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = BambuAccentGreen, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("SYNCED", color = BambuAccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                SyncStatus.PENDING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(BambuWarningYellow.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = BambuWarningYellow, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("QUEUED", color = BambuWarningYellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                SyncStatus.FAILED -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(BambuErrorRed.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.SyncProblem, contentDescription = null, tint = BambuErrorRed, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("RETRY", color = BambuErrorRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Metrics Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Quiescent: ${String.format("%.1f", record.quiescentCurrentMa)}mA",
                color = BambuTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Peak: ${String.format("%.1f", record.peakCurrentMa)}mA",
                color = BambuTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${String.format("%.2f", record.busVoltageV)}V",
                color = BambuTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
