package com.urunkarpm.pingpin.data.repository

import com.urunkarpm.pingpin.data.local.dao.MakeupWfoSuggestionDao
import com.urunkarpm.pingpin.data.local.entity.MakeupWfoSuggestionEntity
import kotlinx.coroutines.flow.Flow

class MakeupWfoRepository(private val dao: MakeupWfoSuggestionDao) {

    val activeSuggestionFlow: Flow<MakeupWfoSuggestionEntity?> = dao.watchActiveSuggestion()
    val acceptedDatesFlow: Flow<List<String>> = dao.watchAcceptedDates()

    suspend fun getActiveSuggestion(): MakeupWfoSuggestionEntity? {
        return dao.getActiveSuggestion()
    }

    suspend fun getByMissedDate(missedDate: String): MakeupWfoSuggestionEntity? {
        return dao.getByMissedDate(missedDate)
    }

    suspend fun insertSuggestion(suggestion: MakeupWfoSuggestionEntity): Long {
        return dao.insert(suggestion)
    }

    suspend fun updateStatus(id: Int, status: String) {
        dao.updateStatus(id, status)
    }

    suspend fun getAll(): List<MakeupWfoSuggestionEntity> {
        return dao.getAll()
    }
}
