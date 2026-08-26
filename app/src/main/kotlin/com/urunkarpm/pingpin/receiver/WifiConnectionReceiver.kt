package com.urunkarpm.pingpin.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.urunkarpm.pingpin.service.AttendanceAutoService
import com.urunkarpm.pingpin.service.WifiService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WifiConnectionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WifiConnectionReceiver"
        const val PREFS_NAME = "pingpin_auto_attendance_prefs"
        const val KEY_LAST_MARKED_DATE = "last_auto_marked_date"
        const val ACTION_WIFI_CONNECTED = "com.urunkarpm.pingpin.ACTION_WIFI_CONNECTED"

        /**
         * Registers a system-level NetworkCallback with PendingIntent that persists
         * with Android OS even when the application process is terminated.
         */
        fun registerWifiNetworkCallback(context: Context) {
            try {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                val intent = Intent(context, WifiConnectionReceiver::class.java).apply {
                    action = ACTION_WIFI_CONNECTED
                }

                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)

                connectivityManager.registerNetworkCallback(request, pendingIntent)
                Log.d(TAG, "System PendingIntent NetworkCallback registered for Wi-Fi events.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register NetworkCallback PendingIntent", e)
            }
        }

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
        val action = intent.action
        if (action != WifiManager.NETWORK_STATE_CHANGED_ACTION && action != ACTION_WIFI_CONNECTED) {
            return
        }

        if (action == WifiManager.NETWORK_STATE_CHANGED_ACTION) {
            val networkInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO, android.net.NetworkInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)
            }
            if (networkInfo?.isConnected != true) return
        } else if (action == ACTION_WIFI_CONNECTED) {
            val wifiService = WifiService(context)
            if (!wifiService.isWiFiConnected()) return
        }

        Log.d(TAG, "WiFi connected event received (action: $action).")

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

