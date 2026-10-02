package com.urunkarpm.pingpin.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Modern, high-contrast toggle switch for PingPin.
 * Ensures high visual contrast in both AMOLED Dark and E-Ink Light themes,
 * with tactile haptics, spring animations, and icon indicators inside the thumb.
 */
@Composable
fun PingPinSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    checkedTrackColor: Color = MaterialTheme.colorScheme.primary,
    uncheckedTrackColor: Color? = null,
    checkedThumbColor: Color = Color.White,
    uncheckedThumbColor: Color? = null,
    checkedBorderColor: Color? = null,
    uncheckedBorderColor: Color? = null,
    checkedIcon: ImageVector? = Icons.Filled.Check,
    uncheckedIcon: ImageVector? = null,
    iconTintChecked: Color? = null,
    iconTintUnchecked: Color? = null,
    contentDescription: String? = null
) {
    val isDark = isSystemInDarkTheme()

    // Vibrant high-contrast unchecked state for translucent glass backgrounds
    val defaultUncheckedTrack = uncheckedTrackColor ?: if (isDark) {
        Color(0xFF1E293B) // Dark Slate-800: distinct visible track on pitch dark surfaces
    } else {
        Color(0xFFCBD5E1) // Light Slate-300: crisp visible track on white paper surfaces
    }

    val defaultUncheckedBorder = uncheckedBorderColor ?: if (isDark) {
        Color(0xFF64748B) // Slate-500: strong high-contrast border outline in dark mode
    } else {
        Color(0xFF475569) // Slate-600: bold outline border in light mode
    }

    val defaultUncheckedThumb = uncheckedThumbColor ?: if (isDark) {
        Color(0xFFF8FAFC) // Slate-50: bright white thumb popping out in dark mode
    } else {
        Color(0xFF0F172A) // Slate-900: deep dark thumb contrasting in light mode
    }

    val actualCheckedBorder = checkedBorderColor ?: checkedTrackColor
    val actualCheckedIconTint = iconTintChecked ?: checkedTrackColor
    val actualUncheckedIconTint = iconTintUnchecked ?: if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    // ponytail: Zero-animation instant switch toggle (Laws of UX: Doherty Threshold). Ceiling: Native Material3 Switch transition. Upgrade path: Spring thumb scale if requested.
    val tactile = com.urunkarpm.pingpin.ui.theme.rememberTactileFeedback()

    Switch(
        checked = checked,
        onCheckedChange = { newValue ->
            if (enabled && onCheckedChange != null) {
                tactile.click()
                onCheckedChange(newValue)
            }
        },
        enabled = enabled,
        modifier = modifier
            .semantics {
                role = Role.Switch
                stateDescription = if (checked) "On" else "Off"
            },
        thumbContent = if (checked && checkedIcon != null) {
            {
                Icon(
                    imageVector = checkedIcon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    tint = actualCheckedIconTint
                )
            }
        } else if (!checked && uncheckedIcon != null) {
            {
                Icon(
                    imageVector = uncheckedIcon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    tint = actualUncheckedIconTint
                )
            }
        } else {
            null
        },
        colors = SwitchDefaults.colors(
            checkedThumbColor = checkedThumbColor,
            checkedTrackColor = checkedTrackColor,
            checkedBorderColor = actualCheckedBorder,
            checkedIconColor = actualCheckedIconTint,

            uncheckedThumbColor = defaultUncheckedThumb,
            uncheckedTrackColor = defaultUncheckedTrack,
            uncheckedBorderColor = defaultUncheckedBorder,
            uncheckedIconColor = actualUncheckedIconTint,

            disabledCheckedThumbColor = Color.Gray.copy(alpha = 0.4f),
            disabledCheckedTrackColor = Color.Gray.copy(alpha = 0.2f),
            disabledUncheckedThumbColor = Color.Gray.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = Color.Gray.copy(alpha = 0.1f)
        )
    )
}
