import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pdf/pdf.dart';
import 'package:printing/printing.dart';
import '../../data/repositories/repositories.dart';
import '../../data/database/app_database.dart';
import '../../core/utils/date_utils.dart';
import '../../services/pdf_export_service.dart';

class InsightsScreen extends ConsumerStatefulWidget {
  const InsightsScreen({super.key});

  @override
  ConsumerState<InsightsScreen> createState() => _InsightsScreenState();
}

class _InsightsScreenState extends ConsumerState<InsightsScreen> {
  late DateTime _selectedMonth;
  bool _isLoading = true;
  StreamSubscription<List<AttendanceRecord>>? _recordsSubscription;
  
  int _totalOfficeDays = 0;
  double _attendancePercentage = 0;
  int _currentStreak = 0;
  int _bestStreak = 0;
  List<AttendanceRecord> _records = [];
  UserProfile? _profile;
  OfficeConfig? _config;
  
  @override
  void initState() {
    super.initState();
    _selectedMonth = DateTime.now();
    _loadData();
  }

  @override
  void dispose() {
    _recordsSubscription?.cancel();
    super.dispose();
  }
  
  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    
    final profileRepo = ref.read(userProfileRepositoryProvider);
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
    
    _profile = await profileRepo.getProfile();
    _config = await officeConfigRepo.getConfig();

    _subscribeToRecords();
  }

  void _subscribeToRecords() {
    _recordsSubscription?.cancel();
    final attendanceRepo = ref.read(attendanceRepositoryProvider);

    _recordsSubscription = attendanceRepo
        .watchForMonth(_selectedMonth.year, _selectedMonth.month)
        .listen((records) {
      if (mounted) {
        setState(() {
          _records = records;
          _calculateMetrics();
          _isLoading = false;
        });
      }
    });
  }
  
  void _calculateMetrics() {
    if (_config == null) return;
    
    final presentRecords = _records.where((r) => r.status == AttendanceStatus.present).toList();
    final lateRecords = _records.where((r) => r.status == AttendanceStatus.late).toList();
    
    _totalOfficeDays = presentRecords.length + lateRecords.length;
    
    // Calculate attendance percentage
    final workingDays = getWorkingDaysInMonth(_selectedMonth.year, _selectedMonth.month, _config!.workingDaysMask);
    final eligibleWorkingDays = workingDays.length;
    _attendancePercentage = eligibleWorkingDays > 0 
        ? (_totalOfficeDays / eligibleWorkingDays * 100)
        : 0;
    
    // Calculate streaks
    final attendedDates = _records.map((r) => r.dateYyyyMmDd).toList();
    
    // Current streak (up to today if current month, otherwise end of month)
    final endDate = _selectedMonth.year == DateTime.now().year && 
                    _selectedMonth.month == DateTime.now().month
        ? DateTime.now()
        : DateTime(_selectedMonth.year, _selectedMonth.month + 1, 0);
    
    _currentStreak = calculateCurrentStreak(
      endDate: endDate,
      attendedDates: attendedDates,
      workingDaysMask: _config!.workingDaysMask,
    );
    
    // Best streak for the month
    final startDate = DateTime(_selectedMonth.year, _selectedMonth.month, 1);
    _bestStreak = calculateBestStreak(
      startDate: startDate,
      endDate: endDate,
      attendedDates: attendedDates,
      workingDaysMask: _config!.workingDaysMask,
    );
  }
  
  void _previousMonth() {
    if (_config != null) {
      final configMonth = DateTime(_config!.createdAt.year, _config!.createdAt.month, 1);
      final prev = DateTime(_selectedMonth.year, _selectedMonth.month - 1, 1);
      if (prev.isBefore(configMonth)) return;
    }
    setState(() {
      _selectedMonth = DateTime(_selectedMonth.year, _selectedMonth.month - 1, 1);
    });
    _loadData();
  }

  void _nextMonth() {
    final now = DateTime.now();
    final next = DateTime(_selectedMonth.year, _selectedMonth.month + 1, 1);
    if (next.isAfter(DateTime(now.year, now.month, 1))) return;
    setState(() {
      _selectedMonth = next;
    });
    _loadData();
  }

  Future<void> _selectMonth() async {
    final now = DateTime.now();
    final minDate = _config != null 
        ? DateTime(_config!.createdAt.year, _config!.createdAt.month, 1) 
        : DateTime(2020, 1, 1);
        
    final selected = await showModalBottomSheet<DateTime>(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        int tempYear = _selectedMonth.year;
        return StatefulBuilder(
          builder: (context, setModalState) {
            return Container(
              padding: const EdgeInsets.all(20),
              height: 390,
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.chevron_left),
                        onPressed: tempYear > minDate.year
                            ? () => setModalState(() => tempYear--)
                            : null,
                      ),
                      Text(
                        '$tempYear',
                        style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                      ),
                      IconButton(
                        icon: const Icon(Icons.chevron_right),
                        onPressed: tempYear < now.year
                            ? () => setModalState(() => tempYear++)
                            : null,
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Expanded(
                    child: GridView.builder(
                      itemCount: 12,
                      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                        crossAxisCount: 3,
                        childAspectRatio: 2.2,
                        crossAxisSpacing: 10,
                        mainAxisSpacing: 10,
                      ),
                      itemBuilder: (context, index) {
                        final monthNum = index + 1;
                        final monthDate = DateTime(tempYear, monthNum, 1);
                        final isFuture = monthDate.isAfter(DateTime(now.year, now.month, 1));
                        final isBeforeConfig = monthDate.isBefore(minDate);
                        final isDisabled = isFuture || isBeforeConfig;
                        final isSelected = _selectedMonth.year == tempYear && _selectedMonth.month == monthNum;

                        return InkWell(
                          onTap: isDisabled
                              ? null
                              : () => Navigator.pop(context, monthDate),
                          borderRadius: BorderRadius.circular(10),
                          child: Container(
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? Theme.of(context).primaryColor
                                  : isDisabled
                                      ? Colors.transparent
                                      : Theme.of(context).colorScheme.surface,
                              borderRadius: BorderRadius.circular(10),
                              border: Border.all(
                                color: isSelected
                                    ? Theme.of(context).primaryColor
                                    : Theme.of(context).dividerColor,
                              ),
                            ),
                            child: Text(
                              DateFormat('MMM').format(monthDate),
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                color: isSelected
                                    ? Theme.of(context).colorScheme.onPrimary
                                    : isDisabled
                                        ? Colors.grey.withValues(alpha: 0.5)
                                        : null,
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );

    if (selected != null) {
      setState(() {
        _selectedMonth = selected;
      });
      _loadData();
    }
  }

  Future<void> _exportPdf() async {
    final profileRepo = ref.read(userProfileRepositoryProvider);
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);

    _profile = await profileRepo.getProfile();
    _config = await officeConfigRepo.getConfig();
    final wfoHistory = await officeConfigRepo.getWfoScheduleHistory();

    if (_profile == null || _config == null) return;
    
    final pdfService = PdfExportService();
    final pdf = await pdfService.generateAttendancePdf(
      year: _selectedMonth.year,
      month: _selectedMonth.month,
      profile: _profile!,
      records: _records,
      workingDaysMask: _config!.workingDaysMask,
      wfoDaysMask: _config!.wfoDaysMask,
      wfoHistory: wfoHistory,
    );
    
    await Printing.layoutPdf(
      onLayout: (PdfPageFormat format) async => pdf.save(),
      name: 'Attendance_${_selectedMonth.year}_${_selectedMonth.month}.pdf',
    );
  }
  
  @override
  Widget build(BuildContext context) {
    final isCurrentMonth = _selectedMonth.year == DateTime.now().year &&
        _selectedMonth.month == DateTime.now().month;
    final colorScheme = Theme.of(context).colorScheme;

    final isConfigMonth = _config != null &&
        (_selectedMonth.year < _config!.createdAt.year ||
            (_selectedMonth.year == _config!.createdAt.year &&
                _selectedMonth.month <= _config!.createdAt.month));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Monthly Insights'),
        actions: [
          IconButton(
            icon: const Icon(Icons.picture_as_pdf_outlined),
            tooltip: 'Export PDF',
            onPressed: _isLoading ? null : _exportPdf,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                // Enhanced Month Selector Header
                Container(
                  margin: const EdgeInsets.fromLTRB(16, 12, 16, 8),
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: colorScheme.surface,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.5)),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withValues(alpha: 0.03),
                        blurRadius: 10,
                        offset: const Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.chevron_left_rounded),
                        onPressed: isConfigMonth ? null : _previousMonth,
                        tooltip: 'Previous Month',
                      ),
                      InkWell(
                        onTap: _selectMonth,
                        borderRadius: BorderRadius.circular(12),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          child: Row(
                            children: [
                              Icon(Icons.calendar_month_rounded, size: 18, color: colorScheme.primary),
                              const SizedBox(width: 8),
                              Text(
                                DateFormat('MMMM yyyy').format(_selectedMonth),
                                style: const TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                  letterSpacing: 0.2,
                                ),
                              ),
                              const SizedBox(width: 4),
                              Icon(Icons.keyboard_arrow_down_rounded, size: 20, color: colorScheme.onSurfaceVariant),
                            ],
                          ),
                        ),
                      ),
                      IconButton(
                        icon: const Icon(Icons.chevron_right_rounded),
                        onPressed: isCurrentMonth ? null : _nextMonth,
                        tooltip: 'Next Month',
                      ),
                    ],
                  ),
                ),
                
                Expanded(
                  child: ListView(
                    physics: const BouncingScrollPhysics(),
                    padding: const EdgeInsets.fromLTRB(16, 12, 16, 100),
                    children: [
                      // Metric Cards Grid / Rows
                      Row(
                        children: [
                          Expanded(
                            child: _buildMetricCard(
                              'Attendance Rate',
                              '${_attendancePercentage.toStringAsFixed(1)}%',
                              Icons.pie_chart_outline_rounded,
                              subtitle: 'Monthly Target',
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _buildMetricCard(
                              'Total Days',
                              '$_totalOfficeDays',
                              Icons.business_center_outlined,
                              subtitle: 'Days Logged',
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      Row(
                        children: [
                          Expanded(
                            child: _buildMetricCard(
                              'Current Streak',
                              '$_currentStreak days',
                              Icons.local_fire_department_outlined,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: _buildMetricCard(
                              'Best Streak',
                              '$_bestStreak days',
                              Icons.emoji_events_outlined,
                            ),
                          ),
                        ],
                      ),
                      
                      const SizedBox(height: 28),
                      
                      // Present/Late dates summary section
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            'Attendance History',
                            style: TextStyle(
                              fontSize: 17,
                              fontWeight: FontWeight.w700,
                              color: colorScheme.onSurface,
                              letterSpacing: -0.2,
                            ),
                          ),
                          if (_records.isNotEmpty)
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                              decoration: BoxDecoration(
                                color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                                borderRadius: BorderRadius.circular(12),
                              ),
                              child: Text(
                                '${_records.length} Entries',
                                style: TextStyle(
                                  fontSize: 12,
                                  fontWeight: FontWeight.w500,
                                  color: colorScheme.onSurfaceVariant,
                                ),
                              ),
                            ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      
                      if (_records.isEmpty)
                        Container(
                          padding: const EdgeInsets.symmetric(vertical: 40, horizontal: 20),
                          alignment: Alignment.center,
                          decoration: BoxDecoration(
                            color: colorScheme.surface,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.3)),
                          ),
                          child: Column(
                            children: [
                              Icon(Icons.event_note_outlined, size: 44, color: colorScheme.onSurfaceVariant.withValues(alpha: 0.5)),
                              const SizedBox(height: 12),
                              Text(
                                'No attendance records found for this month',
                                style: TextStyle(color: colorScheme.onSurfaceVariant, fontSize: 14),
                              ),
                            ],
                          ),
                        )
                      else
                        Container(
                          decoration: BoxDecoration(
                            color: colorScheme.surface,
                            borderRadius: BorderRadius.circular(16),
                            border: Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.4)),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.02),
                                blurRadius: 8,
                                offset: const Offset(0, 2),
                              ),
                            ],
                          ),
                          child: ListView.separated(
                            shrinkWrap: true,
                            physics: const NeverScrollableScrollPhysics(),
                            itemCount: _records.length,
                            separatorBuilder: (context, index) => Divider(
                              height: 1,
                              indent: 56,
                              endIndent: 16,
                              color: colorScheme.outlineVariant.withValues(alpha: 0.3),
                            ),
                            itemBuilder: (context, index) {
                              final record = _records[index];
                              final isPresent = record.status == AttendanceStatus.present;

                              return Padding(
                                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                                child: Row(
                                  children: [
                                    Container(
                                      padding: const EdgeInsets.all(8),
                                      decoration: BoxDecoration(
                                        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                                        shape: BoxShape.circle,
                                      ),
                                      child: Icon(
                                        isPresent ? Icons.check_circle_outline_rounded : Icons.access_time_rounded,
                                        size: 18,
                                        color: colorScheme.onSurface,
                                      ),
                                    ),
                                    const SizedBox(width: 14),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment: CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            _formatDate(record.dateYyyyMmDd),
                                            style: const TextStyle(
                                              fontWeight: FontWeight.w600,
                                              fontSize: 14.5,
                                            ),
                                          ),
                                          const SizedBox(height: 2),
                                          Text(
                                            'Marked at: ${DateFormat.jm().format(record.markedAt)}',
                                            style: TextStyle(
                                              fontSize: 12,
                                              color: colorScheme.onSurfaceVariant,
                                            ),
                                          ),
                                        ],
                                      ),
                                    ),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                      decoration: BoxDecoration(
                                        color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.6),
                                        borderRadius: BorderRadius.circular(20),
                                        border: Border.all(
                                          color: colorScheme.outlineVariant.withValues(alpha: 0.5),
                                          width: 1,
                                        ),
                                      ),
                                      child: Text(
                                        record.status.name.toUpperCase(),
                                        style: TextStyle(
                                          fontSize: 11,
                                          fontWeight: FontWeight.w700,
                                          color: colorScheme.onSurface,
                                          letterSpacing: 0.5,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              );
                            },
                          ),
                        ),
                    ],
                  ),
                ),
              ],
            ),
    );
  }
  
  Widget _buildMetricCard(String label, String value, IconData icon, {String? subtitle}) {
    final colorScheme = Theme.of(context).colorScheme;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorScheme.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: colorScheme.outlineVariant.withValues(alpha: 0.4)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.02),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: colorScheme.surfaceContainerHighest.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(icon, size: 20, color: colorScheme.onSurface),
              ),
              if (subtitle != null)
                Text(
                  subtitle,
                  style: TextStyle(fontSize: 11, color: colorScheme.onSurfaceVariant.withValues(alpha: 0.8)),
                ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            value,
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: colorScheme.onSurface,
              letterSpacing: -0.5,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            style: TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w500,
              color: colorScheme.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }

  String _formatDate(String dateStr) {
    try {
      final date = DateTime.parse(dateStr);
      return DateFormat('EEE, MMM d, yyyy').format(date);
    } catch (e) {
      return dateStr;
    }
  }
}

