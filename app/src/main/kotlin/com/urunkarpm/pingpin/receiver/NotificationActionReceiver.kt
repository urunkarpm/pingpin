package com.urunkarpm.pingpin.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.urunkarpm.pingpin.MainActivity
import com.urunkarpm.pingpin.service.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotifActionReceiver"

        const val ACTION_SNOOZE = "com.urunkarpm.pingpin.ACTION_SNOOZE"
        const val ACTION_OPEN_PORTAL = "com.urunkarpm.pingpin.ACTION_OPEN_PORTAL"
        const val ACTION_DISMISS = "com.urunkarpm.pingpin.ACTION_DISMISS"

        const val EXTRA_ALARM_ID = "alarmId"
        const val EXTRA_PORTAL_URL = "portalUrl"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, 101)
        val portalUrl = intent.getStringExtra(EXTRA_PORTAL_URL) ?: ""

        Log.d(TAG, "Notification action received: $action (alarmId=$alarmId, portalUrl=$portalUrl)")

        val notifService = NotificationService(context)

        when (action) {
            ACTION_SNOOZE -> {
                notifService.dismissNotification(alarmId)
                notifService.snoozeAlarm(alarmId = alarmId, durationMins = 10, portalUrl = portalUrl)
                Log.d(TAG, "Snoozed alarm $alarmId for 10 minutes via notification button.")
            }
            ACTION_OPEN_PORTAL -> {
                notifService.dismissNotification(alarmId)
                openPortal(context, portalUrl, alarmId)
            }
            ACTION_DISMISS -> {
                notifService.dismissNotification(alarmId)
                Log.d(TAG, "Dismissed notification $alarmId.")
            }
        }
    }

    private fun openPortal(context: Context, portalUrl: String, alarmId: Int = 101) {
        val pendingResult = goAsync()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.urunkarpm.pingpin.data.local.AppDatabase.getInstance(context.applicationContext)
                val config = db.officeConfigDao().getConfig()
                val portalMode = config?.portalMode ?: "EXTERNAL_BROWSER"
                val rawUrl = portalUrl.ifBlank { config?.portalUrl ?: "" }.trim()
                val urlToOpen = if (rawUrl.isNotBlank()) {
                    if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                        "https://$rawUrl"
                    } else rawUrl
                } else "https://google.com"

                val actionType = if (alarmId == NotificationService.CHECK_OUT_ALARM_ID || alarmId == NotificationService.CHECK_OUT_SNOOZE_ID) {
                    com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_OUT
                } else {
                    com.urunkarpm.pingpin.ui.portal.PortalActivity.ACTION_CHECK_IN
                }

                val useFloating = config?.useFloatingPortal ?: true

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (portalMode == "IN_APP_AUTO") {
                        if (useFloating && android.provider.Settings.canDrawOverlays(context)) {
                            com.urunkarpm.pingpin.service.portal.FloatingPortalService.startService(
                                context = context,
                                actionType = actionType,
                                portalUrl = urlToOpen,
                                alarmId = alarmId
                            )
                        } else {
                            val portalIntent = com.urunkarpm.pingpin.ui.portal.PortalActivity.createIntent(
                                context = context,
                                actionType = actionType,
                                portalUrl = urlToOpen,
                                alarmId = alarmId
                            ).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            }
                            context.startActivity(portalIntent)
                        }
                    } else {
                        launchExternalBrowser(context, urlToOpen)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open portal: ${e.message}", e)
                launchExternalBrowser(context, portalUrl)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun launchExternalBrowser(context: Context, url: String) {
        try {
            val rawUrl = url.trim().ifBlank { "https://google.com" }
            val urlToOpen = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) "https://$rawUrl" else rawUrl
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val options = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                android.app.ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }.toBundle()
            } else null

            if (options != null) {
                context.startActivity(intent, options)
            } else {
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch external browser: ${e.message}", e)
            val fallbackIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(fallbackIntent)
        }
    }
}
