import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../data/repositories/repositories.dart';
import '../services/attendance_service.dart';
import '../services/notification_service.dart';
import '../core/constants/app_constants.dart';

/// Provider for theme mode management
final themeModeProvider = StateNotifierProvider<ThemeModeNotifier, ThemeMode>((ref) {
  return ThemeModeNotifier();
});

class ThemeModeNotifier extends StateNotifier<ThemeMode> {
  static const _themeModeKey = 'app_theme_mode';

  ThemeModeNotifier() : super(ThemeMode.system) {
    _loadThemeMode();
  }

  Future<void> _loadThemeMode() async {
    final prefs = await SharedPreferences.getInstance();
    final savedMode = prefs.getString(_themeModeKey);
    if (savedMode != null) {
      if (savedMode == 'light') state = ThemeMode.light;
      if (savedMode == 'dark') state = ThemeMode.dark;
      if (savedMode == 'system') state = ThemeMode.system;
    }
  }

  Future<void> setThemeMode(ThemeMode mode) async {
    state = mode;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_themeModeKey, mode.name);
  }
}

/// Provider for attendance service
final attendanceServiceProvider = Provider<AttendanceService>((ref) {
  return AttendanceService();
});

/// Provider for notification service
final notificationServiceProvider = Provider<NotificationService>((ref) {
  return NotificationService();
});

/// Provider for onboarding completion status
final onboardingCompleteProvider = StateProvider<bool>((ref) => false);

/// Provider for selected month in insights
final selectedMonthProvider = StateProvider<DateTime>((ref) {
  final now = DateTime.now();
  return DateTime(now.year, now.month, 1);
});

/// Provider for notification settings
final notificationSettingsProvider = StateNotifierProvider<NotificationSettingsNotifier, NotificationSettings>((ref) {
  return NotificationSettingsNotifier();
});

class NotificationSettings {
  final bool enableNearOfficeNotif;
  final bool enablePermissionWarning;
  final bool enableSuccessNotif;
  
  const NotificationSettings({
    this.enableNearOfficeNotif = true,
    this.enablePermissionWarning = true,
    this.enableSuccessNotif = true,
  });
  
  NotificationSettings copyWith({
    bool? enableNearOfficeNotif,
    bool? enablePermissionWarning,
    bool? enableSuccessNotif,
  }) {
    return NotificationSettings(
      enableNearOfficeNotif: enableNearOfficeNotif ?? this.enableNearOfficeNotif,
      enablePermissionWarning: enablePermissionWarning ?? this.enablePermissionWarning,
      enableSuccessNotif: enableSuccessNotif ?? this.enableSuccessNotif,
    );
  }
}

class NotificationSettingsNotifier extends StateNotifier<NotificationSettings> {
  NotificationSettingsNotifier() : super(const NotificationSettings());
  
  void toggleNearOfficeNotif(bool value) {
    state = state.copyWith(enableNearOfficeNotif: value);
  }
  
  void togglePermissionWarning(bool value) {
    state = state.copyWith(enablePermissionWarning: value);
  }
  
  void toggleSuccessNotif(bool value) {
    state = state.copyWith(enableSuccessNotif: value);
  }
}

/// Provider for smart notification manager
final smartNotificationManagerProvider = Provider<SmartNotificationManager>((ref) {
  return SmartNotificationManager(
    notificationService: ref.watch(notificationServiceProvider),
    notificationLogRepo: ref.watch(notificationLogRepositoryProvider),
    settings: ref.watch(notificationSettingsProvider),
  );
});

/// Manages smart notifications with throttling
class SmartNotificationManager {
  final NotificationService _notificationService;
  final NotificationLogRepository _notificationLogRepo;
  final NotificationSettings _settings;
  
  SmartNotificationManager({
    required NotificationService notificationService,
    required NotificationLogRepository notificationLogRepo,
    required NotificationSettings settings,
  })  : _notificationService = notificationService,
        _notificationLogRepo = notificationLogRepo,
        _settings = settings;
  
  /// Shows near office notification with throttling (max once every 2 hours)
  Future<void> showNearOfficeNotOnWifi() async {
    if (!_settings.enableNearOfficeNotif) return;
    
    const throttleDuration = Duration(hours: 2);
    final shouldThrottle = await _notificationLogRepo.shouldThrottle(
      type: AppStrings.notifNearOfficeNotOnWifi,
      throttleDuration: throttleDuration,
    );
    
    if (!shouldThrottle) {
      await _notificationService.showNearOfficeNotOnWifi();
      await _notificationLogRepo.logNotification(
        type: AppStrings.notifNearOfficeNotOnWifi,
        triggeredAt: DateTime.now(),
        dateYyyyMmDd: getCurrentDateYyyyMmDd(),
      );
    }
  }
  
  /// Shows permission warning with throttling (max once per day)
  Future<void> showPermissionWarning() async {
    if (!_settings.enablePermissionWarning) return;
    
    const throttleDuration = Duration(days: 1);
    final shouldThrottle = await _notificationLogRepo.shouldThrottle(
      type: AppStrings.notifPermissionWarning,
      throttleDuration: throttleDuration,
    );
    
    if (!shouldThrottle) {
      await _notificationService.showPermissionWarning();
      await _notificationLogRepo.logNotification(
        type: AppStrings.notifPermissionWarning,
        triggeredAt: DateTime.now(),
        dateYyyyMmDd: getCurrentDateYyyyMmDd(),
      );
    }
  }
  
  /// Shows success notification (once per successful day)
  Future<void> showSuccessNotification() async {
    if (!_settings.enableSuccessNotif) return;
    
    const throttleDuration = Duration(days: 1);
    final shouldThrottle = await _notificationLogRepo.shouldThrottle(
      type: AppStrings.notifSuccess,
      throttleDuration: throttleDuration,
    );
    
    if (!shouldThrottle) {
      await _notificationService.showAttendanceSuccess();
      await _notificationLogRepo.logNotification(
        type: AppStrings.notifSuccess,
        triggeredAt: DateTime.now(),
        dateYyyyMmDd: getCurrentDateYyyyMmDd(),
      );
    }
  }
}

String getCurrentDateYyyyMmDd() {
  final now = DateTime.now();
  return '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
}
