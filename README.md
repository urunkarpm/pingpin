<p align="center">
  <img src="logo.png" alt="PingPin Logo" width="140" height="140" />
</p>

<h1 align="center">📍 PingPin</h1>

<p align="center">
  <b>The Ultimate Privacy-First Hybrid Work & Attendance Assistant for Android</b>
</p>

<p align="center">
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-brightgreen?logo=android&logoColor=white" alt="Platform" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20%2B%20Material%203-4285F4?logo=jetpackcompose&logoColor=white" alt="UI" /></a>
  <a href="#-100-privacy-guarantee"><img src="https://img.shields.io/badge/Privacy-100%25%20On--Device%20Local-success?logo=shieldcheck&logoColor=white" alt="Privacy" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
</p>

<p align="center">
  Never miss a WFO mandate again. Effortlessly track attendance, auto-schedule makeup days, check live commute weather, and auto-punch HR portals — 100% on-device with zero cloud tracking.
</p>

---

## 🌟 Why PingPin?

Managing corporate hybrid work schedules (WFO / WFH) should be effortless, not a daily headache. **PingPin** combines modern Android engineering with a stunning glassmorphism design to automate your work life while keeping **100% of your data private**.

```
   ┌────────────────────────────────────────────────────────────────────────┐
   │ 📡 Auto Wi-Fi Attendance   ├──> Local Calendar Logging (Present / Late)│
   │ 🌐 HR Portal Auto-Punch   ├──> Embedded Auto-Fill & Auto-Redirection   │
   │ 🔄 Makeup WFO Locator     ├──> Smart Missed Day Compensation           │
   │ 🌦️ Commute Weather Engine ├──> Hourly Travel Advisories & Warnings      │
   │ 📅 Holiday Directory      ├──> Pre-loaded 2026 Long Weekend Badges     │
   │ 📊 PDF Report Exporter    ├──> Formal Monthly Attendance Summaries     │
   └────────────────────────────────────────────────────────────────────────┘
```

---

## 🔥 Key Features

### 📡 1. Zero-Touch Local Wi-Fi Attendance
- **Local Office Detection**: Automatically logs attendance on your in-app calendar when connected to your office Wi-Fi network.
- **On-Time vs. Late Tagging**: Dynamically classifies check-ins as `PRESENT` (before cutoff time) or `LATE` (after cutoff time).
- **Battery-Friendly WorkManager**: Powered by Android `WorkManager` for background checks without battery drain.
- **Pre-Alert 2:00 PM Verification**: Executes an automated check at 2:00 PM to detect afternoon office arrivals and suppress false missed-day notifications.
- **🔒 Privacy Safe**: Wi-Fi detection **only** updates your local in-app calendar. It **never** connects to or logs into company servers automatically.

### 🌐 2. Smart HR Portal Auto-Punch Engine
- **One-Tap Check-In**: Alarm alert notifications (`AlarmActivity`) allow 1-click official company HR portal check-ins.
- **Embedded WebView Auto-Fill**: Embedded `PortalActivity` auto-fills credentials and auto-clicks check-in/out buttons.
- **SPA & Auto-Redirect Support**: Supports single-page applications (React, Vue, Angular) with framework-aware DOM setters and seamless auto-login redirections.

### 🔄 3. Intelligent "Makeup WFO" Rescheduling
- **Missed Day Locator**: Automatically flags missed WFO days daily after 2:00 PM or retroactively.
- **Smart Candidate Finder**: Proposes compensation dates on upcoming WFH days (strictly excluding weekends, holidays, existing WFO days, and recorded attendance).
- **Interactive UI Cards**: Accept or decline suggestions directly on the Home Screen. Suggestions automatically mark as `COMPLETED` when attendance is recorded.

### 🌦️ 4. Commute Weather & Travel Advisory Engine
- **Live Hourly Commute Weather**: Powered by the Open-Meteo API using GPS or office coordinates.
- **Commute Breakdown**: Displays temperature, precipitation probability, and weather icons for key commute windows (8 AM, 9 AM, 1 PM, 5 PM, 6 PM, 8 PM).
- **Smart Safety Advisories**: Recommends optimal transit options (Cab/Metro vs AC Transit) and alerts for severe rain or high heat ($>34^\circ\text{C}$).

### 📅 5. Indian Public Holiday & Long Weekend Directory
- **Pre-Loaded 2026 Directory**: Full list of Gazetted, Restricted, National, and Regional holidays.
- **Long Weekend Detector**: Highlights holidays falling on Monday or Friday with a prominent `LONG WEEKEND` badge.
- **Countdown Timers**: Real-time relative tags ("Today!", "Tomorrow!", "In 3 days").

### ⏰ 6. Exact Alarms & OEM Battery Optimization Guides
- **Deep Sleep Alarms**: Utilizes `AlarmManager.setExactAndAllowWhileIdle()` so alarms fire reliably even during Doze mode.
- **Full-Screen Alert UI**: Custom alarm chime (`beep.mp3`) with direct action buttons.
- **OEM Guides**: Built-in battery optimization instructions for Xiaomi/POCO (MIUI/HyperOS), Samsung, OnePlus, Vivo, Oppo, and RealMe devices.

### 📊 7. Analytics, Radial Rings & Native PDF Exporter
- **Streak Counter**: Tracks consecutive working days with verified attendance.
- **Compliance Gauge**: Progress ring tracking your monthly WFO target percentage.
- **Native PDF Summary**: Export formal monthly attendance statements and daily log tables powered by Android's `PdfDocument` API.

### ♿ 8. Accessibility Excellence
- **TalkBack & Screen Readers**: Comprehensive Android accessibility integration with explicit content descriptions, custom action labels, and semantic roles.
- **Touch Target Sizing**: Guarantees a minimum 48dp x 48dp touch target boundary across all interactive cards, switches, and buttons.
- **High-Contrast Legibility**: Crisp visual contrast for date tiles, calendar badges, and text headers for maximum legibility.

---

## 🏗️ Architecture & Technology Stack

PingPin is architected according to **Modern Android Architecture (MVVM / Clean Architecture)** standards:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Presentation Layer                            │
│           Jetpack Compose • Material 3 • Glassmorphism UI             │
├────────────────────────────────────────────────────────────────────────┤
│                            Business Layer                              │
│         Attendance Engine • Makeup WFO Manager • Weather Service        │
├────────────────────────────────────────────────────────────────────────┤
│                              Data Layer                                │
│       Android Jetpack Room SQLite • SharedPreferences Repository       │
└────────────────────────────────────────────────────────────────────────┘
```

- **Core**: Kotlin 2.0, Android SDK 34 (Android 14)
- **UI Framework**: Jetpack Compose, Material 3, Custom Glassmorphism Tokens
- **Asynchrony**: Kotlin Coroutines & Flow
- **Persistence**: Room SQLite Database
- **Background Tasks**: Android WorkManager & AlarmManager
- **Networking**: Ktor / OkHttp (Open-Meteo Weather API)
- **Testing**: JUnit 5, Mockk, Appium E2E Suite, Playwright Component Tests

Detailed technical documents:
- 📖 [Full Documentation](file:///c:/Users/uprasenjeet/Documents/pingpin/DOCUMENTATION.md)
- 🏛️ [Architecture Specification](file:///c:/Users/uprasenjeet/Documents/pingpin/ARCHITECTURE.md)
- 📝 [Changelog](file:///c:/Users/uprasenjeet/Documents/pingpin/CHANGELOG.md)

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug (2024.2.1+)** or IntelliJ IDEA with Android Plugin
- **Android SDK 34** (Android 14) or higher
- **JDK 17**

### Building & Running
1. **Clone the repository**:
   ```bash
   git clone https://github.com/urunkarpm/pingpin.git
   cd pingpin
   ```

2. **Build debug APK**:
   ```bash
   ./gradlew assembleDebug
   ```

3. **Run unit tests**:
   ```bash
   ./gradlew test
   ```

4. **Install on device / emulator**:
   ```bash
   ./gradlew installDebug
   ```

---

## 🔒 100% Privacy Guarantee

> **Your data belongs to you.**

PingPin runs **entirely on your local Android device**.
- ❌ **NO** analytics or cloud telemetry collected.
- ❌ **NO** location or Wi-Fi SSIDs uploaded to external servers.
- ❌ **NO** user account or sign-up required.
- ❌ **NO** background tracking outside your defined office Wi-Fi parameters.

All logs, calendar records, and settings remain encrypted in your local Room SQLite database (`app_database.db`).

---

## 📜 License

PingPin is open-source software licensed under the **Apache 2.0 License**. See [LICENSE](LICENSE) for details.

<div align="center">
  <sub>Built with ❤️ for hybrid workers everywhere.</sub>
</div>
