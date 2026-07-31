import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:drift/drift.dart';
import '../database/app_database.dart';

/// Provider for the database instance
final databaseProvider = Provider<AppDatabase>((ref) {
  return AppDatabase();
});

/// Provider for office config repository
final officeConfigRepositoryProvider = Provider<OfficeConfigRepository>((ref) {
  return OfficeConfigRepository(ref.watch(databaseProvider));
});

/// Provider for attendance repository
final attendanceRepositoryProvider = Provider<AttendanceRepository>((ref) {
  return AttendanceRepository(ref.watch(databaseProvider));
});

/// Provider for user profile repository
final userProfileRepositoryProvider = Provider<UserProfileRepository>((ref) {
  return UserProfileRepository(ref.watch(databaseProvider));
});

/// Provider for notification log repository
final notificationLogRepositoryProvider = Provider<NotificationLogRepository>((ref) {
  return NotificationLogRepository(ref.watch(databaseProvider));
});

/// Repository for office configuration
class OfficeConfigRepository {
  final AppDatabase _db;
  
  OfficeConfigRepository(this._db);
  
  Future<OfficeConfig?> getConfig() async {
    return await _db.getOfficeConfig();
  }
  
  Future<int> saveConfig({
    int? id,
    required String ssid,
    required double latitude,
    required double longitude,
    required int radiusMeters,
    required String workStartTime,
    required String lateCutoffTime,
    required int workingDaysMask,
  }) async {
    final companion = OfficeConfigsCompanion(
      id: id != null ? Value(id) : const Value.absent(),
      ssid: Value(ssid),
      latitude: Value(latitude),
      longitude: Value(longitude),
      radiusMeters: Value(radiusMeters),
      workStartTime: Value(workStartTime),
      lateCutoffTime: Value(lateCutoffTime),
      workingDaysMask: Value(workingDaysMask),
      updatedAt: Value(DateTime.now()),
    );
    
    return await _db.saveOfficeConfig(companion);
  }
  
  Future<void> deleteConfig() async {
    // Not typically used, but available if needed
  }
}

/// Repository for attendance records
class AttendanceRepository {
  final AppDatabase _db;
  
  AttendanceRepository(this._db);
  
  Future<AttendanceRecord?> getByDate(String dateYyyyMmDd) async {
    return await _db.getAttendanceByDate(dateYyyyMmDd);
  }
  
  Future<List<AttendanceRecord>> getForMonth(int year, int month) async {
    return await _db.getAttendanceForMonth(year, month);
  }
  
  Future<void> insertRecord({
    required String dateYyyyMmDd,
    required AttendanceStatus status,
    required DateTime markedAt,
    String? ssidSnapshot,
    double? distanceMeters,
  }) async {
    final companion = AttendanceRecordsCompanion(
      dateYyyyMmDd: Value(dateYyyyMmDd),
      status: Value(status),
      markedAt: Value(markedAt),
      ssidSnapshot: Value(ssidSnapshot),
      distanceMeters: Value(distanceMeters),
    );
    
    await _db.insertAttendanceRecord(companion);
  }
  
  Future<bool> hasRecordForToday() async {
    final today = _getCurrentDateYyyyMmDd();
    final record = await getByDate(today);
    return record != null;
  }
  
  String _getCurrentDateYyyyMmDd() {
    final now = DateTime.now();
    return '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
  }
}

/// Repository for user profile
class UserProfileRepository {
  final AppDatabase _db;
  
  UserProfileRepository(this._db);
  
  Future<UserProfile?> getProfile() async {
    return await _db.getUserProfile();
  }
  
  Future<int> saveProfile({
    int? id,
    required String fullName,
    required String designation,
    String? photoPath,
    String? employeeId,
    String? email,
    String? phone,
  }) async {
    final companion = UserProfilesCompanion(
      id: id != null ? Value(id) : const Value.absent(),
      fullName: Value(fullName),
      designation: Value(designation),
      photoPath: Value(photoPath),
      employeeId: Value(employeeId),
      email: Value(email),
      phone: Value(phone),
      updatedAt: Value(DateTime.now()),
    );
    
    return await _db.saveUserProfile(companion);
  }
}

/// Repository for notification logs
class NotificationLogRepository {
  final AppDatabase _db;
  
  NotificationLogRepository(this._db);
  
  Future<DateTime?> getLastNotification(String type) async {
    return await _db.getLastNotificationByType(type);
  }
  
  Future<void> logNotification({
    required String type,
    required DateTime triggeredAt,
    String? dateYyyyMmDd,
  }) async {
    await _db.logNotification(
      type: type,
      triggeredAt: triggeredAt,
      dateYyyyMmDd: dateYyyyMmDd,
    );
  }
  
  /// Checks if notification should be throttled
  Future<bool> shouldThrottle({
    required String type,
    required Duration throttleDuration,
  }) async {
    final lastTriggered = await getLastNotification(type);
    if (lastTriggered == null) return false;
    
    final now = DateTime.now();
    return now.difference(lastTriggered) < throttleDuration;
  }
}
