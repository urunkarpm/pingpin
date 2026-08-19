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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val db = AppDatabase.getInstance(context)
    private val officeConfigRepo = OfficeConfigRepository(db.officeConfigDao())
    private val profileRepo = UserProfileRepository(db.userProfileDao())
    val notifService = NotificationService(context)

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
