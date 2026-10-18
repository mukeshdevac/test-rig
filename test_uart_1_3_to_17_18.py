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
    print("TESTING PHYSICAL UART CONNECTION:")
    print("  Test Rig (COM5) : TX=GPIO 1, RX=GPIO 3")
    print("  Test PCB (COM11): TX=GPIO 17, RX=GPIO 18")
    print("=" * 65)

    ser_dut = serial.Serial(DUT_PORT, BAUD, timeout=0.1, rtscts=False, dsrdtr=False)
    ser_dut.dtr = False
    ser_dut.rts = True
    time.sleep(0.1)
    ser_dut.rts = False
    time.sleep(0.1)

    print("\nWaiting for Test PCB to finish 10s countdown and launch /program/user.py...")
    start = time.time()
    while time.time() - start < 14.0:
        if ser_dut.in_waiting:
            line = ser_dut.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  [DUT] {line}")
                if "Awaiting Test Rig Handshake" in line:
                    break
        time.sleep(0.02)

    print("\n" + "#" * 65)
    print("Test PCB is now actively listening on RX (GPIO 18)!")
    print("Now opening COM5 (Test Rig GPIO 1 TX / GPIO 3 RX)...")
    print("#" * 65)

    ser_rig = serial.Serial(RIG_PORT, BAUD, timeout=0.1, rtscts=False, dsrdtr=False)
    ser_rig.dtr = False
    ser_rig.rts = False
    time.sleep(1.0)
    ser_rig.reset_input_buffer()
    ser_dut.reset_input_buffer()

    print("\n--- Test 1: Sending Handshake PING from COM5 (GPIO 1) -> Test PCB RX (GPIO 18) ---")
    ping_payload = b'{"cmd":"PING","from":"TEST_RIG"}\n'
    print(f"Sending on COM5: {ping_payload.strip().decode()}")
    ser_rig.write(ping_payload)
    ser_rig.flush()

    start = time.time()
    while time.time() - start < 3.0:
        if ser_dut.in_waiting:
            line = ser_dut.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  [DUT RECEIVED/LOG] {line}")
        if ser_rig.in_waiting:
            line = ser_rig.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  [RIG RECEIVED on GPIO 3] {line}")
        time.sleep(0.02)

    ser_rig.close()
    ser_dut.close()
    print("\nTest completed.")

if __name__ == "__main__":
    main()
