package com.urunkarpm.pingpin.data.model

import androidx.compose.ui.graphics.Color
import com.urunkarpm.pingpin.ui.theme.*

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

    val badgeColor: Color
        get() = when (this) {
            NATIONAL -> AmberOrange
            GAZETTED -> ElectricBlue
            RESTRICTED -> Color(0xFFFF9800)
            REGIONAL -> TealAccent
        }

    val badgeBgColorLight: Color
        get() = when (this) {
            NATIONAL -> AmberOrangeBgLight
            GAZETTED -> ElectricBlueBgLight
            RESTRICTED -> Color(0xFFFFF3E0)
            REGIONAL -> TealAccentBgLight
        }

    val badgeBgColorDark: Color
        get() = when (this) {
            NATIONAL -> AmberOrangeBgDark
            GAZETTED -> ElectricBlueBgDark
            RESTRICTED -> Color(0xFFE65100)
            REGIONAL -> TealAccentBgDark
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
