package com.urunkarpm.pingpin.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.urunkarpm.pingpin.data.local.dao.*
import com.urunkarpm.pingpin.data.local.entity.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Migration from 1 to 2 if needed
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wfo_schedule_history` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `wfoDaysMask` INTEGER NOT NULL,
                `effectiveFrom` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `makeup_wfo_suggestions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `missedDateYyyyMmDd` TEXT NOT NULL,
                `suggestedDateYyyyMmDd` TEXT NOT NULL,
                `status` TEXT NOT NULL DEFAULT 'PENDING',
                `alarmId` INTEGER NOT NULL DEFAULT 201,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_makeup_wfo_suggestions_missedDateYyyyMmDd`
            ON `makeup_wfo_suggestions` (`missedDateYyyyMmDd`)
            """.trimIndent()
        )
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `office_configs` ADD COLUMN `portalMode` TEXT NOT NULL DEFAULT 'EXTERNAL_BROWSER'")
        db.execSQL("ALTER TABLE `office_configs` ADD COLUMN `autoLoginEnabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `office_configs` ADD COLUMN `autoCheckInEnabled` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `office_configs` ADD COLUMN `portalPreset` TEXT NOT NULL DEFAULT 'GENERIC'")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `office_configs` ADD COLUMN `customCheckInKeywords` TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE `office_configs` ADD COLUMN `customCheckOutKeywords` TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [
        OfficeConfigEntity::class,
        AttendanceRecordEntity::class,
        UserProfileEntity::class,
        NotificationLogEntity::class,
        WfoScheduleHistoryEntity::class,
        MakeupWfoSuggestionEntity::class
    ],
    version = 6,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
