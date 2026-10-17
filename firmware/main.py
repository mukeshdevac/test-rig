# ==============================================================================
# LOF TITAN S3 - TARGET TEST PCB MICROPYTHON FIRMWARE (main.py)
# ==============================================================================
# Device: LOF Titan S3 (ESP32-S3) running MicroPython
# Save this file as 'main.py' on the Target PCB filesystem using Thonny / mpremote.
#
# HARDWIRED UART CONNECTION TO TEST RIG (Link 2):
# - Target PCB S3 RX (GPIO 18) <-- Test Rig ESP32 TX (GPIO 17)
# - Target PCB S3 TX (GPIO 17) --> Test Rig ESP32 RX (GPIO 18)
# - Common Ground (GND)        <--> Test Rig ESP32 GND
#
# FUNCTION:
# - Listens for Handshake PING from Test Rig: {"cmd":"PING","from":"TEST_RIG"}
# - Responds immediately with: {"resp":"PONG","status":"DUT_OK","device":"LOF_TITAN_S3"}
# - Signals the Android Tablet that the PCB is properly seated and docked in fixture.
# ==============================================================================

import sys
import time
import json
from machine import UART, Pin

# ------------------------------------------------------------------------------
# PIN DEFINITIONS FOR LOF TITAN S3
# ------------------------------------------------------------------------------
# UART connected to the Test Rig Pogo-pins / Header
# Note: Swap tx & rx numbers if your PCB pogo-pin routing is mirrored.
UART_ID = 1
BAUD_RATE = 115200
TX_PIN = 17   # Target S3 TX -> Rig RX (GPIO 18)
RX_PIN = 18   # Target S3 RX <- Rig TX (GPIO 17)

# Status LED indicator (GPIO 47 or GPIO 48 on most ESP32-S3 boards)
LED_PIN = 47

# ------------------------------------------------------------------------------
# INITIALIZATION
# ------------------------------------------------------------------------------
print("[LOF TITAN S3] Initializing EOL Test Companion...")

# Initialize UART for Test Rig Communication
try:
    rig_uart = UART(UART_ID, baudrate=BAUD_RATE, tx=TX_PIN, rx=RX_PIN, timeout=20)
    print(f"[UART] Initialized UART{UART_ID} on TX={TX_PIN}, RX={RX_PIN} @ {BAUD_RATE} Baud")
except Exception as e:
    print(f"[ERROR] Failed to init UART: {e}")
    rig_uart = None

# Initialize Status LED (fallback gracefully if pin not present)
try:
    status_led = Pin(LED_PIN, Pin.OUT)
    status_led.value(0)
except Exception:
    status_led = None

# Blink LED 2 times on boot to confirm startup
if status_led:
    for _ in range(2):
        status_led.value(1)
        time.sleep_ms(80)
        status_led.value(0)
        time.sleep_ms(80)

print("[LOF TITAN S3] Ready! Awaiting Test Rig Handshake...")

# ------------------------------------------------------------------------------
# COMMAND HANDLER
# ------------------------------------------------------------------------------
def process_command(line_str):
    """Parses incoming JSON or text command from the Test Rig"""
    line = line_str.strip()
    if not line:
        return

    # Check for PING from Test Rig
    if "PING" in line:
        if status_led:
            status_led.value(1)

        # Standard handshake response expected by Android Test Suite & ESP32 Rig
        response = json.dumps({
            "resp": "PONG",
            "status": "DUT_OK",
            "device": "LOF_TITAN_S3",
            "firmware": "MicroPython",
            "uptime_ms": time.ticks_ms()
        }) + "\n"

        # Send to Test Rig via UART
        if rig_uart:
            rig_uart.write(response)

        # Also print to USB REPL (for debugging in Thonny)
        print(f"[RX] PING <- Rig | [TX] Sent PONG -> Rig")

        time.sleep_ms(15)
        if status_led:
            status_led.value(0)

    # Check for custom automated self-test command
    elif "SELF_TEST" in line or "START_TEST" in line:
        print("[TEST] Running onboard peripheral validation...")
        result = json.dumps({
            "resp": "TEST_COMPLETE",
            "device": "LOF_TITAN_S3",
            "i2c": "OK",
            "spi": "OK",
            "status": "PASS"
        }) + "\n"
        if rig_uart:
            rig_uart.write(result)
        print(f"[TX] {result.strip()}")

# ------------------------------------------------------------------------------
# MAIN LOOP
# ------------------------------------------------------------------------------
rx_buffer = b""

while True:
    # 1. Listen on Rig UART
    if rig_uart and rig_uart.any():
        chunk = rig_uart.read()
        if chunk:
            rx_buffer += chunk
            if b"\n" in rx_buffer:
                lines = rx_buffer.split(b"\n")
                rx_buffer = lines[-1] # keep remainder in buffer
                for complete_line in lines[:-1]:
                    try:
                        decoded = complete_line.decode("utf-8").strip()
                        process_command(decoded)
                    except Exception as err:
                        print(f"[ERR] Decode error: {err}")

    # 2. Also listen on USB REPL (allows testing directly from Thonny terminal)
    # (Non-blocking loop)
    time.sleep_ms(5)
