import 'package:flutter/material.dart';
import 'package:workmanager/workmanager.dart';
import 'package:permission_handler/permission_handler.dart';
import '../data/repositories/repositories.dart';
import '../data/database/app_database.dart';
import 'wifi_service.dart';
import 'attendance_service.dart';
import 'notification_service.dart';

const String backgroundWifiAttendanceTask = "com.example.pingpin.wifiCheckTask";

@pragma('vm:entry-point')
void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    try {
      final db = AppDatabase();
      final officeConfigRepo = OfficeConfigRepository(db);
      final attendanceRepo = AttendanceRepository(db);
      final config = await officeConfigRepo.getConfig();

      if (config != null) {
        final wifiService = WifiService();
        final attendanceService = AttendanceService(wifiService: wifiService);
        final notificationService = NotificationService();

        await attendanceService.checkAndMarkAttendance(
          officeConfig: config,
          attendanceRepo: attendanceRepo,
          onAttendanceMarked: () async {
            await notificationService.showAttendanceSuccess();

          },
        );
      }
    } catch (e) {
      debugPrint('Background Wi-Fi task error: $e');
    }
    return Future.value(true);
  });
}

class BackgroundService {
  /// Initializes Workmanager for background Wi-Fi attendance checks
  static Future<void> initializeBackgroundService() async {
    try {
      await Workmanager().initialize(
        callbackDispatcher,
        isInDebugMode: false,
      );
      await registerPeriodicWifiCheck();
    } catch (e) {
      debugPrint('Failed to initialize Workmanager: $e');
    }
  }

  /// Registers a periodic task (runs every 15 mins) to check Wi-Fi & auto mark attendance
  static Future<void> registerPeriodicWifiCheck() async {
    try {
      await Workmanager().registerPeriodicTask(
        "pingpin_wifi_periodic_task",
        backgroundWifiAttendanceTask,
        frequency: const Duration(minutes: 15),
        existingWorkPolicy: ExistingPeriodicWorkPolicy.keep,
        constraints: Constraints(
          networkType: NetworkType.connected,
        ),
      );
    } catch (e) {
      debugPrint('Failed to register periodic task: $e');
    }
  }


  /// Prompts Android to request battery optimization exemption for uninterrupted background execution.
  /// Checks whether battery optimization exemption is currently granted.
  static Future<bool> isBatteryOptimizationExempted() async {
    final status = await Permission.ignoreBatteryOptimizations.status;
    return status.isGranted;
  }

  /// Prompts Android to request battery optimization exemption for uninterrupted background execution.
  /// Automatically opens System Settings if user interaction is required.
  static Future<bool> requestBatteryOptimizationExemption() async {
    final status = await Permission.ignoreBatteryOptimizations.status;
    if (!status.isGranted) {
      final result = await Permission.ignoreBatteryOptimizations.request();
      if (!result.isGranted) {
        await openAppSettings();
      }
      return result.isGranted;
    }
    return true;
  }
}

