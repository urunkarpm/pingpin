package com.urunkarpm.pingpin

import android.app.Application
import android.content.IntentFilter
import android.net.wifi.WifiManager
import com.urunkarpm.pingpin.receiver.WifiConnectionReceiver
import com.urunkarpm.pingpin.service.WifiCheckWorker

class PingPinApplication : Application() {

    private val wifiConnectionReceiver = WifiConnectionReceiver()

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager background Wi-Fi attendance check (15-min fallback)
        WifiCheckWorker.schedulePeriodicCheck(this)

        // Register persistent system PendingIntent NetworkCallback for Wi-Fi events
        // so background Wi-Fi connects trigger attendance checks even if app is closed.
        WifiConnectionReceiver.registerWifiNetworkCallback(this)

        // Dynamically register the WiFi connection receiver for active runtime events
        val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiConnectionReceiver, filter)
    }
}

