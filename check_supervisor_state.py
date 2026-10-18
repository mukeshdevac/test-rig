import serial
import time

dut = serial.Serial()
dut.port = 'COM11'
dut.baudrate = 115200
dut.dtr = False
dut.rts = False
dut.open()

for cmd in ['{"cmd":"STATUS"}', '{"cmd":"INFO"}', '{"cmd":"FILES"}']:
    print(f"Sending: {cmd}")
    dut.write((cmd + '\n').encode('utf-8'))
    time.sleep(0.4)
    print("Response:", repr(dut.read(dut.in_waiting or 0)))

dut.close()
