import serial
import time

rig = serial.Serial()
rig.port = 'COM5'
rig.baudrate = 115200
rig.dtr = False
rig.rts = False
rig.open()

print("Sending CHECK_DUT to Test Rig (COM5)...")
rig.reset_input_buffer()
rig.write(b'CHECK_DUT\n')
time.sleep(1.0)

while rig.in_waiting:
    line = rig.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("[RIG]:", line)

rig.close()
