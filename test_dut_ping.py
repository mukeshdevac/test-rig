import serial
import time

dut = serial.Serial()
dut.port = 'COM11'
dut.baudrate = 115200
dut.dtr = False
dut.rts = False
dut.open()

dut.write(b'{"cmd":"PING"}\n')
time.sleep(0.5)
res = dut.read(dut.in_waiting or 0)
print('Direct COM11 PING response:', repr(res))
dut.close()
