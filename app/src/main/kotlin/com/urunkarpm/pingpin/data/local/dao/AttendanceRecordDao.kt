package com.urunkarpm.pingpin.data.local.dao

import androidx.room.*
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records WHERE dateYyyyMmDd = :dateYyyyMmDd LIMIT 1")
    suspend fun getByDate(dateYyyyMmDd: String): AttendanceRecordEntity?

    @Query("SELECT * FROM attendance_records WHERE dateYyyyMmDd LIKE :monthPrefix || '%' ORDER BY dateYyyyMmDd ASC")
    suspend fun getForMonth(monthPrefix: String): List<AttendanceRecordEntity>

    @Query("SELECT * FROM attendance_records WHERE dateYyyyMmDd LIKE :monthPrefix || '%' ORDER BY dateYyyyMmDd ASC")
    fun watchForMonth(monthPrefix: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY dateYyyyMmDd DESC")
    fun watchAll(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records ORDER BY dateYyyyMmDd DESC")
    suspend fun getAll(): List<AttendanceRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: AttendanceRecordEntity): Long

    @Query("DELETE FROM attendance_records WHERE dateYyyyMmDd = :dateYyyyMmDd")
    suspend fun deleteByDate(dateYyyyMmDd: String)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAll()
}
