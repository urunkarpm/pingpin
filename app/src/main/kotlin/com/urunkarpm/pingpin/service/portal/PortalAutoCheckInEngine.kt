package com.urunkarpm.pingpin.service.portal

import android.webkit.JavascriptInterface
import android.util.Log

class PortalAutoCheckInEngine {

    interface PortalCallback {
        fun onStatusUpdate(status: String)
        fun onLoginSubmitted()
        fun onPunchAttempted(actionType: String)
        fun onPunchSuccess(actionType: String)
        fun onError(message: String)
    }

    class WebBridge(private val callback: PortalCallback) {
        @JavascriptInterface
        fun updateStatus(msg: String) {
            callback.onStatusUpdate(msg)
        }

        @JavascriptInterface
        fun loginSubmitted() {
            callback.onLoginSubmitted()
        }

        @JavascriptInterface
        fun punchAttempted(actionType: String) {
            callback.onPunchAttempted(actionType)
        }

        @JavascriptInterface
        fun punchSuccess(actionType: String) {
            callback.onPunchSuccess(actionType)
        }

        @JavascriptInterface
        fun logError(err: String) {
            callback.onError(err)
        }
    }

    companion object {
        private const val TAG = "PortalAutoEngine"

        fun generateAutomationScript(
            actionType: String, // "CHECK_IN" or "CHECK_OUT"
            username: String,
            password: String,
            autoLogin: Boolean,
            autoPunch: Boolean
        ): String {
            val escapedUser = username.replace("'", "\\'").replace("\n", "")
            val escapedPass = password.replace("'", "\\'").replace("\n", "")
            val isCheckIn = actionType.equals("CHECK_IN", ignoreCase = true)

            return """
            (function() {
                if (window.__pingpin_automation_active) return;
                window.__pingpin_automation_active = true;

                console.log("PingPin Portal Engine started for action: $actionType");

                function notifyStatus(msg) {
                    if (window.PingPinBridge && window.PingPinBridge.updateStatus) {
                        window.PingPinBridge.updateStatus(msg);
                    }
                }

                function triggerInputChange(element, value) {
                    try {
                        element.focus();
                        element.value = value;
                        element.dispatchEvent(new Event('input', { bubbles: true }));
                        element.dispatchEvent(new Event('change', { bubbles: true }));
                        element.dispatchEvent(new Event('blur', { bubbles: true }));
                    } catch(e){}
                }

                function tryAutoLogin() {
                    if (!${autoLogin}) return false;
                    
                    var userInput = document.querySelector('input[type="email"], input[type="text"][name*="user"], input[name*="user"], input[name*="login"], input[name*="email"], input[name*="emp"], #username, #email, #emp_id');
                    var passInput = document.querySelector('input[type="password"], input[name*="pass"], #password');

                    if (userInput && passInput && ('$escapedUser' !== '' && '$escapedPass' !== '')) {
                        if (!userInput.value) {
                            triggerInputChange(userInput, '$escapedUser');
                        }
                        if (!passInput.value) {
                            triggerInputChange(passInput, '$escapedPass');
                        }

                        notifyStatus("Auto-filling credentials...");

                        var submitBtn = document.querySelector('button[type="submit"], input[type="submit"], button[class*="login"], button[id*="login"], .btn-primary, #loginBtn, #submit');
                        if (submitBtn) {
                            setTimeout(function() {
                                notifyStatus("Submitting login...");
                                if (window.PingPinBridge && window.PingPinBridge.loginSubmitted) {
                                    window.PingPinBridge.loginSubmitted();
                                }
                                submitBtn.click();
                            }, 600);
                            return true;
                        }
                    }
                    return false;
                }

                function tryAutoPunch() {
                    if (!${autoPunch}) return false;

                    var targetKeywords = ${if (isCheckIn) 
                        "['check in', 'check-in', 'clock in', 'clock-in', 'punch in', 'punch-in', 'web punch', 'web-punch', 'checkin', 'punchin', 'clockin', 'mark attendance', 'sign in', 'present', 'in']" 
                    else 
                        "['check out', 'check-out', 'clock out', 'clock-out', 'punch out', 'punch-out', 'web-punch out', 'checkout', 'punchout', 'clockout', 'sign out', 'out']"};

                    var candidateElements = Array.from(document.querySelectorAll('button, a, input[type="button"], input[type="submit"], div[role="button"], span[role="button"], .btn, .button, [class*="punch"], [class*="checkin"], [class*="checkout"]'));
                    
                    for (var i = 0; i < candidateElements.length; i++) {
                        var el = candidateElements[i];
                        var text = (
                            (el.innerText || '') + ' ' + 
                            (el.value || '') + ' ' + 
                            (el.getAttribute('aria-label') || '') + ' ' + 
                            (el.getAttribute('title') || '') + ' ' + 
                            (el.getAttribute('id') || '') + ' ' + 
                            (el.getAttribute('class') || '')
                        ).toLowerCase().trim();
                        
                        for (var k = 0; k < targetKeywords.length; k++) {
                            var kw = targetKeywords[k];
                            if (text.includes(kw)) {
                                notifyStatus("Found target button (" + kw + "). Clicking...");
                                if (window.PingPinBridge && window.PingPinBridge.punchAttempted) {
                                    window.PingPinBridge.punchAttempted('$actionType');
                                }
                                el.click();
                                setTimeout(function() {
                                    if (window.PingPinBridge && window.PingPinBridge.punchSuccess) {
                                        window.PingPinBridge.punchSuccess('$actionType');
                                    }
                                }, 1500);
                                return true;
                            }
                        }
                    }
                    return false;
                }

                function checkSpecialStates() {
                    var bodyText = (document.body ? document.body.innerText : '').toLowerCase();
                    
                    var alreadyInKeywords = ['already checked in', 'already punched', 'checked in at', 'punched in at', 'already clocked in', 'shift in progress'];
                    for (var a = 0; a < alreadyInKeywords.length; a++) {
                        if (bodyText.includes(alreadyInKeywords[a])) {
                            notifyStatus("✅ Already checked in today on portal!");
                            return true;
                        }
                    }

                    var mfaKeywords = ['enter otp', 'verification code', 'authenticator', 'captcha', '2-step verification', 'enter code'];
                    for (var m = 0; m < mfaKeywords.length; m++) {
                        if (bodyText.includes(mfaKeywords[m])) {
                            notifyStatus("🔒 2FA / OTP required. Please complete verification manually.");
                            return true;
                        }
                    }

                    return false;
                }

                var attempts = 0;
                var maxAttempts = 15; // Poll every 800ms for 12 seconds
                var intervalTimer = null;

                function pollEngine() {
                    attempts++;

                    if (checkSpecialStates()) {
                        clearInterval(intervalTimer);
                        window.__pingpin_automation_active = false;
                        return;
                    }

                    var loginSuccess = tryAutoLogin();
                    if (loginSuccess) {
                        clearInterval(intervalTimer);
                        window.__pingpin_automation_active = false;
                        return;
                    }

                    var punchSuccess = tryAutoPunch();
                    if (punchSuccess) {
                        clearInterval(intervalTimer);
                        window.__pingpin_automation_active = false;
                        return;
                    }

                    if (attempts >= maxAttempts) {
                        clearInterval(intervalTimer);
                        window.__pingpin_automation_active = false;
                        notifyStatus("⚠️ Check-in button not found. You can interact with the portal manually or tap 'Open in Chrome'.");
                    }
                }

                intervalTimer = setInterval(pollEngine, 800);
                pollEngine();
            })();
            """.trimIndent()
        }
    }
}
