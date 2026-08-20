package com.urunkarpm.pingpin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.urunkarpm.pingpin.ui.components.LiquidGlassNavBar
import com.urunkarpm.pingpin.ui.components.LiquidGlassNavRail
import com.urunkarpm.pingpin.ui.home.HomeScreen
import com.urunkarpm.pingpin.ui.insights.InsightsScreen
import com.urunkarpm.pingpin.ui.settings.SettingsScreen
import com.urunkarpm.pingpin.ui.theme.rememberWindowSizeInfo

@Composable
fun MainShell(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onToggleTheme: (Boolean) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val windowSizeInfo = rememberWindowSizeInfo()
    val saveableStateHolder = rememberSaveableStateHolder()

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
                            onToggleTheme = onToggleTheme
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
                            onToggleTheme = onToggleTheme
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
