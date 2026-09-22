# PingPin v2.6.2 Release

## What's New & Refined

### Laws of UX Tactile & Target Sizing Overhaul
- **Instant Tactile Haptic Feedback**: Replaced non-standard handle feedback with native `HapticFeedbackType.LongPress` across interactive cards (`GlassCard`), custom switches (`PingPinSwitch`), and day selectors (`WfoDaysSelector`, `WorkingDaysSelector`) to provide instant feedback fulfilling the **Doherty Threshold** (<400ms response) and **Peak-End Rule**.
- **Fitts's Law Touch Target Sizing**: Enforced 48dp minimum touch target bounds (`minimumTouchTargetSize`) across all day selector pills and interactive controls to eliminate misclicks across screen densities.
- **Jakob's Law Accessibility Semantics**: Standardized semantic state descriptions (`"Selected"` / `"Not selected"`, `"On"` / `"Off"`) and accessibility roles across all custom widgets to conform with native Android platform mental models.

### 60/120fps Smooth Blur Load/Offload Animation
- **Animated Blur Transitions**: Replaced harsh instant modifier toggling with native Compose `animateDpAsState` spring animations (`Spring.StiffnessMediumLow`) for backdrop blur on bottom sheets and calendar overlays.
- **Zero-Stutter Load/Offload**: Smoothly interpolates blur radius frame-by-frame from `0.dp` to `16.dp`, eliminating visual pop-in/pop-out delay on 120Hz displays.
