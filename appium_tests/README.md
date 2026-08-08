# PingPin Appium Alarm Tests

End-to-end alarm tests for the **PingPin** Flutter app using [Appium 2](https://appium.io/) + [WebdriverIO 9](https://webdriver.io/) + Mocha.

---

## Prerequisites

| Tool | Version | Install |
|---|---|---|
| Node.js | ≥ 18 | https://nodejs.org |
| Java (JDK) | ≥ 11 | `winget install Microsoft.OpenJDK.17` |
| Android SDK / ADB | any | Android Studio or `sdkmanager` |
| Appium 2 | latest | installed automatically by npm |
| UiAutomator2 driver | latest | see step 3 below |

---

## Setup

```bash
# 1. Enter the test directory
cd appium_tests

# 2. Install Node dependencies (Appium 2 + WebdriverIO + Mocha)
npm install

# 3. Install the UiAutomator2 Appium driver (once only)
npx appium driver install uiautomator2

# 4. Verify your Android device / emulator is connected
adb devices
#   List of devices attached
#   R52RB0W0VYN     device          ← your device must appear here
```

Make sure the **PingPin debug APK** is installed on the device:
```bash
# From the Flutter project root (not this folder):
flutter install
```

---

## Running Tests

```bash
# All suites
npm test

# Only the quick 5-second alarm trigger tests
npm run test:alarm

# Full check-in alarm flow (schedules, clock advance, dismiss)
npm run test:checkin

# Full check-out alarm flow
npm run test:checkout

# Settings UI tests (time-picker, persistence, cancel)
npm run test:settings
```

---

## Test Suites

### `tests/alarm.test.js` — Quick Alarm Tests (5-second trigger)
Uses PingPin's built-in **"Test Check-in Alarm"** button in Settings (fires in 5 s):

| # | What it tests |
|---|---|
| 1 | AlarmRingingScreen appears within 15 s of tapping the test button |
| 2 | Badge, time label, CHECK-IN and APPLY FOR LEAVE buttons visible |
| 3 | Tap CHECK-IN → alarm screen closes |
| 4 | Tap APPLY FOR LEAVE → alarm screen closes |
| 5 | Hardware back button is **blocked** (PopScope canPop: false) |

---

### `tests/checkin_alarm.test.js` — Check-In Alarm Flow
Schedules check-in alarm for **now + 1 minute**, advances device clock via ADB:

| # | What it tests |
|---|---|
| 1 | Alarm time can be changed to 1 min from now in Settings |
| 2 | Alarm fires after clock advances 65 s |
| 3 | Badge, prompt, CHECK-IN button visible; no CHECK-OUT button |
| 4 | CHECK-IN button dismisses the screen |
| 5 | APPLY FOR LEAVE also dismisses the screen |

---

### `tests/checkout_alarm.test.js` — Check-Out Alarm Flow
Same structure as check-in, targeting alarm ID 102:

| # | What it tests |
|---|---|
| 1 | Check-out alarm scheduled for now + 1 min |
| 2 | Fires after clock advance |
| 3 | Badge, prompt, CHECK-OUT button; no CHECK-IN / LEAVE button |
| 4 | CHECK-OUT button dismisses the screen |
| 5 | Hardware back button locked on check-out screen |

---

### `tests/alarm_settings.test.js` — Settings UI
Pure UI tests that do **not** fire an alarm:

| # | What it tests |
|---|---|
| 1 | Check-in Time and Check-out Time tiles visible |
| 2 | Time picker updates check-in time tile |
| 3 | Time picker updates check-out time tile |
| 4 | Times persist after Save + navigating away + back |
| 5 | Pressing Cancel leaves time unchanged |
| 6 | Edge-case time 23:50 accepted by picker |

---

## Key File Structure

```
appium_tests/
├── package.json          – npm dependencies & test scripts
├── wdio.conf.js          – WebdriverIO + Appium configuration
├── helpers.js            – Shared utilities (selectors, navigation, assertions)
└── tests/
    ├── alarm.test.js           – Quick 5-second alarm tests
    ├── checkin_alarm.test.js   – Full check-in flow
    ├── checkout_alarm.test.js  – Full check-out flow
    └── alarm_settings.test.js  – Settings screen UI tests
```

---

## How It Works

### App identification
```
appPackage:  com.urunkarpm.pingpin
appActivity: com.urunkarpm.pingpin.MainActivity
```
Appium launches the installed app without reinstalling (`noReset: true`).

### Element location strategy
Flutter does not expose a standard Android view hierarchy, so tests use **UiAutomator2 text selectors** (matching the exact text rendered on screen) and fall back to accessibility IDs where Flutter semantic labels are set.

### Clock advancement (checkin/checkout suites)
Real scheduled alarms (ID 101 / 102) fire at the configured daily time.  
To avoid waiting 24 hours, tests use:
```js
await driver.executeScript('mobile: shell', [{
  command: 'date',
  args: ['-s', `@$(($(date +%s) + 65))`],
}]);
```
This requires `relaxedSecurity: true` in the Appium server args (already set in `wdio.conf.js`).

> **Note:** Device clock modification via ADB shell works on debug builds and most emulators. On production-signed builds or locked devices it may be unavailable — use the 5-second test-trigger tests (`npm run test:alarm`) instead.

---

## Troubleshooting

| Issue | Fix |
|---|---|
| `Could not find a connected Android device` | Run `adb devices`; enable USB Debugging on device |
| `App not found on device` | Run `flutter install` from the project root |
| `element not found: Check-in Time` | Scroll Settings screen; the tile may be below the fold |
| Clock advance doesn't work | Use `npm run test:alarm` (5-second trigger) instead |
| Appium server fails to start | Check Java is on PATH: `java -version` |
