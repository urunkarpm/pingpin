package com.urunkarpm.pingpin.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class IndianHolidayServiceTest {

    private lateinit var service: IndianHolidayService

    @Before
    fun setUp() {
        service = IndianHolidayService()
    }

    @Test
    fun testGetAllHolidays_ReturnsSortedList() {
        val holidays = service.getAllHolidays()
        assertTrue(holidays.isNotEmpty())
        for (i in 0 until holidays.size - 1) {
            assertTrue(holidays[i].dateYyyyMmDd <= holidays[i + 1].dateYyyyMmDd)
        }
    }

    @Test
    fun testGetHolidaysFromPresent_FiltersPastHolidays() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 14, 0, 0, 0)
        }
        val presentHolidays = service.getHolidaysFromPresent(cal)

        assertTrue(presentHolidays.isNotEmpty())
        presentHolidays.forEach { holiday ->
            assertTrue(holiday.dateYyyyMmDd >= "2026-08-14")
        }

        // Verify August 15 (Independence Day) is included and January holidays are excluded
        val hasIndependenceDay = presentHolidays.any { it.dateYyyyMmDd == "2026-08-15" }
        val hasNewYears = presentHolidays.any { it.dateYyyyMmDd == "2026-01-01" }

        assertTrue(hasIndependenceDay)
        assertTrue(!hasNewYears)
    }

    @Test
    fun testCalculateDaysRemaining() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 14, 0, 0, 0)
        }
        
        val daysToday = service.calculateDaysRemaining("2026-08-14", cal)
        assertEquals(0, daysToday)

        val daysTomorrow = service.calculateDaysRemaining("2026-08-15", cal)
        assertEquals(1, daysTomorrow)

        val daysPast = service.calculateDaysRemaining("2026-08-10", cal)
        assertEquals(-4, daysPast)
    }
}
