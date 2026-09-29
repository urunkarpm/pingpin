package com.urunkarpm.pingpin.service

import android.content.Context
import android.os.Environment
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.util.Calendar

class ExtraWfoTest {

    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: android.content.SharedPreferences
    private lateinit var pdfExportService: PdfExportService

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "pingpin_test_docs")
        tempDir.mkdirs()
        `when`(mockContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).thenReturn(tempDir)
        `when`(mockContext.filesDir).thenReturn(tempDir)
        mockSharedPreferences = mock(android.content.SharedPreferences::class.java)
        val mockEditor = mock(android.content.SharedPreferences.Editor::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)

        pdfExportService = PdfExportService(mockContext)
    }

    @Test
    fun testExtraWfoDayDetection() {
        // Mask: Mon(1), Tue(2) = 3 (WFO days)
        val wfoDaysMask = 3

        val calMon = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 7) } // Monday
        val calWed = Calendar.getInstance().apply { set(2026, Calendar.SEPTEMBER, 9) } // Wednesday (Non-WFO)

        assertTrue(WorkingDays.isWfoDay(calMon, wfoDaysMask))
        assertTrue(!WorkingDays.isWfoDay(calWed, wfoDaysMask))

        // On Wednesday (Non-WFO day), attendance marked = Extra WFO
        val isWedAttended = true
        val isWedWfo = WorkingDays.isWfoDay(calWed, wfoDaysMask)
        val isExtraWfo = isWedAttended && !isWedWfo

        assertTrue(isExtraWfo)
    }

    @Test
    fun testExtraWfoComplianceCalculation() {
        val wfoDaysMask = 3 // Mon, Tue
        val workingDaysMask = 31 // Mon-Fri

        // 2 attendance records: 1 WFO day (Monday 2026-09-07), 1 Non-WFO day (Wednesday 2026-09-09)
        val records = listOf(
            AttendanceRecordEntity(dateYyyyMmDd = "2026-09-07", status = "present", markedAt = 1757235600000L),
            AttendanceRecordEntity(dateYyyyMmDd = "2026-09-09", status = "present", markedAt = 1757408400000L)
        )

        val totalAttended = records.size
        assertEquals(2, totalAttended)

        // Count Extra WFO records (attended on non-WFO or non-working days)
        val extraWfoCount = records.count { rec ->
            val cal = Calendar.getInstance().apply {
                val parts = rec.dateYyyyMmDd.split("-")
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            !WorkingDays.isWfoDay(cal, wfoDaysMask) || !WorkingDays.isWorkingDay(cal, workingDaysMask)
        }

        assertEquals(1, extraWfoCount)

        // Standard WFO days evaluated (e.g. 1 WFO day up to Sept 7)
        val evaluatedWfoDays = 1
        val compliancePct = (totalAttended.toDouble() / evaluatedWfoDays) * 100.0

        // Attendance exceeds 100% target when extra WFO days are attended
        assertTrue(compliancePct > 100.0)
        assertEquals(200.0, compliancePct, 0.01)
    }

    @Test
    fun testInsightsAndPdfMetricsMatch() {
        val selectedYear = 2026
        val selectedMonth = 9
        val workingDaysMask = 31 // Mon-Fri
        val wfoDaysMask = 31     // Mon-Fri

        // App installed on Sept 15, 2026
        val installCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 15, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Today is Sept 25, 2026
        val todayCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 25, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val maxDays = 30 // Sept has 30 days

        val records = listOf(
            AttendanceRecordEntity(dateYyyyMmDd = "2026-09-15", status = "present", markedAt = 1757926800000L),
            AttendanceRecordEntity(dateYyyyMmDd = "2026-09-16", status = "late", markedAt = 1758013200000L),
            AttendanceRecordEntity(dateYyyyMmDd = "2026-09-20", status = "present", markedAt = 1758358800000L) // Sunday = Extra WFO
        )
        val recordsMap = records.associateBy { it.dateYyyyMmDd }

        // --- InsightsScreen Calculation ---
        var insightsWfoTotal = 0
        var insightsWfoElapsed = 0
        var insightsAttendedWfo = 0
        var insightsExtraWfo = 0

        val currentYear = todayCal.get(Calendar.YEAR)
        val currentMonth = todayCal.get(Calendar.MONTH) + 1
        val currentDay = todayCal.get(Calendar.DAY_OF_MONTH)

        val dayCal = Calendar.getInstance()
        for (day in 1..maxDays) {
            val dateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, day)
            dayCal.set(selectedYear, selectedMonth - 1, day, 0, 0, 0)
            dayCal.set(Calendar.MILLISECOND, 0)
            if (dayCal.before(installCal) && !recordsMap.containsKey(dateStr)) {
                continue
            }

            val isPastOrToday = when {
                selectedYear < currentYear -> true
                selectedYear > currentYear -> false
                selectedMonth < currentMonth -> true
                selectedMonth > currentMonth -> false
                else -> day <= currentDay
            }

            val isWork = WorkingDays.isWorkingDay(dayCal, workingDaysMask)
            val isWfo = isWork && WorkingDays.isWfoDay(dayCal, wfoDaysMask)

            if (isWfo) {
                insightsWfoTotal++
                if (isPastOrToday) insightsWfoElapsed++
                if (recordsMap.containsKey(dateStr)) insightsAttendedWfo++
            } else if (recordsMap.containsKey(dateStr)) {
                insightsExtraWfo++
            }
        }
        val insightsAttendedTotal = records.size
        val insightsCompliancePct = (insightsAttendedTotal.toDouble() / insightsWfoElapsed) * 100.0

        // --- PDF Export Calculation ---
        val pdfWfoDays = mutableListOf<Calendar>()
        for (day in 1..maxDays) {
            val cal = Calendar.getInstance()
            cal.set(selectedYear, selectedMonth - 1, day, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val isoDate = String.format(java.util.Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, day)
            val isAttended = recordsMap.containsKey(isoDate)
            val isWfo = WorkingDays.isWorkingDay(cal, workingDaysMask) && WorkingDays.isWfoDay(cal, wfoDaysMask)

            val isAfterOrOnInstall = !cal.before(installCal) || isAttended
            if (isAfterOrOnInstall && isWfo) {
                pdfWfoDays.add(cal)
            }
        }

        val pdfEvaluatedWfoDays = pdfWfoDays.filter { !it.after(todayCal) }
        val pdfEvaluatedCount = pdfEvaluatedWfoDays.size
        val pdfTotalOfficeDays = recordsMap.size
        val pdfCompliancePct = (pdfTotalOfficeDays.toDouble() / pdfEvaluatedCount) * 100.0

        // Assert: Target WFO (Scheduled) must match
        assertEquals(insightsWfoTotal, pdfWfoDays.size)

        // Assert: Evaluated WFO days must match
        assertEquals(insightsWfoElapsed, pdfEvaluatedCount)

        // Assert: Attended days must match
        assertEquals(insightsAttendedTotal, pdfTotalOfficeDays)

        // Assert: Compliance % must match
        assertEquals(insightsCompliancePct, pdfCompliancePct, 0.01)

        // Assert: Extra WFO count must match
        val pdfExtraWfo = records.count { rec ->
            val cal = Calendar.getInstance().apply {
                val parts = rec.dateYyyyMmDd.split("-")
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            !WorkingDays.isWfoDay(cal, wfoDaysMask) || !WorkingDays.isWorkingDay(cal, workingDaysMask)
        }
        assertEquals(insightsExtraWfo, pdfExtraWfo)
    }
}
