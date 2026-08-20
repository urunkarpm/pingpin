package com.urunkarpm.pingpin.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.pingpin.data.local.AppDatabase
import com.urunkarpm.pingpin.data.local.entity.OfficeConfigEntity
import com.urunkarpm.pingpin.data.local.entity.UserProfileEntity
import com.urunkarpm.pingpin.data.repository.OfficeConfigRepository
import com.urunkarpm.pingpin.data.repository.UserProfileRepository
import com.urunkarpm.pingpin.service.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val officeConfigRepo = OfficeConfigRepository(db.officeConfigDao())
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    val notifService = NotificationService(context)
    val updateManager = com.urunkarpm.pingpin.service.UpdateManager(context)

    private val _updateState = kotlinx.coroutines.flow.MutableStateFlow<com.urunkarpm.pingpin.service.UpdateState>(com.urunkarpm.pingpin.service.UpdateState.Idle)
    val updateState: StateFlow<com.urunkarpm.pingpin.service.UpdateState> = _updateState.asStateFlow()

    fun checkForUpdates() {
        viewModelScope.launch {
            _updateState.value = com.urunkarpm.pingpin.service.UpdateState.Checking
            val currentVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.7.0"
            } catch (e: Exception) {
                "1.7.0"
            }
            _updateState.value = updateManager.checkForUpdate(currentVersion)
        }
    }

    fun downloadAndInstallUpdate(updateInfo: com.urunkarpm.pingpin.service.UpdateInfo) {
        viewModelScope.launch {
            _updateState.value = com.urunkarpm.pingpin.service.UpdateState.Downloading(0f, 0L, updateInfo.apkSize)
            val result = updateManager.downloadApk(updateInfo) { progress, downloaded, total ->
                _updateState.value = com.urunkarpm.pingpin.service.UpdateState.Downloading(progress, downloaded, total)
            }
            result.onSuccess { apkFile ->
                _updateState.value = com.urunkarpm.pingpin.service.UpdateState.ReadyToInstall(apkFile, updateInfo)
                installDownloadedApk(apkFile)
            }.onFailure { error ->
                _updateState.value = com.urunkarpm.pingpin.service.UpdateState.Error(error.localizedMessage ?: "Failed to download update.")
            }
        }
    }

    fun installDownloadedApk(apkFile: java.io.File) {
        if (!updateManager.hasInstallPermission()) {
            updateManager.openInstallPermissionSettings()
            return
        }
        val result = updateManager.installApk(apkFile)
        result.onFailure { error ->
            _updateState.value = com.urunkarpm.pingpin.service.UpdateState.Error(error.localizedMessage ?: "Failed to launch package installer.")
        }
    }

    val configState: StateFlow<OfficeConfigEntity?> = officeConfigRepo.configFlow

        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profileState: StateFlow<UserProfileEntity?> = profileRepo.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveConfigAndProfile(
        fullName: String,
        ssid: String,
        checkInTime: String,
        checkOutTime: String,
        portalUrl: String,
        workingDaysMask: Int,
        wfoDaysMask: Int,
        portalMode: String = "EXTERNAL_BROWSER",
        autoLoginEnabled: Boolean = false,
        autoCheckInEnabled: Boolean = false,
        portalPreset: String = "GENERIC",
        portalUsername: String = "",
        portalPassword: String = "",
        customCheckInKeywords: String = "",
        customCheckOutKeywords: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentConfig = configState.value
            val currentProfile = profileState.value

            val newConfig = (currentConfig ?: OfficeConfigEntity()).copy(
                ssid = ssid.trim(),
                checkInTime = checkInTime.trim(),
                checkOutTime = checkOutTime.trim(),
                portalUrl = portalUrl.trim(),
                workingDaysMask = workingDaysMask,
                wfoDaysMask = wfoDaysMask,
                portalMode = portalMode,
                autoLoginEnabled = autoLoginEnabled,
                autoCheckInEnabled = autoCheckInEnabled,
                portalPreset = portalPreset,
                customCheckInKeywords = customCheckInKeywords.trim(),
                customCheckOutKeywords = customCheckOutKeywords.trim()
            )
            officeConfigRepo.saveConfig(newConfig)

            val newProfile = (currentProfile ?: UserProfileEntity()).copy(
                fullName = fullName.trim()
            )
            profileRepo.saveProfile(newProfile)

            val credManager = com.urunkarpm.pingpin.service.portal.PortalCredentialManager(context)
            if (portalUsername.isNotBlank() || portalPassword.isNotBlank()) {
                credManager.saveCredentials(portalUsername, portalPassword)
            }

            notifService.scheduleAlarmsFromConfig(newConfig)
        }
    }
}
