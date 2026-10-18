import serial
import time
import json

print("=" * 65)
print("CONNECTING TO TEST PCB THROUGH TEST RIG (COM5)")
print("=" * 65)

ser = serial.Serial()
ser.port = 'COM5'
ser.baudrate = 115200
ser.dtr = False
ser.rts = False
ser.open()
time.sleep(0.3)
ser.reset_input_buffer()

# 1. Trigger Handshake Check
print("\n[STEP 1] Sending CHECK_DUT to Test Rig...")
ser.write(b'CHECK_DUT\n')
time.sleep(0.8)

lines = []
while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        lines.append(line)
        print("  <--", line)

# 2. Query GET_DUT_NAME
print("\n[STEP 2] Sending GET_DUT_NAME to Test Rig...")
ser.write(b'GET_DUT_NAME\n')
time.sleep(0.5)

while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        lines.append(line)
        print("  <--", line)

# 3. Check DUT Status Frame
print("\n[STEP 3] Querying GET_STATUS...")
ser.write(b'{"cmd":"GET_STATUS"}\n')
time.sleep(0.5)

while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        lines.append(line)
        print("  <--", line)

ser.close()

# Evaluate connection
connected = any('"connected":true' in l or 'ONLINE' in l for l in lines)
print("\n" + "=" * 65)
if connected:
    print("SUCCESS: TEST PCB IS ONLINE AND CONNECTED THROUGH TEST RIG!")
else:
    print("STATUS: Test Rig is reachable, awaiting Test PCB handshake response.")
print("=" * 65)
