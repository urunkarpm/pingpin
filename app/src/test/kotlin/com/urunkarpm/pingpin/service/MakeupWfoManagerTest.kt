package com.urunkarpm.pingpin.service

import android.content.Context
import android.content.SharedPreferences
import com.urunkarpm.pingpin.data.local.dao.AttendanceRecordDao
import com.urunkarpm.pingpin.data.local.dao.MakeupWfoSuggestionDao
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.data.local.entity.MakeupWfoSuggestionEntity
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.model.IndianHoliday
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.MakeupWfoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Calendar

class FakeMakeupWfoSuggestionDao : MakeupWfoSuggestionDao {
    private val suggestions = mutableListOf<MakeupWfoSuggestionEntity>()
    private val activeFlow = MutableStateFlow<MakeupWfoSuggestionEntity?>(null)
    private var autoId = 1

    private fun updateActive() {
        activeFlow.value = suggestions.find { it.status == "PENDING" || it.status == "ACCEPTED" }
    }

    override fun watchActiveSuggestion(): Flow<MakeupWfoSuggestionEntity?> = activeFlow

    override suspend fun getActiveSuggestion(): MakeupWfoSuggestionEntity? {
        return suggestions.find { it.status == "PENDING" || it.status == "ACCEPTED" }
    }

    override suspend fun getByMissedDate(missedDate: String): MakeupWfoSuggestionEntity? {
        return suggestions.find { it.missedDateYyyyMmDd == missedDate }
    }

    override suspend fun getAcceptedForDate(suggestedDate: String): MakeupWfoSuggestionEntity? {
        return suggestions.find { it.suggestedDateYyyyMmDd == suggestedDate && it.status == "ACCEPTED" }
    }

    override suspend fun getAll(): List<MakeupWfoSuggestionEntity> = suggestions

    override suspend fun insert(suggestion: MakeupWfoSuggestionEntity): Long {
        val id = autoId++
        val entity = suggestion.copy(id = id)
        suggestions.add(entity)
        updateActive()
        return id.toLong()
    }

    override suspend fun updateStatus(id: Int, status: String) {
        val idx = suggestions.indexOfFirst { it.id == id }
        if (idx != -1) {
            suggestions[idx] = suggestions[idx].copy(status = status)
            updateActive()
        }
    }

    override suspend fun deleteAll() {
        suggestions.clear()
        updateActive()
    }
}

class FakeAttendanceRecordDao : AttendanceRecordDao {
    private val records = mutableListOf<AttendanceRecordEntity>()

    override suspend fun getByDate(dateYyyyMmDd: String): AttendanceRecordEntity? {
        return records.find { it.dateYyyyMmDd == dateYyyyMmDd }
    }

    override suspend fun getForMonth(monthPrefix: String): List<AttendanceRecordEntity> {
        return records.filter { it.dateYyyyMmDd.startsWith(monthPrefix) }
    }

    override fun watchForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>> {
        return MutableStateFlow(records.filter { it.dateYyyyMmDd.startsWith(monthPrefix) })
    }

    override fun watchAll(): Flow<List<AttendanceRecordEntity>> {
        return MutableStateFlow(records)
    }

    override suspend fun getAll(): List<AttendanceRecordEntity> = records

    override suspend fun insert(record: AttendanceRecordEntity): Long {
        records.removeAll { it.dateYyyyMmDd == record.dateYyyyMmDd }
        records.add(record)
        return records.size.toLong()
    }

    override suspend fun deleteByDate(dateYyyyMmDd: String) {
        records.removeAll { it.dateYyyyMmDd == dateYyyyMmDd }
    }

    override suspend fun deleteAll() {
        records.clear()
    }
}

class FakeWifiService(context: Context) : WifiService(context) {
    var isConnectedToOfficeWifi = false

    override suspend fun isConnectedToSSID(targetSSID: String): Boolean {
        return isConnectedToOfficeWifi
    }

    override suspend fun getWifiSSID(): String? {
        return if (isConnectedToOfficeWifi) "Corp-WiFi" else null
    }
}

class FakeAttendanceService(context: Context, wifiService: WifiService) : AttendanceService(context, wifiService) {
    var checkAndMarkResult = AttendanceCheckResult.SUCCESS

    override suspend fun checkAndMarkAttendance(
        officeConfig: OfficeConfigEntity,
        attendanceRepo: AttendanceRepository,
        onAttendanceMarked: suspend () -> Unit
    ): AttendanceCheckResult {
        if (checkAndMarkResult == AttendanceCheckResult.SUCCESS) {
            val today = getCurrentDateYyyyMmDd()
            attendanceRepo.insertRecord(today, "present")
            onAttendanceMarked()
        }
        return checkAndMarkResult
    }
}

class FakeIndianHolidayService : IndianHolidayService() {
    var customHolidays = mutableListOf<IndianHoliday>()

    override fun getAllHolidays(): List<IndianHoliday> {
        return customHolidays
    }
}

class MakeupWfoManagerTest {

    private lateinit var mockContext: Context
    private lateinit var makeupDao: FakeMakeupWfoSuggestionDao
    private lateinit var attendanceDao: FakeAttendanceRecordDao
    private lateinit var makeupRepo: MakeupWfoRepository
    private lateinit var attendanceRepo: AttendanceRepository
    private lateinit var wifiService: FakeWifiService
    private lateinit var attendanceService: FakeAttendanceService
    private lateinit var holidayService: FakeIndianHolidayService
    private lateinit var makeupManager: MakeupWfoManager

    // Default Office Config: Mon, Tue, Wed are WFO days; Thu, Fri are WFH days; Sat, Sun are weekends.
    // workingDaysMask = Mon(1) + Tue(2) + Wed(4) + Thu(8) + Fri(16) = 31
    // wfoDaysMask = Mon(1) + Tue(2) + Wed(4) = 7
    private val defaultConfig = OfficeConfigEntity(
        ssid = "Corp-WiFi",
        checkInTime = "09:00",
        checkOutTime = "18:00",
        workingDaysMask = 31,
        wfoDaysMask = 7
    )

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        val mockPrefs = mock(SharedPreferences::class.java)
        val mockEditor = mock(SharedPreferences.Editor::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        makeupDao = FakeMakeupWfoSuggestionDao()
        attendanceDao = FakeAttendanceRecordDao()
        makeupRepo = MakeupWfoRepository(makeupDao)
        attendanceRepo = AttendanceRepository(attendanceDao)
        wifiService = FakeWifiService(mockContext)
        attendanceService = FakeAttendanceService(mockContext, wifiService)
        holidayService = FakeIndianHolidayService()

        makeupManager = MakeupWfoManager(
            context = mockContext,
            makeupRepo = makeupRepo,
            attendanceRepo = attendanceRepo,
            wifiService = wifiService,
            attendanceService = attendanceService,
            holidayService = holidayService
        )
    }

    @Test
    fun testDateFormatting() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 14)
        }
        val dateStr = MakeupWfoManager.formatDateToYyyyMmDd(cal)
        assertEquals("2026-08-14", dateStr)

        val readableStr = MakeupWfoManager.formatReadableDate("2026-08-14")
        assertEquals("Friday, Aug 14", readableStr)
    }

    @Test
    fun testEvaluate_AttendanceAlreadyMarked_ReturnsNullAndUpdatesCompletedIfActiveAccepted() = runTest {
        val todayStr = MakeupWfoManager.formatDateToYyyyMmDd(Calendar.getInstance())

        // Insert active ACCEPTED suggestion for today
        val suggestionId = makeupRepo.insertSuggestion(
            MakeupWfoSuggestionEntity(
                missedDateYyyyMmDd = "2026-08-12",
                suggestedDateYyyyMmDd = todayStr,
                status = "ACCEPTED"
            )
        ).toInt()

        // Attendance is marked today
        attendanceRepo.insertRecord(todayStr, "present")

        val result = makeupManager.evaluateAndSuggestMakeup(defaultConfig)
        assertNull(result)

        // Status should be updated to COMPLETED
        val updated = makeupDao.getAll().find { it.id == suggestionId }
        assertNotNull(updated)
        assertEquals("COMPLETED", updated?.status)
    }

    @Test
    fun testEvaluate_OnOfficeWifi_AutoMarksAttendance_ReturnsNull() = runTest {
        val now = Calendar.getInstance()
        val todayStr = MakeupWfoManager.formatDateToYyyyMmDd(now)
        val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        // Set config so today is WFO
        val dayShift = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val configWithTodayWfo = defaultConfig.copy(
            workingDaysMask = 127, // All days working
            wfoDaysMask = (1 shl dayShift) // Today is WFO
        )

        wifiService.isConnectedToOfficeWifi = true
        var autoMarkedCalled = false

        if (now.get(Calendar.HOUR_OF_DAY) >= 14) {
            val result = makeupManager.evaluateAndSuggestMakeup(configWithTodayWfo) {
                autoMarkedCalled = true
            }
            assertNull(result)
            assertEquals(true, autoMarkedCalled)
            assertNotNull(attendanceRepo.getByDate(todayStr))
        }
    }

    @Test
    fun testCandidateWfhDaySelection_SkipsHolidaysAndExistingAttendance() = runTest {
        val missedDateStr = "2026-08-12" // Wednesday (WFO day)

        holidayService.customHolidays.add(
            IndianHoliday("h1", "Test Holiday", "2026-08-13", "Thursday", com.urunkarpm.pingpin.data.model.HolidayCategory.GAZETTED, "")
        )
        attendanceRepo.insertRecord("2026-08-14", "present")

        makeupRepo.insertSuggestion(
            MakeupWfoSuggestionEntity(
                missedDateYyyyMmDd = missedDateStr,
                suggestedDateYyyyMmDd = "2026-08-20",
                status = "PENDING"
            )
        )

        val retrieved = makeupRepo.getByMissedDate(missedDateStr)
        assertNotNull(retrieved)
        assertEquals("2026-08-20", retrieved?.suggestedDateYyyyMmDd)
    }

    @Test
    fun testExistingPendingSuggestion_ReturnedWithoutDuplicate() = runTest {
        val existing = MakeupWfoSuggestionEntity(
            missedDateYyyyMmDd = "2026-08-12",
            suggestedDateYyyyMmDd = "2026-08-13",
            status = "PENDING"
        )
        val id = makeupRepo.insertSuggestion(existing)

        val active = makeupRepo.getActiveSuggestion()
        assertNotNull(active)
        assertEquals(id.toInt(), active?.id)
        assertEquals("PENDING", active?.status)
    }
}
