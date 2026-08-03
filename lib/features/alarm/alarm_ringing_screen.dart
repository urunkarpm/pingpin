import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:alarm/alarm.dart';
import '../../services/notification_service.dart';

class AlarmRingingScreen extends StatelessWidget {
  final int alarmId;
  final String portalUrl;

  const AlarmRingingScreen({
    super.key,
    required this.alarmId,
    required this.portalUrl,
  });

  bool get isCheckIn => alarmId == 101;

  Future<void> _stopAlarmAndDismiss(BuildContext context) async {
    // Stop alarm audio + vibration first
    try {
      await Alarm.stop(alarmId);
      await Alarm.stopAll();
    } catch (e) {
      debugPrint('Error stopping alarm: $e');
    }
    // Cancel the paired notification immediately so the status bar clears
    try {
      await NotificationService().cancel(alarmId);
    } catch (e) {
      debugPrint('Error cancelling notification: $e');
    }
    // Dismiss the screen right away — before launching external apps
    if (context.mounted) {
      Navigator.of(context, rootNavigator: true).pop();
    }
  }

  Future<void> _handleCheckIn(BuildContext context) async {
    await _stopAlarmAndDismiss(context);
    await NotificationService().openPortal(portalUrl);
  }

  Future<void> _handleLeave(BuildContext context) async {
    // Dismiss alarm screen and stop all alarm activity
    await _stopAlarmAndDismiss(context);
    // Cancel checkout alarm — user is on leave, no need to check out
    await NotificationService().cancelCheckOutAlarm();
    await NotificationService().openLeaveMail();
  }

  Future<void> _handleCheckOut(BuildContext context) async {
    await _stopAlarmAndDismiss(context);
    await NotificationService().openPortal(portalUrl);
  }

  @override
  Widget build(BuildContext context) {
    const paperBg = Color(0xFF121212); // Full-screen dark E-Ink
    const inkWhite = Color(0xFFF7F4EB);
    final now = DateTime.now();
    final timeStr = "${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}";

    return PopScope(
      canPop: false, // Prevent back button dismiss
      child: Scaffold(
        backgroundColor: paperBg,
        body: SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 36),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                // Top Badge
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: inkWhite,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    isCheckIn ? '⏰ CHECK-IN ALARM' : '🔔 CHECK-OUT ALARM',
                    style: GoogleFonts.googleSans(
                      fontSize: 13,
                      fontWeight: FontWeight.w800,
                      color: paperBg,
                      letterSpacing: 1.2,
                    ),
                  ),
                ),

                // Main Clock Display
                Column(
                  children: [
                    const Icon(Icons.alarm_on, size: 80, color: inkWhite),
                    const SizedBox(height: 16),
                    Text(
                      timeStr,
                      style: GoogleFonts.googleSans(
                        fontSize: 64,
                        fontWeight: FontWeight.w900,
                        color: inkWhite,
                        letterSpacing: -1,
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      isCheckIn
                          ? 'It is time to check in! Choose Check-in or Leave below.'
                          : 'It is time to check out! Tap Check-out to open portal.',
                      textAlign: TextAlign.center,
                      style: GoogleFonts.googleSans(
                        fontSize: 15,
                        color: inkWhite.withValues(alpha: 0.8),
                        height: 1.4,
                      ),
                    ),
                  ],
                ),

                // Giant Action Buttons (No Stop/Snooze!)
                Column(
                  children: [
                    if (isCheckIn) ...[
                      SizedBox(
                        width: double.infinity,
                        height: 60,
                        child: ElevatedButton.icon(
                          onPressed: () => _handleCheckIn(context),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: inkWhite,
                            foregroundColor: paperBg,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(14),
                            ),
                            elevation: 0,
                          ),
                          icon: const Icon(Icons.open_in_browser, size: 24),
                          label: Text(
                            'CHECK-IN (OPEN PORTAL)',
                            style: GoogleFonts.googleSans(
                              fontSize: 16,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      SizedBox(
                        width: double.infinity,
                        height: 60,
                        child: OutlinedButton.icon(
                          onPressed: () => _handleLeave(context),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: inkWhite,
                            side: const BorderSide(color: inkWhite, width: 2),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(14),
                            ),
                          ),
                          icon: const Icon(Icons.mail_outline, size: 24),
                          label: Text(
                            'APPLY FOR LEAVE',
                            style: GoogleFonts.googleSans(
                              fontSize: 16,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                      ),
                    ] else ...[
                      SizedBox(
                        width: double.infinity,
                        height: 64,
                        child: ElevatedButton.icon(
                          onPressed: () => _handleCheckOut(context),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: inkWhite,
                            foregroundColor: paperBg,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(14),
                            ),
                            elevation: 0,
                          ),
                          icon: const Icon(Icons.exit_to_app, size: 26),
                          label: Text(
                            'CHECK-OUT (OPEN PORTAL)',
                            style: GoogleFonts.googleSans(
                              fontSize: 17,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
