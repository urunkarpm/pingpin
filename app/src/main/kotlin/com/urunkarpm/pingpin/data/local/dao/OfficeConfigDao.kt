package com.urunkarpm.pingpin.data.local.dao

import androidx.room.*
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeConfigDao {
    @Query("SELECT * FROM office_configs LIMIT 1")
    fun watchConfig(): Flow<OfficeConfigEntity?>

    @Query("SELECT * FROM office_configs LIMIT 1")
    suspend fun getConfig(): OfficeConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: OfficeConfigEntity): Long

    @Query("DELETE FROM office_configs")
    suspend fun deleteAll()
}
