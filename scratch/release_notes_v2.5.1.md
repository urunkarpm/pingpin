# PingPin v2.5.1 Release - Polish & Performance

### 🌟 What's New & Refined

#### 📅 Calendar Legend Scroll Wheel
- Upgraded the calendar legend dropdown into a compact in-place vertical scroll wheel selector (`170dp x 36dp`).
- Added smooth gesture handling, animated indicator dots, and consistent sizing without unwanted popups or press shadows.

#### 🌊 Edge-to-Edge Backdrop Blur
- Replaced background dark scrim shadows on Weather Detail and Holiday Radar bottom sheets with frosted glass `Modifier.blur(16.dp)` background blur.
- Configured edge-to-edge window insets (`windowInsets = WindowInsets(0, 0, 0, 0)`) so the backdrop extends seamlessly behind the transparent status bar while keeping status bar time and icons sharp.

#### ☔ Rain Probability Trend Graph Fix
- Fixed the 24-hour rain chance trend curve being hidden underneath hourly weather cards by switching to a stacked `Column` layout with a dedicated `52dp` curve canvas height.

#### ⚡ Smooth 60/120Hz Animations
- Removed conflicting height animation wrappers in `LiquidGlassNavBar` for smooth bottom bar tab transitions.
- Added stable Compose item keys and remembered calculations to eliminate frame drops during bottom sheet slide animations.

#### 🛠️ System Health Streamlining
- Cleaned up redundant manual test alarm scheduling button in Settings for a clean, clutter-free System Health section.
