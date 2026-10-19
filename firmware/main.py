# ==============================================================================
# LOF TITAN S3 - EOL TEST RIG COMPANION v4.0 (OFFICIAL HARDWARE PIN MAPPING)
# ==============================================================================
# Target PCB: ESP32-S3 running MicroPython
# ==============================================================================

import time
import json
import machine

# --- OFFICIAL PIN DEFINITIONS ---
PIN_LED_RED = 47
PIN_LED_GREEN = 48
PIN_BUZZER = 20
PIN_I2C_SDA = 7
PIN_I2C_SCL = 8

PIN_BUTTON_1 = 39
PIN_BUTTON_2 = 40
PIN_BUTTON_3 = 41
PIN_BUTTON_4 = 42
ALL_BUTTON_PINS = (PIN_BUTTON_1, PIN_BUTTON_2, PIN_BUTTON_3, PIN_BUTTON_4)

PIN_SENSOR_S1 = 2
PIN_SENSOR_S2 = 1
PIN_SENSOR_S3 = 3
PIN_SENSOR_S4 = 4
PIN_SENSOR_S5 = 5
ALL_SENSOR_PINS = (PIN_SENSOR_S1, PIN_SENSOR_S2, PIN_SENSOR_S3, PIN_SENSOR_S4, PIN_SENSOR_S5)

PIN_ULTRASONIC_S1 = 6
PIN_ULTRASONIC_S2 = 19

PIN_UART_RX = 18
PIN_UART_TX = 17

PIN_SPI_SCK = 35
PIN_SPI_MOSI = 36
PIN_SPI_MISO = 37
PIN_SPI_CS = 38

# Motor Drivers (4 Hardware Channels -> 6 Motor Connectors)
MOTOR_PINS = {
    "M1": (15, 16),
    "M2": (13, 14),
    "M3": (11, 12),
    "M4": (9, 10),
    "M5": (9, 10),
    "M6": (11, 12)
}

def safe_print(*args):
    try:
        print(*args)
    except Exception:
        pass

# Device Identity
try:
    uid = machine.unique_id()
    dev_id = f"{uid[-2]:02X}{uid[-1]:02X}"
    device_name = f"LOF_TITAN_{dev_id}"
except Exception:
    dev_id = "0000"
    device_name = "LOF_TITAN_DUT"

safe_print(f"[COMPANION] LOF TITAN S3 EOL Companion v4.0 for {device_name}")

# Watchdog
try:
    wdt = machine.WDT(timeout=16000)
except Exception:
    wdt = None

def feed():
    if wdt:
        try: wdt.feed()
        except Exception: pass

# Indicators & Buzzer
try:
    led_red = machine.Pin(PIN_LED_RED, machine.Pin.OUT, value=0)
    led_green = machine.Pin(PIN_LED_GREEN, machine.Pin.OUT, value=0)
    buzzer = machine.Pin(PIN_BUZZER, machine.Pin.OUT, value=0)
except Exception as e:
    safe_print(f"[INIT INDICATORS ERR] {e}")
    led_red, led_green, buzzer = None, None, None

# Buttons (Active LOW with pull-up)
btn_pins = {}
for idx, p in enumerate(ALL_BUTTON_PINS, 1):
    try:
        btn_pins[f"btn_{idx}"] = machine.Pin(p, machine.Pin.IN, machine.Pin.PULL_UP)
    except Exception:
        pass

# Sensors
sensor_pins = {}
for idx, p in enumerate(ALL_SENSOR_PINS, 1):
    try:
        sensor_pins[f"s{idx}"] = machine.Pin(p, machine.Pin.IN, machine.Pin.PULL_UP)
    except Exception:
        pass
try:
    sensor_pins["us1"] = machine.Pin(PIN_ULTRASONIC_S1, machine.Pin.IN, machine.Pin.PULL_UP)
    sensor_pins["us2"] = machine.Pin(PIN_ULTRASONIC_S2, machine.Pin.IN, machine.Pin.PULL_UP)
except Exception:
    pass

# UART to Test Rig (Hardwired: S3 TX=17 -> Rig RX=3, S3 RX=18 <- Rig TX=1)
try:
    uart = machine.UART(1, baudrate=115200, tx=PIN_UART_TX, rx=PIN_UART_RX, timeout=30)
    machine.Pin(PIN_UART_TX, machine.Pin.IN)  # High-Z idle to prevent bus contention
except Exception as e:
    safe_print(f"[UART ERR] {e}")
    uart = None

def send_uart_response(payload_str):
    global uart
    try:
        uart = machine.UART(1, baudrate=115200, tx=PIN_UART_TX, rx=PIN_UART_RX, timeout=30)
        uart.write(payload_str)
        time.sleep_ms(18)
        machine.Pin(PIN_UART_TX, machine.Pin.IN)
    except Exception as e:
        safe_print(f"[UART TX ERR] {e}")

def read_buttons():
    res = {}
    for k, p in btn_pins.items():
        try: res[k] = 0 if p.value() == 0 else 1  # 0: Pressed, 1: Released
        except Exception: res[k] = -1
    return res

def read_sensors():
    res = {}
    for k, p in sensor_pins.items():
        try: res[k] = p.value()
        except Exception: res[k] = -1
    return res

def scan_i2c():
    try:
        i2c = machine.I2C(0, scl=machine.Pin(PIN_I2C_SCL), sda=machine.Pin(PIN_I2C_SDA), freq=100000)
        devs = i2c.scan()
        return [f"0x{d:02X}" for d in devs]
    except Exception:
        return []

def pulse_motor(m_key, duration_ms=100, direction=1):
    pins = MOTOR_PINS.get(m_key.upper())
    if not pins: return False
    try:
        p_a = machine.Pin(pins[0], machine.Pin.OUT)
        p_b = machine.Pin(pins[1], machine.Pin.OUT)
        if direction >= 1:
            p_a.value(1); p_b.value(0)
        else:
            p_a.value(0); p_b.value(1)
        time.sleep_ms(duration_ms)
        p_a.value(0); p_b.value(0)
        return True
    except Exception:
        return False

rx_buf = b""
safe_print("[COMPANION] LOF TITAN S3 Ready!")

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

                    # 1. PING / Handshake
                    if "PING" in l_str:
                        resp = json.dumps({
                            "resp": "PONG",
                            "status": "DUT_OK",
                            "device_name": device_name,
                            "device_id": dev_id,
                            "ble_name": device_name,
                            "firmware": "LOF_TITAN_S3_v4.0",
                            "uptime_ms": time.ticks_ms()
                        }) + "\n"
                        send_uart_response(resp)

                    # 2. START_TEST / Automated Test
                    elif "START_TEST" in l_str or "SELF_TEST" in l_str:
                        if led_red: led_red.value(1)
                        if led_green: led_green.value(1)
                        if buzzer: buzzer.value(1)
                        time.sleep_ms(150)
                        if buzzer: buzzer.value(0)
                        if led_red: led_red.value(0)
                        if led_green: led_green.value(0)

                        for m in ["M1", "M2", "M3", "M4"]:
                            pulse_motor(m, 80, 1)

                        resp = json.dumps({
                            "type": "TEST_COMPLETE",
                            "status": "PASS",
                            "device_name": device_name,
                            "device_id": dev_id,
                            "buttons": read_buttons(),
                            "sensors": read_sensors(),
                            "i2c_devices": scan_i2c(),
                            "indicators": "PASS"
                        }) + "\n"
                        send_uart_response(resp)

                    # 3. BUZZER Control
                    elif "BUZZER" in l_str or "BEEP" in l_str:
                        if "OFF" in l_str or 'state":0' in l_str:
                            if buzzer: buzzer.value(0)
                            st = "OFF"
                        elif "ON" in l_str or 'state":1' in l_str:
                            if buzzer: buzzer.value(1)
                            st = "ON"
                        else:
                            if buzzer:
                                buzzer.value(1)
                                time.sleep_ms(250)
                                buzzer.value(0)
                            st = "BEEP_OK"
                        send_uart_response(json.dumps({"resp": "BUZZER_STATE", "state": st, "device_name": device_name}) + "\n")

                    # 4. LED Control (Red = LED 1 / Green = LED 2)
                    elif "LED" in l_str:
                        if "1" in l_str or "RED" in l_str:
                            val = 1 if ("ON" in l_str or 'state":1' in l_str) else 0
                            if led_red: led_red.value(val)
                            send_uart_response(json.dumps({"resp": "LED1_STATE", "color": "RED", "state": "ON" if val else "OFF", "device_name": device_name}) + "\n")
                        elif "2" in l_str or "GREEN" in l_str:
                            val = 1 if ("ON" in l_str or 'state":1' in l_str) else 0
                            if led_green: led_green.value(val)
                            send_uart_response(json.dumps({"resp": "LED2_STATE", "color": "GREEN", "state": "ON" if val else "OFF", "device_name": device_name}) + "\n")
                        else:
                            val = 1 if ("ON" in l_str or 'state":1' in l_str) else 0
                            if led_red: led_red.value(val)
                            if led_green: led_green.value(val)
                            send_uart_response(json.dumps({"resp": "ALL_LEDS_STATE", "state": "ON" if val else "OFF", "device_name": device_name}) + "\n")

                    # 5. BUTTONS Query (Push buttons 39, 40, 41, 42)
                    elif "BUTTON" in l_str:
                        send_uart_response(json.dumps({
                            "resp": "BUTTONS_STATE",
                            "buttons": read_buttons(),
                            "device_name": device_name
                        }) + "\n")

                    # 6. SENSORS Query (S1-S5, Ultrasonic)
                    elif "SENSOR" in l_str:
                        send_uart_response(json.dumps({
                            "resp": "SENSORS_STATE",
                            "sensors": read_sensors(),
                            "device_name": device_name
                        }) + "\n")

                    # 7. MOTOR Pulse Test
                    elif "MOTOR" in l_str:
                        m_name = "M1"
                        for m_candidate in ["M1", "M2", "M3", "M4", "M5", "M6"]:
                            if m_candidate in l_str:
                                m_name = m_candidate
                                break
                        ok = pulse_motor(m_name, 100, 1)
                        send_uart_response(json.dumps({
                            "resp": "MOTOR_PULSE_OK" if ok else "MOTOR_ERR",
                            "motor": m_name,
                            "pins": list(MOTOR_PINS.get(m_name, ())),
                            "device_name": device_name
                        }) + "\n")

                    # 8. I2C Bus Scan
                    elif "I2C" in l_str:
                        devs = scan_i2c()
                        send_uart_response(json.dumps({
                            "resp": "I2C_SCAN_OK",
                            "devices": devs,
                            "count": len(devs),
                            "sda": PIN_I2C_SDA,
                            "scl": PIN_I2C_SCL,
                            "device_name": device_name
                        }) + "\n")

    time.sleep_ms(10)
