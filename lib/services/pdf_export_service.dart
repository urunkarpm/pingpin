import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import '../data/database/app_database.dart';
import '../core/utils/date_utils.dart';

/// Service for generating modern, clean PDF attendance reports
class PdfExportService {
  /// Generates PDF for a specific month
  Future<pw.Document> generateAttendancePdf({
    required int year,
    required int month,
    required UserProfile profile,
    required List<AttendanceRecord> records,
    required int workingDaysMask,
  }) async {
    final pdf = pw.Document();
    
    final totalOfficeDays = records.length;
    final workingDays = getWorkingDaysInMonth(year, month, workingDaysMask);
    final eligibleWorkingDays = workingDays.length;
    final attendancePercentage = eligibleWorkingDays > 0 
        ? (totalOfficeDays / eligibleWorkingDays * 100).toStringAsFixed(1)
        : '0.0';
    
    // Sort records by date
    final sortedRecords = List<AttendanceRecord>.from(records)
      ..sort((a, b) => a.dateYyyyMmDd.compareTo(b.dateYyyyMmDd));
    
    final monthYear = formatMonthYear(year, month);
    
    // Clean styling constants
    const primaryColor = PdfColor.fromInt(0xFF121212);
    const borderColor = PdfColor.fromInt(0xFF222222);
    const surfaceBg = PdfColor.fromInt(0xFFF7F4EB);
    
    pdf.addPage(
      pw.MultiPage(
        pageFormat: PdfPageFormat.a4,
        margin: const pw.EdgeInsets.all(32),
        build: (pw.Context context) {
          return [
            // Title Header
            pw.Row(
              mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
              crossAxisAlignment: pw.CrossAxisAlignment.center,
              children: [
                pw.Column(
                  crossAxisAlignment: pw.CrossAxisAlignment.start,
                  children: [
                    pw.Text(
                      'PingPin Attendance Report',
                      style: pw.TextStyle(
                        fontSize: 22,
                        fontWeight: pw.FontWeight.bold,
                        color: primaryColor,
                      ),
                    ),
                    pw.SizedBox(height: 4),
                    pw.Text(
                      monthYear,
                      style: const pw.TextStyle(
                        fontSize: 14,
                        color: PdfColors.grey700,
                      ),
                    ),
                  ],
                ),
                pw.Container(
                  padding: const pw.EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: pw.BoxDecoration(
                    border: pw.Border.all(color: borderColor, width: 1.5),
                    borderRadius: pw.BorderRadius.circular(6),
                    color: surfaceBg,
                  ),
                  child: pw.Text(
                    '${attendancePercentage}% Attendance',
                    style: pw.TextStyle(
                      fontWeight: pw.FontWeight.bold,
                      fontSize: 12,
                      color: primaryColor,
                    ),
                  ),
                ),
              ],
            ),
            
            pw.SizedBox(height: 20),
            pw.Divider(color: borderColor, thickness: 1.5),
            pw.SizedBox(height: 16),
            
            // Employee Profile & Statistics Grid
            pw.Row(
              crossAxisAlignment: pw.CrossAxisAlignment.start,
              children: [
                // Profile Section
                pw.Expanded(
                  child: pw.Container(
                    padding: const pw.EdgeInsets.all(12),
                    decoration: pw.BoxDecoration(
                      border: pw.Border.all(color: borderColor, width: 1.5),
                      borderRadius: pw.BorderRadius.circular(8),
                      color: surfaceBg,
                    ),
                    child: pw.Column(
                      crossAxisAlignment: pw.CrossAxisAlignment.start,
                      children: [
                        pw.Text(
                          'Employee Profile',
                          style: pw.TextStyle(
                            fontSize: 13,
                            fontWeight: pw.FontWeight.bold,
                            color: primaryColor,
                          ),
                        ),
                        pw.SizedBox(height: 8),
                        pw.Text('Name: ${profile.fullName}', style: const pw.TextStyle(fontSize: 11)),
                        pw.Text('Designation: ${profile.designation}', style: const pw.TextStyle(fontSize: 11)),
                        if (profile.employeeId != null && profile.employeeId!.isNotEmpty)
                          pw.Text('ID: ${profile.employeeId}', style: const pw.TextStyle(fontSize: 11)),
                        if (profile.email != null && profile.email!.isNotEmpty)
                          pw.Text('Email: ${profile.email}', style: const pw.TextStyle(fontSize: 11)),
                      ],
                    ),
                  ),
                ),
                pw.SizedBox(width: 16),
                
                // Statistics Summary
                pw.Expanded(
                  child: pw.Container(
                    padding: const pw.EdgeInsets.all(12),
                    decoration: pw.BoxDecoration(
                      border: pw.Border.all(color: borderColor, width: 1.5),
                      borderRadius: pw.BorderRadius.circular(8),
                      color: surfaceBg,
                    ),
                    child: pw.Column(
                      crossAxisAlignment: pw.CrossAxisAlignment.start,
                      children: [
                        pw.Text(
                          'Monthly Metrics',
                          style: pw.TextStyle(
                            fontSize: 13,
                            fontWeight: pw.FontWeight.bold,
                            color: primaryColor,
                          ),
                        ),
                        pw.SizedBox(height: 8),
                        pw.Text('Total Office Days Attended: $totalOfficeDays', style: const pw.TextStyle(fontSize: 11)),
                        pw.Text('Total Working Days: $eligibleWorkingDays', style: const pw.TextStyle(fontSize: 11)),
                        pw.Text('Attendance Rate: $attendancePercentage%', style: const pw.TextStyle(fontSize: 11)),
                      ],
                    ),
                  ),
                ),
              ],
            ),
            
            pw.SizedBox(height: 24),
            
            // Attended Dates Table / List
            pw.Text(
              'Recorded Attendance Dates',
              style: pw.TextStyle(
                fontSize: 14,
                fontWeight: pw.FontWeight.bold,
                color: primaryColor,
              ),
            ),
            pw.SizedBox(height: 10),
            
            if (sortedRecords.isNotEmpty)
              pw.Wrap(
                spacing: 8,
                runSpacing: 8,
                children: sortedRecords
                    .map((r) => pw.Container(
                          padding: const pw.EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                          decoration: pw.BoxDecoration(
                            border: pw.Border.all(color: borderColor, width: 1.2),
                            borderRadius: pw.BorderRadius.circular(6),
                            color: surfaceBg,
                          ),
                          child: pw.Text(
                            _formatDateForDisplay(r.dateYyyyMmDd),
                            style: pw.TextStyle(
                              fontSize: 11,
                              fontWeight: pw.FontWeight.bold,
                              color: primaryColor,
                            ),
                          ),
                        ))
                    .toList(),
              )
            else
              pw.Text(
                'No attendance records found for this month.',
                style: const pw.TextStyle(fontSize: 11, color: PdfColors.grey700),
              ),

            pw.SizedBox(height: 30),
            
            // Footer
            pw.Divider(color: borderColor, thickness: 1),
            pw.SizedBox(height: 6),
            pw.Row(
              mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
              children: [
                pw.Text(
                  'PingPin Local Attendance System',
                  style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey700),
                ),
                pw.Text(
                  'Generated on ${DateTime.now().toString().split(' ')[0]}',
                  style: const pw.TextStyle(fontSize: 9, color: PdfColors.grey700),
                ),
              ],
            ),
          ];
        },
      ),
    );
    
    return pdf;
  }
  
  String _formatDateForDisplay(String dateStr) {
    try {
      final parts = dateStr.split('-');
      final month = int.parse(parts[1]);
      final day = int.parse(parts[2]);
      
      const months = [
        'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
        'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
      ];
      
      return '${months[month - 1]} $day';
    } catch (e) {
      return dateStr;
    }
  }
}
