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

print("Sending: {\"cmd\":\"BUZZER\"}")
ser.write(b'{"cmd":"BUZZER"}\n')
ser.flush()

start = time.time()
while time.time() - start < 3.0:
    if ser.in_waiting:
        raw = ser.readline().decode('utf-8', 'ignore').strip()
        if raw:
            print(f"[{time.strftime('%H:%M:%S')}] {raw}")
    time.sleep(0.02)

ser.close()
