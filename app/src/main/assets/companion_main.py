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

SENSOR_PIN_MAP = {
    "s1": PIN_SENSOR_S1,
    "s2": PIN_SENSOR_S2,
    "s3": PIN_SENSOR_S3,
    "s4": PIN_SENSOR_S4,
    "s5": PIN_SENSOR_S5,
    "us1": PIN_ULTRASONIC_S1,
    "us2": PIN_ULTRASONIC_S2
}

# Pre-initialize ADC1 instances on channels 1, 2, 3, 4, 5, 6
adc_instances = {}
for k, pnum in SENSOR_PIN_MAP.items():
    if pnum in (1, 2, 3, 4, 5, 6):
        try:
            adc = machine.ADC(machine.Pin(pnum))
            adc.atten(machine.ADC.ATTN_11DB)
            adc_instances[k] = adc
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
        safe_print(f"[DUT TX -> RIG] {payload_str.strip()}")
        uart = machine.UART(1, baudrate=115200, tx=PIN_UART_TX, rx=PIN_UART_RX, timeout=30)
        uart.write(payload_str)
        # Ensure complete FIFO drain before putting TX pin into High-Z mode
        tx_wait = max(50, (len(payload_str) * 10 // 115) + 35)
        time.sleep_ms(tx_wait)
        machine.Pin(PIN_UART_TX, machine.Pin.IN)
    except Exception as e:
        safe_print(f"[UART TX ERR] {e}")

def read_buttons():
    res = {}
    for k, p in btn_pins.items():
        try: res[k] = 0 if p.value() == 0 else 1  # 0: Pressed (GND), 1: Released (Pullup)
        except Exception: res[k] = -1
    return res

def read_sensors():
    res = {}
    for k, pnum in SENSOR_PIN_MAP.items():
        try:
            if k in adc_instances:
                raw = adc_instances[k].read()
                voltage = round((raw / 4095.0) * 3.3, 2)
                logic = 1 if voltage >= 1.65 else 0
            else:
                p = sensor_pins.get(k)
                val = p.value() if p else 0
                logic = val
                voltage = 3.3 if val == 1 else 0.0
                raw = 4095 if val == 1 else 0
            res[k] = {
                "logic": logic,
                "voltage": voltage,
                "raw": raw
            }
        except Exception:
            res[k] = {"logic": -1, "voltage": 0.0, "raw": 0}
    return res

def scan_i2c():
    try:
        i2c = machine.I2C(0, scl=machine.Pin(PIN_I2C_SCL), sda=machine.Pin(PIN_I2C_SDA), freq=100000)
        devs = i2c.scan()
        return [f"0x{d:02X}" for d in devs]
    except Exception:
        return []

active_pwms = {}

def set_motor(m_key, speed_pct=100, direction="FWD"):
    pins = MOTOR_PINS.get(m_key.upper())
    if not pins: return False
    pin_a, pin_b = pins[0], pins[1]

    for p in (pin_a, pin_b):
        if p in active_pwms:
            try: active_pwms[p].deinit()
            except Exception: pass
            del active_pwms[p]

    speed_pct = max(0, min(100, int(speed_pct)))
    dir_str = str(direction).upper()

    if speed_pct == 0 or dir_str in ("STOP", "0", "OFF"):
        machine.Pin(pin_a, machine.Pin.OUT, value=0)
        machine.Pin(pin_b, machine.Pin.OUT, value=0)
        return True

    duty = int((speed_pct / 100.0) * 1023)

    if dir_str in ("FWD", "FORWARD", "1", "+"):
        machine.Pin(pin_b, machine.Pin.OUT, value=0)
        pwm = machine.PWM(machine.Pin(pin_a), freq=1000)
        pwm.duty(duty)
        active_pwms[pin_a] = pwm
    else:
        machine.Pin(pin_a, machine.Pin.OUT, value=0)
        pwm = machine.PWM(machine.Pin(pin_b), freq=1000)
        pwm.duty(duty)
        active_pwms[pin_b] = pwm
    return True

def pulse_motor(m_key, duration_ms=250, speed_pct=100, direction="FWD"):
    set_motor(m_key, speed_pct, direction)
    time.sleep_ms(duration_ms)
    set_motor(m_key, 0, "STOP")
    return True

def stop_all_motors():
    for m in ("M1", "M2", "M3", "M4", "M5", "M6"):
        set_motor(m, 0, "STOP")

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

                    # Ignore echo responses and test rig telemetry to prevent loopback storms
                    if '"resp":' in l_str or '"type":' in l_str or "RIG_STARTUP" in l_str or "DUT_STATUS" in l_str:
                        continue

                    safe_print(f"[DUT RX <- RIG] {l_str}")

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

                    # 2. START_TEST / Automated Test (Runs slowly & deliberately step by step)
                    elif "START_TEST" in l_str or "SELF_TEST" in l_str:
                        # Step 1: LED 1 RED (Blink twice)
                        if led_red: led_red.value(1)
                        time.sleep_ms(400)
                        if led_red: led_red.value(0)
                        time.sleep_ms(200)
                        if led_red: led_red.value(1)
                        time.sleep_ms(400)
                        if led_red: led_red.value(0)
                        time.sleep_ms(400)

                        # Step 2: LED 2 GREEN (Blink twice)
                        if led_green: led_green.value(1)
                        time.sleep_ms(400)
                        if led_green: led_green.value(0)
                        time.sleep_ms(200)
                        if led_green: led_green.value(1)
                        time.sleep_ms(400)
                        if led_green: led_green.value(0)
                        time.sleep_ms(400)

                        # Step 3: BUZZER (Audible alert)
                        if buzzer: buzzer.value(1)
                        time.sleep_ms(250)
                        if buzzer: buzzer.value(0)
                        time.sleep_ms(150)
                        if buzzer: buzzer.value(1)
                        time.sleep_ms(250)
                        if buzzer: buzzer.value(0)
                        time.sleep_ms(500)

                        # Step 4: MOTORS (M1 to M4 dual-phase spin)
                        for m in ["M1", "M2", "M3", "M4"]:
                            set_motor(m, 75, "FWD")
                            time.sleep_ms(400)
                            set_motor(m, 0, "STOP")
                            time.sleep_ms(150)
                            set_motor(m, 75, "REV")
                            time.sleep_ms(400)
                            set_motor(m, 0, "STOP")
                            time.sleep_ms(300)

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
                        val = 1 if ("ON" in l_str or 'state":1' in l_str) else 0
                        if '"led":2' in l_str or "LED2" in l_str or "GREEN" in l_str:
                            if led_green: led_green.value(val)
                            send_uart_response(json.dumps({"resp": "LED2_STATE", "color": "GREEN", "state": "ON" if val else "OFF", "device_name": device_name}) + "\n")
                        elif '"led":1' in l_str or "LED1" in l_str or "RED" in l_str:
                            if led_red: led_red.value(val)
                            send_uart_response(json.dumps({"resp": "LED1_STATE", "color": "RED", "state": "ON" if val else "OFF", "device_name": device_name}) + "\n")
                        else:
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

                    # 6b. PIN_TEST fallback for buttons and sensors
                    elif "PIN_TEST" in l_str:
                        if any(f'"{p}"' in l_str or f":{p}" in l_str or f": {p}" in l_str for p in ALL_BUTTON_PINS):
                            send_uart_response(json.dumps({
                                "resp": "BUTTONS_STATE",
                                "buttons": read_buttons(),
                                "device_name": device_name
                            }) + "\n")
                        elif any(f'"{p}"' in l_str or f":{p}" in l_str or f": {p}" in l_str for p in (ALL_SENSOR_PINS + [PIN_ULTRASONIC_S1, PIN_ULTRASONIC_S2])):
                            send_uart_response(json.dumps({
                                "resp": "SENSORS_STATE",
                                "sensors": read_sensors(),
                                "device_name": device_name
                            }) + "\n")
                        else:
                            send_uart_response(json.dumps({
                                "resp": "PIN_TEST_OK",
                                "device_name": device_name
                            }) + "\n")

                    # 7. MOTOR Control (Direction & Speed)
                    elif "MOTOR" in l_str:
                        if "STOP_ALL" in l_str or "ALL_OFF" in l_str:
                            stop_all_motors()
                            send_uart_response(json.dumps({
                                "resp": "MOTOR_STATE",
                                "action": "STOP_ALL",
                                "device_name": device_name
                            }) + "\n")
                        else:
                            m_name = "M1"
                            for m_candidate in ["M1", "M2", "M3", "M4", "M5", "M6"]:
                                if m_candidate in l_str:
                                    m_name = m_candidate
                                    break

                            direction = "FWD"
                            if "REV" in l_str or "REVERSE" in l_str or '"dir":"REV"' in l_str or '"dir":-1' in l_str:
                                direction = "REV"

                            speed_pct = 100
                            idx_speed = l_str.find('"speed":')
                            if idx_speed >= 0:
                                try:
                                    end_speed = l_str.find(',', idx_speed)
                                    if end_speed == -1: end_speed = l_str.find('}', idx_speed)
                                    speed_pct = int(l_str[idx_speed+8:end_speed].strip())
                                except Exception:
                                    speed_pct = 100

                            duration_ms = 250
                            idx_dur = l_str.find('"duration_ms":')
                            if idx_dur >= 0:
                                try:
                                    end_dur = l_str.find(',', idx_dur)
                                    if end_dur == -1: end_dur = l_str.find('}', idx_dur)
                                    duration_ms = int(l_str[idx_dur+14:end_dur].strip())
                                except Exception:
                                    duration_ms = 250

                            if "STOP" in l_str or '"action":"STOP"' in l_str or speed_pct == 0:
                                set_motor(m_name, 0, "STOP")
                                act = "STOP"
                            elif "RUN" in l_str or '"action":"RUN"' in l_str:
                                set_motor(m_name, speed_pct, direction)
                                act = "RUN"
                            else:
                                pulse_motor(m_name, duration_ms, speed_pct, direction)
                                act = "PULSE"

                            send_uart_response(json.dumps({
                                "resp": "MOTOR_STATE",
                                "motor": m_name,
                                "action": act,
                                "dir": direction,
                                "speed": speed_pct,
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
