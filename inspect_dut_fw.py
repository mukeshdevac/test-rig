import serial
import time

dut = serial.Serial()
dut.port = 'COM11'
dut.baudrate = 115200
dut.dtr = False
dut.rts = False
dut.open()

# Send command to execute raw python or inspect files
# Let's see: user.py is running. Can we tell user.py to execute python code or list files?
# Let's look at firmware/main.py!
dut.close()
