package com.urunkarpm.pingpin.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.urunkarpm.pingpin.service.UpdateInfo
import com.urunkarpm.pingpin.service.UpdateManager
import com.urunkarpm.pingpin.service.UpdateState
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val updateManager = UpdateManager(context)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private var dismissedVersion: String? = null

    fun checkForUpdates(isAutoCheck: Boolean = false) {
        viewModelScope.launch {
            _updateState.value = UpdateState.Checking
            val currentVersion = getCurrentVersionName()
            val state = updateManager.checkForUpdate(currentVersion)
            _updateState.value = state

            if (state is UpdateState.UpdateAvailable) {
                val newVersion = state.updateInfo.versionName
                if (!isAutoCheck || dismissedVersion != newVersion) {
                    _showDialog.value = true
                }
            } else if (!isAutoCheck) {
                _showDialog.value = true
            }
        }
    }

    fun downloadAndInstallUpdate(updateInfo: UpdateInfo) {
        viewModelScope.launch {
            _showDialog.value = true
            _updateState.value = UpdateState.Downloading(0f, 0L, updateInfo.apkSize)
            val result = updateManager.downloadApk(updateInfo) { progress, downloaded, total ->
                _updateState.value = UpdateState.Downloading(progress, downloaded, total)
            }
            result.onSuccess { apkFile ->
                _updateState.value = UpdateState.ReadyToInstall(apkFile, updateInfo)
                installDownloadedApk(apkFile)
            }.onFailure { error ->
                _updateState.value = UpdateState.Error(error.localizedMessage ?: "Failed to download update.")
            }
        }
    }

    fun installDownloadedApk(apkFile: File) {
        if (!updateManager.hasInstallPermission()) {
            updateManager.openInstallPermissionSettings()
            return
        }
        val result = updateManager.installApk(apkFile)
        result.onFailure { error ->
            _updateState.value = UpdateState.Error(error.localizedMessage ?: "Failed to launch package installer.")
        }
    }

    fun dismissDialog() {
        _showDialog.value = false
        val currentState = _updateState.value
        if (currentState is UpdateState.UpdateAvailable) {
            dismissedVersion = currentState.updateInfo.versionName
        }
    }

    fun openDialog() {
        _showDialog.value = true
    }

    fun getCurrentVersionName(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "2.1.0"
        } catch (e: Exception) {
            "2.1.0"
        }
    }
}
