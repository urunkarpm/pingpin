import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;
import 'package:timezone/data/latest_all.dart' as tz_data;
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:alarm/alarm.dart';
import '../core/constants/app_constants.dart';
import '../data/database/app_database.dart';
import '../core/router/router.dart';
import '../features/alarm/alarm_ringing_screen.dart';

/// Service for managing local notifications and Check-in / Check-out alarms
class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin _notifications =
      FlutterLocalNotificationsPlugin();

  bool _isInitialized = false;
  String _storedPortalUrl = '';

  static bool _isAlarmScreenShowing = false;

  /// Helper to safely present the AlarmRingingScreen
  static void showAlarmScreen(int alarmId) {
    final context = rootNavigatorKey.currentContext;
    if (context != null && !_isAlarmScreenShowing) {
      _isAlarmScreenShowing = true;
      Navigator.of(context, rootNavigator: true)
          .push(
        MaterialPageRoute(
          builder: (_) => AlarmRingingScreen(
            alarmId: alarmId,
            portalUrl: _instance._storedPortalUrl,
          ),
        ),
      )
          .then((_) {
        _isAlarmScreenShowing = false;
      });
    }
  }

  /// Initialize notification service
  Future<void> initialize({String? portalUrl}) async {
    if (portalUrl != null && portalUrl.isNotEmpty) {
      _storedPortalUrl = portalUrl;
    }
    if (_isInitialized) return;

    // Initialize timezone & Alarm package
    tz_data.initializeTimeZones();
    try {
      final String timeZoneName = await FlutterTimezone.getLocalTimezone();
      tz.setLocalLocation(tz.getLocation(timeZoneName));
    } catch (_) {
      try {
        final String timeZoneName = DateTime.now().timeZoneName;
        tz.setLocalLocation(tz.getLocation(timeZoneName));
      } catch (_) {
        // Fallback if lookup fails
      }
    }
    await Alarm.init();

    // ─── Single source of truth for the full-screen alarm UI ───────────────
    Alarm.ringStream.stream.listen((alarmSettings) {
      showAlarmScreen(alarmSettings.id);
    });

    // Android initialization settings
    const androidSettings =
        AndroidInitializationSettings('@mipmap/ic_launcher');

    // iOS initialization settings
    const iosSettings = DarwinInitializationSettings(
      requestAlertPermission: true,
      requestBadgePermission: true,
      requestSoundPermission: true,
    );

    const initSettings = InitializationSettings(
      android: androidSettings,
      iOS: iosSettings,
    );

    await _notifications.initialize(
      initSettings,
      onDidReceiveNotificationResponse: _onNotificationResponse,
      onDidReceiveBackgroundNotificationResponse: notificationTapBackground,
    );

    // Check if app was cold-launched by a fullScreenIntent or notification tap
    final launchDetails =
        await _notifications.getNotificationAppLaunchDetails();
    if (launchDetails != null && launchDetails.didNotificationLaunchApp) {
      final response = launchDetails.notificationResponse;
      if (response != null && (response.id == 101 || response.id == 102)) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          showAlarmScreen(response.id!);
        });
      }
    }

    // Request permissions & setup channels
    await _requestPermissions();

    _isInitialized = true;
  }

  void setPortalUrl(String url) {
    _storedPortalUrl = url;
  }

  /// Handles taps & fullScreenIntent launches for notifications
  Future<void> _onNotificationResponse(NotificationResponse response) async {
    if (response.id == 101 || response.id == 102) {
      if (response.id != null) {
        showAlarmScreen(response.id!);
      }
      return;
    }

    final payloadUrl =
        (response.payload != null && response.payload!.startsWith('http'))
            ? response.payload!
            : _storedPortalUrl;

    if (response.payload != null) {
      await openPortal(payloadUrl);
    }
  }

  /// Launch portal URL in default browser
  Future<void> openPortal(String urlStr) async {
    var rawUrl =
        urlStr.trim().isNotEmpty ? urlStr.trim() : _storedPortalUrl.trim();
    if (rawUrl.isEmpty) rawUrl = 'https://google.com';

    if (!rawUrl.startsWith('http://') && !rawUrl.startsWith('https://')) {
      rawUrl = 'https://$rawUrl';
    }

    final uri = Uri.tryParse(rawUrl);
    if (uri != null && (uri.isScheme('HTTP') || uri.isScheme('HTTPS'))) {
      try {
        await launchUrl(uri, mode: LaunchMode.externalApplication);
      } catch (e) {
        debugPrint('Error launching portal URL ($rawUrl): $e');
      }
    } else {
      debugPrint('Blocked launch of non-HTTP/HTTPS portal URL: $rawUrl');
    }
  }

  /// Launch email client with a leave application draft
  Future<void> openLeaveMail() async {
    final Uri mailUri = Uri(
      scheme: 'mailto',
      path: '',
      queryParameters: {
        'subject': 'Leave Application',
        'body': 'Dear Team,\n\nI will be taking leave today.\n\nThank you,',
      },
    );

    try {
      await launchUrl(mailUri, mode: LaunchMode.externalApplication);
    } catch (e) {
      debugPrint('Error launching mail app: $e');
    }
  }

  Future<void> _requestPermissions() async {
    final plugin = _notifications
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>();

    // Delete legacy notification channels to force Android to re-create channel with updated audio settings
    await plugin?.deleteNotificationChannel('alarm_channel');
    await plugin?.deleteNotificationChannel('alarm_channel_v2');

    const attendanceChannel = AndroidNotificationChannel(
      'attendance_channel',
      'Attendance Notifications',
      description: 'Notifications for attendance marking and reminders',
      importance: Importance.high,
      playSound: true,
      enableVibration: false, // vibration only via alarm package
    );

    // alarm_channel_v3 uses Android System Default Selected Alarm Tone & fullScreenIntent
    const alarmChannel = AndroidNotificationChannel(
      'alarm_channel_v3',
      'Check-In & Check-Out Clock Alarms',
      description: 'Daily check-in and check-out alarms',
      importance: Importance.max,
      playSound: true,
      enableVibration: true,
      audioAttributesUsage: AudioAttributesUsage.alarm,
    );

    await plugin?.createNotificationChannel(attendanceChannel);
    await plugin?.createNotificationChannel(alarmChannel);
    await plugin?.requestNotificationsPermission();
    await plugin?.requestExactAlarmsPermission();
  }

  /// Calculates the next occurrence of a given HH:mm time
  DateTime _nextInstanceOfTime(int hour, int minute) {
    final now = DateTime.now();
    DateTime scheduled =
        DateTime(now.year, now.month, now.day, hour, minute);
    if (scheduled.isBefore(now)) {
      scheduled = scheduled.add(const Duration(days: 1));
    }
    return scheduled;
  }

  // ─── Alarm scheduling helpers ─────────────────────────────────────────────



  /// Schedule Check-In Daily Clock Alarm
  Future<void> scheduleCheckInAlarm(
      {required String checkInTimeStr, String? portalUrl}) async {
    if (!_isInitialized) await initialize(portalUrl: portalUrl);
    if (portalUrl != null) _storedPortalUrl = portalUrl;

    final parts = checkInTimeStr.split(':');
    if (parts.length < 2) return;
    final hour = int.parse(parts[0]);
    final minute = int.parse(parts[1]);
    final scheduledDate = _nextInstanceOfTime(hour, minute);

    // 1. Alarm package — handles full screen launch + default system alarm sound + vibration
    await Alarm.set(
      alarmSettings: AlarmSettings(
        id: 101,
        dateTime: scheduledDate,
        assetAudioPath: 'assets/audio/beep.mp3',
        loopAudio: true,
        vibrate: true,
        volume: 1.0,
        fadeDuration: 0.0,
        notificationSettings: const NotificationSettings(
          title: 'CHECK-IN ALARM',
          body: 'Tap Check-in to open portal or Leave to apply for leave.',
          stopButton: 'Dismiss',
        ),
        warningNotificationOnKill: true,
        androidFullScreenIntent: true,
      ),
    );

    // Cancel any standalone local notification with ID 101 to avoid notification center popups
    await _notifications.cancel(101);
  }

  /// Schedule Check-Out Daily Clock Alarm
  Future<void> scheduleCheckOutAlarm(
      {required String checkOutTimeStr, String? portalUrl}) async {
    if (!_isInitialized) await initialize(portalUrl: portalUrl);
    if (portalUrl != null) _storedPortalUrl = portalUrl;

    final parts = checkOutTimeStr.split(':');
    if (parts.length < 2) return;
    final hour = int.parse(parts[0]);
    final minute = int.parse(parts[1]);
    final scheduledDate = _nextInstanceOfTime(hour, minute);

    // 1. Alarm package
    await Alarm.set(
      alarmSettings: AlarmSettings(
        id: 102,
        dateTime: scheduledDate,
        assetAudioPath: 'assets/audio/beep.mp3',
        loopAudio: true,
        vibrate: true,
        volume: 1.0,
        fadeDuration: 0.0,
        notificationSettings: const NotificationSettings(
          title: 'CHECK-OUT ALARM',
          body: 'Tap Check-out to open portal.',
          stopButton: 'Dismiss',
        ),
        warningNotificationOnKill: true,
        androidFullScreenIntent: true,
      ),
    );

    // Cancel any standalone local notification with ID 102 to avoid notification center popups
    await _notifications.cancel(102);
  }

  /// Schedule both alarms from OfficeConfig
  Future<void> scheduleAlarmsFromConfig(OfficeConfig config) async {
    await scheduleCheckInAlarm(
        checkInTimeStr: config.checkInTime, portalUrl: config.portalUrl);
    await scheduleCheckOutAlarm(
        checkOutTimeStr: config.checkOutTime, portalUrl: config.portalUrl);
  }

  /// Test Check-in Alarm — fires in 5 seconds.
  /// The ringStream listener above shows AlarmRingingScreen automatically.
  /// No Future.delayed or manual screen push here.
  Future<void> testCheckInAlarmNow({String? portalUrl}) async {
    if (!_isInitialized) await initialize(portalUrl: portalUrl);
    if (portalUrl != null) _storedPortalUrl = portalUrl;

    final scheduledDate = DateTime.now().add(const Duration(seconds: 5));

    // 1. Alarm package
    await Alarm.set(
      alarmSettings: AlarmSettings(
        id: 101,
        dateTime: scheduledDate,
        assetAudioPath: 'assets/audio/beep.mp3',
        loopAudio: true,
        vibrate: true,
        volume: 1.0,
        fadeDuration: 0.0,
        notificationSettings: const NotificationSettings(
          title: 'TEST CHECK-IN ALARM',
          body: 'Tap Check-in to open portal or Leave to apply for leave.',
          stopButton: 'Dismiss',
        ),
        warningNotificationOnKill: true,
        androidFullScreenIntent: true,
      ),
    );

    await _notifications.cancel(101);
  }

  /// Shows attendance success notification
  Future<void> showAttendanceSuccess() async {
    if (!_isInitialized) await initialize();
    const androidDetails = AndroidNotificationDetails(
      'attendance_channel',
      'Attendance Notifications',
      importance: Importance.high,
      priority: Priority.high,
      icon: '@mipmap/ic_launcher',
    );
    await _notifications.show(
      1,
      'Attendance Marked Successfully',
      'Your attendance has been recorded for today.',
      const NotificationDetails(android: androidDetails),
      payload: AppStrings.notifSuccess,
    );
  }

  /// Shows near office notification
  Future<void> showNearOfficeNotOnWifi() async {
    if (!_isInitialized) await initialize();
    const androidDetails = AndroidNotificationDetails(
      'attendance_channel',
      'Attendance Notifications',
      importance: Importance.high,
      priority: Priority.high,
      icon: '@mipmap/ic_launcher',
    );
    await _notifications.show(
      2,
      'Near Office',
      'You are near the office but not connected to office WiFi.',
      const NotificationDetails(android: androidDetails),
    );
  }

  /// Shows permission warning notification
  Future<void> showPermissionWarning() async {
    if (!_isInitialized) await initialize();
    const androidDetails = AndroidNotificationDetails(
      'attendance_channel',
      'Attendance Notifications',
      importance: Importance.high,
      priority: Priority.high,
      icon: '@mipmap/ic_launcher',
    );
    await _notifications.show(
      3,
      'Permission Warning',
      'Please check app permissions to ensure attendance tracking works properly.',
      const NotificationDetails(android: androidDetails),
    );
  }

  /// Cancels all notifications and alarms
  Future<void> cancelAll() async {
    await Alarm.stopAll();
    await _notifications.cancelAll();
  }

  /// Cancels a specific alarm + notification by ID
  Future<void> cancel(int id) async {
    await Alarm.stop(id);
    await _notifications.cancel(id);
  }

  /// Cancels only the check-out alarm (ID 102).
  /// Called when the user selects Leave at check-in time.
  Future<void> cancelCheckOutAlarm() async {
    await Alarm.stop(102);
    await _notifications.cancel(102);
  }
}

@pragma('vm:entry-point')
void notificationTapBackground(NotificationResponse response) {
  // Background tap handler — fullScreenIntent or notification tap brings MainActivity to foreground,
  // where _onNotificationResponse or getNotificationAppLaunchDetails presents AlarmRingingScreen.
}
