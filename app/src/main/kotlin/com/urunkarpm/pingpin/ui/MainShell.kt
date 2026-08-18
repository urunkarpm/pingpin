package com.urunkarpm.pingpin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    var selectedTab by remember { mutableStateOf(0) }
    val windowSizeInfo = rememberWindowSizeInfo()

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
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                LiquidGlassNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(innerPadding)
            ) {
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
}

