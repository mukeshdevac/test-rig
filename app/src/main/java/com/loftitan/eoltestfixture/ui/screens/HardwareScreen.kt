package com.loftitan.eoltestfixture.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.loftitan.eoltestfixture.domain.model.ConnectionState
import com.loftitan.eoltestfixture.domain.model.PcbConnectionState
import com.loftitan.eoltestfixture.domain.model.UsbDeviceStatus
import com.loftitan.eoltestfixture.ui.components.PcbStatusBadge
import com.loftitan.eoltestfixture.ui.components.RigStatusBadge
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuErrorRed
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary
import com.loftitan.eoltestfixture.ui.theme.BambuWarningYellow

@Composable
fun HardwareScreen(
    connectionState: ConnectionState,
    pcbConnectionState: PcbConnectionState,
    attachedDevices: List<UsbDeviceStatus>,
    scanMessage: String?,
    recentLogs: List<String>,
    onScanAndConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onSendPing: () -> Unit,
    onRequestPermission: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(recentLogs.size) {
        if (recentLogs.isNotEmpty()) {
            listState.animateScrollToItem(recentLogs.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(BambuAccentGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, BambuAccentGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = "Hardware",
                        tint = BambuAccentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TEST RIG & PCB HARDWARE LINK",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BambuTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Dual-Stage Hardwired Architecture: USB OTG (115200 Baud) + ESP32 Hardwired UART",
                        fontSize = 12.sp,
                        color = BambuTextSecondary
                    )
                }
            }
        }

        // Dual-Stage Connection Topology Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "HARDWARE TOPOLOGY (DUAL CONNECTION BUS)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BambuAccentGreen,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Stage 1: Tablet <--> Test Rig ESP32
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BambuSlateBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, BambuCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Cable, contentDescription = null, tint = BambuAccentGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Link 1: Tablet <== USB-C OTG Serial ==> Test Rig ESP32",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BambuTextPrimary
                            )
                            Text(
                                text = "Port: Type-C OTG Host • Baud Rate: 115200 • Format: 8N1",
                                fontSize = 11.sp,
                                color = BambuTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    RigStatusBadge(connectionState = connectionState)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stage 2: Test Rig ESP32 <--> Target PCB
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BambuSlateBackground, RoundedCornerShape(8.dp))
                        .border(1.dp, BambuCardBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Memory, contentDescription = null, tint = BambuWarningYellow, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Link 2: Test Rig ESP32 <== Hardwired UART ==> Target LOF Titan PCB",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BambuTextPrimary
                            )
                            Text(
                                text = "Rig TX (GPIO 17) <-> PCB RX • Rig RX (GPIO 18) <-> PCB TX (115200 Baud)",
                                fontSize = 11.sp,
                                color = BambuTextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    PcbStatusBadge(
                        pcbConnectionState = pcbConnectionState,
                        isRigConnected = connectionState == ConnectionState.CONNECTED
                    )
                }

                if (!scanMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Current Status: $scanMessage",
                        fontSize = 12.sp,
                        color = BambuTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Action Buttons Row
        item {
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
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCAN & CONNECT RIG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                if (connectionState == ConnectionState.CONNECTED) {
                    Button(
                        onClick = onSendPing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BambuSlateBackground,
                            contentColor = BambuAccentGreen
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PING RIG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDisconnect,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BambuErrorRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("DISCONNECT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Detected USB Devices Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "ATTACHED USB DEVICES (${attachedDevices.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BambuTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (attachedDevices.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BambuWarningYellow.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                            .border(1.dp, BambuWarningYellow.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = BambuWarningYellow, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No USB Hardware Detected on Tablet Type-C Port",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = BambuWarningYellow
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "1. Connect an OTG Host Cable / Adapter directly to the tablet's Type-C port.\n" +
                                    "2. Plug the ESP32 Test Rig USB cable into the OTG adapter.\n" +
                                    "3. Verify ESP32 power LED is lit.\n" +
                                    "4. Note: When connected to a computer for ADB debugging, the tablet port stays in device/charging mode.",
                            fontSize = 11.sp,
                            color = BambuTextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        attachedDevices.forEach { dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BambuSlateBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, BambuCardBorder, RoundedCornerShape(8.dp))
                                    .padding(12.dp),
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
                                        text = "${dev.hexVidPid} • Driver: ${if (dev.hasSupportedDriver) "CDC-ACM / Supported" else "Standard USB"}",
                                        fontSize = 11.sp,
                                        color = BambuTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                if (!dev.hasPermission) {
                                    Button(
                                        onClick = { onRequestPermission(dev.deviceName) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = BambuWarningYellow,
                                            contentColor = BambuSlateBackground
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Text("GRANT PERMISSION", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Permitted",
                                            tint = BambuAccentGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Permitted", color = BambuAccentGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Serial Monitor / Frame Console
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, BambuCardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE SERIAL FRAME MONITOR (115200 BAUD)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BambuAccentGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${recentLogs.size} Events",
                        fontSize = 11.sp,
                        color = BambuTextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF090D10), RoundedCornerShape(8.dp))
                        .border(1.dp, BambuCardBorder, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    if (recentLogs.isEmpty()) {
                        Text(
                            text = "Awaiting serial transmission...",
                            color = BambuTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(recentLogs) { logLine ->
                                val logColor = when {
                                    logLine.contains("[RX]") -> BambuAccentGreen
                                    logLine.contains("[TX]") -> Color(0xFF64B5F6)
                                    logLine.contains("[ERR]") -> BambuErrorRed
                                    logLine.contains("[USB]") -> BambuWarningYellow
                                    else -> BambuTextSecondary
                                }
                                Text(
                                    text = logLine,
                                    color = logColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
