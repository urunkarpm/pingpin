package com.urunkarpm.pingpin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    indices = [Index(value = ["dateYyyyMmDd"], unique = true)]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateYyyyMmDd: String, // Format: YYYY-MM-DD
    val status: String,        // "present" or "late"
    val markedAt: Long = System.currentTimeMillis(),
    val ssidSnapshot: String? = null,
    val distanceMeters: Double? = null
)
