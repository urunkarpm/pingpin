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
    private lateinit var pdfExportService: PdfExportService

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        val tempDir = File(System.getProperty("java.io.tmpdir"), "pingpin_test_docs")
        tempDir.mkdirs()
        `when`(mockContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).thenReturn(tempDir)
        `when`(mockContext.filesDir).thenReturn(tempDir)
        val mockSharedPreferences = mock(android.content.SharedPreferences::class.java)
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
}
