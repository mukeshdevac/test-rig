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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun SettingsScreen(
    googleScriptUrl: String,
    onSaveGoogleScriptUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var urlInput by remember(googleScriptUrl) { mutableStateOf(googleScriptUrl) }
    var showSavedMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Google Sheets Web App Endpoint
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Link, contentDescription = null, tint = BambuAccentGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GOOGLE APPS SCRIPT WEB APP ENDPOINT",
                        color = BambuTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Deployed URL of the Google Apps Script Web App receiving test telemetry and results",
                    color = BambuTextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = {
                            urlInput = it
                            showSavedMessage = false
                        },
                        label = { Text("Web App Deployment URL (https://script.google.com/macros/s/...)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BambuTextPrimary,
                            unfocusedTextColor = BambuTextPrimary,
                            focusedBorderColor = BambuAccentGreen,
                            unfocusedBorderColor = BambuCardBorder,
                            focusedLabelColor = BambuAccentGreen,
                            unfocusedLabelColor = BambuTextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            onSaveGoogleScriptUrl(urlInput)
                            showSavedMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BambuAccentGreen,
                            contentColor = BambuSlateBackground
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("SAVE URL", fontWeight = FontWeight.Bold)
                    }
                }

                if (showSavedMessage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = BambuAccentGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Endpoint URL saved successfully.", color = BambuAccentGreen, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Hardware Pin Mapping & Thresholds Reference Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BambuTextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LOF TITAN S3 HARDWARE SPECIFICATION",
                        color = BambuTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "• Connection 1 (Tablet <-> Rig): USB-C OTG Serial @ 115200 Baud\n" +
                            "• Connection 2 (Rig <-> Test PCB): Hardwired UART (Rig TX:17 -> PCB RX, Rig RX:18 <- PCB TX) @ 115200 Baud\n" +
                            "• SPI Loopback: SCK (35), MOSI (36), MISO (37), CS (38)\n" +
                            "• I2C Bus Scan: SCL (GPIO 8), SDA (GPIO 7)\n" +
                            "• Motor Drivers: M1(15/16), M2(13/14), M3(11/12), M4(9/10)\n" +
                            "• Indicators: LED1(47), LED2(48), Buzzer(20)\n" +
                            "• Sensor Ports: S1(2), S2(1), S3(3), S4(4), S5(5), Ultrasonic 1(6), Ultrasonic 2(19)\n" +
                            "• Quiescent Threshold: < 50.0 mA (INA219) | Voltage Window: 11.4V - 12.6V",
                    color = BambuTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
