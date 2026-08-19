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
    ) { _ -> }

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
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
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
        }
    }
}
