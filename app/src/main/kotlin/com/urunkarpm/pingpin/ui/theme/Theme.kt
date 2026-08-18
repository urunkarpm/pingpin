package com.urunkarpm.pingpin.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueBgDark,
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    tertiary = AmberOrange,
    background = PitchBlackBg,
    onBackground = InkWhite,
    surface = PitchDarkSurface,
    onSurface = InkWhite,
    surfaceContainer = PitchSurfaceContainer,
    surfaceContainerHighest = PitchSurfaceContainerHighest,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = BorderPitchDark,
    outlineVariant = BorderPitchDark.copy(alpha = 0.4f)
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = ElectricBlueBgLight,
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = EmeraldGreen,
    onSecondary = Color.White,
    tertiary = AmberOrange,
    background = PaperLightBg,
    onBackground = InkBlack,
    surface = PaperLightSurface,
    onSurface = InkBlack,
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    onSurfaceVariant = InkMuted,
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.4f)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PingPinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
