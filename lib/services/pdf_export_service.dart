import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import '../data/database/app_database.dart';
import '../core/utils/date_utils.dart';

/// Service for generating modern, sleek, and beautifully formatted PDF attendance reports
class PdfExportService {
  /// Generates PDF for a specific month
  Future<pw.Document> generateAttendancePdf({
    required int year,
    required int month,
    required UserProfile profile,
    required List<AttendanceRecord> records,
    required int workingDaysMask,
    int wfoDaysMask = 31,
    List<WfoScheduleHistoryEntry> wfoHistory = const [],
  }) async {
    final pdf = pw.Document();

    final today = DateTime.now();
    final todayTruncated = DateTime(today.year, today.month, today.day);

    final totalOfficeDays = records.length;
    final workingDays = getWorkingDaysInMonth(year, month, workingDaysMask);

    int getWfoMaskForDate(DateTime date) {
      if (wfoHistory.isEmpty) return wfoDaysMask;
      final dateOnly = DateTime(date.year, date.month, date.day);
      int mask = wfoHistory.first.wfoDaysMask;
      for (final entry in wfoHistory) {
        final effDate = DateTime(
          entry.effectiveFrom.year,
          entry.effectiveFrom.month,
          entry.effectiveFrom.day,
        );
        if (dateOnly.isBefore(effDate)) break;
        mask = entry.wfoDaysMask;
      }
      return mask;
    }

    // Total required WFO days in the entire month based on active schedule at each date
    final wfoWorkingDays = workingDays
        .where((d) => (getWfoMaskForDate(d) & (1 << (d.weekday - 1))) != 0)
        .toList();
    final wfoDaysCount = wfoWorkingDays.length;

    // Filter past and present WFO days (up to today) for attendance rate & absent calculation
    final evaluatedWfoDays = wfoWorkingDays
        .where((d) => !d.isAfter(todayTruncated))
        .toList();
    final evaluatedWfoCount = evaluatedWfoDays.length;

    final double pct = evaluatedWfoCount > 0
        ? (totalOfficeDays / evaluatedWfoCount * 100)
        : 0.0;
    final attendancePercentage = pct.toStringAsFixed(1);
    final absentDays = evaluatedWfoCount > totalOfficeDays
        ? evaluatedWfoCount - totalOfficeDays
        : 0;
    final eligibleWorkingDays = workingDays.length;

    // Set of attended dates formatted YYYY-MM-DD
    final attendedDatesSet = records.map((r) => r.dateYyyyMmDd).toSet();

    final monthYear = formatMonthYear(year, month);

    // Modern color palette
    const primaryDark = PdfColor.fromInt(0xFF1E293B); // Slate 800
    const primaryAccent = PdfColor.fromInt(0xFF0F766E); // Teal 700
    const accentLightBg = PdfColor.fromInt(0xFFF0FDF4); // Emerald 50
    const accentLightBorder = PdfColor.fromInt(0xFFBBF7D0); // Emerald 200
    const textDark = PdfColor.fromInt(0xFF0F172A); // Slate 900
    const textMuted = PdfColor.fromInt(0xFF64748B); // Slate 500
    const borderSoft = PdfColor.fromInt(0xFFE2E8F0); // Slate 200
    const bgSoft = PdfColor.fromInt(0xFFF8FAFC); // Slate 50
    const successGreen = PdfColor.fromInt(0xFF16A34A); // Green 600
    const softRed = PdfColor.fromInt(0xFFDC2626); // Red 600
    const wfoPurple = PdfColor.fromInt(0xFF6D28D9); // Purple 700

    pdf.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.all(36),
        header: (pw.Context context) {
          return pw.Container(
            margin: const pw.EdgeInsets.only(bottom: 20),
            padding: const pw.EdgeInsets.only(bottom: 12),
            decoration: const pw.BoxDecoration(
              border: pw.Border(
                bottom: pw.BorderSide(color: borderSoft, width: 1),
              ),
            ),
            child: pw.Row(
              mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
              children: [
                pw.Row(
                  crossAxisAlignment: pw.CrossAxisAlignment.center,
                  children: [
                    pw.Container(
                      width: 24,
                      height: 24,
                      decoration: pw.BoxDecoration(
                        color: primaryDark,
                        borderRadius: pw.BorderRadius.circular(6),
                      ),
                      child: pw.Center(
                        child: pw.Text(
                          'P',
                          style: pw.TextStyle(
                            color: PdfColors.white,
                            fontWeight: pw.FontWeight.bold,
                            fontSize: 14,
                          ),
                        ),
                      ),
                    ),
                    pw.SizedBox(width: 8),
                    pw.Text(
                      'PingPin',
                      style: pw.TextStyle(
                        fontSize: 16,
                        fontWeight: pw.FontWeight.bold,
                        color: primaryDark,
                        letterSpacing: 0.5,
                      ),
                    ),
                    pw.SizedBox(width: 6),
                    pw.Text(
                      '|  Attendance Statement',
                      style: const pw.TextStyle(
                        fontSize: 12,
                        color: textMuted,
                      ),
                    ),
                  ],
                ),
                pw.Text(
                  monthYear,
                  style: pw.TextStyle(
                    fontSize: 12,
                    fontWeight: pw.FontWeight.bold,
                    color: primaryAccent,
                  ),
                ),
              ],
            ),
          );
        },
        footer: (pw.Context context) {
          return pw.Container(
            margin: const pw.EdgeInsets.only(top: 20),
            padding: const pw.EdgeInsets.only(top: 12),
            decoration: const pw.BoxDecoration(
              border: pw.Border(
                top: pw.BorderSide(color: borderSoft, width: 1),
              ),
            ),
            child: pw.Row(
              mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
              children: [
                pw.Text(
                  'Confidential • Generated automatically by PingPin',
                  style: const pw.TextStyle(fontSize: 8, color: textMuted),
                ),
                pw.Text(
                  'Page ${context.pageNumber} of ${context.pagesCount}',
                  style: const pw.TextStyle(fontSize: 8, color: textMuted),
                ),
              ],
            ),
          );
        },
        build: (pw.Context context) {
          return [
            // User & Hero Badge Row
            pw.Row(
              crossAxisAlignment: pw.CrossAxisAlignment.start,
              children: [
                // User Details Card
                pw.Expanded(
                  flex: 3,
                  child: pw.Container(
                    padding: const pw.EdgeInsets.all(16),
                    decoration: pw.BoxDecoration(
                      color: bgSoft,
                      borderRadius: pw.BorderRadius.circular(10),
                      border: pw.Border.all(color: borderSoft, width: 1),
                    ),
                    child: pw.Column(
                      crossAxisAlignment: pw.CrossAxisAlignment.start,
                      children: [
                        pw.Text(
                          profile.fullName.toUpperCase(),
                          style: pw.TextStyle(
                            fontSize: 16,
                            fontWeight: pw.FontWeight.bold,
                            color: textDark,
                            letterSpacing: 0.5,
                          ),
                        ),
                        pw.SizedBox(height: 4),
                        pw.Text(
                          profile.designation,
                          style: pw.TextStyle(
                            fontSize: 11,
                            fontWeight: pw.FontWeight.bold,
                            color: primaryAccent,
                          ),
                        ),
                        pw.SizedBox(height: 10),
                        pw.Divider(color: borderSoft, thickness: 0.8),
                        pw.SizedBox(height: 8),
                        pw.Row(
                          children: [
                            if (profile.employeeId != null &&
                                profile.employeeId!.isNotEmpty) ...[
                              pw.Text(
                                'Emp ID: ',
                                style: const pw.TextStyle(
                                    fontSize: 9.5, color: textMuted),
                              ),
                              pw.Text(
                                profile.employeeId!,
                                style: pw.TextStyle(
                                  fontSize: 9.5,
                                  fontWeight: pw.FontWeight.bold,
                                  color: textDark,
                                ),
                              ),
                              pw.SizedBox(width: 16),
                            ],
                            if (profile.email != null &&
                                profile.email!.isNotEmpty) ...[
                              pw.Text(
                                'Email: ',
                                style: const pw.TextStyle(
                                    fontSize: 9.5, color: textMuted),
                              ),
                              pw.Text(
                                profile.email!,
                                style: pw.TextStyle(
                                  fontSize: 9.5,
                                  fontWeight: pw.FontWeight.bold,
                                  color: textDark,
                                ),
                              ),
                            ],
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                pw.SizedBox(width: 14),

                // Key Attendance Score Badge
                pw.Expanded(
                  flex: 2,
                  child: pw.Container(
                    padding: const pw.EdgeInsets.all(16),
                    decoration: pw.BoxDecoration(
                      color: accentLightBg,
                      borderRadius: pw.BorderRadius.circular(10),
                      border:
                          pw.Border.all(color: accentLightBorder, width: 1),
                    ),
                    child: pw.Column(
                      mainAxisAlignment: pw.MainAxisAlignment.center,
                      crossAxisAlignment: pw.CrossAxisAlignment.center,
                      children: [
                        pw.Text(
                          'ATTENDANCE RATE',
                          style: pw.TextStyle(
                            fontSize: 9,
                            fontWeight: pw.FontWeight.bold,
                            color: primaryAccent,
                            letterSpacing: 1.0,
                          ),
                        ),
                        pw.SizedBox(height: 6),
                        pw.Text(
                          '$attendancePercentage%',
                          style: pw.TextStyle(
                            fontSize: 26,
                            fontWeight: pw.FontWeight.bold,
                            color: textDark,
                          ),
                        ),
                        pw.SizedBox(height: 4),
                        pw.Text(
                          '$totalOfficeDays of $evaluatedWfoCount required WFO days attended',
                          textAlign: pw.TextAlign.center,
                          style: const pw.TextStyle(
                            fontSize: 8.5,
                            color: textMuted,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),

            pw.SizedBox(height: 18),

            // Statistics Overview Cards
            pw.Row(
              children: [
                _buildStatCard(
                  label: 'WORKING DAYS',
                  value: '$eligibleWorkingDays',
                  accentColor: primaryDark,
                ),
                pw.SizedBox(width: 10),
                _buildStatCard(
                  label: 'REQUIRED WFO DAYS',
                  value: '$wfoDaysCount',
                  accentColor: wfoPurple,
                ),
                pw.SizedBox(width: 10),
                _buildStatCard(
                  label: 'DAYS PRESENT',
                  value: '$totalOfficeDays',
                  accentColor: successGreen,
                ),
                pw.SizedBox(width: 10),
                _buildStatCard(
                  label: 'DAYS ABSENT',
                  value: '$absentDays',
                  accentColor: softRed,
                ),
              ],
            ),

            pw.SizedBox(height: 24),

            // Detailed Attendance Breakdown Section
            pw.Text(
              'WFO Scheduled Attendance Breakdown',
              style: pw.TextStyle(
                fontSize: 13,
                fontWeight: pw.FontWeight.bold,
                color: textDark,
              ),
            ),
            pw.SizedBox(height: 4),
            pw.Text(
              'Daily check-in status for scheduled Work From Office (WFO) days in $monthYear.',
              style: const pw.TextStyle(
                fontSize: 9.5,
                color: textMuted,
              ),
            ),
            pw.SizedBox(height: 12),

            // Attendance Table
            if (wfoWorkingDays.isNotEmpty)
              _buildAttendanceTable(
                wfoDays: wfoWorkingDays,
                attendedSet: attendedDatesSet,
                records: records,
              )
            else
              pw.Container(
                padding: const pw.EdgeInsets.all(20),
                alignment: pw.Alignment.center,
                decoration: pw.BoxDecoration(
                  color: bgSoft,
                  borderRadius: pw.BorderRadius.circular(8),
                  border: pw.Border.all(color: borderSoft, width: 1),
                ),
                child: pw.Text(
                  'No WFO days scheduled for this month.',
                  style: const pw.TextStyle(fontSize: 10, color: textMuted),
                ),
              ),
          ];
        },
      ),
    );

    return pdf;
  }

  pw.Widget _buildStatCard({
    required String label,
    required String value,
    required PdfColor accentColor,
  }) {
    const borderSoft = PdfColor.fromInt(0xFFE2E8F0);
    const textMuted = PdfColor.fromInt(0xFF64748B);
    const bgSoft = PdfColor.fromInt(0xFFF8FAFC);

    return pw.Expanded(
      child: pw.Container(
        padding: const pw.EdgeInsets.symmetric(vertical: 12, horizontal: 12),
        decoration: pw.BoxDecoration(
          color: bgSoft,
          borderRadius: pw.BorderRadius.circular(8),
          border: pw.Border.all(color: borderSoft, width: 1),
        ),
        child: pw.Column(
          crossAxisAlignment: pw.CrossAxisAlignment.start,
          children: [
            pw.Text(
              label,
              style: pw.TextStyle(
                fontSize: 7.5,
                fontWeight: pw.FontWeight.bold,
                color: textMuted,
                letterSpacing: 0.5,
              ),
            ),
            pw.SizedBox(height: 6),
            pw.Row(
              children: [
                pw.Container(
                  width: 4,
                  height: 16,
                  decoration: pw.BoxDecoration(
                    color: accentColor,
                    borderRadius: pw.BorderRadius.circular(2),
                  ),
                ),
                pw.SizedBox(width: 8),
                pw.Text(
                  value,
                  style: pw.TextStyle(
                    fontSize: 18,
                    fontWeight: pw.FontWeight.bold,
                    color: const PdfColor.fromInt(0xFF0F172A),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  pw.Widget _buildAttendanceTable({
    required List<DateTime> wfoDays,
    required Set<String> attendedSet,
    required List<AttendanceRecord> records,
  }) {
    const textDark = PdfColor.fromInt(0xFF0F172A);
    const borderSoft = PdfColor.fromInt(0xFFE2E8F0);
    const tableHeaderBg = PdfColor.fromInt(0xFFF1F5F9);
    const presentBg = PdfColor.fromInt(0xFFDCFCE7); // Green 100
    const presentText = PdfColor.fromInt(0xFF15803D); // Green 700
    const absentBg = PdfColor.fromInt(0xFFFEE2E2); // Red 100
    const absentText = PdfColor.fromInt(0xFFB91C1C); // Red 700
    const upcomingBg = PdfColor.fromInt(0xFFF1F5F9); // Slate 100
    const upcomingText = PdfColor.fromInt(0xFF64748B); // Slate 500

    final today = DateTime.now();
    final todayTruncated = DateTime(today.year, today.month, today.day);

    const daysOfWeek = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const months = [
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec'
    ];

    return pw.Table(
      border: pw.TableBorder.all(color: borderSoft, width: 0.8),
      columnWidths: {
        0: const pw.FlexColumnWidth(4),
        1: const pw.FlexColumnWidth(3),
        2: const pw.FlexColumnWidth(3),
      },
      children: [
        // Header Row
        pw.TableRow(
          decoration: const pw.BoxDecoration(color: tableHeaderBg),
          children: [
            _tableCell('Date', isHeader: true),
            _tableCell('Day', isHeader: true),
            _tableCell('Status', isHeader: true, align: pw.TextAlign.center),
          ],
        ),
        // Data Rows - strictly for WFO days
        ...wfoDays.map((date) {
          final dateStr = _formatDateIso(date);
          final isPresent = attendedSet.contains(dateStr);
          final isFuture = date.isAfter(todayTruncated);

          final dateDisplay =
              '${months[date.month - 1]} ${date.day}, ${date.year}';
          final dayName = daysOfWeek[date.weekday - 1];

          final String statusLabel = isPresent
              ? 'PRESENT'
              : (isFuture ? 'UPCOMING' : 'ABSENT');

          final PdfColor badgeBg = isPresent
              ? presentBg
              : (isFuture ? upcomingBg : absentBg);

          final PdfColor badgeText = isPresent
              ? presentText
              : (isFuture ? upcomingText : absentText);

          return pw.TableRow(
            children: [
              _tableCell(dateDisplay, isBold: true),
              _tableCell(dayName, textColor: textDark, isBold: true),
              // Status Badge
              pw.Padding(
                padding:
                    const pw.EdgeInsets.symmetric(horizontal: 6, vertical: 5),
                child: pw.Center(
                  child: pw.Container(
                    padding: const pw.EdgeInsets.symmetric(
                        horizontal: 8, vertical: 3),
                    decoration: pw.BoxDecoration(
                      color: badgeBg,
                      borderRadius: pw.BorderRadius.circular(4),
                    ),
                    child: pw.Text(
                      statusLabel,
                      style: pw.TextStyle(
                        fontSize: 8,
                        fontWeight: pw.FontWeight.bold,
                        color: badgeText,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          );
        }),
      ],
    );
  }

  pw.Widget _tableCell(
    String text, {
    bool isHeader = false,
    bool isBold = false,
    PdfColor textColor = const PdfColor.fromInt(0xFF0F172A),
    pw.TextAlign align = pw.TextAlign.left,
  }) {
    return pw.Padding(
      padding: const pw.EdgeInsets.symmetric(horizontal: 10, vertical: 7),
      child: pw.Text(
        text,
        textAlign: align,
        style: pw.TextStyle(
          fontSize: isHeader ? 9 : 8.5,
          fontWeight: (isHeader || isBold) ? pw.FontWeight.bold : pw.FontWeight.normal,
          color: isHeader ? const PdfColor.fromInt(0xFF334155) : textColor,
        ),
      ),
    );
  }

  String _formatDateIso(DateTime dt) {
    return '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')}';
  }
}


