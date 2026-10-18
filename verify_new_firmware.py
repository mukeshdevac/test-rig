import sys
import time
import json
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

RIG_PORT = r"\\.\COM5"
DUT_PORT = r"\\.\COM11"
BAUD = 115200

def test_new_firmware():
    print("=" * 65)
    print("VERIFYING UPDATED TEST RIG FIRMWARE & HARDWIRED UART LINK")
    print(f"Test Rig : {RIG_PORT} (Hardwired TX=GPIO 1, RX=GPIO 3)")
    print(f"Test PCB : {DUT_PORT} (MicroPython TX=GPIO 17, RX=GPIO 18)")
    print("=" * 65)

    # 1. Check Test Rig on COM5
    print("\n[Step 1] Connecting to Test Rig on COM5...")
    try:
        ser_rig = serial.Serial(RIG_PORT, BAUD, timeout=0.1, rtscts=False, dsrdtr=False)
        ser_rig.dtr = False
        ser_rig.rts = False
    except Exception as e:
        print(f"Failed to open {RIG_PORT}: {e}")
        return

    time.sleep(1.0)
    # Check boot banner or send PING
    ser_rig.write(b'{"cmd":"PING"}\n')
    time.sleep(0.5)
    while ser_rig.in_waiting:
        line = ser_rig.readline().decode('utf-8', 'ignore').strip()
        print(f"  [RIG] {line}")

    # 2. Check Test PCB on COM11
    print("\n[Step 2] Resetting Test PCB (COM11) to boot into EOL companion...")
    try:
        ser_dut = serial.Serial(DUT_PORT, BAUD, timeout=0.1, rtscts=False, dsrdtr=False)
        ser_dut.dtr = False
        ser_dut.rts = True
        time.sleep(0.1)
        ser_dut.rts = False
        time.sleep(0.1)
    except Exception as e:
        print(f"Failed to open {DUT_PORT}: {e}")
        ser_rig.close()
        return

    # Wait for companion to start
    start = time.time()
    dut_ready = False
    last_ping = 0

    while time.time() - start < 15.0:
        if time.time() - last_ping > 1.0:
            last_ping = time.time()
            ser_rig.write(b'{"cmd":"PING"}\n')

        while ser_rig.in_waiting:
            line = ser_rig.readline().decode('utf-8', 'ignore').strip()
            if "PONG" in line:
                print(f"  [RIG STATUS] {line}")

        while ser_dut.in_waiting:
            line = ser_dut.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  [DUT] {line}")
                if "Awaiting Test Rig Handshake" in line:
                    dut_ready = True
                    break
        if dut_ready:
            break
        time.sleep(0.02)

    if not dut_ready:
        print("  DUT did not signal ready.")
    else:
        print("\n" + "#" * 65)
        print("TARGET PCB COMPANION IS READY! TESTING HARDWIRED UART LINK...")
        print("#" * 65)

        # 3. Send CHECK_DUT to Test Rig
        print("\n[Step 3] Sending CHECK_DUT to Test Rig...")
        ser_rig.write(b"CHECK_DUT\n")
        time.sleep(0.6)

        start = time.time()
        while time.time() - start < 2.0:
            while ser_rig.in_waiting:
                print(f"  >>> [RIG OUT] {ser_rig.readline().decode('utf-8', 'ignore').strip()}")
            while ser_dut.in_waiting:
                print(f"  <<< [DUT OUT] {ser_dut.readline().decode('utf-8', 'ignore').strip()}")
            time.sleep(0.02)

        # 4. Trigger START_TEST
        print("\n[Step 4] Triggering automated START_TEST...")
        ser_rig.write(b"START_TEST\n")
        
        start = time.time()
        while time.time() - start < 4.0:
            while ser_rig.in_waiting:
                print(f"  >>> [RIG OUT] {ser_rig.readline().decode('utf-8', 'ignore').strip()}")
            while ser_dut.in_waiting:
                print(f"  <<< [DUT OUT] {ser_dut.readline().decode('utf-8', 'ignore').strip()}")
            time.sleep(0.02)

    ser_rig.close()
    ser_dut.close()
    print("\n" + "=" * 65)
    print("TEST COMPLETE")
    print("=" * 65)

if __name__ == "__main__":
    test_new_firmware()
