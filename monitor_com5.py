import serial
import time

ser = serial.Serial()
ser.port = 'COM5'
ser.baudrate = 115200
ser.dtr = False
ser.rts = False
ser.open()
time.sleep(0.2)

print("Listening on COM5 for 10 seconds...")
start = time.time()
last_ping = 0

while time.time() - start < 10.0:
    # Send PING every 2 seconds
    if time.time() - last_ping > 2.0:
        ser.write(b'{"cmd":"GET_STATUS"}\n')
        last_ping = time.time()

    if ser.in_waiting:
        line = ser.readline().decode('utf-8', 'ignore').strip()
        if line:
            print(f"[{time.strftime('%H:%M:%S')}] {line}")
    time.sleep(0.05)

ser.close()
