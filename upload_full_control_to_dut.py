import sys
import time
import json
import base64
import serial

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

DUT_PORT = r"\\.\COM11"
BAUD = 115200

def upload_to_dut():
    print("=" * 65)
    print("UPLOADING COMPANION TO TEST PCB (/program/user.py)")
    print(f"Port: {DUT_PORT} @ {BAUD} Baud")
    print("=" * 65)

    with open(r"c:\Users\BALAJI\TEST RIG\firmware\main.py", "r", encoding="utf-8") as f:
        code_content = f.read()

    code_bytes = code_content.encode("utf-8")
    total_len = len(code_bytes)
    print(f"Firmware size: {total_len} bytes")

    ser = serial.Serial(DUT_PORT, BAUD, timeout=0.2)
    ser.dtr = False
    ser.rts = True
    time.sleep(0.1)
    ser.rts = False
    time.sleep(0.2)

    start = time.time()
    while time.time() - start < 10.0:
        if ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if "Starting in" in line or "countdown" in line:
                break
        time.sleep(0.01)

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
        print("  [DUT]", ser.readline().decode('utf-8', 'ignore').strip())

    chunk_size = 120
    chunks = [code_bytes[i:i+chunk_size] for i in range(0, total_len, chunk_size)]
    print(f"Uploading {len(chunks)} chunks with sequence numbering...")

    for idx, raw_chunk in enumerate(chunks):
        b64_str = base64.b64encode(raw_chunk).decode("ascii")
        chunk_cmd = json.dumps({"cmd": "CHUNK", "seq": idx, "data": b64_str}) + "\n"
        ser.write(chunk_cmd.encode("utf-8"))
        time.sleep(0.1)
        while ser.in_waiting:
            line = ser.readline().decode('utf-8', 'ignore').strip()
            if "PROGRAM_READY" in line or "verified" in line:
                print("  [DUT]", line)

    time.sleep(0.5)
    while ser.in_waiting:
        print("  [DUT]", ser.readline().decode('utf-8', 'ignore').strip())

    print("Resetting Test PCB...")
    ser.dtr = False
    ser.rts = True
    time.sleep(0.1)
    ser.rts = False
    time.sleep(0.2)
    ser.close()
    print("Done! Companion will boot in 10s.")

if __name__ == "__main__":
    upload_to_dut()
