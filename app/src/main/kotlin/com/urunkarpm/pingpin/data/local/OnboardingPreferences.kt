package com.urunkarpm.pingpin.data.local

import android.content.Context
import android.content.SharedPreferences

object OnboardingPreferences {
    private const val PREFS_NAME = "pingpin_onboarding_prefs"
    private const val KEY_IS_COMPLETE = "is_onboarding_complete"

    fun isOnboardingComplete(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_COMPLETE, false)
    }

    fun setOnboardingComplete(context: Context, complete: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_COMPLETE, complete).apply()
    }
}
