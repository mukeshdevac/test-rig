package com.loftitan.eoltestfixture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.PinMapping
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

@Composable
fun ManualDiagnosticsScreen(
    onTestPin: (Int) -> Unit,
    onPulseLed: (Int) -> Unit = {},
    onToggleLed: (Int, Boolean) -> Unit = { _, _ -> },
    onBeepBuzzer: () -> Unit = {},
    onToggleBuzzer: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var led1Active by remember { mutableStateOf(false) }
    var led2Active by remember { mutableStateOf(false) }
    var buzzerActive by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
            // Section 1: Indicators & Buzzer Direct Controls
            item {
                ManualSectionCard(
                    title = "INDICATOR & BUZZER TEST (TARGET PCB)",
                    description = "Pulse or toggle visual status LEDs and audible buzzer on LOF Titan PCB"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // LED 1 (Red / GPIO 47)
                        IndicatorControlRow(
                            label = "LED 1 RED (GPIO 47)",
                            icon = Icons.Default.Lightbulb,
                            activeColor = BambuErrorRed,
                            isActive = led1Active,
                            onPulse = { onPulseLed(1); onTestPin(PinMapping.Indicators.LED1_GPIO) },
                            onToggle = { led1Active = !led1Active; onToggleLed(1, led1Active) }
                        )

                        // LED 2 (Green / GPIO 48)
                        IndicatorControlRow(
                            label = "LED 2 GREEN (GPIO 48)",
                            icon = Icons.Default.Lightbulb,
                            activeColor = BambuAccentGreen,
                            isActive = led2Active,
                            onPulse = { onPulseLed(2); onTestPin(PinMapping.Indicators.LED2_GPIO) },
                            onToggle = { led2Active = !led2Active; onToggleLed(2, led2Active) }
                        )

                        // BUZZER (Alert / GPIO 20)
                        IndicatorControlRow(
                            label = "BUZZER (GPIO 20)",
                            icon = Icons.Default.VolumeUp,
                            activeColor = BambuWarningYellow,
                            isActive = buzzerActive,
                            onPulse = { onBeepBuzzer(); onTestPin(PinMapping.Indicators.BUZZER_GPIO) },
                            onToggle = { buzzerActive = !buzzerActive; onToggleBuzzer(buzzerActive) }
                        )
                    }
                }
            }

            // Section 2: Motor Drivers Manual Test
            item {
                ManualSectionCard(
                    title = "MOTOR DRIVERS MANUAL PULSE",
                    description = "Trigger individual H-bridge forward/reverse lines to verify motor phase currents"
                ) {
                    PinMapping.Motors.getAllDrivers().forEach { (motorId, gpios) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$motorId (${gpios.joinToString(",")})",
                                color = BambuTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onTestPin(gpios[0]) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BambuCardSurface, contentColor = BambuAccentGreen),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("INA (${gpios[0]})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { onTestPin(gpios[1]) },
                                    colors = ButtonDefaults.buttonColors(containerColor = BambuCardSurface, contentColor = BambuAccentGreen),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) { Text("INB (${gpios[1]})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }

            // Section 3: Push Buttons (Active LOW with internal pull-up)
            item {
                ManualSectionCard(
                    title = "PUSH BUTTONS (ACTIVE LOW 39-42)",
                    description = "Query digital state of on-board push buttons (0 = Pressed, 1 = Released)"
                ) {
                    PinMapping.Buttons.getAllButtons().forEach { (btnName, gpio) ->
                        ProbeRow(btnName, gpio, "READ BUTTON") { onTestPin(gpio) }
                    }
                }
            }

            // Section 4: Sensor Logic Port Probes
            item {
                ManualSectionCard(
                    title = "SENSOR PORTS LOGIC PROBE (S1-S5, US)",
                    description = "Query high/low input logic levels on sensor headers"
                ) {
                    PinMapping.Sensors.getAllSensors().forEach { (sensorName, gpio) ->
                        ProbeRow(sensorName, gpio, "READ LOGIC") { onTestPin(gpio) }
                    }
                }
            }
        }
    }

@Composable
private fun IndicatorControlRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    isActive: Boolean,
    onPulse: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BambuSlateBackground, RoundedCornerShape(8.dp))
            .border(1.dp, if (isActive) activeColor else BambuCardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) activeColor else BambuTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = if (isActive) BambuTextPrimary else BambuTextSecondary,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onPulse,
                colors = ButtonDefaults.buttonColors(containerColor = BambuCardSurface, contentColor = activeColor),
                border = androidx.compose.foundation.BorderStroke(1.dp, activeColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("PULSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) activeColor else BambuCardSurface,
                    contentColor = if (isActive) BambuSlateBackground else BambuTextPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) activeColor else BambuCardBorder),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(if (isActive) "TURN OFF" else "TURN ON", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ManualSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BambuCardSurface, RoundedCornerShape(12.dp))
            .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            color = BambuTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = description,
            color = BambuTextSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun ProbeRow(name: String, gpio: Int, actionText: String, onAction: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$name -> GPIO $gpio",
            color = BambuTextPrimary,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        OutlinedButton(
            onClick = onAction,
            border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(actionText, color = BambuTextSecondary, fontSize = 11.sp)
        }
    }
}

