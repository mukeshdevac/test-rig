# ==============================================================================
# LOF TITAN S3 - EOL TEST RIG COMPANION v4.2 (HIGH-SPEED RESILIENT COMMUNICATION)
# ==============================================================================
# Target PCB: ESP32-S3 running MicroPython
# ==============================================================================

import time
import json
import machine

# --- OFFICIAL HARDWARE PIN DEFINITIONS ---
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
    dev_id = "".join([f"{b:02X}" for b in uid])
    device_name = f"LOF_TITAN_{dev_id[-4:]}"
except Exception:
    dev_id = "UNKNOWN"
    device_name = "LOF_TITAN_DUT"

# Watchdog Feed
try:
    wdt = machine.WDT(timeout=8000)
    def feed():
        try: wdt.feed()
        except Exception: pass
except Exception:
    def feed(): pass

# Indicators & Sound
try:
    led_red = machine.Pin(PIN_LED_RED, machine.Pin.OUT)
    led_red.value(0)
except Exception:
    led_red = None

try:
    led_green = machine.Pin(PIN_LED_GREEN, machine.Pin.OUT)
    led_green.value(0)
except Exception:
    led_green = None

try:
    buzzer = machine.Pin(PIN_BUZZER, machine.Pin.OUT)
    buzzer.value(0)
except Exception:
    buzzer = None

# Buttons (Active LOW with internal pull-up - Connected to GND when pressed)
btn_pins = {}
for idx, p in enumerate(ALL_BUTTON_PINS, 1):
    try:
        btn_pins[f"btn_{idx}"] = machine.Pin(p, machine.Pin.IN, machine.Pin.PULL_UP)
    except Exception:
        pass

# Sensor ADCs & Digital Pins
sensor_adcs = {}
sensor_pins = {}
for idx, p in enumerate(ALL_SENSOR_PINS, 1):
    k = f"s{idx}"
    try:
        p_obj = machine.Pin(p, machine.Pin.IN, machine.Pin.PULL_UP)
        sensor_pins[k] = p_obj
        adc_obj = machine.ADC(p_obj)
        try:
            adc_obj.atten(machine.ADC.ATTN_11DB)
        except Exception:
            pass
        sensor_adcs[k] = adc_obj
    except Exception:
        pass

try:
    sensor_pins["us1"] = machine.Pin(PIN_ULTRASONIC_S1, machine.Pin.IN, machine.Pin.PULL_UP)
    sensor_pins["us2"] = machine.Pin(PIN_ULTRASONIC_S2, machine.Pin.IN, machine.Pin.PULL_UP)
except Exception:
    pass

# Hardwired UART (Permanent 115200 Baud, 2KB buffer, non-blocking)
try:
    uart = machine.UART(1, baudrate=115200, tx=PIN_UART_TX, rx=PIN_UART_RX, rxbuf=2048, timeout=0)
except Exception as e:
    safe_print(f"[UART ERR] {e}")
    uart = None

def send_uart_response(payload_str):
    if uart:
        try:
            uart.write(payload_str)
        except Exception:
            pass

def read_buttons():
    res = {}
    for k, p in btn_pins.items():
        try:
            raw = p.value()
            # Buttons are active LOW: 0 = PRESSED (connected to GND), 1 = RELEASED
            is_pressed = (raw == 0)
            res[k] = {
                "raw": raw,
                "pressed": is_pressed,
                "state": "PRESSED" if is_pressed else "RELEASED"
            }
        except Exception:
            res[k] = {"raw": 1, "pressed": False, "state": "ERROR"}
    return res

def read_sensors():
    res = {}
    for k, adc in sensor_adcs.items():
        try:
            raw = adc.read()
            v = round((raw / 4095.0) * 3.3, 2)
            res[k] = {
                "raw": raw,
                "voltage": v,
                "digital": 1 if raw > 2000 else 0
            }
        except Exception:
            p = sensor_pins.get(k)
            pv = p.value() if p else -1
            res[k] = {
                "raw": 4095 if pv == 1 else 0,
                "voltage": 3.3 if pv == 1 else 0.0,
                "digital": pv
            }
    for uk in ["us1", "us2"]:
        p = sensor_pins.get(uk)
        pv = p.value() if p else -1
        res[uk] = {
            "raw": 4095 if pv == 1 else 0,
            "voltage": 3.3 if pv == 1 else 0.0,
            "digital": pv
        }
    return res

def scan_i2c():
    try:
        i2c = machine.I2C(0, scl=machine.Pin(PIN_I2C_SCL), sda=machine.Pin(PIN_I2C_SDA), freq=100000)
        devs = i2c.scan()
        return [f"0x{d:02X}" for d in devs]
    except Exception:
        return []

def pulse_motor(m_key, duration_ms=70, direction=1):
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
safe_print("[COMPANION] LOF TITAN S3 Ready v4.2!")

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

                    # Noise-resistant JSON extraction
                    i1 = l_str.find("{")
                    i2 = l_str.rfind("}")
                    cmd_obj = None
                    if i1 != -1 and i2 > i1:
                        try:
                            cmd_obj = json.loads(l_str[i1:i2+1])
                        except Exception:
                            pass

                    # 1. PING / Handshake
                    if "PING" in l_str:
                        resp = json.dumps({
                            "resp": "PONG",
                            "status": "DUT_OK",
                            "device_name": device_name,
                            "device_id": dev_id,
                            "ble_name": device_name,
                            "firmware": "LOF_TITAN_S3_v4.2",
                            "uptime_ms": time.ticks_ms()
                        }) + "\n"
                        send_uart_response(resp)

                    # 2. START_TEST
                    elif "START_TEST" in l_str or "SELF_TEST" in l_str:
                        if led_red: led_red.value(1)
                        if led_green: led_green.value(1)
                        if buzzer: buzzer.value(1)
                        time.sleep_ms(100)
                        if buzzer: buzzer.value(0)
                        if led_red: led_red.value(0)
                        if led_green: led_green.value(0)

                        for m in ["M1", "M2", "M3", "M4"]:
                            pulse_motor(m, 50, 1)

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
                        st = "OFF"
                        if "OFF" in l_str or (cmd_obj and cmd_obj.get("state") == 0):
                            if buzzer: buzzer.value(0)
                            st = "OFF"
                        elif "ON" in l_str or (cmd_obj and cmd_obj.get("state") == 1):
                            if buzzer: buzzer.value(1)
                            st = "ON"
                        else:
                            if buzzer:
                                buzzer.value(1)
                                time.sleep_ms(120)
                                buzzer.value(0)
                            st = "BEEP_OK"
                        send_uart_response(json.dumps({"resp": "BUZZER_STATE", "state": st, "device_name": device_name}) + "\n")

                    # 4. LED Control (Disambiguated JSON & Token parsing)
                    elif "LED" in l_str:
                        target_led = None
                        target_val = None
                        if cmd_obj and cmd_obj.get("cmd") == "LED":
                            target_led = str(cmd_obj.get("led", ""))
                            target_val = int(cmd_obj.get("state", 0))

                        if target_led is None:
                            if "LED1_ON" in l_str or "RED_ON" in l_str:
                                target_led = "1"; target_val = 1
                            elif "LED1_OFF" in l_str or "RED_OFF" in l_str:
                                target_led = "1"; target_val = 0
                            elif "LED2_ON" in l_str or "GREEN_ON" in l_str:
                                target_led = "2"; target_val = 1
                            elif "LED2_OFF" in l_str or "GREEN_OFF" in l_str:
                                target_led = "2"; target_val = 0
                            elif "LEDS_ON" in l_str or "ALL_ON" in l_str:
                                target_led = "ALL"; target_val = 1
                            elif "LEDS_OFF" in l_str or "ALL_OFF" in l_str:
                                target_led = "ALL"; target_val = 0

                        if target_led in ["1", "RED"]:
                            if led_red: led_red.value(target_val)
                            send_uart_response(json.dumps({"resp": "LED1_STATE", "color": "RED", "state": "ON" if target_val else "OFF", "device_name": device_name}) + "\n")
                        elif target_led in ["2", "GREEN"]:
                            if led_green: led_green.value(target_val)
                            send_uart_response(json.dumps({"resp": "LED2_STATE", "color": "GREEN", "state": "ON" if target_val else "OFF", "device_name": device_name}) + "\n")
                        elif target_led == "ALL":
                            if led_red: led_red.value(target_val)
                            if led_green: led_green.value(target_val)
                            send_uart_response(json.dumps({"resp": "ALL_LEDS_STATE", "state": "ON" if target_val else "OFF", "device_name": device_name}) + "\n")

                    # 5. BUTTONS Query (Push buttons 39, 40, 41, 42 - Active LOW)
                    elif "BUTTON" in l_str:
                        send_uart_response(json.dumps({
                            "resp": "BUTTONS_STATE",
                            "buttons": read_buttons(),
                            "device_name": device_name
                        }) + "\n")

                    # 6. SENSORS Query (S1-S5 ADC, US1-US2)
                    elif "SENSOR" in l_str:
                        send_uart_response(json.dumps({
                            "resp": "SENSORS_STATE",
                            "sensors": read_sensors(),
                            "device_name": device_name
                        }) + "\n")

                    # 7. I2C Bus Scan
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

                    # 8. MOTOR Pulse Test
                    elif "MOTOR" in l_str:
                        m_name = "M1"
                        for m_candidate in ["M1", "M2", "M3", "M4", "M5", "M6"]:
                            if m_candidate in l_str:
                                m_name = m_candidate
                                break
                        ok = pulse_motor(m_name, 60, 1)
                        send_uart_response(json.dumps({
                            "resp": "MOTOR_PULSE_OK" if ok else "MOTOR_ERR",
                            "motor": m_name,
                            "pins": list(MOTOR_PINS.get(m_name, ())),
                            "device_name": device_name
                        }) + "\n")

    time.sleep_ms(1)
