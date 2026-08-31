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
            autoPunch: Boolean,
            customCheckInKeywords: String = "",
            customCheckOutKeywords: String = "",
            targetPortalUrl: String = ""
        ): String {
            val escapedUser = username.replace("'", "\\'").replace("\n", "")
            val escapedPass = password.replace("'", "\\'").replace("\n", "")
            val escapedTargetUrl = targetPortalUrl.replace("'", "\\'").replace("\n", "")
            val isCheckIn = actionType.equals("CHECK_IN", ignoreCase = true)

            val parsedCustomKeywords = if (isCheckIn) {
                customCheckInKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            } else {
                customCheckOutKeywords.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            }

            val defaultKeywords = if (isCheckIn) {
                listOf(
                    "check in", "check-in", "clock in", "clock-in", "punch in", "punch-in",
                    "web punch", "web-punch", "checkin", "punchin", "clockin",
                    "mark attendance", "mark present"
                )
            } else {
                listOf(
                    "check out", "check-out", "clock out", "clock-out", "punch out", "punch-out",
                    "web-punch out", "web punch out", "checkout", "punchout", "clockout", "mark checkout",
                    "mark check-out", "out punch", "punch out now", "end shift"
                )
            }

            val activeKeywords = if (parsedCustomKeywords.isNotEmpty()) parsedCustomKeywords else defaultKeywords
            val keywordsJsArray = activeKeywords.joinToString(prefix = "[", postfix = "]") { "'${it.replace("'", "\\'")}'" }

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

                function checkUrlMismatch() {
                    try {
                        var targetUrl = '$escapedTargetUrl';
                        if (!targetUrl) return;

                        var currentHref = (window.location.href || '').toLowerCase();
                        var targetLower = targetUrl.toLowerCase();

                        function clean(u) {
                            return u.replace(/^https?:\/\//, '').replace(/\/+$/, '').replace(/#.*$/, '').replace(/\?.*$/, '');
                        }

                        var cleanCurrent = clean(currentHref);
                        var cleanTarget = clean(targetLower);

                        if (cleanCurrent && cleanTarget && cleanCurrent !== cleanTarget && !cleanCurrent.startsWith(cleanTarget) && !cleanTarget.startsWith(cleanCurrent)) {
                            var isAuthPage = currentHref.includes('login') || currentHref.includes('auth') || currentHref.includes('signin') || currentHref.includes('sso');
                            if (isAuthPage) {
                                notifyStatus("🔒 Login page detected. Attempting auto-login...");
                            } else {
                                notifyStatus("Redirected to (" + cleanCurrent + "). Redirecting to target URL...");
                            }
                        }
                    } catch(e){}
                }

                function ensureViewportMeta() {
                    try {
                        if (!document.querySelector('meta[name="viewport"]')) {
                            var meta = document.createElement('meta');
                            meta.name = 'viewport';
                            meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
                            if (document.head) {
                                document.head.appendChild(meta);
                            } else if (document.body) {
                                document.body.appendChild(meta);
                            }
                        }
                    } catch(e){}
                }

                function triggerInputChange(element, value) {
                    if (!element) return;
                    try {
                        element.focus();
                        var setter = null;
                        if (element.constructor && element.constructor.prototype) {
                            var pd = Object.getOwnPropertyDescriptor(element.constructor.prototype, 'value');
                            if (pd && pd.set) setter = pd.set;
                        }
                        if (!setter && window.HTMLInputElement && window.HTMLInputElement.prototype) {
                            var pd2 = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                            if (pd2 && pd2.set) setter = pd2.set;
                        }
                        if (setter) {
                            setter.call(element, value);
                        } else {
                            element.value = value;
                        }
                        element.dispatchEvent(new Event('input', { bubbles: true }));
                        element.dispatchEvent(new Event('change', { bubbles: true }));
                        element.dispatchEvent(new Event('blur', { bubbles: true }));
                    } catch(e) {
                        try {
                            element.value = value;
                            element.dispatchEvent(new Event('input', { bubbles: true }));
                            element.dispatchEvent(new Event('change', { bubbles: true }));
                        } catch(ex){}
                    }
                }

                function clickElement(el) {
                    if (!el) return;
                    try {
                        el.removeAttribute('disabled');
                        el.disabled = false;
                    } catch(e){}
                    try {
                        el.focus();
                    } catch(e){}

                    try {
                        var evt = new MouseEvent('click', {
                            bubbles: true,
                            cancelable: true,
                            view: window
                        });
                        el.dispatchEvent(evt);
                    } catch(e){}

                    try {
                        el.click();
                    } catch(e){}

                    if (el.form) {
                        try {
                            if (typeof el.form.requestSubmit === 'function') {
                                el.form.requestSubmit();
                            } else if (typeof el.form.submit === 'function') {
                                el.form.submit();
                            }
                        } catch(e){}
                    }
                }

                function isVisible(el) {
                    return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
                }

                function getElementText(el) {
                    return (el.innerText || el.textContent || el.value || el.getAttribute('aria-label') || el.getAttribute('title') || '').toLowerCase().trim();
                }

                function findSubmitButton(formContext) {
                    var root = formContext || document;
                    var submitBtn = root.querySelector('button[type="submit"], input[type="submit"], button[class*="login"], button[id*="login"], #loginBtn, #submit, button[id*="next"], button[class*="next"], #next, #continue');
                    if (submitBtn && isVisible(submitBtn)) return submitBtn;

                    var candidates = Array.from(root.querySelectorAll('button, input[type="button"], input[type="submit"], a, div[role="button"], span[role="button"], div[class*="btn"], span[class*="btn"]'));
                    var targetKeywords = ['log in', 'login', 'sign in', 'signin', 'submit', 'proceed', 'next', 'continue', 'verify'];

                    for (var i = 0; i < candidates.length; i++) {
                        var c = candidates[i];
                        if (!isVisible(c)) continue;
                        var txt = getElementText(c);
                        if (!txt) continue;
                        for (var k = 0; k < targetKeywords.length; k++) {
                            if (txt === targetKeywords[k] || txt.startsWith(targetKeywords[k] + ' ') || txt.endsWith(' ' + targetKeywords[k])) {
                                return c;
                            }
                        }
                    }
                    return null;
                }

                function tryAutoLogin() {
                    if (!${autoLogin}) return false;
                    if ('$escapedUser' === '' && '$escapedPass' === '') return false;

                    ensureViewportMeta();

                    var userInput = document.querySelector('input[type="email"], input[type="text"][name*="user"], input[name*="user"], input[name*="login"], input[name*="email"], input[name*="emp"], #username, #email, #emp_id, input[id*="user"], input[id*="email"], input[aria-label*="user"], input[aria-label*="email"]');
                    var passInput = document.querySelector('input[type="password"], input[name*="pass"], #password, input[id*="pass"], input[aria-label*="pass"]');

                    if (userInput && passInput && isVisible(userInput) && isVisible(passInput)) {
                        triggerInputChange(userInput, '$escapedUser');
                        triggerInputChange(passInput, '$escapedPass');

                        notifyStatus("Auto-filling login credentials...");

                        var submitBtn = findSubmitButton();
                        if (submitBtn) {
                            setTimeout(function() {
                                notifyStatus("Submitting login...");
                                if (window.PingPinBridge && window.PingPinBridge.loginSubmitted) {
                                    window.PingPinBridge.loginSubmitted();
                                }
                                clickElement(submitBtn);
                            }, 500);
                            return true;
                        }
                    } 
                    else if (userInput && isVisible(userInput) && (!passInput || !isVisible(passInput))) {
                        if ('$escapedUser' !== '') {
                            triggerInputChange(userInput, '$escapedUser');
                            notifyStatus("Auto-filling username (Step 1)...");
                            var nextBtn = findSubmitButton();
                            if (nextBtn) {
                                setTimeout(function() {
                                    notifyStatus("Clicking Next...");
                                    clickElement(nextBtn);
                                }, 500);
                                return true;
                            }
                        }
                    }
                    else if (passInput && isVisible(passInput) && (!userInput || !isVisible(userInput))) {
                        if ('$escapedPass' !== '') {
                            triggerInputChange(passInput, '$escapedPass');
                            notifyStatus("Auto-filling password (Step 2)...");
                            var finalBtn = findSubmitButton();
                            if (finalBtn) {
                                setTimeout(function() {
                                    notifyStatus("Submitting login...");
                                    if (window.PingPinBridge && window.PingPinBridge.loginSubmitted) {
                                        window.PingPinBridge.loginSubmitted();
                                    }
                                    clickElement(finalBtn);
                                }, 500);
                                return true;
                            }
                        }
                    }

                    return false;
                }

                function tryAutoPunch() {
                    if (!${autoPunch}) return false;

                    var targetKeywords = $keywordsJsArray;
                    var blacklist = ['log in', 'login', 'sign in', 'signin', 'log out', 'logout', 'sign out', 'signout', 'register', 'forgot password', 'user', 'email', 'password'];

                    var candidateElements = Array.from(document.querySelectorAll('button, a, input[type="button"], input[type="submit"], div[role="button"], span[role="button"], .btn, .button, [class*="punch"], [class*="checkin"], [class*="checkout"]'));
                    
                    for (var i = 0; i < candidateElements.length; i++) {
                        var el = candidateElements[i];
                        if (!isVisible(el)) continue;

                        var text = getElementText(el);
                        if (!text) continue;

                        var isBlacklisted = false;
                        for (var b = 0; b < blacklist.length; b++) {
                            if (text === blacklist[b] || text.startsWith(blacklist[b] + ' ')) {
                                isBlacklisted = true;
                                break;
                            }
                        }
                        if (isBlacklisted) continue;
                        
                        for (var k = 0; k < targetKeywords.length; k++) {
                            var kw = targetKeywords[k];
                            if (text.includes(kw)) {
                                var initialBodyText = (document.body ? document.body.innerText : '').toLowerCase();
                                notifyStatus("Found target button (" + kw + "). Clicking...");
                                if (window.PingPinBridge && window.PingPinBridge.punchAttempted) {
                                    window.PingPinBridge.punchAttempted('$actionType');
                                }
                                el.click();
                                verifyPunchSuccess(initialBodyText);
                                return true;
                            }
                        }
                    }
                    return false;
                }

                function verifyPunchSuccess(initialBodyText) {
                    var isCheckIn = ${isCheckIn};
                    var actionLabel = isCheckIn ? "check-in" : "check-out";
                    notifyStatus("Punch clicked. Checking for location/modal confirmation...");

                    var confirmKeywords = isCheckIn ? [
                        'confirm', 'confirm check-in', 'confirm check in', 'confirm punch',
                        'yes', 'yes, check in', 'yes, check-in', 'submit', 'proceed', 'save',
                        'mark attendance', 'mark present', 'punch now', 'clock in now', 'confirm location', 'ok'
                    ] : [
                        'confirm', 'confirm check-out', 'confirm check out', 'confirm punch',
                        'yes', 'yes, check out', 'yes, check-out', 'submit', 'proceed', 'save',
                        'mark checkout', 'mark check-out', 'punch out now', 'clock out now', 'confirm location', 'ok'
                    ];

                    var successKeywords = isCheckIn ?
                        ['already checked in', 'already punched', 'checked in at', 'punched in at', 'already clocked in', 'shift in progress', 'clocked in', 'attendance marked', 'punch recorded', 'check in successful', 'checked in successfully'] :
                        ['already checked out', 'already punched out', 'checked out at', 'punched out at', 'clocked out', 'shift ended', 'punched out successfully', 'check out successful'];

                    var modalClicked = false;
                    var vAttempts = 0;
                    var maxVAttempts = 24; // Poll every 500ms for 12 seconds

                    var vTimer = setInterval(function() {
                        vAttempts++;

                        if (!modalClicked) {
                            var modalCandidates = Array.from(document.querySelectorAll(
                                '[role="dialog"] button, .modal button, .dialog button, [class*="modal"] button, [class*="popup"] button, [class*="confirm"] button, [class*="dialog"] button, button[class*="confirm"], button[id*="confirm"], .btn-primary, .button-primary, button[type="submit"]'
                            ));

                            for (var m = 0; m < modalCandidates.length; m++) {
                                var mEl = modalCandidates[m];
                                if (!isVisible(mEl)) continue;
                                var mText = getElementText(mEl);
                                if (!mText) continue;

                                for (var c = 0; c < confirmKeywords.length; c++) {
                                    if (mText === confirmKeywords[c] || (mText.length < 35 && mText.includes(confirmKeywords[c]))) {
                                        notifyStatus("Found confirmation popup (" + mText + "). Clicking to confirm...");
                                        mEl.click();
                                        modalClicked = true;
                                        break;
                                    }
                                }
                                if (modalClicked) break;
                            }
                        }

                        var currentBodyText = (document.body ? document.body.innerText : '').toLowerCase();

                        for (var s = 0; s < successKeywords.length; s++) {
                            var sk = successKeywords[s];
                            if (currentBodyText.includes(sk)) {
                                if (initialBodyText.includes(sk) && !modalClicked && vAttempts < 5) {
                                    continue;
                                }
                                clearInterval(vTimer);
                                notifyStatus("🎉 Punch verified and confirmed on portal!");
                                if (window.PingPinBridge && window.PingPinBridge.punchSuccess) {
                                    window.PingPinBridge.punchSuccess('$actionType');
                                }
                                window.__pingpin_automation_active = false;
                                return;
                            }
                        }

                        if (vAttempts >= maxVAttempts) {
                            clearInterval(vTimer);
                            window.__pingpin_automation_active = false;
                            notifyStatus("Clicked " + actionLabel + " button. Waiting for server confirmation...");
                            if (window.PingPinBridge && window.PingPinBridge.punchSuccess) {
                                window.PingPinBridge.punchSuccess('$actionType');
                            }
                        }
                    }, 500);
                }

                function checkSpecialStates() {
                    var bodyText = (document.body ? document.body.innerText : '').toLowerCase();
                    
                    var locationKeywords = [
                        'location permission', 'enable location', 'allow location', 'location disabled',
                        'location access', 'location required', 'geolocation error', 'location denied',
                        'turn on location', 'gps required', 'fetch location', 'getting location'
                    ];
                    for (var loc = 0; loc < locationKeywords.length; loc++) {
                        if (bodyText.includes(locationKeywords[loc])) {
                            notifyStatus("📍 Portal requires Location Access. Please allow location permission.");
                            return true;
                        }
                    }

                    var isCheckIn = ${isCheckIn};
                    if (isCheckIn) {
                        var alreadyInKeywords = ['already checked in', 'already punched', 'checked in at', 'punched in at', 'already clocked in', 'shift in progress'];
                        for (var a = 0; a < alreadyInKeywords.length; a++) {
                            if (bodyText.includes(alreadyInKeywords[a])) {
                                notifyStatus("✅ Already checked in today on portal!");
                                return true;
                            }
                        }
                    } else {
                        var alreadyOutKeywords = ['already checked out', 'already punched out', 'checked out at', 'punched out at', 'clocked out', 'shift ended', 'punched out successfully'];
                        for (var o = 0; o < alreadyOutKeywords.length; o++) {
                            if (bodyText.includes(alreadyOutKeywords[o])) {
                                notifyStatus("✅ Already checked out today on portal!");
                                return true;
                            }
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

                    if (attempts === 1) {
                        checkUrlMismatch();
                    }

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
                        return;
                    }

                    if (attempts >= maxAttempts) {
                        clearInterval(intervalTimer);
                        window.__pingpin_automation_active = false;
                        var btnLabel = isCheckIn ? "Check-in" : "Check-out";
                        notifyStatus("⚠️ " + btnLabel + " button not found. You can interact with the portal manually or tap 'Open in Chrome'.");
                    }
                }

                intervalTimer = setInterval(pollEngine, 800);
                pollEngine();
            })();
            """.trimIndent()
        }
    }
}
