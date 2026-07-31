import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import 'package:pdf/pdf.dart';
import 'package:printing/printing.dart';
import '../../data/repositories/repositories.dart';
import '../../data/database/app_database.dart';
import '../../core/constants/app_constants.dart';
import '../../providers/providers.dart';
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
  
  Future<void> _loadData() async {
    setState(() => _isLoading = true);
    
    final attendanceRepo = ref.read(attendanceRepositoryProvider);
    final profileRepo = ref.read(userProfileRepositoryProvider);
    final officeConfigRepo = ref.read(officeConfigRepositoryProvider);
    
    _records = await attendanceRepo.getForMonth(_selectedMonth.year, _selectedMonth.month);
    _profile = await profileRepo.getProfile();
    _config = await officeConfigRepo.getConfig();
    
    _calculateMetrics();
    
    setState(() => _isLoading = false);
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
                        onPressed: tempYear > 2020
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
                        final isSelected = _selectedMonth.year == tempYear && _selectedMonth.month == monthNum;

                        return InkWell(
                          onTap: isFuture
                              ? null
                              : () => Navigator.pop(context, monthDate),
                          borderRadius: BorderRadius.circular(10),
                          child: Container(
                            alignment: Alignment.center,
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? Theme.of(context).primaryColor
                                  : isFuture
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
                                    : isFuture
                                        ? Colors.grey
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
    if (_profile == null || _config == null) return;
    
    final pdfService = PdfExportService();
    final pdf = await pdfService.generateAttendancePdf(
      year: _selectedMonth.year,
      month: _selectedMonth.month,
      profile: _profile!,
      records: _records,
      workingDaysMask: _config!.workingDaysMask,
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

    return Scaffold(
      appBar: AppBar(
        title: const Text('Monthly Insights'),
        actions: [
          IconButton(
            icon: const Icon(Icons.picture_as_pdf),
            onPressed: _isLoading ? null : _exportPdf,
          ),
        ],
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                // Enhanced Month Selector Header
                Container(
                  margin: const EdgeInsets.fromLTRB(16, 12, 16, 4),
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surface,
                    borderRadius: BorderRadius.circular(14),
                    border: Border.all(color: Theme.of(context).dividerColor, width: 1.5),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      IconButton(
                        icon: const Icon(Icons.chevron_left_rounded),
                        onPressed: _previousMonth,
                        tooltip: 'Previous Month',
                      ),
                      InkWell(
                        onTap: _selectMonth,
                        borderRadius: BorderRadius.circular(8),
                        child: Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          child: Row(
                            children: [
                              const Icon(Icons.calendar_month_outlined, size: 20),
                              const SizedBox(width: 8),
                              Text(
                                DateFormat('MMMM yyyy').format(_selectedMonth),
                                style: const TextStyle(
                                  fontSize: 17,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const Icon(Icons.arrow_drop_down),
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
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
                    children: [
                      _buildMetricCard(
                        'Attendance %',
                        '${_attendancePercentage.toStringAsFixed(1)}%',
                        Icons.pie_chart,
                        AppColors.green,
                      ),
                      const SizedBox(height: 16),
                      _buildMetricCard(
                        'Total Office Days',
                        '$_totalOfficeDays',
                        Icons.business,
                        AppColors.black,
                      ),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: _buildMetricCard(
                              'Current Streak',
                              '$_currentStreak days',
                              Icons.local_fire_department,
                              Colors.orange,
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: _buildMetricCard(
                              'Best Streak',
                              '$_bestStreak days',
                              Icons.emoji_events,
                              Colors.amber,
                            ),
                          ),
                        ],
                      ),
                      
                      // Present/Late dates summary
                      if (_records.isNotEmpty) ...[
                        const SizedBox(height: 24),
                        const Text(
                          'Attendance Details',
                          style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 8),
                        ..._records.map((record) => ListTile(
                          leading: Icon(
                            record.status == AttendanceStatus.present
                                ? Icons.check_circle
                                : Icons.warning,
                            color: record.status == AttendanceStatus.present
                                ? AppColors.green
                                : Colors.orange,
                          ),
                          title: Text(_formatDate(record.dateYyyyMmDd)),
                          subtitle: Text(record.status.name.toUpperCase()),
                        )),
                      ],
                    ],
                  ),
                ),
              ],
            ),
    );
  }
  
  Widget _buildMetricCard(String label, String value, IconData icon, Color color) {
    final onSurface = Theme.of(context).colorScheme.onSurface;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: onSurface.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(14),
                border: Border.all(color: onSurface.withValues(alpha: 0.3), width: 1.5),
              ),
              child: Icon(icon, size: 28, color: onSurface),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: TextStyle(fontSize: 13, color: onSurface.withValues(alpha: 0.7)),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    value,
                    style: TextStyle(
                      fontSize: 22,
                      fontWeight: FontWeight.bold,
                      color: onSurface,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDate(String dateStr) {
    try {
      final date = DateTime.parse(dateStr);
      return DateFormat('EEEE, MMM d, yyyy').format(date);
    } catch (e) {
      return dateStr;
    }
  }
}
