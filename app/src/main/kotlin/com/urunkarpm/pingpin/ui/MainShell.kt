package com.urunkarpm.pingpin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.urunkarpm.pingpin.ui.components.LiquidGlassNavBar
import com.urunkarpm.pingpin.ui.components.LiquidGlassNavRail
import com.urunkarpm.pingpin.ui.components.UpdateAvailableDialog
import com.urunkarpm.pingpin.ui.home.HomeScreen
import com.urunkarpm.pingpin.ui.insights.InsightsScreen
import com.urunkarpm.pingpin.ui.settings.SettingsScreen
import com.urunkarpm.pingpin.ui.theme.rememberWindowSizeInfo
import com.urunkarpm.pingpin.ui.update.AppUpdateViewModel

@Composable
fun MainShell(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {},
    appUpdateViewModel: AppUpdateViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val windowSizeInfo = rememberWindowSizeInfo()
    val saveableStateHolder = rememberSaveableStateHolder()

    val updateState by appUpdateViewModel.updateState.collectAsState()
    val showUpdateDialog by appUpdateViewModel.showDialog.collectAsState()

    // Trigger automatic check on app startup
    LaunchedEffect(Unit) {
        appUpdateViewModel.checkForUpdates(isAutoCheck = true)
    }

    if (showUpdateDialog) {
        UpdateAvailableDialog(
            updateState = updateState,
            onDismiss = { appUpdateViewModel.dismissDialog() },
            onDownloadAndInstall = { appUpdateViewModel.downloadAndInstallUpdate(it) },
            onInstallApk = { appUpdateViewModel.installDownloadedApk(it) },
            onRetryCheck = { appUpdateViewModel.checkForUpdates(isAutoCheck = false) }
        )
    }

    if (windowSizeInfo.useNavRail) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            LiquidGlassNavRail(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                saveableStateHolder.SaveableStateProvider(key = selectedTab) {
                    when (selectedTab) {
                        0 -> HomeScreen()
                        1 -> InsightsScreen()
                        2 -> SettingsScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            appUpdateViewModel = appUpdateViewModel
                        )
                    }
                }
            }
        }
    } else {
        // Seamless Floating Overlap Architecture: Content flows full-screen underneath the floating navbar
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            // 1. Full-screen Body Content Layer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                saveableStateHolder.SaveableStateProvider(key = selectedTab) {
                    when (selectedTab) {
                        0 -> HomeScreen()
                        1 -> InsightsScreen()
                        2 -> SettingsScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = onToggleTheme,
                            appUpdateViewModel = appUpdateViewModel
                        )
                    }
                }
            }

            // 2. Floating Liquid Glass Bottom Navbar (Seamless Overlap at Bottom Center)
            LiquidGlassNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

