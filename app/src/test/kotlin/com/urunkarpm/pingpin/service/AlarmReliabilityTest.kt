package com.urunkarpm.pingpin.service

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Calendar
import java.util.Locale

class AlarmReliabilityTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        `when`(mockEditor.putInt(anyString(), anyInt())).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
    }

    // ==========================================
    // 1. Time Parsing & Validation Matrix
    // ==========================================

    @Test
    fun testParseTimeValidFormats() {
        assertEquals(Pair(9, 30), NotificationService.parseTime("09:30"))
        assertEquals(Pair(9, 30), NotificationService.parseTime("9:30"))
        assertEquals(Pair(0, 0), NotificationService.parseTime("00:00"))
        assertEquals(Pair(0, 0), NotificationService.parseTime("0:0"))
        assertEquals(Pair(23, 59), NotificationService.parseTime("23:59"))
        assertEquals(Pair(18, 5), NotificationService.parseTime("18:05"))
        assertEquals(Pair(12, 0), NotificationService.parseTime("  12:00  "))
    }

    @Test
    fun testParseTimeInvalidFormats() {
        assertNull("Hour > 23 should be invalid", NotificationService.parseTime("24:00"))
        assertNull("Hour < 0 should be invalid", NotificationService.parseTime("-1:00"))
        assertNull("Minute > 59 should be invalid", NotificationService.parseTime("12:60"))
        assertNull("Minute < 0 should be invalid", NotificationService.parseTime("12:-1"))
        assertNull("Non-numeric strings should return null", NotificationService.parseTime("invalid"))
        assertNull("Empty string should return null", NotificationService.parseTime(""))
        assertNull("Missing colon should return null", NotificationService.parseTime("0930"))
    }

    // ==========================================
    // 2. Deterministic Next Occurrence Scheduling
    // ==========================================

    @Test
    fun testMondayMorningBeforeCheckInTime() {
        // Given base time is Monday at 08:00 AM (Check-in is 09:30 AM)
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 8, 0, 0) // Mon Sep 7, 2026 08:00
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(Calendar.MONDAY, baseCal.get(Calendar.DAY_OF_WEEK))

        val nextCal = NotificationService.getNextOccurrence(
            hour = 9,
            minute = 30,
            workingDaysMask = WorkingDays.DEFAULT_WEEKDAYS, // Mon-Fri
            baseTimeMillis = baseCal.timeInMillis
        )

        assertEquals("Should schedule for SAME day (Monday)", Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCal.get(Calendar.MINUTE))
        assertEquals(baseCal.get(Calendar.DAY_OF_YEAR), nextCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testMondayExactTriggerTimeMovesToTuesday() {
        // Given base time is Monday at 09:30:00.000 (when alarm fires)
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 9, 30, 0) // Mon Sep 7, 2026 09:30
            set(Calendar.MILLISECOND, 0)
        }

        val nextCal = NotificationService.getNextOccurrence(
            hour = 9,
            minute = 30,
            workingDaysMask = WorkingDays.DEFAULT_WEEKDAYS,
            baseTimeMillis = baseCal.timeInMillis
        )

        assertEquals("Should advance to Tuesday", Calendar.TUESDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun testFridayCheckInSkipsWeekendToMonday() {
        // Given base time is Friday at 09:30 AM (Check-in trigger)
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 11, 9, 30, 0) // Fri Sep 11, 2026 09:30
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(Calendar.FRIDAY, baseCal.get(Calendar.DAY_OF_WEEK))

        val nextCal = NotificationService.getNextOccurrence(
            hour = 9,
            minute = 30,
            workingDaysMask = WorkingDays.DEFAULT_WEEKDAYS, // Mon-Fri (31)
            baseTimeMillis = baseCal.timeInMillis
        )

        assertEquals("Next check-in after Friday 09:30 MUST land on Monday", Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCal.get(Calendar.MINUTE))
        assertEquals(baseCal.get(Calendar.DAY_OF_YEAR) + 3, nextCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun testFridayCheckOutSkipsWeekendToMonday() {
        // Given base time is Friday at 18:00 PM (Check-out trigger)
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 11, 18, 0, 0) // Fri Sep 11, 2026 18:00
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(Calendar.FRIDAY, baseCal.get(Calendar.DAY_OF_WEEK))

        val nextCal = NotificationService.getNextOccurrence(
            hour = 18,
            minute = 0,
            workingDaysMask = WorkingDays.DEFAULT_WEEKDAYS, // Mon-Fri
            baseTimeMillis = baseCal.timeInMillis
        )

        assertEquals("Next check-out after Friday 18:00 MUST land on Monday 18:00", Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(18, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun testSaturdayRescheduleLandsOnMonday() {
        // Given base time is Saturday at 14:00 PM (e.g. app opened or settings edited on weekend)
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 12, 14, 0, 0) // Sat Sep 12, 2026
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(Calendar.SATURDAY, baseCal.get(Calendar.DAY_OF_WEEK))

        val nextCal = NotificationService.getNextOccurrence(
            hour = 9,
            minute = 30,
            workingDaysMask = WorkingDays.DEFAULT_WEEKDAYS,
            baseTimeMillis = baseCal.timeInMillis
        )

        assertEquals("From Saturday, next alarm MUST land on Monday", Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun testSundayMidnightDateChangeLandsOnMonday() {
        // Given base time is Sunday at 00:00:01 AM (ACTION_DATE_CHANGED triggered on Sunday)
        val sundayMidnight = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 13, 0, 0, 1) // Sun Sep 13, 2026 00:00:01
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(Calendar.SUNDAY, sundayMidnight.get(Calendar.DAY_OF_WEEK))

        val nextCal = NotificationService.getNextOccurrence(
            hour = 9,
            minute = 30,
            workingDaysMask = WorkingDays.DEFAULT_WEEKDAYS,
            baseTimeMillis = sundayMidnight.timeInMillis
        )

        assertEquals("Sunday 00:00 must schedule for Monday 09:30", Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCal.get(Calendar.MINUTE))
    }

    @Test
    fun testSundayEveningBeforeMondayLandsOnMonday() {
        // Given base time is Sunday at 23:55 PM
        val sundayNight = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 13, 23, 55, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val nextCal = NotificationService.getNextOccurrence(
            hour = 9,
            minute = 30,
            workingDaysMask = WorkingDays.DEFAULT_WEEKDAYS,
            baseTimeMillis = sundayNight.timeInMillis
        )

        assertEquals("Sunday night must schedule for Monday morning", Calendar.MONDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, nextCal.get(Calendar.MINUTE))
    }

    // ==========================================
    // 3. Custom Working Day Schedules
    // ==========================================

    @Test
    fun testCustomScheduleTueThuOnly() {
        // Schedule: Tuesday and Thursday only (Tue=2 + Thu=8 = 10)
        val tueThuMask = WorkingDays.TUESDAY or WorkingDays.THURSDAY

        // Base: Monday 10:00 -> should schedule Tuesday 09:00
        val monCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0) // Monday
        }
        val nextFromMon = NotificationService.getNextOccurrence(9, 0, tueThuMask, monCal.timeInMillis)
        assertEquals(Calendar.TUESDAY, nextFromMon.get(Calendar.DAY_OF_WEEK))

        // Base: Tuesday 09:00 trigger -> should schedule Thursday 09:00
        val tueCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 8, 9, 0, 0) // Tuesday 09:00
        }
        val nextFromTue = NotificationService.getNextOccurrence(9, 0, tueThuMask, tueCal.timeInMillis)
        assertEquals(Calendar.THURSDAY, nextFromTue.get(Calendar.DAY_OF_WEEK))

        // Base: Thursday 09:00 trigger -> should schedule next Tuesday 09:00
        val thuCal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 10, 9, 0, 0) // Thursday 09:00
        }
        val nextFromThu = NotificationService.getNextOccurrence(9, 0, tueThuMask, thuCal.timeInMillis)
        assertEquals(Calendar.TUESDAY, nextFromThu.get(Calendar.DAY_OF_WEEK))
    }

    @Test
    fun testZeroWorkingDaysSafety() {
        // Safety guard: if workingDaysMask is 0, must not enter infinite loop
        val cal = Calendar.getInstance()
        val result = NotificationService.getNextOccurrence(9, 30, workingDaysMask = 0, baseTimeMillis = cal.timeInMillis)
        assertNotNull(result)
    }

    // ==========================================
    // 4. Month and Year Boundaries
    // ==========================================

    @Test
    fun testYearBoundaryDec31ToJan1() {
        // Thursday Dec 31, 2026 18:00 PM (Check-out)
        val dec31 = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 31, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals(Calendar.THURSDAY, dec31.get(Calendar.DAY_OF_WEEK))

        val nextCal = NotificationService.getNextOccurrence(9, 30, WorkingDays.DEFAULT_WEEKDAYS, dec31.timeInMillis)
        assertEquals("Should advance to Friday Jan 1, 2027", Calendar.FRIDAY, nextCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(2027, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, nextCal.get(Calendar.MONTH))
        assertEquals(1, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    // ==========================================
    // 5. WorkingDays Bitmask Precision
    // ==========================================

    @Test
    fun testAllSevenIndividualDays() {
        val cal = Calendar.getInstance()

        // Mon: bit 0 (value 1)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        assertTrue(WorkingDays.isWorkingDay(cal, WorkingDays.MONDAY))
        assertFalse(WorkingDays.isWorkingDay(cal, WorkingDays.TUESDAY))

        // Tue: bit 1 (value 2)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
        assertTrue(WorkingDays.isWorkingDay(cal, WorkingDays.TUESDAY))
        assertFalse(WorkingDays.isWorkingDay(cal, WorkingDays.WEDNESDAY))

        // Wed: bit 2 (value 4)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
        assertTrue(WorkingDays.isWorkingDay(cal, WorkingDays.WEDNESDAY))

        // Thu: bit 3 (value 8)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
        assertTrue(WorkingDays.isWorkingDay(cal, WorkingDays.THURSDAY))

        // Fri: bit 4 (value 16)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
        assertTrue(WorkingDays.isWorkingDay(cal, WorkingDays.FRIDAY))

        // Sat: bit 5 (value 32)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        assertTrue(WorkingDays.isWorkingDay(cal, WorkingDays.SATURDAY))

        // Sun: bit 6 (value 64)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        assertTrue(WorkingDays.isWorkingDay(cal, WorkingDays.SUNDAY))
    }

    @Test
    fun testPresetMaskValues() {
        assertEquals("Mon-Fri sum must be 31", 31, WorkingDays.DEFAULT_WEEKDAYS)
        assertEquals("Mon-Sat sum must be 63", 63, WorkingDays.DEFAULT_WEEKDAYS or WorkingDays.SATURDAY)
        assertEquals("All 7 days sum must be 127", 127, WorkingDays.DEFAULT_WEEKDAYS or WorkingDays.SATURDAY or WorkingDays.SUNDAY)
    }

    // ==========================================
    // 6. Service Constants & DirectBoot Integrity
    // ==========================================

    @Test
    fun testAlarmSoundServiceConstants() {
        assertEquals("com.urunkarpm.pingpin.ACTION_START_ALARM", AlarmSoundService.ACTION_START_ALARM)
        assertEquals("com.urunkarpm.pingpin.ACTION_STOP_ALARM", AlarmSoundService.ACTION_STOP_ALARM)
        assertEquals("alarmId", AlarmSoundService.EXTRA_ALARM_ID)
        assertEquals("title", AlarmSoundService.EXTRA_TITLE)
        assertEquals("portalUrl", AlarmSoundService.EXTRA_PORTAL_URL)
        assertEquals("actionType", AlarmSoundService.EXTRA_ACTION_TYPE)
    }

    @Test
    fun testNotificationServiceConstants() {
        assertEquals(101, NotificationService.CHECK_IN_ALARM_ID)
        assertEquals(102, NotificationService.CHECK_OUT_ALARM_ID)
        assertEquals(103, NotificationService.CHECK_IN_SNOOZE_ID)
        assertEquals(104, NotificationService.CHECK_OUT_SNOOZE_ID)
        assertEquals("alarm_channel_v4", NotificationService.ALARM_CHANNEL_ID)
    }

    @Test
    fun testDirectBootPreferencesFallback() {
        val prefs = NotificationService.getAlarmPreferences(mockContext)
        assertNotNull("Preferences should not be null", prefs)
    }
}
