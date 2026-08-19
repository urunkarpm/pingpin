# PingPin 📍

**PingPin** is a modern, privacy-focused native Android application designed to effortlessly manage and track hybrid work schedules (WFO / WFH). Built with Kotlin, Jetpack Compose, Material 3, and Room SQLite, PingPin operates 100% on-device with zero cloud tracking, zero telemetry, and total privacy.

---

## 🌟 Key Features

### 📡 Automatic Wi-Fi Calendar Attendance (Local Only)
- **Local Calendar Logging**: Automatically marks local attendance on your in-app calendar when connected to your configured office Wi-Fi network.
- **On-Time vs. Late Tracking**: Categorizes local check-ins as `PRESENT` (before check-in cutoff time) or `LATE` (after cutoff time) in Room SQLite.
- **Background Auto-Check Worker**: Powered by Android WorkManager (`WifiCheckWorker`) for reliable local background verification without draining battery.
- **Pre-Alert Auto-Check**: Triggers a local check-in evaluation at 2:00 PM if connected to office Wi-Fi later in the day.
- **⚠️ Important**: Wi-Fi detection **only** marks attendance on your local in-app calendar & database. It **never** logs into or marks attendance on your company HR portal.

### 🌐 Official Company HR Portal Check-In
- **Alarm Alert Trigger**: Portal attendance is **only** performed when you explicitly click **"Check In"** on the alarm alert screen (`AlarmActivity`).
- **In-App Auto Engine & Browser Options**: Supports embedded auto-fill WebView (`PortalActivity`) or external browser launch to log official company attendance.
- **Zero Ambiguity**: Clear separation ensures local calendar tracking remains automated while official company portal check-in requires user confirmation on alarm alerts.

### 🔄 Intelligent WFO Rescheduling & Compensation ("Makeup WFO")
- **Automated Missed Day Detection**: Detects missed WFO days daily after 2:00 PM or retroactively for yesterday.
- **Smart Compensation Candidate Finder**: Automatically suggests candidate compensation days on upcoming WFH working days (strictly excluding weekends, existing WFO days, public holidays, and days with existing attendance).
- **Interactive UI Cards**: Accept or decline suggestions directly on the Home Screen. Suggestions automatically mark as `COMPLETED` when attendance is recorded on the makeup date.

### 💻 BLE Office Presence & Laptop Occupancy Scanner
- **Agentless Laptop Headcount**: Scans nearby Bluetooth Low Energy (BLE) signals to detect active office laptops without requiring software installation on laptops.
- **Cross-Platform Detection**: Differentiates Apple macOS (AirDrop / Handoff signatures `0x004C`) and Microsoft Windows (`0x0006`) laptops.
- **RSSI & Proximity Deduplication**: Filters out distant signals ($<-82\text{ dBm}$) and deduplicates randomized BLE MAC addresses to provide accurate bay occupancy counts.

### 📅 Indian Public Holiday & Long Weekend Calendar
- **Pre-Loaded 2026 Directory**: Complete list of Gazetted, Restricted, National, and Regional holidays.
- **Relative Proximity Countdown**: Real-time tags ("Today!", "Tomorrow!", "In X days", "In 1 week").
- **Long Weekend Detector**: Automatically flags holidays falling on Monday or Friday with a highlighted `LONG WEEKEND` badge.
- **Full Year Directory Modal**: Browse the complete 2026 holiday directory grouped by month and category.

### 🌦️ Commute Weather & Travel Advisory Engine
- **Live Hourly Commute Weather**: Powered by Open-Meteo API using device GPS location, office coordinates, or smart city defaults.
- **Hourly Commute Breakdown**: Shows temperature, precipitation probability, and weather icons for key commute hours (8 AM, 9 AM, 1 PM, 5 PM, 6 PM, 8 PM).
- **Travel Safety Insights**: Generates contextual alerts (Rain Warning, High Heat, Umbrella Advised) and recommends ideal transport modes (Cab/Metro, AC Transit, Covered Vehicle).
- **Offline Fallback**: Simulated weather engine ensures UI continuity even when offline.

### ⏰ Exact Alarms & OEM Battery Optimization
- **Reliable Reminders**: Uses `AlarmManager.setExactAndAllowWhileIdle()` to guarantee check-in alarms fire even during deep sleep.
- **Full-Screen Alarm Activity**: Custom alarm screen (`AlarmActivity`) with alarm chimes, vibration, and direct HR Portal Check-In action button.
- **Boot Persistence**: Automatically reschedules active alarms after device restart.
- **OEM Battery Optimization Guides**: Built-in, step-by-step background battery setup instructions tailored for Xiaomi/POCO (MIUI/HyperOS), Samsung, OnePlus, Vivo, Oppo, and RealMe devices.

### 📊 Analytics, Insights & PDF Monthly Reports
- **Streak Counter**: Tracks consecutive working days with recorded attendance since app installation.
- **Compliance Radial Gauge**: Visual progress ring tracking target attendance percentages.
- **Native PDF Exporter**: Exports formal monthly attendance summary statements and daily log tables using Android's native `PdfDocument` API.

### 🎨 Dual Themes & E-Ink Newsprint Styling
- **E-Ink Paper Newsprint Theme**: High-contrast paper background (`#F7F4EB`) designed for maximum legibility in outdoor sunlight.
- **Charcoal Dark Theme**: Modern dark mode featuring glassmorphism cards (`GlassCard`), liquid glass navigation bar (`LiquidGlassNavBar`), and custom Material 3 typography.

---

## 🏗️ Architecture Overview

PingPin follows Modern Android Architecture guidelines (MVVM / Clean Architecture principles):

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│      (Jetpack Compose UI Screens, Cards & Dialogs)       │
├─────────────────────────────────────────────────────────┤
│                      Business Logic                      │
│     (Attendance, WFO Rescheduling, BLE, Weather, Alarm)  │
├─────────────────────────────────────────────────────────┤
│                      Data Access                         │
│       (Repositories + Room Database DAOs & Entities)     │
└─────────────────────────────────────────────────────────┘
```

- **UI Layer**: Jetpack Compose + Material 3 custom design tokens.
- **Logic Layer**: Kotlin Flow + Coroutines + Services.
- **Data Layer**: SQLite powered by Android Jetpack Room DAO pattern.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or IntelliJ IDEA with Android plugin.
- Android SDK 34 (Android 14) or higher.
- JDK 17.

### Building & Running
1. Clone the repository:
   ```bash
   git clone https://github.com/urunkarpm/pingpin.git
   cd pingpin
   ```
2. Build the project using Gradle wrapper:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run unit tests:
   ```bash
   ./gradlew test
   ```
4. Install on connected Android device / emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 🧪 Testing

- **Unit Tests**: Located in `app/src/test/kotlin/com/urunkarpm/pingpin/service/` covering `AttendanceService`, `MakeupWfoManager`, `IndianHolidayService`, and `WeatherService`.
- **E2E UI Tests**: Automated Appium test suite located in `appium_tests/` for automated regression testing on physical devices/emulators.

---

## 🔒 Privacy Statement

PingPin is **100% local and private**. No attendance data, Wi-Fi SSIDs, BLE scan metrics, location coordinates, or user profiles are ever sent to external cloud servers. All database records remain securely on your device.

---

## 📜 License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.
