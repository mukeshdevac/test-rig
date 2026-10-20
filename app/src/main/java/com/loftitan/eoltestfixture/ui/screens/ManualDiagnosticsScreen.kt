package com.loftitan.eoltestfixture.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ManualDiagnosticsScreen(
    buttonStates: Map<Int, Int> = emptyMap(),
    sensorStates: Map<Int, Int> = emptyMap(),
    sensorVoltages: Map<Int, Float> = emptyMap(),
    onReadButtons: () -> Unit = {},
    onReadSensors: () -> Unit = {},
    onTestPin: (Int) -> Unit = {},
    onPulseLed: (Int) -> Unit = {},
    onToggleLed: (Int, Boolean) -> Unit = { _, _ -> },
    onBeepBuzzer: () -> Unit = {},
    onToggleBuzzer: (Boolean) -> Unit = {},
    onPulseMotor: (String, String, Int) -> Unit = { _, _, _ -> },
    onSetMotorRun: (String, String, Int) -> Unit = { _, _, _ -> },
    onStopMotor: (String) -> Unit = {},
    onStopAllMotors: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var led1Active by remember { mutableStateOf(false) }
    var led2Active by remember { mutableStateOf(false) }
    var buzzerActive by remember { mutableStateOf(false) }
    var isLivePolling by remember { mutableStateOf(false) }

    // Motor control states per motor ID (M1..M6)
    val motorDirections = remember { mutableStateMapOf<String, String>() }
    val motorSpeeds = remember { mutableStateMapOf<String, Int>() }
    val motorRunning = remember { mutableStateMapOf<String, Boolean>() }

    // Auto-read on screen load
    LaunchedEffect(Unit) {
        onReadButtons()
        onReadSensors()
    }

    // Continuous Live polling loop when enabled
    LaunchedEffect(isLivePolling) {
        if (isLivePolling) {
            while (isActive) {
                onReadButtons()
                delay(600)
                onReadSensors()
                delay(600)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BambuSlateBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section 0: Live Monitoring & Quick Refresh Toolbar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BambuCardSurface, RoundedCornerShape(10.dp))
                    .border(
                        1.dp,
                        if (isLivePolling) BambuAccentGreen.copy(alpha = 0.6f) else BambuCardBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isLivePolling) BambuAccentGreen else BambuTextSecondary.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isLivePolling) "LIVE MONITORING ACTIVE (1.2s)" else "LIVE POLLING DISABLED",
                            color = if (isLivePolling) BambuAccentGreen else BambuTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isLivePolling) "Streaming real-time digital states & ADC voltages from LOF Titan PCB" else "Enable to continuously stream button presses & cable sensor voltages",
                            color = BambuTextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onReadButtons()
                            onReadSensors()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BambuSlateBackground, contentColor = BambuAccentGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("REFRESH ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Switch(
                        checked = isLivePolling,
                        onCheckedChange = { isLivePolling = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = BambuAccentGreen,
                            checkedTrackColor = BambuAccentGreen.copy(alpha = 0.3f),
                            uncheckedThumbColor = BambuTextSecondary,
                            uncheckedTrackColor = BambuCardSurface
                        )
                    )
                }
            }
        }

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

        // Section 2: Motor Drivers Bi-Directional & Speed Controls
        item {
            ManualSectionCard(
                title = "MOTOR DRIVERS CONTROL (M1 - M6)",
                description = "Bi-directional PWM speed control (0-100%), continuous run, and pulse test for H-bridge drivers",
                action = {
                    Button(
                        onClick = {
                            motorRunning.clear()
                            onStopAllMotors()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BambuSlateBackground, contentColor = BambuErrorRed),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuErrorRed.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("■ STOP ALL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PinMapping.Motors.getAllDrivers().forEach { (motorId, gpios) ->
                        val dir = motorDirections[motorId] ?: "FWD"
                        val speed = motorSpeeds[motorId] ?: 100
                        val isRunning = motorRunning[motorId] ?: false

                        MotorControlCard(
                            motorId = motorId,
                            gpios = gpios,
                            isRunning = isRunning,
                            direction = dir,
                            speedPct = speed,
                            onDirectionChange = { newDir ->
                                motorDirections[motorId] = newDir
                                if (isRunning) {
                                    onSetMotorRun(motorId, newDir, speed)
                                }
                            },
                            onSpeedChange = { newSpeed ->
                                motorSpeeds[motorId] = newSpeed
                                if (isRunning) {
                                    onSetMotorRun(motorId, dir, newSpeed)
                                }
                            },
                            onPulse = {
                                onPulseMotor(motorId, dir, speed)
                            },
                            onToggleRun = {
                                if (isRunning) {
                                    motorRunning[motorId] = false
                                    onStopMotor(motorId)
                                } else {
                                    motorRunning[motorId] = true
                                    onSetMotorRun(motorId, dir, speed)
                                }
                            },
                            onRawPinTest = { gpio -> onTestPin(gpio) }
                        )
                    }
                }
            }
        }

        // Section 3: Push Buttons (Active LOW with internal pull-up)
        item {
            ManualSectionCard(
                title = "PUSH BUTTONS (ACTIVE LOW 39-42)",
                description = "Live digital state of on-board push buttons (0 = Pressed to GND, 1 = Released to VCC)",
                action = {
                    Button(
                        onClick = onReadButtons,
                        colors = ButtonDefaults.buttonColors(containerColor = BambuSlateBackground, contentColor = BambuAccentGreen),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("READ ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PinMapping.Buttons.getAllButtons().forEach { (btnName, gpio) ->
                        val st = buttonStates[gpio]
                        ButtonProbeRow(
                            name = btnName,
                            gpio = gpio,
                            state = st,
                            onRead = { onReadButtons() }
                        )
                    }
                }
            }
        }

        // Section 4: Sensor Logic & ADC Analog Probes
        item {
            ManualSectionCard(
                title = "SENSOR PORTS & CABLE SENSORS (S1-S5, US)",
                description = "Live digital logic (0/1) and 12-bit ADC analog voltage (0.00V - 3.30V)",
                action = {
                    Button(
                        onClick = onReadSensors,
                        colors = ButtonDefaults.buttonColors(containerColor = BambuSlateBackground, contentColor = Color(0xFF00E5FF)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("READ ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PinMapping.Sensors.getAllSensors().forEach { (sensorName, gpio) ->
                        val st = sensorStates[gpio]
                        val voltage = sensorVoltages[gpio]
                        SensorProbeRow(
                            name = sensorName,
                            gpio = gpio,
                            state = st,
                            voltage = voltage,
                            onRead = { onReadSensors() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MotorControlCard(
    motorId: String,
    gpios: List<Int>,
    isRunning: Boolean,
    direction: String,
    speedPct: Int,
    onDirectionChange: (String) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onPulse: () -> Unit,
    onToggleRun: () -> Unit,
    onRawPinTest: (Int) -> Unit
) {
    val cyanColor = Color(0xFF00E5FF)
    val cardBorderColor = if (isRunning) BambuAccentGreen.copy(alpha = 0.8f) else BambuCardBorder

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BambuSlateBackground, RoundedCornerShape(10.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Motor Header + Status Badge + Run/Stop / Pulse
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = motorId,
                        color = BambuTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (isRunning) BambuAccentGreen.copy(alpha = 0.2f) else BambuCardSurface,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (isRunning) BambuAccentGreen else BambuCardBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isRunning) "● RUNNING ($direction @ $speedPct%)" else "○ STOPPED",
                            color = if (isRunning) BambuAccentGreen else BambuTextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                }
                Text(
                    text = "INA: GPIO ${gpios[0]}  |  INB: GPIO ${gpios[1]}",
                    color = BambuTextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Action Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Pulse Button (250ms burst)
                Button(
                    onClick = onPulse,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BambuCardSurface,
                        contentColor = BambuWarningYellow
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BambuWarningYellow.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("PULSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // RUN / STOP Toggle Button
                Button(
                    onClick = onToggleRun,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) BambuErrorRed else BambuAccentGreen,
                        contentColor = if (isRunning) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isRunning) "STOP ■" else "RUN ▶",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Row 2: Direction Selector & Speed Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "DIR:",
                    color = BambuTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )

                // FWD Button
                val isFwd = direction == "FWD"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFwd) BambuAccentGreen else BambuCardSurface)
                        .border(
                            1.dp,
                            if (isFwd) BambuAccentGreen else BambuCardBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onDirectionChange("FWD") }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "FWD ▶",
                        color = if (isFwd) Color.Black else BambuTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // REV Button
                val isRev = direction == "REV"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isRev) cyanColor else BambuCardSurface)
                        .border(
                            1.dp,
                            if (isRev) cyanColor else BambuCardBorder,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onDirectionChange("REV") }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "REV ◀",
                        color = if (isRev) Color.Black else BambuTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Speed Preset Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(25, 50, 75, 100).forEach { preset ->
                    val isSelected = speedPct == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) BambuAccentGreen.copy(alpha = 0.25f) else BambuCardSurface
                            )
                            .border(
                                1.dp,
                                if (isSelected) BambuAccentGreen else BambuCardBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSpeedChange(preset) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$preset%",
                            color = if (isSelected) BambuAccentGreen else BambuTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Row 3: PWM Speed Slider (10% - 100%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "PWM SPEED: $speedPct%",
                color = BambuTextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(115.dp)
            )

            Slider(
                value = speedPct.toFloat(),
                onValueChange = { onSpeedChange(it.toInt()) },
                valueRange = 10f..100f,
                steps = 17, // 5% increments
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = BambuAccentGreen,
                    activeTrackColor = BambuAccentGreen,
                    inactiveTrackColor = BambuCardSurface
                )
            )
        }

        // Row 4: Raw Pin Diagnostics (INA / INB Direct Pulse)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Direct Pin Logic Pulse:",
                color = BambuTextSecondary.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onRawPinTest(gpios[0]) },
                    colors = ButtonDefaults.buttonColors(containerColor = BambuCardSurface, contentColor = BambuTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("INA (${gpios[0]})", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = { onRawPinTest(gpios[1]) },
                    colors = ButtonDefaults.buttonColors(containerColor = BambuCardSurface, contentColor = BambuTextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BambuCardBorder),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("INB (${gpios[1]})", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ButtonProbeRow(
    name: String,
    gpio: Int,
    state: Int?,
    onRead: () -> Unit
) {
    val isPressed = state == 0
    val isReleased = state == 1
    val isUnread = state == null

    val badgeBg = when {
        isPressed -> BambuAccentGreen.copy(alpha = 0.22f)
        isReleased -> BambuCardSurface
        else -> BambuCardSurface.copy(alpha = 0.5f)
    }
    val badgeBorder = when {
        isPressed -> BambuAccentGreen
        isReleased -> BambuCardBorder
        else -> BambuCardBorder.copy(alpha = 0.4f)
    }
    val badgeTextColor = when {
        isPressed -> BambuAccentGreen
        isReleased -> BambuTextSecondary
        else -> BambuTextSecondary.copy(alpha = 0.5f)
    }
    val badgeText = when {
        isPressed -> "● PRESSED (0)"
        isReleased -> "○ RELEASED (1)"
        else -> "-- UNREAD --"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BambuSlateBackground, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (isPressed) BambuAccentGreen.copy(alpha = 0.7f) else if (isUnread) BambuCardBorder.copy(alpha = 0.5f) else BambuCardBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$name  (GPIO $gpio)",
                color = BambuTextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = "Active LOW: 0 = Pressed to GND / 1 = Released",
                color = BambuTextSecondary,
                fontSize = 10.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live status badge
            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(6.dp))
                    .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            // Read action button
            Button(
                onClick = onRead,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BambuCardSurface,
                    contentColor = BambuAccentGreen
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, BambuAccentGreen.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("READ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SensorProbeRow(
    name: String,
    gpio: Int,
    state: Int?,
    voltage: Float?,
    onRead: () -> Unit
) {
    val isHigh = state == 1
    val isLow = state == 0
    val isUnread = state == null && voltage == null

    val cyanColor = Color(0xFF00E5FF)
    val amberColor = Color(0xFFFF9100)

    val badgeBg = when {
        isHigh -> cyanColor.copy(alpha = 0.20f)
        isLow -> amberColor.copy(alpha = 0.20f)
        voltage != null -> cyanColor.copy(alpha = 0.15f)
        else -> BambuCardSurface.copy(alpha = 0.5f)
    }
    val badgeBorder = when {
        isHigh -> cyanColor
        isLow -> amberColor
        voltage != null -> cyanColor.copy(alpha = 0.6f)
        else -> BambuCardBorder.copy(alpha = 0.4f)
    }
    val badgeTextColor = when {
        isHigh -> cyanColor
        isLow -> amberColor
        voltage != null -> cyanColor
        else -> BambuTextSecondary.copy(alpha = 0.5f)
    }

    val badgeText = when {
        voltage != null && state != null -> {
            val logicLabel = if (state == 1) "HIGH" else "LOW"
            "● $logicLabel (${"%.2f".format(voltage)}V)"
        }
        voltage != null -> "● ${"%.2f".format(voltage)}V"
        isHigh -> "● HIGH (1 / 3.3V)"
        isLow -> "○ LOW (0 / 0V)"
        else -> "-- UNREAD --"
    }

    val pinDescription = when (gpio) {
        PinMapping.Sensors.S1_GPIO -> "ADC1 CH1 (Cable S1 / Analog + Digital)"
        PinMapping.Sensors.S2_GPIO -> "ADC1 CH0 (Cable S2 / Analog + Digital)"
        PinMapping.Sensors.S3_GPIO -> "ADC1 CH2 (Cable S3 / Analog + Digital)"
        PinMapping.Sensors.S4_GPIO -> "ADC1 CH3 (Cable S4 / Analog + Digital)"
        PinMapping.Sensors.S5_GPIO -> "ADC1 CH4 (Cable S5 / Analog + Digital)"
        PinMapping.Sensors.ULTRASONIC_S1_GPIO -> "ADC1 CH5 (Ultrasonic Trig/S1)"
        PinMapping.Sensors.ULTRASONIC_S2_GPIO -> "GPIO 7 (Ultrasonic Echo/S2)"
        else -> "Sensor Port"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BambuSlateBackground, RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (!isUnread) badgeBorder.copy(alpha = 0.6f) else BambuCardBorder,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "$name  (GPIO $gpio)",
                color = BambuTextPrimary,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
            Text(
                text = pinDescription,
                color = BambuTextSecondary,
                fontSize = 10.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live status badge
            Box(
                modifier = Modifier
                    .background(badgeBg, RoundedCornerShape(6.dp))
                    .border(1.dp, badgeBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = badgeTextColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            // Read action button
            Button(
                onClick = onRead,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BambuCardSurface,
                    contentColor = cyanColor
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, cyanColor.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("READ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
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
            }
            if (action != null) {
                Spacer(modifier = Modifier.width(8.dp))
                action()
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}
