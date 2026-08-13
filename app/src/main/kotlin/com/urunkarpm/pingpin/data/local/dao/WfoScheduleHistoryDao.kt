package com.urunkarpm.pingpin.data.local.dao

import androidx.room.*
import com.urunkarpm.pingpin.data.local.entity.WfoScheduleHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WfoScheduleHistoryDao {
    @Query("SELECT * FROM wfo_schedule_history ORDER BY effectiveFrom ASC")
    suspend fun getHistory(): List<WfoScheduleHistoryEntity>

    @Query("SELECT * FROM wfo_schedule_history ORDER BY effectiveFrom ASC")
    fun watchHistory(): Flow<List<WfoScheduleHistoryEntity>>

    @Insert
    suspend fun insert(entry: WfoScheduleHistoryEntity): Long

    @Query("DELETE FROM wfo_schedule_history")
    suspend fun deleteAll()
}
