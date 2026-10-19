package com.loftitan.eoltestfixture.ui.screens.diagnostics

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loftitan.eoltestfixture.domain.model.LiveI2cScanStatus
import com.loftitan.eoltestfixture.ui.theme.BambuAccentGreen
import com.loftitan.eoltestfixture.ui.theme.BambuCardBorder
import com.loftitan.eoltestfixture.ui.theme.BambuCardSurface
import com.loftitan.eoltestfixture.ui.theme.BambuSlateBackground
import com.loftitan.eoltestfixture.ui.theme.BambuTextPrimary
import com.loftitan.eoltestfixture.ui.theme.BambuTextSecondary

@Composable
fun LiveI2cScanCard(
    scanStatus: LiveI2cScanStatus,
    onTriggerScan: () -> Unit
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
                    text = "I2C BUS SCANNER (SDA: GPIO 7, SCL: GPIO 8)",
                    color = BambuTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Detect on-board I2C peripherals (e.g. INA219, IMU sensors)",
                    color = BambuTextSecondary,
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = onTriggerScan,
                enabled = !scanStatus.isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BambuCardSurface,
                    contentColor = BambuAccentGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (scanStatus.isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = BambuAccentGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SCANNING...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = BambuAccentGreen
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SCAN BUS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (scanStatus.devices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuSlateBackground, RoundedCornerShape(8.dp))
                    .border(1.dp, BambuCardBorder, RoundedCornerShape(8.dp))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (scanStatus.lastScanTimestamp > 0) "No I2C devices responded on address 0x01-0x7F" else "Press 'SCAN BUS' to probe I2C addresses",
                    color = BambuTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                scanStatus.devices.forEach { devHex ->
                    I2cChip(addressHex = devHex)
                }
            }
        }
    }
}

@Composable
private fun I2cChip(addressHex: String) {
    val description = when (addressHex.uppercase()) {
        "0X40" -> "INA219 Power"
        "0X68" -> "MPU6050 IMU"
        "0X76", "0X77" -> "BMP280 Sensor"
        else -> "I2C Peripheral"
    }

    Box(
        modifier = Modifier
            .background(BambuSlateBackground, RoundedCornerShape(8.dp))
            .border(1.dp, BambuAccentGreen, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = addressHex.uppercase(),
                color = BambuAccentGreen,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            Text(
                text = description,
                color = BambuTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
