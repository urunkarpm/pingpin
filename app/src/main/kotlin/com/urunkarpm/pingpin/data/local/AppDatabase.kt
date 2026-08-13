package com.urunkarpm.pingpin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.urunkarpm.pingpin.data.local.dao.*
import com.urunkarpm.pingpin.data.local.entity.*

@Database(
    entities = [
        OfficeConfigEntity::class,
        AttendanceRecordEntity::class,
        UserProfileEntity::class,
        NotificationLogEntity::class,
        WfoScheduleHistoryEntity::class,
        MakeupWfoSuggestionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun officeConfigDao(): OfficeConfigDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun notificationLogDao(): NotificationLogDao
    abstract fun wfoScheduleHistoryDao(): WfoScheduleHistoryDao
    abstract fun makeupWfoSuggestionDao(): MakeupWfoSuggestionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pingpin.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
