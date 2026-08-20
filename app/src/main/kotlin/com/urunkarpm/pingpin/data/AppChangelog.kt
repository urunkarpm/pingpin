package com.urunkarpm.pingpin.data

object AppChangelog {
    const val CURRENT_VERSION_CHANGELOG = """
### Added
- **HR Portal Opened URL Verification & Safeguard**: Real-time URL checking on WebView page load events (`onPageStarted`, `onPageFinished`) and in-engine JS verification (`checkUrlMismatch`). Displays an amber warning banner and single-tap **"Load Target URL"** button if the browser hops or redirects to a different page.
- **Framework-Aware Auto-Login Form Submission**: Enhanced credential auto-filling using native `HTMLInputElement` setters for Single-Page Applications (React, Vue, Angular), supporting both 1-step and 2-step (username -> next -> password -> submit) authentication flows.
- **Full Viewport Responsive Page Scaling**: Configured `MATCH_PARENT` layout parameters on WebView container with responsive zoom controls (`textZoom = 100`, `setSupportZoom(true)`) and dynamic `<meta name="viewport">` injection to eliminate page cropping.
"""

    const val FULL_CHANGELOG = """
## [1.8.0] - Current Release

### Added
- **HR Portal Opened URL Verification & Safeguard**: Real-time URL checking on WebView page load events (`onPageStarted`, `onPageFinished`) and in-engine JS verification (`checkUrlMismatch`). Displays an amber warning banner and single-tap **"Load Target URL"** button if the browser hops or redirects to a different page.
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
