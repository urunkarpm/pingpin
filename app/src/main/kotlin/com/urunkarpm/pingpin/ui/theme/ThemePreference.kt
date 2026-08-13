package com.urunkarpm.pingpin.ui.theme

import android.content.Context

object ThemePreference {
    private const val PREFS = "pingpin_theme_prefs"
    private const val KEY_DARK = "is_dark_mode"

    fun isDarkMode(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK, false) // default = light

    fun setDarkMode(context: Context, dark: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DARK, dark)
            .apply()
    }
}
