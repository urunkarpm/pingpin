package com.urunkarpm.pingpin.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "makeup_wfo_suggestions",
    indices = [Index(value = ["missedDateYyyyMmDd"], unique = true)]
)
data class MakeupWfoSuggestionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val missedDateYyyyMmDd: String,
    val suggestedDateYyyyMmDd: String,
    val status: String = "PENDING", // PENDING, ACCEPTED, DECLINED, COMPLETED, EXPIRED
    val alarmId: Int = 201,
    val createdAt: Long = System.currentTimeMillis()
)
