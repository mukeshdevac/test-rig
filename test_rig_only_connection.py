import serial
import time
import json

ser = serial.Serial()
ser.port = 'COM5'
ser.baudrate = 115200
ser.dtr = False
ser.rts = False
ser.open()
time.sleep(0.3)
ser.reset_input_buffer()

print("=" * 65)
print("TESTING HARDWIRED UART CONNECTION TO TEST PCB VIA RIG (COM5)")
print("=" * 65)

# 1. Query GET_STATUS
print("\n[STEP 1] Querying GET_STATUS from Test Rig...")
ser.write(b'{"cmd":"GET_STATUS"}\n')
time.sleep(0.5)
while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("  <--", line)

# 2. Query GET_DUT_NAME
print("\n[STEP 2] Querying GET_DUT_NAME from Test Rig...")
ser.write(b'{"cmd":"GET_DUT_NAME"}\n')
time.sleep(0.5)
while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("  <--", line)

# 3. PING Target PCB directly through CMD_FORWARD
print("\n[STEP 3] Sending PING to Target PCB through Rig...")
ser.write(b'{"cmd":"CMD_FORWARD","payload":"{\\"cmd\\":\\"PING\\"}"}\n')
time.sleep(0.5)
while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("  <--", line)

# 4. Trigger START_TEST through Rig
print("\n[STEP 4] Sending START_TEST to Target PCB through Rig...")
ser.write(b'{"cmd":"START_TEST","serial_number":"EOL-TITAN-001"}\n')
time.sleep(1.0)
while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("  <--", line)

ser.close()
print("\nDone.")
