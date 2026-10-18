import sys
import time
import json
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

PORT = r"\\.\COM5"
BAUD = 115200

def main():
    print("=" * 65)
    print("TEST RIG QUERY: TARGET PCB UNIQUE BLUETOOTH / HARDWARE NAME")
    print(f"Connecting to Test Rig on {PORT}...")
    print("=" * 65)

    ser = serial.Serial(PORT, BAUD, timeout=0.2, rtscts=False, dsrdtr=False)
    ser.dtr = False
    ser.rts = False

    print("Waiting 12 seconds for Test PCB to finish autorun countdown...")
    start = time.time()
    last_ping = 0
    while time.time() - start < 13.0:
        if time.time() - last_ping > 1.0:
            last_ping = time.time()
            ser.write(b'{"cmd":"PING"}\n')
            ser.flush()
        while ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if "PONG" in line:
                print(f"  [HEARTBEAT] {line}")
        time.sleep(0.05)

    print("\n" + "#" * 65)
    print("SENDING CHECK_DUT / HANDSHAKE TO CAPTURE UNIQUE PCB NAME...")
    print("#" * 65)

    ser.reset_input_buffer()
    ser.write(b'CHECK_DUT\n')
    ser.flush()

    start = time.time()
    captured_name = None
    while time.time() - start < 3.0:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  <<< {line}")
                if "device_name" in line:
                    try:
                        data = json.loads(line)
                        if "device_name" in data:
                            captured_name = data["device_name"]
                    except Exception:
                        pass
        time.sleep(0.02)

    print("\n--- Triggering Automated Test with Unique Tagging ---")
    ser.write(b'START_TEST\n')
    ser.flush()
    start = time.time()
    while time.time() - start < 3.0:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  <<< [TEST REPORT] {line}")
        time.sleep(0.02)

    ser.close()
    print("\n" + "=" * 65)
    print(f"CAPTURED TARGET PCB UNIQUE NAME: {captured_name}")
    print("=" * 65)

if __name__ == "__main__":
    main()
