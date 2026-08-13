package com.urunkarpm.pingpin.data.repository

import com.urunkarpm.pingpin.data.local.dao.UserProfileDao
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class UserProfileRepository(private val dao: UserProfileDao) {

    val profileFlow: Flow<UserProfileEntity?> = dao.watchProfile()

    suspend fun getProfile(): UserProfileEntity? {
        return dao.getProfile()
    }

    suspend fun saveProfile(profile: UserProfileEntity): Long {
        return dao.insertOrUpdate(profile)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
