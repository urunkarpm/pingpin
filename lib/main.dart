import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:alarm/alarm.dart';
import 'core/router/router.dart';
import 'core/theme/app_theme.dart';
import 'data/database/app_database.dart';
import 'providers/providers.dart';
import 'services/background_service.dart';
import 'services/notification_service.dart';
import 'services/oem_battery_helper.dart';
import 'core/widgets/theme_transition_wrapper.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting();

  runApp(
    const ProviderScope(
      child: PingPinApp(),
    ),
  );

  // Initialize background services asynchronously after first frame.
  // CRITICAL: NotificationService must be initialized FIRST so that the
  // ringStream listener is attached before Alarm.init() fires. This ensures
  // the full-screen alarm UI appears even when the app is cold-started by a
  // ringing alarm (screen off / app killed / swiped away).
  Future.microtask(() async {
    await OemBatteryHelper.init(); // cache device manufacturer/brand for OEM detection
    await NotificationService().initialize(); // sets up ringStream listener + Alarm.init()
    await BackgroundService.initializeBackgroundService();
    await _rescheduleAlarmsFromSavedConfig();
  });
}

/// Cancels all existing alarms and reschedules them from the saved OfficeConfig.
/// This ensures stale alarms (with wrong assetAudioPath from old builds) are replaced.
///
/// Guard: if an alarm is due within the next 60 seconds we skip stopAll() to
/// avoid a race where the cancellation races the imminent alarm firing.
Future<void> _rescheduleAlarmsFromSavedConfig() async {
  try {
    // Alarm.init() has already been called by NotificationService.initialize()
    // above, so no need to call it again here.

    // Only stopAll if no alarm is about to fire in the next 60 seconds.
    final pending = await Alarm.getAlarms();
    final now = DateTime.now();
    final hasImminent = pending.any((a) =>
        a.dateTime.isAfter(now) &&
        a.dateTime.difference(now).inSeconds <= 60);

    if (!hasImminent) {
      await Alarm.stopAll(); // Cancel stale / incorrectly configured alarms
    }

    final db = AppDatabase();
    final config = await db.getOfficeConfig();
    if (config != null) {
      await NotificationService().scheduleAlarmsFromConfig(config);
    }
  } catch (e) {
    debugPrint('Alarm reschedule on startup failed: $e');
  }
}

class PingPinApp extends ConsumerWidget {
  const PingPinApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    final themeMode = ref.watch(themeModeProvider);

    return MaterialApp.router(
      title: 'PingPin',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: themeMode,
      routerConfig: router,
      builder: (context, child) {
        return ThemeTransitionWrapper(
          child: child ?? const SizedBox.shrink(),
        );
      },
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      supportedLocales: const [
        Locale('en', ''),
      ],
    );
  }
}
