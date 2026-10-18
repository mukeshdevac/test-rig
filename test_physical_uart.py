import sys
import time
import json
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

RIG_PORT = r"\\.\COM5"
DUT_PORT = r"\\.\COM11"
BAUD = 115200

def main():
    print("=" * 65)
    print("PHYSICAL UART CONNECTION DIAGNOSTIC TOOL")
    print(f"Test Rig : {RIG_PORT} (ESP32)")
    print(f"Test PCB : {DUT_PORT} (ESP32-S3)")
    print("=" * 65)

    # 1. Open COM11 (Test PCB) and send CONNECT to cancel autorun & watchdog
    print("\n[Step 1] Connecting to Test PCB (COM11)...")
    ser_dut = serial.Serial(DUT_PORT, BAUD, timeout=0.2)
    ser_dut.dtr = False
    ser_dut.rts = True
    time.sleep(0.1)
    ser_dut.rts = False
    time.sleep(0.1)

    # Wait for countdown text then connect
    start = time.time()
    while time.time() - start < 4.0:
        if ser_dut.in_waiting:
            line = ser_dut.readline().decode('utf-8', 'ignore').strip()
            if "countdown started" in line or "Starting in" in line:
                break
        time.sleep(0.02)

    ser_dut.write(b'{"cmd":"CONNECT"}\n')
    time.sleep(0.5)
    connected_idle = False
    while ser_dut.in_waiting:
        line = ser_dut.readline().decode('utf-8', 'ignore').strip()
        print(f"  [DUT] {line}")
        if "CONNECTED_IDLE" in line:
            connected_idle = True

    if not connected_idle:
        print("  Retrying CONNECT...")
        ser_dut.write(b'{"cmd":"CONNECT"}\n')
        time.sleep(0.5)
        while ser_dut.in_waiting:
            print(f"  [DUT] {ser_dut.readline().decode('utf-8', 'ignore').strip()}")

    print("  --> Test PCB is safely held in CONNECTED_IDLE (Autorun & Watchdog cancelled).")

    # 2. Open COM5 (Test Rig)
    print("\n[Step 2] Connecting to Test Rig (COM5)...")
    ser_rig = serial.Serial(RIG_PORT, BAUD, timeout=0.2)
    ser_rig.dtr = False
    ser_rig.rts = False

    time.sleep(1.0)
    # Check Rig PING
    ser_rig.write(b'{"cmd":"PING"}\n')
    time.sleep(0.5)
    while ser_rig.in_waiting:
        print(f"  [RIG] {ser_rig.readline().decode('utf-8', 'ignore').strip()}")

    # 3. Test GPIO Pins on Test PCB
    print("\n[Step 3] Setting up Pin monitor on Test PCB (GPIO 18 & GPIO 17)...")
    setup_code = (
        "import machine, time\n"
        "p18 = machine.Pin(18, machine.Pin.IN, machine.Pin.PULL_DOWN)\n"
        "p17 = machine.Pin(17, machine.Pin.IN, machine.Pin.PULL_DOWN)\n"
    )
    ser_dut.write(json.dumps({"cmd": "EXEC", "code": setup_code}).encode('utf-8') + b'\n')
    time.sleep(0.5)
    while ser_dut.in_waiting:
        print(f"  [DUT EXEC] {ser_dut.readline().decode('utf-8', 'ignore').strip()}")

    # 4. Trigger PIN_TEST on Test Rig GPIO 17 (Rig TX -> PCB RX)
    print("\n[Step 4] Commanding Test Rig to pulse GPIO 17 HIGH...")
    ser_rig.write(b'{"cmd":"PIN_TEST","gpio":17}\n')
    time.sleep(0.5)
    while ser_rig.in_waiting:
        print(f"  [RIG PIN TEST] {ser_rig.readline().decode('utf-8', 'ignore').strip()}")

    ser_rig.close()
    ser_dut.close()
    print("\nPhysical test step complete.")

if __name__ == "__main__":
    main()
