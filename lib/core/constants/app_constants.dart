import 'package:flutter/material.dart';

// App-wide constants

class AppColors {
  static const Color primary = Color(0xFF0F172A); // Deep Slate Navy
  static const Color primaryDark = Color(0xFF020617);
  static const Color accent = Color(0xFF10B981); // Emerald Green
  static const Color accentSoft = Color(0xFFD1FAE5);
  static const Color orange = Color(0xFFF59E0B); // Amber Late
  static const Color orangeSoft = Color(0xFFFEF3C7);
  static const Color blue = Color(0xFF3B82F6); // Electric Blue
  static const Color blueSoft = Color(0xFFDBEAFE);
  static const Color background = Color(0xFFF8FAFC); // Crisp Off-White
  static const Color cardBg = Color(0xFFFFFFFF);
  static const Color border = Color(0xFFE2E8F0);
  static const Color textPrimary = Color(0xFF0F172A);
  static const Color textSecondary = Color(0xFF64748B);
  
  // Legacy aliases
  static const Color black = primary;
  static const Color white = Color(0xFFFFFFFF);
  static const Color green = accent;
  static const Color gray = textSecondary;
  static const Color lightGray = Color(0xFFF1F5F9);
}


class AppStrings {
  static const String appName = 'PingPin';
  static const String present = 'Present';
  static const String late = 'Late';
  static const String absent = 'Absent';
  
  // Notification types
  static const String notifNearOfficeNotOnWifi = 'near_office_not_on_wifi';
  static const String notifPermissionWarning = 'permission_warning';
  static const String notifSuccess = 'attendance_success';
}

class AppKeys {
  static const String onboardingComplete = 'onboarding_complete';
  static const String dbFileName = 'pingpin.db';
}
