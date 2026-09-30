<p align="center">
  <img src="logo.png" alt="PingPin Logo" width="140" height="140" />
</p>

<h1 align="center">📍 PingPin</h1>

<p align="center">
  <b>The Ultimate Privacy-First Hybrid Work & Attendance Assistant for Android</b>
</p>

<p align="center">
  <a href="https://github.com/urunkarpm/pingpin/releases/latest"><img src="https://img.shields.io/github/v/release/urunkarpm/pingpin?color=blue&logo=github" alt="Latest Release" /></a>
  <a href="#-100-privacy-guarantee"><img src="https://img.shields.io/badge/Privacy-100%25%20On--Device%20Local-success?logo=shieldcheck&logoColor=white" alt="Privacy" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-7.0%2B%20(API%2024%2B)-brightgreen?logo=android&logoColor=white" alt="Android" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
</p>

<p align="center">
  Never miss an office mandate again. Automatically log local attendance on office Wi-Fi, reschedule missed WFO days, monitor commute weather, auto-punch your company HR portal, and export executive PDF analytics reports — 100% privately on your device.
</p>

---

## 🌟 What Can PingPin Do?

PingPin is your personal assistant for managing hybrid work schedules (WFO / WFH). It handles daily attendance tracking, company HR portal check-ins, missed day recovery, executive PDF reporting, and commute planning so you don't have to worry about missing attendance targets.

---

## 🔥 Key Features

### 📡 1. Automated Wi-Fi Attendance Tracking
- **Automatic Office Detection**: Logs attendance on your calendar automatically as soon as your phone connects to your office Wi-Fi network.
- **On-Time vs. Late Marking**: Automatically classifies check-ins as **Present** or **Late** based on your configured shift start time.
- **Smart Afternoon Verifier**: Verifies afternoon office arrivals to prevent false missed-day notifications.

### 🌐 2. HR Portal Auto-Punch & In-App Viewer
- **Integrated HR Portal Viewer**: Directly view and log into your company HR web portal with persistent session cookies.
- **Auto-Login & Auto-Punch**: Fills in your credentials and triggers Check-In/Check-Out buttons automatically.
- **1-Tap Alarm Actions**: Perform check-ins or check-outs directly from punctual alarm screens.

### 🔄 3. Smart "Makeup WFO" Rescheduler
- **Missed Day Recovery**: Detects missed WFO days and proposes optimal replacement dates on upcoming WFH days.
- **Target Tracking**: Recovery suggestions automatically resolve as soon as weekly office attendance targets are fulfilled.

### 📊 4. Executive PDF Analytics & Human Insights
- **Smart Narrative Digest**: Generates personalized performance commentary tailored to your attendance data.
- **Executive KPI Cards**: Displays Attendance Rate, Days Attended, Punctuality Score, and Auto-Verification percentages.
- **Clean Audit Log Table**: Exports formatted attendance logs with status badges (`PRESENT`, `LATE`, `EXTRA WFO`, `ABSENT`).

### 🌦️ 5. Commute Weather & Travel Advisories
- **Live Commute Weather**: Displays hourly weather forecasts for key travel hours (8 AM, 9 AM, 5 PM, 6 PM, 8 PM).
- **Rain & Heat Alerts**: Sends timely advisories for severe rain or extreme heat to plan your commute mode.

### 📅 6. Multi-Year Indian Holiday Directory
- **36 States & UTs Filter**: Filter official holidays across 2024–2036 by region.
- **Long Weekend Badges**: Highlights holidays adjacent to weekends with `LONG WEEKEND` tags.

### ⏰ 7. Guaranteed Alarm Alerts & Leave Application
- **Punctual Alarm Alerts**: Non-stop ringtone alerts that fire reliably even in deep sleep mode.
- **1-Tap Leave Request**: Send leave application emails with 1 tap directly from the alarm screen.

---

## 🛠 Architecture & Tech Stack

PingPin is built strictly following modern Android architecture guidelines:

- **UI Framework**: 100% Jetpack Compose with Material 3 Design System
- **Database**: Room Database with Coroutines & Flow
- **Background Execution**: WorkManager & BroadcastReceivers for Wi-Fi polling
- **PDF Engine**: Android Native `PdfDocument` Canvas Rendering
- **Async Runtime**: Kotlin Coroutines & StateFlow

```
com.urunkarpm.pingpin
 ├── data
 │    ├── local (Room DB Entities & DAOs)
 │    ├── model (Data Transfer Objects & Models)
 │    └── repository (Attendance, Profile & Config Repositories)
 ├── receiver (Alarm, Boot & Wi-Fi Connection Receivers)
 ├── service (Attendance Logic, PDF Exporter, Weather & Alarms)
 └── ui
      ├── components (Reusable Glassmorphic UI Cards, Nav Bars & Dialogs)
      ├── home (HomeScreen & HomeViewModel)
      ├── insights (Analytics Dashboard & PDF Export ViewModel)
      ├── onboarding (Initial Setup Flow)
      ├── portal (In-App HR Portal Web Viewer)
      ├── settings (Settings Dashboard & Category Navigation)
      └── theme (Colors, Fonts, Typography & Glassmorphism Tokens)
```

---

## 🔒 100% Privacy Guarantee

> **Your data belongs to you.**

PingPin runs **entirely on your local Android device**:
- ❌ **NO** analytics or telemetry collected.
- ❌ **NO** location data or Wi-Fi passwords uploaded to cloud servers.
- ❌ **NO** mandatory cloud accounts or sign-ups required.

All logs, credentials, and attendance records stay encrypted on your phone.

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug (2024.2+) or JDK 17+
- Android SDK 34 (Android 14)

### Build Commands
```bash
# Debug Build & Test
./gradlew assembleDebug

# Signed Release APK
./gradlew assembleRelease
```

---

## 📜 License

PingPin is open-source software licensed under the **Apache 2.0 License**. See [LICENSE](LICENSE) for details.
