package com.urunkarpm.pingpin.data.repository

import com.urunkarpm.pingpin.data.local.dao.OfficeConfigDao
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import kotlinx.coroutines.flow.Flow

class OfficeConfigRepository(private val dao: OfficeConfigDao) {

    val configFlow: Flow<OfficeConfigEntity?> = dao.watchConfig()

    suspend fun getConfig(): OfficeConfigEntity? {
        return dao.getConfig()
    }

    suspend fun saveConfig(config: OfficeConfigEntity): Long {
        return dao.insertOrUpdate(config)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
