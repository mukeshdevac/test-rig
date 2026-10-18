import time
import json
import serial

ser = serial.Serial(r'\\.\COM11', 115200, timeout=0.2)
ser.dtr = False
ser.rts = True
time.sleep(0.1)
ser.rts = False
time.sleep(0.1)

# Wait for countdown
start = time.time()
while time.time() - start < 4.0:
    if ser.in_waiting:
        line = ser.readline().decode('utf-8', 'ignore').strip()
        if "countdown" in line or "Starting in" in line:
            break
    time.sleep(0.02)

ser.write(b'{"cmd":"CONNECT"}\n')
time.sleep(0.5)
while ser.in_waiting:
    ser.read(ser.in_waiting)

# Query unique_id
check_code = (
    "import machine, ubinascii\n"
    "uid = machine.unique_id()\n"
    "hex_id = ubinascii.hexlify(uid).decode().upper()\n"
    "print('RAW_UID_HEX:', hex_id)\n"
    "print('DEVICE_ID:', hex_id[-4:])\n"
    "print('DEVICE_NAME:', 'LOF_TITAN_' + hex_id[-4:])\n"
)
ser.write(json.dumps({"cmd": "EXEC", "code": check_code}).encode('utf-8') + b'\n')
time.sleep(0.5)

start = time.time()
while time.time() - start < 2.0:
    if ser.in_waiting:
        print(ser.readline().decode('utf-8', 'ignore').strip())
    time.sleep(0.05)

ser.close()
