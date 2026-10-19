import serial
import time
import sys
import json

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

PORT = 'COM5'
BAUD = 115200

print("=" * 70)
print("LOF TITAN S3 - OFFICIAL PIN DEFINITION & PERIPHERAL VERIFICATION")
print(f"Connecting to Test Rig on {PORT} @ {BAUD} Baud...")
print("=" * 70)

try:
    ser = serial.Serial(PORT, BAUD, timeout=0.1)
    ser.dtr = False
    ser.rts = False
except Exception as e:
    print(f"Error opening {PORT}: {e}")
    sys.exit(1)

time.sleep(1.0)
ser.reset_input_buffer()

def send_and_wait(desc, cmd, wait_time=0.8):
    print(f"\n>>> [{desc}] Sending: '{cmd}'")
    ser.write((cmd + '\n').encode('utf-8'))
    ser.flush()
    start = time.time()
    responses = []
    while time.time() - start < wait_time:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', errors='replace').strip()
            if line and not line.startswith('{"type":"TELEMETRY"'):
                print(f"  <-- {line}")
                responses.append(line)
        time.sleep(0.02)
    return responses

# 1. Ping / Identity Check
send_and_wait("PING Target PCB", '{"cmd":"PING"}', wait_time=1.0)

# 2. Indicators (Red & Green LEDs)
send_and_wait("Turn ON Red LED (GPIO 47)", "LED1_ON", wait_time=0.8)
send_and_wait("Turn OFF Red LED", "LED1_OFF", wait_time=0.5)

send_and_wait("Turn ON Green LED (GPIO 48)", "LED2_ON", wait_time=0.8)
send_and_wait("Turn OFF Green LED", "LED2_OFF", wait_time=0.5)

# 3. Buzzer (GPIO 20)
send_and_wait("Buzzer Beep (GPIO 20)", "BEEP", wait_time=0.8)

# 4. Push Buttons (GPIO 39, 40, 41, 42)
send_and_wait("Query 4 Push Buttons (39, 40, 41, 42)", '{"cmd":"BUTTONS"}', wait_time=1.0)

# 5. Sensor Ports (S1:2, S2:1, S3:3, S4:4, S5:5, US:6,19)
send_and_wait("Query Sensors (S1-S5 & Ultrasonic)", '{"cmd":"SENSOR"}', wait_time=1.0)

# 6. Motor Channels (M1:15,16; M2:13,14; M3/M6:11,12; M4/M5:9,10)
for m in ["M1", "M2", "M3", "M4"]:
    send_and_wait(f"Pulse Motor Channel {m}", f'{{"cmd":"MOTOR","motor":"{m}"}}', wait_time=0.8)

# 7. I2C Bus Scan (SDA:7, SCL:8)
send_and_wait("Scan I2C Bus (SDA:7, SCL:8)", '{"cmd":"I2C"}', wait_time=1.0)

# 8. Full Self-Test
send_and_wait("Run Full EOL Self-Test", "START_TEST", wait_time=2.0)

ser.close()
print("\n" + "=" * 70)
print("VERIFICATION COMPLETE")
print("=" * 70)
