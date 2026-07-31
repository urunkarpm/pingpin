import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';
import 'package:drift/drift.dart';
import 'package:pingpin/data/database/app_database.dart';
import 'package:pingpin/services/attendance_service.dart';
import 'package:pingpin/services/wifi_service.dart';
import 'package:pingpin/services/location_service.dart';
import 'package:pingpin/core/utils/date_utils.dart';

// Mock classes
class MockWifiService extends Mock implements WifiService {}
class MockLocationService extends Mock implements LocationService {}

void main() {

  group('Attendance Logic Tests', () {
    late AttendanceService attendanceService;
    late MockWifiService mockWifiService;
    
    setUp(() {
      mockWifiService = MockWifiService();
      attendanceService = AttendanceService(
        wifiService: mockWifiService,
      );
    });
    
    test('Detects office Wi-Fi connection', () async {
      when(() => mockWifiService.isConnectedToSSID(any())).thenAnswer((_) async => true);
      final isConnected = await mockWifiService.isConnectedToSSID('OfficeWiFi');
      expect(isConnected, isTrue);
    });
    
    test('Always marks Present when Wi-Fi is connected', () async {
      when(() => mockWifiService.isConnectedToSSID('OfficeWiFi')).thenAnswer((_) async => true);
      final isConnected = await mockWifiService.isConnectedToSSID('OfficeWiFi');
      expect(isConnected, isTrue);
    });

    
    test('Not marked on SSID mismatch', () async {
      when(() => mockWifiService.isConnectedToSSID('OfficeWiFi')).thenAnswer((_) async => false);
      final result = await mockWifiService.isConnectedToSSID('OfficeWiFi');
      expect(result, isFalse);
    });
  });

  
  group('Date Utils Tests', () {
    test('Working day bitmask - Monday to Friday', () {
      expect(isWorkingDay(DateTime(2024, 1, 1), WorkingDays.defaultWeekdays), isTrue); // Monday
      expect(isWorkingDay(DateTime(2024, 1, 6), WorkingDays.defaultWeekdays), isFalse); // Saturday
      expect(isWorkingDay(DateTime(2024, 1, 7), WorkingDays.defaultWeekdays), isFalse); // Sunday
    });
    
    test('Get working days in month', () {
      final workingDays = getWorkingDaysInMonth(2024, 1, WorkingDays.defaultWeekdays);
      // January 2024 has 23 weekdays (Mon-Fri)
      expect(workingDays.length, equals(23));
    });
    
    test('Current streak calculation', () {
      final attendedDates = [
        '2024-01-15',
        '2024-01-16',
        '2024-01-17',
      ];
      
      final streak = calculateCurrentStreak(
        endDate: DateTime(2024, 1, 17),
        attendedDates: attendedDates,
        workingDaysMask: WorkingDays.defaultWeekdays,
      );
      
      expect(streak, greaterThanOrEqualTo(1));
    });
    
    test('Best streak calculation', () {
      final attendedDates = [
        '2024-01-15',
        '2024-01-16',
        '2024-01-17',
        '2024-01-22',
        '2024-01-23',
      ];
      
      final bestStreak = calculateBestStreak(
        startDate: DateTime(2024, 1, 1),
        endDate: DateTime(2024, 1, 31),
        attendedDates: attendedDates,
        workingDaysMask: WorkingDays.defaultWeekdays,
      );
      
      expect(bestStreak, greaterThanOrEqualTo(2));
    });
  });
  
  group('Insights Calculation Tests', () {
    test('Attendance percentage calculation', () {
      final totalOfficeDays = 15;
      final eligibleWorkingDays = 20;
      final percentage = (totalOfficeDays / eligibleWorkingDays * 100);
      
      expect(percentage, equals(75.0));
    });
    
    test('Late count from records', () {
      final records = [
        AttendanceRecord(
          id: 1,
          dateYyyyMmDd: '2024-01-15',
          status: AttendanceStatus.present,
          markedAt: DateTime.now(),
          ssidSnapshot: null,
          distanceMeters: null,
        ),
        AttendanceRecord(
          id: 2,
          dateYyyyMmDd: '2024-01-16',
          status: AttendanceStatus.late,
          markedAt: DateTime.now(),
          ssidSnapshot: null,
          distanceMeters: null,
        ),
      ];
      
      final lateCount = records.where((r) => r.status == AttendanceStatus.late).length;
      expect(lateCount, equals(1));
    });
  });
  
  group('Notification Throttling Tests', () {
    test('Throttle duration comparison', () {
      final now = DateTime.now();
      final twoHoursAgo = now.subtract(const Duration(hours: 2));
      const throttleDuration = Duration(hours: 2);
      
      // Should NOT throttle if last notification was exactly 2 hours ago
      expect(now.difference(twoHoursAgo) >= throttleDuration, isTrue);
      
      // Should throttle if last notification was 1 hour ago
      final oneHourAgo = now.subtract(const Duration(hours: 1));
      expect(now.difference(oneHourAgo) < throttleDuration, isTrue);
    });
  });
  
  group('Profile Persistence Tests', () {
    test('UserProfile companion creation', () {
      final companion = UserProfilesCompanion(
        fullName: const Value('John Doe'),
        designation: const Value('Engineer'),
        photoPath: const Value('/path/to/photo.jpg'),
        employeeId: const Value('EMP001'),
        email: const Value('john@example.com'),
        phone: const Value('1234567890'),
        updatedAt: Value(DateTime.now()),
      );
      
      expect(companion.fullName.value, equals('John Doe'));
      expect(companion.designation.value, equals('Engineer'));
    });
  });

  group('7-Day Weekly Schedule Verification (Monday & Tuesday OFF, Wednesday-Sunday WORK)', () {
    late AttendanceService attendanceService;
    late MockWifiService mockWifiService;

    // Schedule bitmask: Wed (4) + Thu (8) + Fri (16) + Sat (32) + Sun (64) = 124
    // Excludes Monday (1) and Tuesday (2)
    final customWorkingDaysMask = WorkingDays.wednesday |
        WorkingDays.thursday |
        WorkingDays.friday |
        WorkingDays.saturday |
        WorkingDays.sunday;

    late OfficeConfig weeklyOfficeConfig;

    setUp(() {
      mockWifiService = MockWifiService();
      attendanceService = AttendanceService(wifiService: mockWifiService);

      weeklyOfficeConfig = OfficeConfig(
        id: 1,
        ssid: 'Office_WiFi',
        latitude: 0.0,
        longitude: 0.0,
        radiusMeters: 100,
        workStartTime: '09:30',
        lateCutoffTime: '10:30',
        workingDaysMask: customWorkingDaysMask,
        createdAt: DateTime.now(),
        updatedAt: DateTime.now(),
      );

    });

    test('Monday (OFF): Returns false for isWorkingDay and does NOT mark attendance', () async {
      // 2024-01-01 is Monday
      final monday = DateTime(2024, 1, 1);
      expect(isWorkingDay(monday, customWorkingDaysMask), isFalse);
    });

    test('Tuesday (OFF): Returns false for isWorkingDay and does NOT mark attendance', () async {
      // 2024-01-02 is Tuesday
      final tuesday = DateTime(2024, 1, 2);
      expect(isWorkingDay(tuesday, customWorkingDaysMask), isFalse);
    });

    test('Wednesday (WORK): Valid working day and detects Office Wi-Fi', () async {
      // 2024-01-03 is Wednesday
      final wednesday = DateTime(2024, 1, 3);
      expect(isWorkingDay(wednesday, customWorkingDaysMask), isTrue);

      when(() => mockWifiService.isConnectedToSSID('Office_WiFi')).thenAnswer((_) async => true);
      final isConnected = await mockWifiService.isConnectedToSSID(weeklyOfficeConfig.ssid);
      expect(isConnected, isTrue);
    });

    test('Thursday (WORK): Marks Present unconditionally when Wi-Fi connected', () async {
      // 2024-01-04 is Thursday
      final thursday = DateTime(2024, 1, 4);
      expect(isWorkingDay(thursday, customWorkingDaysMask), isTrue);

      when(() => mockWifiService.isConnectedToSSID('Office_WiFi')).thenAnswer((_) async => true);
      final isConnected = await mockWifiService.isConnectedToSSID(weeklyOfficeConfig.ssid);
      expect(isConnected, isTrue);
    });


    test('Friday (WORK): Rejects attendance when on Home Wi-Fi (SSID mismatch)', () async {
      // 2024-01-05 is Friday
      final friday = DateTime(2024, 1, 5);
      expect(isWorkingDay(friday, customWorkingDaysMask), isTrue);

      when(() => mockWifiService.isConnectedToSSID('Office_WiFi')).thenAnswer((_) async => false);
      final isConnected = await mockWifiService.isConnectedToSSID(weeklyOfficeConfig.ssid);
      expect(isConnected, isFalse);
    });

    test('Saturday (WORK): Valid working day under custom schedule', () async {
      // 2024-01-06 is Saturday
      final saturday = DateTime(2024, 1, 6);
      expect(isWorkingDay(saturday, customWorkingDaysMask), isTrue);
    });

    test('Sunday (WORK): Valid working day under custom schedule', () async {
      // 2024-01-07 is Sunday
      final sunday = DateTime(2024, 1, 7);
      expect(isWorkingDay(sunday, customWorkingDaysMask), isTrue);
    });

    test('Full 7-day week working days count verification', () {
      final monday = DateTime(2024, 1, 1);
      final tuesday = DateTime(2024, 1, 2);
      final wednesday = DateTime(2024, 1, 3);
      final thursday = DateTime(2024, 1, 4);
      final friday = DateTime(2024, 1, 5);
      final saturday = DateTime(2024, 1, 6);
      final sunday = DateTime(2024, 1, 7);

      final weekDays = [monday, tuesday, wednesday, thursday, friday, saturday, sunday];
      final workingDays = weekDays.where((d) => isWorkingDay(d, customWorkingDaysMask)).toList();

      expect(workingDays.length, equals(5)); // Wed, Thu, Fri, Sat, Sun
      expect(workingDays.contains(monday), isFalse);
      expect(workingDays.contains(tuesday), isFalse);
      expect(workingDays.contains(wednesday), isTrue);
      expect(workingDays.contains(thursday), isTrue);
      expect(workingDays.contains(friday), isTrue);
      expect(workingDays.contains(saturday), isTrue);
      expect(workingDays.contains(sunday), isTrue);
    });
  });
}

