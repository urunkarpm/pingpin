package com.urunkarpm.pingpin.service

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppInstallManager {
    private const val PREFS_NAME = "pingpin_install_prefs"
    private const val KEY_INSTALL_DATE = "app_install_date_yyyy_mm_dd"
    private const val KEY_INSTALL_TIME_MS = "app_install_time_ms"

    fun getInstallTimeMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTime = prefs.getLong(KEY_INSTALL_TIME_MS, 0L)
        if (savedTime > 0L) {
            return savedTime
        }

        val packageInstallTime = try {
            context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val finalInstallTime = if (packageInstallTime > 0L) packageInstallTime else System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val installDateStr = sdf.format(Date(finalInstallTime))

        prefs.edit()
            .putLong(KEY_INSTALL_TIME_MS, finalInstallTime)
            .putString(KEY_INSTALL_DATE, installDateStr)
            .apply()

        return finalInstallTime
    }

    fun getInstallDateYyyyMmDd(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDate = prefs.getString(KEY_INSTALL_DATE, null)
        if (!savedDate.isNullOrEmpty()) {
            return savedDate
        }

        val installTimeMs = getInstallTimeMs(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val installDateStr = sdf.format(Date(installTimeMs))

        prefs.edit().putString(KEY_INSTALL_DATE, installDateStr).apply()
        return installDateStr
    }

    fun getInstallDateCalendar(context: Context): Calendar {
        val installTimeMs = getInstallTimeMs(context)
        return Calendar.getInstance().apply {
            timeInMillis = installTimeMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
