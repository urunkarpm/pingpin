package com.urunkarpm.pingpin.data.local

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class OnboardingPreferencesTest {

    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private val prefsStore = mutableMapOf<String, Boolean>()

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)

        prefsStore.clear()

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        `when`(mockPrefs.getBoolean(anyString(), anyBoolean())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val default = invocation.arguments[1] as Boolean
            prefsStore[key] ?: default
        }

        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenAnswer { invocation ->
            val key = invocation.arguments[0] as String
            val value = invocation.arguments[1] as Boolean
            prefsStore[key] = value
            mockEditor
        }

        `when`(mockEditor.apply()).thenAnswer { null }
    }

    @Test
    fun testDefaultOnboardingStatusIsFalse() {
        assertFalse(OnboardingPreferences.isOnboardingComplete(mockContext))
    }

    @Test
    fun testSetOnboardingCompleteUpdatesStatusToTrue() {
        OnboardingPreferences.setOnboardingComplete(mockContext, true)
        assertTrue(OnboardingPreferences.isOnboardingComplete(mockContext))
    }
}
