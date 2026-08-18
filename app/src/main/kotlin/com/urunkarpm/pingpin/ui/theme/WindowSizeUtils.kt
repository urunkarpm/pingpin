package com.urunkarpm.pingpin.ui.theme

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    COMPACT, MEDIUM, EXPANDED
}

enum class WindowHeightClass {
    COMPACT, MEDIUM, EXPANDED
}

data class WindowSizeInfo(
    val widthClass: WindowWidthClass,
    val heightClass: WindowHeightClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isLandscape: Boolean
) {
    val isCompactWidth: Boolean get() = widthClass == WindowWidthClass.COMPACT
    val isMediumWidth: Boolean get() = widthClass == WindowWidthClass.MEDIUM
    val isExpandedWidth: Boolean get() = widthClass == WindowWidthClass.EXPANDED
    val isCompactHeight: Boolean get() = heightClass == WindowHeightClass.COMPACT

    // Side navigation rail should be used on tablets/foldables (width >= 600dp) or landscape mode
    val useNavRail: Boolean get() = isMediumWidth || isExpandedWidth || (isLandscape && screenWidthDp >= 480.dp)
}

@Composable
fun rememberWindowSizeInfo(): WindowSizeInfo {
    val config = LocalConfiguration.current
    val widthDp = config.screenWidthDp.dp
    val heightDp = config.screenHeightDp.dp
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    val widthClass = when {
        config.screenWidthDp < 600 -> WindowWidthClass.COMPACT
        config.screenWidthDp < 840 -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.EXPANDED
    }

    val heightClass = when {
        config.screenHeightDp < 480 -> WindowHeightClass.COMPACT
        config.screenHeightDp < 900 -> WindowHeightClass.MEDIUM
        else -> WindowHeightClass.EXPANDED
    }

    return WindowSizeInfo(
        widthClass = widthClass,
        heightClass = heightClass,
        screenWidthDp = widthDp,
        screenHeightDp = heightDp,
        isLandscape = isLandscape
    )
}
