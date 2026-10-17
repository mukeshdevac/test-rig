/*
 * ==============================================================================
 * LOF TITAN S3 - TARGET TEST PCB COMPANION FIRMWARE (SAMPLE)
 * ==============================================================================
 * Upload this sketch to the Target PCB under test (or a 2nd ESP32 acting as the DUT).
 * 
 * HARDWIRED UART CONNECTIONS (Link 2):
 * - Target PCB RX (e.g. GPIO 16 on standard ESP32, or RX pin on MCU) <- Test Rig TX (GPIO 17)
 * - Target PCB TX (e.g. GPIO 17 on standard ESP32, or TX pin on MCU) -> Test Rig RX (GPIO 18)
 * - Common GND connected between both boards
 * 
 * BEHAVIOR:
 * - Listens for incoming commands from the Test Rig:
 *     {"cmd":"PING","from":"TEST_RIG"}
 * - Responds immediately with:
 *     {"resp":"PONG","status":"DUT_OK","device":"LOF_TITAN_S3_DUT"}
 * ==============================================================================
 */

#include <Arduino.h>

// If using a 2nd ESP32 as DUT, use Serial2 or Serial (depending on your board setup)
#define DUT_UART_BAUD 115200

// Define onboard LED pin (many ESP32 board packages do not define LED_BUILTIN)
#ifndef LED_BUILTIN
  #define LED_BUILTIN 2 // Standard ESP32 onboard blue LED is on GPIO 2
#endif

#if defined(ESP32)
  HardwareSerial RigUart(2);
  #define DUT_RX_PIN 16 // DUT RX <- Test Rig TX (17)
  #define DUT_TX_PIN 17 // DUT TX -> Test Rig RX (18)
#endif

void setup() {
  Serial.begin(DUT_UART_BAUD);

#if defined(ESP32)
  RigUart.begin(DUT_UART_BAUD, SERIAL_8N1, DUT_RX_PIN, DUT_TX_PIN);
#endif

  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, LOW);
}

void processIncomingCommand(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) return;

  if (cmd.indexOf("PING") >= 0) {
    // Flash LED on handshake
    digitalWrite(LED_BUILTIN, HIGH);

    String pongReply = "{\"resp\":\"PONG\",\"status\":\"DUT_OK\",\"device\":\"LOF_TITAN_S3_DUT\"}";
    Serial.println(pongReply);
#if defined(ESP32)
    RigUart.println(pongReply);
#endif

    delay(20);
    digitalWrite(LED_BUILTIN, LOW);
  }
}

void loop() {
  // Listen on USB/Primary Serial
  if (Serial.available()) {
    String cmd = Serial.readStringUntil('\n');
    processIncomingCommand(cmd);
  }

#if defined(ESP32)
  // Listen on Hardware Serial2 (connected to Test Rig)
  if (RigUart.available()) {
    String cmd = RigUart.readStringUntil('\n');
    processIncomingCommand(cmd);
  }
#endif
}
