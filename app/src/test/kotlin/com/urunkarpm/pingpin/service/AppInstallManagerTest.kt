package com.urunkarpm.pingpin.service

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class AppInstallManagerTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private val prefsStore = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        prefsStore.clear()

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        `when`(mockPrefs.getString(anyString(), org.mockito.ArgumentMatchers.nullable(String::class.java))).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val default = invocation.arguments[1] as String?
            (prefsStore[key] as? String) ?: default
        }

        `when`(mockPrefs.getLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val default = invocation.arguments[1] as Long
            (prefsStore[key] as? Long) ?: default
        }

        `when`(mockEditor.putString(anyString(), anyString())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as String
            prefsStore[key] = value
            mockEditor
        }

        `when`(mockEditor.putLong(anyString(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as Long
            prefsStore[key] = value
            mockEditor
        }

        `when`(mockEditor.apply()).thenAnswer { null }
    }

    @Test
    fun testAdjustInstallDateIfOlderRecordExists() {
        prefsStore["app_install_date_yyyy_mm_dd"] = "2026-08-10"
        prefsStore["app_install_time_ms"] = 1770681600000L

        val initialDate = AppInstallManager.getInstallDateYyyyMmDd(mockContext)
        assertEquals("2026-08-10", initialDate)

        // Adjust with an older record date
        AppInstallManager.adjustInstallDateIfOlderRecordExists(mockContext, "2026-07-01")

        val adjustedDate = AppInstallManager.getInstallDateYyyyMmDd(mockContext)
        assertEquals("2026-07-01", adjustedDate)
    }

    @Test
    fun testAdjustInstallDateDoesNotOverwrittenIfRecordIsNewer() {
        prefsStore["app_install_date_yyyy_mm_dd"] = "2026-08-10"
        prefsStore["app_install_time_ms"] = 1770681600000L

        // Adjust with a newer record date (should be ignored)
        AppInstallManager.adjustInstallDateIfOlderRecordExists(mockContext, "2026-08-14")

        val adjustedDate = AppInstallManager.getInstallDateYyyyMmDd(mockContext)
        assertEquals("2026-08-10", adjustedDate)
    }
}
