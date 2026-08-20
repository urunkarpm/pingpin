package com.urunkarpm.pingpin.service

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UpdateManagerTest {

    private lateinit var mockContext: Context
    private lateinit var updateManager: UpdateManager

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        updateManager = UpdateManager(mockContext)
    }

    @Test
    fun testVersionComparisonNewerReleaseAvailable() {
        // Test reflection or public interface helper for version comparison
        val method = UpdateManager::class.java.getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
        method.isAccessible = true

        val isNewer1 = method.invoke(updateManager, "1.8.0", "1.7.0") as Boolean
        assertTrue("1.8.0 should be newer than 1.7.0", isNewer1)

        val isNewer2 = method.invoke(updateManager, "v2.0.0", "1.7.0") as Boolean
        assertTrue("v2.0.0 should be newer than 1.7.0", isNewer2)

        val isNewer3 = method.invoke(updateManager, "1.7.1", "1.7.0") as Boolean
        assertTrue("1.7.1 should be newer than 1.7.0", isNewer3)
    }

    @Test
    fun testVersionComparisonSameOrOlderRelease() {
        val method = UpdateManager::class.java.getDeclaredMethod("isNewerVersion", String::class.java, String::class.java)
        method.isAccessible = true

        val isNewerSame = method.invoke(updateManager, "1.7.0", "1.7.0") as Boolean
        assertFalse("1.7.0 should not be newer than 1.7.0", isNewerSame)

        val isNewerOlder = method.invoke(updateManager, "1.6.9", "1.7.0") as Boolean
        assertFalse("1.6.9 should not be newer than 1.7.0", isNewerOlder)

        val isNewerWithV = method.invoke(updateManager, "v1.7.0", "1.7.0") as Boolean
        assertFalse("v1.7.0 should not be newer than 1.7.0", isNewerWithV)
    }
}
