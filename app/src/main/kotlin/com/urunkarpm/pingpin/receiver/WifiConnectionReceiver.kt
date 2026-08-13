package com.urunkarpm.pingpin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager

import android.os.Build
import android.util.Log
import com.urunkarpm.pingpin.service.AttendanceAutoService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WifiConnectionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiConnectionReceiver"
        const val PREFS_NAME = "pingpin_auto_attendance_prefs"
        const val KEY_LAST_MARKED_DATE = "last_auto_marked_date"

        /**
         * Marks today's date as already processed so the receiver won't trigger again.
         * Called by AttendanceAutoService after a successful attendance mark.
         */
        fun markTodayAsHandled(context: Context) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_MARKED_DATE, today)
                .apply()
            Log.d(TAG, "Daily guard set for $today — receiver will skip for the rest of today.")
        }

        /**
         * Returns true if attendance has already been handled today (either marked or
         * confirmed already-marked), so we should not re-trigger.
         */
        fun isAlreadyHandledToday(context: Context): Boolean {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_LAST_MARKED_DATE, null)
            return stored == today
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WifiManager.NETWORK_STATE_CHANGED_ACTION) return

        val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO, android.net.NetworkInfo::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
        }

        if (networkInfo?.isConnected != true) return

        Log.d(TAG, "WiFi connected event received.")

        // ── Daily guard: skip if already handled today ──────────────────────
        if (isAlreadyHandledToday(context)) {
            Log.d(TAG, "Attendance already handled today — skipping service launch.")
            return
        }

        // ── Launch foreground service to safely do the DB check + write ─────
        Log.d(TAG, "Launching AttendanceAutoService for automatic attendance check.")
        val serviceIntent = Intent(context, AttendanceAutoService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
