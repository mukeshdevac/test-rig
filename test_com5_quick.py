import serial
import time

rig = serial.Serial()
rig.port = 'COM5'
rig.baudrate = 115200
rig.dtr = False
rig.rts = False
rig.open()
time.sleep(0.2)
rig.reset_input_buffer()

print("Sending GET_STATUS to Rig...")
rig.write(b'{"cmd":"GET_STATUS"}\n')
time.sleep(0.5)

while rig.in_waiting:
    line = rig.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("[RIG]:", line)

print("\nSending PING to Rig...")
rig.write(b'{"cmd":"PING"}\n')
time.sleep(0.5)

while rig.in_waiting:
    line = rig.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("[RIG]:", line)

rig.close()
