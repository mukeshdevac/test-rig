package com.loftitan.eoltestfixture.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.PcbConnectionState
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TopStatusBar(
    operatorId: String,
    isOperatorLoggedIn: Boolean,
    activeSerialNumber: String,
    connectionState: ConnectionState,
    pcbConnectionState: PcbConnectionState = PcbConnectionState.NOT_DETECTED,
    onOperatorClick: () -> Unit,
    onRigClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTimeString by remember { mutableStateOf(getFormattedTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = getFormattedTime()
            delay(1000)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(BambuCardSurface)
            .border(width = 1.dp, color = BambuCardBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val isCompact = maxWidth < 600.dp

        if (isCompact) {
            // Adaptive 2-row layout for phone portrait / narrow screens
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OperatorBadge(
                        operatorId = operatorId,
                        isOperatorLoggedIn = isOperatorLoggedIn,
                        onOperatorClick = onOperatorClick
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RigStatusBadge(connectionState = connectionState, onClick = onRigClick)
                        Spacer(modifier = Modifier.width(6.dp))
                        PcbStatusBadge(
                            pcbConnectionState = pcbConnectionState,
                            isRigConnected = connectionState == ConnectionState.CONNECTED,
                            onClick = onRigClick
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = "SN:",
                            color = BambuTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = activeSerialNumber.ifBlank { "NO PCB" },
                            color = BambuTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    ClockBadge(timeString = currentTimeString)
                }
            }
        } else {
            // Expanded 1-row layout for landscape tablets and desktop/DeX
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OperatorBadge(
                    operatorId = operatorId,
                    isOperatorLoggedIn = isOperatorLoggedIn,
                    onOperatorClick = onOperatorClick
                )

                // Active PCB Serial Number
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ACTIVE PCB:",
                        color = BambuTextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = activeSerialNumber.ifBlank { "NO PCB SCANNED" },
                        color = BambuTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RigStatusBadge(connectionState = connectionState, onClick = onRigClick)
                    Spacer(modifier = Modifier.width(8.dp))
                    PcbStatusBadge(
                        pcbConnectionState = pcbConnectionState,
                        isRigConnected = connectionState == ConnectionState.CONNECTED,
                        onClick = onRigClick
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    ClockBadge(timeString = currentTimeString)
                }
            }
        }
    }
}

@Composable
private fun OperatorBadge(
    operatorId: String,
    isOperatorLoggedIn: Boolean,
    onOperatorClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable { onOperatorClick() }
            .background(
                color = if (isOperatorLoggedIn) BambuSlateBackground else BambuErrorRed.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                width = 1.dp,
                color = if (isOperatorLoggedIn) BambuAccentGreen.copy(alpha = 0.5f) else BambuErrorRed,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Operator",
            tint = if (isOperatorLoggedIn) BambuAccentGreen else BambuErrorRed,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (isOperatorLoggedIn) "OP: $operatorId" else "TAP TO LOGIN",
            color = if (isOperatorLoggedIn) BambuTextPrimary else BambuErrorRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ClockBadge(timeString: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "Clock",
            tint = BambuTextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = timeString,
            color = BambuTextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RigStatusBadge(
    connectionState: ConnectionState,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsatingAlpha"
    )

    val (badgeText, badgeColor, shouldPulse) = when (connectionState) {
        ConnectionState.CONNECTED -> Triple("RIG: ONLINE", BambuAccentGreen, false)
        ConnectionState.RECONNECTING -> Triple("RIG: RECONNECTING", BambuWarningYellow, true)
        ConnectionState.DISCONNECTED -> Triple("RIG: OFFLINE", BambuErrorRed, false)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(if (shouldPulse) alphaAnim else 1.0f)
                .background(badgeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            imageVector = Icons.Default.Usb,
            contentDescription = "Rig USB",
            tint = badgeColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = badgeText,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.alpha(if (shouldPulse) alphaAnim else 1.0f)
        )
    }
}

@Composable
fun PcbStatusBadge(
    pcbConnectionState: PcbConnectionState,
    isRigConnected: Boolean,
    onClick: (() -> Unit)? = null
) {
    val (badgeText, badgeColor) = when {
        !isRigConnected -> Pair("PCB: WAITING RIG", BambuTextSecondary)
        pcbConnectionState == PcbConnectionState.DOCKED -> Pair("PCB: DOCKED", BambuAccentGreen)
        pcbConnectionState == PcbConnectionState.DETECTING -> Pair("PCB: DETECTING", BambuWarningYellow)
        else -> Pair("PCB: NOT SEATED", BambuWarningYellow)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .background(badgeColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(badgeColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            imageVector = Icons.Default.Memory,
            contentDescription = "Target PCB",
            tint = badgeColor,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = badgeText,
            color = badgeColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun getFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}
