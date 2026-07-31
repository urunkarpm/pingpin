import 'package:flutter/material.dart';
import '../data/database/app_database.dart';
import '../data/repositories/repositories.dart';
import 'wifi_service.dart';
import '../../core/utils/date_utils.dart';

/// Result of attendance check
enum AttendanceCheckResult {
  success, // Marked successfully
  alreadyMarked, // Already marked today
  wifiMismatch, // Not connected to office WiFi
  noOfficeConfig, // Office not configured
  nonWorkingDay, // Today is not a working day
  error, // Other error
}

/// Service for handling automated Wi-Fi attendance marking logic
class AttendanceService {
  final WifiService _wifiService;
  
  AttendanceService({
    WifiService? wifiService,
  })  : _wifiService = wifiService ?? WifiService();
  
  /// Checks and automatically marks attendance if connected to office Wi-Fi network
  Future<AttendanceCheckResult> checkAndMarkAttendance({
    required OfficeConfig officeConfig,
    required AttendanceRepository attendanceRepo,
    required Function() onAttendanceMarked,
  }) async {
    try {
      // Check if already marked today
      final today = getCurrentDateYyyyMmDd();
      final existingRecord = await attendanceRepo.getByDate(today);
      if (existingRecord != null) {
        return AttendanceCheckResult.alreadyMarked;
      }
      
      // Check if today is a working day
      final now = DateTime.now();
      if (!isWorkingDay(now, officeConfig.workingDaysMask)) {
        return AttendanceCheckResult.nonWorkingDay;
      }
      
      // Check WiFi SSID
      final isOnOfficeWifi = await _wifiService.isConnectedToSSID(officeConfig.ssid);
      if (!isOnOfficeWifi) {
        return AttendanceCheckResult.wifiMismatch;
      }
      
      // Attendance is automatically marked as Present whenever Wi-Fi is connected
      const status = AttendanceStatus.present;

      
      // Automatically mark attendance
      await attendanceRepo.insertRecord(
        dateYyyyMmDd: today,
        status: status,
        markedAt: DateTime.now(),
        ssidSnapshot: await _wifiService.getWifiSSID(),
        distanceMeters: null,
      );
      
      // Callback for notification
      onAttendanceMarked();
      
      return AttendanceCheckResult.success;
    } catch (e) {
      print('Error checking automated attendance: $e');
      return AttendanceCheckResult.error;
    }
  }
  
  /// Gets current WiFi status
  Future<Map<String, dynamic>> getWifiStatus() async {
    try {
      final ssid = await _wifiService.getWifiSSID();
      final isConnected = ssid != null;
      
      return {
        'isConnected': isConnected,
        'ssid': ssid,
      };
    } catch (e) {
      print('Error getting WiFi status: $e');
      return {'isConnected': false, 'ssid': null};
    }
  }
}

