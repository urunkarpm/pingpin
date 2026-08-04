import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';

/// Calculates the distance between two coordinates in meters using Haversine formula
double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
  return Geolocator.distanceBetween(lat1, lon1, lat2, lon2);
}

/// Formats time as HH:mm string
String formatTime(int hour, int minute) {
  return '${hour.toString().padLeft(2, '0')}:${minute.toString().padLeft(2, '0')}';
}

/// Parses time string (HH:mm) to TimeOfDay
TimeOfDay parseTimeString(String timeStr) {
  final parts = timeStr.split(':');
  return TimeOfDay(
    hour: int.parse(parts[0]),
    minute: int.parse(parts[1]),
  );
}

/// Adds hours and minutes to a TimeOfDay and returns the resulting TimeOfDay
TimeOfDay addHoursAndMinutes(TimeOfDay time, int hours, int minutes) {
  final totalMinutes = time.hour * 60 + time.minute + hours * 60 + minutes;
  final newHour = (totalMinutes ~/ 60) % 24;
  final newMinute = totalMinutes % 60;
  return TimeOfDay(hour: newHour, minute: newMinute);
}


/// Gets current date in YYYY-MM-DD format
String getCurrentDateYyyyMmDd() {
  final now = DateTime.now();
  return '${now.year}-${now.month.toString().padLeft(2, '0')}-${now.day.toString().padLeft(2, '0')}';
}

/// Formats date for display (e.g., "January 2024")
String formatMonthYear(int year, int month) {
  const months = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];
  return '${months[month - 1]} $year';
}

/// Checks if a day is a working day based on bitmask
bool isWorkingDay(DateTime date, int workingDaysMask) {
  // DateTime.weekday: Monday = 1, Sunday = 7
  final weekday = date.weekday;
  final bitPosition = weekday - 1; // Convert to 0-based index
  final bitValue = 1 << bitPosition;
  return (workingDaysMask & bitValue) != 0;
}

/// Gets all working days in a month
List<DateTime> getWorkingDaysInMonth(int year, int month, int workingDaysMask) {
  final workingDays = <DateTime>[];
  final daysInMonth = DateTime(year, month + 1, 0).day;
  
  for (int day = 1; day <= daysInMonth; day++) {
    final date = DateTime(year, month, day);
    if (isWorkingDay(date, workingDaysMask)) {
      workingDays.add(date);
    }
  }
  
  return workingDays;
}

/// Calculates consecutive working days streak up to a given date
int calculateCurrentStreak({
  required DateTime endDate,
  required List<String> attendedDates, // List of YYYY-MM-DD strings
  required int workingDaysMask,
}) {
  int streak = 0;
  var currentDate = DateTime(endDate.year, endDate.month, endDate.day);
  
  // Go backwards from end date
  while (true) {
    // Skip non-working days
    if (!isWorkingDay(currentDate, workingDaysMask)) {
      currentDate = currentDate.subtract(const Duration(days: 1));
      continue;
    }
    
    // Check if this working day is attended
    final dateStr = '${currentDate.year}-${currentDate.month.toString().padLeft(2, '0')}-${currentDate.day.toString().padLeft(2, '0')}';
    if (attendedDates.contains(dateStr)) {
      streak++;
      currentDate = currentDate.subtract(const Duration(days: 1));
    } else {
      break;
    }
  }
  
  return streak;
}

/// Calculates best consecutive working days streak in a period
int calculateBestStreak({
  required DateTime startDate,
  required DateTime endDate,
  required List<String> attendedDates,
  required int workingDaysMask,
}) {
  int bestStreak = 0;
  int currentStreak = 0;
  
  var currentDate = DateTime(startDate.year, startDate.month, startDate.day);
  final endDateTime = DateTime(endDate.year, endDate.month, endDate.day);
  
  while (currentDate.isBefore(endDateTime) || currentDate.isAtSameMomentAs(endDateTime)) {
    // Skip non-working days
    if (!isWorkingDay(currentDate, workingDaysMask)) {
      // Reset streak when hitting non-working day
      if (currentStreak > bestStreak) {
        bestStreak = currentStreak;
      }
      currentStreak = 0;
      currentDate = currentDate.add(const Duration(days: 1));
      continue;
    }
    
    // Check if this working day is attended
    final dateStr = '${currentDate.year}-${currentDate.month.toString().padLeft(2, '0')}-${currentDate.day.toString().padLeft(2, '0')}';
    if (attendedDates.contains(dateStr)) {
      currentStreak++;
    } else {
      // Break in streak
      if (currentStreak > bestStreak) {
        bestStreak = currentStreak;
      }
      currentStreak = 0;
    }
    
    currentDate = currentDate.add(const Duration(days: 1));
  }
  
  // Final check
  if (currentStreak > bestStreak) {
    bestStreak = currentStreak;
  }
  
  return bestStreak;
}
