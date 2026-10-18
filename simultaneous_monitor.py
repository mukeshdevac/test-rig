import serial
import time

rig = serial.Serial()
rig.port = 'COM5'
rig.baudrate = 115200
rig.dtr = False
rig.rts = False
rig.open()

dut = serial.Serial()
dut.port = 'COM11'
dut.baudrate = 115200
dut.dtr = False
dut.rts = False
dut.open()

print("Both COM5 and COM11 open.")
rig.reset_input_buffer()
dut.reset_input_buffer()

t0 = time.time()
while time.time() - t0 < 6:
    if rig.in_waiting:
        line = rig.readline().decode('utf-8', 'ignore').strip()
        if line:
            print(f"[RIG COM5]: {line}")
    if dut.in_waiting:
        line = dut.readline().decode('utf-8', 'ignore').strip()
        if line:
            print(f"  [DUT COM11]: {line}")
    time.sleep(0.01)

rig.close()
dut.close()
