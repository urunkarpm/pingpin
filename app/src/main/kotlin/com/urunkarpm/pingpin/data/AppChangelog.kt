package com.urunkarpm.pingpin.data

object AppChangelog {
    const val CURRENT_VERSION_CHANGELOG = """
### Added & Enhanced
- **Android Accessibility Principles**: Full accessibility audit compliance including TalkBack screen reader support, explicit element content descriptions, custom accessibility action labels, semantic roles (`Role.Button`, `Role.Checkbox`, `Role.Tab`), merged legend descriptions, and minimum 48dp x 48dp touch target boundaries.
- **High-Contrast Calendar Day Numbers**: Solid high-contrast black (white in dark mode) calendar numbers on non-WFO days for crisp readability across all light & dark themes.
- **Weather & Commute Intelligence**: Integrated real-time weather forecasts (`WeatherService`), commute safety advisories, and Material 3 weather visuals into the daily home dashboard.
- **In-App Update Checker**: Automated GitHub API release update notifications with 1-tap installation via `UpdateAvailableDialog`.
- **PDF Attendance Export Engine**: Formatted PDF export capabilities allowing users to customize date ranges and save clean PDF attendance logs.
"""

    const val FULL_CHANGELOG = """
## [2.1.0] - Current Release

### Added & Enhanced
- **Android Accessibility Principles**: Full accessibility audit compliance including TalkBack screen reader support, explicit element content descriptions, custom accessibility action labels, semantic roles (`Role.Button`, `Role.Checkbox`, `Role.Tab`), merged legend descriptions, and minimum 48dp x 48dp touch target boundaries.
- **High-Contrast Calendar Day Numbers**: Solid high-contrast black (white in dark mode) calendar numbers on non-WFO days for crisp readability across all light & dark themes.
- **Weather & Commute Intelligence**: Integrated real-time weather forecasts (`WeatherService`), commute safety advisories, and Material 3 weather visuals into the daily home dashboard.
- **In-App Update Checker**: Automated GitHub API release update notifications with 1-tap installation via `UpdateAvailableDialog`.
- **PDF Attendance Export Engine**: Formatted PDF export capabilities allowing users to customize date ranges and save clean PDF attendance logs.

---

## [2.0.0]

### Added & Enhanced
- **High-Contrast Calendar Day Numbers**: Made date numbers on non-WFO days (off-days, non-working days, and pre-install dates) solid high-contrast black (white in dark mode) so date numbers remain crisp and viewable everywhere on all backgrounds.
- **Weather & Commute Intelligence**: Integrated real-time weather forecasts (`WeatherService`), commute safety advisories, and Material 3 weather hero visuals into the daily home dashboard.
- **In-App Update Checker**: Introduced `UpdateAvailableDialog` and `UpdateChecker` targeting GitHub API releases to automatically inform users when newer versions of PingPin are released.
- **PDF Attendance Export Engine**: Formatted PDF export capabilities allowing users to customize date ranges and save clean PDF attendance logs.
- **Background Attendance & Wi-Fi Tracking**: Enhanced background service reliability for Wi-Fi check-in/check-out detection with WorkManager fallback polling and `BootReceiver` startup triggers.
- **Accessibility Enhancements**: Applied full accessibility audit fixes including explicit content descriptions, touch target sizing, high-contrast support, and dynamic scaling across UI components.
- **Calendar & Navigation Refinement**: Fixed padding and tile spacing in calendar views and optimized settings screen layouts.

---

## [1.9.0]

### Fixed
- **Alarm Alert Check-In & Check-Out Action Execution**: Replaced task stack termination (`finishAndRemoveTask()`) with standard activity finish (`finish()`) when launching portal or browser actions from `AlarmActivity`. Prevents the app from abruptly closing or going to background when tapping Check-In or Check-Out.
- **Dynamic Notification Action Resolution**: Resolved `ACTION_CHECK_IN` vs `ACTION_CHECK_OUT` dynamically based on alarm IDs (`CHECK_OUT_ALARM_ID`/`CHECK_OUT_SNOOZE_ID`), intent extras, and alarm titles across both `AlarmActivity` and `NotificationActionReceiver`.
- **Structured Coroutine Concurrency**: Bound portal action database operations to `lifecycleScope` in `AlarmActivity` for safe concurrency.

---

## [1.8.0]

### Added
- **Automatic Portal Redirection & Auto-Punch**: When opening the portal viewer, if redirected to a different page or login screen, PingPin handles authentication and automatically redirects the WebView directly to the target portal URL to perform the check-in or check-out action seamlessly without displaying error banners.
- **Framework-Aware Auto-Login Form Submission**: Enhanced credential auto-filling using native `HTMLInputElement` setters for Single-Page Applications (React, Vue, Angular), supporting both 1-step and 2-step (username -> next -> password -> submit) authentication flows.
- **Full Viewport Responsive Page Scaling**: Configured `MATCH_PARENT` layout parameters on WebView container with responsive zoom controls (`textZoom = 100`, `setSupportZoom(true)`) and dynamic `<meta name="viewport">` injection to eliminate page cropping.

---

## [1.7.0]

### Added
- **GitHub Release Update Checker & Installer**: Check for new app updates directly in Settings via GitHub Releases, download APKs, and install with 1-tap.
- **Explicit Onboarding Preference Tracking**: Added `OnboardingPreferences` to ensure initial setup is only marked complete after explicit confirmation at Step 7 ("COMPLETE & LAUNCH").
- **Auto-Cancellation of Makeup WFO Suggestions**: Automatically cancels pending or accepted makeup WFO suggestions and clears scheduled notification alarms when all required WFO days for the current week are completed or when a missed WFO date is marked present in the calendar.
- **Wizard Back Navigation**: Added Compose `BackHandler` support to the initial setup screen (`OnboardingScreen`) to step backward through setup wizard pages on system back action.

### Changed
- **Calendar Color Scheme Refinement**: Updated **WFO Day (Scheduled WFO)** to **Royal Purple** (`WfoDayPurple` - `#8B5CF6`) with soft lavender fills (`#F3E8FF` / `#4C1D95`) and purple status dots.
- **Today Day Cell Styling**: Today is now uniquely identified by a prominent 2.5dp **Electric Blue Ring** (`#3B82F6`), eliminating visual confusion between Present (Green), Today (Blue Ring), WFO Day (Purple), Makeup WFO (Amber), and Missed (Red).

### Fixed
- **Premature Setup Completion Bug**: Fixed an issue where clicking "Run Test" in Step 6 of initial setup saved a temporary configuration to Room DB and prematurely navigated to the App Home screen on Back press before settings and user profile were finalized.
- **Visual Overlap on Calendar Grid**: Resolved color overlap between Present (Green), WFO Day (Purple), and Today (Blue Ring) tiles.

---

## [1.6.0]

### Added
- Embedded `PortalActivity` WebView auto-punch engine (`PortalAutoCheckInEngine`).
- Adaptive Navigation Rail layout for larger screens and tablets.
- Architectural clarification separating local Wi-Fi calendar tracking from company HR portal check-in.

---

## [1.5.0]

### Added
- Expandable Weekly Calendar Strip (`ExpandableWeeklyCalendarCard`) with 7-day strip and monthly view toggle.
- Custom notification alarm chime sound (`beep.mp3`) and full-screen alarm alert UI (`AlarmActivity`).
"""
}
