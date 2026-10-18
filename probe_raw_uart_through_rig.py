import serial
import time

ser = serial.Serial()
ser.port = 'COM5'
ser.baudrate = 115200
ser.dtr = False
ser.rts = False
ser.open()
time.sleep(0.3)
ser.reset_input_buffer()

print("Testing raw UART communication through Test Rig (GPIO 1 -> DUT GPIO 18, DUT GPIO 17 -> GPIO 3)...")

probes = [
    b'\r\n',
    b'\x03\x03', # Ctrl-C
    b'\x02\r\n', # Ctrl-B
    b'{"cmd":"CONNECT"}\n',
    b'{"cmd":"PING"}\n',
    b'{"cmd":"STATUS"}\n',
    b'print("HELLO_FROM_REPL")\r\n',
    b'import machine; print(machine.unique_id())\r\n'
]

for p in probes:
    print(f"\n[SENDING RAW]: {repr(p)}")
    # We send directly to Test Rig Serial.
    # Note: On the Test Rig, Serial is GPIO 1 (TX).
    ser.write(p)
    time.sleep(0.4)
    resp = ser.read(ser.in_waiting or 0)
    if resp:
        print(f"[RECEIVED ON COM5]: {repr(resp)}")
    else:
        print("[NO RESPONSE]")

ser.close()
