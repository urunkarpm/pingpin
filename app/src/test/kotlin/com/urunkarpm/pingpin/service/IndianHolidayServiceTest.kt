package com.urunkarpm.pingpin.service

import com.urunkarpm.pingpin.data.model.HolidayCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

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

    @Test
    fun testParseHolidaysJson_ValidJsonArray() {
        val mockApiResponse = """
            [
              {
                "date": "2026-01-26",
                "name": "Republic Day",
                "type": "national",
                "state_code": "IN",
                "description": "Celebrates the adoption of Constitution of India"
              },
              {
                "date": "2026-08-15",
                "name": "Independence Day",
                "type": "national",
                "state_code": "IN",
                "description": "Independence Day of India"
              },
              {
                "date": "2026-11-01",
                "name": "Kannada Rajyotsava",
                "type": "regional",
                "state_code": "KA",
                "description": "Karnataka State Formation Day"
              }
            ]
        """.trimIndent()

        val parsed = IndianHolidayService.parseHolidaysJson(mockApiResponse)
        assertEquals(3, parsed.size)

        val republicDay = parsed.find { it.name == "Republic Day" }
        assertNotNull(republicDay)
        assertEquals("2026-01-26", republicDay?.dateYyyyMmDd)
        assertEquals(HolidayCategory.NATIONAL, republicDay?.category)
        assertEquals("Monday", republicDay?.dayOfWeek)
        assertTrue(republicDay?.isLongWeekend == true)

        val rajyotsava = parsed.find { it.name == "Kannada Rajyotsava" }
        assertNotNull(rajyotsava)
        assertEquals(HolidayCategory.REGIONAL, rajyotsava?.category)
    }

    @Test
    fun testMapTypeToCategory_HandlesVariations() {
        assertEquals(HolidayCategory.NATIONAL, IndianHolidayService.mapTypeToCategory("national", "IN"))
        assertEquals(HolidayCategory.GAZETTED, IndianHolidayService.mapTypeToCategory("public", "IN"))
        assertEquals(HolidayCategory.GAZETTED, IndianHolidayService.mapTypeToCategory("gazetted", "IN"))
        assertEquals(HolidayCategory.RESTRICTED, IndianHolidayService.mapTypeToCategory("restricted", "IN"))
        assertEquals(HolidayCategory.REGIONAL, IndianHolidayService.mapTypeToCategory("regional", "KA"))
    }
}
