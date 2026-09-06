## What's Changed in v2.4.5

### 🧹 Ponytail Over-Engineering Cleanups
- **Simplified AppInstallManager**: Removed redundant `@Volatile` in-memory cached fields over Android's native `SharedPreferences` (which maintains its own in-memory cache).
- **Streamlined Settings LaunchedEffect**: Cleaned up state comparison logic in settings auto-save.

### 🎨 UI & Calendar Polish
- **Squircle Calendar Cells**: Updated calendar day tiles to modern 12.dp squircles.
- **Dynamic Legend Filter Chips**: Added interactive filter chips with live status counts.
- **Refined Animations**: Optimized expand/collapse transition animations for calendar views.
