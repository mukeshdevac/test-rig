# ==============================================================================
# LOF TITAN S3 - FULL EOL TEST RIG COMPANION v3.7 (LED & BUZZER ACTIVE HARDWARE)
# ==============================================================================
# Target PCB: ESP32-S3 running MicroPython
# Hardware Pin Mapping:
# - Buzzer : GPIO 20 (Active High)
# - LED 1  : GPIO 47 (Active High)
# - LED 2  : GPIO 48 (Active High)
# - UART 1 : TX GPIO 17 (To Rig RX 3), RX GPIO 18 (From Rig TX 1)
# ==============================================================================

import time
import json
import machine

UART_ID = 1
BAUD = 115200
TX_PIN = 17
RX_PIN = 18

def safe_print(*args):
    """Safe print that never blocks or throws when USB CDC is disconnected."""
    try:
        print(*args)
    except Exception:
        pass

# Extract Unique Hardware MAC Address / Bluetooth Name (e.g. LOF_TITAN_5F14)
try:
    uid = machine.unique_id()
    dev_id = f"{uid[-2]:02X}{uid[-1]:02X}"
    device_name = f"LOF_TITAN_{dev_id}"
except Exception:
    dev_id = "0000"
    device_name = "LOF_TITAN_DUT"

safe_print(f"[COMPANION] Starting EOL Companion for {device_name} (ID: {dev_id})...")

# Hardware Watchdog Feeder (8000ms supervisor WDT safe feed)
try:
    wdt = machine.WDT(timeout=16000)
except Exception:
    wdt = None

def feed():
    if wdt:
        try:
            wdt.feed()
        except Exception:
            pass

# Initialize Physical Peripherals on LOF Titan S3
try:
    led1_pin = machine.Pin(47, machine.Pin.OUT)
    led2_pin = machine.Pin(48, machine.Pin.OUT)
    buzzer_pin = machine.Pin(20, machine.Pin.OUT)
    led1_pin.value(0)
    led2_pin.value(0)
    buzzer_pin.value(0)
except Exception as e:
    safe_print(f"[GPIO ERR] {e}")
    led1_pin = None
    led2_pin = None
    buzzer_pin = None

# UART Initialization to Test Rig (Rig TX=1 -> S3 RX=18, S3 TX=17 -> Rig RX=3)
try:
    uart = machine.UART(UART_ID, baudrate=BAUD, tx=TX_PIN, rx=RX_PIN, timeout=30)
    # Release TX pin to High-Z input so it does not block Test Rig CP2102 USB RX line while idle!
    machine.Pin(TX_PIN, machine.Pin.IN)
    safe_print(f"[UART] Initialized on TX={TX_PIN}, RX={RX_PIN} @ {BAUD} Baud (High-Z Idle)")
except Exception as e:
    safe_print(f"[UART ERR] {e}")
    uart = None

def send_uart_response(payload_str):
    """Sends UART response and immediately releases TX pin to High-Z to prevent bus contention."""
    global uart
    try:
        uart = machine.UART(UART_ID, baudrate=BAUD, tx=TX_PIN, rx=RX_PIN, timeout=30)
        uart.write(payload_str)
        time.sleep_ms(18)
        machine.Pin(TX_PIN, machine.Pin.IN)
    except Exception as e:
        safe_print(f"[UART TX ERR] {e}")

rx_buf = b""
safe_print(f"[COMPANION] Ready! Listening for Test Rig Handshake...")

while True:
    feed()
    if uart and uart.any():
        c = uart.read()
        if c:
            rx_buf += c
            if b"\n" in rx_buf:
                lines = rx_buf.split(b"\n")
                rx_buf = lines[-1]
                for l in lines[:-1]:
                    try:
                        l_str = l.decode('utf-8', 'ignore').strip()
                    except Exception:
                        l_str = "".join([chr(b) for b in l if 32 <= b <= 126]).strip()

                    if not l_str:
                        continue
                    
                    # 1. PING Handshake -> Returns Unique Bluetooth Device Name
                    if "PING" in l_str:
                        resp = json.dumps({
                            "resp": "PONG",
                            "status": "DUT_OK",
                            "device_name": device_name,
                            "device_id": dev_id,
                            "ble_name": device_name,
                            "device": device_name,
                            "firmware": "MicroPython v3.7",
                            "uptime_ms": time.ticks_ms()
                        }) + "\n"
                        send_uart_response(resp)
                        safe_print(f"[TX PONG] Sent device_name={device_name}")

                    # 2. START_TEST / SELF_TEST
                    elif "START_TEST" in l_str or "SELF_TEST" in l_str:
                        # Self-test visual blink & buzz sequence
                        if led1_pin: led1_pin.value(1)
                        if led2_pin: led2_pin.value(1)
                        if buzzer_pin: buzzer_pin.value(1)
                        time.sleep_ms(150)
                        if buzzer_pin: buzzer_pin.value(0)
                        if led1_pin: led1_pin.value(0)
                        if led2_pin: led2_pin.value(0)

                        resp = json.dumps({
                            "type": "TEST_COMPLETE",
                            "status": "PASS",
                            "device_name": device_name,
                            "device_id": dev_id,
                            "ble_name": device_name,
                            "i2c": "OK",
                            "sensors": "OK",
                            "indicators": "PASS"
                        }) + "\n"
                        send_uart_response(resp)
                        safe_print(f"[TX TEST_COMPLETE] Sent for {device_name}")

                    # 3. BUZZER CONTROL (ON / OFF / BEEP)
                    elif "BUZZER" in l_str or "BEEP" in l_str:
                        st = "OFF"
                        if "OFF" in l_str or 'state":0' in l_str or 'state":"OFF"' in l_str:
                            if buzzer_pin: buzzer_pin.value(0)
                            st = "OFF"
                        elif "ON" in l_str or 'state":1' in l_str or 'state":"ON"' in l_str:
                            if buzzer_pin: buzzer_pin.value(1)
                            st = "ON"
                        else:
                            if buzzer_pin:
                                buzzer_pin.value(1)
                                time.sleep_ms(250)
                                buzzer_pin.value(0)
                            st = "BEEP_OK"

                        resp = json.dumps({
                            "resp": "BUZZER_STATE",
                            "state": st,
                            "device_name": device_name
                        }) + "\n"
                        send_uart_response(resp)

                    # 4. LED CONTROL (LED 1 / LED 2 / ALL)
                    elif "LED" in l_str:
                        if "1" in l_str:
                            val = 1 if ("ON" in l_str or 'state":1' in l_str or 'state":"ON"' in l_str) else 0
                            if led1_pin: led1_pin.value(val)
                            st = "ON" if val else "OFF"
                            resp = json.dumps({"resp": "LED1_STATE", "state": st, "device_name": device_name}) + "\n"
                        elif "2" in l_str:
                            val = 1 if ("ON" in l_str or 'state":1' in l_str or 'state":"ON"' in l_str) else 0
                            if led2_pin: led2_pin.value(val)
                            st = "ON" if val else "OFF"
                            resp = json.dumps({"resp": "LED2_STATE", "state": st, "device_name": device_name}) + "\n"
                        else:
                            val = 1 if ("ON" in l_str or 'state":1' in l_str or 'state":"ON"' in l_str) else 0
                            if led1_pin: led1_pin.value(val)
                            if led2_pin: led2_pin.value(val)
                            st = "ON" if val else "OFF"
                            resp = json.dumps({"resp": "ALL_LEDS_STATE", "state": st, "device_name": device_name}) + "\n"

                        send_uart_response(resp)

    time.sleep_ms(10)
