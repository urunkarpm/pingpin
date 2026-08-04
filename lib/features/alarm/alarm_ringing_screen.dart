import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:alarm/alarm.dart';
import '../../services/notification_service.dart';

class AlarmRingingScreen extends StatefulWidget {
  final int alarmId;
  final String portalUrl;

  const AlarmRingingScreen({
    super.key,
    required this.alarmId,
    required this.portalUrl,
  });

  @override
  State<AlarmRingingScreen> createState() => _AlarmRingingScreenState();
}

class _AlarmRingingScreenState extends State<AlarmRingingScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  bool get isCheckIn => widget.alarmId == 101;

  @override
  void initState() {
    super.initState();
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1200),
    )..repeat(reverse: true);

    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.15).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _pulseController.dispose();
    super.dispose();
  }

  Future<void> _stopAlarmAndDismiss(BuildContext context) async {
    HapticFeedback.heavyImpact();
    try {
      await Alarm.stop(widget.alarmId);
      await Alarm.stopAll();
    } catch (e) {
      debugPrint('Error stopping alarm: $e');
    }
    try {
      await NotificationService().cancel(widget.alarmId);
    } catch (e) {
      debugPrint('Error cancelling notification: $e');
    }
    if (context.mounted) {
      Navigator.of(context, rootNavigator: true).pop();
    }
  }

  Future<void> _handleCheckIn(BuildContext context) async {
    await _stopAlarmAndDismiss(context);
    await NotificationService().openPortal(widget.portalUrl);
  }

  Future<void> _handleLeave(BuildContext context) async {
    await _stopAlarmAndDismiss(context);
    await NotificationService().cancelCheckOutAlarm();
    await NotificationService().openLeaveMail();
  }

  Future<void> _handleCheckOut(BuildContext context) async {
    await _stopAlarmAndDismiss(context);
    await NotificationService().openPortal(widget.portalUrl);
  }

  @override
  Widget build(BuildContext context) {
    const paperBg = Color(0xFF0F0F10);
    const inkWhite = Color(0xFFF7F4EB);
    final now = DateTime.now();
    final timeStr =
        "${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}";

    return PopScope(
      canPop: false,
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
                  padding:
                      const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
                  decoration: BoxDecoration(
                    color: inkWhite,
                    borderRadius: BorderRadius.circular(20),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.white.withValues(alpha: 0.15),
                        blurRadius: 16,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                  child: Text(
                    isCheckIn ? 'CHECK-IN ALARM' : 'CHECK-OUT ALARM',
                    style: GoogleFonts.googleSans(
                      fontSize: 13,
                      fontWeight: FontWeight.w800,
                      color: paperBg,
                      letterSpacing: 1.2,
                    ),
                  ),
                ),

                // Main Clock Display with Pulse Animation
                Column(
                  children: [
                    ScaleTransition(
                      scale: _pulseAnimation,
                      child: Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: Colors.white.withValues(alpha: 0.05),
                          border: Border.all(
                            color: Colors.white.withValues(alpha: 0.15),
                            width: 2,
                          ),
                        ),
                        child: const Icon(
                          Icons.alarm_on,
                          size: 72,
                          color: inkWhite,
                        ),
                      ),
                    ),
                    const SizedBox(height: 24),
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

                // Giant Action Buttons
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
                              borderRadius: BorderRadius.circular(16),
                            ),
                            elevation: 4,
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
                              borderRadius: BorderRadius.circular(16),
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
                              borderRadius: BorderRadius.circular(16),
                            ),
                            elevation: 4,
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

