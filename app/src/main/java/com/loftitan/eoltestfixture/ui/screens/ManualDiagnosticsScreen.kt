package com.loftitan.eoltestfixture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lightbulb
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
import com.loftitan.eoltestfixture.domain.model.PinMapping
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary

@Composable
fun ManualDiagnosticsScreen(
    onTestPin: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
    ) {
        val isNarrow = maxWidth < 540.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Section: Motor Drivers Manual Test
            item {
                ManualSectionCard(
                    title = "MOTOR DRIVERS MANUAL PULSE",
                    description = "Trigger individual H-bridge forward/reverse lines to verify motor phase currents"
                ) {
                    PinMapping.Motors.getAllDrivers().forEach { (motorId, gpios) ->
                        if (isNarrow) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BambuSlateBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, BambuCardBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$motorId (GPIOs ${gpios.joinToString(", ")})",
                                    color = BambuTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onTestPin(gpios[0]) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BambuCardSurface,
                                            contentColor = BambuAccentGreen
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("PULSE A (${gpios[0]})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onTestPin(gpios[1]) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BambuCardSurface,
                                            contentColor = BambuAccentGreen
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("PULSE B (${gpios[1]})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$motorId (GPIOs ${gpios.joinToString(", ")})",
                                    color = BambuTextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row {
                                    Button(
                                        onClick = { onTestPin(gpios[0]) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BambuCardSurface,
                                            contentColor = BambuAccentGreen
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("PULSE INA (${gpios[0]})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onTestPin(gpios[1]) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BambuCardSurface,
                                            contentColor = BambuAccentGreen
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("PULSE INB (${gpios[1]})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Indicators & Buzzer
            item {
                ManualSectionCard(
                    title = "INDICATOR & BUZZER TEST",
                    description = "Toggle visual LEDs and audible alert buzzer on LOF Titan S3"
                ) {
                    if (isNarrow) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            IndicatorActionCard(
                                name = "LED 1 (GPIO 47)",
                                icon = Icons.Default.Lightbulb,
                                onTrigger = { onTestPin(PinMapping.Indicators.LED1_GPIO) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            IndicatorActionCard(
                                name = "LED 2 (GPIO 48)",
                                icon = Icons.Default.Lightbulb,
                                onTrigger = { onTestPin(PinMapping.Indicators.LED2_GPIO) },
                                modifier = Modifier.fillMaxWidth()
                            )
                            IndicatorActionCard(
                                name = "BUZZER (GPIO 20)",
                                icon = Icons.Default.Campaign,
                                onTrigger = { onTestPin(PinMapping.Indicators.BUZZER_GPIO) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IndicatorActionCard(
                                name = "LED 1 (GPIO 47)",
                                icon = Icons.Default.Lightbulb,
                                onTrigger = { onTestPin(PinMapping.Indicators.LED1_GPIO) },
                                modifier = Modifier.weight(1f)
                            )
                            IndicatorActionCard(
                                name = "LED 2 (GPIO 48)",
                                icon = Icons.Default.Lightbulb,
                                onTrigger = { onTestPin(PinMapping.Indicators.LED2_GPIO) },
                                modifier = Modifier.weight(1f)
                            )
                            IndicatorActionCard(
                                name = "BUZZER (GPIO 20)",
                                icon = Icons.Default.Campaign,
                                onTrigger = { onTestPin(PinMapping.Indicators.BUZZER_GPIO) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section: Sensor Logic Port Probes
            item {
                ManualSectionCard(
                    title = "SENSOR PORTS LOGIC PROBE",
                    description = "Query high/low input logic levels on sensor headers"
                ) {
                    PinMapping.Sensors.getAllSensors().forEach { (sensorName, gpio) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$sensorName -> GPIO $gpio",
                                color = BambuTextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                            OutlinedButton(
                                onClick = { onTestPin(gpio) },
                                border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("READ LOGIC", color = BambuTextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }
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
fun IndicatorActionCard(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onTrigger,
        colors = ButtonDefaults.buttonColors(
            containerColor = BambuSlateBackground,
            contentColor = BambuAccentGreen
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(52.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = BambuAccentGreen)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
