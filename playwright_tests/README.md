# 🧪 PingPin Playwright Test Module

Automated, thorough test suite module built with **Playwright + JavaScript** to verify all PingPin UI components, alarm services, and application workflows before every app release.

---

## 📁 Directory Structure

```
playwright_tests/
├── package.json               # Module dependencies & npm test scripts
├── playwright.config.js       # Playwright test runner configuration
├── README.md                  # Comprehensive user manual & execution guide
├── lib/
│   ├── android-driver.js      # Playwright native _android ADB helper
│   └── component-harness.js   # Mock data generators & component assertion utilities
└── tests/                     # 16 Component & Functional Test Specifications
    ├── glass-card.spec.js           # GlassCard component tests
    ├── pingpin-switch.spec.js       # PingPinSwitch component tests
    ├── calendar-view.spec.js        # CalendarView monthly grid & month navigation tests
    ├── weekly-calendar-card.spec.js # ExpandableWeeklyCalendarCard tests
    ├── makeup-wfo-card.spec.js      # MakeupWfoCard missed day compensation tests
    ├── time-picker-dialog.spec.js   # AdvancedTimePickerDialog & preset tests
    ├── wifi-ssid-picker.spec.js     # WifiSsidPickerField & network scan tests
    ├── holidays-card.spec.js        # UpcomingHolidaysCard & 2026 Indian holidays tests
    ├── days-selectors.spec.js       # WfoDaysSelector & WorkingDaysSelector tests
    ├── liquid-glass-nav.spec.js     # LiquidGlassNavBar & NavRail tests
    ├── progress-radial-ring.spec.js # ProgressRadialRing percentage & color tests
    ├── weather-travel-card.spec.js  # WeatherTravelCard commute rain warning tests
    ├── update-dialog.spec.js        # UpdateAvailableDialog version changelog tests
    ├── alarm-activity.spec.js       # Full-screen AlarmActivity & Snooze/Dismiss tests
    ├── alarm-service.spec.js        # AlarmManager scheduling & holiday suppression tests
    └── pre-release-suite.spec.js    # Master end-to-end release verification suite
```

---

## 🚀 Getting Started

### 1. Install Dependencies
From the project root or `playwright_tests` directory:

```bash
cd playwright_tests
npm install
npx playwright install chromium
```

---

## 🏃 Running Tests

### Run All Tests
Execute all 16 component and integration test specs:
```bash
npm test
```

### Run Component Tests Only
Run UI component contracts:
```bash
npm run test:components
```

### Run Alarm Tests Only
Run Alarm UI, Snooze/Dismiss, and AlarmManager service tests:
```bash
npm run test:alarms
```

### Run Pre-Release Verification Suite
Execute pre-release verification before cutting an APK release tag:
```bash
npm run test:release
```

### Interactive UI Mode
Launch interactive Playwright Test Runner UI:
```bash
npm run test:ui
```

### View HTML Test Report
View visual execution results and screenshots:
```bash
npm run test:report
```

---

## 📋 Component Test Matrix

| Component | File | Coverage |
| :--- | :--- | :--- |
| **GlassCard** | [`tests/glass-card.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/glass-card.spec.js) | Press scale, haptics, theme contrast, semantics |
| **PingPinSwitch** | [`tests/pingpin-switch.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/pingpin-switch.spec.js) | On/Off state, disabled state, thumb icon, track contrast |
| **CalendarView** | [`tests/calendar-view.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/calendar-view.spec.js) | Month navigation, day status badges, day selection |
| **WeeklyCard** | [`tests/weekly-calendar-card.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/weekly-calendar-card.spec.js) | Expand/collapse toggle, day checkmarks, weekly target |
| **MakeupWfoCard** | [`tests/makeup-wfo-card.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/makeup-wfo-card.spec.js) | Missed day counter, suggested dates, completion flow |
| **TimePickerDialog** | [`tests/time-picker-dialog.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/time-picker-dialog.spec.js) | Custom time, preset chips, +/-15m step buttons, AM/PM |
| **WifiSsidPicker** | [`tests/wifi-ssid-picker.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/wifi-ssid-picker.spec.js) | SSID input, network scanner, signal strength |
| **UpcomingHolidays** | [`tests/holidays-card.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/holidays-card.spec.js) | 2026 Indian holidays, filter tabs, long weekend badges |
| **DaysSelectors** | [`tests/days-selectors.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/days-selectors.spec.js) | WFO target chips (1-5), working days multi-select |
| **LiquidGlassNav** | [`tests/liquid-glass-nav.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/liquid-glass-nav.spec.js) | Fluid pill animation, bottom bar vs side rail layout |
| **ProgressRadialRing** | [`tests/progress-radial-ring.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/progress-radial-ring.spec.js) | Sweep angle, color thresholds (Red/Yellow/Green) |
| **WeatherTravelCard** | [`tests/weather-travel-card.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/weather-travel-card.spec.js) | Forecast, commute rain advisory, manual refresh |
| **UpdateAvailableDialog** | [`tests/update-dialog.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/update-dialog.spec.js) | Version string, release notes list, Update/Later buttons |
| **AlarmActivity** | [`tests/alarm-activity.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/alarm-activity.spec.js) | Full-screen alarm UI, Snooze (5m), Dismiss, Open Portal |
| **AlarmService** | [`tests/alarm-service.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/alarm-service.spec.js) | AlarmManager IDs, intent payload extras, holiday suppression |
| **Master Release Suite** | [`tests/pre-release-suite.spec.js`](file:///c:/Users/uprasenjeet/Documents/pingpin/playwright_tests/tests/pre-release-suite.spec.js) | End-to-end journey across Home, Insights, Settings |
