package com.urunkarpm.pingpin.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

enum class DetectedLaptopOS { WINDOWS, MACOS, UNKNOWN }

data class DetectedLaptopDevice(
    val id: String,
    val name: String,
    val os: DetectedLaptopOS,
    val rssi: Int,
    val lastSeen: Long = System.currentTimeMillis()
)

data class BleLaptopScanResult(
    val totalCount: Int,
    val windowsCount: Int,
    val macCount: Int,
    val devices: List<DetectedLaptopDevice>,
    val scannedAt: Long = System.currentTimeMillis()
)

class BleLaptopScannerService(private val context: Context) {

    companion object {
        private const val TAG = "BleLaptopScanner"
        const val MICROSOFT_VENDOR_ID = 0x0006
        const val APPLE_VENDOR_ID = 0x004C
    }

    @SuppressLint("MissingPermission")
    suspend fun scanForLaptops(scanDurationMs: Long = 5000L): BleLaptopScanResult = withContext(Dispatchers.IO) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            throw IllegalStateException("Bluetooth is not turned on. Please enable Bluetooth to scan.")
        }

        val scanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth LE Scanner unavailable.")

        val detectedLaptops = ConcurrentHashMap<String, DetectedLaptopDevice>()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                processScanResult(result, detectedLaptops)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                for (res in results) {
                    processScanResult(res, detectedLaptops)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE Scan failed with error code: $errorCode")
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
        delay(scanDurationMs)
        scanner.stopScan(scanCallback)

        val rawDevices = detectedLaptops.values.toList()
        val deduplicated = deduplicateDevices(rawDevices)

        val winCount = deduplicated.count { it.os == DetectedLaptopOS.WINDOWS }
        val macCount = deduplicated.count { it.os == DetectedLaptopOS.MACOS }

        BleLaptopScanResult(
            totalCount = deduplicated.size,
            windowsCount = winCount,
            macCount = macCount,
            devices = deduplicated
        )
    }

    @SuppressLint("MissingPermission")
    private fun processScanResult(
        result: ScanResult,
        detectedLaptops: ConcurrentHashMap<String, DetectedLaptopDevice>
    ) {
        val deviceId = result.device.address
        val rssi = result.rssi

        if (rssi < -82) return // Filter out distant signals

        val record: ScanRecord = result.scanRecord ?: return
        val mfrData = record.manufacturerSpecificData

        var detectedOS: DetectedLaptopOS? = null
        var defaultName = ""

        try {
            defaultName = result.device.name ?: ""
        } catch (_: Exception) {}

        if (defaultName.isEmpty()) {
            defaultName = record.deviceName ?: ""
        }

        // 1. Check Microsoft Vendor ID (0x0006)
        val msBytes = mfrData.get(MICROSOFT_VENDOR_ID)
        if (msBytes != null && msBytes.isNotEmpty()) {
            val subType = msBytes[0].toInt() and 0xFF
            if (subType == 0x03 || subType == 0x08 || subType == 0x01) {
                detectedOS = DetectedLaptopOS.WINDOWS
                if (defaultName.isEmpty()) {
                    defaultName = "Windows Laptop"
                }
            }
        }
        // 2. Check Apple Vendor ID (0x004C)
        else {
            val appleBytes = mfrData.get(APPLE_VENDOR_ID)
            if (appleBytes != null && appleBytes.size > 2) {
                val type = appleBytes[0].toInt() and 0xFF
                if (type == 0x05 || type == 0x0C) { // AirDrop / Handoff signatures
                    detectedOS = DetectedLaptopOS.MACOS
                    if (defaultName.isEmpty()) {
                        defaultName = "MacBook / Mac"
                    }
                }
            }
        }

        if (detectedOS != null) {
            detectedLaptops[deviceId] = DetectedLaptopDevice(
                id = deviceId,
                name = defaultName,
                os = detectedOS,
                rssi = rssi
            )
        }
    }

    private fun deduplicateDevices(raw: List<DetectedLaptopDevice>): List<DetectedLaptopDevice> {
        val deduplicated = mutableListOf<DetectedLaptopDevice>()

        for (device in raw) {
            val existingIndex = deduplicated.indexOfFirst { d ->
                d.os == device.os && abs(d.rssi - device.rssi) <= 4
            }

            if (existingIndex == -1) {
                deduplicated.add(device)
            } else {
                if (device.rssi > deduplicated[existingIndex].rssi) {
                    deduplicated[existingIndex] = device
                }
            }
        }
        return deduplicated
    }
}
