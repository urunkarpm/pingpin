# Changelog

All notable changes to **PingPin** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] - 2026-08-31

### Fixed & Enhanced
- **Check-Out Alarm Automation Resolution**: Resolved automation engine issue where check-out actions triggered a "searching for check in button" error message. Action type resolution (`CHECK_OUT` vs `CHECK_IN`) is now explicitly propagated across all alarms, background sound services, and quick actions, with expanded default check-out keywords (`"mark check-out"`, `"out punch"`, `"punch out now"`, `"end shift"`).
- **Floating Mini-Window Maximization Fix**: Fixed race condition during full-screen maximization from mini floating portal window where `PortalActivity` rendered an uninitialized blank/black screen. Added synchronized `ConfigBundle` caching and guaranteed `WebView` initialization with active URL handoff.
- **Dynamic Floating Overlay Notifications**: Updated floating overlay foreground service notifications to reflect specific active action ("check-in" or "check-out").

---

## [2.2.0] - 2026-08-30

### Added & Enhanced
- **Holiday2API Integration**: Integrated `holiday2api.vercel.app` for dynamic multi-year Indian holidays (2024–2036), state-specific holiday filtering across all 36 States/UTs, upcoming holidays, and automatic background synchronization.
- **Offline-First Resilience**: Retained preloaded offline fallback datasets ensuring zero latency and 100% offline functionality.
- **Reactive Holiday State**: Updated home dashboard to reactively observe and refresh holiday calendars and long weekend badges seamlessly.

---

## [2.1.0] - 2026-08-27

### Added & Enhanced
- **Android Accessibility Principles**: Implemented comprehensive accessibility enhancements across UI components including screen reader (TalkBack) semantics, explicit element descriptions, custom action labels, semantic roles (`Role.Button`, `Role.Checkbox`, `Role.Tab`), merged legend descriptions, and minimum 48dp x 48dp touch target boundaries.
- **High-Contrast Calendar Day Numbers**: Made date numbers on non-WFO days (off-days, non-working days, and pre-install dates) solid high-contrast black (white in dark mode) so date numbers remain crisp and viewable everywhere.

---

## [2.0.0] - 2026-08-27

### Added
- **Weather & Commute Intelligence**: Integrated real-time weather forecasts (`WeatherService`), commute safety advisories, and Material 3 weather hero visuals into the daily home dashboard.
- **In-App Update Checker**: Introduced `UpdateAvailableDialog` and `UpdateChecker` targeting GitHub API releases to automatically inform users when newer versions of PingPin are released.
- **PDF Attendance Export Engine**: Formatted PDF export capabilities allowing users to customize date ranges and save clean PDF attendance logs.

### Fixed
- **Background Attendance & Wi-Fi Tracking**: Enhanced background service reliability for Wi-Fi check-in/check-out detection with WorkManager fallback polling and `BootReceiver` startup triggers.
- **Accessibility Enhancements**: Applied full accessibility audit fixes including explicit content descriptions, touch target sizing, high-contrast support, and dynamic scaling across UI components.
- **Calendar & Navigation Refinement**: Fixed padding and tile spacing in calendar views and optimized settings screen layouts.

---

## [1.9.0] - 2026-08-22

### Fixed
- **Alarm Alert Check-In & Check-Out Action Execution**: Replaced task stack termination (`finishAndRemoveTask()`) with standard activity finish (`finish()`) when launching portal or browser actions from `AlarmActivity`. Prevents the app from abruptly closing or going to background when tapping Check-In or Check-Out.
- **Dynamic Notification Action Resolution**: Resolved `ACTION_CHECK_IN` vs `ACTION_CHECK_OUT` dynamically based on alarm IDs (`CHECK_OUT_ALARM_ID`/`CHECK_OUT_SNOOZE_ID`), intent extras, and alarm titles across both `AlarmActivity` and `NotificationActionReceiver`.
- **Structured Coroutine Concurrency**: Bound portal action database operations to `lifecycleScope` in `AlarmActivity` for safe concurrency.

---

## [1.8.0] - 2026-08-20

### Added
- **Automatic Portal Redirection & Auto-Punch**: When opening the portal viewer, if redirected to a different page or login screen, PingPin handles authentication and automatically redirects the WebView directly to the target portal URL to perform the check-in or check-out action seamlessly without displaying error banners.
- **Framework-Aware Auto-Login Form Submission**: Enhanced credential auto-filling using native `HTMLInputElement` setters for Single-Page Applications (React, Vue, Angular), supporting both 1-step and 2-step (username -> next -> password -> submit) authentication flows.
- **Full Viewport Responsive Page Scaling**: Configured `MATCH_PARENT` layout parameters on WebView container with responsive zoom controls (`textZoom = 100`, `setSupportZoom(true)`) and dynamic `<meta name="viewport">` injection to eliminate page cropping.

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
