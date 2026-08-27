package com.urunkarpm.pingpin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "office_configs")
data class OfficeConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ssid: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Int = 100,
    val lateCutoffTime: String = "10:30",
    val checkInTime: String = "09:30",
    val checkOutTime: String = "17:30",
    val portalUrl: String = "",
    val workingDaysMask: Int = 31,     // Mon-Fri default (1|2|4|8|16)
    val wfoDaysMask: Int = 31,         // Mon-Fri default WFO
    val portalMode: String = "EXTERNAL_BROWSER", // "IN_APP_AUTO" vs "EXTERNAL_BROWSER"
    val autoLoginEnabled: Boolean = false,
    val autoCheckInEnabled: Boolean = false,
    val portalPreset: String = "GENERIC",
    val customCheckInKeywords: String = "",
    val customCheckOutKeywords: String = "",
    val useFloatingPortal: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
