import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'package:alarm/alarm.dart';
import 'core/router/router.dart';
import 'core/theme/app_theme.dart';
import 'data/database/app_database.dart';
import 'providers/providers.dart';
import 'services/background_service.dart';
import 'services/notification_service.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initializeDateFormatting();

  runApp(
    const ProviderScope(
      child: PingPinApp(),
    ),
  );

  // Initialize background services asynchronously after first frame
  Future.microtask(() async {
    await BackgroundService.initializeBackgroundService();
    await _rescheduleAlarmsFromSavedConfig();
  });
}

/// Cancels all existing alarms and reschedules them from the saved OfficeConfig.
/// This ensures stale alarms (with wrong assetAudioPath from old builds) are replaced.
Future<void> _rescheduleAlarmsFromSavedConfig() async {
  try {
    await Alarm.init();
    await Alarm.stopAll(); // Cancel all stale / incorrectly configured alarms

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
    );
  }
}
