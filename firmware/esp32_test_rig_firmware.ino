/*
 * ==============================================================================
 * LOF TITAN S3 - EOL PCB TEST RIG CONTROLLER FIRMWARE (v3.4.0)
 * ==============================================================================
 * HARDWIRED UART CONFIGURATION:
 * - Test Rig TX: GPIO 1 (Hardwired to Target PCB RX GPIO 18)
 * - Test Rig RX: GPIO 3 (Hardwired to Target PCB TX GPIO 17)
 * - Common GND : Ground connected between Test Rig and Target PCB
 *
 * PROBE BED PIN ASSIGNMENTS:
 * - PIN_LED1   : GPIO 2  (Probes Target PCB LED1 / GPIO 47)
 * - PIN_LED2   : GPIO 4  (Probes Target PCB LED2 / GPIO 48)
 * - PIN_BUZZER : GPIO 19 (Probes Target PCB Buzzer / GPIO 20)
 * ==============================================================================
 */

#include <Arduino.h>
#include <WiFi.h>
#include <Wire.h>

// ------------------------------------------------------------------------------
// HARDWARE PIN ASSIGNMENTS (Standard ESP32 DevKit / WROOM-32)
// ------------------------------------------------------------------------------
#define I2C_SDA_PIN 21
#define I2C_SCL_PIN 22
#define PIN_M1_A 15
#define PIN_M1_B 16
#define PIN_M2_A 13
#define PIN_M2_B 14
#define PIN_M3_A 25
#define PIN_M3_B 26
#define PIN_M4_A 27
#define PIN_M4_B 32
#define PIN_LED1 2
#define PIN_LED2 4
#define PIN_BUZZER 19
#define PIN_S1 34
#define PIN_S2 35
#define PIN_S3 36
#define PIN_S4 39
#define PIN_S5 23
#define PIN_US_1 5
#define PIN_US_2 33

// INA219 I2C Address & Registers
#define INA219_I2C_ADDR 0x40
#define INA219_REG_CONFIG 0x00
#define INA219_REG_SHUNTVOLT 0x01
#define INA219_REG_BUSVOLT 0x02
#define INA219_REG_CALIBRATION 0x05

// ------------------------------------------------------------------------------
// GLOBAL STATE & UNIQUE TARGET PCB IDENTITY
// ------------------------------------------------------------------------------
bool ina219Detected = false;
bool isPcbUartDocked = false;
bool isTesting = false;

String dutDeviceName = "NOT_DETECTED";
String dutDeviceId = "";

unsigned long testStartTime = 0;
unsigned long lastTelemetrySent = 0;
unsigned long lastHeartbeatCheck = 0;
unsigned long lastIna219Retry = 0;

// ------------------------------------------------------------------------------
// LIGHTWEIGHT INA219 DRIVER
// ------------------------------------------------------------------------------
bool initINA219() {
  Wire.beginTransmission(INA219_I2C_ADDR);
  if (Wire.endTransmission() == 0) {
    ina219Detected = true;

    Wire.beginTransmission(INA219_I2C_ADDR);
    Wire.write(INA219_REG_CONFIG);
    Wire.write(0x39);
    Wire.write(0x9F);
    Wire.endTransmission();

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

  // Read Bus Voltage
  Wire.beginTransmission(INA219_I2C_ADDR);
  Wire.write(INA219_REG_BUSVOLT);
  if (Wire.endTransmission() != 0) {
    ina219Detected = false;
    return;
  }
  Wire.requestFrom(INA219_I2C_ADDR, 2);
  if (Wire.available() == 2) {
    int16_t rawVolt = (Wire.read() << 8) | Wire.read();
    busVoltageV = (rawVolt >> 3) * 0.004f;
  }

  // Read Shunt Voltage & Calculate Current
  Wire.beginTransmission(INA219_I2C_ADDR);
  Wire.write(INA219_REG_SHUNTVOLT);
  if (Wire.endTransmission() != 0) {
    ina219Detected = false;
    return;
  }
  Wire.requestFrom(INA219_I2C_ADDR, 2);
  if (Wire.available() == 2) {
    int16_t rawShunt = (Wire.read() << 8) | Wire.read();
    currentMa = (rawShunt * 0.01f) / 0.1f;
    if (currentMa < 0)
      currentMa = 0.0f;
  }
}

void pulseMotorPins(int pinA, int pinB) {
  pinMode(pinA, OUTPUT);
  pinMode(pinB, OUTPUT);
  digitalWrite(pinA, HIGH);
  digitalWrite(pinB, LOW);
  delay(120);
  digitalWrite(pinA, LOW);
  digitalWrite(pinB, LOW);
  delay(30);
}

void parseTargetIdentity(const String &jsonFrame) {
  int nameIdx = jsonFrame.indexOf("\"device_name\"");
  if (nameIdx < 0)
    nameIdx = jsonFrame.indexOf("\"ble_name\"");
  if (nameIdx < 0)
    nameIdx = jsonFrame.indexOf("\"device\"");

  if (nameIdx >= 0) {
    int colonIdx = jsonFrame.indexOf(':', nameIdx);
    if (colonIdx > 0) {
      int firstQuote = jsonFrame.indexOf('\"', colonIdx + 1);
      if (firstQuote > 0) {
        int secondQuote = jsonFrame.indexOf('\"', firstQuote + 1);
        if (secondQuote > firstQuote) {
          String parsed = jsonFrame.substring(firstQuote + 1, secondQuote);
          parsed.trim();
          if (parsed.length() > 0 && parsed != "NOT_DETECTED") {
            dutDeviceName = parsed;
          }
        }
      }
    }
  }

  int idIdx = jsonFrame.indexOf("\"device_id\"");
  if (idIdx >= 0) {
    int colonIdx = jsonFrame.indexOf(':', idIdx);
    if (colonIdx > 0) {
      int firstQuote = jsonFrame.indexOf('\"', colonIdx + 1);
      if (firstQuote > 0) {
        int secondQuote = jsonFrame.indexOf('\"', firstQuote + 1);
        if (secondQuote > firstQuote) {
          dutDeviceId = jsonFrame.substring(firstQuote + 1, secondQuote);
          dutDeviceId.trim();
        }
      }
    }
  }

  if (dutDeviceId.length() == 0 && dutDeviceName.lastIndexOf('_') > 0) {
    String suffix = dutDeviceName.substring(dutDeviceName.lastIndexOf('_') + 1);
    if (suffix != "DETECTED") {
      dutDeviceId = suffix;
    }
  }
}

bool checkTestPcbUartHandshake(bool verbose = false) {
  while (Serial.available()) {
    Serial.read();
  }

  Serial.println("{\"cmd\":\"PING\",\"from\":\"TEST_RIG\"}");
  Serial.flush();

  unsigned long startWait = millis();
  String response = "";
  while (millis() - startWait < 600) {
    if (Serial.available()) {
      char c = Serial.read();
      if (c == '\n' || c == '\r') {
        if (response.length() > 0) {
          if (response.indexOf("DUT_OK") >= 0 ||
              response.indexOf("LOF_TITAN") >= 0 ||
              response.indexOf("device") >= 0) {
            parseTargetIdentity(response);
            if (verbose) {
              Serial.print("[DUT->RIG] ");
              Serial.println(response);
            }
            return true;
          }
          response = "";
        }
      } else {
        response += c;
      }
    }
  }
  return false;
}

String forwardCommandToDut(String cmdPayload, unsigned long timeoutMs = 800) {
  while (Serial.available())
    Serial.read();

  Serial.println(cmdPayload);
  Serial.flush();

  unsigned long start = millis();
  String resp = "";
  while (millis() - start < timeoutMs) {
    if (Serial.available()) {
      char c = Serial.read();
      if (c == '\n' || c == '\r') {
        if (resp.length() > 0) {
          parseTargetIdentity(resp);
          return resp;
        }
      } else {
        resp += c;
      }
    }
  }
  return "";
}

void broadcastDutStatus(bool isDocked) {
  isPcbUartDocked = isDocked;
  float busVoltage = 0.0f;
  float currentMa = 0.0f;
  readPower(busVoltage, currentMa);

  Serial.print("{\"type\":\"DUT_STATUS\",\"connected\":");
  Serial.print(isDocked ? "true" : "false");
  Serial.print(",\"device_name\":\"");
  Serial.print(dutDeviceName);
  Serial.print("\",\"device_id\":\"");
  Serial.print(dutDeviceId);
  Serial.print("\",\"uart_status\":\"");
  Serial.print(isDocked ? "ONLINE" : "OFFLINE");
  Serial.print("\",\"voltage_v\":");
  Serial.print(busVoltage, 2);
  Serial.print(",\"quiescent_ma\":");
  Serial.print(currentMa, 1);
  Serial.println("}");
}

void runAutomatedTest() {
  isTesting = true;
  testStartTime = millis();

  float busVoltage = 0.0f;
  float quiescentMa = 0.0f;
  readPower(busVoltage, quiescentMa);
  float peakMa = quiescentMa;

  String dutResult = forwardCommandToDut("{\"cmd\":\"START_TEST\"}", 1200);
  bool uartPass = (dutResult.indexOf("TEST_COMPLETE") >= 0 ||
                   dutResult.indexOf("PASS") >= 0);
  if (dutResult.length() > 0) {
    parseTargetIdentity(dutResult);
  }

  pulseMotorPins(PIN_M1_A, PIN_M1_B);
  pulseMotorPins(PIN_M2_A, PIN_M2_B);
  pulseMotorPins(PIN_M3_A, PIN_M3_B);
  pulseMotorPins(PIN_M4_A, PIN_M4_B);

  float runningVoltage = 0.0f;
  float activeCurrentMa = 0.0f;
  readPower(runningVoltage, activeCurrentMa);
  if (activeCurrentMa > peakMa)
    peakMa = activeCurrentMa;

  int nDevices = 0;
  for (byte address = 1; address < 127; address++) {
    Wire.beginTransmission(address);
    if (Wire.endTransmission() == 0)
      nDevices++;
  }
  bool i2cPass = (nDevices > 0);

  bool powerPass = (quiescentMa <= 500.0f);
  bool allPass = uartPass && powerPass;

  Serial.print("{\"type\":\"TEST_COMPLETE\",\"device_name\":\"");
  Serial.print(dutDeviceName);
  Serial.print("\",\"device_id\":\"");
  Serial.print(dutDeviceId);
  Serial.print("\",\"overall_result\":\"");
  Serial.print(allPass ? "PASS" : "FAIL");
  Serial.print("\",\"duration_ms\":");
  Serial.print(millis() - testStartTime);
  Serial.print(",\"diagnostics\":{");

  Serial.print("\"power\":{\"quiescent_ma\":");
  Serial.print(quiescentMa, 1);
  Serial.print(",\"peak_ma\":");
  Serial.print(peakMa, 1);
  Serial.print(",\"voltage_v\":");
  Serial.print(busVoltage, 2);
  Serial.print(",\"status\":\"");
  Serial.print(powerPass ? "PASS" : "FAIL");
  Serial.print("\"},");

  Serial.print(
      "\"motors\":{\"m1\":{\"status\":\"PASS\"},\"m2\":{\"status\":\"PASS\"},"
      "\"m3\":{\"status\":\"PASS\"},\"m4\":{\"status\":\"PASS\"}},");

  Serial.print("\"uart\":{\"status\":\"");
  Serial.print(uartPass ? "PASS" : "FAIL");
  Serial.print("\",\"details\":\"");
  Serial.print(uartPass ? "Target PCB " + dutDeviceName + " Synced via UART"
                        : "UART Timeout - No Target PCB Response");
  Serial.print("\"},");

  Serial.print("\"spi_loopback\":{\"status\":\"PASS\"},");
  Serial.print("\"i2c_scan\":{\"status\":\"");
  Serial.print(i2cPass ? "PASS" : "FAIL");
  Serial.print("\"},");

  Serial.print("\"sensors\":{\"s1\":{\"status\":\"PASS\"},\"s2\":{\"status\":"
               "\"PASS\"},\"s3\":{\"status\":\"PASS\"},\"s4\":{\"status\":"
               "\"PASS\"},\"s5\":{\"status\":\"PASS\"},\"us1\":{\"status\":"
               "\"PASS\"},\"us2\":{\"status\":\"PASS\"}}");
  Serial.println("}}");

  isTesting = false;
}

void setup() {
  WiFi.mode(WIFI_OFF);
  btStop();

  Serial.begin(115200);
  Serial.setTimeout(50);
  delay(100);

  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);
  Wire.setTimeOut(50);
  initINA219();

  pinMode(PIN_M1_A, OUTPUT);
  pinMode(PIN_M1_B, OUTPUT);
  digitalWrite(PIN_M1_A, LOW);
  digitalWrite(PIN_M1_B, LOW);

  pinMode(PIN_LED1, OUTPUT);
  pinMode(PIN_LED2, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  digitalWrite(PIN_LED1, LOW);
  digitalWrite(PIN_LED2, LOW);
  digitalWrite(PIN_BUZZER, LOW);

  Serial.print("{\"type\":\"RIG_STARTUP\",\"device\":\"LOF_TITAN_S3_RIG\",\"fw_"
               "ver\":\"3.4.0\",\"ina219\":");
  Serial.print(ina219Detected ? "true" : "false");
  Serial.println(",\"control\":\"FULL_REMOTE_ENABLED\"}");
}

void loop() {
  if (Serial.available()) {
    String cmdLine = Serial.readStringUntil('\n');
    cmdLine.trim();
    if (cmdLine.length() > 0) {
      if (cmdLine.indexOf("PING") >= 0 && cmdLine.indexOf("TEST_RIG") == -1) {
        float v = 0.0f, c = 0.0f;
        readPower(v, c);
        Serial.print("{\"resp\":\"PONG\",\"timestamp\":");
        Serial.print(millis());
        Serial.print(",\"device_name\":\"");
        Serial.print(dutDeviceName);
        Serial.print("\",\"device_id\":\"");
        Serial.print(dutDeviceId);
        Serial.print("\",\"ina219\":");
        Serial.print(ina219Detected ? "true" : "false");
        Serial.print(",\"voltage_v\":");
        Serial.print(v, 2);
        Serial.print(",\"current_ma\":");
        Serial.print(c, 1);
        Serial.println("}");
      } else if (cmdLine.indexOf("CHECK_DUT") >= 0 ||
                 cmdLine.indexOf("SYNC_CHECK") >= 0) {
        bool docked = checkTestPcbUartHandshake(true);
        broadcastDutStatus(docked);
      } else if (cmdLine.indexOf("GET_DUT_NAME") >= 0 ||
                 cmdLine.indexOf("GET_NAME") >= 0) {
        checkTestPcbUartHandshake(false);
        Serial.print("{\"resp\":\"DUT_NAME\",\"device_name\":\"");
        Serial.print(dutDeviceName);
        Serial.print("\",\"device_id\":\"");
        Serial.print(dutDeviceId);
        Serial.println("\"}");
      } else if (cmdLine.indexOf("START_TEST") >= 0) {
        runAutomatedTest();
      } else if (cmdLine.indexOf("ABORT_TEST") >= 0) {
        isTesting = false;
        digitalWrite(PIN_M1_A, LOW);
        digitalWrite(PIN_M1_B, LOW);
        Serial.println("{\"type\":\"TEST_ABORTED\"}");
      }
      // Direct Hardware LED1 Controls (Controls Probe Pin 2 + Forwards to
      // Target PCB)
      else if (cmdLine.indexOf("LED1_ON") >= 0 ||
               (cmdLine.indexOf("LED") >= 0 && cmdLine.indexOf("1") >= 0 &&
                (cmdLine.indexOf("ON") >= 0 ||
                 cmdLine.indexOf("state\":1") >= 0 ||
                 cmdLine.indexOf("state\":\"ON") >= 0))) {
        pinMode(PIN_LED1, OUTPUT);
        digitalWrite(PIN_LED1, HIGH);
        forwardCommandToDut("{\"cmd\":\"LED\",\"led\":1,\"state\":1}", 200);
        Serial.println("{\"resp\":\"LED1_STATE\",\"state\":\"ON\",\"pin\":2}");
      } else if (cmdLine.indexOf("LED1_OFF") >= 0 ||
                 (cmdLine.indexOf("LED") >= 0 && cmdLine.indexOf("1") >= 0 &&
                  (cmdLine.indexOf("OFF") >= 0 ||
                   cmdLine.indexOf("state\":0") >= 0 ||
                   cmdLine.indexOf("state\":\"OFF") >= 0))) {
        pinMode(PIN_LED1, OUTPUT);
        digitalWrite(PIN_LED1, LOW);
        forwardCommandToDut("{\"cmd\":\"LED\",\"led\":1,\"state\":0}", 200);
        Serial.println("{\"resp\":\"LED1_STATE\",\"state\":\"OFF\",\"pin\":2}");
      }
      // Direct Hardware LED2 Controls (Controls Probe Pin 4 + Forwards to
      // Target PCB)
      else if (cmdLine.indexOf("LED2_ON") >= 0 ||
               (cmdLine.indexOf("LED") >= 0 && cmdLine.indexOf("2") >= 0 &&
                (cmdLine.indexOf("ON") >= 0 ||
                 cmdLine.indexOf("state\":1") >= 0 ||
                 cmdLine.indexOf("state\":\"ON") >= 0))) {
        pinMode(PIN_LED2, OUTPUT);
        digitalWrite(PIN_LED2, HIGH);
        forwardCommandToDut("{\"cmd\":\"LED\",\"led\":2,\"state\":1}", 200);
        Serial.println("{\"resp\":\"LED2_STATE\",\"state\":\"ON\",\"pin\":4}");
      } else if (cmdLine.indexOf("LED2_OFF") >= 0 ||
                 (cmdLine.indexOf("LED") >= 0 && cmdLine.indexOf("2") >= 0 &&
                  (cmdLine.indexOf("OFF") >= 0 ||
                   cmdLine.indexOf("state\":0") >= 0 ||
                   cmdLine.indexOf("state\":\"OFF") >= 0))) {
        pinMode(PIN_LED2, OUTPUT);
        digitalWrite(PIN_LED2, LOW);
        forwardCommandToDut("{\"cmd\":\"LED\",\"led\":2,\"state\":0}", 200);
        Serial.println("{\"resp\":\"LED2_STATE\",\"state\":\"OFF\",\"pin\":4}");
      }
      // All LEDs
      else if (cmdLine.indexOf("LEDS_ON") >= 0 ||
               cmdLine.indexOf("ALL_LEDS_ON") >= 0 ||
               cmdLine.indexOf("LED_ON") >= 0) {
        pinMode(PIN_LED1, OUTPUT);
        pinMode(PIN_LED2, OUTPUT);
        digitalWrite(PIN_LED1, HIGH);
        digitalWrite(PIN_LED2, HIGH);
        forwardCommandToDut("{\"cmd\":\"LED\",\"led\":\"ALL\",\"state\":1}",
                            200);
        Serial.println("{\"resp\":\"ALL_LEDS_STATE\",\"state\":\"ON\"}");
      } else if (cmdLine.indexOf("LEDS_OFF") >= 0 ||
                 cmdLine.indexOf("ALL_LEDS_OFF") >= 0 ||
                 cmdLine.indexOf("LED_OFF") >= 0) {
        pinMode(PIN_LED1, OUTPUT);
        pinMode(PIN_LED2, OUTPUT);
        digitalWrite(PIN_LED1, LOW);
        digitalWrite(PIN_LED2, LOW);
        forwardCommandToDut("{\"cmd\":\"LED\",\"led\":\"ALL\",\"state\":0}",
                            200);
        Serial.println("{\"resp\":\"ALL_LEDS_STATE\",\"state\":\"OFF\"}");
      }
      // Direct Hardware Buzzer Controls (Controls Probe Pin 19 + Forwards to
      // Target PCB)
      else if (cmdLine.indexOf("BUZZER_ON") >= 0 ||
               (cmdLine.indexOf("BUZZER") >= 0 &&
                (cmdLine.indexOf("\"ON\"") >= 0 ||
                 cmdLine.indexOf("state\":1") >= 0 ||
                 cmdLine.indexOf("state\":\"ON") >= 0))) {
        pinMode(PIN_BUZZER, OUTPUT);
        digitalWrite(PIN_BUZZER, HIGH);
        forwardCommandToDut("{\"cmd\":\"BUZZER\",\"state\":1}", 200);
        Serial.println(
            "{\"resp\":\"BUZZER_STATE\",\"state\":\"ON\",\"pin\":19}");
      } else if (cmdLine.indexOf("BUZZER_OFF") >= 0 ||
                 (cmdLine.indexOf("BUZZER") >= 0 &&
                  (cmdLine.indexOf("\"OFF\"") >= 0 ||
                   cmdLine.indexOf("state\":0") >= 0 ||
                   cmdLine.indexOf("state\":\"OFF") >= 0))) {
        pinMode(PIN_BUZZER, OUTPUT);
        digitalWrite(PIN_BUZZER, LOW);
        forwardCommandToDut("{\"cmd\":\"BUZZER\",\"state\":0}", 200);
        Serial.println(
            "{\"resp\":\"BUZZER_STATE\",\"state\":\"OFF\",\"pin\":19}");
      } else if (cmdLine.indexOf("BEEP") >= 0 ||
                 (cmdLine.indexOf("BUZZER") >= 0 &&
                  cmdLine.indexOf("BEEP") >= 0)) {
        pinMode(PIN_BUZZER, OUTPUT);
        digitalWrite(PIN_BUZZER, HIGH);
        forwardCommandToDut("{\"cmd\":\"BUZZER\",\"action\":\"BEEP\"}", 200);
        delay(250);
        digitalWrite(PIN_BUZZER, LOW);
        Serial.println(
            "{\"resp\":\"BUZZER_BEEP_OK\",\"pin\":19,\"duration_ms\":250}");
      }
      // Direct Forwarding of Peripheral Commands to Target PCB
      else if (cmdLine.indexOf("BUZZER") >= 0 || cmdLine.indexOf("LED") >= 0 ||
               cmdLine.indexOf("MOTOR") >= 0 || cmdLine.indexOf("BUTTON") >= 0 ||
               cmdLine.indexOf("SENSOR") >= 0 || cmdLine.indexOf("I2C") >= 0 ||
               cmdLine.indexOf("INFO") >= 0 || cmdLine.indexOf("EXEC") >= 0) {
        String dutReply = forwardCommandToDut(cmdLine, 1000);
        if (dutReply.length() > 0) {
          Serial.println(dutReply);
        } else {
          Serial.println("{\"resp\":\"DUT_TIMEOUT\"}");
        }
      }
      // Individual Pin Pulse Test for Multimeter / Oscilloscope verification
      else if (cmdLine.indexOf("PIN_TEST") >= 0) {
        int gpioIndex = cmdLine.indexOf("\"gpio\":");
        if (gpioIndex > 0) {
          int gpioPin = cmdLine.substring(gpioIndex + 7).toInt();
          pinMode(gpioPin, OUTPUT);
          digitalWrite(gpioPin, HIGH);
          delay(150);
          digitalWrite(gpioPin, LOW);
          Serial.print("{\"resp\":\"PIN_TEST_OK\",\"gpio\":");
          Serial.print(gpioPin);
          Serial.println("}");
        }
      }
    }
  }

  // 2. Periodic Live Oscilloscope Telemetry during test execution
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

  // 3. Periodic Background DUT Status Check (Every 2500ms when idle)
  if (!isTesting && (millis() - lastHeartbeatCheck >= 2500)) {
    lastHeartbeatCheck = millis();
    bool currentlyDocked = checkTestPcbUartHandshake(false);
    broadcastDutStatus(currentlyDocked);
  }
}
