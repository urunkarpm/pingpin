<p align="center">
  <img src="logo.gif" alt="PingPin Animated Logo" width="160" height="160" />
</p>

<h1 align="center">📍 PingPin</h1>

<p align="center">
  <b>Work-Life Balance, Simplified. Your Personal WFO & Attendance Assistant.</b><br />
  <i>One less alarm to set. 5 less steps to log in. Just 1 tap to do the magic.</i>
</p>

<p align="center">
  <a href="https://github.com/urunkarpm/pingpin/releases/latest"><img src="https://img.shields.io/github/v/release/urunkarpm/pingpin?color=blue&logo=github" alt="Latest Release" /></a>
  <a href="#-100-privacy-guarantee--on-device"><img src="https://img.shields.io/badge/Privacy-100%25%20On--Device-success?logo=shieldcheck&logoColor=white" alt="Privacy" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Android-7.0%2B-brightgreen?logo=android&logoColor=white" alt="Android" /></a>
  <a href="https://developer.apple.com/ios/"><img src="https://img.shields.io/badge/iOS-16.0%2B-black?logo=apple&logoColor=white" alt="iOS" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
</p>

<p align="center">
  <b>PingPin</b> takes the pain out of corporate attendance compliance. Walk into office, connect to Wi-Fi, and your attendance is logged automatically. Need to punch into your company HR portal? Skip the 5-step browser login and do it with <b>1 tap</b>. Missed an office day? PingPin automatically reschedules it for you so you stay 100% compliant with zero HR emails.
</p>

---

## ✨ Why You’ll Love PingPin

PingPin was created for working professionals and corporate employees who want to spend less time managing attendance policies and more time doing great work.

<table>
  <tr>
    <td width="50%">
      <h3>⏰ One Less Alarm to Worry About</h3>
      <p>No more setting repetitive daily alarms. PingPin checks your shift start time, commute weather, and attendance status to notify you <i>only when it matters</i>.</p>
    </td>
    <td width="50%">
      <h3>🚀 5 Less Steps to Punch In</h3>
      <p>Forget opening a web browser, typing your HR portal URL, logging in, navigating 5 sub-menus, and finding the check-in button. Open PingPin and tap <b>Auto Punch</b> — magic done in 1 second.</p>
    </td>
  </tr>
  <tr>
    <td width="50%">
      <h3>📡 Zero-Touch Wi-Fi Check-In</h3>
      <p>Walk into your office building. The moment your phone connects to office Wi-Fi, PingPin automatically logs your attendance on your calendar. You don't even need to open the app!</p>
    </td>
    <td width="50%">
      <h3>🔄 Missed WFO Day? Automatically Fixed!</h3>
      <p>Took a sick day or worked from home on a mandatory office day? PingPin automatically suggests the best upcoming WFH day to swap, keeping your monthly WFO percentage green without HR follow-ups.</p>
    </td>
  </tr>
</table>

---

## 🌟 Everyday Magic Features

### 🏢 1. Automated Office Detection & Wi-Fi Check-In
- **Automatic Arrival Logging**: Connects to your office Wi-Fi and logs your presence on your personal attendance calendar.
- **Punctuality Rating**: Automatically tags your check-in as **On-Time** or **Late** based on your official shift time.
- **Afternoon Smart Verifier**: Re-verifies afternoon arrivals so half-day office visits are never accidentally marked as absent.

### 🌐 2. 1-Tap HR Portal Auto-Punch
- **In-App HR Web Viewer**: Access your company HR portal directly inside PingPin with persistent, secure login sessions.
- **Auto-Punch Script**: Fills credentials and clicks the HR portal **Check-In** / **Check-Out** buttons with 1 click.
- **Lock-Screen Action**: Punch in or out directly from your morning notification or alarm screen.

### 🔄 3. Smart "Makeup WFO" Day Rescheduler
- **Compliance Recovery**: Detects missed office days and calculates the optimal replacement date on an upcoming WFH day.
- **HR Peace of Mind**: Automatically resolves recovery prompts as soon as your weekly WFO percentage target (e.g. 85%) is met.

### 📊 4. Executive PDF Analytics & Appraisal Reports
- **Smart Narrative Digest**: Generates a human-written summary of your monthly attendance, punctuality rating, and WFO compliance.
- **Appraisal-Ready Statement**: Download a clean, formatted PDF statement with 1 tap for monthly HR submissions or appraisal reviews.
- **Audit Log Table**: Includes detailed check-in timestamps, status badges (`PRESENT`, `LATE`, `EXTRA WFO`, `ABSENT`), and Wi-Fi verification flags.

### 🌦️ 5. Commute Weather & Travel Advisories
- **Commute Window Forecast**: Check hourly weather predictions specifically during your commute hours (8–9 AM & 5–6 PM).
- **Rain & Heat Alerts**: Receive early advisories for heavy rain or severe heat to plan whether to drive, take a cab, or adjust departure times.

### 📅 6. Multi-Year Indian Holiday Directory (2024–2036)
- **36 States & UTs Filter**: Select your state to view official regional holidays instantly.
- **Long Weekend Badges**: Automatically highlights holidays adjacent to weekends with **LONG WEEKEND** badges to help you plan vacations effortlessly.

---

## 🔒 100% Privacy Guarantee — On-Device & Private

> **Your corporate and personal data never leaves your phone.**

PingPin is built with a strict **Privacy-First Architecture**:
- ❌ **NO** cloud servers, analytics, or background tracking.
- ❌ **NO** location data or Wi-Fi passwords stored anywhere outside your device.
- ❌ **NO** mandatory user accounts or sign-ups required.

All attendance records, shift settings, and HR portal credentials stay encrypted locally on your device.

---

## 📱 Platform Support

PingPin is available on both **Android** and **iOS**:

- 🤖 **Android**: Native Kotlin + Jetpack Compose app (`app-release.apk`)
- 🍎 **iOS**: Kotlin Multiplatform (KMP) shared engine + Native SwiftUI app (`pingpin.ipa`)

---

## 🛠 Tech Stack

- **UI & UX**: Jetpack Compose (Android) / SwiftUI (iOS) with Material 3 & Glassmorphism Design
- **Cross-Platform Shared Core**: Kotlin Multiplatform (KMP) for shared domain models & business logic
- **Local Database**: Room Database with Coroutines & Flow
- **Background Engine**: WorkManager & BroadcastReceivers for Wi-Fi polling
- **PDF Engine**: Android Native `PdfDocument` Canvas Renderer

---

## 📥 Download & Getting Started

Download the latest release for your platform:

- 📦 **[Download Latest APK / IPA Release (v3.0.0)](https://github.com/urunkarpm/pingpin/releases/latest)**

### Build from Source
```bash
# Clone repository
git clone https://github.com/urunkarpm/pingpin.git
cd pingpin

# Build Android Debug APK
./gradlew :app:assembleDebug

# Build KMP Shared Framework & Android Release
./gradlew :shared:assemble :app:assembleRelease
```

---

## 📜 License

PingPin is open-source software licensed under the **Apache 2.0 License**. See [LICENSE](LICENSE) for details.
