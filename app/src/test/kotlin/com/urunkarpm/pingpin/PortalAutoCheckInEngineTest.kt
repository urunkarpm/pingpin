package com.urunkarpm.pingpin

import com.urunkarpm.pingpin.service.portal.PortalAutoCheckInEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalAutoCheckInEngineTest {

    @Test
    fun `generateAutomationScript scopes variables at outer IIFE level`() {
        val script = PortalAutoCheckInEngine.generateAutomationScript(
            actionType = "CHECK_IN",
            username = "testuser",
            password = "testpassword",
            autoLogin = true,
            autoPunch = true,
            customCheckInKeywords = "punch in, check in",
            customCheckOutKeywords = "",
            targetPortalUrl = "https://example.com/portal"
        )

        // Verify outer closure variables are declared
        assertTrue("Script must declare isCheckIn at outer scope", script.contains("var isCheckIn = true;"))
        assertTrue("Script must declare actionLabel at outer scope", script.contains("var actionLabel = isCheckIn ?"))
        assertTrue("Script must declare targetKeywords at outer scope", script.contains("var targetKeywords ="))
        assertTrue("Script must declare targetUrl at outer scope", script.contains("var targetUrl = 'https://example.com/portal';"))

        // Verify try-catch in pollEngine to prevent silent crash loops
        assertTrue("pollEngine must be wrapped in try-catch", script.contains("catch(err)") || script.contains("catch (err)"))
    }

    @Test
    fun `generateAutomationScript correctly sets Check Out parameters`() {
        val script = PortalAutoCheckInEngine.generateAutomationScript(
            actionType = "CHECK_OUT",
            username = "testuser",
            password = "testpassword",
            autoLogin = false,
            autoPunch = true,
            customCheckInKeywords = "",
            customCheckOutKeywords = "punch out, logout",
            targetPortalUrl = "https://example.com/portal"
        )

        assertTrue("Script must set isCheckIn to false for CHECK_OUT", script.contains("var isCheckIn = false;"))
        assertTrue("Script must include custom checkout keywords", script.contains("'punch out'"))
    }
}
