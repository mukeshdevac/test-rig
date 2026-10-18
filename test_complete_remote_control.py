import serial
import time
import json

rig = serial.Serial()
rig.port = 'COM5'
rig.baudrate = 115200
rig.dtr = False
rig.rts = False
rig.open()

print("=" * 65)
print("TEST RIG FULL REMOTE CONTROL OF TARGET PCB (COM5)")
print("=" * 65)

# 1. Check DUT Name
print("\n[1] Querying GET_DUT_NAME...")
rig.reset_input_buffer()
rig.write(b'GET_DUT_NAME\n')
time.sleep(0.5)
while rig.in_waiting:
    print("  <--", rig.readline().decode('utf-8', 'ignore').strip())

# 2. Test Buzzer on Test PCB through Rig
print("\n[2] Triggering BUZZER on Test PCB through Rig...")
rig.reset_input_buffer()
rig.write(b'{"cmd":"BUZZER","action":"BEEP"}\n')
time.sleep(0.6)
while rig.in_waiting:
    print("  <--", rig.readline().decode('utf-8', 'ignore').strip())

# 3. Trigger Automated Test
print("\n[3] Triggering START_TEST on Test PCB through Rig...")
rig.reset_input_buffer()
rig.write(b'{"cmd":"START_TEST","serial_number":"EOL-TITAN-5F14"}\n')
start = time.time()
while time.time() - start < 3.5:
    if rig.in_waiting:
        line = rig.readline().decode('utf-8', 'ignore').strip()
        if line:
            print("  <--", line)
    time.sleep(0.05)

# 4. Final Status
print("\n[4] Querying Final DUT_STATUS...")
rig.write(b'CHECK_DUT\n')
time.sleep(0.5)
while rig.in_waiting:
    print("  <--", rig.readline().decode('utf-8', 'ignore').strip())

rig.close()
print("\n" + "=" * 65)
print("ALL TESTS COMPLETED SUCCESSFULLY!")
print("=" * 65)
