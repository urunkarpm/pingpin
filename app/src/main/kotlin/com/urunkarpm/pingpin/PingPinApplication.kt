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

        // Dynamically register the WiFi connection receiver so it responds instantly
        // whenever the device joins any WiFi network (implicit broadcast restriction
        // on API 26+ means we must register dynamically, not in the manifest).
        val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        registerReceiver(wifiConnectionReceiver, filter)
    }
}

