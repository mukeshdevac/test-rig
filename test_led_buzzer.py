import serial
import time
import json

ser = serial.Serial()
ser.port = 'COM5'
ser.baudrate = 115200
ser.dtr = False
ser.rts = False
ser.open()
time.sleep(0.3)
ser.reset_input_buffer()

print("=" * 65)
print("TESTING LED & BUZZER CONTROLS ON TEST PCB THROUGH TEST RIG (COM5)")
print("=" * 65)

# 1. Test BUZZER trigger through Rig
print("\n[TEST 1] Triggering BUZZER on Test PCB (Forwarded via UART)...")
ser.write(b'{"cmd":"BUZZER","action":"BEEP","duration_ms":300}\n')
time.sleep(0.6)
while ser.in_waiting:
    print("  <--", ser.readline().decode('utf-8', 'ignore').strip())

# 2. Test BUZZER Pin (GPIO 19 on Rig probe bed)
print("\n[TEST 2] Pulsing BUZZER Probe Pin (Rig GPIO 19)...")
ser.write(b'{"cmd":"PIN_TEST","gpio":19}\n')
time.sleep(0.5)
while ser.in_waiting:
    print("  <--", ser.readline().decode('utf-8', 'ignore').strip())

# 3. Test LED 1 Pin (GPIO 2 on Rig probe bed / PCB GPIO 47)
print("\n[TEST 3] Pulsing LED 1 Probe Pin (Rig GPIO 2)...")
ser.write(b'{"cmd":"PIN_TEST","gpio":2}\n')
time.sleep(0.5)
while ser.in_waiting:
    print("  <--", ser.readline().decode('utf-8', 'ignore').strip())

# 4. Test LED 2 Pin (GPIO 4 on Rig probe bed / PCB GPIO 48)
print("\n[TEST 4] Pulsing LED 2 Probe Pin (Rig GPIO 4)...")
ser.write(b'{"cmd":"PIN_TEST","gpio":4}\n')
time.sleep(0.5)
while ser.in_waiting:
    print("  <--", ser.readline().decode('utf-8', 'ignore').strip())

ser.close()
print("\nDone.")
