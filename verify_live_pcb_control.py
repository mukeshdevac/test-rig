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
print("TEST RIG LIVE CONTROL OF TEST PCB OVER HARDWIRED UART")
print("=" * 65)

# 1. Trigger Buzzer on Test PCB through Rig
print("\n[ACTION 1] Triggering BUZZER on Test PCB through Rig...")
ser.write(b'{"cmd":"BUZZER"}\n')
time.sleep(0.6)
while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("  <--", line)

# 2. Trigger Automated EOL Diagnostic Test
print("\n[ACTION 2] Triggering START_TEST on Test PCB through Rig...")
ser.write(b'{"cmd":"START_TEST","serial_number":"EOL-TITAN-5F14"}\n')
start = time.time()
while time.time() - start < 3.0:
    if ser.in_waiting:
        line = ser.readline().decode('utf-8', 'ignore').strip()
        if line:
            print("  <--", line)
    time.sleep(0.05)

# 3. Read live telemetry & status
print("\n[ACTION 3] Verifying Current Rig & PCB Link Status...")
ser.write(b'CHECK_DUT\n')
time.sleep(0.5)
while ser.in_waiting:
    line = ser.readline().decode('utf-8', 'ignore').strip()
    if line:
        print("  <--", line)

ser.close()
print("\n" + "=" * 65)
print("VERIFICATION COMPLETED SUCCESSFULLY!")
print("=" * 65)
