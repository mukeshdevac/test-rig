package com.loftitan.eoltestfixture.domain.model

import kotlinx.serialization.Serializable

/**
 * High-level connection state for USB OTG Serial interface.
 */
enum class ConnectionState {
    CONNECTED,
    RECONNECTING,
    DISCONNECTED
}

/**
 * Physical mounting and electrical sensing state of the Target Test PCB (DUT)
 * seated onto the test rig fixture bed via pogo-pins.
 */
enum class PcbConnectionState {
    DOCKED,          // Test PCB is mounted, clamped, and detected on the fixture bed
    DETECTING,       // Sensed initial contact / probing
    NOT_DETECTED     // No PCB placed in fixture bed
}


/**
 * Overall and per-component test pass/fail outcome.
 */
enum class TestStatus {
    IDLE,
    RUNNING,
    PASS,
    FAIL,
    WARNING
}

/**
 * Real-time telemetry data point emitted by INA219 probe via ESP32.
 */
@Serializable
data class TelemetryPoint(
    val timestampMs: Long,
    val currentMa: Float,
    val busVoltageV: Float = 0.0f
)

/**
 * Diagnostic card summary for the UI dashboard.
 */
data class PowerDiagnostic(
    val quiescentCurrentMa: Float = 0f,
    val peakCurrentMa: Float = 0f,
    val busVoltageV: Float = 0f,
    val status: TestStatus = TestStatus.IDLE
)

data class MotorStatus(
    val id: String, // "M1", "M2", "M3", "M4"
    val gpioPins: List<Int>,
    val status: TestStatus = TestStatus.IDLE,
    val details: String = "Standby"
)

data class CommPortStatus(
    val portName: String, // "UART", "SPI", "I2C"
    val pins: String,
    val status: TestStatus = TestStatus.IDLE,
    val details: String = "Untested"
)

data class SensorPortStatus(
    val portName: String, // "S1" .. "S5", "Ultrasonic S1", "Ultrasonic S2"
    val gpio: Int,
    val logicLevelHigh: Boolean = false,
    val status: TestStatus = TestStatus.IDLE
)

data class IndicatorStatus(
    val name: String, // "LED1", "LED2", "Buzzer"
    val gpio: Int,
    val verified: Boolean = false
)

/**
 * Complete test run outcome across all sub-systems.
 */
data class CompleteTestResult(
    val testId: String,
    val serialNumber: String,
    val operatorId: String,
    val timestamp: Long,
    val overallStatus: TestStatus,
    val durationMs: Long,
    val power: PowerDiagnostic,
    val motors: List<MotorStatus>,
    val comms: List<CommPortStatus>,
    val sensors: List<SensorPortStatus>,
    val indicators: List<IndicatorStatus>,
    val waveformPoints: List<TelemetryPoint>
)

data class UsbDeviceStatus(
    val deviceName: String,
    val vendorId: Int,
    val productId: Int,
    val manufacturerName: String?,
    val productName: String?,
    val hasPermission: Boolean,
    val hasSupportedDriver: Boolean
) {
    val displayName: String
        get() = productName?.ifBlank { null }
            ?: when (vendorId) {
                0x10C4 -> "Silicon Labs CP210x Bridge"
                0x1A86 -> "QinHeng CH340 / CH9102 Serial"
                0x303A -> "Espressif USB-JTAG / Serial (ESP32-S3)"
                0x0403 -> "FTDI USB-Serial UART"
                else -> "USB Serial Device ($deviceName)"
            }

    val hexVidPid: String
        get() = "VID: 0x${vendorId.toString(16).uppercase().padStart(4, '0')} | PID: 0x${productId.toString(16).uppercase().padStart(4, '0')}"
}

data class LiveButtonStatus(
    val id: String,
    val label: String,
    val gpio: Int,
    val isPressed: Boolean = false,
    val rawValue: Int = 1
)

data class LiveSensorStatus(
    val id: String,
    val label: String,
    val gpio: Int,
    val rawAdc: Int = 0,
    val voltageV: Float = 0f,
    val isDigitalHigh: Boolean = false
)

data class LiveI2cScanStatus(
    val devices: List<String> = emptyList(),
    val isScanning: Boolean = false,
    val lastScanTimestamp: Long = 0L
)

