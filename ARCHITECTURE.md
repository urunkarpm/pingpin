# Architecture & Technical Documentation — PingPin

## 1. Architectural Overview

PingPin is built strictly following Modern Android Architecture standards (MVVM / Clean Architecture Principles):

```
┌────────────────────────────────────────────────────────────────────────┐
│                          PRESENTATION LAYER                            │
│                                                                        │
│  [MainShell] ── [HomeScreen] ── [InsightsScreen] ── [SettingsScreen]   │
│       │               │                 │                  │           │
│       └───────────────┴─────────┬───────┴──────────────────┘           │
│                                 ▼                                      │
│                      [Custom UI Components]                            │
│  (CalendarView, ExpandableWeeklyCalendar, MakeupWfoCard,               │
│   OfficeOccupancyCard, WeatherTravelCard, UpcomingHolidaysCard)        │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Collects StateFlow / State
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                            BUSINESS LOGIC                              │
│                                                                        │
│  [AttendanceService]  ───  [MakeupWfoManager]  ───  [WifiService]      │
│  [BleLaptopScanner]   ───  [WeatherService]    ───  [HolidayService]   │
│  [NotificationService]───  [PdfExportService]  ───  [OemBatteryHelper] │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Calls DAOs & Repositories
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                             DATA ACCESS                                │
│                                                                        │
│  Repositories:  [AttendanceRepo] [MakeupWfoRepo] [OfficeConfigRepo]    │
│  Room DAOs:     [AttendanceDao]  [MakeupWfoDao]  [OfficeConfigDao]     │
│  Room Entities: [AttendanceRecordEntity] [MakeupWfoSuggestionEntity]  │
│                 [OfficeConfigEntity]    [UserProfileEntity]            │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Directory Structure

```
app/src/main/kotlin/com/urunkarpm/pingpin/
├── MainActivity.kt               # Main activity entry point & Compose setContent setup
├── AlarmActivity.kt              # Full-screen alarm reminder activity
├── PingPinApplication.kt         # Application instance & WorkManager initialization
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt        # Room database configuration & singleton instance
│   │   ├── dao/                  # AttendanceRecord, MakeupWfoSuggestion, OfficeConfig, UserProfile, WfoScheduleHistory, NotificationLog DAOs
│   │   └── entity/               # Room entities (SQLite schema definitions)
│   ├── model/                    # Data models (WeatherState, IndianHoliday, BleLaptopScanResult, etc.)
│   └── repository/               # Repositories exposing Kotlin Flow state streams
├── receiver/
│   └── BootReceiver.kt           # Reschedules exact alarms after device reboot
├── service/
│   ├── AppInstallManager.kt      # App installation date tracking & streak baselines
│   ├── AttendanceAutoService.kt  # Auto-attendance background service
│   ├── AttendanceService.kt      # Core attendance rules (Present / Late logic)
│   ├── BleLaptopScannerService.kt# BLE scanner for nearby macOS/Windows laptops
│   ├── IndianHolidayService.kt   # 2026 Indian holiday directory & long weekend logic
│   ├── MakeupWfoManager.kt       # WFO missed day detection & compensation algorithm
│   ├── NotificationService.kt    # AlarmManager exact scheduling & notifications
│   ├── OemBatteryHelper.kt       # OEM background battery optimization guides
│   ├── PdfExportService.kt       # Native Android PdfDocument export engine
│   ├── WeatherService.kt         # Open-Meteo live weather & travel advisory engine
│   ├── WifiCheckWorker.kt        # WorkManager periodic Wi-Fi check worker
│   └── WifiService.kt            # Wi-Fi SSID detection & network helpers
└── ui/
    ├── MainShell.kt              # Main navigation shell & bottom bar wrapper
    ├── components/               # Custom UI components & dialogs
    │   ├── AdvancedTimePickerDialog.kt
    │   ├── CalendarView.kt
    │   ├── ExpandableWeeklyCalendarCard.kt
    │   ├── GlassCard.kt
    │   ├── LiquidGlassNavBar.kt
    │   ├── MakeupWfoCard.kt
    │   ├── OfficeOccupancyCard.kt
    │   ├── ProgressRadialRing.kt
    │   ├── TimePickerField.kt
    │   ├── UpcomingHolidaysCard.kt
    │   ├── WeatherTravelCard.kt
    │   ├── WfoDaysSelector.kt
    │   ├── WifiSsidPickerField.kt
    │   └── WorkingDaysSelector.kt
    ├── home/
    │   └── HomeScreen.kt         # Home dashboard & status cards
    ├── insights/
    │   └── InsightsScreen.kt     # Stats, streaks, calendar & PDF export
    ├── onboarding/
    │   └── OnboardingScreen.kt   # First-run wizard setup
    ├── settings/
    │   └── SettingsScreen.kt     # Profile, workspace, alarm & battery settings
    └── theme/                    # Material 3 typography, colors & light/dark theme tokens
```

---

## 3. Detailed Component Architecture

### 3.1 Attendance & Schedule Engine

#### Working Days Bitmask (`WorkingDays.kt`)
Efficiently stores working day configurations using a 7-bit bitmask integer:
- Monday = 1 ($2^0$)
- Tuesday = 2 ($2^1$)
- Wednesday = 4 ($2^2$)
- Thursday = 8 ($2^3$)
- Friday = 16 ($2^4$)
- Saturday = 32 ($2^5$)
- Sunday = 64 ($2^6$)
- **Default Weekdays Mask**: $1 + 2 + 4 + 8 + 16 = 31$.

#### Attendance Check Rules (`AttendanceService.kt`)
When connected to the designated office Wi-Fi SSID:
```kotlin
if (wifiService.isConnectedToSSID(officeConfig.ssid)) {
    val status = if (currentTime <= officeConfig.checkInTime) {
        AttendanceStatus.PRESENT
    } else {
        AttendanceStatus.LATE
    }
    saveRecord(todayDate, status, officeConfig.ssid)
}
```

### 3.2 WFO Rescheduling & Compensation Engine (`MakeupWfoManager.kt`)

Evaluates missed WFO days daily after 2:00 PM (14:00) or retroactively for yesterday:

```
                            [Check Today / Yesterday WFO]
                                         │
                         ┌───────────────┴───────────────┐
                         ▼                               ▼
                 Attendance Marked             Attendance NOT Marked
                         │                               │
                [No Action Needed]         [Attempt Wi-Fi Auto Check]
                                                         │
                                         ┌───────────────┴───────────────┐
                                         ▼                               ▼
                                  Check-in Success               Check-in Failed
                                         │                               │
                                 [Mark PRESENT/LATE]         [Execute Candidate Search]
```

#### Candidate Compensation Day Rules:
1. Searches up to 14 days into the future.
2. Must be a configured **Working Day** (`workingDaysMask`).
3. Must **STRICTLY NOT** be a scheduled WFO day (`!wfoDaysMask` $\rightarrow$ WFH day).
4. Must **NOT** be a public holiday (`IndianHolidayService`).
5. Must **NOT** already have attendance logged in Room DB.

### 3.3 BLE Office Occupancy Scanner (`BleLaptopScannerService.kt`)

Scans Bluetooth Low Energy (BLE) advertisements without agent software on laptops:

- **Microsoft Windows**: Checks Manufacturer Specific Data for Vendor ID `0x0006` and beacon sub-types (`0x03`, `0x08`, `0x01`).
- **Apple macOS**: Checks Manufacturer Specific Data for Vendor ID `0x004C` (AirDrop / Handoff signatures `0x05`, `0x0C`).
- **RSSI Thresholding**: Filters signals below $-82\text{ dBm}$ to exclude distant devices in other office areas.
- **Proximity Deduplication**: Groups signals by OS type and RSSI proximity ($\le 4\text{ dBm}$) to avoid double-counting MAC address rotators.

### 3.4 Commute Weather & Travel Insights (`WeatherService.kt`)

Fetches live weather data from Open-Meteo API:
- Resolves location: **GPS Location** $\rightarrow$ **Office Config lat/lon** $\rightarrow$ **Default City (Bengaluru)**.
- Analyzes key commute hours: 8 AM, 9 AM, 1 PM, 5 PM, 6 PM, 8 PM.
- Generates contextual alerts:
  - `Severe Weather Alert` ($\ge 70\%$ rain or thunderstorm) $\rightarrow$ Cab / Metro advised.
  - `Evening/Morning Rain Alert` ($\ge 45\%$ rain) $\rightarrow$ Umbrella advised.
  - `High Heat Warning` ($\ge 34^\circ\text{C}$) $\rightarrow$ AC transit advised.
- Includes local time-based simulated weather fallback for offline operation.

### 3.5 Exact Alarms & OEM Battery Optimization (`NotificationService.kt`, `OemBatteryHelper.kt`)

- Uses `AlarmManager.setExactAndAllowWhileIdle()` for guaranteed reminder delivery.
- Launches full-screen `AlarmActivity` with chime (`beep.mp3`) and vibration.
- Provides custom step-by-step background battery optimization setup instructions for Xiaomi/POCO, Samsung, OnePlus, Vivo, Oppo, and RealMe devices.

---

## 4. Room Database Schema

Database versioned and initialized via `AppDatabase.kt`:

| Table Name | Primary Key | Key Columns | Description |
| :--- | :--- | :--- | :--- |
| `attendance_records` | `dateYyyyMmDd` (String) | `status`, `ssid`, `checkInTime`, `checkOutTime`, `notes` | Stores daily attendance history |
| `office_configs` | `id` (Int) | `ssid`, `checkInTime`, `checkOutTime`, `workingDaysMask`, `wfoDaysMask`, `latitude`, `longitude` | Stores office environment & schedule setup |
| `user_profiles` | `id` (Int) | `fullName`, `email`, `employeeId` | User profile data |
| `makeup_wfo_suggestions` | `id` (Int, Auto) | `missedDateYyyyMmDd`, `suggestedDateYyyyMmDd`, `status` (`PENDING`/`ACCEPTED`/`DECLINED`/`COMPLETED`) | Tracks WFO compensation requests |
| `wfo_schedule_history` | `id` (Int, Auto) | `effectiveDate`, `workingDaysMask`, `wfoDaysMask` | Audit log of schedule changes |
| `notification_logs` | `id` (Int, Auto) | `timestamp`, `title`, `message`, `type` | Notification audit history |

---

## 5. UI Architecture & Design Tokens

- **Jetpack Compose Single-Activity Pattern**: Managed via `MainShell.kt` navigation shell.
- **E-Ink Newsprint Light Mode**: Visual palette using `#F7F4EB` background, crisp dark text `#111111`, and paper newsprint styling.
- **Modern Charcoal Dark Mode**: Deep slate background `#0F172A`, glassmorphic containers ([`GlassCard.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/GlassCard.kt)), and floating navbar ([`LiquidGlassNavBar.kt`](file:///c:/Users/uprasenjeet/Documents/pingpin/app/src/main/kotlin/com/urunkarpm/pingpin/ui/components/LiquidGlassNavBar.kt)).

---

## 6. PDF Export Architecture (`PdfExportService.kt`)

Uses native Android `android.graphics.pdf.PdfDocument`:
1. Renders high-resolution 595x842 (A4 format) page canvas.
2. Draws header banner, user metadata, streak stats, monthly attendance breakdown, and date-by-date tabular records.
3. Streams output file to local public storage or triggers native share intent.

---

## 7. Testing Strategy

1. **Kotlin Unit Tests**: Standard JUnit4 unit tests under `app/src/test/kotlin/com/urunkarpm/pingpin/service/` testing business logic services.
2. **Appium E2E Automation**: WebdriverIO + Appium test suite under `appium_tests/` for automated UI regression testing on physical devices/emulators.
