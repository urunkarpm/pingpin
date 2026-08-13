package com.urunkarpm.pingpin.data.local.dao

import androidx.room.*
import com.urunkarpm.pingpin.data.local.entity.MakeupWfoSuggestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MakeupWfoSuggestionDao {
    @Query("SELECT * FROM makeup_wfo_suggestions WHERE status IN ('PENDING', 'ACCEPTED') ORDER BY createdAt DESC LIMIT 1")
    fun watchActiveSuggestion(): Flow<MakeupWfoSuggestionEntity?>

    @Query("SELECT * FROM makeup_wfo_suggestions WHERE status IN ('PENDING', 'ACCEPTED') ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveSuggestion(): MakeupWfoSuggestionEntity?

    @Query("SELECT * FROM makeup_wfo_suggestions WHERE missedDateYyyyMmDd = :missedDate LIMIT 1")
    suspend fun getByMissedDate(missedDate: String): MakeupWfoSuggestionEntity?

    @Query("SELECT * FROM makeup_wfo_suggestions WHERE suggestedDateYyyyMmDd = :suggestedDate AND status = 'ACCEPTED' LIMIT 1")
    suspend fun getAcceptedForDate(suggestedDate: String): MakeupWfoSuggestionEntity?

    @Query("SELECT * FROM makeup_wfo_suggestions ORDER BY createdAt DESC")
    suspend fun getAll(): List<MakeupWfoSuggestionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(suggestion: MakeupWfoSuggestionEntity): Long

    @Query("UPDATE makeup_wfo_suggestions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("DELETE FROM makeup_wfo_suggestions")
    suspend fun deleteAll()
}
