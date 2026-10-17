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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.TestStatus
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

@Composable
fun DiagnosticCard(
    title: String,
    subtitle: String,
    status: TestStatus,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .background(BambuCardSurface, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = when (status) {
                    TestStatus.PASS -> BambuAccentGreen.copy(alpha = 0.5f)
                    TestStatus.FAIL -> BambuErrorRed.copy(alpha = 0.6f)
                    TestStatus.RUNNING -> BambuWarningYellow.copy(alpha = 0.5f)
                    else -> BambuCardBorder
                },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    color = BambuTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = BambuTextSecondary,
                    fontSize = 11.sp
                )
            }
            StatusPill(status = status)
        }

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

@Composable
fun StatusPill(status: TestStatus) {
    val (color, text) = when (status) {
        TestStatus.PASS -> BambuAccentGreen to "PASS"
        TestStatus.FAIL -> BambuErrorRed to "FAIL"
        TestStatus.RUNNING -> BambuWarningYellow to "TESTING"
        TestStatus.WARNING -> BambuWarningYellow to "WARN"
        TestStatus.IDLE -> BambuTextSecondary to "STANDBY"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (status == TestStatus.RUNNING) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                color = BambuWarningYellow,
                strokeWidth = 2.dp
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun MetricRow(
    label: String,
    value: String,
    status: TestStatus? = null,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = BambuTextSecondary,
            fontSize = 13.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = if (highlight) BambuAccentGreen else BambuTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            if (status != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = when (status) {
                        TestStatus.PASS -> Icons.Default.Check
                        TestStatus.FAIL -> Icons.Default.Close
                        else -> Icons.Default.HourglassEmpty
                    },
                    contentDescription = null,
                    tint = when (status) {
                        TestStatus.PASS -> BambuAccentGreen
                        TestStatus.FAIL -> BambuErrorRed
                        else -> BambuTextSecondary
                    },
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
