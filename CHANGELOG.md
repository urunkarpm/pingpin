# Changelog

All notable changes to **PingPin** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.7.0] - 2026-08-19

### Added
- **Explicit Onboarding Preference Tracking**: Added `OnboardingPreferences` to ensure initial setup is only marked complete after explicit confirmation at Step 7 ("COMPLETE & LAUNCH").
- **Auto-Cancellation of Makeup WFO Suggestions**: Automatically cancels pending or accepted makeup WFO suggestions and clears scheduled notification alarms when all required WFO days for the current week are completed or when a missed WFO date is marked present in the calendar.
- **Wizard Back Navigation**: Added Compose `BackHandler` support to the initial setup screen (`OnboardingScreen`) to step backward through setup wizard pages on system back action.
- **Unit Test Coverage**: Added comprehensive unit tests for `OnboardingPreferences` and `MakeupWfoManager` week completion cancellation.

### Changed
- **Calendar Color Scheme Refinement**: Updated **WFO Day (Scheduled WFO)** to **Royal Purple** (`WfoDayPurple` - `#8B5CF6`) with soft lavender fills (`#F3E8FF` / `#4C1D95`) and purple status dots.
- **Today Day Cell Styling**: Today is now uniquely identified by a prominent 2.5dp **Electric Blue Ring** (`#3B82F6`), eliminating all visual confusion between Present (Green), Today (Blue Ring), WFO Day (Purple), Makeup WFO (Amber), and Missed (Red).

### Fixed
- **Premature Setup Completion Bug**: Fixed an issue where clicking "Run Test" in Step 6 of initial setup saved a temporary configuration to Room DB and prematurely navigated to the App Home screen on Back press before settings and user profile were finalized.
- **Visual Overlap on Calendar Grid**: Resolved color overlap between Present (Green), WFO Day (Purple), and Today (Blue Ring) tiles.

---

## [1.6.0] - 2026-08-18

### Added
- Embedded `PortalActivity` WebView auto-punch engine (`PortalAutoCheckInEngine`).
- Adaptive Navigation Rail layout for larger screens and tablets.
- Architectural clarification separating local Wi-Fi calendar tracking from company HR portal check-in.

---

## [1.5.0] - 2026-08-14

### Added
- Expandable Weekly Calendar Strip (`ExpandableWeeklyCalendarCard`) with 7-day strip and monthly view toggle.
- Custom notification alarm chime sound (`beep.mp3`) and full-screen alarm alert UI (`AlarmActivity`).
