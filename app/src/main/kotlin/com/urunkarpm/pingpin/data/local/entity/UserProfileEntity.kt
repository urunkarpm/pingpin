package com.urunkarpm.pingpin.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String = "",
    val designation: String = "",
    val photoPath: String? = null,
    val employeeId: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
