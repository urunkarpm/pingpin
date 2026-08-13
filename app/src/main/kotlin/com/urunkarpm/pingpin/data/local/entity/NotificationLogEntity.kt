package com.urunkarpm.pingpin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,
    val triggeredAt: Long = System.currentTimeMillis(),
    val dateYyyyMmDd: String? = null
)
