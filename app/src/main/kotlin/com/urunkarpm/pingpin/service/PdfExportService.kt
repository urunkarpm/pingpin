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
        val pageWidth = 595  // A4 Standard Width
        val pageHeight = 842 // A4 Standard Height

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Executive Corporate Palette
        val primaryDark = Color.parseColor("#0F172A")    // Slate 900
        val secondaryDark = Color.parseColor("#1E293B")  // Slate 800
        val textDark = Color.parseColor("#0F172A")       // Authoritative text
        val textMuted = Color.parseColor("#475569")      // Slate 600
        val textSubtle = Color.parseColor("#94A3B8")     // Slate 400
        val bgSoft = Color.parseColor("#F8FAFC")         // Slate 50
        val borderSoft = Color.parseColor("#E2E8F0")     // Slate 200
        val borderStrong = Color.parseColor("#CBD5E1")   // Slate 300
        val accentBlue = Color.parseColor("#2563EB")     // Royal Blue Accent

        // Status Colors
        val successGreenFg = Color.parseColor("#166534") // Emerald 800
        val successGreenBg = Color.parseColor("#DCFCE7") // Emerald 100
        val successGreenBorder = Color.parseColor("#86EFAC")

        val warningAmberFg = Color.parseColor("#92400E") // Amber 800

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

        // Auto Check-In Stats
        val wifiCheckIns = records.count { !it.ssidSnapshot.isNullOrBlank() }
        val autoPunchPct = if (records.isNotEmpty()) (wifiCheckIns.toDouble() / records.size * 100) else 0.0

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)
        val generatedTimestamp = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US).format(Date())
        val nameHash = Math.abs((profile.fullName + year + month + records.size).hashCode()).toString(16).uppercase(Locale.US).padStart(6, '0')
        val docRefId = "PP-${year}${String.format(Locale.US, "%02d", month)}-$nameHash"

        // Smart Human Insights Narrative Callout
        val humanInsightText = when {
            evaluatedCount == 0 ->
                "Welcome! Your monthly hybrid attendance tracking has started. PingPin will log and verify your office check-ins here."
            extraWfoCount > 0 && pct >= 100.0 ->
                "Outstanding commitment! You've exceeded your monthly WFO target with $extraWfoCount extra office ${if (extraWfoCount == 1) "day" else "days"} logged. Excellent effort!"
            pct >= 100.0 ->
                "Flawless compliance! You achieved 100% WFO attendance for $monthName. Great work keeping up with your office targets."
            pct >= 75.0 ->
                "Solid progress! You achieved ${String.format(Locale.US, "%.0f%%", pct)} attendance compliance this month. You're well on track with your hybrid schedule."
            pct >= 50.0 ->
                "Fair progress ($attendancePctStr% compliance). A quick makeup day will help get your monthly office target right back on track."
            else ->
                "Notice: Monthly attendance compliance is currently at $attendancePctStr%. We recommend scheduling makeup WFO days to stay aligned with office mandates."
        }

        val totalPages = if (reportDays.size > 16) 2 else 1

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        fun drawRunningHeader(c: Canvas) {
            paint.color = primaryDark
            c.drawRect(RectF(36f, 20f, 559f, 48f), paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText("PINGPIN  •  ATTENDANCE LOG (CONTINUED)", 48f, 38f, textPaint)

            val rMonth = monthName.uppercase(Locale.US)
            val rWidth = textPaint.measureText(rMonth)
            textPaint.textSize = 10f
            c.drawText(rMonth, 547f - rWidth, 37.5f, textPaint)
        }

        fun drawTableHeader(c: Canvas, startY: Float) {
            paint.color = secondaryDark
            c.drawRect(RectF(36f, startY, 559f, startY + 22f), paint)

            textPaint.color = Color.WHITE
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            c.drawText("DATE", 46f, startY + 14.5f, textPaint)
            c.drawText("DAY", 130f, startY + 14.5f, textPaint)
            c.drawText("CHECK-IN TIME", 210f, startY + 14.5f, textPaint)
            c.drawText("VERIFICATION SOURCE", 315f, startY + 14.5f, textPaint)
            c.drawText("STATUS", 480f, startY + 14.5f, textPaint)
        }

        // ================= PAGE 1 RENDER =================
        // 1. Sleek Header Banner
        paint.color = primaryDark
        canvas.drawRoundRect(RectF(36f, 24f, 559f, 74f), 6f, 6f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 16f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("PINGPIN", 50f, 46f, textPaint)

        textPaint.color = Color.parseColor("#93C5FD") // Soft Cyan Blue
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("EXECUTIVE ATTENDANCE & HYBRID WORK ANALYTICS STATEMENT", 50f, 62f, textPaint)

        // Month Badge Top Right
        val periodText = monthName.uppercase(Locale.US)
        textPaint.color = Color.WHITE
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val periodWidth = textPaint.measureText(periodText)
        canvas.drawText(periodText, 545f - periodWidth, 45f, textPaint)

        textPaint.color = Color.parseColor("#CBD5E1")
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        val refText = "Ref: $docRefId"
        val refWidth = textPaint.measureText(refText)
        canvas.drawText(refText, 545f - refWidth, 61f, textPaint)

        // 2. Executive Details & Smart Human Insights Card
        val summaryY = 82f
        val summaryHeight = 84f

        // Left Box: Employee Info
        val empRect = RectF(36f, summaryY, 260f, summaryY + summaryHeight)
        paint.color = bgSoft
        canvas.drawRoundRect(empRect, 6f, 6f, paint)
        paint.color = borderSoft
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(empRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = accentBlue
        textPaint.textSize = 7f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("EMPLOYEE & WORKPLACE PROFILE", 46f, summaryY + 15f, textPaint)

        val nameText = if (profile.fullName.isBlank()) "VERIFIED EMPLOYEE" else profile.fullName.trim().uppercase(Locale.US)
        textPaint.color = textDark
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(nameText, 46f, summaryY + 31f, textPaint)

        val desigText = if (!profile.designation.isNullOrBlank()) profile.designation.trim() else "Team Member"
        textPaint.color = textMuted
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(desigText, 46f, summaryY + 44f, textPaint)

        val empIdText = if (!profile.employeeId.isNullOrBlank()) profile.employeeId.trim() else "N/A"
        textPaint.textSize = 7.5f
        textPaint.color = textSubtle
        canvas.drawText("ID: $empIdText   •   $generatedTimestamp", 46f, summaryY + 58f, textPaint)

        val officeName = officeConfig?.ssid?.takeIf { it.isNotBlank() }?.let { "Wi-Fi: $it" } ?: "Main Corporate Office"
        canvas.drawText("Location: $officeName", 46f, summaryY + 71f, textPaint)

        // Right Box: Human Insights & Monthly Digest
        val insightRect = RectF(268f, summaryY, 559f, summaryY + summaryHeight)
        paint.color = Color.parseColor("#F0F9FF") // Light Blue Glow
        canvas.drawRoundRect(insightRect, 6f, 6f, paint)
        paint.color = Color.parseColor("#BAE6FD")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(insightRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = Color.parseColor("#0369A1") // Sky Blue 700
        textPaint.textSize = 7f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("💡 EXECUTIVE SUMMARY & HUMAN INSIGHTS", 278f, summaryY + 15f, textPaint)

        // Multi-line wrap for Human Insight text
        textPaint.color = textDark
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT

        fun drawWrappedText(text: String, x: Float, startY: Float, maxWidth: Float, lineSpacing: Float) {
            val words = text.split(" ")
            var line = ""
            var currentY = startY
            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                if (textPaint.measureText(testLine) <= maxWidth) {
                    line = testLine
                } else {
                    canvas.drawText(line, x, currentY, textPaint)
                    line = word
                    currentY += lineSpacing
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, x, currentY, textPaint)
            }
        }

        drawWrappedText(humanInsightText, 278f, summaryY + 31f, 270f, 11f)

        // 3. 4 Executive KPI Stat Cards (Grid Layout)
        val kpiY = 174f
        val kpiWidth = 124f
        val kpiHeight = 44f
        val kpiGap = 8f

        val kpiItems = listOf(
            Triple("ATTENDANCE RATE", "$attendancePctStr%", if (pct >= 75.0) successGreenFg else warningAmberFg),
            Triple("DAYS ATTENDED", "$totalOfficeDays / ${wfoDays.size}", primaryDark),
            Triple("POLICY COMPLIANCE", if (pct >= 75.0) "100%" else "$attendancePctStr%", successGreenFg),
            Triple("AUTO-VERIFIED", String.format(Locale.US, "%.0f%%", autoPunchPct), accentBlue)
        )

        for (i in kpiItems.indices) {
            val item = kpiItems[i]
            val left = 36f + i * (kpiWidth + kpiGap)
            val rect = RectF(left, kpiY, left + kpiWidth, kpiY + kpiHeight)

            paint.color = bgSoft
            canvas.drawRoundRect(rect, 4f, 4f, paint)

            paint.color = borderSoft
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(rect, 4f, 4f, paint)
            paint.style = Paint.Style.FILL

            // Top accent bar
            paint.color = item.third
            canvas.drawRect(RectF(left, kpiY, left + kpiWidth, kpiY + 3f), paint)

            textPaint.color = textMuted
            textPaint.textSize = 6.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(item.first, left + 8f, kpiY + 16f, textPaint)

            textPaint.color = item.third
            textPaint.textSize = 13f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(item.second, left + 8f, kpiY + 35f, textPaint)
        }

        // 4. Section Title for Detailed Log Table
        textPaint.color = primaryDark
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("DETAILED ATTENDANCE AUDIT LOG", 36f, 234f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Chronological record of office check-ins & verification telemetry", 215f, 234f, textPaint)

        // 5. Render Table Header
        var startY = 242f
        drawTableHeader(canvas, startY)
        startY += 22f

        val sdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val sdfDay = SimpleDateFormat("EEEE", Locale.US)
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.US)
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val rowHeight = 22f
        var rowIndex = 0

        for (cal in reportDays) {
            // Split to Page 2 at row 17 for multi-page logs
            if (rowIndex == 17 && totalPages > 1) {
                document.finishPage(page)

                pageNum = 2
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                drawRunningHeader(canvas)
                startY = 56f
                drawTableHeader(canvas, startY)
                startY += 22f
            }

            val isoDate = sdfIso.format(cal.time)
            val record = recordsMap[isoDate]
            val isPresent = record != null
            val isWfo = WorkingDays.isWorkingDay(cal, workingDaysMask) && WorkingDays.isWfoDay(cal, wfoDaysMask)
            val isExtraWfo = isPresent && !isWfo
            val isFuture = cal.after(todayCal)

            // Alternating Row Background
            if (rowIndex % 2 == 1) {
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRect(RectF(36f, startY, 559f, startY + rowHeight), paint)
            }

            // Divider Line
            paint.color = borderSoft
            paint.strokeWidth = 0.6f
            canvas.drawLine(36f, startY + rowHeight, 559f, startY + rowHeight, paint)

            // Col 1: Date
            textPaint.color = textDark
            textPaint.textSize = 8.2f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(sdfDate.format(cal.time), 46f, startY + 14.5f, textPaint)

            // Col 2: Day
            textPaint.color = textMuted
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sdfDay.format(cal.time), 130f, startY + 14.5f, textPaint)

            // Col 3: Check-in Time
            val timeMarkedText = if (record != null) {
                sdfTime.format(Date(record.markedAt))
            } else "—"

            textPaint.color = if (isPresent) (if (isExtraWfo) extraBlueFg else successGreenFg) else textSubtle
            textPaint.typeface = if (isPresent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textPaint.textSize = 8.2f
            canvas.drawText(timeMarkedText, 210f, startY + 14.5f, textPaint)

            // Col 4: Verification Source
            val sourceText = if (record != null) {
                if (!record.ssidSnapshot.isNullOrBlank()) "Office Wi-Fi (${record.ssidSnapshot})"
                else if (record.distanceMeters != null) "Geofence Check-in (${record.distanceMeters.toInt()}m)"
                else "Manual Verification"
            } else "—"

            textPaint.color = textMuted
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sourceText, 315f, startY + 14.5f, textPaint)

            // Col 5: Status Badge Pill
            val statusStr = when {
                isExtraWfo -> "EXTRA WFO"
                isPresent -> "PRESENT"
                isFuture -> "UPCOMING"
                else -> "ABSENT"
            }

            val badgeBgColor = when {
                isExtraWfo -> extraBlueBg
                isPresent -> successGreenBg
                isFuture -> upcomingBg
                else -> softRedBg
            }

            val badgeBorderColor = when {
                isExtraWfo -> extraBlueBorder
                isPresent -> successGreenBorder
                isFuture -> upcomingBorder
                else -> softRedBorder
            }

            val badgeTextColor = when {
                isExtraWfo -> extraBlueFg
                isPresent -> successGreenFg
                isFuture -> upcomingFg
                else -> softRedFg
            }

            val bRect = RectF(465f, startY + 3.5f, 550f, startY + 18.5f)
            paint.color = badgeBgColor
            canvas.drawRoundRect(bRect, 7f, 7f, paint)

            paint.color = badgeBorderColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.8f
            canvas.drawRoundRect(bRect, 7f, 7f, paint)
            paint.style = Paint.Style.FILL

            textPaint.color = badgeTextColor
            textPaint.textSize = 6.8f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            val stWidth = textPaint.measureText(statusStr)
            canvas.drawText(statusStr, 465f + (85f - stWidth) / 2f, startY + 13.8f, textPaint)

            startY += rowHeight
            rowIndex++
        }

        // 6. Summary Totals Bar
        val totalsHeight = 22f
        paint.color = bgSoft
        canvas.drawRect(RectF(36f, startY, 559f, startY + totalsHeight), paint)

        paint.color = borderStrong
        paint.strokeWidth = 1f
        canvas.drawLine(36f, startY, 559f, startY, paint)
        canvas.drawLine(36f, startY + totalsHeight, 559f, startY + totalsHeight, paint)

        textPaint.color = secondaryDark
        textPaint.textSize = 7.5f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(
            "TOTALS:  ${wfoDays.size} Scheduled   •   $evaluatedCount Evaluated   •   $totalOfficeDays Attended   •   $attendancePctStr% Compliance",
            46f,
            startY + 14.5f,
            textPaint
        )
        startY += totalsHeight

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
