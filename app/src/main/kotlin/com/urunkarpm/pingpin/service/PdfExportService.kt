package com.urunkarpm.pingpin.service

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfExportService(private val context: Context) {

    // ponytail: Hardcoded 2-page ceiling for standard monthly attendance statements (up to 31 days).
    // Upgrade path: dynamic multi-page canvas flow if annual/multi-month reports are requested.
    suspend fun generateAttendancePdf(
        year: Int,
        month: Int,
        profile: UserProfileEntity,
        records: List<AttendanceRecordEntity>,
        workingDaysMask: Int,
        wfoDaysMask: Int = 31,
        officeConfig: OfficeConfigEntity? = null
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Corporate Executive Palette
        val primaryDark = Color.parseColor("#0F172A")    // Slate 900
        val secondaryDark = Color.parseColor("#334155")  // Slate 700
        val textDark = Color.parseColor("#0F172A")       // Authoritative body text
        val textMuted = Color.parseColor("#64748B")      // Slate 500
        val textSubtle = Color.parseColor("#94A3B8")     // Slate 400
        val bgSoft = Color.parseColor("#F8FAFC")         // Slate 50
        val borderSoft = Color.parseColor("#E2E8F0")     // Slate 200
        val borderStrong = Color.parseColor("#CBD5E1")   // Slate 300
        val headerFill = Color.parseColor("#F1F5F9")     // Table Header fill

        // Status Colors
        val successGreenFg = Color.parseColor("#166534") // Emerald 800
        val successGreenBg = Color.parseColor("#DCFCE7") // Emerald 100
        val successGreenBorder = Color.parseColor("#86EFAC")

        val warningAmberFg = Color.parseColor("#92400E") // Amber 800
        val warningAmberBg = Color.parseColor("#FEF3C7") // Amber 100
        val warningAmberBorder = Color.parseColor("#FCD34D")

        val softRedFg = Color.parseColor("#991B1B")       // Red 800
        val softRedBg = Color.parseColor("#FEE2E2")       // Red 100
        val softRedBorder = Color.parseColor("#FCA5A5")

        val extraBlueFg = Color.parseColor("#1E40AF")     // Blue 800
        val extraBlueBg = Color.parseColor("#DBEAFE")     // Blue 100
        val extraBlueBorder = Color.parseColor("#93C5FD")

        val upcomingFg = Color.parseColor("#475569")      // Slate 600
        val upcomingBg = Color.parseColor("#F1F5F9")      // Slate 100
        val upcomingBorder = Color.parseColor("#CBD5E1")

        val installCal = AppInstallManager.getInstallDateCalendar(context)

        // Month Setup
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val recordsMap = records.associateBy { it.dateYyyyMmDd }

        val wfoDays = mutableListOf<Calendar>()
        val reportDaysMap = LinkedHashMap<String, Calendar>()

        for (day in 1..maxDays) {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            val isAttended = recordsMap.containsKey(isoDate)
            val isWfo = WorkingDays.isWorkingDay(cal, workingDaysMask) && WorkingDays.isWfoDay(cal, wfoDaysMask)
            val isAfterOrOnInstall = !cal.before(installCal) || isAttended

            if (isAfterOrOnInstall && isWfo) {
                wfoDays.add(cal)
            }
            if (isAfterOrOnInstall && (isWfo || isAttended)) {
                reportDaysMap[isoDate] = cal
            }
        }
        val reportDays = reportDaysMap.values.toList()

        val totalOfficeDays = recordsMap.size

        val extraWfoCount = records.count { rec ->
            val cal = Calendar.getInstance().apply {
                val parts = rec.dateYyyyMmDd.split("-")
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            !WorkingDays.isWfoDay(cal, wfoDaysMask) || !WorkingDays.isWorkingDay(cal, workingDaysMask)
        }

        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)

        val evaluatedWfoDays = wfoDays.filter { !it.after(todayCal) }
        val evaluatedCount = evaluatedWfoDays.size
        val pct = if (evaluatedCount > 0) (totalOfficeDays.toDouble() / evaluatedCount * 100) else 0.0
        val attendancePctStr = String.format(Locale.US, "%.1f", pct)

        // Punctuality & Check-In Stats
        var lateCount = 0
        var onTimeCount = 0
        for (r in records) {
            if (r.status.equals("late", ignoreCase = true)) {
                lateCount++
            } else {
                onTimeCount++
            }
        }
        val punctualityPct = if (records.isNotEmpty()) (onTimeCount.toDouble() / records.size * 100) else 100.0

        val wifiCheckIns = records.count { !it.ssidSnapshot.isNullOrBlank() }
        val autoPunchPct = if (records.isNotEmpty()) (wifiCheckIns.toDouble() / records.size * 100) else 0.0

        // Average Check-in Time Calculation
        var avgCheckInTimeStr = "--:--"
        if (records.isNotEmpty()) {
            val sdfTime = SimpleDateFormat("hh:mm a", Locale.US)
            val calTmp = Calendar.getInstance()
            var totalMinutes = 0L
            var validCount = 0
            for (rec in records) {
                calTmp.timeInMillis = rec.markedAt
                val mins = calTmp.get(Calendar.HOUR_OF_DAY) * 60 + calTmp.get(Calendar.MINUTE)
                totalMinutes += mins
                validCount++
            }
            if (validCount > 0) {
                val avgMins = (totalMinutes / validCount).toInt()
                val avgHour = avgMins / 60
                val avgMin = avgMins % 60
                calTmp.set(Calendar.HOUR_OF_DAY, avgHour)
                calTmp.set(Calendar.MINUTE, avgMin)
                avgCheckInTimeStr = sdfTime.format(calTmp.time)
            }
        }

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)
        val generatedTimestamp = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date())
        val nameHash = Math.abs((profile.fullName + year + month + records.size).hashCode()).toString(16).uppercase(Locale.US).padStart(6, '0')
        val docRefId = "PP-${year}${String.format(Locale.US, "%02d", month)}-$nameHash"

        // Up to 18 rows fit cleanly on Page 1 along with the executive summary and signature block
        val totalPages = if (reportDays.size > 18) 2 else 1

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawFooter(c: Canvas, pNum: Int) {
            paint.color = borderSoft
            paint.strokeWidth = 1f
            c.drawLine(40f, 804f, 555f, 804f, paint)

            textPaint.color = textSubtle
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.DEFAULT
            c.drawText("Confidential • Official Workplace Attendance Statement • Security Ref: $docRefId", 40f, 818f, textPaint)
            val pageStr = "Page $pNum of $totalPages"
            val pWidth = textPaint.measureText(pageStr)
            c.drawText(pageStr, 555f - pWidth, 818f, textPaint)
        }

        fun drawRunningHeader(c: Canvas) {
            textPaint.color = primaryDark
            textPaint.textSize = 13f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText("PINGPIN", 40f, 44f, textPaint)

            textPaint.color = textMuted
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.DEFAULT
            c.drawText("  |   Attendance Log (Continued)", 95f, 43.5f, textPaint)

            textPaint.color = secondaryDark
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            val rMonth = monthName.uppercase(Locale.US)
            val rWidth = textPaint.measureText(rMonth)
            c.drawText(rMonth, 555f - rWidth, 43.5f, textPaint)

            paint.color = borderStrong
            paint.strokeWidth = 1f
            c.drawLine(40f, 54f, 555f, 54f, paint)
        }

        fun drawTableHeader(c: Canvas, startY: Float) {
            paint.color = headerFill
            c.drawRect(RectF(40f, startY, 555f, startY + 24f), paint)

            paint.color = borderStrong
            paint.strokeWidth = 1f
            c.drawLine(40f, startY, 555f, startY, paint)
            c.drawLine(40f, startY + 24f, 555f, startY + 24f, paint)

            textPaint.color = secondaryDark
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText("DATE", 50f, startY + 15.5f, textPaint)
            c.drawText("DAY", 135f, startY + 15.5f, textPaint)
            c.drawText("CHECK-IN TIME", 210f, startY + 15.5f, textPaint)
            c.drawText("VERIFICATION METHOD", 310f, startY + 15.5f, textPaint)
            c.drawText("STATUS", 475f, startY + 15.5f, textPaint)
        }

        // ================= PAGE 1 SETUP =================
        // 1. Corporate Masthead Header
        textPaint.color = primaryDark
        textPaint.textSize = 20f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("PINGPIN", 40f, 54f, textPaint)

        textPaint.color = secondaryDark
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("MONTHLY ATTENDANCE STATEMENT", 40f, 68f, textPaint)

        textPaint.color = textSubtle
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Official Employee In-Office Verification Report", 40f, 78f, textPaint)

        // Metadata Block on Top Right
        textPaint.color = primaryDark
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val periodText = monthName.uppercase(Locale.US)
        val periodWidth = textPaint.measureText(periodText)
        canvas.drawText(periodText, 555f - periodWidth, 50f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        val refText = "Document Ref: $docRefId"
        val refWidth = textPaint.measureText(refText)
        canvas.drawText(refText, 555f - refWidth, 64f, textPaint)

        val genText = "Generated: $generatedTimestamp"
        val genWidth = textPaint.measureText(genText)
        canvas.drawText(genText, 555f - genWidth, 76f, textPaint)

        // Masthead Divider Line
        paint.color = primaryDark
        paint.strokeWidth = 1.5f
        canvas.drawLine(40f, 86f, 555f, 86f, paint)

        // 2. Executive Summary & Employee Details (Two Elegant Balanced Panels)
        val summaryY = 96f
        val summaryHeight = 78f

        // Left Panel: Employee Details (40f to 285f)
        val empRect = RectF(40f, summaryY, 285f, summaryY + summaryHeight)
        paint.color = bgSoft
        canvas.drawRoundRect(empRect, 4f, 4f, paint)
        paint.color = borderSoft
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(empRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = textMuted
        textPaint.textSize = 7f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("EMPLOYEE DETAILS", 52f, summaryY + 14f, textPaint)

        val nameText = if (profile.fullName.isBlank()) "VERIFIED EMPLOYEE" else profile.fullName.trim().uppercase(Locale.US)
        textPaint.color = textDark
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(nameText, 52f, summaryY + 31f, textPaint)

        val desigText = if (!profile.designation.isNullOrBlank()) profile.designation.trim() else "Team Member"
        textPaint.color = secondaryDark
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(desigText, 52f, summaryY + 44f, textPaint)

        val empIdText = if (!profile.employeeId.isNullOrBlank()) profile.employeeId.trim() else "N/A"
        val contactText = if (!profile.email.isNullOrBlank()) profile.email.trim() else if (!profile.phone.isNullOrBlank()) profile.phone.trim() else "Verified PingPin Device"
        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        canvas.drawText("Employee ID: $empIdText   •   $contactText", 52f, summaryY + 58f, textPaint)

        val officeName = officeConfig?.ssid?.takeIf { it.isNotBlank() }?.let { "Wi-Fi: $it" } ?: "Main Corporate Office"
        canvas.drawText("Work Location: $officeName", 52f, summaryY + 70f, textPaint)

        // Right Panel: Attendance Performance Summary (295f to 555f)
        val perfRect = RectF(295f, summaryY, 555f, summaryY + summaryHeight)
        paint.color = bgSoft
        canvas.drawRoundRect(perfRect, 4f, 4f, paint)
        paint.color = borderSoft
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(perfRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = textMuted
        textPaint.textSize = 7f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("MONTHLY ATTENDANCE SUMMARY", 307f, summaryY + 14f, textPaint)

        // Rating Status
        val ratingStatus = when {
            evaluatedCount == 0 -> "N/A"
            extraWfoCount > 0 && pct >= 100.0 -> "EXCEEDED TARGET"
            pct >= 100.0 -> "EXCELLENT"
            pct >= 75.0 -> "ON TRACK"
            pct >= 50.0 -> "ATTENTION REQUIRED"
            else -> "LOW COMPLIANCE"
        }

        // Metrics Grid Row 1
        textPaint.color = textDark
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Scheduled WFO: ${wfoDays.size} Days", 307f, summaryY + 31f, textPaint)
        canvas.drawText("Evaluated: $evaluatedCount Days", 435f, summaryY + 31f, textPaint)

        // Metrics Grid Row 2
        val extraSub = if (extraWfoCount > 0) " (+$extraWfoCount Extra)" else ""
        canvas.drawText("Attended: $totalOfficeDays Days$extraSub", 307f, summaryY + 45f, textPaint)

        val complianceColor = if (pct >= 75.0) successGreenFg else if (pct >= 50.0) warningAmberFg else softRedFg
        textPaint.color = complianceColor
        canvas.drawText("Compliance: $attendancePctStr%  ($ratingStatus)", 435f, summaryY + 45f, textPaint)

        // Metrics Grid Row 3
        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Punctuality: ${String.format(Locale.US, "%.0f%%", punctualityPct)} ($onTimeCount on-time, $lateCount late)", 307f, summaryY + 59f, textPaint)

        val primaryWifi = officeConfig?.ssid?.takeIf { it.isNotBlank() } ?: "Office Wi-Fi"
        canvas.drawText("Avg Arrival: $avgCheckInTimeStr   •   Auto-Punch: ${String.format(Locale.US, "%.0f%%", autoPunchPct)} via $primaryWifi", 307f, summaryY + 71f, textPaint)

        // 3. Section Title for Detailed Log
        textPaint.color = primaryDark
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("DETAILED ATTENDANCE LOG", 40f, 196f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Chronological audit of scheduled WFO days & verification telemetry", 215f, 196f, textPaint)

        // 4. Draw Table Header
        var startY = 205f
        drawTableHeader(canvas, startY)
        startY += 24f

        val sdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val sdfDay = SimpleDateFormat("EEEE", Locale.US)
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.US)
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val rowHeight = 24f
        var rowIndex = 0

        for (cal in reportDays) {
            // Split to Page 2 at row 18 for multi-page logs
            if (rowIndex == 18 && totalPages > 1) {
                drawFooter(canvas, pageNum)
                document.finishPage(page)

                pageNum = 2
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                drawRunningHeader(canvas)
                startY = 64f
                drawTableHeader(canvas, startY)
                startY += 24f
            }

            val isoDate = sdfIso.format(cal.time)
            val record = recordsMap[isoDate]
            val isPresent = record != null
            val isWfo = WorkingDays.isWorkingDay(cal, workingDaysMask) && WorkingDays.isWfoDay(cal, wfoDaysMask)
            val isExtraWfo = isPresent && !isWfo
            val isFuture = cal.after(todayCal)
            val isLate = record?.status.equals("late", ignoreCase = true)

            // Alternating Row Background
            if (rowIndex % 2 == 1) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(RectF(40f, startY, 555f, startY + rowHeight), paint)
            }

            // Bottom Divider Line
            paint.color = borderSoft
            paint.strokeWidth = 0.8f
            canvas.drawLine(40f, startY + rowHeight, 555f, startY + rowHeight, paint)

            // Column 1: Date
            textPaint.color = textDark
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(sdfDate.format(cal.time), 50f, startY + 15.5f, textPaint)

            // Column 2: Day of Week
            textPaint.color = textMuted
            textPaint.textSize = 8.2f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sdfDay.format(cal.time), 135f, startY + 15.5f, textPaint)

            // Column 3: Check-in Time
            val timeMarkedText = if (record != null) {
                val tStr = sdfTime.format(Date(record.markedAt))
                if (isLate) "$tStr (Late)" else tStr
            } else "—"

            textPaint.color = if (isPresent) (if (isExtraWfo) extraBlueFg else if (isLate) warningAmberFg else successGreenFg) else textSubtle
            textPaint.typeface = if (isPresent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textPaint.textSize = 8.5f
            canvas.drawText(timeMarkedText, 210f, startY + 15.5f, textPaint)

            // Column 4: Verification Source
            val sourceText = if (record != null) {
                if (!record.ssidSnapshot.isNullOrBlank()) "Wi-Fi: ${record.ssidSnapshot}"
                else if (record.distanceMeters != null) "Geofence (${record.distanceMeters.toInt()}m)"
                else "Manual Check-in"
            } else "—"

            textPaint.color = textMuted
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sourceText, 310f, startY + 15.5f, textPaint)

            // Column 5: Status Badge Pill
            val statusStr = when {
                isExtraWfo && isLate -> "EXTRA (LATE)"
                isExtraWfo -> "EXTRA WFO"
                isPresent && isLate -> "LATE"
                isPresent -> "PRESENT"
                isFuture -> "UPCOMING"
                else -> "ABSENT"
            }

            val badgeBgColor = when {
                isExtraWfo -> extraBlueBg
                isPresent && isLate -> warningAmberBg
                isPresent -> successGreenBg
                isFuture -> upcomingBg
                else -> softRedBg
            }

            val badgeBorderColor = when {
                isExtraWfo -> extraBlueBorder
                isPresent && isLate -> warningAmberBorder
                isPresent -> successGreenBorder
                isFuture -> upcomingBorder
                else -> softRedBorder
            }

            val badgeTextColor = when {
                isExtraWfo -> extraBlueFg
                isPresent && isLate -> warningAmberFg
                isPresent -> successGreenFg
                isFuture -> upcomingFg
                else -> softRedFg
            }

            val bRect = RectF(460f, startY + 4f, 545f, startY + 20f)
            paint.color = badgeBgColor
            canvas.drawRoundRect(bRect, 8f, 8f, paint)

            paint.color = badgeBorderColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f
            canvas.drawRoundRect(bRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            textPaint.color = badgeTextColor
            textPaint.textSize = 7f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            val stWidth = textPaint.measureText(statusStr)
            canvas.drawText(statusStr, 460f + (85f - stWidth) / 2f, startY + 14.8f, textPaint)

            startY += rowHeight
            rowIndex++
        }

        // 5. Table Totals Summary Row
        val totalsHeight = 24f
        paint.color = bgSoft
        canvas.drawRect(RectF(40f, startY, 555f, startY + totalsHeight), paint)

        paint.color = borderStrong
        paint.strokeWidth = 1f
        canvas.drawLine(40f, startY, 555f, startY, paint)
        canvas.drawLine(40f, startY + totalsHeight, 555f, startY + totalsHeight, paint)

        textPaint.color = secondaryDark
        textPaint.textSize = 7.8f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(
            "TOTALS:  ${wfoDays.size} Scheduled   •   $evaluatedCount Evaluated   •   $totalOfficeDays Attended   •   $attendancePctStr% Compliance   •   $onTimeCount On-Time",
            50f,
            startY + 15.5f,
            textPaint
        )
        startY += totalsHeight

        // 6. Corporate Attestation & Signature Block
        // Anchor towards bottom on single-page reports to balance the page gracefully
        val signOffTop = if (totalPages == 1) maxOf(startY + 40f, 620f) else (startY + 30f)

        // Attestation Statement
        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(
            "I hereby attest and certify that the above recorded in-office days represent an accurate accounting of physical workplace attendance.",
            40f,
            signOffTop,
            textPaint
        )

        // Left Signature: Employee
        val sigLineY = signOffTop + 45f
        paint.color = borderStrong
        paint.strokeWidth = 1f
        canvas.drawLine(40f, sigLineY, 230f, sigLineY, paint)

        textPaint.color = textDark
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(nameText, 40f, sigLineY + 13f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Employee Signature & Date", 40f, sigLineY + 24f, textPaint)

        // Right Signature: Supervisor / HR
        canvas.drawLine(365f, sigLineY, 555f, sigLineY, paint)

        textPaint.color = textDark
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("HR Operations / Workplace Admin", 365f, sigLineY + 13f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Authorized Approver & Date", 365f, sigLineY + 24f, textPaint)

        // Official Digital Verification Stamp Note
        textPaint.color = textSubtle
        textPaint.textSize = 6.8f
        val stampNote = "Officially Audited & Verified • PingPin Compliance Engine • Ref: $docRefId • Hash: $nameHash"
        val stampWidth = textPaint.measureText(stampNote)
        canvas.drawText(stampNote, (pageWidth - stampWidth) / 2f, sigLineY + 45f, textPaint)

        // Final Page Footer
        drawFooter(canvas, pageNum)
        document.finishPage(page)

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val pdfFile = File(outputDir, "PingPin_Attendance_${year}_${String.format(Locale.US, "%02d", month)}.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        pdfFile
    }
}
