import 'dart:async';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:table_calendar/table_calendar.dart';
import '../../data/repositories/repositories.dart';
import '../../data/database/app_database.dart';
import '../../providers/providers.dart';

import '../../services/attendance_service.dart';
import 'package:intl/intl.dart';
import 'office_occupancy_card.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  late DateTime _focusedDay;
  DateTime? _selectedDay;

  Map<String, AttendanceStatus> _attendanceMap = {};
  OfficeConfig? _officeConfig;
  List<WfoScheduleHistoryEntry> _wfoHistory = [];
  bool _isLoading = true;
  StreamSubscription<List<ConnectivityResult>>? _connectivitySubscription;

  @override
  void initState() {
    super.initState();
    _focusedDay = DateTime.now();
    _selectedDay = DateTime.now();
    _loadData();
    _setupWifiListener();
  }

  @override
  void dispose() {
    _connectivitySubscription?.cancel();
    super.dispose();
  }

  void _setupWifiListener() {
    _connectivitySubscription = Connectivity().onConnectivityChanged.listen((results) {
      if (results.contains(ConnectivityResult.wifi)) {
        _checkWifiAttendance();
      }
    });
  }

  Future<void> _checkWifiAttendance() async {
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
    final attendanceRepo = ref.read(attendanceRepositoryProvider);
    final config = _officeConfig ?? await officeConfigRepo.getConfig();

    if (config != null) {
      final attendanceService = ref.read(attendanceServiceProvider);
      final notificationManager = ref.read(smartNotificationManagerProvider);

      final result = await attendanceService.checkAndMarkAttendance(
        officeConfig: config,
        attendanceRepo: attendanceRepo,
        onAttendanceMarked: () async {
          await notificationManager.showSuccessNotification();
        },
      );

      if (result == AttendanceCheckResult.success) {
        final now = DateTime.now();
        final records = await attendanceRepo.getForMonth(now.year, now.month);
        if (mounted) {
          setState(() {
            _attendanceMap = {
              for (var record in records) record.dateYyyyMmDd: record.status,
            };
          });
        }
      }
    }
  }

  Future<void> _loadData() async {
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
    final attendanceRepo = ref.read(attendanceRepositoryProvider);

    _officeConfig = await officeConfigRepo.getConfig();
    _wfoHistory = await officeConfigRepo.getWfoScheduleHistory();

    if (_wfoHistory.isEmpty && _officeConfig != null) {
      await officeConfigRepo.addWfoScheduleHistory(
        wfoDaysMask: _officeConfig!.wfoDaysMask,
        effectiveFrom: DateTime(2020, 1, 1),
      );
      _wfoHistory = await officeConfigRepo.getWfoScheduleHistory();
    }
    final now = DateTime.now();
    final records = await attendanceRepo.getForMonth(now.year, now.month);

    if (mounted) {
      setState(() {
        _attendanceMap = {
          for (var record in records) record.dateYyyyMmDd: record.status,
        };
        _isLoading = false;
      });
    }

    // Perform Wi-Fi attendance check in background without delaying UI render
    if (_officeConfig != null) {
      final attendanceService = ref.read(attendanceServiceProvider);
      final notificationManager = ref.read(smartNotificationManagerProvider);

      final result = await attendanceService.checkAndMarkAttendance(
        officeConfig: _officeConfig!,
        attendanceRepo: attendanceRepo,
        onAttendanceMarked: () async {
          await notificationManager.showSuccessNotification();
        },
      );

      if (result == AttendanceCheckResult.success && mounted) {
        final updatedRecords = await attendanceRepo.getForMonth(now.year, now.month);
        setState(() {
          _attendanceMap = {
            for (var record in updatedRecords) record.dateYyyyMmDd: record.status,
          };
        });
      }
    }
  }

  int _getWfoMaskForDate(DateTime date) {
    if (_wfoHistory.isEmpty) {
      return _officeConfig?.wfoDaysMask ?? 31;
    }

    final dateOnly = DateTime(date.year, date.month, date.day);
    int mask = _wfoHistory.first.wfoDaysMask;

    for (final entry in _wfoHistory) {
      final effectiveDate = DateTime(
        entry.effectiveFrom.year,
        entry.effectiveFrom.month,
        entry.effectiveFrom.day,
      );

      if (dateOnly.isBefore(effectiveDate)) {
        break;
      }
      mask = entry.wfoDaysMask;
    }

    return mask;
  }

  List<AttendanceRecord> _getAttendanceForDay(DateTime day) {
    final dateStr =
        '${day.year}-${day.month.toString().padLeft(2, '0')}-${day.day.toString().padLeft(2, '0')}';
    if (_attendanceMap.containsKey(dateStr)) {
      return [
        AttendanceRecord(
          id: 0,
          dateYyyyMmDd: dateStr,
          status: _attendanceMap[dateStr]!,
          markedAt: DateTime.now(),
          ssidSnapshot: null,
          distanceMeters: null,
        ),
      ];
    }
    return [];
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    final onSurface = Theme.of(context).colorScheme.onSurface;

    return Scaffold(
      appBar: AppBar(
        title: const Text('PingPin'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _loadData,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Status card
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(
                        color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.15),
                        width: 1.5,
                      ),
                    ),
                    child: Column(
                      children: [
                        Text(
                          'Today\'s Status',
                          style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.6),
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 6),
                        _buildTodayStatus(),
                        const SizedBox(height: 4),
                        Text(
                          _officeConfig != null
                              ? 'Office Wi-Fi: ${_officeConfig!.ssid}'
                              : 'Configuring Office Wi-Fi...',
                          style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.7),
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 12),

                  // Calendar Card
                  Card(
                    elevation: 0,
                    color: Theme.of(context).colorScheme.surface,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(20),
                      side: BorderSide(
                        color: onSurface.withValues(alpha: isDark ? 0.15 : 0.08),
                      ),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                      child: TableCalendar<AttendanceRecord>(
                        firstDay: _officeConfig != null
                            ? DateTime.utc(_officeConfig!.createdAt.year, _officeConfig!.createdAt.month, 1)
                            : DateTime.utc(2020, 1, 1),
                        lastDay: DateTime.utc(2030, 12, 31),
                        focusedDay: _focusedDay,
                        startingDayOfWeek: StartingDayOfWeek.sunday,
                        selectedDayPredicate: (day) =>
                            isSameDay(_selectedDay, day),
                        calendarFormat: CalendarFormat.month,
                        availableCalendarFormats: const {
                          CalendarFormat.month: 'Month',
                        },
                        eventLoader: _getAttendanceForDay,
                        onDaySelected: (selectedDay, focusedDay) {
                          setState(() {
                            _selectedDay = selectedDay;
                            _focusedDay = focusedDay;
                          });
                        },
                        onPageChanged: (focusedDay) {
                          setState(() {
                            _focusedDay = focusedDay;
                          });
                        },
                        calendarBuilders: CalendarBuilders(
                          headerTitleBuilder: (context, day) {
                            final monthText = DateFormat.MMMM().format(day);
                            return Center(
                              child: AnimatedSwitcher(
                                duration: const Duration(milliseconds: 350),
                                switchInCurve: Curves.easeOutCubic,
                                switchOutCurve: Curves.easeInCubic,
                                transitionBuilder: (Widget child, Animation<double> animation) {
                                  final isIncoming = child.key == ValueKey(day.month);
                                  final double beginOffset = isIncoming ? 0.35 : -0.35;
                                  final offsetAnimation = Tween<Offset>(
                                    begin: Offset(beginOffset, 0.0),
                                    end: Offset.zero,
                                  ).animate(animation);

                                  return FadeTransition(
                                    opacity: animation,
                                    child: SlideTransition(
                                      position: offsetAnimation,
                                      child: child,
                                    ),
                                  );
                                },
                                child: Text(
                                  monthText,
                                  key: ValueKey(day.month),
                                  style: TextStyle(
                                    fontSize: 22,
                                    fontWeight: FontWeight.bold,
                                    color: onSurface,
                                  ),
                                ),
                              ),
                            );
                          },
                          prioritizedBuilder: (context, day, focusedDay) {
                            final wfoMask = _getWfoMaskForDate(day);
                            final bitPosition = day.weekday - 1;
                            final isWfoDay = (wfoMask & (1 << bitPosition)) != 0;

                            if (!isWfoDay) return null;

                            final isSelected = isSameDay(_selectedDay, day);
                            final isToday = isSameDay(day, DateTime.now());
                            final isOutside = day.month != focusedDay.month;

                            const blueColor = Color(0xFF3B82F6);

                            return Container(
                              margin: const EdgeInsets.all(4.0),
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: isSelected
                                    ? blueColor
                                    : (isOutside ? blueColor.withValues(alpha: 0.08) : blueColor.withValues(alpha: 0.18)),
                                border: Border.all(
                                  color: isToday
                                      ? (isSelected ? Colors.white : blueColor)
                                      : blueColor.withValues(alpha: isOutside ? 0.3 : 0.7),
                                  width: isToday ? 2.0 : 1.2,
                                ),
                              ),
                              child: Text(
                                '${day.day}',
                                style: TextStyle(
                                  fontSize: 15,
                                  fontWeight: isToday || isSelected ? FontWeight.bold : FontWeight.w600,
                                  color: isSelected
                                      ? Colors.white
                                      : (isOutside
                                          ? onSurface.withValues(alpha: 0.4)
                                          : (isDark ? const Color(0xFF93C5FD) : const Color(0xFF1D4ED8))),
                                ),
                              ),
                            );
                          },
                          markerBuilder: (context, day, events) {
                            if (events.isNotEmpty) {
                              return Positioned(
                                bottom: 4,
                                child: Row(
                                  mainAxisSize: MainAxisSize.min,
                                  children: events.map((record) {
                                    final dotColor = record.status == AttendanceStatus.present
                                        ? (isDark ? const Color(0xFF60A5FA) : const Color(0xFF3B82F6))
                                        : (isDark ? const Color(0xFFFBBF24) : const Color(0xFFF59E0B));
                                    return Container(
                                      margin: const EdgeInsets.symmetric(horizontal: 1.5),
                                      width: 5,
                                      height: 5,
                                      decoration: BoxDecoration(
                                        shape: BoxShape.circle,
                                        color: dotColor,
                                      ),
                                    );
                                  }).toList(),
                                ),
                              );
                            }
                            return null;
                          },
                        ),
                        daysOfWeekStyle: DaysOfWeekStyle(
                          weekdayStyle: TextStyle(
                            color: onSurface.withValues(alpha: isDark ? 0.5 : 0.55),
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                          ),
                          weekendStyle: TextStyle(
                            color: onSurface.withValues(alpha: isDark ? 0.5 : 0.55),
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                          ),
                          dowTextFormatter: (date, locale) =>
                              DateFormat.E(locale).format(date)[0],
                        ),
                        calendarStyle: CalendarStyle(
                          outsideDaysVisible: true,
                          defaultTextStyle: TextStyle(
                            color: onSurface,
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                          weekendTextStyle: TextStyle(
                            color: onSurface,
                            fontSize: 16,
                            fontWeight: FontWeight.w500,
                          ),
                          outsideTextStyle: TextStyle(
                            color: onSurface.withValues(alpha: isDark ? 0.25 : 0.35),
                            fontSize: 16,
                            fontWeight: FontWeight.w400,
                          ),
                          todayTextStyle: TextStyle(
                            color: isSameDay(_selectedDay, DateTime.now())
                                ? (isDark ? const Color(0xFF0F172A) : Colors.white)
                                : onSurface,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                          todayDecoration: BoxDecoration(
                            color: isSameDay(_selectedDay, DateTime.now())
                                ? (isDark ? const Color(0xFFF8FAFC) : const Color(0xFF1E293B))
                                : Colors.transparent,
                            shape: BoxShape.circle,
                            border: isSameDay(_selectedDay, DateTime.now())
                                ? null
                                : Border.all(
                                    color: onSurface.withValues(alpha: isDark ? 0.4 : 0.3),
                                    width: 1.5,
                                  ),
                          ),
                          selectedTextStyle: TextStyle(
                            color: isDark ? const Color(0xFF0F172A) : Colors.white,
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                          selectedDecoration: BoxDecoration(
                            color: isDark ? const Color(0xFFF8FAFC) : const Color(0xFF1E293B),
                            shape: BoxShape.circle,
                          ),
                        ),
                        headerStyle: HeaderStyle(
                          formatButtonVisible: false,
                          titleCentered: true,
                          leftChevronIcon: Icon(
                            Icons.chevron_left,
                            color: onSurface.withValues(alpha: 0.8),
                            size: 22,
                          ),
                          rightChevronIcon: Icon(
                            Icons.chevron_right,
                            color: onSurface.withValues(alpha: 0.8),
                            size: 22,
                          ),
                          leftChevronPadding: const EdgeInsets.all(4),
                          rightChevronPadding: const EdgeInsets.all(4),
                          leftChevronMargin: EdgeInsets.zero,
                          rightChevronMargin: EdgeInsets.zero,
                          titleTextStyle: TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                            color: onSurface,
                          ),
                        ),
                      ),
                    ),
                  ),

                  const SizedBox(height: 12),

                  // Office Occupancy Card
                  const OfficeOccupancyCard(),
                ],
              ),
            ),
    );
  }

  Widget _buildTodayStatus() {
    final today = DateTime.now();
    final dateStr =
        '${today.year}-${today.month.toString().padLeft(2, '0')}-${today.day.toString().padLeft(2, '0')}';
    final status = _attendanceMap[dateStr];
    final onSurface = Theme.of(context).colorScheme.onSurface;

    if (status == null) {
      return Column(
        children: [
          Icon(Icons.pending_actions_rounded,
              size: 28, color: onSurface.withValues(alpha: 0.5)),
          const SizedBox(height: 4),
          Text(
            'Pending Check-in',
            style: TextStyle(
              color: onSurface,
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      );
    }

    final color = onSurface;
    const label = 'Attendance Marked Today';

    return Column(
      children: [
        Icon(
          Icons.check_circle_rounded,
          size: 32,
          color: color,
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: TextStyle(
            color: color,
            fontSize: 15,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}
