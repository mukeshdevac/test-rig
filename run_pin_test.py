import serial
import time

ser = serial.Serial()
ser.port = 'COM5'
ser.baudrate = 115200
ser.dtr = False
ser.rts = False
ser.open()
time.sleep(0.3)
ser.reset_input_buffer()

for pin in [19, 2, 4]:
    cmd = f'{{"cmd":"PIN_TEST","gpio":{pin}}}\n'
    print(f"Sending: {cmd.strip()}")
    ser.write(cmd.encode('utf-8'))
    ser.flush()
    start_time = time.time()
    while time.time() - start_time < 0.8:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  <-- {line}")
        time.sleep(0.01)

ser.close()
