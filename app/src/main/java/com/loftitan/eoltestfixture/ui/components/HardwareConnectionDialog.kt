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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.UsbDeviceStatus
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

@Composable
fun HardwareConnectionDialog(
    connectionState: ConnectionState,
    attachedDevices: List<UsbDeviceStatus>,
    scanMessage: String?,
    onScanAndConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(Unit) {
        onScanAndConnect()
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BambuCardSurface, RoundedCornerShape(16.dp))
                .border(1.dp, BambuCardBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            tint = BambuAccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TEST RIG HARDWARE LINK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BambuTextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = BambuTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Status Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BambuSlateBackground, RoundedCornerShape(10.dp))
                        .border(1.dp, BambuCardBorder, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rig Connection State",
                            fontSize = 11.sp,
                            color = BambuTextSecondary
                        )
                        Text(
                            text = when (connectionState) {
                                ConnectionState.CONNECTED -> "ONLINE (115200 Baud)"
                                ConnectionState.RECONNECTING -> "CONNECTING / RECONNECTING..."
                                ConnectionState.DISCONNECTED -> "OFFLINE / DISCONNECTED"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (connectionState) {
                                ConnectionState.CONNECTED -> BambuAccentGreen
                                ConnectionState.RECONNECTING -> BambuWarningYellow
                                ConnectionState.DISCONNECTED -> BambuErrorRed
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    RigStatusBadge(connectionState = connectionState)
                }

                if (!scanMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Status: $scanMessage",
                        fontSize = 12.sp,
                        color = BambuTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Attached USB Devices List / Warning
                Text(
                    text = "ATTACHED USB DEVICES (${attachedDevices.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BambuTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (attachedDevices.isEmpty()) {
                    // Helpful troubleshooting box when no device is detected
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BambuWarningYellow.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, BambuWarningYellow.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = BambuWarningYellow,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No USB Hardware Detected on Port",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BambuWarningYellow
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "To establish connection with ESP32 Test Rig:\n" +
                                    "1. Use a USB-C OTG cable or USB OTG adapter.\n" +
                                    "2. Ensure OTG host connector is plugged into tablet.\n" +
                                    "3. Connect the other end to the ESP32 Test Rig.\n" +
                                    "4. Confirm ESP32 power LED is lit.\n\n" +
                                    "(Note: A regular PC charging cable keeps tablet in client mode)",
                            fontSize = 11.sp,
                            color = BambuTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attachedDevices) { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BambuSlateBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, BambuCardBorder, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dev.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BambuTextPrimary
                                    )
                                    Text(
                                        text = dev.hexVidPid,
                                        fontSize = 11.sp,
                                        color = BambuTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (dev.hasPermission) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Permitted",
                                            tint = BambuAccentGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "Need Permission",
                                            color = BambuWarningYellow,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onScanAndConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BambuAccentGreen,
                            contentColor = BambuSlateBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SCAN & CONNECT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    if (connectionState == ConnectionState.CONNECTED) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BambuErrorRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Text("DISCONNECT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
