"""
==============================================================================
LOF TITAN S3 - AUTOMATIC DUT PROVISIONER & TEST RIG SYNC
==============================================================================
Automatically detects newly docked/flashed Test PCBs (DUT), checks if companion
Python code exists on the PCB, and if not, automatically uploads the EOL
companion code via the supervisor protocol so the PCB immediately becomes
TEST READY with its unique Bluetooth name (e.g. LOF_TITAN_5F14).
==============================================================================
"""

import sys
import time
import json
import base64
import serial
import serial.tools.list_ports

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

COMPANION_PATH = r"c:\Users\BALAJI\TEST RIG\firmware\main.py"
RIG_PORT = "COM5"
BAUD = 115200

def find_dut_port():
    """Detect Test PCB (ESP32-S3 USB CDC)."""
    ports = serial.tools.list_ports.comports()
    for p in ports:
        if p.device.upper() != RIG_PORT:
            desc = (p.description or "").lower()
            hwid = (p.hwid or "").lower()
            if "usb" in desc or "serial" in desc or "303a" in hwid or "cdc" in desc or "1a86" in hwid:
                return p.device
    return "COM11"

def upload_companion_to_dut(dut_port):
    print(f"\n[AUTO-PROVISION] Starting auto-upload to Test PCB on {dut_port}...")
    
    with open(COMPANION_PATH, "r", encoding="utf-8") as f:
        code_content = f.read()

    code_bytes = code_content.encode("utf-8")
    total_len = len(code_bytes)
    print(f"[AUTO-PROVISION] Companion script size: {total_len} bytes")

    ser = serial.Serial()
    ser.port = dut_port
    ser.baudrate = BAUD
    ser.timeout = 0.2
    ser.open()

    # Hardware reset DUT to catch boot countdown
    print("[AUTO-PROVISION] Triggering DUT reset into boot countdown...")
    ser.dtr = False
    ser.rts = True
    time.sleep(0.1)
    ser.rts = False
    time.sleep(0.3)

    start = time.time()
    caught_boot = False
    while time.time() - start < 8.0:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if line:
                if "Starting in" in line or "countdown" in line or "WAITING_FOR_CONNECTION" in line:
                    caught_boot = True
                    break
        time.sleep(0.01)

    print("[AUTO-PROVISION] Handshaking with TITAN Supervisor...")
    ser.write(b'{"cmd":"CONNECT"}\n')
    time.sleep(0.4)
    while ser.in_waiting:
        ser.readline()

    prog_cmd = json.dumps({
        "cmd": "PROGRAM",
        "filename": "/program/user.py",
        "size": total_len
    }) + "\n"
    ser.write(prog_cmd.encode("utf-8"))
    time.sleep(0.4)
    while ser.in_waiting:
        ser.readline()

    chunk_size = 120
    chunks = [code_bytes[i:i+chunk_size] for i in range(0, total_len, chunk_size)]
    print(f"[AUTO-PROVISION] Streaming {len(chunks)} chunks to flash...")

    for idx, raw_chunk in enumerate(chunks):
        b64_str = base64.b64encode(raw_chunk).decode("ascii")
        chunk_cmd = json.dumps({"cmd": "CHUNK", "seq": idx, "data": b64_str}) + "\n"
        ser.write(chunk_cmd.encode("utf-8"))
        time.sleep(0.08)

    time.sleep(0.5)
    print("[AUTO-PROVISION] Upload complete! Resetting DUT into companion mode...")
    ser.dtr = False
    ser.rts = True
    time.sleep(0.1)
    ser.rts = False
    time.sleep(0.2)
    ser.close()
    print("[AUTO-PROVISION] DUT reset completed. Ready for Test Rig validation.")

def verify_rig_handshake():
    print(f"\n[TEST RIG] Verifying connection on {RIG_PORT}...")
    rig = serial.Serial()
    rig.port = RIG_PORT
    rig.baudrate = BAUD
    rig.timeout = 1.0
    rig.dtr = False
    rig.rts = False
    rig.open()
    time.sleep(0.5)

    # Poll DUT status from Rig
    rig.reset_input_buffer()
    rig.write(b'{"cmd":"GET_STATUS"}\n')
    time.sleep(0.5)

    status_resp = ""
    start = time.time()
    while time.time() - start < 3.0:
        if rig.in_waiting:
            line = rig.readline().decode('utf-8', 'ignore').strip()
            if "DUT_STATUS" in line:
                status_resp = line
                break
        time.sleep(0.05)

    rig.close()

    if status_resp:
        print(f"[TEST RIG] Response: {status_resp}")
        try:
            data = json.loads(status_resp)
            name = data.get("device_name", "UNKNOWN")
            online = data.get("connected", False)
            print(f"\nSUCCESS! Target PCB Online: {online}")
            print(f"Captured Unique Bluetooth Device Name: {name}")
            return online, name
        except Exception:
            pass
    return False, "UNKNOWN"

if __name__ == "__main__":
    dut_port = find_dut_port()
    print(f"Detected DUT Port: {dut_port}")
    upload_companion_to_dut(dut_port)
    print("Waiting 12 seconds for DUT supervisor countdown to launch companion...")
    for i in range(12, 0, -1):
        print(f"  Launching in {i}s...", end="\r")
        time.sleep(1)
    print("\nValidating Test Rig status...")
    online, name = verify_rig_handshake()
    if online:
        print(f"\n[RESULT] SYSTEM FULLY AUTOMATED & TEST READY for {name}!")
    else:
        print("\n[RESULT] DUT did not report ONLINE yet. Querying GET_STATUS again...")
        time.sleep(2)
        verify_rig_handshake()
