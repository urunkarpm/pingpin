package com.urunkarpm.pingpin.ui.portal

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import android.app.KeyguardManager
import android.os.Build
import android.view.WindowManager
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.service.AttendanceService
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.service.portal.PortalAutoCheckInEngine
import com.urunkarpm.pingpin.service.portal.PortalCredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PortalActivity : ComponentActivity(), PortalAutoCheckInEngine.PortalCallback {

    private var webViewRef: WebView? = null
    private var actionType: String = ACTION_CHECK_IN
    private var targetPortalUrl: String = ""
    private var alarmId: Int = -1

    private val statusMessageState = mutableStateOf("Initializing Portal...")
    private val isLoadingState = mutableStateOf(true)

    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        pendingGeoCallback?.invoke(pendingGeoOrigin, granted, true)
        pendingGeoOrigin = null
        pendingGeoCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndKeyguard()

        actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_CHECK_IN
        targetPortalUrl = intent.getStringExtra(EXTRA_PORTAL_URL) ?: ""
        alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)

        setContent {
            PortalScreenContent()
        }

        loadConfigAndInit()
    }

    private fun turnScreenOnAndKeyguard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    private fun loadConfigAndInit() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val config = db.officeConfigDao().getConfig()
            var url = targetPortalUrl

            if (url.isBlank() && config != null) {
                url = config.portalUrl
            }
            if (url.isBlank()) {
                url = "https://google.com"
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            targetPortalUrl = url
            val autoLogin = config?.autoLoginEnabled ?: false
            val autoCheckInConfigured = config?.autoCheckInEnabled ?: false
            val officeSsid = config?.ssid ?: ""
            val wifiService = com.urunkarpm.pingpin.service.WifiService(applicationContext)
            val isConnectedToOffice = if (officeSsid.isNotBlank()) wifiService.isConnectedToSSID(officeSsid) else false

            val isTestMode = intent.getBooleanExtra(EXTRA_IS_TEST_MODE, false)
            val safeAutoPunch = (autoCheckInConfigured && isConnectedToOffice) || isTestMode
            val customCheckInKeywords = config?.customCheckInKeywords ?: ""
            val customCheckOutKeywords = config?.customCheckOutKeywords ?: ""

            withContext(Dispatchers.Main) {
                initWebView(
                    autoLogin = autoLogin,
                    autoCheckIn = safeAutoPunch,
                    customCheckInKeywords = customCheckInKeywords,
                    customCheckOutKeywords = customCheckOutKeywords,
                    isConnectedToOffice = isConnectedToOffice || isTestMode,
                    officeSsid = officeSsid,
                    autoCheckInConfigured = autoCheckInConfigured
                )
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(
        autoLogin: Boolean,
        autoCheckIn: Boolean,
        customCheckInKeywords: String,
        customCheckOutKeywords: String,
        isConnectedToOffice: Boolean,
        officeSsid: String,
        autoCheckInConfigured: Boolean
    ) {
        val webView = webViewRef ?: return
        val credManager = PortalCredentialManager(this)
        val username = credManager.getUsername()
        val password = credManager.getPassword()

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            setGeolocationDatabasePath(filesDir.path)
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = webView.settings.userAgentString + " PingPinApp/1.5"
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }

        webView.addJavascriptInterface(
            PortalAutoCheckInEngine.WebBridge(this),
            "PingPinBridge"
        )

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoadingState.value = false
                if (autoCheckInConfigured && !isConnectedToOffice) {
                    statusMessageState.value = "⚠️ Not connected to Office Wi-Fi ($officeSsid). Auto-punch paused for safety."
                } else {
                    statusMessageState.value = "Page loaded. Running check-in engine..."
                }

                val script = PortalAutoCheckInEngine.generateAutomationScript(
                    actionType = actionType,
                    username = username,
                    password = password,
                    autoLogin = autoLogin,
                    autoPunch = autoCheckIn,
                    customCheckInKeywords = customCheckInKeywords,
                    customCheckOutKeywords = customCheckOutKeywords
                )
                view?.evaluateJavascript(script, null)
            }

            @Deprecated("Deprecated in API 23")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                isLoadingState.value = false
                statusMessageState.value = "⚠️ Portal Connection Error: ${description ?: "Network issue"}"
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    isLoadingState.value = true
                    statusMessageState.value = "Loading portal... $newProgress%"
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissions.Callback?
            ) {
                if (origin == null || callback == null) return
                val hasFine = ContextCompat.checkSelfPermission(
                    this@PortalActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    this@PortalActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    callback.invoke(origin, true, true)
                } else {
                    pendingGeoOrigin = origin
                    pendingGeoCallback = callback
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }

        webView.loadUrl(targetPortalUrl)
    }

    private fun triggerManualScriptRun() {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val config = db.officeConfigDao().getConfig()
            val credManager = PortalCredentialManager(this@PortalActivity)
            val wifiService = com.urunkarpm.pingpin.service.WifiService(applicationContext)
            val officeSsid = config?.ssid ?: ""
            val isConnectedToOffice = if (officeSsid.isNotBlank()) wifiService.isConnectedToSSID(officeSsid) else false

            val script = PortalAutoCheckInEngine.generateAutomationScript(
                actionType = actionType,
                username = credManager.getUsername(),
                password = credManager.getPassword(),
                autoLogin = config?.autoLoginEnabled ?: false,
                autoPunch = true,
                customCheckInKeywords = config?.customCheckInKeywords ?: "",
                customCheckOutKeywords = config?.customCheckOutKeywords ?: ""
            )

            withContext(Dispatchers.Main) {
                if (!isConnectedToOffice && officeSsid.isNotBlank()) {
                    statusMessageState.value = "⚠️ Device is not on Office Wi-Fi ($officeSsid). Running manual punch trigger..."
                } else {
                    statusMessageState.value = "Retrying automation script..."
                }
                webViewRef?.evaluateJavascript(script, null)
            }
        }
    }

    private fun openExternalBrowser() {
        val url = targetPortalUrl.ifBlank { "https://google.com" }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open external browser", Toast.LENGTH_SHORT).show()
        }
    }

    // --- PortalAutoCheckInEngine.PortalCallback Implementation ---

    override fun onStatusUpdate(status: String) {
        runOnUiThread {
            statusMessageState.value = status
        }
    }

    override fun onLoginSubmitted() {
        runOnUiThread {
            statusMessageState.value = "Login submitted! Waiting for portal dashboard..."
        }
    }

    override fun onPunchAttempted(actionType: String) {
        runOnUiThread {
            statusMessageState.value = "Attempting auto-punch for $actionType..."
        }
    }

    override fun onPunchSuccess(actionType: String) {
        runOnUiThread {
            val displayAction = if (actionType.equals("CHECK_IN", ignoreCase = true)) "Check In" else "Check Out"
            statusMessageState.value = "🎉 $displayAction recorded! Syncing session with portal (closing in 6s)..."
            Toast.makeText(this@PortalActivity, "$displayAction recorded!", Toast.LENGTH_LONG).show()

            // Record attendance in PingPin database
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(applicationContext)
                    val repo = com.urunkarpm.pingpin.data.repository.AttendanceRepository(db.attendanceRecordDao())
                    val today = AttendanceService.getCurrentDateYyyyMmDd()
                    val status = if (actionType.equals("CHECK_IN", ignoreCase = true)) "present" else "checked_out"
                    repo.insertRecord(dateYyyyMmDd = today, status = status)

                    // Dismiss alarm if applicable
                    if (alarmId != -1) {
                        val notifService = NotificationService(this@PortalActivity)
                        notifService.dismissNotification(alarmId)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Auto finish after 6s delay to allow pending background location/API network calls to finish on portal server
                kotlinx.coroutines.delay(6000)
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            statusMessageState.value = "Note: $message"
        }
    }

    // --- UI Layout ---

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun PortalScreenContent() {
        val emeraldGreen = Color(0xFF10B981)
        val darkBg = Color(0xFF0F172A)
        val cardBg = Color(0xFF1E293B)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "HR Portal Viewer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (actionType.equals("CHECK_IN", ignoreCase = true)) "Action: Check In" else "Action: Check Out",
                                fontSize = 11.sp,
                                color = emeraldGreen
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { finish() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { openExternalBrowser() }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Chrome", tint = Color.White)
                        }
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = darkBg
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = darkBg,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { triggerManualScriptRun() },
                            colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-Run Auto Punch", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        TextButton(onClick = { openExternalBrowser() }) {
                            Text("Open in Chrome", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(darkBg)
            ) {
                // Status Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoadingState.value) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = emeraldGreen,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = statusMessageState.value,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // WebView Container
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    companion object {
        const val EXTRA_ACTION_TYPE = "extra_action_type"
        const val EXTRA_PORTAL_URL = "extra_portal_url"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_IS_TEST_MODE = "extra_is_test_mode"

        const val ACTION_CHECK_IN = "CHECK_IN"
        const val ACTION_CHECK_OUT = "CHECK_OUT"

        fun createIntent(
            context: Context,
            actionType: String = ACTION_CHECK_IN,
            portalUrl: String = "",
            alarmId: Int = -1,
            isTestMode: Boolean = false
        ): Intent {
            return Intent(context, PortalActivity::class.java).apply {
                putExtra(EXTRA_ACTION_TYPE, actionType)
                putExtra(EXTRA_PORTAL_URL, portalUrl)
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_IS_TEST_MODE, isTestMode)
            }
        }
    }
}
