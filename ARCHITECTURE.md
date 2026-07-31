# Architecture Notes - PingPin

## Overview

PingPin follows a clean architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    Presentation Layer                    │
│  (UI Widgets, Screens, ConsumerWidgets with Riverpod)   │
├─────────────────────────────────────────────────────────┤
│                     State Management                     │
│              (Riverpod Providers & Notifiers)            │
├─────────────────────────────────────────────────────────┤
│                      Business Logic                      │
│                   (Services Layer)                       │
├─────────────────────────────────────────────────────────┤
│                      Data Access                         │
│              (Repositories + Drift Database)             │
└─────────────────────────────────────────────────────────┘
```

## Directory Structure

```
lib/
├── main.dart                 # App entry point, ProviderScope initialization
├── core/
│   ├── constants/
│   │   └── app_constants.dart    # Colors, strings, keys
│   ├── theme/
│   │   └── app_theme.dart        # ThemeData configuration
│   ├── utils/
│   │   └── date_utils.dart       # Date formatting, streak calculations
│   └── router/
│       └── router.dart           # GoRouter configuration
├── data/
│   ├── database/
│   │   ├── app_database.dart     # Drift schema + queries
│   │   └── database.dart         # Re-exports
│   └── repositories/
│       └── repositories.dart     # Repository classes + providers
├── features/
│   ├── onboarding/
│   │   └── onboarding_screen.dart
│   ├── home/
│   │   └── home_screen.dart
│   ├── insights/
│   │   └── insights_screen.dart
│   └── settings/
│       └── settings_screen.dart
├── services/
│   ├── attendance_service.dart   # Core attendance logic
│   ├── wifi_service.dart         # WiFi SSID checking
│   ├── location_service.dart     # GPS location handling
│   ├── notification_service.dart # Local notifications
│   └── pdf_export_service.dart   # PDF generation
└── providers/
    └── providers.dart            # Riverpod providers
```

## Key Design Decisions

### 1. State Management (Riverpod)

- **Providers**: All dependencies injected via Riverpod providers
- **StateNotifier**: Used for complex state (notification settings)
- **ConsumerWidget/ConsumerStatefulWidget**: UI reacts to state changes

### 2. Database (Drift)

- Single SQLite database with 4 tables
- Type-safe queries with compile-time verification
- Companion classes for inserts/updates
- Custom type converters (AttendanceStatus enum ↔ String)

### 3. Attendance Logic

The `AttendanceService` encapsulates the core business rule:

```dart
// Pseudocode
if (hasLocationPermission && 
    isConnectedToOfficeWiFi(ssid) && 
    isWithinRadius(lat, lon, radius)) {
    
    if (currentTime <= lateCutoff) {
        status = PRESENT
    } else {
        status = LATE
    }
    
    saveRecord(date, status, ssidSnapshot, distance)
}
```

### 4. Working Days Bitmask

Efficient storage of working days using bitwise operations:

```dart
// Monday = 1, Tuesday = 2, Wednesday = 4, etc.
// Default Mon-Fri = 1|2|4|8|16 = 31
final workingDaysMask = 31;

// Check if day is working day
bool isWorking = (workingDaysMask & (1 << (weekday - 1))) != 0;
```

### 5. Notification Throttling

Prevents spam by tracking last notification time:

```dart
// Near office warning: max once every 2 hours
// Permission warning: max once per day
// Success notification: once per successful day

Future<bool> shouldThrottle(String type, Duration duration) async {
    final lastTriggered = await getLastNotification(type);
    if (lastTriggered == null) return false;
    return DateTime.now().difference(lastTriggered) < duration;
}
```

### 6. Streak Calculation

Streaks only count eligible working days:

```dart
// Current streak: consecutive working days up to today with attendance
int calculateCurrentStreak(endDate, attendedDates, workingDaysMask) {
    int streak = 0;
    while (true) {
        if (!isWorkingDay(currentDate, mask)) {
            currentDate--; continue; // Skip non-working days
        }
        if (attendedDates.contains(dateStr)) {
            streak++; currentDate--;
        } else {
            break;
        }
    }
    return streak;
}
```

### 7. PDF Export

Generates professional PDF reports with:
- Header with month/year
- Employee profile summary
- Metrics (attendance %, total days, late count)
- Present dates list
- Late dates list

## Platform-Specific Considerations

### Android
- Requires runtime permissions for location, notifications (API 33+)
- Background location may need foreground service
- WiFi SSID access restricted in background on newer Android versions

### iOS
- Location permission requires usage description in Info.plist
- Background fetch is best-effort (not guaranteed)
- WiFi SSID limited when app is backgrounded

## Testing Strategy

1. **Unit Tests**: Business logic (date utils, streak calculations)
2. **Widget Tests**: UI components (calendar, metric cards)
3. **Integration Tests**: Full attendance flow (mocking services)

## Future Enhancements

1. **Background Checks**: Implement WorkManager for periodic checks
2. **Geofencing**: Use native geofencing APIs for better battery
3. **Export Formats**: CSV export in addition to PDF
4. **Dark Mode**: Theme variant support
5. **Localization**: i18n support for multiple languages
