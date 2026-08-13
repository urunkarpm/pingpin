package com.urunkarpm.pingpin.data.local.dao

import androidx.room.*
import com.urunkarpm.pingpin.data.local.entity.NotificationLogEntity

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs WHERE type = :type ORDER BY triggeredAt DESC")
    suspend fun getByType(type: String): List<NotificationLogEntity>

    @Query("SELECT triggeredAt FROM notification_logs WHERE type = :type ORDER BY triggeredAt DESC LIMIT 1")
    suspend fun getLastTriggeredTime(type: String): Long?

    @Insert
    suspend fun insert(log: NotificationLogEntity): Long

    @Query("DELETE FROM notification_logs")
    suspend fun deleteAll()
}
