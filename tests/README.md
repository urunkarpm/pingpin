# PingPin Automated Test Suite

Comprehensive multi-tiered testing suite for PingPin, spanning unit logic, component design tokens, and live device end-to-end integration.

---

## 📁 Directory Structure

```
tests/
├── run_all.ps1                  # Master test orchestrator (runs any or all suites)
├── README.md                    # This documentation
├── artifacts/                   # Saved test execution artifacts (screenshots, dumped XML, exported PDFs)
│
├── unit/
│   └── run_unit_tests.ps1       # Runs 58 Gradle JVM unit tests (Alarm, Portal, Weather, Holidays, WFO)
│
├── playwright/
│   └── run_playwright_tests.ps1 # Runs 116 Playwright component & visual token specs
│
└── device/
    └── run_device_tests.py      # 12 Live device integration tests executed via ADB against physical phone
```

---

## 🚀 How to Run Tests

### 1. Run Everything (Master Suite)
Runs JVM unit tests, Playwright component tests, and Live Device E2E tests:
```powershell
.\tests\run_all.ps1 -Suite all
```

### 2. Run Only Live Device Tests (Phone via USB or ADB Wi-Fi)
Executes the 12 live device verification tests against your connected Android phone:
```powershell
# Auto-detect connected device
.\tests\run_all.ps1 -Suite device

# Or run directly with Python:
python .\tests\device\run_device_tests.py

# Or specify a device serial / Wi-Fi address:
python .\tests\device\run_device_tests.py --device 10BG4Y0TDS001TD
python .\tests\device\run_device_tests.py --device 192.168.1.8:36265
```

### 3. Run Only JVM Unit Tests
Fast execution (under 4 seconds) verifying all core background engines:
```powershell
.\tests\run_all.ps1 -Suite unit
# Or:
.\tests\unit\run_unit_tests.ps1
```

### 4. Run Only Playwright UI Component Tests
Verifies Jetpack Compose design tokens, glassmorphism, calendars, and dialog mechanics:
```powershell
.\tests\run_all.ps1 -Suite playwright
# Or:
.\tests\playwright\run_playwright_tests.ps1
# To render synthetic design snapshots:
.\tests\playwright\run_playwright_tests.ps1 -Snapshots
```

---

## 📱 Live Device Test Coverage (`run_device_tests.py`)

| Test # | Test Name | What it Verifies |
| :---: | :--- | :--- |
| **01** | `test_01_device_readiness` | Queries Android model/brand, verifies Android SDK >= 26, ensures screen is awake and unlocked. |
| **02** | `test_02_app_launch` | Launches `MainActivity`, verifies PingPin is the top foreground focus activity. |
| **03** | `test_03_home_weather_tab` | Verifies Home screen weather header (Kalyan), commute status card, and weekly attendance strip. |
| **04** | `test_04_home_holidays_tab` | Taps Indian Holidays tab, verifies holiday cards, long-weekend tags, and countdown counters. |
| **05** | `test_05_monthly_calendar_expansion` | Swipes up to expand full-month calendar bottom sheet, checks date attendance indicators, collapses back. |
| **06** | `test_06_insights_screen` | Taps Insights tab, verifies WFO compliance ring (60%), punctuality gauge, and daily attendance audit log. |
| **07** | `test_07_settings_profile` | Taps Settings -> Profile, verifies user name (`Prasenjeet`), SSID (`LionMobile`), and shift schedule. |
| **08** | `test_08_settings_automation` | Taps Settings -> Automation, verifies in-app auto portal execution mode, punch toggle, and keyword tags. |
| **09** | `test_09_settings_health` | Taps Settings -> Health, verifies exact alarm permissions and customized OEM battery optimization advice (Vivo/FuntouchOS). |
| **10** | `test_10_settings_updates` | Taps Settings -> Updates, verifies installed version (`v2.9.0`) and GitHub release check banner. |
| **11** | `test_11_alarm_screen_and_chime` | Launches full-screen AMOLED `AlarmActivity`, verifies glowing clock, snooze chips, slider controls, and safely dismisses chime. |
| **12** | `test_12_pdf_statement_integrity` | Verifies generated monthly attendance statement PDF in device storage, validates `%PDF-` magic header, checks file size, and pulls to `tests/artifacts/`. |

---

## 📸 Test Artifacts

Every test execution automatically saves screenshots and documents to:
[`tests/artifacts/`](file:///C:/Users/uprasenjeet/Documents/pingpin/tests/artifacts)
- `test_02_launch.png`
- `test_03_home_weather.png`
- `test_04_home_holidays.png`
- `test_05_calendar_expanded.png`
- `test_06_insights_top.png` & `test_06_insights_log.png`
- `test_07_settings_profile.png`
- `test_08_settings_automation.png`
- `test_09_settings_health.png`
- `test_10_settings_updates.png`
- `test_11_alarm_screen.png`
- `PingPin_Attendance_YYYY_MM.pdf`
