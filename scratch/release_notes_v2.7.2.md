# PingPin v2.7.2 Release Notes 🚀

## What's New & Enhancements

### 🔔 Alarm Notification Dismissal Fix
- **Instant Audio & Vibration Teardown**: Tapping **Dismiss** on the alarm notification now immediately halts audio playback, vibration, and wake locks. Resolved Android 12+ background service start restrictions (`ForegroundServiceStartNotAllowedException`) by using direct singleton instance signals.

### 🎛️ Redesigned Side-by-Side Vertical Sliders
- **Vertical Gesture Controls**: Replaced horizontal sliders on the Alarm screen with smooth, side-by-side vertical slide controls extending up to half the screen height.
- **Unobstructed Labels**: Optimized vertical label positioning (`60.dp` bottom offset) ensuring slider text and status indicators are always 100% visible and never covered by slider handle orbs.
- **Preserved Actions**: Kept the **Apply for Leave** action button cleanly positioned below the sliders.

### 🖼️ Slate OLED Glass PIP Overlay
- **Modern Floating Window**: Upgraded the Floating Auto Portal PIP window with M3 Slate OLED Glass styling (`#F20F172A`), brand accent borders, frosted headers, and refined glass action buttons (`⛶`, `─`, `✕`).

### ⚙️ Automation Status Pill Accuracy
- **Context-Aware Status**: Fixed Settings status pill logic so `"AUTOMATION READY"` appears strictly when **In-App Auto Portal** is selected, displaying `"PROFILE READY"` for external browser mode.

### ♿ Accessibility & Layout Stability
- **Large Font Scaling**: Capped maximum font scale at `1.15f` and added vertical scrolling on the Alarm screen to prevent text overlapping or UI clipping when Android accessibility options (Large display density / Big font size) are enabled.

### 🧹 Clean Production Release
- **No Developer Buttons**: Removed internal test alarm trigger buttons before building release.
