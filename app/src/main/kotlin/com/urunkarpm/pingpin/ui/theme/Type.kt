package com.urunkarpm.pingpin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.urunkarpm.pingpin.R

// Plus Jakarta Sans — Premium geometric neo-grotesque font family
val PlusJakartaSansFontFamily = FontFamily(
    Font(resId = R.font.plus_jakarta_sans_regular, weight = FontWeight.Normal),
    Font(resId = R.font.plus_jakarta_sans_medium, weight = FontWeight.Medium),
    Font(resId = R.font.plus_jakarta_sans_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.plus_jakarta_sans_bold, weight = FontWeight.Bold),
    Font(resId = R.font.plus_jakarta_sans_extrabold, weight = FontWeight.ExtraBold)
)

// Aliases for backwards compatibility
val GoogleSansFontFamily = PlusJakartaSansFontFamily
val InterFontFamily = PlusJakartaSansFontFamily

private val defaultTypography = Typography()

val AppTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = PlusJakartaSansFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = PlusJakartaSansFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = PlusJakartaSansFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = PlusJakartaSansFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = PlusJakartaSansFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = PlusJakartaSansFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Medium),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = PlusJakartaSansFontFamily, fontWeight = FontWeight.Medium)
)
