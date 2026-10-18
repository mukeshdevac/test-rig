import serial
import time

# Rig on COM5, DUT on COM11
rig = serial.Serial('COM5', 115200, timeout=0.5)
dut = serial.Serial('COM11', 115200, timeout=0.5)

print("Clearing buffers...")
rig.reset_input_buffer()
dut.reset_input_buffer()

# We can tell Rig to send raw bytes directly out of GPIO 1 (TX) to DUT GPIO 18 (RX)
# In firmware/esp32_test_rig_firmware.ino:
# When Rig receives CMD_FORWARD or test command, it writes to Serial.
# But wait! On the Test Rig (ESP32 DevKit):
# Serial is connected to BOTH the USB CP2102/CH340 chip AND to GPIO 1 (TX) / GPIO 3 (RX)!
# So any byte sent to COM5 is automatically transmitted on GPIO 1!
# And any byte received on GPIO 3 is sent to COM5!

print("Testing direct forwarding...")
# Let's send a supervisor command into Rig COM5:
test_msg = '{"cmd":"CONNECT"}\n'
print(f"Sending into Rig (COM5 -> GPIO 1 -> DUT GPIO 18): {test_msg.strip()}")
rig.write(test_msg.encode('utf-8'))

time.sleep(0.5)

# Check if DUT USB (COM11) saw anything or responded:
dut_data = dut.read(dut.in_waiting or 200)
print(f"DUT USB received/printed: {repr(dut_data)}")

# Check if Rig received anything back on GPIO 3 (from DUT GPIO 17):
rig_data = rig.read(rig.in_waiting or 200)
print(f"Rig COM5 received back: {repr(rig_data)}")

rig.close()
dut.close()
