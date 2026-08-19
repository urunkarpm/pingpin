# PingPin — Detailed Implemented Features Manual

This document provides a comprehensive, feature-by-feature breakdown of all capabilities implemented in **PingPin** (Native Android application built with Kotlin, Jetpack Compose, Room SQLite, and Material 3).

---

## Table of Contents
1. [Automatic Wi-Fi Attendance Tracking](#1-automatic-wi-fi-attendance-tracking)
2. [Hybrid WFO Schedule & Rescheduling ("Makeup WFO")](#2-hybrid-wfo-schedule--rescheduling-makeup-wfo)
3. [BLE Office Presence & Laptop Occupancy Scanner](#3-ble-office-presence--laptop-occupancy-scanner)
4. [Indian Public Holiday & Long Weekend Directory](#4-indian-public-holiday--long-weekend-directory)
5. [Commute Weather & Travel Advisory Engine](#5-commute-weather--travel-advisory-engine)
6. [Notifications, Alarms & OEM Battery Optimization](#6-notifications-alarms--oem-battery-optimization)
7. [Analytics, Insights & Native PDF Exporter](#7-analytics-insights--native-pdf-exporter)
8. [Interactive UI & Dual Design Themes](#8-interactive-ui--dual-design-themes)
9. [Data Architecture & Local Storage](#9-data-architecture--local-storage)

---

## 1. Local Wi-Fi Calendar Attendance Tracking

### Overview
PingPin provides zero-touch office attendance detection on your local in-app calendar powered by Wi-Fi network verification and customizable late arrival rules.

> ⚠️ **Important Clarification**: Connecting to office Wi-Fi **only** marks local attendance in PingPin's on-device SQLite database (`attendance_records`) to populate the in-app calendar, streak statistics, and compliance radial gauge. It **never** logs into or marks attendance on your company HR portal.

### Key Capabilities
- **Office SSID Verification**: Automatically compares connected Wi-Fi network SSID against the configured office SSID in [`WifiService.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/WifiService.kt).
- **Status Classification**:
  - `PRESENT`: Marked locally when checked in prior to configured check-in time (e.g. 09:30 AM).
  - `LATE`: Marked locally when checked in after late cutoff time.
- **Background Periodic Worker**: [`WifiCheckWorker.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/WifiCheckWorker.kt) runs via Android WorkManager to auto-mark local calendar attendance in the background.
- **Pre-Alert Auto Check**: At 2:00 PM (14:00), PingPin executes an automated Wi-Fi check. If the user arrived at the office later in the day, local attendance is marked immediately, suppressing false missed day alerts.
- **Manual Local Check-In Action**: Single-tap check-in button on the Home Screen card for immediate local attendance recording.

---

## 2. Hybrid WFO Schedule & Rescheduling ("Makeup WFO")

### Overview
Managed in [`MakeupWfoManager.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/MakeupWfoManager.kt), this feature manages corporate hybrid WFO mandates by identifying missed office days and proposing compensation dates on upcoming WFH days.

### Key Capabilities
- **7-Day Schedule Masking**: Flexible bitmask setup (`WorkingDays.kt`) allowing users to define working days (e.g., Mon-Fri) and mandatory WFO days (e.g., Mon, Wed, Fri).
- **Missed Day Detection**: Automatically evaluates missed WFO days after 2:00 PM daily or retroactively for yesterday.
- **Strict Candidate Day Selection**:
  1. Searches up to 14 days ahead.
  2. Candidate day **must** be a working day.
  3. Candidate day **must strictly NOT** be a scheduled WFO day (must be a WFH day).
  4. Candidate day **must NOT** be a public holiday.
  5. Candidate day **must NOT** already have attendance recorded.
- **Interactive UI Lifecycle**:
  - [`MakeupWfoCard.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/MakeupWfoCard.kt) presents candidate compensation dates with Accept and Decline controls.
  - Automatically transitions to `COMPLETED` when attendance is recorded on the makeup date.
  - Highlights compensation days with a purple `MAKEUP` badge on the calendar.

---

## 3. Tactical Office Radar & Mobile BLE Signal Scanner

### Overview
Located in [`BleMobileScannerService.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/BleMobileScannerService.kt), this service non-intrusively measures office bay occupancy by scanning Bluetooth Low Energy (BLE) signals from nearby mobile phones (Android and iOS).

### Key Capabilities
- **Cross-Platform Mobile Device Detection**:
  - **Android Devices**: Parses Manufacturer Specific Data for Google Vendor ID `0x00E0` and Samsung Vendor ID `0x0075`.
  - **Apple iPhones / iOS Devices**: Parses Manufacturer Specific Data for Apple Vendor ID `0x004C`.
- **RSSI Proximity Distance Calculation**: Converts RSSI signal strength into estimated distance meters ($\text{Range } 0.8\text{m} - 8\text{m}$).
- **Proximity Zone Categorization**: Classifies devices into Immediate Bay ($< 2\text{m}$), Adjacent Wing ($2-5\text{m}$), and Outer Zone ($> 5\text{m}$).
- **Military Radar HUD Display**: [`OfficeOccupancyCard.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/OfficeOccupancyCard.kt) renders a high-tech tactical military radar with electric neon green/cyan glow, 360° rotating sweep beam, range reticles, target blips, and sector density status.

---

## 4. Indian Public Holiday & Long Weekend Directory

### Overview
Implemented in [`IndianHolidayService.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/IndianHolidayService.kt), providing a built-in calendar of Indian holidays across 2026.

### Key Capabilities
- **Categorized Holidays**: Gazetted, Restricted, National, and Regional holiday classifications.
- **Proximity Tracking**: Dynamic relative tags ("Today!", "Tomorrow!", "In X days", "In 1 week").
- **Long Weekend Identification**: Identifies holidays falling on Monday or Friday with a `LONG WEEKEND` visual tag.
- **Full Year Directory Modal**: [`UpcomingHolidaysCard.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/UpcomingHolidaysCard.kt) includes an interactive modal allowing users to explore all 2026 holidays grouped by month.

---

## 5. Commute Weather & Travel Advisory Engine

### Overview
Located in [`WeatherService.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/WeatherService.kt), fetching live weather data from Open-Meteo API.

### Key Capabilities
- **Geolocation Resolution**: GPS location $\rightarrow$ Office Config lat/lon $\rightarrow$ Default location (Bengaluru).
- **Hourly Commute Breakdown**: Shows temperature, rain probability, and weather condition icons for 8 AM, 9 AM, 1 PM, 5 PM, 6 PM, and 8 PM.
- **Contextual Travel Insights**:
  - `Severe Weather Alert` ($\ge 70\%$ rain / thunderstorm): Recommends Cab / Metro and traffic delay alerts.
  - `Evening/Morning Rain Alert` ($\ge 45\%$ rain): Recommends umbrella or covered transport.
  - `High Heat Warning` ($\ge 34^\circ\text{C}$): Hydration advice and AC transit recommendation.
- **Offline Fallback**: Time-based simulated weather engine ensures continuous UI rendering when offline.
- **UI Display**: [`WeatherTravelCard.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/WeatherTravelCard.kt).

---

## 6. Notifications, Alarms & Company HR Portal Check-In

### Overview
Implemented in [`NotificationService.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/NotificationService.kt), [`AlarmActivity.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/AlarmActivity.kt), [`PortalActivity.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/portal/PortalActivity.kt), and [`OemBatteryHelper.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/OemBatteryHelper.kt).

### Key Capabilities
- **Exact Alarm Reminders**: Uses `AlarmManager.setExactAndAllowWhileIdle()` to guarantee check-in reminders fire reliably during device deep sleep.
- **Full-Screen Alarm Activity**: Custom alarm screen ([`AlarmActivity.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/AlarmActivity.kt)) with alarm chime sound (`beep.mp3`), vibration, sonar pulse animations, and dedicated action buttons.
- **Company HR Portal Attendance Trigger**: Official portal check-in is **only** performed when the user explicitly clicks **"CHECK-IN (OPEN PORTAL)"** on the alarm alert screen:
  - **In-App Auto Mode (`IN_APP_AUTO`)**: Opens embedded [`PortalActivity.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/portal/PortalActivity.kt) with [`PortalAutoCheckInEngine.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/portal/PortalAutoCheckInEngine.kt) to automatically log in and click the portal check-in button via JS injection.
  - **External Browser Mode (`EXTERNAL_BROWSER`)**: Opens the system default browser directly to the configured portal URL.
- **Boot Persistence**: [`BootReceiver.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/receiver/BootReceiver.kt) reschedules exact alarms after device reboot.
- **OEM Battery Setup Guides**: Built-in instructions for Xiaomi/POCO (MIUI/HyperOS), Samsung, OnePlus, Vivo, Oppo, and RealMe to prevent OS background task killing.

---

## 7. Analytics, Insights & Native PDF Exporter

### Overview
Located in [`InsightsScreen.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/insights/InsightsScreen.kt) and [`PdfExportService.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/service/PdfExportService.kt).

### Key Capabilities
- **Attendance Streaks**: Counts consecutive working days with recorded attendance since installation date.
- **Status Metrics**: Totals for Present, Late, Missed, WFH, Makeup WFO, and Public Holidays.
- **Progress Gauge**: [`ProgressRadialRing.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/ProgressRadialRing.kt) displays monthly attendance target completion percentage.
- **Native PDF Exporter**: Uses Android native `PdfDocument` to generate monthly attendance summaries and daily log statements.

---

## 8. Interactive UI & Dual Design Themes

### Overview
Built entirely with Jetpack Compose and Material 3 custom design tokens.

### Key Capabilities
- **Full Month Calendar Grid**: [`CalendarView.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/CalendarView.kt) with color-coded day tiles (Green = Present, Orange = Late, Red = Missed, Blue = WFH, Purple = Makeup WFO, Gold = Holiday).
- **Expandable Weekly Strip**: [`ExpandableWeeklyCalendarCard.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/ExpandableWeeklyCalendarCard.kt) compact 7-day view with inline toggle to expand to full month.
- **Network Dropdown Selector**: [`WifiSsidPickerField.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/WifiSsidPickerField.kt) displaying active SSID, historical saved SSIDs, and Wi-Fi scan results.
- **Dual Themes**:
  - **E-Ink Newsprint**: `#F7F4EB` background for sunlight readability.
  - **Charcoal Dark**: Deep dark theme with glassmorphic cards ([`GlassCard.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/GlassCard.kt)) and floating liquid navigation bar ([`LiquidGlassNavBar.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/LiquidGlassNavBar.kt)).

---

## 9. Data Architecture & Local Storage

PingPin is **100% on-device and local**, keeping all data inside Android Room SQLite ([`AppDatabase.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/data/local/AppDatabase.kt)). Zero telemetry or external server tracking.
