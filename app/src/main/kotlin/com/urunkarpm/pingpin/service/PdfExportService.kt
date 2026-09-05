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

        // Executive Palette
        val primaryDark = Color.parseColor("#0F172A")    // Slate 900
        val primaryAccent = Color.parseColor("#0F766E")  // Teal 700
        val textDark = Color.parseColor("#0F172A")       // Dark body text
        val textMuted = Color.parseColor("#64748B")      // Slate 500
        val bgSoft = Color.parseColor("#F8FAFC")         // Slate 50
        val borderSoft = Color.parseColor("#E2E8F0")     // Slate 200
        val headerFill = Color.parseColor("#F1F5F9")     // Table Header fill
        val headerText = Color.parseColor("#334155")     // Slate 700

        // Status Colors
        val successGreen = Color.parseColor("#16A34A")
        val successGreenBg = Color.parseColor("#DCFCE7")
        val successGreenFg = Color.parseColor("#15803D")

        val warningAmber = Color.parseColor("#D97706")
        val warningAmberBg = Color.parseColor("#FEF3C7")
        val warningAmberFg = Color.parseColor("#B45309")

        val softRedBg = Color.parseColor("#FEE2E2")
        val softRedFg = Color.parseColor("#B91C1C")

        val upcomingBg = Color.parseColor("#F1F5F9")
        val upcomingFg = Color.parseColor("#64748B")

        val installCal = AppInstallManager.getInstallDateCalendar(context)

        // Month Setup
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val recordsMap = records.associateBy { it.dateYyyyMmDd }

        val wfoDays = mutableListOf<Calendar>()
        for (day in 1..maxDays) {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
            if ((!cal.before(installCal) || recordsMap.containsKey(isoDate)) &&
                WorkingDays.isWorkingDay(cal, workingDaysMask) &&
                WorkingDays.isWfoDay(cal, wfoDaysMask)
            ) {
                wfoDays.add(cal)
            }
        }

        val totalOfficeDays = recordsMap.size

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
        val presentRecords = records.filter { it.status == "present" || it.status == "late" }
        val lateCount = records.count { it.status == "late" }
        val onTimeCount = records.count { it.status == "present" }
        val punctualityPct = if (presentRecords.isNotEmpty()) (onTimeCount.toDouble() / presentRecords.size * 100) else 100.0

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

        // Weekly Distribution
        val weeklyAtt = IntArray(5)
        val weeklyTot = IntArray(5)
        for (cal in wfoDays) {
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val weekIdx = ((dayOfMonth - 1) / 7).coerceIn(0, 4)
            weeklyTot[weekIdx]++
            val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month, dayOfMonth)
            if (recordsMap.containsKey(isoDate)) {
                weeklyAtt[weekIdx]++
            }
        }

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)
        val generatedTimestamp = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date())
        val nameHash = Math.abs((profile.fullName + year + month + records.size).hashCode()).toString(16).uppercase(Locale.US).padStart(6, '0')
        val docRefId = "PP-${year}${String.format(Locale.US, "%02d", month)}-$nameHash"

        // Total page estimate
        val totalPages = if (wfoDays.size > 14) 2 else 1

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawFooter(c: Canvas, pNum: Int) {
            paint.color = borderSoft
            paint.strokeWidth = 1f
            c.drawLine(36f, 800f, 559f, 800f, paint)

            textPaint.color = textMuted
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT
            c.drawText("Confidential • PingPin WFO Report • Ref: $docRefId • Generated: $generatedTimestamp", 36f, 814f, textPaint)
            c.drawText("Page $pNum of $totalPages", 510f, 814f, textPaint)
        }

        fun drawRunningHeader(c: Canvas) {
            // Header Bar
            paint.color = primaryDark
            c.drawRoundRect(RectF(36f, 30f, 54f, 48f), 4f, 4f, paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText("P", 42f, 43f, textPaint)

            textPaint.color = primaryDark
            textPaint.textSize = 14f
            c.drawText("PingPin", 60f, 44f, textPaint)

            textPaint.color = textMuted
            textPaint.textSize = 9.5f
            textPaint.typeface = Typeface.DEFAULT
            c.drawText("|  WFO Attendance Log (Continued)", 115f, 44f, textPaint)

            textPaint.color = primaryAccent
            textPaint.textSize = 11f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText(monthName.uppercase(Locale.US), 470f, 44f, textPaint)

            paint.color = borderSoft
            paint.strokeWidth = 1f
            c.drawLine(36f, 56f, 559f, 56f, paint)
        }

        fun drawTableHeader(c: Canvas, startY: Float) {
            paint.color = headerFill
            c.drawRoundRect(RectF(36f, startY, 559f, startY + 22f), 4f, 4f, paint)

            textPaint.color = headerText
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText("DATE", 46f, startY + 15f, textPaint)
            c.drawText("DAY", 135f, startY + 15f, textPaint)
            c.drawText("CHECK-IN TIME", 205f, startY + 15f, textPaint)
            c.drawText("VERIFICATION / NETWORK", 315f, startY + 15f, textPaint)
            c.drawText("STATUS", 465f, startY + 15f, textPaint)
        }

        // ================= PAGE 1 SETUP =================
        // 1. Executive Top Header
        paint.color = primaryDark
        canvas.drawRoundRect(RectF(36f, 32f, 60f, 56f), 6f, 6f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("P", 44f, 49f, textPaint)

        // Accent pin dot
        paint.color = primaryAccent
        canvas.drawCircle(54f, 38f, 3f, paint)

        textPaint.color = primaryDark
        textPaint.textSize = 18f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("PingPin", 68f, 50f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 10.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("|  Executive WFO Attendance Statement", 142f, 50f, textPaint)

        // Month Badge
        val monthBadgeRect = RectF(440f, 34f, 559f, 56f)
        paint.color = primaryAccent
        canvas.drawRoundRect(monthBadgeRect, 11f, 11f, paint)
        textPaint.color = Color.WHITE
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val monthBadgeText = monthName.uppercase(Locale.US)
        val textWidth = textPaint.measureText(monthBadgeText)
        canvas.drawText(monthBadgeText, 440f + (119f - textWidth) / 2f, 49f, textPaint)

        paint.color = borderSoft
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 68f, 559f, 68f, paint)

        // 2. User Profile Card & Compliance Rating Card
        val profileCardRect = RectF(36f, 78f, 335f, 174f)
        paint.color = bgSoft
        canvas.drawRoundRect(profileCardRect, 10f, 10f, paint)
        paint.color = borderSoft
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(profileCardRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("EMPLOYEE IDENTIFICATION", 50f, 94f, textPaint)

        val nameText = if (profile.fullName.isBlank()) "VERIFIED EMPLOYEE" else profile.fullName.trim().uppercase()
        textPaint.color = textDark
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(nameText, 50f, 112f, textPaint)

        val desigText = if (!profile.designation.isNullOrBlank()) profile.designation.trim() else "Team Member"
        textPaint.color = primaryAccent
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(desigText, 50f, 127f, textPaint)

        paint.color = borderSoft
        canvas.drawLine(50f, 136f, 321f, 136f, paint)

        val empIdText = if (!profile.employeeId.isNullOrBlank()) profile.employeeId.trim() else "N/A"
        val contactText = if (!profile.email.isNullOrBlank()) profile.email.trim() else if (!profile.phone.isNullOrBlank()) profile.phone.trim() else "PingPin Verified WFO"
        textPaint.color = textMuted
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Emp ID: $empIdText   •   Contact: $contactText", 50f, 151f, textPaint)
        canvas.drawText("Doc Ref: $docRefId", 50f, 164f, textPaint)

        // Compliance Rating Card
        val complianceCardRect = RectF(345f, 78f, 559f, 174f)
        paint.color = successGreenBg
        canvas.drawRoundRect(complianceCardRect, 10f, 10f, paint)
        paint.color = Color.parseColor("#BBF7D0")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(complianceCardRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = primaryAccent
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("WFO COMPLIANCE RATE", 360f, 94f, textPaint)

        textPaint.color = textDark
        textPaint.textSize = 26f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("$attendancePctStr%", 360f, 126f, textPaint)

        // Rating Badge Pill inside compliance card
        val ratingStatus = if (evaluatedCount == 0) "N/A" else if (pct >= 100.0) "EXCELLENT" else if (pct >= 75.0) "ON TRACK" else if (pct >= 50.0) "ATTENTION" else "LOW"
        val badgeBg = if (pct >= 75.0) successGreenFg else if (pct >= 50.0) warningAmberFg else softRedFg
        val statusPillRect = RectF(465f, 106f, 545f, 124f)
        paint.color = badgeBg
        canvas.drawRoundRect(statusPillRect, 9f, 9f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val statusTextWidth = textPaint.measureText(ratingStatus)
        canvas.drawText(ratingStatus, 465f + (80f - statusTextWidth) / 2f, 118f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("$totalOfficeDays of $evaluatedCount evaluated days completed", 360f, 152f, textPaint)
        canvas.drawText("Target WFO: ${wfoDays.size} days in month", 360f, 164f, textPaint)

        // 3. Stat Overview Cards (4 Columns)
        val tileWidth = (523f - 30f) / 4f
        val tileLabels = listOf("SCHEDULED WFO", "EVALUATED", "ATTENDED", "PUNCTUALITY")
        val tileValues = listOf(
            "${wfoDays.size} Days",
            "$evaluatedCount Days",
            "$totalOfficeDays Days",
            "${String.format(Locale.US, "%.0f%%", punctualityPct)}"
        )
        val tileColors = listOf(primaryDark, primaryAccent, successGreen, warningAmber)

        for (i in 0..3) {
            val left = 36f + i * (tileWidth + 10f)
            val tileRect = RectF(left, 184f, left + tileWidth, 236f)

            paint.color = bgSoft
            canvas.drawRoundRect(tileRect, 8f, 8f, paint)
            paint.color = borderSoft
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(tileRect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            textPaint.color = textMuted
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(tileLabels[i], left + 8f, 198f, textPaint)

            paint.color = tileColors[i]
            canvas.drawRoundRect(RectF(left + 8f, 206f, left + 11f, 224f), 2f, 2f, paint)

            textPaint.color = textDark
            textPaint.textSize = 14f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(tileValues[i], left + 16f, 222f, textPaint)
        }

        // 4. Monthly Insights & Weekly Breakdown Box
        val insightsBoxRect = RectF(36f, 246f, 559f, 312f)
        paint.color = bgSoft
        canvas.drawRoundRect(insightsBoxRect, 8f, 8f, paint)
        paint.color = borderSoft
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(insightsBoxRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = primaryAccent
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("MONTHLY INSIGHTS & ATTENDANCE PATTERNS", 48f, 260f, textPaint)

        // Left Column Stats
        textPaint.color = textDark
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("• Avg Arrival Time: $avgCheckInTimeStr", 48f, 276f, textPaint)
        canvas.drawText("• Punctuality: $onTimeCount On-time, $lateCount Late arrivals", 48f, 290f, textPaint)
        val primaryWifi = officeConfig?.ssid?.takeIf { it.isNotBlank() } ?: "Office Wi-Fi / Geofence"
        canvas.drawText("• Verification: ${String.format(Locale.US, "%.0f%%", autoPunchPct)} via $primaryWifi", 48f, 304f, textPaint)

        // Right Column Stats (Weekly Breakdown)
        textPaint.color = textDark
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Weekly WFO Distribution:", 310f, 276f, textPaint)

        val w1Str = "W1: ${weeklyAtt[0]}/${weeklyTot[0]}"
        val w2Str = "W2: ${weeklyAtt[1]}/${weeklyTot[1]}"
        val w3Str = "W3: ${weeklyAtt[2]}/${weeklyTot[2]}"
        val w4Str = "W4: ${weeklyAtt[3]}/${weeklyTot[3]}"
        val w5Str = if (weeklyTot[4] > 0) "W5: ${weeklyAtt[4]}/${weeklyTot[4]}" else ""

        textPaint.color = textMuted
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("$w1Str   |   $w2Str   |   $w3Str", 310f, 290f, textPaint)
        canvas.drawText("$w4Str${if (w5Str.isNotEmpty()) "   |   $w5Str" else ""}", 310f, 304f, textPaint)

        // 5. Section Header for Detailed Attendance Log
        textPaint.color = textDark
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Detailed Attendance & Punch Log", 36f, 332f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Chronological record of scheduled WFO days and check-in verifications", 230f, 332f, textPaint)

        // 6. Draw Table Header
        var startY = 342f
        drawTableHeader(canvas, startY)
        startY += 22f

        val sdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val sdfDay = SimpleDateFormat("EEEE", Locale.US)
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.US)
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        var rowIndex = 0
        for (cal in wfoDays) {
            // If nearing bottom of page 1, split to Page 2
            if (startY + 20f > 720f && pageNum == 1) {
                drawFooter(canvas, pageNum)
                document.finishPage(page)

                pageNum = 2
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                drawRunningHeader(canvas)
                startY = 68f
                drawTableHeader(canvas, startY)
                startY += 22f
            }

            val isoDate = sdfIso.format(cal.time)
            val record = recordsMap[isoDate]
            val isPresent = record != null
            val isFuture = cal.after(todayCal)
            val isLate = record?.status == "late"

            // Alternating Row Background
            if (rowIndex % 2 == 1) {
                paint.color = bgSoft
                canvas.drawRect(RectF(36f, startY, 559f, startY + 20f), paint)
            }

            // Bottom Border
            paint.color = borderSoft
            paint.style = Paint.Style.STROKE
            canvas.drawRect(RectF(36f, startY, 559f, startY + 20f), paint)
            paint.style = Paint.Style.FILL

            // Column 1: Date
            textPaint.color = textDark
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(sdfDate.format(cal.time), 46f, startY + 14f, textPaint)

            // Column 2: Day
            textPaint.color = textMuted
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sdfDay.format(cal.time), 135f, startY + 14f, textPaint)

            // Column 3: Check-in Time
            val timeMarkedText = if (record != null) {
                val tStr = sdfTime.format(Date(record.markedAt))
                if (isLate) "$tStr (Late)" else tStr
            } else "--:--"

            textPaint.color = if (isPresent) (if (isLate) warningAmberFg else successGreenFg) else textMuted
            textPaint.typeface = if (isPresent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(timeMarkedText, 205f, startY + 14f, textPaint)

            // Column 4: Verification Source
            val sourceText = if (record != null) {
                if (!record.ssidSnapshot.isNullOrBlank()) "Wi-Fi: ${record.ssidSnapshot}"
                else if (record.distanceMeters != null) "Geofence (${record.distanceMeters.toInt()}m)"
                else "Manual Check-in"
            } else "--"

            textPaint.color = textMuted
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sourceText, 315f, startY + 14f, textPaint)

            // Column 5: Status Badge Pill
            val statusStr = if (isPresent) (if (isLate) "LATE" else "PRESENT") else if (isFuture) "UPCOMING" else "ABSENT"
            val badgeBgColor = if (isPresent) (if (isLate) warningAmberBg else successGreenBg) else if (isFuture) upcomingBg else softRedBg
            val badgeTextColor = if (isPresent) (if (isLate) warningAmberFg else successGreenFg) else if (isFuture) upcomingFg else softRedFg

            val bRect = RectF(455f, startY + 3f, 535f, startY + 17f)
            paint.color = badgeBgColor
            canvas.drawRoundRect(bRect, 4f, 4f, paint)

            textPaint.color = badgeTextColor
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            val stWidth = textPaint.measureText(statusStr)
            canvas.drawText(statusStr, 455f + (80f - stWidth) / 2f, startY + 13f, textPaint)

            startY += 20f
            rowIndex++
        }

        // 7. Corporate Sign-off & Verification Seal Block
        if (startY + 75f > 750f && pageNum == 1) {
            drawFooter(canvas, pageNum)
            document.finishPage(page)

            pageNum = 2
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas

            drawRunningHeader(canvas)
            startY = 70f
        }

        val signOffTop = startY + 12f
        paint.color = borderSoft
        paint.strokeWidth = 1f
        canvas.drawLine(36f, signOffTop, 559f, signOffTop, paint)

        // Employee Signature Box
        textPaint.color = textMuted
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("___________________________________________", 50f, signOffTop + 30f, textPaint)
        textPaint.color = textDark
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Employee Signature & Date", 50f, signOffTop + 42f, textPaint)
        textPaint.color = textMuted
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(nameText, 50f, signOffTop + 54f, textPaint)

        // Manager / HR Signature Box
        textPaint.color = textMuted
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("___________________________________________", 340f, signOffTop + 30f, textPaint)
        textPaint.color = textDark
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Manager / HR Approver & Date", 340f, signOffTop + 42f, textPaint)
        textPaint.color = textMuted
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Verified WFO Attendance Log", 340f, signOffTop + 54f, textPaint)

        // Draw Footer on final page
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

