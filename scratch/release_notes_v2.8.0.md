# PingPin v2.8.0 Release Notes 🚀

## What's New & Enhancements

### 🖥️ In-App Portal Diagnostics & Real-Time Console
- **Interactive Live Logging**: Integrated an expandable live automation diagnostics console directly into `PortalActivity`. Live-streams DOM scanning attempts, detected buttons, keyword matches, and real-time status feedback during auto check-in.
- **Problem State Alerts**: Automatically surfaces diagnostic notices and actionable suggestions (e.g. detected page buttons, keyword hints) if an expected check-in or check-out button isn't matched within the retry threshold.

### 🛡️ Clean Permission-Light Architecture
- **Overlay Window Decommissioned**: Completely removed the invasive "Display over other apps" (`SYSTEM_ALERT_WINDOW`) permission requirement.
- **Streamlined Workflow**: Retired the background `FloatingPortalService` overlay in favor of a fast, native in-app portal workflow that eliminates OS-level permission friction and background service restrictions.

### ⚡ Hardened Web Automation Engine
- **Deep DOM & iFrame Traversal**: `PortalAutoCheckInEngine` now seamlessly penetrates nested iframes and Shadow DOM boundaries commonly used in modern enterprise HR/payroll portals.
- **Smart Heuristics**: Improved fuzzy keyword matching for check-in and check-out actions, and enhanced automated punch verification to distinguish pre-existing status text from real-time punch acknowledgements.
- **Unit Testing Coverage**: Backed by a comprehensive unit testing suite verifying URL parsing, script injection, and verification logic.

### 🚀 High-Performance Rendering & Tactile UX
- **Calendar Squircle Allocation**: Reused static squircle shape singletons (`DayCellSquircleShape`) and optimized click modifiers across monthly calendar views, eliminating memory pressure and frame drops during scrolling.
- **Instantaneous Day Selectors**: Streamlined WFO and working day selector pills to zero-overhead static layouts, providing sub-100ms tactile feedback conforming to the Doherty Threshold.
- **Backdrop Animations**: Smooth fade transitions for full calendar overlays on the home dashboard.
