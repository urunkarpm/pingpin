import 'dart:io';
import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';

part 'app_database.g.dart';

// Enum for attendance status
enum AttendanceStatus { present, late }

// Enum for working days (bitmask values)
class WorkingDays {
  static const int monday = 1;
  static const int tuesday = 2;
  static const int wednesday = 4;
  static const int thursday = 8;
  static const int friday = 16;
  static const int saturday = 32;
  static const int sunday = 64;
  
  static const int defaultWeekdays = monday | tuesday | wednesday | thursday | friday;
}

@DataClassName('OfficeConfig')
class OfficeConfigs extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get ssid => text().withLength(min: 1, max: 255)();
  RealColumn get latitude => real()();
  RealColumn get longitude => real()();
  IntColumn get radiusMeters => integer().withDefault(const Constant(100))();
  TextColumn get lateCutoffTime => text().withDefault(const Constant('10:30'))();
  TextColumn get checkInTime => text().withDefault(const Constant('09:30'))();
  TextColumn get checkOutTime => text().withDefault(const Constant('17:30'))();
  TextColumn get portalUrl => text().withDefault(const Constant(''))();
  IntColumn get workingDaysMask => integer().withDefault(const Constant(31))(); // Mon-Fri default
  DateTimeColumn get createdAt => dateTime().withDefault(currentDateAndTime)();
  DateTimeColumn get updatedAt => dateTime().withDefault(currentDateAndTime)();
}

@DataClassName('AttendanceRecord')
class AttendanceRecords extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get dateYyyyMmDd => text().unique()(); // Format: YYYY-MM-DD
  TextColumn get status => text().map(AttendanceStatusConverter())();
  DateTimeColumn get markedAt => dateTime()();
  TextColumn get ssidSnapshot => text().nullable()();
  RealColumn get distanceMeters => real().nullable()();
}

class AttendanceStatusConverter extends TypeConverter<AttendanceStatus, String> {
  @override
  String toSql(AttendanceStatus value) {
    switch (value) {
      case AttendanceStatus.present:
        return 'present';
      case AttendanceStatus.late:
        return 'late';
    }
  }

  @override
  AttendanceStatus fromSql(String fromDb) {
    switch (fromDb) {
      case 'present':
        return AttendanceStatus.present;
      case 'late':
        return AttendanceStatus.late;
      default:
        throw ArgumentError('Unknown attendance status: $fromDb');
    }
  }
}

@DataClassName('UserProfile')
class UserProfiles extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get fullName => text()();
  TextColumn get designation => text()();
  TextColumn get photoPath => text().nullable()();
  TextColumn get employeeId => text().nullable()();
  TextColumn get email => text().nullable()();
  TextColumn get phone => text().nullable()();
  DateTimeColumn get updatedAt => dateTime().withDefault(currentDateAndTime)();
}

@DataClassName('NotificationLog')
class NotificationLogs extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get type => text()();
  DateTimeColumn get triggeredAt => dateTime()();
  TextColumn get dateYyyyMmDd => text().nullable()(); // Format: YYYY-MM-DD
}

@DriftDatabase(tables: [
  OfficeConfigs,
  AttendanceRecords,
  UserProfiles,
  NotificationLogs,
])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 1;

  // Office Config queries
  Future<OfficeConfig?> getOfficeConfig() async {
    final results = await select(officeConfigs).get();
    return results.isNotEmpty ? results.first : null;
  }

  Future<int> saveOfficeConfig(OfficeConfigsCompanion config) async {
    if (config.id.present) {
      return (update(officeConfigs)..where((t) => t.id.equals(config.id.value)))
          .write(config);
    } else {
      return into(officeConfigs).insert(config);
    }
  }


  // Attendance queries
  Future<AttendanceRecord?> getAttendanceByDate(String dateYyyyMmDd) async {
    final results = await (select(attendanceRecords)
          ..where((t) => t.dateYyyyMmDd.equals(dateYyyyMmDd)))
        .get();
    return results.isNotEmpty ? results.first : null;
  }

  Future<List<AttendanceRecord>> getAttendanceForMonth(int year, int month) async {
    final startDate = '$year-${month.toString().padLeft(2, '0')}';
    
    return (select(attendanceRecords)
          ..where((t) => t.dateYyyyMmDd.like('$startDate%')))
        .get();
  }

  Future<void> insertAttendanceRecord(AttendanceRecordsCompanion record) async {
    await into(attendanceRecords).insert(record);
  }

  // User profile queries
  Future<UserProfile?> getUserProfile() async {
    final results = await select(userProfiles).get();
    return results.isNotEmpty ? results.first : null;
  }

  Future<int> saveUserProfile(UserProfilesCompanion profile) async {
    if (profile.id.present) {
      return (update(userProfiles)..where((t) => t.id.equals(profile.id.value)))
          .write(profile);
    } else {
      return into(userProfiles).insert(profile);
    }
  }


  // Notification log queries
  Future<List<NotificationLog>> getNotificationsByType(String type) async {
    return (select(notificationLogs)..where((t) => t.type.equals(type))).get();
  }

  Future<DateTime?> getLastNotificationByType(String type) async {
    final results = await (select(notificationLogs)
          ..where((t) => t.type.equals(type))
          ..orderBy([(t) => OrderingTerm.desc(t.triggeredAt)])
          ..limit(1))
        .get();
    return results.isNotEmpty ? results.first.triggeredAt : null;
  }


  Future<void> logNotification({
    required String type,
    required DateTime triggeredAt,
    String? dateYyyyMmDd,
  }) async {
    await into(notificationLogs).insert(NotificationLogsCompanion(
      type: Value(type),
      triggeredAt: Value(triggeredAt),
      dateYyyyMmDd: Value(dateYyyyMmDd),
    ));
  }

  // Clear all data (for reset)
  Future<void> clearAllData() async {
    await delete(attendanceRecords).go();
    await delete(notificationLogs).go();
    // Don't clear office config and user profile unless explicitly requested
  }

  Future<void> fullReset() async {
    await delete(attendanceRecords).go();
    await delete(notificationLogs).go();
    await delete(userProfiles).go();
    await delete(officeConfigs).go();
  }
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'pingpin.db'));
    return NativeDatabase.createInBackground(file);
  });
}

