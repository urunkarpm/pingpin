package com.urunkarpm.pingpin.service

import com.urunkarpm.pingpin.data.model.HolidayCategory
import com.urunkarpm.pingpin.data.model.IndianHoliday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testInitialState_IsEmptyWithoutHardcodedHolidays() {
        val holidays = service.getAllHolidays()
        assertTrue(holidays.isEmpty())
    }

    @Test
    fun testSetHolidays_ReturnsSortedList() {
        val sampleHolidays = listOf(
            IndianHoliday("h2", "Independence Day", "2026-08-15", "Saturday", HolidayCategory.NATIONAL, "Independence Day"),
            IndianHoliday("h1", "Republic Day", "2026-01-26", "Monday", HolidayCategory.NATIONAL, "Republic Day"),
            IndianHoliday("h3", "Gandhi Jayanti", "2026-10-02", "Friday", HolidayCategory.NATIONAL, "Gandhi Jayanti")
        )
        service.setHolidays(sampleHolidays)

        val holidays = service.getAllHolidays()
        assertEquals(3, holidays.size)
        assertEquals("2026-01-26", holidays[0].dateYyyyMmDd)
        assertEquals("2026-08-15", holidays[1].dateYyyyMmDd)
        assertEquals("2026-10-02", holidays[2].dateYyyyMmDd)
    }

    @Test
    fun testGetHolidaysFromPresent_FiltersPastHolidays() {
        val sampleHolidays = listOf(
            IndianHoliday("h1", "Republic Day", "2026-01-26", "Monday", HolidayCategory.NATIONAL, "Republic Day"),
            IndianHoliday("h2", "Independence Day", "2026-08-15", "Saturday", HolidayCategory.NATIONAL, "Independence Day"),
            IndianHoliday("h3", "Gandhi Jayanti", "2026-10-02", "Friday", HolidayCategory.NATIONAL, "Gandhi Jayanti")
        )
        service.setHolidays(sampleHolidays)

        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 14, 0, 0, 0)
        }
        val presentHolidays = service.getHolidaysFromPresent(cal)

        assertEquals(2, presentHolidays.size)
        presentHolidays.forEach { holiday ->
            assertTrue(holiday.dateYyyyMmDd >= "2026-08-14")
        }

        val hasIndependenceDay = presentHolidays.any { it.dateYyyyMmDd == "2026-08-15" }
        val hasNewYears = presentHolidays.any { it.dateYyyyMmDd == "2026-01-26" }

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
    fun testGetUpcomingHolidays_WithinDaysAheadWindow() {
        val sampleHolidays = listOf(
            IndianHoliday("h1", "Past Event", "2026-08-01", "Saturday", HolidayCategory.REGIONAL, "Past"),
            IndianHoliday("h2", "Independence Day", "2026-08-15", "Saturday", HolidayCategory.NATIONAL, "Independence Day"),
            IndianHoliday("h3", "Raksha Bandhan", "2026-08-28", "Friday", HolidayCategory.RESTRICTED, "Raksha Bandhan"),
            IndianHoliday("h4", "Far Future Event", "2026-11-08", "Sunday", HolidayCategory.GAZETTED, "Diwali")
        )
        service.setHolidays(sampleHolidays)

        val fromCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 10, 0, 0, 0)
        }

        val upcoming = service.getUpcomingHolidays(fromDate = fromCal, daysAhead = 21)
        assertEquals(2, upcoming.size)
        assertEquals("Independence Day", upcoming[0].holiday.name)
        assertEquals(5, upcoming[0].daysRemaining)
        assertEquals("In 5 days", upcoming[0].relativeTag)

        assertEquals("Raksha Bandhan", upcoming[1].holiday.name)
        assertEquals(18, upcoming[1].daysRemaining)
        assertEquals("In 18 days", upcoming[1].relativeTag)
    }

    @Test
    fun testGetHolidayForDateAndLongWeekends() {
        val sampleHolidays = listOf(
            IndianHoliday("h1", "Republic Day", "2026-01-26", "Monday", HolidayCategory.NATIONAL, "Republic Day"),
            IndianHoliday("h2", "Independence Day", "2026-08-15", "Saturday", HolidayCategory.NATIONAL, "Independence Day"),
            IndianHoliday("h3", "Gandhi Jayanti", "2026-10-02", "Friday", HolidayCategory.NATIONAL, "Gandhi Jayanti")
        )
        service.setHolidays(sampleHolidays)

        val found = service.getHolidayForDate("2026-08-15")
        assertNotNull(found)
        assertEquals("Independence Day", found?.name)

        val notFound = service.getHolidayForDate("2026-08-16")
        assertNull(notFound)

        val longWeekends = service.getUpcomingLongWeekends()
        assertEquals(2, longWeekends.size) // Monday and Friday
        assertTrue(longWeekends.any { it.name == "Republic Day" })
        assertTrue(longWeekends.any { it.name == "Gandhi Jayanti" })
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
