package com.urunkarpm.pingpin.service.portal

import android.content.Context
import android.content.SharedPreferences

class PortalCredentialManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(username: String, password: String) {
        prefs.edit()
            .putString(KEY_USERNAME, username.trim())
            .putString(KEY_PASSWORD, password.trim())
            .apply()
    }

    fun getUsername(): String {
        return prefs.getString(KEY_USERNAME, "") ?: ""
    }

    fun getPassword(): String {
        return prefs.getString(KEY_PASSWORD, "") ?: ""
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    fun hasCredentials(): Boolean {
        return getUsername().isNotBlank() && getPassword().isNotBlank()
    }

    companion object {
        private const val PREFS_NAME = "pingpin_portal_credentials"
        private const val KEY_USERNAME = "portal_username"
        private const val KEY_PASSWORD = "portal_password"
    }
}
