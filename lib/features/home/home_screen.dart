import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:table_calendar/table_calendar.dart';
import '../../data/repositories/repositories.dart';
import '../../data/database/app_database.dart';
import '../../core/constants/app_constants.dart';
import '../../providers/providers.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  late DateTime _focusedDay;
  DateTime? _selectedDay;
  CalendarFormat _calendarFormat = CalendarFormat.month;

  Map<String, AttendanceStatus> _attendanceMap = {};
  OfficeConfig? _officeConfig;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _focusedDay = DateTime.now();
    _selectedDay = DateTime.now();
    _loadData();
  }

  Future<void> _loadData() async {
    setState(() => _isLoading = true);

    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
    final attendanceRepo = ref.read(attendanceRepositoryProvider);

    _officeConfig = await officeConfigRepo.getConfig();

    final now = DateTime.now();

    if (_officeConfig != null) {
      final attendanceService = ref.read(attendanceServiceProvider);
      final notificationManager = ref.read(smartNotificationManagerProvider);

      await attendanceService.checkAndMarkAttendance(
        officeConfig: _officeConfig!,
        attendanceRepo: attendanceRepo,
        onAttendanceMarked: () async {
          await notificationManager.showSuccessNotification();
        },
      );
    }

    final records = await attendanceRepo.getForMonth(now.year, now.month);

    setState(() {
      _attendanceMap = {
        for (var record in records) record.dateYyyyMmDd: record.status,
      };
      _isLoading = false;
    });
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
    final totalPresent = _attendanceMap.values
        .where((v) => v == AttendanceStatus.present)
        .length;
    final totalLate =
        _attendanceMap.values.where((v) => v == AttendanceStatus.late).length;

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
          : SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Status card
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: Theme.of(context).colorScheme.surface,
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.3),
                        width: 2,
                      ),
                    ),
                    child: Column(
                      children: [
                        Text(
                          'Today\'s Status',
                          style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.7),
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 16),
                        _buildTodayStatus(),
                        const SizedBox(height: 12),
                        Text(
                          _officeConfig != null
                              ? 'Office Wi-Fi: ${_officeConfig!.ssid}'
                              : 'Configuring Office Wi-Fi...',
                          style: TextStyle(
                            color: Theme.of(context).colorScheme.onSurface.withValues(alpha: 0.8),
                            fontSize: 13,
                          ),
                        ),
                      ],
                    ),
                  ),

                  const SizedBox(height: 20),

                  // Summary stats row
                  Row(
                    children: [
                      Expanded(
                        child: _buildStatChip(
                          icon: Icons.check_circle_rounded,
                          color: AppColors.accent,
                          label: 'Attendance Marked',
                          value: '$totalPresent',
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: _buildStatChip(
                          icon: Icons.calendar_today_rounded,
                          color: AppColors.blue,
                          label: 'Total Days',
                          value: '${totalPresent + totalLate}',
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 24),

                  const Text(
                    'Attendance Calendar',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: AppColors.textPrimary,
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Calendar Card
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      child: TableCalendar<AttendanceRecord>(
                        firstDay: DateTime.utc(2020, 1, 1),
                        lastDay: DateTime.utc(2030, 12, 31),
                        focusedDay: _focusedDay,
                        selectedDayPredicate: (day) =>
                            isSameDay(_selectedDay, day),
                        calendarFormat: _calendarFormat,
                        eventLoader: _getAttendanceForDay,
                        onDaySelected: (selectedDay, focusedDay) {
                          setState(() {
                            _selectedDay = selectedDay;
                            _focusedDay = focusedDay;
                          });
                        },
                        onFormatChanged: (format) {
                          setState(() => _calendarFormat = format);
                        },
                        onPageChanged: (focusedDay) {
                          _focusedDay = focusedDay;
                        },
                        calendarBuilders: CalendarBuilders(
                          defaultBuilder: (context, day, focusedDay) {
                            final dateStr =
                                '${day.year}-${day.month.toString().padLeft(2, '0')}-${day.day.toString().padLeft(2, '0')}';
                            if (_attendanceMap.containsKey(dateStr)) {
                              final isSelected = isSameDay(_selectedDay, day);
                              final isToday = isSameDay(DateTime.now(), day);
                              return Container(
                                margin: const EdgeInsets.all(4.0),
                                alignment: Alignment.center,
                                decoration: BoxDecoration(
                                  color: AppColors.green,
                                  shape: BoxShape.circle,
                                  border: isSelected
                                      ? Border.all(color: AppColors.blue, width: 2)
                                      : null,
                                ),
                                child: Text(
                                  '${day.day}',
                                  style: TextStyle(
                                    color: Colors.white,
                                    fontWeight: (isToday || isSelected)
                                        ? FontWeight.bold
                                        : FontWeight.normal,
                                  ),
                                ),
                              );
                            }
                            return null;
                          },
                          todayBuilder: (context, day, focusedDay) {
                            final dateStr =
                                '${day.year}-${day.month.toString().padLeft(2, '0')}-${day.day.toString().padLeft(2, '0')}';
                            if (_attendanceMap.containsKey(dateStr)) {
                              final isSelected = isSameDay(_selectedDay, day);
                              return Container(
                                margin: const EdgeInsets.all(4.0),
                                alignment: Alignment.center,
                                decoration: BoxDecoration(
                                  color: AppColors.green,
                                  shape: BoxShape.circle,
                                  border: Border.all(
                                      color: isSelected ? AppColors.blue : AppColors.primary,
                                      width: 2),
                                ),
                                child: Text(
                                  '${day.day}',
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              );
                            }
                            return null;
                          },
                          selectedBuilder: (context, day, focusedDay) {
                            final dateStr =
                                '${day.year}-${day.month.toString().padLeft(2, '0')}-${day.day.toString().padLeft(2, '0')}';
                            if (_attendanceMap.containsKey(dateStr)) {
                              return Container(
                                margin: const EdgeInsets.all(4.0),
                                alignment: Alignment.center,
                                decoration: BoxDecoration(
                                  color: AppColors.green,
                                  shape: BoxShape.circle,
                                  border: Border.all(color: AppColors.blue, width: 2.5),
                                ),
                                child: Text(
                                  '${day.day}',
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              );
                            }
                            return null;
                          },
                        ),
                        calendarStyle: CalendarStyle(
                          todayDecoration: const BoxDecoration(
                            color: AppColors.primary,
                            shape: BoxShape.circle,
                          ),
                          todayTextStyle: const TextStyle(
                              color: AppColors.white,
                              fontWeight: FontWeight.bold),
                          selectedDecoration: const BoxDecoration(
                            color: AppColors.blue,
                            shape: BoxShape.circle,
                          ),
                          markerDecoration: const BoxDecoration(
                            color: AppColors.accent,
                            shape: BoxShape.circle,
                          ),
                          weekendTextStyle:
                              const TextStyle(color: AppColors.textSecondary),
                        ),
                        headerStyle: const HeaderStyle(
                          formatButtonVisible: true,
                          titleCentered: true,
                          titleTextStyle: TextStyle(
                              fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
    );
  }

  Widget _buildStatChip({
    required IconData icon,
    required Color color,
    required String label,
    required String value,
  }) {
    final onSurface = Theme.of(context).colorScheme.onSurface;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            Icon(icon, color: onSurface, size: 24),
            const SizedBox(height: 6),
            Text(
              value,
              style: TextStyle(
                  fontSize: 20, fontWeight: FontWeight.bold, color: onSurface),
            ),
            Text(
              label,
              style: TextStyle(fontSize: 12, color: onSurface.withValues(alpha: 0.7)),
            ),
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
              size: 44, color: onSurface.withValues(alpha: 0.5)),
          const SizedBox(height: 6),
          Text(
            'Pending Check-in',
            style: TextStyle(
              color: onSurface,
              fontSize: 16,
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
          size: 48,
          color: color,
        ),
        const SizedBox(height: 6),
        Text(
          label,
          style: TextStyle(
            color: color,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ],
    );
  }
}
