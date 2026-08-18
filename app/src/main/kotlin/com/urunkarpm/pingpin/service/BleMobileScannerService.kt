package com.urunkarpm.pingpin.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

enum class DetectedMobileOS { ANDROID, IOS, UNKNOWN }

enum class ProximityZone {
    IMMEDIATE, // < 2 meters (Same Desk/Bay)
    NEARBY,    // 2 - 5 meters (Adjacent Bay/Wing)
    PERIMETER  // > 5 meters (Distant Floor Zone)
}

data class DetectedMobileDevice(
    val id: String,
    val name: String,
    val os: DetectedMobileOS,
    val rssi: Int,
    val estimatedDistanceMeters: Float = 3.0f,
    val proximityZone: ProximityZone = ProximityZone.NEARBY,
    val lastSeen: Long = System.currentTimeMillis()
)

data class BleMobileScanResult(
    val totalCount: Int,
    val androidCount: Int,
    val iosCount: Int,
    val immediateCount: Int = 0,
    val nearbyCount: Int = 0,
    val perimeterCount: Int = 0,
    val devices: List<DetectedMobileDevice>,
    val scannedAt: Long = System.currentTimeMillis()
)

class BleMobileScannerService(private val context: Context) {

    companion object {
        private const val TAG = "BleMobileScanner"
        const val APPLE_VENDOR_ID = 0x004C
        const val GOOGLE_VENDOR_ID = 0x00E0
        const val SAMSUNG_VENDOR_ID = 0x0075

        fun calculateDistanceMeters(rssi: Int, txPower: Int = -59): Float {
            if (rssi >= 0) return -1.0f
            val ratio = (txPower - rssi) / 20.0
            val dist = Math.pow(10.0, ratio).toFloat()
            return dist.coerceIn(0.5f, 15.0f)
        }

        fun classifyProximityZone(rssi: Int): ProximityZone {
            return when {
                rssi >= -65 -> ProximityZone.IMMEDIATE
                rssi >= -75 -> ProximityZone.NEARBY
                else -> ProximityZone.PERIMETER
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun scanForMobiles(scanDurationMs: Long = 5000L): BleMobileScanResult = withContext(Dispatchers.IO) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            throw IllegalStateException("Bluetooth is turned off. Please turn on Bluetooth to scan.")
        }

        val scanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth LE Scanner unavailable.")

        val detectedMobiles = ConcurrentHashMap<String, DetectedMobileDevice>()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                processScanResult(result, detectedMobiles)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (res in results) {
                    processScanResult(res, detectedMobiles)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE Mobile Scan failed with error code: $errorCode")
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
            delay(scanDurationMs)
        } finally {
            try {
                scanner.stopScan(scanCallback)
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping BLE scan", e)
            }
        }

        val rawDevices = detectedMobiles.values.toList()
        val deduplicated = deduplicateDevices(rawDevices)

        val androidCount = deduplicated.count { it.os == DetectedMobileOS.ANDROID }
        val iosCount = deduplicated.count { it.os == DetectedMobileOS.IOS }
        val immCount = deduplicated.count { it.proximityZone == ProximityZone.IMMEDIATE }
        val nearCount = deduplicated.count { it.proximityZone == ProximityZone.NEARBY }
        val perimCount = deduplicated.count { it.proximityZone == ProximityZone.PERIMETER }

        BleMobileScanResult(
            totalCount = deduplicated.size,
            androidCount = androidCount,
            iosCount = iosCount,
            immediateCount = immCount,
            nearbyCount = nearCount,
            perimeterCount = perimCount,
            devices = deduplicated
        )
    }

    @SuppressLint("MissingPermission")
    private fun processScanResult(
        result: ScanResult,
        detectedMobiles: ConcurrentHashMap<String, DetectedMobileDevice>
    ) {
        val deviceId = result.device.address
        val rssi = result.rssi

        if (rssi < -88) return // Filter out faint distant noise

        val record: ScanRecord? = result.scanRecord
        val mfrData = record?.manufacturerSpecificData

        var defaultName = ""

        try {
            defaultName = result.device.name ?: ""
        } catch (_: Exception) {}

        if (defaultName.isEmpty() && record != null) {
            defaultName = record.deviceName ?: ""
        }

        // 1. Check Apple Vendor ID (0x004C) for iOS devices (iPhones/iPads)
        val detectedOS: DetectedMobileOS = if (mfrData != null && mfrData.get(APPLE_VENDOR_ID) != null) {
            if (defaultName.isEmpty()) {
                defaultName = "iOS Signal ($rssi dBm)"
            }
            DetectedMobileOS.IOS
        } else if (mfrData != null && (mfrData.get(GOOGLE_VENDOR_ID) != null || mfrData.get(SAMSUNG_VENDOR_ID) != null)) {
            if (defaultName.isEmpty()) {
                defaultName = "Android Signal ($rssi dBm)"
            }
            DetectedMobileOS.ANDROID
        } else {
            if (defaultName.isNotEmpty()) {
                val lower = defaultName.lowercase()
                if (lower.contains("iphone") || lower.contains("ipad") || lower.contains("ios") || lower.contains("apple")) {
                    DetectedMobileOS.IOS
                } else {
                    DetectedMobileOS.ANDROID
                }
            } else {
                defaultName = "BLE Signal ($rssi dBm)"
                DetectedMobileOS.ANDROID
            }
        }

        val distance = calculateDistanceMeters(rssi)
        val zone = classifyProximityZone(rssi)

        detectedMobiles[deviceId] = DetectedMobileDevice(
            id = deviceId,
            name = defaultName,
            os = detectedOS,
            rssi = rssi,
            estimatedDistanceMeters = distance,
            proximityZone = zone
        )
    }

    private fun deduplicateDevices(raw: List<DetectedMobileDevice>): List<DetectedMobileDevice> {
        val deviceMap = mutableMapOf<String, DetectedMobileDevice>()
        for (device in raw) {
            val existing = deviceMap[device.id]
            if (existing == null || device.rssi > existing.rssi) {
                deviceMap[device.id] = device
            }
        }
        return deviceMap.values.toList()
    }
}
