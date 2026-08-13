package com.urunkarpm.pingpin.data.repository

import com.urunkarpm.pingpin.data.local.dao.AttendanceRecordDao
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(private val dao: AttendanceRecordDao) {

    suspend fun getByDate(dateYyyyMmDd: String): AttendanceRecordEntity? {
        return dao.getByDate(dateYyyyMmDd)
    }

    suspend fun getForMonth(year: Int, month: Int): List<AttendanceRecordEntity> {
        val monthPrefix = String.format("%04d-%02d", year, month)
        return dao.getForMonth(monthPrefix)
    }

    fun watchForMonth(year: Int, month: Int): Flow<List<AttendanceRecordEntity>> {
        val monthPrefix = String.format("%04d-%02d", year, month)
        return dao.watchForMonth(monthPrefix)
    }

    fun watchAll(): Flow<List<AttendanceRecordEntity>> {
        return dao.watchAll()
    }

    suspend fun insertRecord(
        dateYyyyMmDd: String,
        status: String,
        markedAt: Long = System.currentTimeMillis(),
        ssidSnapshot: String? = null,
        distanceMeters: Double? = null
    ): Long {
        val record = AttendanceRecordEntity(
            dateYyyyMmDd = dateYyyyMmDd,
            status = status,
            markedAt = markedAt,
            ssidSnapshot = ssidSnapshot,
            distanceMeters = distanceMeters
        )
        return dao.insert(record)
    }

    suspend fun deleteByDate(dateYyyyMmDd: String) {
        dao.deleteByDate(dateYyyyMmDd)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
