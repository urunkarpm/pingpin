package com.urunkarpm.pingpin.shared.model

enum class HolidayCategory {
    NATIONAL,
    GAZETTED,
    RESTRICTED,
    REGIONAL;

    val label: String
        get() = when (this) {
            NATIONAL -> "National Holiday"
            GAZETTED -> "Gazetted Holiday"
            RESTRICTED -> "Restricted Holiday"
            REGIONAL -> "Regional Holiday"
        }
}

data class IndianHoliday(
    val id: String,
    val name: String,
    val dateYyyyMmDd: String, // e.g. "2026-08-15"
    val dayOfWeek: String, // e.g. "Saturday"
    val category: HolidayCategory,
    val description: String,
    val isLongWeekendOverride: Boolean? = null
) {
    val isLongWeekend: Boolean
        get() = isLongWeekendOverride ?: (dayOfWeek.equals("Friday", ignoreCase = true) || dayOfWeek.equals("Monday", ignoreCase = true))
}

data class UpcomingHolidayData(
    val holiday: IndianHoliday,
    val daysRemaining: Int,
    val relativeTag: String
)
