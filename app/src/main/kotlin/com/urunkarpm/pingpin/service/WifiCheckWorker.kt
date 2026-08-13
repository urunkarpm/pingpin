package com.urunkarpm.pingpin.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.repository.AttendanceRepository
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import java.util.concurrent.TimeUnit

class WifiCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "WifiCheckWorker"
        const val WORK_NAME = "pingpin_wifi_periodic_task"

        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<WifiCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Periodic WorkManager Wi-Fi check scheduled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val officeConfigRepo = OfficeConfigRepository(db.officeConfigDao())
            val attendanceRepo = AttendanceRepository(db.attendanceRecordDao())

            val config = officeConfigRepo.getConfig()
            if (config != null) {
                val wifiService = WifiService(applicationContext)
                val attendanceService = AttendanceService(applicationContext, wifiService)
                val notificationService = NotificationService(applicationContext)

                attendanceService.checkAndMarkAttendance(
                    officeConfig = config,
                    attendanceRepo = attendanceRepo,
                    onAttendanceMarked = {
                        notificationService.showAttendanceSuccessNotification()
                    }
                )
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Background Wi-Fi check worker error", e)
            Result.retry()
        }
    }
}
