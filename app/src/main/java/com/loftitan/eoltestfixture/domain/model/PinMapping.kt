package com.loftitan.eoltestfixture.domain.model

/**
 * Exact hardware pin layout mapping for the LOF Titan S3 PCB under test.
 * As commanded by the ESP32 Test Rig.
 */
object PinMapping {

    // 1. Communication Ports
    object UART {
        const val TX_GPIO = 17 // Test Rig ESP32 TX -> Test PCB RX
        const val RX_GPIO = 18 // Test Rig ESP32 RX <- Test PCB TX
        const val BAUD_RATE = 115200
        const val DESCRIPTION = "Hardwired UART: Rig ESP32 (TX:17, RX:18) <-> Test PCB (RX, TX)"
    }

    object SPI {
        const val SCK_GPIO = 35
        const val MOSI_GPIO = 36
        const val MISO_GPIO = 37
        const val CS_GPIO = 38
        const val DESCRIPTION = "SCK (35), MOSI (36), MISO (37), CS (38)"
    }

    object I2C {
        const val SCL_GPIO = 8
        const val SDA_GPIO = 7
        const val DESCRIPTION = "SCL (GPIO 8), SDA (GPIO 7) Bus Scan"
    }

    // 2. Motor Drivers M1 - M4
    object Motors {
        val M1_GPIOS = listOf(15, 16)
        val M2_GPIOS = listOf(13, 14)
        val M3_GPIOS = listOf(11, 12)
        val M4_GPIOS = listOf(9, 10)

        fun getAllDrivers(): Map<String, List<Int>> = mapOf(
            "M1" to M1_GPIOS,
            "M2" to M2_GPIOS,
            "M3" to M3_GPIOS,
            "M4" to M4_GPIOS
        )
    }

    // 3. Indicators
    object Indicators {
        const val LED1_GPIO = 47
        const val LED2_GPIO = 48
        const val BUZZER_GPIO = 20
    }

    // 4. Sensor Ports (S1 - S5 & Ultrasonic)
    object Sensors {
        const val S1_GPIO = 2
        const val S2_GPIO = 1
        const val S3_GPIO = 3
        const val S4_GPIO = 4
        const val S5_GPIO = 5
        const val ULTRASONIC_S1_GPIO = 6
        const val ULTRASONIC_S2_GPIO = 19

        fun getAllSensors(): Map<String, Int> = mapOf(
            "S1" to S1_GPIO,
            "S2" to S2_GPIO,
            "S3" to S3_GPIO,
            "S4" to S4_GPIO,
            "S5" to S5_GPIO,
            "Ultrasonic S1" to ULTRASONIC_S1_GPIO,
            "Ultrasonic S2" to ULTRASONIC_S2_GPIO
        )
    }

    // 5. Default Pass/Fail Electrical Thresholds
    object Thresholds {
        const val MAX_QUIESCENT_CURRENT_MA = 50.0f
        const val MIN_OPERATING_VOLTAGE_V = 11.4f
        const val MAX_OPERATING_VOLTAGE_V = 12.6f
        const val PEAK_CURRENT_LIMIT_MA = 1500.0f
    }
}
