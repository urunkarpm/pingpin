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

    @Test
    fun testWorkingDaysMaskEvaluation() {
        val calendar = Calendar.getInstance()

        // Monday (bit 0 = 1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        assertTrue("Monday should be a working day under DEFAULT_WEEKDAYS", WorkingDays.isWorkingDay(calendar, WorkingDays.DEFAULT_WEEKDAYS))
        assertFalse("Monday should NOT be working when Monday bit is 0", WorkingDays.isWorkingDay(calendar, WorkingDays.DEFAULT_WEEKDAYS and WorkingDays.MONDAY.inv()))

        // Sunday (bit 6 = 64)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        assertFalse("Sunday should not be a working day under DEFAULT_WEEKDAYS", WorkingDays.isWorkingDay(calendar, WorkingDays.DEFAULT_WEEKDAYS))
        assertTrue("Sunday should be working when SUNDAY bit is added", WorkingDays.isWorkingDay(calendar, WorkingDays.DEFAULT_WEEKDAYS or WorkingDays.SUNDAY))

        // Saturday (bit 5 = 32)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        assertFalse("Saturday should not be working under default weekdays", WorkingDays.isWorkingDay(calendar, WorkingDays.DEFAULT_WEEKDAYS))
        assertTrue("Saturday should be working when SATURDAY bit is set", WorkingDays.isWorkingDay(calendar, WorkingDays.SATURDAY))
    }

    @Test
    fun testAlarmSoundServiceConstants() {
        assertEquals("com.urunkarpm.pingpin.ACTION_START_ALARM", AlarmSoundService.ACTION_START_ALARM)
        assertEquals("com.urunkarpm.pingpin.ACTION_STOP_ALARM", AlarmSoundService.ACTION_STOP_ALARM)
        assertEquals("alarmId", AlarmSoundService.EXTRA_ALARM_ID)
        assertEquals("title", AlarmSoundService.EXTRA_TITLE)
        assertEquals("portalUrl", AlarmSoundService.EXTRA_PORTAL_URL)
    }

    @Test
    fun testDirectBootPreferencesFallback() {
        val prefs = NotificationService.getAlarmPreferences(mockContext)
        assertNotNull("Preferences should not be null", prefs)
    }
}
