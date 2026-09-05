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
  Never miss a WFO mandate again. Effortlessly track local attendance, auto-schedule makeup days, monitor live commute weather, access floating HR portal tools, and auto-punch company portals — 100% on-device with zero cloud tracking.
</p>

---

## 🌟 Why PingPin?

Managing corporate hybrid work schedules (WFO / WFH) should be effortless, not a daily headache. **PingPin** combines modern Android engineering with a stunning glassmorphism design to automate your work life while keeping **100% of your data private**.

```
┌────────────────────────────────────────────────────────────────────────┐
│ 🦄 Floating Ponytail Overlay ├──> GPU-Accelerated Drag Mini-Window & HR Punch │
│ ⚡ 120Hz Refresh Rate Engine ├──> Peak Native Refresh Rate across All Screens  │
│ 📡 Auto Wi-Fi Attendance     ├──> Local Calendar Logging (Present / Late)      │
│ 🌐 HR Portal Auto-Punch      ├──> Check-In / Check-Out Auto-Fill & Auto-Click │
│ 🔄 Makeup WFO Locator        ├──> Smart Missed Day Compensation & Auto-Cancel  │
│ 🌦️ Commute Weather Engine   ├──> Hourly Travel Advisories & Rain Warnings     │
│ 📅 Holiday2API Directory     ├──> Live Multi-Year Holidays & 36 State Filters  │
│ ⏰ Exact Deep Sleep Alarms   ├──> Native Ringtone Engine & OEM Doze Guides     │
│ 📊 PDF Report Exporter       ├──> Formal Monthly Attendance Summaries          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🔥 Key Features

### 🦄 1. GPU-Accelerated Floating Portal Overlay ("Ponytail Overlay")
- **System Mini-Window Overlay**: High-performance floating window (`FloatingPortalService`) allowing background HR portal monitoring and 1-tap access while using other apps.
- **GPU Hardware Composition**: Powered by `FLAG_HARDWARE_ACCELERATED` and sub-millisecond `TouchPacer` touch displacement pacing for zero-lag dragging and smooth interactions.
- **Dynamic Action Status**: Foreground service notification dynamically updates to show active state (Check-In vs Check-Out).
- **1-Tap Maximization**: Instantly expands floating overlay into full `PortalActivity` with complete state and URL synchronization.

### ⚡ 2. 120Hz Native Refresh Rate & GPU Animations
- **High Refresh Engine**: Forces peak native display refresh rates (120Hz / 90Hz / 144Hz) across `MainActivity`, `PortalActivity`, and `AlarmActivity`.
- **Zero-Layout-Pass GPU Graphics**: Hardware-accelerated Compose `graphicsLayer` translation animations on navigation bars and components for fluid, stutter-free transitions.

### 📡 3. Zero-Touch Local Wi-Fi Attendance Tracking
- **Local Office Detection**: Automatically logs attendance on your local in-app calendar when connected to your office Wi-Fi network.
- **On-Time vs. Late Tagging**: Dynamically classifies check-ins as `PRESENT` (before cutoff time) or `LATE` (after cutoff time).
- **Battery-Friendly WorkManager**: Powered by Android `WorkManager` for background checks without battery drain.
- **Pre-Alert 2:00 PM Verification**: Executes an automated check at 2:00 PM to detect afternoon office arrivals and suppress false missed-day notifications.
- **🔒 Privacy Safe**: Wi-Fi detection **only** updates your local in-app calendar. It **never** connects to or logs into company servers automatically.

### 🌐 4. Smart HR Portal Auto-Punch Engine (Check-In & Check-Out)
- **One-Tap Alarm Check-In / Check-Out**: Full-screen alarm alert notifications (`AlarmActivity`) allow 1-click official company HR portal punches.
- **Embedded WebView Auto-Fill**: Embedded `PortalActivity` auto-fills credentials and auto-clicks check-in/out buttons via intelligent DOM JS injection.
- **SPA & Auto-Redirect Support**: Supports Single-Page Applications (React, Vue, Angular) with framework-aware DOM setters, 2-step login flows, and seamless auto-redirection.
- **Dual Portal Modes**: Choose between embedded in-app browser with floating overlay controls or external default browser.

### 🔄 5. Intelligent "Makeup WFO" Rescheduling Engine
- **Missed Day Locator**: Automatically flags missed WFO days daily after 2:00 PM or retroactively.
- **Smart Candidate Finder**: Proposes compensation dates on upcoming WFH days (strictly excluding weekends, holidays, existing WFO days, and recorded attendance).
- **Interactive UI & Auto-Cancellation**: Accept or decline suggestions directly on the Home Screen. Suggestions automatically cancel when required weekly WFO targets are fulfilled or attendance is recorded.

### 🌦️ 6. Commute Weather & Travel Advisory Engine
- **Live Hourly Commute Weather**: Powered by Open-Meteo API using GPS or office coordinates.
- **Commute Breakdown**: Displays temperature, precipitation probability, and weather condition icons for key commute windows (8 AM, 9 AM, 1 PM, 5 PM, 6 PM, 8 PM).
- **Contextual Safety Advisories**: Recommends transit options (Cab/Metro vs AC Transit) and alerts for severe rain ($\ge 70\%$) or high heat ($\ge 34^\circ\text{C}$).
- **Offline Fallback Engine**: Time-based simulated weather engine ensures continuous UI rendering when offline.

### 📅 7. Holiday2API Integration & Multi-Year Indian Holiday Directory
- **Dynamic Multi-Year API**: Fully integrated with PingPin's in-house [`Holiday2API`](https://holiday2api.vercel.app) for live Indian holiday schedules across 2024–2036.
- **36 States & Union Territories**: Filter holidays specifically by your state or region.
- **Long Weekend Detector**: Highlights holidays falling on Monday or Friday with a prominent `LONG WEEKEND` badge.
- **Countdown Timers & Interactive Modal**: Real-time relative tags ("Today!", "Tomorrow!", "In 3 days") and monthly multi-year holiday breakdown.

### ⏰ 8. Exact Alarms & Native Audio Ringtone Engine
- **Deep Sleep Alarms**: Utilizes `AlarmManager.setExactAndAllowWhileIdle()` and CPU `WakeLock` protection so alarms fire reliably even during deep Doze mode and Monday mornings.
- **Isolated Task Affinity**: `AlarmActivity` runs with isolated task affinity to prevent auto-minimizing on custom ROMs (Vivo OriginOS, MIUI, OneUI).
- **Native Ringtone Audio Engine**: Continuous non-interruptible ringtone looping with fallback media playback.
- **OEM Battery Setup Guides**: Built-in optimization guides for Xiaomi/POCO, Samsung, OnePlus, Vivo, Oppo, and RealMe devices.

### 📅 9. Expandable Weekly Calendar & Custom Visual Architecture
- **Dual View Calendar**: Toggle effortlessly between a 7-day expandable weekly strip (`ExpandableWeeklyCalendarCard`) and a full monthly calendar view (`CalendarView`).
- **High-Contrast Day Tiles**: Visual distinction between Present (Green), Late (Orange), Missed (Red), WFH (Blue), Scheduled WFO (Purple), Today (Electric Blue Ring), Makeup WFO (Amber), and Holiday (Gold).
- **Dual Design Themes**: Choose between E-Ink Newsprint theme for outdoor legibility or Charcoal Dark theme with glassmorphic cards and floating liquid navigation bar.

### 📊 10. Analytics, Radial Compliance Gauge & Native PDF Exporter
- **Streak Counter**: Tracks consecutive working days with verified attendance.
- **Compliance Gauge**: Progress radial ring tracking your monthly WFO target percentage.
- **Native PDF Summary**: Export formal monthly attendance statements and daily log tables powered by Android's `PdfDocument` API.

### 🚀 11. In-App Auto-Updater & Accessibility Excellence
- **GitHub Release Updater**: Integrated update checker (`UpdateManager`) notifying users of new APK releases with 1-tap dialog updates.
- **Accessibility Integration**: Full screen reader (TalkBack) support with explicit content descriptions, custom action labels, semantic roles (`Role.Button`, `Role.Tab`), and guaranteed 48dp x 48dp minimum touch targets.

---

## 🏗️ Architecture & Technology Stack

PingPin is architected according to **Modern Android Architecture (MVVM / Clean Architecture)** standards:

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Presentation Layer                            │
│    Jetpack Compose • Material 3 • Glassmorphism UI • Ponytail Overlay  │
├────────────────────────────────────────────────────────────────────────┤
│                            Business Layer                              │
│  Attendance Engine • Makeup WFO Manager • Weather & Holiday2API Engine │
├────────────────────────────────────────────────────────────────────────┤
│                              Data Layer                                │
│       Android Jetpack Room SQLite • SharedPreferences Repository       │
└────────────────────────────────────────────────────────────────────────┘
```

- **Core**: Kotlin 2.0, Android SDK 34 (Android 14)
- **UI Framework**: Jetpack Compose, Material 3, Custom Glassmorphism Tokens
- **Asynchrony**: Kotlin Coroutines & Flow
- **Persistence**: Room SQLite Database (`app_database.db`)
- **Background Tasks**: Android WorkManager, AlarmManager, GPU-Accelerated Foreground Services
- **Networking**: Ktor / OkHttp (Open-Meteo & Holiday2API)
- **Testing**: JUnit 5, Mockk, Appium E2E Suite, Playwright Component Tests

Detailed technical documents:
- 📖 [Full Documentation](DOCUMENTATION.md)
- 🏛️ [Architecture Specification](ARCHITECTURE.md)
- 📝 [Changelog](CHANGELOG.md)

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
