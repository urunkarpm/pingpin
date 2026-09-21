package com.urunkarpm.pingpin.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.urunkarpm.pingpin.MainActivity
import com.urunkarpm.pingpin.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Quick Settings Tile Service providing 1-tap glance at today's WFO status from system shade.
 *
 * ponytail: Native Android TileService with zero dependencies or background polling.
 */
@RequiresApi(Build.VERSION_CODES.N)
class WfoTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(intent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val config = db.officeConfigDao().getConfig()
                val cal = Calendar.getInstance()
                val workingMask = config?.workingDaysMask ?: 31
                val wfoMask = config?.wfoDaysMask ?: 31
                val isWorking = WorkingDays.isWorkingDay(cal, workingMask)
                val isWfo = WorkingDays.isWfoDay(cal, wfoMask)

                val todayStr = AttendanceService.getCurrentDateYyyyMmDd()
                val todayRecord = db.attendanceRecordDao().getByDate(todayStr)

                val (label, state) = when {
                    todayRecord != null -> "PingPin • Present" to Tile.STATE_ACTIVE
                    isWorking && isWfo -> "PingPin • WFO Day" to Tile.STATE_ACTIVE
                    else -> "PingPin • WFH / Off" to Tile.STATE_INACTIVE
                }

                tile.label = label
                tile.state = state
                tile.updateTile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
