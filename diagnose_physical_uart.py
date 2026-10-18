import sys
import time
import json
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

RIG_PORT = r"\\.\COM5"
DUT_PORT = r"\\.\COM11"
BAUD = 115200

def test_uart_physical():
    print("=" * 70)
    print("PHYSICAL UART WIRING & SIGNAL CONTINUITY TEST")
    print("=" * 70)
    print("Targeted Wiring:")
    print("  [Link A] Test Rig TX (GPIO 1)  --->  Test PCB RX (GPIO 18)")
    print("  [Link B] Test PCB TX (GPIO 17) --->  Test Rig RX (GPIO 3)")
    print("  [Link C] Common Ground (GND)   <---> Common Ground (GND)")
    print("=" * 70)

    # 1. Open COM11 (Test PCB)
    print("\n[1/4] Connecting to Test PCB (COM11)...")
    ser_dut = serial.Serial(DUT_PORT, BAUD, timeout=0.1, rtscts=False, dsrdtr=False)
    ser_dut.dtr = False
    ser_dut.rts = True
    time.sleep(0.1)
    ser_dut.rts = False
    time.sleep(0.1)

    # Wait for countdown
    start = time.time()
    while time.time() - start < 13.0:
        if ser_dut.in_waiting:
            line = ser_dut.readline().decode('utf-8', 'ignore').strip()
            if "Awaiting Test Rig Handshake" in line:
                print(f"  [DUT] Companion running and listening on UART (GPIO 18 RX)!")
                break
        time.sleep(0.05)

    # 2. Open COM5 (Test Rig)
    print("\n[2/4] Connecting to Test Rig (COM5)...")
    ser_rig = serial.Serial(RIG_PORT, BAUD, timeout=0.1, rtscts=False, dsrdtr=False)
    ser_rig.dtr = False
    ser_rig.rts = False
    time.sleep(1.0)
    ser_rig.reset_input_buffer()
    ser_dut.reset_input_buffer()

    # 3. Test Link A: Send PING frame from Rig GPIO 1 to PCB GPIO 18
    print("\n[3/4] Testing Link A: Test Rig (GPIO 1) -> Test PCB (GPIO 18)...")
    link_a_pass = False
    for attempt in range(1, 4):
        print(f"  Attempt {attempt}: Transmitting frame from COM5...")
        ser_rig.write(b'{"cmd":"PING","from":"TEST_RIG"}\n')
        ser_rig.flush()
        
        start = time.time()
        while time.time() - start < 1.0:
            if ser_dut.in_waiting:
                chunk = ser_dut.read(ser_dut.in_waiting).decode('utf-8', 'ignore')
                print(f"    [DUT RX LOG] {chunk.strip()}")
                if "PING" in chunk or "Decode" in chunk or "Sent PONG" in chunk:
                    link_a_pass = True
            time.sleep(0.05)
        if link_a_pass:
            break

    # 4. Results
    print("\n[4/4] Physical Continuity Results:")
    print("-" * 50)
    if link_a_pass:
        print("  [Link A: Rig TX -> PCB RX] : PASS (Electrical signal detected on GPIO 18!)")
    else:
        print("  [Link A: Rig TX -> PCB RX] : NO SIGNAL DETECTED")

    ser_rig.close()
    ser_dut.close()
    print("=" * 70)

if __name__ == "__main__":
    test_uart_physical()
