import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:timezone/timezone.dart' as tz;
import 'package:timezone/data/latest_all.dart' as tz_data;
import '../core/constants/app_constants.dart';
import '../data/database/app_database.dart';

/// Service for managing local notifications
class NotificationService {
  final FlutterLocalNotificationsPlugin _notifications = 
      FlutterLocalNotificationsPlugin();
  
  bool _isInitialized = false;
  
  /// Initialize notification service
  Future<void> initialize() async {
    if (_isInitialized) return;
    
    // Initialize timezone
    tz_data.initializeTimeZones();
    
    // Android initialization settings
    const androidSettings = AndroidInitializationSettings('@mipmap/ic_launcher');
    
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
    );
    
    // Request permissions
    await _requestPermissions();
    
    _isInitialized = true;
  }
  
  void _onNotificationResponse(NotificationResponse response) {
    // Handle notification tap if needed
    print('Notification tapped: ${response.payload}');
  }
  
  Future<void> _requestPermissions() async {
    const androidPermissions = AndroidNotificationChannel(
      'attendance_channel',
      'Attendance Notifications',
      description: 'Notifications for attendance marking and reminders',
      importance: Importance.high,
      playSound: true,
      enableVibration: true,
    );
    
    await _notifications
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.createNotificationChannel(androidPermissions);
    
    await _notifications
        .resolvePlatformSpecificImplementation<
            AndroidFlutterLocalNotificationsPlugin>()
        ?.requestNotificationsPermission();
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
    
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    
    const details = NotificationDetails(android: androidDetails, iOS: iosDetails);
    
    await _notifications.show(
      1,
      'Attendance Marked Successfully',
      'Your attendance has been recorded for today.',
      details,
      payload: AppStrings.notifSuccess,
    );
  }
  
  /// Shows near office but not on WiFi notification
  Future<void> showNearOfficeNotOnWifi() async {
    if (!_isInitialized) await initialize();
    
    const androidDetails = AndroidNotificationDetails(
      'attendance_channel',
      'Attendance Notifications',
      importance: Importance.high,
      priority: Priority.high,
      icon: '@mipmap/ic_launcher',
    );
    
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    
    const details = NotificationDetails(android: androidDetails, iOS: iosDetails);
    
    await _notifications.show(
      2,
      'Near Office',
      'You are near the office but not connected to office WiFi. Please connect to mark attendance.',
      details,
      payload: AppStrings.notifNearOfficeNotOnWifi,
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
    
    const iosDetails = DarwinNotificationDetails(
      presentAlert: true,
      presentBadge: true,
      presentSound: true,
    );
    
    const details = NotificationDetails(android: androidDetails, iOS: iosDetails);
    
    await _notifications.show(
      3,
      'Location Permission Required',
      'Location permission is disabled. Attendance marking may fail. Please enable location services.',
      details,
      payload: AppStrings.notifPermissionWarning,
    );
  }
  
  /// Cancels all notifications
  Future<void> cancelAll() async {
    await _notifications.cancelAll();
  }
  
  /// Cancels specific notification by ID
  Future<void> cancel(int id) async {
    await _notifications.cancel(id);
  }
}
