import subprocess
import time
import serial

print("=" * 65)
print("PREPARING TO FLASH TEST RIG ON COM5")
print("1. Silencing Test PCB on COM11 to avoid UART bus contention...")
print("=" * 65)

try:
    dut = serial.Serial('COM11', 115200, timeout=0.2)
    dut.dtr = False
    dut.rts = True
    time.sleep(0.1)
    dut.rts = False
    time.sleep(0.8)
    dut.write(b'{"cmd":"CONNECT"}\n')
    time.sleep(0.5)
    dut.write(b'{"cmd":"EXEC","code":"import machine\\np=machine.Pin(17, machine.Pin.IN)\\n"}\n')
    time.sleep(0.5)
    dut.close()
    print("Test PCB on COM11 successfully paused in idle mode.")
except Exception as e:
    print(f"Note: Could not pause COM11: {e}")

time.sleep(1.0)
print("\n2. Launching arduino-cli upload to COM5...")
cli_path = r"C:\Users\BALAJI\AppData\Local\Programs\Arduino IDE\resources\app\lib\backend\resources\arduino-cli.exe"
sketch_path = r"c:\Users\BALAJI\TEST RIG\firmware\esp32_test_rig_firmware\esp32_test_rig_firmware.ino"

cmd = [cli_path, "upload", "-p", "COM5", "-b", "esp32:esp32:esp32", sketch_path]
proc = subprocess.run(cmd, capture_output=True, text=True)
print("STDOUT:\n", proc.stdout)
print("STDERR:\n", proc.stderr)
print(f"Exit code: {proc.returncode}")

if proc.returncode == 0:
    print("\nSUCCESS! Test Rig firmware v3.3.0 uploaded!")
    print("3. Resetting Test PCB so it starts companion program...")
    try:
        dut = serial.Serial('COM11', 115200, timeout=0.2)
        dut.dtr = False
        dut.rts = True
        time.sleep(0.1)
        dut.rts = False
        time.sleep(0.5)
        dut.close()
        print("Test PCB reset successfully.")
    except Exception as e:
        print(f"Reset error: {e}")
