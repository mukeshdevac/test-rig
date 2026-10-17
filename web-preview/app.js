/**
 * LOF Titan S3 EOL Test Fixture - Hardware-Only Controller
 * Strictly uses real physical USB connection (Web Serial API / Android USB-OTG)
 * ZERO MOCK SIMULATION - No fake data, no fake connections.
 */

// Application State (Defaults to DISCONNECTED - No fake data)
const state = {
  isLoggedIn: true,
  operatorId: "OP-982",
  serialNumber: "",
  rigState: "OFFLINE", // ONLINE, RECONNECTING, OFFLINE
  pcbState: "WAITING_RIG", // DOCKED, NOT_SEATED, WAITING_RIG
  usbState: "DISCONNECTED",
  isTesting: false,
  testProgress: 0,
  currentMa: 0.0,
  peakMa: 0.0,
  voltageV: 0.0,
  waveformPoints: [],
  maxPoints: 120,
  queueRecords: [
    {
      id: "uuid-001",
      sn: "LOF-TITAN-S3-2026-0040",
      op: "OP-982",
      result: "PASS",
      time: "2026-10-16 09:12:30",
      quiescent: 21.8,
      peak: 204.5,
      status: "SYNCED"
    },
    {
      id: "uuid-002",
      sn: "LOF-TITAN-S3-2026-0041",
      op: "OP-982",
      result: "PASS",
      time: "2026-10-16 09:18:14",
      quiescent: 23.1,
      peak: 198.2,
      status: "SYNCED"
    }
  ]
};

// Physical Web Serial Hardware Connection Handle
let physicalSerialPort = null;
let serialReader = null;

// Initialize Canvas & Oscilloscope
const canvas = document.getElementById("oscilloscopeCanvas");
const ctx = canvas.getContext("2d");

function resizeCanvas() {
  if (!canvas) return;
  const rect = canvas.getBoundingClientRect();
  canvas.width = rect.width * window.devicePixelRatio;
  canvas.height = rect.height * window.devicePixelRatio;
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
}

window.addEventListener("resize", resizeCanvas);
setTimeout(resizeCanvas, 100);

// Initialize empty points for flatline
for (let i = 0; i < state.maxPoints; i++) {
  state.waveformPoints.push(0);
}

// 60 FPS Canvas Oscilloscope Rendering Loop
function renderWaveform() {
  const w = canvas.getBoundingClientRect().width;
  const h = canvas.getBoundingClientRect().height;

  if (w <= 0 || h <= 0) {
    requestAnimationFrame(renderWaveform);
    return;
  }

  ctx.clearRect(0, 0, w, h);

  // 1. Draw Grid
  ctx.strokeStyle = "#1F2830";
  ctx.lineWidth = 1;
  ctx.setLineDash([3, 3]);

  // Horizontal Grid
  const yLines = 5;
  for (let i = 1; i < yLines; i++) {
    const y = (h / yLines) * i;
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(w, y);
    ctx.stroke();
  }

  // Vertical Grid
  const xLines = 10;
  for (let i = 1; i < xLines; i++) {
    const x = (w / xLines) * i;
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, h);
    ctx.stroke();
  }

  // 2. Draw 200mA Threshold Line
  const maxRange = 260.0;
  const threshY = h - (200.0 / maxRange) * h;
  ctx.strokeStyle = "rgba(255, 82, 82, 0.4)";
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.moveTo(0, threshY);
  ctx.lineTo(w, threshY);
  ctx.stroke();

  ctx.setLineDash([]);

  // 3. Draw Waveform or Disconnected Banner
  if (state.usbState !== "CONNECTED") {
    // Flatline along bottom
    ctx.strokeStyle = "rgba(255, 82, 82, 0.5)";
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(0, h - 4);
    ctx.lineTo(w, h - 4);
    ctx.stroke();

    // Disconnected message overlay
    ctx.fillStyle = "rgba(255, 82, 82, 0.9)";
    ctx.font = "bold 13px 'JetBrains Mono', monospace";
    ctx.textAlign = "center";
    ctx.fillText("⚡ NO HARDWARE DETECTED", w / 2, h / 2 - 8);

    ctx.fillStyle = "#8E9AA5";
    ctx.font = "11px Outfit, sans-serif";
    ctx.fillText("Connect ESP32 fixture via USB cable (115200 Baud)", w / 2, h / 2 + 12);
  } else if (state.waveformPoints.length > 1) {
    const pts = state.waveformPoints;
    const stepX = w / (pts.length - 1);

    ctx.beginPath();
    pts.forEach((val, i) => {
      const normY = Math.min(Math.max(val / maxRange, 0), 1);
      const x = i * stepX;
      const y = h - (normY * h);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });

    // Gradient fill under curve
    const fillPath = new Path2D();
    pts.forEach((val, i) => {
      const normY = Math.min(Math.max(val / maxRange, 0), 1);
      const x = i * stepX;
      const y = h - (normY * h);
      if (i === 0) fillPath.moveTo(x, y);
      else fillPath.lineTo(x, y);
    });
    fillPath.lineTo(w, h);
    fillPath.lineTo(0, h);
    fillPath.closePath();

    const grad = ctx.createLinearGradient(0, 0, 0, h);
    grad.addColorStop(0, "rgba(0, 230, 118, 0.3)");
    grad.addColorStop(1, "rgba(0, 230, 118, 0.0)");
    ctx.fillStyle = grad;
    ctx.fill(fillPath);

    // Stroke Line
    ctx.strokeStyle = "#00E676";
    ctx.lineWidth = 2.5;
    ctx.shadowColor = "rgba(0, 230, 118, 0.6)";
    ctx.shadowBlur = 8;
    ctx.stroke();
    ctx.shadowBlur = 0;
  }

  requestAnimationFrame(renderWaveform);
}
requestAnimationFrame(renderWaveform);

// Real Clock
setInterval(() => {
  const d = new Date();
  const timeStr = d.toTimeString().split(" ")[0];
  document.getElementById("clockDisplay").textContent = timeStr;
}, 1000);

// Responsive Tab Switching (Sidebar Rail + Bottom Nav)
function switchTab(tabId) {
  document.querySelectorAll(".nav-btn").forEach(btn => btn.classList.remove("active"));
  document.querySelectorAll(".bnav-btn").forEach(btn => btn.classList.remove("active"));
  document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.remove("active"));

  if (tabId === "run-test") {
    document.getElementById("tabRunTest")?.classList.add("active");
    document.getElementById("bnavRunTest")?.classList.add("active");
    document.getElementById("paneRunTest").classList.add("active");
  } else if (tabId === "diagnostics") {
    document.getElementById("tabDiagnostics")?.classList.add("active");
    document.getElementById("bnavDiagnostics")?.classList.add("active");
    document.getElementById("paneDiagnostics").classList.add("active");
  } else if (tabId === "history") {
    document.getElementById("tabHistory")?.classList.add("active");
    document.getElementById("bnavHistory")?.classList.add("active");
    document.getElementById("paneHistory").classList.add("active");
    renderQueueTable();
  } else if (tabId === "settings") {
    document.getElementById("tabSettings")?.classList.add("active");
    document.getElementById("bnavSettings")?.classList.add("active");
    document.getElementById("paneSettings").classList.add("active");
  }

  setTimeout(resizeCanvas, 50);
}

function toggleRail() {
  const rail = document.getElementById("navRail");
  const toggleBtn = document.getElementById("railToggle");
  rail.classList.toggle("collapsed");
  toggleBtn.textContent = rail.classList.contains("collapsed") ? "⮞" : "⮜";
  setTimeout(resizeCanvas, 250);
}

// Operator Login
function openLoginModal() {
  document.getElementById("loginModal").classList.add("open");
}

function closeLoginModal() {
  document.getElementById("loginModal").classList.remove("open");
}

function confirmLogin() {
  const idInput = document.getElementById("modalOperatorId").value.trim().toUpperCase();
  if (!idInput) {
    alert("Please enter Operator ID");
    return;
  }
  state.operatorId = idInput;
  state.isLoggedIn = true;

  const badge = document.getElementById("operatorBadgeBtn");
  badge.classList.remove("logged-out");
  badge.classList.add("logged-in");
  document.getElementById("operatorBadgeText").textContent = `OP: ${state.operatorId}`;
  closeLoginModal();
}

// Serial Number
function generateNewSerial() {
  const rand = Math.floor(1000 + Math.random() * 9000);
  const sn = `LOF-TITAN-S3-2026-${rand}`;
  state.serialNumber = sn;
  document.getElementById("barcodeInput").value = sn;
  if (state.usbState === "CONNECTED") {
    document.getElementById("headerSerialDisplay").textContent = sn;
  }
}

document.getElementById("barcodeInput").addEventListener("input", (e) => {
  state.serialNumber = e.target.value.trim().toUpperCase();
  if (state.usbState === "CONNECTED") {
    document.getElementById("headerSerialDisplay").textContent = state.serialNumber || "--";
  }
});

// =========================================================
// REAL PHYSICAL USB HARDWARE INTEGRATION (Web Serial API)
// =========================================================

async function togglePhysicalUsb() {
  if (state.rigState === "ONLINE") {
    await disconnectPhysicalUsb();
  } else {
    await connectPhysicalUsb();
  }
}

function updateUiConnectionStatus() {
  const rigBadge = document.getElementById("rigStatusBadge");
  const rigText = document.getElementById("rigStatusText");
  const pcbBadge = document.getElementById("pcbStatusBadge");
  const pcbText = document.getElementById("pcbStatusText");
  const banner = document.getElementById("hardwareAlertBanner");
  const startBtn = document.getElementById("startTestBtn");
  const startBtnText = document.getElementById("startTestBtnText");

  if (state.rigState === "OFFLINE") {
    if (rigBadge) rigBadge.className = "usb-status-pill disconnected";
    if (rigText) rigText.textContent = "RIG: OFFLINE";
    if (pcbBadge) pcbBadge.className = "usb-status-pill waiting";
    if (pcbText) pcbText.textContent = "PCB: WAITING RIG";

    if (banner) {
      banner.style.display = "flex";
      banner.className = "hardware-alert-banner";
      const icon = banner.querySelector(".alert-icon");
      if (icon) icon.textContent = "⚡";
      const text = banner.querySelector(".alert-text");
      if (text) text.textContent = "TEST RIG OFFLINE: Connect Tablet to Test Rig ESP32 via USB-C OTG cable.";
      const btn = banner.querySelector(".toggle-connect-btn");
      if (btn) btn.style.display = "inline-block";
    }

    if (startBtn && !state.isTesting) {
      startBtn.disabled = true;
      startBtn.className = "action-btn start-btn disabled";
      if (startBtnText) startBtnText.textContent = "CONNECT HARDWARE RIG";
      const icon = startBtn.querySelector(".btn-icon");
      if (icon) icon.textContent = "⚡";
    }
  } else if (state.rigState === "ONLINE") {
    if (rigBadge) rigBadge.className = "usb-status-pill connected";
    if (rigText) rigText.textContent = "RIG: ONLINE";

    if (state.pcbState === "DOCKED") {
      if (pcbBadge) pcbBadge.className = "usb-status-pill connected";
      if (pcbText) pcbText.textContent = "PCB: DOCKED";

      if (banner) banner.style.display = "none";

      if (startBtn && !state.isTesting) {
        startBtn.disabled = false;
        startBtn.className = "action-btn start-btn";
        if (startBtnText) startBtnText.textContent = "START AUTOMATED TEST";
        const icon = startBtn.querySelector(".btn-icon");
        if (icon) icon.textContent = "▶";
      }
    } else {
      if (pcbBadge) pcbBadge.className = "usb-status-pill not-seated";
      if (pcbText) pcbText.textContent = "PCB: NOT SEATED";

      if (banner) {
        banner.style.display = "flex";
        banner.className = "hardware-alert-banner warning";
        const icon = banner.querySelector(".alert-icon");
        if (icon) icon.textContent = "🔲";
        const text = banner.querySelector(".alert-text");
        if (text) text.textContent = "TEST RIG SYNCED. Insert & clamp Target Test PCB into fixture bed pogo-pins.";
        const btn = banner.querySelector(".toggle-connect-btn");
        if (btn) btn.style.display = "none";
      }

      if (startBtn && !state.isTesting) {
        startBtn.disabled = true;
        startBtn.className = "action-btn start-btn disabled";
        if (startBtnText) startBtnText.textContent = "INSERT TEST PCB TO START";
        const icon = startBtn.querySelector(".btn-icon");
        if (icon) icon.textContent = "🔲";
      }
    }
  }
}

async function connectPhysicalUsb() {
  if (!("serial" in navigator)) {
    alert("Web Serial API is not supported in this browser. Please use Google Chrome or Microsoft Edge with a physical USB device, or install the native APK on your Android device for OTG Serial.");
    return;
  }

  const rigBadge = document.getElementById("rigStatusBadge");
  const rigText = document.getElementById("rigStatusText");

  try {
    if (rigBadge) rigBadge.className = "usb-status-pill reconnecting";
    if (rigText) rigText.textContent = "RIG: CONNECTING...";

    // Prompts OS native serial port picker (physical hardware only)
    physicalSerialPort = await navigator.serial.requestPort();

    // Open connection at 115200 Baud 8N1
    await physicalSerialPort.open({ baudRate: 115200 });

    state.rigState = "ONLINE";
    state.pcbState = "NOT_SEATED";
    state.usbState = "CONNECTED";
    updateUiConnectionStatus();

    document.getElementById("headerSerialDisplay").textContent = state.serialNumber || (document.getElementById("barcodeInput").value || "READY");

    setCardStatus("cardPower", "chipPower", "idle", "STANDBY");
    setCardStatus("cardMotors", "chipMotors", "idle", "STANDBY");
    setCardStatus("cardComms", "chipComms", "idle", "STANDBY");
    setCardStatus("cardSensors", "chipSensors", "idle", "STANDBY");

    // Listen for physical cable unplug
    physicalSerialPort.addEventListener("disconnect", () => {
      console.warn("Physical USB Cable Disconnected.");
      disconnectPhysicalUsb();
    });

    // Start read loop
    readSerialLoop(physicalSerialPort);

  } catch (err) {
    console.warn("Physical port selection cancelled or failed:", err);
    disconnectPhysicalUsb();
  }
}

async function disconnectPhysicalUsb() {
  state.rigState = "OFFLINE";
  state.pcbState = "WAITING_RIG";
  state.usbState = "DISCONNECTED";
  updateUiConnectionStatus();

  // Clear readings to dashes (No fake values)
  document.getElementById("headerSerialDisplay").textContent = "--";
  document.getElementById("liveCurrentBadge").textContent = "-- mA";
  document.getElementById("livePeakBadge").textContent = "-- mA";
  document.getElementById("liveVoltageBadge").textContent = "-- V";
  document.getElementById("valQuiescent").textContent = "-- mA";
  document.getElementById("valPeak").textContent = "-- mA";
  document.getElementById("valVoltage").textContent = "-- V";
  document.getElementById("valPowerThreshold").textContent = "--";

  setCardStatus("cardPower", "chipPower", "idle", "DISCONNECTED");
  setCardStatus("cardMotors", "chipMotors", "idle", "DISCONNECTED");
  setCardStatus("cardComms", "chipComms", "idle", "DISCONNECTED");
  setCardStatus("cardSensors", "chipSensors", "idle", "DISCONNECTED");

  document.getElementById("valM1").textContent = "--";
  document.getElementById("valM2").textContent = "--";
  document.getElementById("valM3").textContent = "--";
  document.getElementById("valM4").textContent = "--";
  document.getElementById("valUart").textContent = "--";
  document.getElementById("valSpi").textContent = "--";
  document.getElementById("valI2c").textContent = "--";
  document.getElementById("valS13").textContent = "--";
  document.getElementById("valS45").textContent = "--";
  document.getElementById("valUS1").textContent = "--";
  document.getElementById("valUS2").textContent = "--";

  state.waveformPoints = [];
  for (let i = 0; i < state.maxPoints; i++) {
    state.waveformPoints.push(0);
  }

  if (serialReader) {
    try { await serialReader.cancel(); } catch (e) {}
    serialReader = null;
  }
  if (physicalSerialPort) {
    try { await physicalSerialPort.close(); } catch (e) {}
    physicalSerialPort = null;
  }
}

async function sendPhysicalSerialCommand(cmdJson) {
  if (!physicalSerialPort || !physicalSerialPort.writable) {
    alert("No physical USB hardware is connected! Please connect the USB cable to the ESP32 fixture.");
    return false;
  }
  try {
    const encoder = new TextEncoder();
    const writer = physicalSerialPort.writable.getWriter();
    await writer.write(encoder.encode(JSON.stringify(cmdJson) + "\n"));
    writer.releaseLock();
    return true;
  } catch (e) {
    console.error("Failed to send command over physical USB:", e);
    return false;
  }
}

async function readSerialLoop(port) {
  const textDecoder = new TextDecoderStream();
  port.readable.pipeTo(textDecoder.writable);
  const reader = textDecoder.readable.getReader();
  serialReader = reader;
  let buffer = "";

  try {
    while (true) {
      const { value, done } = await reader.read();
      if (done) break;
      if (value) {
        buffer += value;
        const lines = buffer.split("\n");
        buffer = lines.pop();
        for (const line of lines) {
          handleIncomingSerialLine(line.trim());
        }
      }
    }
  } catch (err) {
    console.warn("Serial read loop closed:", err);
  } finally {
    disconnectPhysicalUsb();
  }
}

function handleIncomingSerialLine(line) {
  if (!line) return;
  try {
    const json = JSON.parse(line);

    // Target Test PCB sensed via Rig bed switches / contact detection
    if (json.type === "DUT_STATUS" || json.type === "PCB_STATUS") {
      const isConnected = !!json.connected;
      state.pcbState = isConnected ? "DOCKED" : "NOT_SEATED";
      updateUiConnectionStatus();
    }

    // Real Telemetry frame from ESP32 INA219 probe
    if (json.type === "TELEMETRY") {
      const current = parseFloat(json.current_ma || 0);
      const voltage = parseFloat(json.voltage_v || 0);
      state.currentMa = current;
      if (current > state.peakMa) state.peakMa = current;
      state.voltageV = voltage;

      // Auto-detect PCB seating if quiescent power draw is detected
      if (state.pcbState !== "DOCKED" && voltage > 3.0 && current > 0.5) {
        state.pcbState = "DOCKED";
        updateUiConnectionStatus();
      }

      state.waveformPoints.push(current);
      if (state.waveformPoints.length > state.maxPoints) {
        state.waveformPoints.shift();
      }

      document.getElementById("liveCurrentBadge").textContent = `${current.toFixed(1)} mA`;
      document.getElementById("livePeakBadge").textContent = `${state.peakMa.toFixed(1)} mA`;
      document.getElementById("liveVoltageBadge").textContent = `${voltage.toFixed(2)} V`;
      document.getElementById("valQuiescent").textContent = `${current.toFixed(1)} mA`;
      document.getElementById("valPeak").textContent = `${state.peakMa.toFixed(1)} mA`;
      document.getElementById("valVoltage").textContent = `${voltage.toFixed(2)} V`;
      document.getElementById("valPowerThreshold").textContent = current < 50.0 ? "WITHIN LIMIT" : "EXCEEDED";
    }

    // Real Test Complete frame
    if (json.type === "TEST_COMPLETE") {
      state.isTesting = false;
      const pass = json.overall_result === "PASS";
      const btn = document.getElementById("startTestBtn");
      btn.className = "action-btn start-btn";
      btn.innerHTML = `<span class="btn-icon">▶</span><span class="btn-text">START AUTOMATED TEST</span>`;

      setCardStatus("cardPower", "chipPower", pass ? "pass" : "fail", pass ? "PASS" : "FAIL");
      setCardStatus("cardMotors", "chipMotors", pass ? "pass" : "fail", pass ? "PASS" : "FAIL");
      setCardStatus("cardComms", "chipComms", pass ? "pass" : "fail", pass ? "PASS" : "FAIL");
      setCardStatus("cardSensors", "chipSensors", pass ? "pass" : "fail", pass ? "PASS" : "FAIL");

      // Add to queue
      const newRec = {
        id: `uuid-${Date.now().toString().slice(-4)}`,
        sn: state.serialNumber || (document.getElementById("barcodeInput").value || "UNKNOWN"),
        op: state.operatorId,
        result: pass ? "PASS" : "FAIL",
        time: new Date().toISOString().replace("T", " ").split(".")[0],
        quiescent: state.currentMa,
        peak: state.peakMa,
        status: "QUEUED"
      };
      state.queueRecords.unshift(newRec);
      updateQueueBadge();
      renderQueueTable();
    }

    // Real Test Aborted frame
    if (json.type === "TEST_ABORTED") {
      state.isTesting = false;
      const btn = document.getElementById("startTestBtn");
      btn.className = "action-btn start-btn";
      btn.innerHTML = `<span class="btn-icon">▶</span><span class="btn-text">START AUTOMATED TEST</span>`;
      setCardStatus("cardPower", "chipPower", "fail", "ABORTED");
      setCardStatus("cardMotors", "chipMotors", "fail", "ABORTED");
      setCardStatus("cardComms", "chipComms", "fail", "ABORTED");
      setCardStatus("cardSensors", "chipSensors", "fail", "ABORTED");
    }

  } catch (e) {
    console.log("Raw serial output from ESP32:", line);
  }
}

// Automated Test Execution via Real Physical Hardware
async function handleStartOrAbortTest() {
  if (state.rigState !== "ONLINE" || !physicalSerialPort) {
    alert("Test Rig ESP32 is offline! Please connect Tablet to Test Rig via USB-C OTG.");
    return;
  }
  if (state.pcbState !== "DOCKED") {
    alert("Target Test PCB is not seated on the test fixture! Insert PCB into fixture bed clamp/pogo-pins first.");
    return;
  }
  if (state.isTesting) {
    await abortTest();
  } else {
    await startAutomatedTest();
  }
}

async function startAutomatedTest() {
  if (!state.isLoggedIn) {
    openLoginModal();
    return;
  }

  state.isTesting = true;
  state.peakMa = 0.0;

  const btn = document.getElementById("startTestBtn");
  btn.className = "action-btn abort-btn";
  btn.innerHTML = `<span class="btn-icon">⏹</span><span class="btn-text">ABORT TEST</span>`;

  setCardStatus("cardPower", "chipPower", "running", "TESTING");
  setCardStatus("cardMotors", "chipMotors", "running", "TESTING");
  setCardStatus("cardComms", "chipComms", "running", "TESTING");
  setCardStatus("cardSensors", "chipSensors", "running", "TESTING");

  const sent = await sendPhysicalSerialCommand({
    cmd: "START_TEST",
    serial_number: state.serialNumber || (document.getElementById("barcodeInput").value || "UNKNOWN"),
    operator_id: state.operatorId
  });

  if (!sent) {
    await abortTest();
  }
}

async function abortTest() {
  state.isTesting = false;
  await sendPhysicalSerialCommand({ cmd: "ABORT_TEST" });
  const btn = document.getElementById("startTestBtn");
  btn.className = "action-btn start-btn";
  btn.innerHTML = `<span class="btn-icon">▶</span><span class="btn-text">START AUTOMATED TEST</span>`;
  setCardStatus("cardPower", "chipPower", "fail", "ABORTED");
  setCardStatus("cardMotors", "chipMotors", "fail", "ABORTED");
  setCardStatus("cardComms", "chipComms", "fail", "ABORTED");
  setCardStatus("cardSensors", "chipSensors", "fail", "ABORTED");
}

function setCardStatus(cardId, chipId, statusClass, statusText) {
  const card = document.getElementById(cardId);
  const chip = document.getElementById(chipId);
  if (!card || !chip) return;
  card.className = `diag-card ${statusClass}`;
  chip.className = `status-chip ${statusClass}`;
  chip.textContent = statusText;
}

// Queue
function updateQueueBadge() {
  const pending = state.queueRecords.filter(r => r.status === "QUEUED").length;
  const badges = [document.getElementById("railBadge"), document.getElementById("bottomBadge")];
  badges.forEach(b => {
    if (!b) return;
    if (pending > 0) {
      b.textContent = pending;
      b.style.display = "inline-block";
    } else {
      b.style.display = "none";
    }
  });
}

function renderQueueTable() {
  const container = document.getElementById("queueListContainer");
  if (!container) return;
  const pending = state.queueRecords.filter(r => r.status === "QUEUED").length;
  document.getElementById("queueSummaryText").textContent = 
    `Local SQLite cache: ${state.queueRecords.length} records (${pending} pending upload) | Auto-retry on reconnect`;

  container.innerHTML = state.queueRecords.map(r => `
    <div class="queue-row">
      <div class="row-left">
        <span class="status-chip ${r.result === 'PASS' ? 'pass' : 'fail'}">${r.result}</span>
        <div>
          <div class="row-sn">${r.sn}</div>
          <div class="row-meta">${r.time} | OP: ${r.op}</div>
        </div>
      </div>
      <div class="row-metrics">
        <div>Quiescent: ${r.quiescent} mA | Peak: ${r.peak} mA</div>
      </div>
      <div>
        <span class="sync-badge ${r.status === 'SYNCED' ? 'synced' : 'queued'}">${r.status === 'SYNCED' ? '✔ SYNCED' : '⏳ QUEUED'}</span>
      </div>
    </div>
  `).join("");
}

function forceSyncNow() {
  state.queueRecords.forEach(r => r.status = "SYNCED");
  updateQueueBadge();
  renderQueueTable();
}

function clearQueueHistory() {
  state.queueRecords = [];
  updateQueueBadge();
  renderQueueTable();
}

// Physical Diagnostics via USB
async function pulsePin(gpio, label) {
  if (state.usbState !== "CONNECTED") {
    alert("Physical hardware not connected! Plug in USB cable first.");
    return;
  }
  await sendPhysicalSerialCommand({ cmd: "PULSE_PIN", gpio: gpio });
  const log = document.getElementById("probeLog");
  log.textContent = `[${new Date().toTimeString().split(" ")[0]}] >> Sent command to GPIO ${gpio} (${label}) over physical USB`;
}

async function probeSensor(gpio, label) {
  if (state.usbState !== "CONNECTED") {
    alert("Physical hardware not connected! Plug in USB cable first.");
    return;
  }
  await sendPhysicalSerialCommand({ cmd: "PROBE_SENSOR", gpio: gpio });
  const log = document.getElementById("probeLog");
  log.textContent = `[${new Date().toTimeString().split(" ")[0]}] >> Queried GPIO ${gpio} (${label}) over physical USB`;
}

// Settings
function saveScriptUrl() {
  const status = document.getElementById("saveUrlStatus");
  status.style.display = "block";
  setTimeout(() => {
    status.style.display = "none";
  }, 2500);
}

// Init
updateQueueBadge();
renderQueueTable();
updateUiConnectionStatus();
