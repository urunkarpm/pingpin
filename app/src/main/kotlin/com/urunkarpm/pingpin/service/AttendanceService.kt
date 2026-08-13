package com.urunkarpm.pingpin.service

import android.content.Context
import android.util.Log
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AttendanceCheckResult {
    SUCCESS,
    ALREADY_MARKED,
    WIFI_MISMATCH,
    NO_OFFICE_CONFIG,
    NON_WORKING_DAY,
    ERROR
}

object WorkingDays {
    const val MONDAY = 1
    const val TUESDAY = 2
    const val WEDNESDAY = 4
    const val THURSDAY = 8
    const val FRIDAY = 16
    const val SATURDAY = 32
    const val SUNDAY = 64

    const val DEFAULT_WEEKDAYS = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY

    fun isWorkingDay(calendar: Calendar, workingDaysMask: Int): Boolean {
        // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, Tuesday=3, Wednesday=4, Thursday=5, Friday=6, Saturday=7
        val dayShift = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        return (workingDaysMask and (1 shl dayShift)) != 0
    }

    fun isWfoDay(calendar: Calendar, wfoDaysMask: Int): Boolean {
        val dayShift = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        return (wfoDaysMask and (1 shl dayShift)) != 0
    }
}

open class AttendanceService(
    private val context: Context,
    private val wifiService: WifiService = WifiService(context)
) {
    companion object {
        private const val TAG = "AttendanceService"

        fun getCurrentDateYyyyMmDd(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }
    }

    open suspend fun checkAndMarkAttendance(
        officeConfig: OfficeConfigEntity,
        attendanceRepo: AttendanceRepository,
        onAttendanceMarked: suspend () -> Unit
    ): AttendanceCheckResult {
        return try {
            val today = getCurrentDateYyyyMmDd()

            // 1. Check if already marked today
            val existing = attendanceRepo.getByDate(today)
            if (existing != null) {
                return AttendanceCheckResult.ALREADY_MARKED
            }

            // 2. Check working days
            val cal = Calendar.getInstance()
            if (!WorkingDays.isWorkingDay(cal, officeConfig.workingDaysMask)) {
                return AttendanceCheckResult.NON_WORKING_DAY
            }

            // 3. Check Wi-Fi match
            val isOnWifi = wifiService.isConnectedToSSID(officeConfig.ssid)
            if (!isOnWifi) {
                return AttendanceCheckResult.WIFI_MISMATCH
            }

            // 4. Record attendance as "present"
            val currentSsid = wifiService.getWifiSSID()
            attendanceRepo.insertRecord(
                dateYyyyMmDd = today,
                status = "present",
                markedAt = System.currentTimeMillis(),
                ssidSnapshot = currentSsid,
                distanceMeters = null
            )

            // Trigger notification callback
            onAttendanceMarked()

            AttendanceCheckResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Error checking attendance", e)
            AttendanceCheckResult.ERROR
        }
    }
}
