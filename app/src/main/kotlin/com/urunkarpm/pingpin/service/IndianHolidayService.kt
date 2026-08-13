package com.urunkarpm.pingpin.service

import com.urunkarpm.pingpin.data.model.HolidayCategory
import com.urunkarpm.pingpin.data.model.IndianHoliday
import com.urunkarpm.pingpin.data.model.UpcomingHolidayData
import java.text.SimpleDateFormat
import java.util.*

open class IndianHolidayService {

    private val allHolidays: List<IndianHoliday> = listOf(
        // 2026 Holidays
        IndianHoliday("h2026_01", "New Year's Day", "2026-01-01", "Thursday", HolidayCategory.RESTRICTED, "First day of the Gregorian calendar year."),
        IndianHoliday("h2026_02", "Makar Sankranti / Pongal", "2026-01-14", "Wednesday", HolidayCategory.GAZETTED, "Harvest festival celebrating the Sun God's transition."),
        IndianHoliday("h2026_03", "Republic Day", "2026-01-26", "Monday", HolidayCategory.NATIONAL, "Honors the date on which the Constitution of India came into effect."),
        IndianHoliday("h2026_04", "Maha Shivratri", "2026-02-15", "Sunday", HolidayCategory.GAZETTED, "Great night of Lord Shiva celebration."),
        IndianHoliday("h2026_05", "Holi", "2026-03-04", "Wednesday", HolidayCategory.GAZETTED, "Festival of colors and victory of good over evil."),
        IndianHoliday("h2026_06", "Eid ul-Fitr", "2026-03-20", "Friday", HolidayCategory.GAZETTED, "Islamic festival marking the end of Ramadan."),
        IndianHoliday("h2026_07", "Ram Navami", "2026-03-27", "Friday", HolidayCategory.RESTRICTED, "Celebration of the birth of Lord Rama."),
        IndianHoliday("h2026_08", "Mahavir Jayanti", "2026-03-31", "Tuesday", HolidayCategory.GAZETTED, "Birth anniversary of Lord Mahavira."),
        IndianHoliday("h2026_09", "Good Friday", "2026-04-03", "Friday", HolidayCategory.GAZETTED, "Christian holiday commemorating the crucifixion of Jesus."),
        IndianHoliday("h2026_10", "Dr. B.R. Ambedkar Jayanti", "2026-04-14", "Tuesday", HolidayCategory.GAZETTED, "Birth anniversary of Dr. B.R. Ambedkar, father of Indian Constitution."),
        IndianHoliday("h2026_11", "May Day / Labour Day", "2026-05-01", "Friday", HolidayCategory.REGIONAL, "International Workers' Day celebration."),
        IndianHoliday("h2026_12", "Bakrid / Eid al-Adha", "2026-05-27", "Wednesday", HolidayCategory.GAZETTED, "Feast of the Sacrifice observed by Muslims."),
        IndianHoliday("h2026_13", "Buddha Purnima", "2026-05-31", "Sunday", HolidayCategory.GAZETTED, "Birth anniversary of Gautama Buddha."),
        IndianHoliday("h2026_14", "Muharram", "2026-06-26", "Friday", HolidayCategory.GAZETTED, "First month of the Islamic calendar."),
        
        // August - September 2026 Window
        IndianHoliday("h2026_15", "Independence Day", "2026-08-15", "Saturday", HolidayCategory.NATIONAL, "National celebration of 79th Independence Day of India."),
        IndianHoliday("h2026_16", "Parsi New Year (Shahenshahi)", "2026-08-16", "Sunday", HolidayCategory.RESTRICTED, "Navroz, the Parsi New Year celebration."),
        IndianHoliday("h2026_17", "Milad-un-Nabi / Eid-e-Milad", "2026-08-25", "Tuesday", HolidayCategory.GAZETTED, "Birth anniversary of Prophet Muhammad."),
        IndianHoliday("h2026_18", "Onam / Thiruvonam", "2026-08-26", "Wednesday", HolidayCategory.REGIONAL, "Major harvest festival of Kerala."),
        IndianHoliday("h2026_19", "Raksha Bandhan", "2026-08-28", "Friday", HolidayCategory.RESTRICTED, "Celebration of the sacred bond between brothers and sisters."),
        IndianHoliday("h2026_20", "Krishna Janmashtami", "2026-09-03", "Thursday", HolidayCategory.GAZETTED, "Birth anniversary of Lord Krishna."),
        IndianHoliday("h2026_21", "Ganesh Chaturthi", "2026-09-14", "Monday", HolidayCategory.GAZETTED, "Grand festival commemorating the arrival of Lord Ganesha."),
        
        // Oct - Dec 2026 Window
        IndianHoliday("h2026_22", "Mahatma Gandhi Jayanti", "2026-10-02", "Friday", HolidayCategory.NATIONAL, "Birth anniversary of Father of the Nation, Mahatma Gandhi."),
        IndianHoliday("h2026_23", "Maha Navami", "2026-10-19", "Monday", HolidayCategory.RESTRICTED, "Ninth day of Durga Puja celebrations."),
        IndianHoliday("h2026_24", "Dussehra (Vijayadashami)", "2026-10-20", "Tuesday", HolidayCategory.GAZETTED, "Triumph of Lord Rama over Ravana."),
        IndianHoliday("h2026_25", "Diwali (Deepavali)", "2026-11-08", "Sunday", HolidayCategory.GAZETTED, "Festival of Lights celebrating light over darkness."),
        IndianHoliday("h2026_26", "Goverdhan Puja", "2026-11-09", "Monday", HolidayCategory.RESTRICTED, "Post-Diwali celebration honoring Lord Krishna."),
        IndianHoliday("h2026_27", "Bhai Dooj", "2026-11-10", "Tuesday", HolidayCategory.RESTRICTED, "Festival celebrating brother-sister affection."),
        IndianHoliday("h2026_28", "Guru Nanak Jayanti", "2026-11-24", "Tuesday", HolidayCategory.GAZETTED, "Birth anniversary of Guru Nanak Dev Ji."),
        IndianHoliday("h2026_29", "Christmas Day", "2026-12-25", "Friday", HolidayCategory.GAZETTED, "Annual festival commemorating the birth of Jesus Christ.")
    )

    open fun getAllHolidays(): List<IndianHoliday> {
        return allHolidays.sortedBy { it.dateYyyyMmDd }
    }

    fun getUpcomingHolidays(
        fromDate: Calendar = Calendar.getInstance(),
        daysAhead: Int = 21
    ): List<UpcomingHolidayData> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val startCal = (fromDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = (startCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, daysAhead)
        }

        val result = mutableListOf<UpcomingHolidayData>()

        for (holiday in allHolidays) {
            val parsedDate = sdf.parse(holiday.dateYyyyMmDd) ?: continue
            val holidayCal = Calendar.getInstance().apply {
                time = parsedDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (!holidayCal.before(startCal) && !holidayCal.after(endCal)) {
                val diffMillis = holidayCal.timeInMillis - startCal.timeInMillis
                val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()

                val relativeTag = when (diffDays) {
                    0 -> "Today!"
                    1 -> "Tomorrow!"
                    in 2..6 -> "In $diffDays days"
                    7 -> "In 1 week"
                    in 8..13 -> "In $diffDays days"
                    14 -> "In 2 weeks"
                    in 15..20 -> "In $diffDays days"
                    21 -> "In 3 weeks"
                    else -> "In $diffDays days"
                }

                result.add(UpcomingHolidayData(holiday, diffDays, relativeTag))
            }
        }

        return result.sortedBy { it.daysRemaining }
    }

    fun getHolidayForDate(dateYyyyMmDd: String): IndianHoliday? {
        return allHolidays.find { it.dateYyyyMmDd == dateYyyyMmDd }
    }

    fun getUpcomingLongWeekends(): List<IndianHoliday> {
        return allHolidays.filter { it.isLongWeekend }
    }
}
