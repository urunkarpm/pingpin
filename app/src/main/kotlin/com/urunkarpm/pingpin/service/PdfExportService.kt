package com.urunkarpm.pingpin.service

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.data.local.entity.WfoScheduleHistoryEntity
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
        wfoDaysMask: Int = 31
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 page size
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Palette
        val primaryDark = Color.parseColor("#1E293B")
        val primaryAccent = Color.parseColor("#0F766E")
        val textDark = Color.parseColor("#0F172A")
        val textMuted = Color.parseColor("#64748B")
        val bgSoft = Color.parseColor("#F8FAFC")
        val borderSoft = Color.parseColor("#E2E8F0")
        val accentLightBg = Color.parseColor("#F0FDF4")
        val accentLightBorder = Color.parseColor("#BBF7D0")
        val successGreen = Color.parseColor("#16A34A")
        val softRed = Color.parseColor("#DC2626")

        val installCal = AppInstallManager.getInstallDateCalendar(context)
        val installDateStr = AppInstallManager.getInstallDateYyyyMmDd(context)

        // Calculations
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val wfoDays = mutableListOf<Calendar>()
        for (day in 1..maxDays) {
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (!cal.before(installCal) && WorkingDays.isWorkingDay(cal, workingDaysMask) && WorkingDays.isWfoDay(cal, wfoDaysMask)) {
                wfoDays.add(cal)
            }
        }

        val recordsMap = records.filter { it.dateYyyyMmDd >= installDateStr }.associateBy { it.dateYyyyMmDd }
        val totalOfficeDays = recordsMap.size

        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)

        val evaluatedWfoDays = wfoDays.filter { !it.after(todayCal) && !it.before(installCal) }
        val evaluatedCount = evaluatedWfoDays.size
        val pct = if (evaluatedCount > 0) (totalOfficeDays.toDouble() / evaluatedCount * 100) else 0.0
        val attendancePctStr = String.format(Locale.US, "%.1f", pct)
        val absentDays = if (evaluatedCount > totalOfficeDays) evaluatedCount - totalOfficeDays else 0

        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(calendar.time)

        // 1. Header Bar
        paint.color = primaryDark
        canvas.drawRoundRect(RectF(36f, 36f, 60f, 60f), 6f, 6f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("P", 44f, 53f, textPaint)

        textPaint.color = primaryDark
        textPaint.textSize = 18f
        canvas.drawText("PingPin", 68f, 54f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("|  WFO Insights & Attendance Report", 135f, 54f, textPaint)

        textPaint.color = primaryAccent
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(monthName, 465f, 54f, textPaint)

        // Divider
        paint.color = borderSoft
        paint.strokeWidth = 1f
        canvas.drawLine(36f, 72f, 559f, 72f, paint)

        // 2. User Profile Card & Attendance Rate Badge
        val cardRect = RectF(36f, 84f, 340f, 180f)
        paint.color = bgSoft
        canvas.drawRoundRect(cardRect, 10f, 10f, paint)
        paint.color = borderSoft
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(cardRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = textDark
        textPaint.textSize = 15f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val nameText = if (profile.fullName.isEmpty()) "EMPLOYEE REPORT" else profile.fullName.uppercase()
        canvas.drawText(nameText, 52f, 110f, textPaint)

        textPaint.color = primaryAccent
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        val desigText = if (profile.designation.isEmpty()) "Team Member" else profile.designation
        canvas.drawText(desigText, 52f, 126f, textPaint)

        paint.color = borderSoft
        canvas.drawLine(52f, 136f, 324f, 136f, paint)

        textPaint.color = textMuted
        textPaint.textSize = 9.5f
        textPaint.typeface = Typeface.DEFAULT
        val empIdText = profile.employeeId?.let { "Emp ID: $it" } ?: "PingPin Verified WFO Log"
        canvas.drawText(empIdText, 52f, 156f, textPaint)

        // WFO Rate Badge Card
        val badgeRect = RectF(352f, 84f, 559f, 180f)
        paint.color = accentLightBg
        canvas.drawRoundRect(badgeRect, 10f, 10f, paint)
        paint.color = accentLightBorder
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(badgeRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        textPaint.color = primaryAccent
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("WFO COMPLIANCE", 390f, 106f, textPaint)

        textPaint.color = textDark
        textPaint.textSize = 28f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("$attendancePctStr%", 410f, 140f, textPaint)

        textPaint.color = textMuted
        textPaint.textSize = 8.5f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("$totalOfficeDays of $evaluatedCount evaluated WFO days", 370f, 160f, textPaint)

        // 3. Stat Overview Cards
        val statWidth = (523f - 30f) / 4f
        val statLabels = listOf("TOTAL WFO DAYS", "EVALUATED WFO", "ATTENDED WFO", "MISSED WFO")
        val statValues = listOf("${wfoDays.size}", "$evaluatedCount", "$totalOfficeDays", "$absentDays")
        val statColors = listOf(primaryDark, primaryAccent, successGreen, softRed)

        for (i in 0..3) {
            val left = 36f + i * (statWidth + 10f)
            val rect = RectF(left, 192f, left + statWidth, 246f)

            paint.color = bgSoft
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            paint.color = borderSoft
            paint.style = Paint.Style.STROKE
            canvas.drawRoundRect(rect, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            textPaint.color = textMuted
            textPaint.textSize = 7.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(statLabels[i], left + 8f, 210f, textPaint)

            paint.color = statColors[i]
            canvas.drawRoundRect(RectF(left + 8f, 218f, left + 12f, 234f), 2f, 2f, paint)

            textPaint.color = textDark
            textPaint.textSize = 16f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(statValues[i], left + 18f, 232f, textPaint)
        }

        // 4. Breakdown Table Title
        textPaint.color = textDark
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("WFO Schedule & Marked Attendance Log", 36f, 272f, textPaint)

        // Table Headers
        var startY = 290f
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(RectF(36f, startY, 559f, startY + 22f), paint)

        textPaint.color = Color.parseColor("#334155")
        textPaint.textSize = 9f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("Date", 46f, startY + 15f, textPaint)
        canvas.drawText("Day", 145f, startY + 15f, textPaint)
        canvas.drawText("Time Marked", 215f, startY + 15f, textPaint)
        canvas.drawText("Network / Source", 330f, startY + 15f, textPaint)
        canvas.drawText("Status", 460f, startY + 15f, textPaint)

        startY += 22f
        val sdfDate = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val sdfDay = SimpleDateFormat("EEE", Locale.US)
        val sdfTime = SimpleDateFormat("hh:mm a", Locale.US)
        val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (cal in wfoDays) {
            if (startY > 770f) break // Single page constraint

            val isoDate = sdfIso.format(cal.time)
            val record = recordsMap[isoDate]
            val isPresent = record != null
            val isFuture = cal.after(todayCal)

            val timeMarkedText = if (record != null) sdfTime.format(Date(record.markedAt)) else "--:--"
            val sourceText = if (record != null) {
                if (!record.ssidSnapshot.isNullOrBlank()) record.ssidSnapshot else "Manual Mark"
            } else "--"

            paint.color = borderSoft
            paint.style = Paint.Style.STROKE
            canvas.drawRect(RectF(36f, startY, 559f, startY + 20f), paint)
            paint.style = Paint.Style.FILL

            textPaint.color = textDark
            textPaint.textSize = 8.5f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(sdfDate.format(cal.time), 46f, startY + 14f, textPaint)

            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sdfDay.format(cal.time), 145f, startY + 14f, textPaint)

            textPaint.color = if (isPresent) successGreen else textMuted
            textPaint.typeface = if (isPresent) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(timeMarkedText, 215f, startY + 14f, textPaint)

            textPaint.color = textMuted
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(sourceText, 330f, startY + 14f, textPaint)

            val statusStr = if (isPresent) "PRESENT" else if (isFuture) "UPCOMING" else "ABSENT"
            val badgeBgColor = if (isPresent) Color.parseColor("#DCFCE7") else if (isFuture) Color.parseColor("#F1F5F9") else Color.parseColor("#FEE2E2")
            val badgeTextColor = if (isPresent) Color.parseColor("#15803D") else if (isFuture) Color.parseColor("#64748B") else Color.parseColor("#B91C1C")

            val bRect = RectF(450f, startY + 3f, 520f, startY + 17f)
            paint.color = badgeBgColor
            canvas.drawRoundRect(bRect, 4f, 4f, paint)

            textPaint.color = badgeTextColor
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(statusStr, 460f, startY + 13f, textPaint)

            startY += 20f
        }

        // Footer
        paint.color = borderSoft
        canvas.drawLine(36f, 800f, 559f, 800f, paint)

        textPaint.color = textMuted
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Confidential • Generated automatically by PingPin", 36f, 814f, textPaint)
        canvas.drawText("Page 1 of 1", 510f, 814f, textPaint)

        document.finishPage(page)

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val pdfFile = File(outputDir, "PingPin_Attendance_${year}_${String.format("%02d", month)}.pdf")

        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        pdfFile
    }
}
