### PingPin v2.7.0 - Extra WFO Attendance Tracking & Performance Overhaul

#### 🚀 What's New & Refined
- **Extra WFO Attendance Detection**: Added full support for tracking office visits made on non-WFO days. Check-ins on non-WFO days now contribute accurately to overall attendance metrics without calculation errors.
- **Uncapped Compliance Rate**: Updated compliance calculation on the Insights page to accurately reflect over-achievement (>100% compliance) when users visit the office more than designated WFO target days.
- **Electric Blue Calendar Badging**: Added dedicated `ElectricBlue` (`#0284C7`) visual markers, squircle borders, status dots, and legend filter options (`EXTRA_WFO`) across calendar cells and monthly log views.
- **Executive PDF Export Statement**: PDF reports now include non-WFO attendance records in detailed log tables, highlight `EXCEEDED TARGET` compliance statuses, and display `EXTRA WFO` status pills in `ElectricBlue`.
- **Signed APK (Play Protect Compliant)**: Build configured with release keystore signing (`upload-keystore.jks`) to prevent Android Play Protect untrusted developer warnings.
- **Ponytail Performance Polish**: Eliminated heavy dynamic blur rendering overhead, removed 24/7 infinite transition CPU recomposition loops, and optimized calendar cell graphics layers for smooth 120fps interactions.
