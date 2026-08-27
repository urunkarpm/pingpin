package com.urunkarpm.pingpin.service.portal

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.urunkarpm.pingpin.R
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.AttendanceRecordEntity
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.ui.portal.PortalActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class FloatingPortalService : Service(), PortalAutoCheckInEngine.PortalCallback {

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null
    private var windowParams: WindowManager.LayoutParams? = null

    private var webView: WebView? = null
    private var statusTextView: TextView? = null
    private var titleTextView: TextView? = null
    private var minimizeBtn: TextView? = null
    private var closeBtn: TextView? = null
    private var webViewContainer: FrameLayout? = null

    private var actionType: String = PortalActivity.ACTION_CHECK_IN
    private var portalUrl: String = ""
    private var alarmId: Int = -1

    private var autoRedirectCount = 0
    private var hasSubmittedLogin = false
    private val maxAutoRedirects = 3
    private var isMinimized = false

    private val handler = Handler(Looper.getMainLooper())
    private val mainScope = CoroutineScope(Dispatchers.Main)

    companion object {
        private const val TAG = "FloatingPortalService"
        private const val NOTIFICATION_ID = 9001
        private const val CHANNEL_ID = "pingpin_floating_portal"

        const val EXTRA_ACTION_TYPE = "EXTRA_ACTION_TYPE"
        const val EXTRA_PORTAL_URL = "EXTRA_PORTAL_URL"
        const val EXTRA_ALARM_ID = "EXTRA_ALARM_ID"

        fun startService(context: Context, actionType: String, portalUrl: String, alarmId: Int) {
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Overlay permission missing, cannot start FloatingPortalService")
                return
            }
            val intent = Intent(context, FloatingPortalService::class.java).apply {
                putExtra(EXTRA_ACTION_TYPE, actionType)
                putExtra(EXTRA_PORTAL_URL, portalUrl)
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, FloatingPortalService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForegroundService()
    }

    private fun startAsForegroundService() {
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PingPin Floating Portal",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            CHANNEL_ID
        } else {
            ""
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PingPin Auto Portal Active")
            .setContentText("Performing automated check-in in floating overlay...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: PortalActivity.ACTION_CHECK_IN
            portalUrl = intent.getStringExtra(EXTRA_PORTAL_URL) ?: ""
            alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        }

        if (floatingView == null) {
            initFloatingView()
            loadPortalAndAutomate()
        }

        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility", "SetJavaScriptEnabled")
    private fun initFloatingView() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.90).toInt().coerceAtMost((360 * displayMetrics.density).toInt())
        val height = (displayMetrics.heightPixels * 0.60).toInt().coerceAtMost((520 * displayMetrics.density).toInt())

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowParams = WindowManager.LayoutParams(
            width,
            height,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = (80 * displayMetrics.density).toInt()
        }

        val density = displayMetrics.density

        // Root container with dark glass theme
        val rootLayout = FrameLayout(this).apply {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * density
                setColor(Color.parseColor("#EE0F172A")) // Slate dark glass background
                setStroke((1.5f * density).toInt(), Color.parseColor("#3338BDF8")) // Cyan border
            }
            background = drawable
            elevation = 16f * density
            setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Header Bar (Draggable)
        val headerBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val hPadding = (12 * density).toInt()
            val vPadding = (8 * density).toInt()
            setPadding(hPadding, vPadding, hPadding, vPadding)
            val headerBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#221E293B"))
            }
            background = headerBg
        }

        // Drag handle indicator & title
        titleTextView = TextView(this).apply {
            text = if (actionType.equals("CHECK_IN", ignoreCase = true)) "📌 PingPin Floating Check-In" else "📌 PingPin Floating Check-Out"
            setTextColor(Color.WHITE)
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f)
        }

        minimizeBtn = TextView(this).apply {
            text = " 🗕 "
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 15f
            setPadding((8 * density).toInt(), 0, (8 * density).toInt(), 0)
            setOnClickListener { toggleMinimize() }
        }

        closeBtn = TextView(this).apply {
            text = " ✕ "
            setTextColor(Color.parseColor("#F87171"))
            textSize = 15f
            setPadding((8 * density).toInt(), 0, (8 * density).toInt(), 0)
            setOnClickListener { stopSelf() }
        }

        headerBar.addView(titleTextView)
        headerBar.addView(minimizeBtn)
        headerBar.addView(closeBtn)

        // Drag listener on Header Bar
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        headerBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams?.x ?: 0
                    initialY = windowParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    windowParams?.x = initialX + (event.rawX - initialTouchX).toInt()
                    windowParams?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(rootLayout, windowParams)
                    true
                }
                else -> false
            }
        }

        // Status Badge Row
        statusTextView = TextView(this).apply {
            text = "Initializing Floating Auto Engine..."
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 11f
            val sPaddingH = (12 * density).toInt()
            val sPaddingV = (6 * density).toInt()
            setPadding(sPaddingH, sPaddingV, sPaddingH, sPaddingV)
        }

        // WebView Container
        webViewContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
            ).apply {
                topMargin = (6 * density).toInt()
            }
            val webBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(Color.WHITE)
            }
            background = webBg
            clipToOutline = true
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = userAgentString.replace("; wv", "")
            }
            addJavascriptInterface(PortalAutoCheckInEngine.WebBridge(this@FloatingPortalService), "PingPinBridge")
        }

        webViewContainer?.addView(webView)

        mainContainer.addView(headerBar)
        mainContainer.addView(statusTextView)
        mainContainer.addView(webViewContainer)
        rootLayout.addView(mainContainer)

        floatingView = rootLayout

        try {
            windowManager?.addView(floatingView, windowParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating view: ${e.message}", e)
            stopSelf()
        }
    }

    private fun toggleMinimize() {
        val view = floatingView ?: return
        val params = windowParams ?: return
        val density = resources.displayMetrics.density

        isMinimized = !isMinimized
        if (isMinimized) {
            webViewContainer?.visibility = View.GONE
            params.width = (240 * density).toInt()
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT
            minimizeBtn?.text = " 🗖 "
        } else {
            webViewContainer?.visibility = View.VISIBLE
            val displayMetrics = resources.displayMetrics
            params.width = (displayMetrics.widthPixels * 0.90).toInt().coerceAtMost((360 * density).toInt())
            params.height = (displayMetrics.heightPixels * 0.60).toInt().coerceAtMost((520 * density).toInt())
            minimizeBtn?.text = " 🗕 "
        }
        windowManager?.updateViewLayout(view, params)
    }

    private fun loadPortalAndAutomate() {
        mainScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val config = db.officeConfigDao().getConfig()
            val credManager = PortalCredentialManager(applicationContext)
            val username = credManager.getUsername()
            val password = credManager.getPassword()

            val targetUrl = portalUrl.ifBlank { config?.portalUrl ?: "" }.trim()
            val fullTargetUrl = if (targetUrl.isNotBlank() && !targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                "https://$targetUrl"
            } else targetUrl

            val autoLogin = config?.autoLoginEnabled ?: false
            val autoCheckIn = config?.autoCheckInEnabled ?: false

            withContext(Dispatchers.Main) {
                if (fullTargetUrl.isBlank()) {
                    statusTextView?.text = "⚠️ No portal URL configured in Settings."
                    return@withContext
                }

                setupWebViewAutomation(
                    targetUrl = fullTargetUrl,
                    username = username,
                    password = password,
                    autoLogin = autoLogin,
                    autoCheckIn = autoCheckIn,
                    customCheckInKeywords = config?.customCheckInKeywords ?: "",
                    customCheckOutKeywords = config?.customCheckOutKeywords ?: ""
                )

                webView?.loadUrl(fullTargetUrl)
            }
        }
    }

    private fun setupWebViewAutomation(
        targetUrl: String,
        username: String,
        password: String,
        autoLogin: Boolean,
        autoCheckIn: Boolean,
        customCheckInKeywords: String,
        customCheckOutKeywords: String
    ) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView?.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    statusTextView?.text = "Loading portal ($newProgress%)..."
                }
            }
        }

        webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                statusTextView?.text = "Portal loaded. Injecting automation..."

                val script = PortalAutoCheckInEngine.generateAutomationScript(
                    actionType = actionType,
                    username = username,
                    password = password,
                    autoLogin = autoLogin,
                    autoPunch = autoCheckIn,
                    customCheckInKeywords = customCheckInKeywords,
                    customCheckOutKeywords = customCheckOutKeywords,
                    targetPortalUrl = targetUrl
                )
                webView?.evaluateJavascript(script, null)
            }
        }
    }

    // PortalAutoCheckInEngine.PortalCallback Implementations
    override fun onStatusUpdate(status: String) {
        handler.post {
            statusTextView?.text = status
        }
    }

    override fun onLoginSubmitted() {
        handler.post {
            hasSubmittedLogin = true
            statusTextView?.text = "🔑 Credentials auto-filled & submitted..."
        }
    }

    override fun onPunchAttempted(actionType: String) {
        handler.post {
            statusTextView?.text = "⚡ Auto-clicking $actionType button..."
        }
    }

    override fun onPunchSuccess(actionType: String) {
        handler.post {
            statusTextView?.text = "✅ $actionType SUCCESSFUL!"
            statusTextView?.setTextColor(Color.parseColor("#4ADE80")) // Light Green
            saveAttendanceLog(actionType)

            // Auto-dismiss floating window after 2.5 seconds
            handler.postDelayed({
                stopSelf()
            }, 2500)
        }
    }

    override fun onError(message: String) {
        handler.post {
            statusTextView?.text = "⚠️ $message"
            statusTextView?.setTextColor(Color.parseColor("#F87171"))
        }
    }

    private fun saveAttendanceLog(action: String) {
        mainScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getInstance(applicationContext)
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val nowTimeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

                val existing = db.attendanceRecordDao().getByDate(todayStr)
                if (existing == null) {
                    val newRecord = AttendanceRecordEntity(
                        dateYyyyMmDd = todayStr,
                        status = "present"
                    )
                    db.attendanceRecordDao().insert(newRecord)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val actionLabel = if (action.equals("CHECK_IN", ignoreCase = true)) "Check In" else "Check Out"

                val notification = NotificationCompat.Builder(applicationContext, NotificationService.ATTENDANCE_CHANNEL_ID)
                    .setContentTitle("PingPin Auto $actionLabel Complete")
                    .setContentText("$actionLabel successfully recorded at $nowTimeStr via Floating Auto Portal.")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save attendance record: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            if (floatingView != null && windowManager != null) {
                windowManager?.removeView(floatingView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating view: ${e.message}", e)
        }
        webView?.destroy()
        floatingView = null
        webView = null
    }
}
