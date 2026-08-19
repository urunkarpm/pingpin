package com.urunkarpm.pingpin.service

import android.content.Context
import android.util.Log
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.MakeupWfoSuggestionEntity
import com.urunkarpm.pingpin.data.model.IndianHoliday
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.MakeupWfoRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MakeupWfoManager(
    private val context: Context,
    private val makeupRepo: MakeupWfoRepository,
    private val attendanceRepo: AttendanceRepository,
    private val wifiService: WifiService = WifiService(context),
    private val attendanceService: AttendanceService = AttendanceService(context, wifiService),
    private val holidayService: IndianHolidayService = IndianHolidayService(),
    private val notifService: NotificationService? = NotificationService(context)
) {
    companion object {
        private const val TAG = "MakeupWfoManager"

        fun formatDateToYyyyMmDd(cal: Calendar): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(cal.time)
        }

        fun formatReadableDate(dateYyyyMmDd: String): String {
            return try {
                val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = inputSdf.parse(dateYyyyMmDd) ?: return dateYyyyMmDd
                val outputSdf = SimpleDateFormat("EEEE, MMM d", Locale.US)
                outputSdf.format(date)
            } catch (e: Exception) {
                dateYyyyMmDd
            }
        }
    }

    /**
     * Calculates all scheduled WFO dates for the current week (Monday through Sunday).
     */
    fun getThisWeekWfoDates(config: OfficeConfigEntity): List<String> {
        val result = mutableListOf<String>()
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val holidays = holidayService.getAllHolidays()

        for (i in 0..6) {
            val dateStr = formatDateToYyyyMmDd(cal)
            val isWorking = WorkingDays.isWorkingDay(cal, config.workingDaysMask)
            val isWfo = WorkingDays.isWfoDay(cal, config.wfoDaysMask)
            val isHoliday = holidays.any { it.dateYyyyMmDd == dateStr }

            if (isWorking && isWfo && !isHoliday) {
                result.add(dateStr)
            }
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    /**
     * Checks if all required WFO days for the current week have attendance marked as completed/present.
     */
    suspend fun areAllThisWeekWfoCompleted(config: OfficeConfigEntity): Boolean {
        val weekWfoDates = getThisWeekWfoDates(config)
        if (weekWfoDates.isEmpty()) return false

        for (dateStr in weekWfoDates) {
            val record = attendanceRepo.getByDate(dateStr)
            if (record == null) {
                return false
            }
        }
        return true
    }

    /**
     * Cancels any pending or accepted makeup WFO suggestions if all WFO days for this week are completed
     * or if the missed date itself was marked completed.
     */
    suspend fun cancelMakeupSuggestionsIfFulfilled(config: OfficeConfigEntity) {
        val isWeekComplete = areAllThisWeekWfoCompleted(config)
        val allSuggestions = makeupRepo.getAll()

        for (suggestion in allSuggestions) {
            if (suggestion.status == "PENDING" || suggestion.status == "ACCEPTED") {
                val missedRecord = attendanceRepo.getByDate(suggestion.missedDateYyyyMmDd)
                if (isWeekComplete || missedRecord != null) {
                    makeupRepo.updateStatus(suggestion.id, "DECLINED")
                    notifService?.cancelAlarm(suggestion.alarmId)
                }
            }
        }
    }

    /**
     * Evaluates missed WFO day conditions after 2:00 PM and attempts automatic attendance check
     * before suggesting a WFH day for compensation.
     */
    suspend fun evaluateAndSuggestMakeup(
        officeConfig: OfficeConfigEntity,
        onAttendanceAutoMarked: (suspend () -> Unit)? = null
    ): MakeupWfoSuggestionEntity? {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val todayStr = formatDateToYyyyMmDd(now)

        // 0. Cancel makeup suggestions if all WFO days for this week are completed or missed date is marked
        cancelMakeupSuggestionsIfFulfilled(officeConfig)
        if (areAllThisWeekWfoCompleted(officeConfig)) {
            return null
        }

        // 1. Check if today is a scheduled WFO day
        val isTodayWfo = WorkingDays.isWorkingDay(now, officeConfig.workingDaysMask) &&
                WorkingDays.isWfoDay(now, officeConfig.wfoDaysMask)

        // 2. Check if attendance is already marked today
        val todayRecord = attendanceRepo.getByDate(todayStr)
        if (todayRecord != null) {
            // If an accepted makeup suggestion was scheduled for today, mark as COMPLETED
            val active = makeupRepo.getActiveSuggestion()
            if (active != null && active.suggestedDateYyyyMmDd == todayStr && active.status == "ACCEPTED") {
                makeupRepo.updateStatus(active.id, "COMPLETED")
            }
            return null
        }

        // 3. If today is WFO and time is past 2:00 PM (14:00)
        if (isTodayWfo && currentHour >= 14) {
            // Pre-Alert Check: Try to mark attendance if connected to Office Wi-Fi
            if (officeConfig.ssid.isNotEmpty() && wifiService.isConnectedToSSID(officeConfig.ssid)) {
                try { Log.d(TAG, "User connected to office Wi-Fi after 2 PM. Marking attendance...") } catch (_: Throwable) {}
                val result = attendanceService.checkAndMarkAttendance(
                    officeConfig = officeConfig,
                    attendanceRepo = attendanceRepo,
                    onAttendanceMarked = {
                        onAttendanceAutoMarked?.invoke()
                    }
                )
                if (result == AttendanceCheckResult.SUCCESS) {
                    try { Log.d(TAG, "Attendance marked successfully at 2 PM. Suppressing alert.") } catch (_: Throwable) {}
                    return null
                }
            }

            // Attendance was not marked & not on office Wi-Fi -> generate/retrieve makeup suggestion
            return processMissedWfoDay(todayStr, officeConfig)
        }

        // 4. Check for yesterday if missed and not evaluated yet
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = formatDateToYyyyMmDd(yesterday)
        val isYesterdayWfo = WorkingDays.isWorkingDay(yesterday, officeConfig.workingDaysMask) &&
                WorkingDays.isWfoDay(yesterday, officeConfig.wfoDaysMask)

        val yesterdayRecord = attendanceRepo.getByDate(yesterdayStr)
        if (isYesterdayWfo && yesterdayRecord == null) {
            return processMissedWfoDay(yesterdayStr, officeConfig)
        }

        // Return current active pending or accepted suggestion if any
        return makeupRepo.getActiveSuggestion()
    }

    private suspend fun processMissedWfoDay(
        missedDateStr: String,
        config: OfficeConfigEntity
    ): MakeupWfoSuggestionEntity? {
        val existing = makeupRepo.getByMissedDate(missedDateStr)
        if (existing != null) {
            if (existing.status == "PENDING" || existing.status == "ACCEPTED") {
                return existing
            }
            return null // Declined, expired, or completed
        }

        // Find candidate compensation day (STRICTLY a WFH working day)
        val candidateDateStr = findNextWfhCompensationDay(missedDateStr, config) ?: return null

        val newSuggestion = MakeupWfoSuggestionEntity(
            missedDateYyyyMmDd = missedDateStr,
            suggestedDateYyyyMmDd = candidateDateStr,
            status = "PENDING"
        )

        val id = makeupRepo.insertSuggestion(newSuggestion)
        return newSuggestion.copy(id = id.toInt())
    }

    /**
     * Finds the next candidate compensation day.
     * MUST be:
     * 1. A working day (workingDaysMask)
     * 2. STRICTLY NOT a WFO day (!wfoDaysMask -> WFH day)
     * 3. NOT a public holiday
     * 4. NOT already marked with attendance
     */
    private suspend fun findNextWfhCompensationDay(
        missedDateStr: String,
        config: OfficeConfigEntity
    ): String? {
        val missedSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val missedDate = missedSdf.parse(missedDateStr) ?: Date()

        val holidays = holidayService.getAllHolidays()

        val calPointer = Calendar.getInstance().apply { time = missedDate }
        for (i in 1..14) {
            calPointer.add(Calendar.DAY_OF_YEAR, 1)
            val dateStr = formatDateToYyyyMmDd(calPointer)
            if (isEligibleWfhCompensationDay(calPointer, config, holidays)) {
                return dateStr
            }
        }

        return null
    }

    private suspend fun isEligibleWfhCompensationDay(
        cal: Calendar,
        config: OfficeConfigEntity,
        holidays: List<IndianHoliday>
    ): Boolean {
        // Must be working day
        if (!WorkingDays.isWorkingDay(cal, config.workingDaysMask)) return false

        // Must STRICTLY NOT be a WFO day (must be a WFH day)
        if (WorkingDays.isWfoDay(cal, config.wfoDaysMask)) return false

        val dateStr = formatDateToYyyyMmDd(cal)

        // Must not be a public holiday
        val isHoliday = holidays.any { it.dateYyyyMmDd == dateStr }
        if (isHoliday) return false

        // Must not already have attendance marked
        val existingAttendance = attendanceRepo.getByDate(dateStr)
        if (existingAttendance != null) return false

        return true
    }
}
