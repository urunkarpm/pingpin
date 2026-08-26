package com.urunkarpm.pingpin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.OnboardingPreferences
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.data.repository.UserProfileRepository
import com.urunkarpm.pingpin.ui.MainShell
import com.urunkarpm.pingpin.ui.onboarding.OnboardingScreen
import com.urunkarpm.pingpin.ui.theme.PingPinTheme
import com.urunkarpm.pingpin.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        checkAndRequestSpecialPermissions()
    }

    private val requestBackgroundLocationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        checkAndRequestSpecialPermissions(skipBgLocation = true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        requestAppPermissions()

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(ThemePreference.isDarkMode(applicationContext))
            }

            PingPinTheme(darkTheme = isDarkTheme, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val db = remember { AppDatabase.getInstance(applicationContext) }
                    val configRepo = remember { OfficeConfigRepository(db.officeConfigDao()) }
                    val profileRepo = remember { UserProfileRepository(db.userProfileDao()) }

                    var isOnboardingComplete by remember { mutableStateOf<Boolean?>(null) }

                    LaunchedEffect(Unit) {
                        val isPrefComplete = OnboardingPreferences.isOnboardingComplete(applicationContext)
                        if (isPrefComplete) {
                            isOnboardingComplete = true
                        } else {
                            val cfg = configRepo.getConfig()
                            val profile = profileRepo.getProfile()
                            val isComplete = (cfg != null && cfg.ssid.isNotEmpty() && profile != null)
                            if (isComplete) {
                                OnboardingPreferences.setOnboardingComplete(applicationContext, true)
                            }
                            isOnboardingComplete = isComplete
                        }
                    }

                    when (isOnboardingComplete) {
                        true -> MainShell(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { dark ->
                                isDarkTheme = dark
                                ThemePreference.setDarkMode(applicationContext, dark)
                            }
                        )
                        false -> OnboardingScreen(
                            onOnboardingComplete = { isOnboardingComplete = true }
                        )
                        null -> { /* Loading */ }
                    }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            checkAndRequestSpecialPermissions()
        }
    }

    private fun checkAndRequestSpecialPermissions(skipBgLocation: Boolean = false) {
        // 1. Background Location (Android 10+)
        if (!skipBgLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasBg = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasFine && !hasBg) {
                try {
                    requestBackgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    return
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error requesting background location permission", e)
                }
            }
        }

        // 2. Unrestricted Battery Optimization Exemption (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    return
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error requesting battery optimization exemption", e)
                }
            }
        }

        // 3. Exact Alarm Scheduling Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    return
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error requesting exact alarm permission", e)
                }
            }
        }

        // 4. Display Over Apps / Overlay Permission (Android 6.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            try {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error requesting overlay permission", e)
            }
        }

        // 5. Full-Screen Intent Permission (Android 14+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            if (notificationManager != null && !notificationManager.canUseFullScreenIntent()) {
                try {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "Error requesting full screen intent permission", e)
                }
            }
        }
    }
}
