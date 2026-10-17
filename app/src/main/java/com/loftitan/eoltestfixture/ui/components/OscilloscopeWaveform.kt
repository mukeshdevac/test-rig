package com.loftitan.eoltestfixture.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.TelemetryPoint
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuCyan
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuGridLine
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary

@Composable
fun OscilloscopeWaveform(
    points: List<TelemetryPoint>,
    currentMa: Float,
    busVoltageV: Float,
    peakMa: Float,
    connectionState: ConnectionState = ConnectionState.CONNECTED,
    modifier: Modifier = Modifier
) {
    val isConnected = connectionState == ConnectionState.CONNECTED

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BambuCardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        // Waveform Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "LIVE TELEMETRY WAVEFORM (INA219)",
                    color = BambuTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (isConnected) "High-frequency dynamic current consumption profile (mA vs. Time)" else "Hardware rig disconnected - waiting for USB OTG connection",
                    color = if (isConnected) BambuTextSecondary else BambuErrorRed,
                    fontSize = 11.sp
                )
            }

            // Real-time Reading Badges (Shows '--' when disconnected)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TelemetryBadge(
                    label = "CURRENT",
                    value = if (isConnected) String.format("%.1f mA", currentMa) else "-- mA",
                    accentColor = if (isConnected) BambuAccentGreen else BambuTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                TelemetryBadge(
                    label = "PEAK",
                    value = if (isConnected) String.format("%.1f mA", peakMa) else "-- mA",
                    accentColor = if (isConnected) BambuCyan else BambuTextSecondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                TelemetryBadge(
                    label = "BUS VOLTAGE",
                    value = if (isConnected) String.format("%.2f V", busVoltageV) else "-- V",
                    accentColor = if (isConnected) BambuTextPrimary else BambuTextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Oscilloscope Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(BambuSlateBackground, RoundedCornerShape(8.dp))
                .border(1.dp, BambuGridLine, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            if (!isConnected) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Disconnected",
                        tint = BambuErrorRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "NO HARDWARE RIG DETECTED",
                        color = BambuErrorRed,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Connect ESP32 test rig via USB-C OTG cable",
                        color = BambuTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw Grid Lines (Horizontal & Vertical)
                val gridLinesX = 10
                val gridLinesY = 5
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)

                for (i in 1 until gridLinesX) {
                    val x = canvasWidth * (i.toFloat() / gridLinesX)
                    drawLine(
                        color = BambuGridLine,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                for (j in 1 until gridLinesY) {
                    val y = canvasHeight * (j.toFloat() / gridLinesY)
                    drawLine(
                        color = BambuGridLine,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                // Threshold indicator line (e.g. 200mA line)
                val maxRange = 300f // mA scale ceiling
                val thresholdY = canvasHeight - ((200f / maxRange) * canvasHeight)
                drawLine(
                    color = BambuErrorRed.copy(alpha = 0.4f),
                    start = Offset(0f, thresholdY),
                    end = Offset(canvasWidth, thresholdY),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )

                if (points.size >= 2) {
                    val path = Path()
                    val fillPath = Path()

                    val stepX = canvasWidth / (points.size - 1).coerceAtLeast(1)

                    points.forEachIndexed { index, point ->
                        val normalizedY = (point.currentMa / maxRange).coerceIn(0f, 1f)
                        val x = index * stepX
                        val y = canvasHeight - (normalizedY * canvasHeight)

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.moveTo(x, canvasHeight)
                            fillPath.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }

                    fillPath.lineTo(canvasWidth, canvasHeight)
                    fillPath.close()

                    // Draw gradient under curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BambuAccentGreen.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw bright oscilloscope line
                    drawPath(
                        path = path,
                        color = BambuAccentGreen,
                        style = Stroke(width = 2.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryBadge(label: String, value: String, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(BambuSlateBackground, RoundedCornerShape(6.dp))
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$label: ",
            color = BambuTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
