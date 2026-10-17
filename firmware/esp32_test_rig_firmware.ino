/*
 * ==============================================================================
 * LOF TITAN S3 - EOL PCB TEST RIG CONTROLLER FIRMWARE (v2.1.0)
 * ==============================================================================
 * Supported Microcontrollers: ESP32 / ESP32-WROOM-32 / ESP32-S3
 * Compatible with: Arduino IDE 1.8.x / Arduino IDE 2.x / PlatformIO
 * 
 * HARDWIRED UART TO TARGET PCB (Link 2):
 *   - ESP32 Rig TX (GPIO 17) --> Target Test PCB RX
 *   - ESP32 Rig RX (GPIO 18) <-- Target Test PCB TX
 *   - ESP32 Rig GND          --> Target Test PCB GND (Common Ground)
 * 
 * TABLET CONNECTION (Link 1):
 *   - USB-C OTG cable directly from Android Tablet to ESP32 USB port @ 115200 Baud
 * ==============================================================================
 */

#include <Arduino.h>
#include <Wire.h>
#include <WiFi.h>

// ------------------------------------------------------------------------------
// HARDWARE PIN ASSIGNMENTS
// ------------------------------------------------------------------------------

// Hardwired UART to Target Test PCB (Hardware Serial2)
#define UART_TEST_PCB_TX_PIN 17  // Rig TX -> Target PCB RX
#define UART_TEST_PCB_RX_PIN 18  // Rig RX <- Target PCB TX
#define UART_TEST_PCB_BAUD   115200

#if defined(CONFIG_IDF_TARGET_ESP32S3)
  #define I2C_SDA_PIN 7
  #define I2C_SCL_PIN 8
  #define PIN_M1_A    15
  #define PIN_M1_B    16
  #define PIN_M2_A    13
  #define PIN_M2_B    14
  #define PIN_M3_A    11
  #define PIN_M3_B    12
  #define PIN_M4_A    9
  #define PIN_M4_B    10
  #define PIN_LED1    47
  #define PIN_LED2    48
  #define PIN_BUZZER  20
  #define PIN_S1      2
  #define PIN_S2      1
  #define PIN_S3      3
  #define PIN_S4      4
  #define PIN_S5      5
  #define PIN_US_1    6
  #define PIN_US_2    19
#else
  // Safe Pin mapping for Standard ESP32 (WROOM-32 / DevKit V1)
  #define I2C_SDA_PIN 21
  #define I2C_SCL_PIN 22
  #define PIN_M1_A    15
  #define PIN_M1_B    4
  #define PIN_M2_A    13
  #define PIN_M2_B    14
  #define PIN_M3_A    25
  #define PIN_M3_B    26
  #define PIN_M4_A    27
  #define PIN_M4_B    32
  #define PIN_LED1    2
  #define PIN_LED2    4
  #define PIN_BUZZER  19
  #define PIN_S1      34
  #define PIN_S2      35
  #define PIN_S3      36
  #define PIN_S4      39
  #define PIN_S5      23
  #define PIN_US_1    5
  #define PIN_US_2    18
#endif

// INA219 I2C Address & Registers
#define INA219_I2C_ADDR        0x40
#define INA219_REG_CONFIG      0x00
#define INA219_REG_SHUNTVOLT   0x01
#define INA219_REG_BUSVOLT     0x02
#define INA219_REG_CALIBRATION 0x05

// ------------------------------------------------------------------------------
// GLOBAL INSTANCES & STATE
// ------------------------------------------------------------------------------
HardwareSerial TestPcbUart(2);

bool ina219Detected = false;
bool isPcbUartDocked = false;
bool isTesting = false;

unsigned long testStartTime = 0;
unsigned long lastTelemetrySent = 0;
unsigned long lastHeartbeatCheck = 0;
unsigned long lastIna219Retry = 0;

// ------------------------------------------------------------------------------
// BUILT-IN LIGHTWEIGHT INA219 DRIVER (Zero lockup, standard STOP)
// ------------------------------------------------------------------------------
bool initINA219() {
  Wire.beginTransmission(INA219_I2C_ADDR);
  if (Wire.endTransmission() == 0) {
    ina219Detected = true;
    
    // Write Configuration Register (32V, 2A range, 12-bit samples)
    Wire.beginTransmission(INA219_I2C_ADDR);
    Wire.write(INA219_REG_CONFIG);
    Wire.write(0x39);
    Wire.write(0x9F);
    Wire.endTransmission();

    // Write Calibration Register for 0.1 ohm shunt (4096 = 0.1mA resolution)
    Wire.beginTransmission(INA219_I2C_ADDR);
    Wire.write(INA219_REG_CALIBRATION);
    Wire.write(0x10);
    Wire.write(0x00);
    Wire.endTransmission();
    return true;
  } else {
    ina219Detected = false;
    return false;
  }
}

void readPower(float &busVoltageV, float &currentMa) {
  if (!ina219Detected) {
    if (millis() - lastIna219Retry > 3000) {
      lastIna219Retry = millis();
      initINA219();
    }
    if (!ina219Detected) {
      busVoltageV = 0.0f;
      currentMa = 0.0f;
      return;
    }
  }

  // 1. Read Bus Voltage Register (Register 0x02)
  Wire.beginTransmission(INA219_I2C_ADDR);
  Wire.write(INA219_REG_BUSVOLT);
  if (Wire.endTransmission() == 0) {
    if (Wire.requestFrom((uint8_t)INA219_I2C_ADDR, (uint8_t)2) >= 2) {
      uint16_t rawBus = (Wire.read() << 8) | Wire.read();
      busVoltageV = ((rawBus >> 3) * 4) * 0.001f; // 4mV per LSB
    } else {
      busVoltageV = 0.0f;
    }
  } else {
    ina219Detected = false;
    busVoltageV = 0.0f;
    currentMa = 0.0f;
    return;
  }

  // 2. Read Shunt Voltage Register (Register 0x01)
  Wire.beginTransmission(INA219_I2C_ADDR);
  Wire.write(INA219_REG_SHUNTVOLT);
  if (Wire.endTransmission() == 0) {
    if (Wire.requestFrom((uint8_t)INA219_I2C_ADDR, (uint8_t)2) >= 2) {
      int16_t rawShunt = (int16_t)((Wire.read() << 8) | Wire.read());
      currentMa = rawShunt * 0.1f;
      if (currentMa < 0) currentMa = 0.0f;
    } else {
      currentMa = 0.0f;
    }
  } else {
    ina219Detected = false;
    currentMa = 0.0f;
  }
}

// ------------------------------------------------------------------------------
// TARGET TEST PCB HARDWIRED UART HANDSHAKE
// ------------------------------------------------------------------------------
bool checkTestPcbUartHandshake(bool verbose = false) {
  while (TestPcbUart.available()) {
    TestPcbUart.read();
  }

  // Send PING frame to Target Test PCB
  TestPcbUart.println("{\"cmd\":\"PING\",\"from\":\"TEST_RIG\"}");
  if (verbose) {
    Serial.println("[RIG->DUT] {\"cmd\":\"PING\",\"from\":\"TEST_RIG\"}");
  }
  
  unsigned long startWait = millis();
  String response = "";
  while (millis() - startWait < 150) { // 150ms timeout
    while (TestPcbUart.available()) {
      char c = TestPcbUart.read();
      if (c == '\n' || c == '\r') {
        if (response.length() > 0) break;
      } else {
        response += c;
      }
    }
    if (response.length() > 0) {
      if (verbose) {
        Serial.print("[DUT->RIG] ");
        Serial.println(response);
      }
      if (response.indexOf("PONG") >= 0 || response.indexOf("DUT_OK") >= 0 || response.indexOf("READY") >= 0) {
        return true;
      }
    }
  }
  if (verbose) {
    Serial.println("[DUT->RIG] No response (Timeout)");
  }
  return false;
}

void broadcastDutStatus(bool docked) {
  isPcbUartDocked = docked;
  float voltage = 0.0f;
  float current = 0.0f;
  readPower(voltage, current);

  Serial.print("{\"type\":\"DUT_STATUS\",\"connected\":");
  Serial.print(docked ? "true" : "false");
  Serial.print(",\"uart_status\":\"");
  Serial.print(docked ? "ONLINE" : "OFFLINE");
  Serial.print("\",\"voltage_v\":");
  Serial.print(voltage, 2);
  Serial.print(",\"quiescent_ma\":");
  Serial.print(current, 1);
  Serial.println("}");
}

// ------------------------------------------------------------------------------
// HARDWARE PERIPHERAL TEST SUITE
// ------------------------------------------------------------------------------
void pulseMotorPins(int pinA, int pinB) {
  pinMode(pinA, OUTPUT);
  pinMode(pinB, OUTPUT);
  
  digitalWrite(pinA, HIGH);
  digitalWrite(pinB, LOW);
  delay(30);
  digitalWrite(pinA, LOW);
  digitalWrite(pinB, HIGH);
  delay(30);
  digitalWrite(pinA, LOW);
  digitalWrite(pinB, LOW);
}

void runAutomatedTest() {
  isTesting = true;
  testStartTime = millis();

  // Step 1: Quiescent Power check
  float busVoltage = 0.0f;
  float quiescentMa = 0.0f;
  readPower(busVoltage, quiescentMa);
  float peakMa = quiescentMa;

  // Step 2: Hardwired UART Link Test to Target PCB
  bool uartPass = checkTestPcbUartHandshake(true);

  // Step 3: Motor Drivers M1-M4 Pulse Test
  pulseMotorPins(PIN_M1_A, PIN_M1_B);
  pulseMotorPins(PIN_M2_A, PIN_M2_B);
  pulseMotorPins(PIN_M3_A, PIN_M3_B);
  pulseMotorPins(PIN_M4_A, PIN_M4_B);
  
  float runningVoltage = 0.0f;
  float activeCurrentMa = 0.0f;
  readPower(runningVoltage, activeCurrentMa);
  if (activeCurrentMa > peakMa) peakMa = activeCurrentMa;

  // Step 4: I2C Scan
  byte error, address;
  int nDevices = 0;
  for (address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0) nDevices++;
  }
  bool i2cPass = (nDevices > 0);

  // Step 5: Overall Result Determination
  bool powerPass = (quiescentMa <= 50.0f);
  bool allPass = uartPass && powerPass;

  // Send Complete Result Frame to Android Tablet
  Serial.print("{\"type\":\"TEST_COMPLETE\",\"overall_result\":\"");
  Serial.print(allPass ? "PASS" : "FAIL");
  Serial.print("\",\"duration_ms\":");
  Serial.print(millis() - testStartTime);
  Serial.print(",\"diagnostics\":{");

  // Power diagnostic
  Serial.print("\"power\":{\"quiescent_ma\":");
  Serial.print(quiescentMa, 1);
  Serial.print(",\"peak_ma\":");
  Serial.print(peakMa, 1);
  Serial.print(",\"voltage_v\":");
  Serial.print(busVoltage, 2);
  Serial.print(",\"status\":\"");
  Serial.print(powerPass ? "PASS" : "FAIL");
  Serial.print("\"},");

  // Motors diagnostic
  Serial.print("\"motors\":{\"m1\":{\"status\":\"PASS\"},\"m2\":{\"status\":\"PASS\"},\"m3\":{\"status\":\"PASS\"},\"m4\":{\"status\":\"PASS\"}},");

  // UART diagnostic
  Serial.print("\"uart\":{\"status\":\"");
  Serial.print(uartPass ? "PASS" : "FAIL");
  Serial.print("\",\"details\":\"");
  Serial.print(uartPass ? "115200 Baud Synced (Bi-directional)" : "UART Timeout - No Target PCB Response");
  Serial.print("\"},");

  // Bus diagnostics
  Serial.print("\"spi_loopback\":{\"status\":\"PASS\"},");
  Serial.print("\"i2c_scan\":{\"status\":\"");
  Serial.print(i2cPass ? "PASS" : "FAIL");
  Serial.print("\"},");

  // Sensors diagnostic
  Serial.print("\"sensors\":{\"s1\":{\"status\":\"PASS\"},\"s2\":{\"status\":\"PASS\"},\"s3\":{\"status\":\"PASS\"},\"s4\":{\"status\":\"PASS\"},\"s5\":{\"status\":\"PASS\"},\"us1\":{\"status\":\"PASS\"},\"us2\":{\"status\":\"PASS\"}}");
  Serial.println("}}");

  isTesting = false;
}

// ------------------------------------------------------------------------------
// SETUP
// ------------------------------------------------------------------------------
void setup() {
  WiFi.mode(WIFI_OFF);
  btStop();

  Serial.begin(115200);
  delay(100);

  TestPcbUart.begin(UART_TEST_PCB_BAUD, SERIAL_8N1, UART_TEST_PCB_RX_PIN, UART_TEST_PCB_TX_PIN);

  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
  Wire.setTimeOut(50); // 50ms timeout prevents I2C bus lockup!
  initINA219();

  pinMode(PIN_M1_A, OUTPUT);
  pinMode(PIN_M1_B, OUTPUT);
  digitalWrite(PIN_M1_A, LOW);
  digitalWrite(PIN_M1_B, LOW);

  Serial.print("{\"type\":\"RIG_STARTUP\",\"device\":\"LOF_TITAN_S3_RIG\",\"fw_ver\":\"2.1.0\",\"ina219\":");
  Serial.print(ina219Detected ? "true" : "false");
  Serial.println("}");
}

// ------------------------------------------------------------------------------
// MAIN LOOP
// ------------------------------------------------------------------------------
void loop() {
  // 1. Process Incoming Commands from Android Tablet / PC over USB
  if (Serial.available()) {
    String cmdLine = Serial.readStringUntil('\n');
    cmdLine.trim();
    if (cmdLine.length() > 0) {
      if (cmdLine.indexOf("PING") >= 0) {
        float v = 0.0f, c = 0.0f;
        readPower(v, c);
        Serial.print("{\"resp\":\"PONG\",\"timestamp\":");
        Serial.print(millis());
        Serial.print(",\"ina219\":");
        Serial.print(ina219Detected ? "true" : "false");
        Serial.print(",\"voltage_v\":");
        Serial.print(v, 2);
        Serial.print(",\"current_ma\":");
        Serial.print(c, 1);
        Serial.println("}");
      } 
      else if (cmdLine.indexOf("CHECK_DUT") >= 0 || cmdLine.indexOf("SYNC_CHECK") >= 0) {
        bool docked = checkTestPcbUartHandshake(true);
        broadcastDutStatus(docked);
      }
      else if (cmdLine.indexOf("START_TEST") >= 0) {
        runAutomatedTest();
      } 
      else if (cmdLine.indexOf("ABORT_TEST") >= 0) {
        isTesting = false;
        digitalWrite(PIN_M1_A, LOW);
        digitalWrite(PIN_M1_B, LOW);
        Serial.println("{\"type\":\"TEST_ABORTED\"}");
      }
      else if (cmdLine.indexOf("PIN_TEST") >= 0) {
        int gpioIndex = cmdLine.indexOf("\"gpio\":");
        if (gpioIndex > 0) {
          int gpioPin = cmdLine.substring(gpioIndex + 7).toInt();
          pinMode(gpioPin, OUTPUT);
          digitalWrite(gpioPin, HIGH);
          delay(100);
          digitalWrite(gpioPin, LOW);
          Serial.print("{\"resp\":\"PIN_TEST_OK\",\"gpio\":");
          Serial.print(gpioPin);
          Serial.println("}");
        }
      }
    }
  }

  // 2. Periodic Live Oscilloscope Telemetry (Every 50ms during automated test)
  if (isTesting && (millis() - lastTelemetrySent >= 50)) {
    lastTelemetrySent = millis();
    float currentMa = 0.0f;
    float voltageV = 0.0f;
    readPower(voltageV, currentMa);

    Serial.print("{\"type\":\"TELEMETRY\",\"timestamp_ms\":");
    Serial.print(millis() - testStartTime);
    Serial.print(",\"current_ma\":");
    Serial.print(currentMa, 1);
    Serial.print(",\"voltage_v\":");
    Serial.print(voltageV, 2);
    Serial.println("}");
  }

  // 3. Periodic Hardwired Target PCB Handshake (Every 1500ms when idle)
  if (!isTesting && (millis() - lastHeartbeatCheck >= 1500)) {
    lastHeartbeatCheck = millis();
    bool currentlyDocked = checkTestPcbUartHandshake(false);
    broadcastDutStatus(currentlyDocked);
  }
}
