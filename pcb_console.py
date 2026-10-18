import sys
import time
import json
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

PORT = r"\\.\COM5"
BAUD = 115200

def send_cmd(ser, cmd_dict, timeout=1.2):
    payload = json.dumps(cmd_dict) + "\n"
    print(f"\n>>> SENDING TO TEST RIG: {payload.strip()}")
    ser.reset_input_buffer()
    ser.write(payload.encode('utf-8'))
    ser.flush()

    start = time.time()
    responses = []
    while time.time() - start < timeout:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"    <-- [REPLY] {line}")
                responses.append(line)
        time.sleep(0.02)
    return responses

def main():
    print("=" * 65)
    print("LOF TITAN S3 — TEST RIG TO TEST PCB MASTER CONSOLE")
    print(f"Connecting to Test Rig on {PORT}...")
    print("=" * 65)

    try:
        ser = serial.Serial(PORT, BAUD, timeout=0.2, rtscts=False, dsrdtr=False)
        ser.dtr = False
        ser.rts = False
    except Exception as e:
        print(f"Error opening {PORT}: {e}")
        return

    time.sleep(1.0)
    # Ping
    send_cmd(ser, {"cmd": "PING"})

    while True:
        print("\n" + "=" * 50)
        print("COMMAND MENU (Controlled through Test Rig COM5):")
        print("  1. Beep Test PCB Buzzer (2500Hz, 300ms)")
        print("  2. Turn ON Test PCB LED 1 (GPIO 47)")
        print("  3. Turn OFF Test PCB LED 1")
        print("  4. Turn ON Test PCB LED 2 (GPIO 48)")
        print("  5. Turn OFF Test PCB LED 2")
        print("  6. Pulse Motor M1 (Forward 150ms)")
        print("  7. Pulse Motor M2 (Forward 150ms)")
        print("  8. Pulse Motor M3 (Forward 150ms)")
        print("  9. Pulse Motor M4 (Forward 150ms)")
        print(" 10. Read All Sensor Inputs (S1-S5, US1, US2)")
        print(" 11. Run Onboard I2C Scan on Test PCB")
        print(" 12. Query Test PCB Chip & Memory Info")
        print(" 13. Query Unique Bluetooth Device Name (GET_DUT_NAME)")
        print(" 14. Run Full Automated EOL Test Suite")
        print("  0. Exit")
        print("=" * 50)

        try:
            choice = input("Enter choice (0-14): ").strip()
        except EOFError:
            break

        if choice == "1":
            send_cmd(ser, {"cmd": "BUZZER", "freq": 2500, "ms": 300})
        elif choice == "2":
            send_cmd(ser, {"cmd": "LED", "led": 1, "state": 1})
        elif choice == "3":
            send_cmd(ser, {"cmd": "LED", "led": 1, "state": 0})
        elif choice == "4":
            send_cmd(ser, {"cmd": "LED", "led": 2, "state": 1})
        elif choice == "5":
            send_cmd(ser, {"cmd": "LED", "led": 2, "state": 0})
        elif choice == "6":
            send_cmd(ser, {"cmd": "MOTOR", "motor": "M1", "dir": 1, "ms": 150})
        elif choice == "7":
            send_cmd(ser, {"cmd": "MOTOR", "motor": "M2", "dir": 1, "ms": 150})
        elif choice == "8":
            send_cmd(ser, {"cmd": "MOTOR", "motor": "M3", "dir": 1, "ms": 150})
        elif choice == "9":
            send_cmd(ser, {"cmd": "MOTOR", "motor": "M4", "dir": 1, "ms": 150})
        elif choice == "10":
            send_cmd(ser, {"cmd": "SENSOR"})
        elif choice == "11":
            send_cmd(ser, {"cmd": "I2C"})
        elif choice == "12":
            send_cmd(ser, {"cmd": "INFO"})
        elif choice == "13":
            send_cmd(ser, {"cmd": "GET_DUT_NAME"})
        elif choice == "14":
            send_cmd(ser, {"cmd": "START_TEST"}, timeout=2.5)
        elif choice == "0":
            break
        else:
            print("Invalid selection.")

    ser.close()
    print("Console closed.")

if __name__ == "__main__":
    main()
