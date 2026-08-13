package com.urunkarpm.pingpin.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.urunkarpm.pingpin.R

val GoogleSansFontFamily = FontFamily(
    Font(resId = R.font.google_sans_semibold, weight = FontWeight.Light),
    Font(resId = R.font.google_sans_semibold, weight = FontWeight.Normal),
    Font(resId = R.font.google_sans_semibold, weight = FontWeight.Medium),
    Font(resId = R.font.google_sans_semibold, weight = FontWeight.SemiBold),
    Font(resId = R.font.google_sans_semibold, weight = FontWeight.Bold),
    Font(resId = R.font.google_sans_semibold, weight = FontWeight.ExtraBold),
    Font(resId = R.font.google_sans_semibold, weight = FontWeight.Black)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = GoogleSansFontFamily),
    displayMedium = TextStyle(fontFamily = GoogleSansFontFamily),
    displaySmall = TextStyle(fontFamily = GoogleSansFontFamily),
    headlineLarge = TextStyle(fontFamily = GoogleSansFontFamily),
    headlineMedium = TextStyle(fontFamily = GoogleSansFontFamily),
    headlineSmall = TextStyle(fontFamily = GoogleSansFontFamily),
    titleLarge = TextStyle(fontFamily = GoogleSansFontFamily),
    titleMedium = TextStyle(fontFamily = GoogleSansFontFamily),
    titleSmall = TextStyle(fontFamily = GoogleSansFontFamily),
    bodyLarge = TextStyle(fontFamily = GoogleSansFontFamily),
    bodyMedium = TextStyle(fontFamily = GoogleSansFontFamily),
    bodySmall = TextStyle(fontFamily = GoogleSansFontFamily),
    labelLarge = TextStyle(fontFamily = GoogleSansFontFamily),
    labelMedium = TextStyle(fontFamily = GoogleSansFontFamily),
    labelSmall = TextStyle(fontFamily = GoogleSansFontFamily)
)

