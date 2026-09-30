package com.urunkarpm.pingpin.shared.domain

object WorkingDays {
    const val MONDAY = 1
    const val TUESDAY = 2
    const val WEDNESDAY = 4
    const val THURSDAY = 8
    const val FRIDAY = 16
    const val SATURDAY = 32
    const val SUNDAY = 64

    const val DEFAULT_WEEKDAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY

    /**
     * Checks if a day shift (0 for Monday .. 6 for Sunday) is included in the working days bitmask.
     */
    fun isWorkingDayShift(dayShift: Int, workingDaysMask: Int): Boolean {
        if (dayShift < 0 || dayShift > 6) return false
        return (workingDaysMask and (1 shl dayShift)) != 0
    }

    /**
     * Checks if a day shift (0 for Monday .. 6 for Sunday) is included in the WFO days bitmask.
     */
    fun isWfoDayShift(dayShift: Int, wfoDaysMask: Int): Boolean {
        if (dayShift < 0 || dayShift > 6) return false
        return (wfoDaysMask and (1 shl dayShift)) != 0
    }
}
