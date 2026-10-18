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

def send_and_read(cmd, wait_sec=1.5):
    print(f"\n---> Sending: {cmd}")
    ser.write((cmd + '\n').encode('utf-8'))
    t0 = time.time()
    got_reply = False
    while time.time() - t0 < wait_sec:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  <--- {line}")
                got_reply = True
        time.sleep(0.02)
    if not got_reply:
        print("  (No direct response within timeout)")

# 1. Test BUZZER
send_and_read('{"cmd":"BUZZER","action":"BEEP"}', wait_sec=2.0)

# 2. Test PIN_TEST GPIO 19 (Rig Buzzer Pin)
send_and_read('{"cmd":"PIN_TEST","gpio":19}', wait_sec=1.5)

# 3. Test PIN_TEST GPIO 2 (Rig LED1 Pin)
send_and_read('{"cmd":"PIN_TEST","gpio":2}', wait_sec=1.5)

# 4. Test PIN_TEST GPIO 4 (Rig LED2 Pin)
send_and_read('{"cmd":"PIN_TEST","gpio":4}', wait_sec=1.5)

# 5. Check DUT status
send_and_read('CHECK_DUT', wait_sec=1.5)

ser.close()
