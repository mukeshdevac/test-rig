package com.loftitan.eoltestfixture.ui.screens.diagnostics

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.LiveButtonStatus
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary

@Composable
fun LiveButtonsCard(
    buttons: List<LiveButtonStatus>,
    isLiveMonitoring: Boolean,
    onToggleLiveMonitoring: () -> Unit,
    onPollButtons: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BambuCardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PUSH BUTTONS (LIVE STATUS 39-42)",
                    color = BambuTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Active LOW with pull-up • Real-time pressed/released state",
                    color = BambuTextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPollButtons,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder)
                ) {
                    Text("POLL ONCE", fontSize = 11.sp, color = BambuTextPrimary)
                }

                Button(
                    onClick = onToggleLiveMonitoring,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLiveMonitoring) BambuAccentGreen else BambuSlateBackground,
                        contentColor = if (isLiveMonitoring) BambuSlateBackground else BambuTextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isLiveMonitoring) BambuAccentGreen else BambuCardBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isLiveMonitoring) "● LIVE: ON" else "○ LIVE: OFF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buttons.forEach { btn ->
                ButtonStatusRow(btn = btn)
            }
        }
    }
}

@Composable
private fun ButtonStatusRow(btn: LiveButtonStatus) {
    val bgAnim by animateColorAsState(
        targetValue = if (btn.isPressed) BambuAccentGreen.copy(alpha = 0.15f) else BambuSlateBackground,
        label = "btnBg"
    )
    val borderAnim by animateColorAsState(
        targetValue = if (btn.isPressed) BambuAccentGreen else BambuCardBorder,
        label = "btnBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgAnim)
            .border(1.dp, borderAnim, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (btn.isPressed) BambuAccentGreen else Color.Gray.copy(alpha = 0.5f))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${btn.label} (GPIO ${btn.gpio})",
                color = if (btn.isPressed) BambuAccentGreen else BambuTextPrimary,
                fontWeight = if (btn.isPressed) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .background(
                    if (btn.isPressed) BambuAccentGreen else BambuCardSurface,
                    RoundedCornerShape(6.dp)
                )
                .border(
                    1.dp,
                    if (btn.isPressed) BambuAccentGreen else BambuCardBorder,
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (btn.isPressed) "PRESSED (LOW)" else "RELEASED (HIGH)",
                color = if (btn.isPressed) BambuSlateBackground else BambuTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
