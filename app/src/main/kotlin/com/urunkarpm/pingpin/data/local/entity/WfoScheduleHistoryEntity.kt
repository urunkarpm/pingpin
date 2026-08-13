package com.urunkarpm.pingpin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wfo_schedule_history")
data class WfoScheduleHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val wfoDaysMask: Int,
    val effectiveFrom: Long
)
