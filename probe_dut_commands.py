import serial
import time

dut = serial.Serial('COM11', 115200, timeout=1)
time.sleep(0.2)
dut.reset_input_buffer()

commands = [
    '{"cmd":"STATUS"}',
    '{"cmd":"PING"}',
    '{"cmd":"GET_STATUS"}',
    '{"cmd":"INFO"}',
    '{"cmd":"HELP"}'
]

for cmd in commands:
    print(f"Sending: {cmd}")
    dut.write((cmd + '\n').encode('utf-8'))
    time.sleep(0.4)
    resp = dut.read(dut.in_waiting or 200)
    print(f"Response: {repr(resp)}")

dut.close()
