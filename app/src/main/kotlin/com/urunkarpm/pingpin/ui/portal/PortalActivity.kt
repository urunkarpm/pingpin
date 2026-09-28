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
import com.urunkarpm.pingpin.service.AlarmSoundService
import com.urunkarpm.pingpin.service.NotificationService
import com.urunkarpm.pingpin.service.portal.PortalAutoCheckInEngine
import com.urunkarpm.pingpin.ui.theme.optimizeDisplayRefreshRate
import com.urunkarpm.pingpin.service.portal.PortalCredentialManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.GeolocationPermissions
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PortalActivity : ComponentActivity(), PortalAutoCheckInEngine.PortalCallback {

    private var webViewRef: WebView? = null
    private var actionType: String = ACTION_CHECK_IN
    private var targetPortalUrl: String = ""
    private var alarmId: Int = -1

    private val statusMessageState = mutableStateOf("Initializing Portal...")
    private val isLoadingState = mutableStateOf(true)
    private val currentUrlState = mutableStateOf("")
    private var autoRedirectCount = 0
    private var hasSubmittedLogin = false
    private val maxAutoRedirects = 3
    private val redirectCauseState = mutableStateOf<String?>(null)
    private val automationLogs = mutableStateListOf<String>()

    private fun addLog(message: String) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            automationLogs.add("[$time] $message")
            if (automationLogs.size > 80) {
                automationLogs.removeAt(0)
            }
        } else {
            runOnUiThread { addLog(message) }
        }
    }

    private fun setStatus(message: String, alsoLog: Boolean = true) {
        runOnUiThread {
            statusMessageState.value = message
            if (alsoLog) {
                addLog(message)
            }
        }
    }

    private fun isAuthUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("login") || lower.contains("auth") || lower.contains("signin") || lower.contains("sso")
    }

    private fun inspectRedirectCause(view: WebView?, loadedUrl: String) {
        val cleanUrl = cleanUrlForDisplay(loadedUrl)
        val urlLower = loadedUrl.lowercase()

        val urlCauseHint = when {
            urlLower.contains("session") && (urlLower.contains("expire") || urlLower.contains("timeout")) -> "Session Expired / Timeout"
            urlLower.contains("denied") || urlLower.contains("unauthorized") || urlLower.contains("forbidden") -> "Access Denied / Unauthorized"
            urlLower.contains("invalid") -> "Invalid Session / Credentials"
            urlLower.contains("returnurl") || urlLower.contains("redirect") -> "Re-authentication required (ReturnUrl parameter detected)"
            urlLower.contains("error") -> "Portal error page"
            else -> null
        }

        val pageTitle = view?.title?.takeIf { it.isNotBlank() && !it.startsWith("http") } ?: ""

        val detectCauseScript = """
            (function() {
                try {
                    var title = document.title || '';
                    var errorEl = document.querySelector('.error, .alert, #error, #message, [role="alert"], h1, h2, p.error-text');
                    var textHint = errorEl ? (errorEl.innerText || errorEl.textContent || '').trim() : '';
                    if (textHint.length > 120) textHint = textHint.substring(0, 120) + '...';
                    return JSON.stringify({ title: title, textHint: textHint });
                } catch(e) {
                    return JSON.stringify({ title: '', textHint: '' });
                }
            })();
        """.trimIndent()

        view?.evaluateJavascript(detectCauseScript) { resultJson ->
            var domTitle = pageTitle
            var domHint = ""

            try {
                if (!resultJson.isNullOrBlank() && resultJson != "null") {
                    var raw = resultJson.trim()
                    if (raw.startsWith("\"") && raw.endsWith("\"")) {
                        raw = raw.substring(1, raw.length - 1)
                            .replace("\\\"", "\"")
                            .replace("\\\\", "\\")
                    }
                    val jsonObj = org.json.JSONObject(raw)
                    val t = jsonObj.optString("title")
                    val h = jsonObj.optString("textHint")
                    if (t.isNotBlank()) domTitle = t
                    if (h.isNotBlank()) domHint = h
                }
            } catch (e: Exception) {
                // Ignore parse error
            }

            val finalCause = when {
                domHint.isNotBlank() -> domHint
                urlCauseHint != null -> "$urlCauseHint ($cleanUrl)"
                domTitle.isNotBlank() -> "Landing Page: \"$domTitle\" ($cleanUrl)"
                else -> "Server continuously redirected to $cleanUrl"
            }

            runOnUiThread {
                redirectCauseState.value = finalCause
                statusMessageState.value = "⚠️ Redirected 3x away from target URL. Cause: $finalCause"
            }
        }
    }

    private var pendingGeoOrigin: String? = null
    private var pendingGeoCallback: GeolocationPermissions.Callback? = null

    private fun normalizeUrl(rawUrl: String): String {
        var u = rawUrl.trim().lowercase()
        if (u.startsWith("https://")) u = u.substring(8)
        else if (u.startsWith("http://")) u = u.substring(7)
        u = u.trimEnd('/')
        val hashIdx = u.indexOf('#')
        if (hashIdx != -1) u = u.substring(0, hashIdx)
        val queryIdx = u.indexOf('?')
        if (queryIdx != -1) u = u.substring(0, queryIdx)
        return u
    }

    private fun isUrlMatching(openedUrl: String, targetUrl: String): Boolean {
        if (openedUrl.isBlank() || targetUrl.isBlank()) return true
        val normOpened = normalizeUrl(openedUrl)
        val normTarget = normalizeUrl(targetUrl)
        if (normOpened.isEmpty() || normTarget.isEmpty()) return true
        return normOpened == normTarget || normOpened.startsWith(normTarget) || normTarget.startsWith(normOpened)
    }

    private fun cleanUrlForDisplay(url: String): String {
        if (url.isBlank()) return ""
        return url.replace("https://", "").replace("http://", "").trimEnd('/')
    }

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
        optimizeDisplayRefreshRate()
        turnScreenOnAndKeyguard()

        actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_CHECK_IN
        targetPortalUrl = intent.getStringExtra(EXTRA_PORTAL_URL) ?: ""
        alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        val displayAction = if (actionType.equals(ACTION_CHECK_IN, ignoreCase = true)) "Check In" else "Check Out"
        addLog("🚀 Initialized Portal Viewer for $displayAction")

        if (alarmId != -1) {
            AlarmSoundService.stopAlarmSound(this)
        }

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

    private data class ConfigBundle(
        val autoLogin: Boolean,
        val autoCheckIn: Boolean,
        val customCheckInKeywords: String,
        val customCheckOutKeywords: String,
        val targetUrl: String
    )

    private var cachedConfig: ConfigBundle? = null
    private var isWebViewConfigured = false

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
            val isTestMode = intent.getBooleanExtra(EXTRA_IS_TEST_MODE, false)
            val safeAutoPunch = autoCheckInConfigured || isTestMode
            val customCheckInKeywords = config?.customCheckInKeywords ?: ""
            val customCheckOutKeywords = config?.customCheckOutKeywords ?: ""

            val bundle = ConfigBundle(
                autoLogin = autoLogin,
                autoCheckIn = safeAutoPunch,
                customCheckInKeywords = customCheckInKeywords,
                customCheckOutKeywords = customCheckOutKeywords,
                targetUrl = targetPortalUrl
            )

            withContext(Dispatchers.Main) {
                cachedConfig = bundle
                webViewRef?.let { webView ->
                    initWebView(webView, bundle)
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(
        webView: WebView,
        config: ConfigBundle
    ) {
        if (isWebViewConfigured) return
        isWebViewConfigured = true

        val credManager = PortalCredentialManager(this)
        val username = credManager.getUsername()
        val password = credManager.getPassword()

        webView.setBackgroundColor(android.graphics.Color.WHITE)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            @Suppress("DEPRECATION")
            setGeolocationDatabasePath(filesDir.path)
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            textZoom = 100
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
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
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                val loadedUrl = url ?: view?.url ?: ""
                currentUrlState.value = loadedUrl
                if (loadedUrl.isNotBlank()) {
                    addLog("🌐 Loading: ${cleanUrlForDisplay(loadedUrl)}")
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoadingState.value = false
                val loadedUrl = url ?: view?.url ?: ""
                currentUrlState.value = loadedUrl

                val matches = isUrlMatching(loadedUrl, targetPortalUrl)
                val isAuth = isAuthUrl(loadedUrl)
                val displayAction = if (actionType.equals(ACTION_CHECK_IN, ignoreCase = true)) "Check In" else "Check Out"

                if (!matches && loadedUrl.isNotBlank()) {
                    if (isAuth && !hasSubmittedLogin) {
                        setStatus("🔒 Login page detected. Attempting auto-login...")
                    } else if (autoRedirectCount < maxAutoRedirects) {
                        autoRedirectCount++
                        setStatus("Redirecting to target portal URL for $displayAction (Attempt $autoRedirectCount/$maxAutoRedirects)...")
                        view?.loadUrl(targetPortalUrl)
                        return
                    } else {
                        setStatus("Inspecting redirect cause on 3rd attempt...")
                        inspectRedirectCause(view, loadedUrl)
                    }
                } else {
                    autoRedirectCount = 0
                    redirectCauseState.value = null
                    setStatus("Target portal loaded. Running $displayAction engine...")
                }

                val script = PortalAutoCheckInEngine.generateAutomationScript(
                    actionType = actionType,
                    username = username,
                    password = password,
                    autoLogin = config.autoLogin,
                    autoPunch = config.autoCheckIn,
                    customCheckInKeywords = config.customCheckInKeywords,
                    customCheckOutKeywords = config.customCheckOutKeywords,
                    targetPortalUrl = targetPortalUrl
                )
                view?.evaluateJavascript(script) { evalResult ->
                    if (evalResult != null && evalResult != "null" && evalResult.isNotBlank()) {
                        android.util.Log.d("PortalActivity", "JS Eval Result: $evalResult")
                    }
                }
            }

            @Deprecated("Deprecated in API 23")
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                isLoadingState.value = false
                setStatus("⚠️ Portal Connection Error: ${description ?: "Network issue"}")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    isLoadingState.value = true
                    statusMessageState.value = "Loading portal... $newProgress%"
                }
            }

            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val msg = it.message()
                    if (it.messageLevel() == android.webkit.ConsoleMessage.MessageLevel.ERROR) {
                        addLog("🔴 JS Error: $msg")
                    } else if (msg.startsWith("[PingPin]")) {
                        addLog(msg.removePrefix("[PingPin] ").trim())
                    }
                }
                return super.onConsoleMessage(consoleMessage)
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

            val script = PortalAutoCheckInEngine.generateAutomationScript(
                actionType = actionType,
                username = credManager.getUsername(),
                password = credManager.getPassword(),
                autoLogin = config?.autoLoginEnabled ?: false,
                autoPunch = true,
                customCheckInKeywords = config?.customCheckInKeywords ?: "",
                customCheckOutKeywords = config?.customCheckOutKeywords ?: "",
                targetPortalUrl = targetPortalUrl
            )

            withContext(Dispatchers.Main) {
                setStatus("🔄 Re-running automation script manually...")
                webViewRef?.evaluateJavascript("window.__pingpin_automation_active = false;\n" + script) { evalResult ->
                    if (evalResult != null && evalResult != "null" && evalResult.isNotBlank()) {
                        android.util.Log.d("PortalActivity", "Manual JS Eval Result: $evalResult")
                    }
                }
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
            addLog(status)
        }
    }

    override fun onLoginSubmitted() {
        runOnUiThread {
            hasSubmittedLogin = true
            val msg = "🔑 Login credentials auto-filled & submitted!"
            statusMessageState.value = msg
            addLog(msg)
        }
    }

    override fun onPunchAttempted(actionType: String) {
        // ponytail: Exact matched element, selector, and keyword are already delivered with high fidelity via onStatusUpdate.
    }

    override fun onPunchSuccess(actionType: String) {
        runOnUiThread {
            val displayAction = if (actionType.equals("CHECK_IN", ignoreCase = true)) "Check In" else "Check Out"
            val msg = "🎉 $displayAction recorded on portal! (closing in 6s)..."
            statusMessageState.value = msg
            addLog(msg)
            Toast.makeText(this@PortalActivity, "$displayAction recorded!", Toast.LENGTH_LONG).show()

            // Dismiss alarm if applicable
            if (alarmId != -1) {
                AlarmSoundService.stopAlarmSound(this@PortalActivity)
                val notifService = NotificationService(this@PortalActivity)
                notifService.dismissNotification(alarmId)
            }

            // Auto finish after 6s delay to allow pending background location/API network calls to finish on portal server
            lifecycleScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(6000)
                finish()
            }
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            val msg = "⚠️ $message"
            statusMessageState.value = msg
            addLog(msg)
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
                // Status Banner & Diagnostics Console
                var showAutomationLogs by rememberSaveable { mutableStateOf(true) }
                val isProblemState = statusMessageState.value.contains("⚠️") || statusMessageState.value.contains("❌")

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (isLoadingState.value) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(16.dp),
                                        color = emeraldGreen,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = statusMessageState.value,
                                    color = if (isProblemState) Color(0xFFFCA5A5) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )
                            }

                            if (automationLogs.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    onClick = { showAutomationLogs = !showAutomationLogs },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (showAutomationLogs || isProblemState) emeraldGreen.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (showAutomationLogs || isProblemState) "Hide Logs" else "Logs (${automationLogs.size})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (showAutomationLogs || isProblemState) emeraldGreen else Color.LightGray
                                        )
                                        Icon(
                                            imageVector = if (showAutomationLogs || isProblemState) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (showAutomationLogs || isProblemState) emeraldGreen else Color.LightGray
                                        )
                                    }
                                }
                            }
                        }

                        // Expandable Live Automation Log Console
                        if ((showAutomationLogs || isProblemState) && automationLogs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            val logScrollState = rememberScrollState()
                            LaunchedEffect(automationLogs.size) {
                                logScrollState.scrollTo(logScrollState.maxValue)
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .background(Color(0xFF0F172A).copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                                    .verticalScroll(logScrollState)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    automationLogs.forEach { log ->
                                        Text(
                                            text = log,
                                            fontSize = 10.sp,
                                            color = when {
                                                log.contains("⚠️") || log.contains("❌") -> Color(0xFFFCA5A5)
                                                log.contains("🎯") || log.contains("🎉") || log.contains("✅") -> Color(0xFF86EFAC)
                                                log.contains("🔍") -> Color(0xFF93C5FD)
                                                else -> Color(0xFFCBD5E1)
                                            },
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3rd Redirect Cause Diagnostic Card
                val causeMsg = redirectCauseState.value
                if (causeMsg != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⚠️ Redirect Loop Cause (3rd Attempt)",
                                    color = Color(0xFFFDBA74),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Cause: $causeMsg",
                                    color = Color(0xFFFED7AA),
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    autoRedirectCount = 0
                                    redirectCauseState.value = null
                                    webViewRef?.loadUrl(targetPortalUrl)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Retry Target", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // WebView Container
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(android.graphics.Color.WHITE)
                            webViewRef = this
                            cachedConfig?.let { bundle ->
                                initWebView(this, bundle)
                            }
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
