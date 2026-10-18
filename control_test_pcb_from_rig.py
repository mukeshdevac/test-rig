import sys
import time
import json
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

RIG_PORT = r"\\.\COM5"
BAUD = 115200

def send_and_wait(ser, cmd_str, timeout=1.5):
    """Sends command through Test Rig and waits for reply from Test PCB"""
    print(f"\n[SEND] {cmd_str.strip()}")
    ser.reset_input_buffer()
    ser.write(cmd_str.encode('utf-8') + b'\n')
    ser.flush()

    start = time.time()
    responses = []
    while time.time() - start < timeout:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  <-- {line}")
                responses.append(line)
        time.sleep(0.02)
    return responses

def main():
    print("=" * 70)
    print("TEST RIG MASTER CONTROLLER: FULL CONTROL OF TEST PCB")
    print(f"Communicating through: {RIG_PORT} (Hardwired UART TX:1, RX:3)")
    print("=" * 70)

    try:
        ser = serial.Serial(RIG_PORT, BAUD, timeout=0.1, rtscts=False, dsrdtr=False)
        ser.dtr = False
        ser.rts = False
    except Exception as e:
        print(f"Failed to open {RIG_PORT}: {e}")
        return

    print("Waiting 12 seconds for Test PCB to finish startup countdown...")
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
                print(f"  [RIG HEARTBEAT] {line}")
        time.sleep(0.05)

    print("\n" + "#" * 70)
    print("TEST RIG CONNECTED! TAKING FULL CONTROL OF TEST PCB PERIPHERALS...")
    print("#" * 70)

    # 1. PING & Handshake
    print("\n--- 1. PING / HANDSHAKE ---")
    send_and_wait(ser, '{"cmd":"PING"}', timeout=1.0)

    # 2. Get Test PCB System & Chip Info
    print("\n--- 2. QUERY SYSTEM & HARDWARE INFO ---")
    send_and_wait(ser, '{"cmd":"INFO"}', timeout=1.2)

    # 3. Test Buzzer on Test PCB
    print("\n--- 3. TEST PCB BUZZER TRIGGER (2500 Hz, 250ms) ---")
    send_and_wait(ser, '{"cmd":"BUZZER","freq":2500,"ms":250}', timeout=1.0)

    # 4. Test LEDs on Test PCB
    print("\n--- 4. TEST PCB LED CONTROL (Turn on LED 1) ---")
    send_and_wait(ser, '{"cmd":"LED","led":1,"state":1}', timeout=0.8)
    time.sleep(0.3)
    print("--- Turning on LED 2 ---")
    send_and_wait(ser, '{"cmd":"LED","led":2,"state":1}', timeout=0.8)
    time.sleep(0.3)
    print("--- Turning off LEDs ---")
    send_and_wait(ser, '{"cmd":"LED","led":1,"state":0}', timeout=0.5)
    send_and_wait(ser, '{"cmd":"LED","led":2,"state":0}', timeout=0.5)

    # 5. Read All Digital Sensor Inputs
    print("\n--- 5. READ REAL-TIME SENSORS (S1-S5, Ultrasonic) ---")
    send_and_wait(ser, '{"cmd":"SENSOR"}', timeout=1.0)

    # 6. Run Onboard I2C Scan
    print("\n--- 6. RUN ONBOARD I2C SCAN ON TEST PCB ---")
    send_and_wait(ser, '{"cmd":"I2C"}', timeout=1.2)

    # 7. Motor Driver Pulse Tests (M1, M2, M3, M4)
    for m in ["M1", "M2", "M3", "M4"]:
        print(f"\n--- 7. PULSE MOTOR DRIVER {m} (100ms) ---")
        send_and_wait(ser, f'{{"cmd":"MOTOR","motor":"{m}","dir":1,"ms":100}}', timeout=0.8)

    # 8. Run Full Automated Test Suite
    print("\n--- 8. RUN FULL AUTOMATED EOL TEST SUITE ---")
    send_and_wait(ser, '{"cmd":"START_TEST"}', timeout=2.5)

    ser.close()
    print("\n" + "=" * 70)
    print("FULL CONTROL DEMONSTRATION COMPLETE")
    print("=" * 70)

if __name__ == "__main__":
    main()
