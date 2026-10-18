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

print("Rig and DUT ports opened.")
rig.reset_input_buffer()
dut.reset_input_buffer()

print("Sending PING from Rig...")
rig.write(b'{"cmd":"PING"}\n')
time.sleep(0.5)

print("DUT buffer on COM11:", repr(dut.read(dut.in_waiting or 0)))
print("Rig buffer on COM5:", repr(rig.read(rig.in_waiting or 0)))

rig.close()
dut.close()
