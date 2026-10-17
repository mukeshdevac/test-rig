# LOF Titan S3 — End-of-Line (EOL) Android PCB Test Fixture

A production-grade, highly polished Android Tablet application engineered for electronics assembly lines. It serves as the operator interface and test runner for the **LOF Titan S3 PCB Test Fixture**, communicating via hardwired **USB-C OTG Serial (115200 Baud)** with an **ESP32 Test Rig**.

---

## 📸 Key Features & Architecture

- **Bambu Lab-Inspired Industrial UI Aesthetic:**
  - Sleek dark theme (`#121212` background, `#1E1E1E` card surfaces, `#00E676` neon green pass accent, `#FFD600` warning amber, `#FF5252` error coral).
  - High touch target design ($\ge 48\,\text{dp}$) optimized for 8"–10" Lenovo Android Tablets.
  - Collapsible Left Navigation Rail: `[Run Test]`, `[Pin Diagnostics]`, `[Sheets Queue]`, `[Settings]`.
  - Dynamic Top Status Bar: Operator Badge, Active PCB Serial Number, Pulsating USB State Badge, and Live Clock.

- **Real-Time Oscilloscope Telemetry:**
  - High-performance Compose Canvas rendering live INA219 current draw ($\text{mA}$ vs. time) with gridlines, peak current capture, and threshold line.

- **Self-Healing USB OTG Serial Engine:**
  - Built with `usb-serial-for-android` at 115200 Baud (8N1).
  - Background Heartbeat Loop (`{"cmd": "PING"}` every 1000ms).
  - If 3 heartbeats fail or cable is disconnected, the app transitions smoothly to `RECONNECTING...` without blocking the UI thread.
  - Active background reconnect poller scans USB VID/PIDs every 500ms and restores the session automatically upon re-plugging.

- **Offline-First Google Sheets Sync Pipeline:**
  - Every completed test is serialized and stored in a local **Room (SQLite)** database.
  - An Android **WorkManager** background worker reads pending records and performs HTTP POST requests to a Google Apps Script Web App.
  - Automatically retries with exponential backoff on Wi-Fi drops, marking records as `SYNCED` upon HTTP 200 `SUCCESS`.

- **Built-in Mock Simulator:**
  - Toggle simulation mode in **Settings** to generate synthetic USB serial frames, realistic current curves, and pass/fail diagnostic responses without physical hardware attached.

---

## 🔌 Hardware Pin Mapping (LOF Titan S3 PCB)

| Subsystem | Hardware Probing Layout |
| :--- | :--- |
| **Power Probe** | INA219 High-Side Current & Bus Voltage Sensor (<50mA Quiescent limit) |
| **UART Loopback** | TX (GPIO 17) $\to$ RX (GPIO 18) @ 115200 Baud |
| **SPI Loopback** | SCK (GPIO 35), MOSI (GPIO 36), MISO (GPIO 37), CS (GPIO 38) |
| **I2C Bus Scan** | SCL (GPIO 8), SDA (GPIO 7) |
| **Motor Drivers** | M1 (GPIO 15/16), M2 (GPIO 13/14), M3 (GPIO 11/12), M4 (GPIO 9/10) |
| **Indicators** | LED1 (GPIO 47), LED2 (GPIO 48), Buzzer (GPIO 20) |
| **Sensors** | S1 (GPIO 2), S2 (GPIO 1), S3 (GPIO 3), S4 (GPIO 4), S5 (GPIO 5), Ultrasonic S1 (GPIO 6), Ultrasonic S2 (GPIO 19) |

---

## 📁 Project Structure

```
test android/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/
│       │   ├── xml/device_filter.xml
│       │   └── values/{colors, strings, themes}.xml
│       └── java/com/loftitan/eoltestfixture/
│           ├── MainActivity.kt
│           ├── EolApplication.kt
│           ├── domain/
│           │   ├── model/{TestModels.kt, PinMapping.kt}
│           │   └── orchestrator/TestSuiteRunner.kt
│           ├── hardware/
│           │   ├── ISerialPortManager.kt
│           │   ├── UsbSerialManager.kt
│           │   ├── MockSerialSimulator.kt
│           │   └── HeartbeatStateMachine.kt
│           ├── data/
│           │   ├── local/{TestRecordEntity.kt, TestRecordDao.kt, AppDatabase.kt}
│           │   ├── sync/{GoogleSheetsApiClient.kt, GoogleSheetsSyncWorker.kt}
│           │   └── repository/TestFixtureRepository.kt
│           └── ui/
│               ├── theme/{Color.kt, Theme.kt, Type.kt}
│               ├── components/{TopStatusBar.kt, NavigationRailView.kt, DiagnosticCard.kt, OscilloscopeWaveform.kt, OperatorAuthDialog.kt}
│               ├── screens/{DashboardScreen.kt, ManualDiagnosticsScreen.kt, TestHistoryQueueScreen.kt, SettingsScreen.kt}
│               └── viewmodel/TestFixtureViewModel.kt
├── google_apps_script/
│   └── Code.gs
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🚀 Getting Started

### 1. Opening in Android Studio
1. Open **Android Studio** (Hedgehog 2023.1.1 or newer recommended).
2. Select **Open** and choose this project directory: `c:\Users\TRG-LOF-112-106\Desktop\test android`.
3. Let Gradle sync complete.
4. Run on your **Lenovo Tab** or an Android Virtual Device (AVD).

### 2. Testing Without Physical Rig (Mock Simulator Mode)
1. On initial launch, the app starts in **Mock Mode** by default.
2. Tap **"TAP TO LOGIN"** in the top status bar (e.g. ID: `OP-01`, PIN: `1234`).
3. Tap **"START AUTOMATED TEST"** to see live current waveforms and simulated diagnostic card updates.
4. Go to **Settings** to toggle Mock Mode off when connecting physical USB hardware.

### 3. Deploying Google Sheets Logging
1. Create a new Google Sheet.
2. Go to **Extensions** $\to$ **Apps Script**.
3. Replace the code with the contents of [`google_apps_script/Code.gs`](file:///c:/Users/TRG-LOF-112-106/Desktop/test%20android/google_apps_script/Code.gs).
4. Run `setupSheetHeaders()` once in the script editor.
5. Click **Deploy** $\to$ **New deployment** $\to$ Select **Web app** $\to$ Execute as: **Me** $\to$ Who has access: **Anyone**.
6. Copy the URL and paste it into the Android app's **Settings** screen.

---

## ⚡ Factory Floor Recommendations
- **OTG + Power Delivery:** Use a powered USB-C OTG Y-cable or docking hub to provide simultaneous 18W–30W charging to the Lenovo Tablet while maintaining data communication with the ESP32.
- **Kiosk / Lock Task Mode:** Lock down the tablet using Android's `startLockTask()` so operators cannot accidentally exit the test fixture environment.
