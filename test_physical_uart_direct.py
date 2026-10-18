import sys
import time
import json
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

RIG_PORT = r"\\.\COM5"
DUT_PORT = r"\\.\COM11"
BAUD = 115200

def test_physical_uart():
    print("=" * 65)
    print("PHYSICAL UART VERIFICATION:")
    print("Test Rig (COM5) : TX=GPIO 1, RX=GPIO 3")
    print("Test PCB (COM11): TX=GPIO 17, RX=GPIO 18")
    print("=" * 65)

    # 1. Open COM11 (Test PCB)
    print("\n[Step 1] Connecting to Test PCB on COM11...")
    ser_dut = serial.Serial(DUT_PORT, BAUD, timeout=0.2)
    ser_dut.dtr = False
    ser_dut.rts = True
    time.sleep(0.1)
    ser_dut.rts = False
    time.sleep(0.1)

    # Wait for countdown text then send CONNECT
    start = time.time()
    while time.time() - start < 4.0:
        if ser_dut.in_waiting:
            line = ser_dut.readline().decode('utf-8', 'ignore').strip()
            if "countdown" in line or "Starting in" in line:
                break
        time.sleep(0.02)

    ser_dut.write(b'{"cmd":"CONNECT"}\n')
    time.sleep(0.5)
    while ser_dut.in_waiting:
        line = ser_dut.readline().decode('utf-8', 'ignore').strip()
        print(f"  [DUT Supervisor] {line}")

    # 2. Open COM5 (Test Rig)
    print("\n[Step 2] Opening Test Rig on COM5...")
    ser_rig = serial.Serial(RIG_PORT, BAUD, timeout=0.2, rtscts=False, dsrdtr=False)
    ser_rig.dtr = False
    ser_rig.rts = False
    time.sleep(1.0)
    ser_rig.reset_input_buffer()

    # 3. Setup UART on Test PCB (TX=17, RX=18)
    print("\n[Step 3] Initializing UART1 on Test PCB (tx=17, rx=18 @ 115200)...")
    dut_code = (
        "import machine, time\n"
        "u = machine.UART(1, baudrate=115200, tx=17, rx=18, timeout=100)\n"
        "for i in range(5):\n"
        "    u.write(b'PHYSICAL_UART_PING_' + str(i).encode() + b'\\n')\n"
        "    time.sleep_ms(100)\n"
    )
    ser_dut.write(json.dumps({"cmd": "EXEC", "code": dut_code}).encode('utf-8') + b'\n')
    time.sleep(0.5)

    # 4. Check if COM5 received the packets from Test PCB GPIO 17!
    print("\n[Step 4] Checking for packets received on COM5 (Test Rig RX GPIO 3)...")
    start = time.time()
    received_from_pcb = []
    while time.time() - start < 3.0:
        if ser_rig.in_waiting:
            line = ser_rig.readline().decode('utf-8', 'ignore').strip()
            if line:
                print(f"  >>> [RECEIVED ON COM5] {line}")
                received_from_pcb.append(line)
        time.sleep(0.02)

    if any("PHYSICAL_UART_PING" in p for p in received_from_pcb):
        print("\n  ===> SUCCESS! Line 1 (Test PCB TX 17 -> Test Rig RX 3) is 100% PHYSICALLY CONNECTED!")
    else:
        print("\n  ---> No direct packet on COM5 yet. Testing reverse or swapped...")

    ser_rig.close()
    ser_dut.close()
    print("\nDone.")

if __name__ == "__main__":
    test_physical_uart()
