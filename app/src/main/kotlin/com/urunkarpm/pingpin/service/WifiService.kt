package com.urunkarpm.pingpin.service

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.urunkarpm.pingpin.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScannedWifiNetwork(
    val ssid: String,
    val rssi: Int,
    val isConnected: Boolean = false,
    val capabilities: String = "",
    val frequency: Int = 0
) {
    val is5GHz: Boolean get() = frequency >= 4900
    val securityLabel: String get() = when {
        capabilities.contains("WPA3", ignoreCase = true) -> "WPA3"
        capabilities.contains("WPA2", ignoreCase = true) -> "WPA2"
        capabilities.contains("WPA", ignoreCase = true) -> "WPA"
        capabilities.contains("WEP", ignoreCase = true) -> "WEP"
        capabilities.contains("ESS", ignoreCase = true) && !capabilities.contains("PSK") && !capabilities.contains("EAP") -> "Open"
        else -> "Secured"
    }
}

class WifiService(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pingpin_wifi_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "WifiService"
        private const val WIFI_HISTORY_KEY = "known_wifi_ssids"
    }

    /**
     * Checks if the app has runtime location permission (required by Android to read Wi-Fi SSID).
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if system location services (GPS) are enabled (required by Android OS to reveal Wi-Fi SSID).
     */
    fun isLocationServicesEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets current active Wi-Fi SSID (removes surrounding quotes).
     */
    suspend fun getWifiSSID(): String? = withContext(Dispatchers.IO) {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val activeNetwork = connectivityManager.activeNetwork ?: return@withContext null
            val capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork) ?: return@withContext null

            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return@withContext null
            }

            var rawSsid: String? = null

            // 1. Try modern TransportInfo API on Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val wifiInfo = capabilities.transportInfo as? WifiInfo
                if (wifiInfo != null) {
                    rawSsid = wifiInfo.ssid
                }
            }

            // 2. Fallback to WifiManager.connectionInfo
            if (rawSsid == null || rawSsid.equals("<unknown ssid>", ignoreCase = true) || rawSsid == "\"<unknown ssid>\"" || rawSsid == "0x") {
                val wifiManager =
                    context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                val connectionInfo: WifiInfo? = wifiManager.connectionInfo
                if (connectionInfo != null) {
                    rawSsid = connectionInfo.ssid
                }
            }

            var ssid = rawSsid?.replace("\"", "")?.trim() ?: return@withContext null

            if (ssid.isNotEmpty() &&
                !ssid.equals("<unknown ssid>", ignoreCase = true) &&
                ssid != "0x"
            ) {
                addKnownSSID(ssid)
                return@withContext ssid
            }
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Wi-Fi SSID", e)
            null
        }
    }

    /**
     * Stores historical SSIDs in SharedPreferences.
     */
    suspend fun addKnownSSID(ssid: String) = withContext(Dispatchers.IO) {
        val trimmed = ssid.trim()
        if (trimmed.isEmpty() || trimmed.equals("<unknown ssid>", ignoreCase = true)) return@withContext
        val set = prefs.getStringSet(WIFI_HISTORY_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (set.add(trimmed)) {
            prefs.edit().putStringSet(WIFI_HISTORY_KEY, set).apply()
        }
    }

    /**
     * Removes a stored SSID from SharedPreferences history.
     */
    suspend fun removeKnownSSID(ssid: String) = withContext(Dispatchers.IO) {
        val set = prefs.getStringSet(WIFI_HISTORY_KEY, emptySet())?.toMutableSet() ?: return@withContext
        if (set.remove(ssid.trim())) {
            prefs.edit().putStringSet(WIFI_HISTORY_KEY, set).apply()
        }
    }

    /**
     * Retrieves known SSIDs from DB and SharedPreferences.
     */
    suspend fun getKnownSSIDs(includeCurrentLive: Boolean = false): List<String> =
        withContext(Dispatchers.IO) {
            val result = mutableSetOf<String>()

            if (includeCurrentLive) {
                val current = getWifiSSID()
                if (!current.isNullOrEmpty()) {
                    result.add(current)
                }
            }

            val db = AppDatabase.getInstance(context)
            val config = db.officeConfigDao().getConfig()
            if (config != null && config.ssid.isNotEmpty()) {
                result.add(config.ssid)
            }

            val storedSet = prefs.getStringSet(WIFI_HISTORY_KEY, emptySet()) ?: emptySet()
            result.addAll(storedSet.filter { it.isNotEmpty() })

            result.toList()
        }

    /**
     * Scans for nearby Wi-Fi networks safely.
     */
    suspend fun getScannedNetworks(): List<ScannedWifiNetwork> = withContext(Dispatchers.IO) {
        val list = mutableListOf<ScannedWifiNetwork>()
        val activeSsid = getWifiSSID()

        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            wifiManager.startScan()
            val results = wifiManager.scanResults ?: emptyList()

            val ssidMap = mutableMapOf<String, ScannedWifiNetwork>()
            for (res in results) {
                @Suppress("DEPRECATION")
                var rawSsid = res.SSID ?: continue
                rawSsid = rawSsid.replace("\"", "").trim()
                if (rawSsid.isEmpty() || rawSsid.equals("<unknown ssid>", ignoreCase = true) || rawSsid == "0x") continue

                val isCurrent = activeSsid != null && activeSsid.equals(rawSsid, ignoreCase = true)
                val existing = ssidMap[rawSsid]

                // Keep strongest signal for duplicated SSIDs
                if (existing == null || res.level > existing.rssi) {
                    ssidMap[rawSsid] = ScannedWifiNetwork(
                        ssid = rawSsid,
                        rssi = res.level,
                        isConnected = isCurrent,
                        capabilities = res.capabilities ?: "",
                        frequency = res.frequency
                    )
                }
            }

            // Ensure current active SSID is included if active but not in scan results
            if (activeSsid != null && !ssidMap.containsKey(activeSsid)) {
                ssidMap[activeSsid] = ScannedWifiNetwork(
                    ssid = activeSsid,
                    rssi = -50,
                    isConnected = true,
                    capabilities = "WPA2",
                    frequency = 5000
                )
            }

            list.addAll(ssidMap.values.sortedWith(
                compareByDescending<ScannedWifiNetwork> { it.isConnected }
                    .thenByDescending { it.rssi }
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning Wi-Fi networks", e)
            if (activeSsid != null) {
                list.add(ScannedWifiNetwork(ssid = activeSsid, rssi = -50, isConnected = true))
            }
        }

        list
    }

    /**
     * Checks if current connection matches target SSID.
     */
    suspend fun isConnectedToSSID(targetSSID: String): Boolean {
        val currentSSID = getWifiSSID() ?: return false
        return currentSSID.equals(targetSSID.trim(), ignoreCase = true)
    }

    /**
     * Checks if Wi-Fi transport is active.
     */
    fun isWiFiConnected(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
