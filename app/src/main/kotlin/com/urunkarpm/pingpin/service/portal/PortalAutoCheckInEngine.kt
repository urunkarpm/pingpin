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
                    "mark attendance", "mark present", "swipe in", "swipe-in",
                    "web check in", "web check-in", "web checkin", "in punch",
                    "punch in now", "check in now", "punch now", "mark in", "punch"
                )
            } else {
                listOf(
                    "check out", "check-out", "clock out", "clock-out", "punch out", "punch-out",
                    "web-punch out", "web punch out", "checkout", "punchout", "clockout",
                    "mark checkout", "mark check-out", "out punch", "punch out now", "end shift",
                    "swipe out", "swipe-out", "web check-out", "web check out", "web checkout",
                    "check out now", "clock out now"
                )
            }

            val activeKeywords = if (parsedCustomKeywords.isNotEmpty()) parsedCustomKeywords else defaultKeywords
            val keywordsJsArray = activeKeywords.joinToString(prefix = "[", postfix = "]") { "'${it.replace("'", "\\'")}'" }

            return """
            (function() {
                if (window.__pingpin_automation_active) return;
                window.__pingpin_automation_active = true;

                var isCheckIn = ${isCheckIn};
                var actionLabel = isCheckIn ? "Check-in" : "Check-out";
                var targetKeywords = $keywordsJsArray;
                var targetUrl = '$escapedTargetUrl';

                console.log("[PingPin] Portal Engine started for action: " + actionLabel);

                function notifyStatus(msg) {
                    console.log("[PingPin] " + msg);
                    if (window.PingPinBridge && window.PingPinBridge.updateStatus) {
                        window.PingPinBridge.updateStatus(msg);
                    }
                }

                notifyStatus("🚀 Automation engine started for " + actionLabel);

                function checkUrlMismatch() {
                    try {
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

                    var target = el.closest('button, a, [role="button"], input[type="button"], input[type="submit"]') || el;

                    try {
                        target.dispatchEvent(new PointerEvent('pointerdown', { bubbles: true, cancelable: true, view: window }));
                        target.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, view: window }));
                        target.dispatchEvent(new PointerEvent('pointerup', { bubbles: true, cancelable: true, view: window }));
                        target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true, cancelable: true, view: window }));
                        target.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
                    } catch(e){}

                    try {
                        target.click();
                    } catch(e){}

                    if (target !== el) {
                        try { el.click(); } catch(e){}
                    }

                    if (target.form) {
                        try {
                            if (typeof target.form.requestSubmit === 'function') {
                                target.form.requestSubmit();
                            } else if (typeof target.form.submit === 'function') {
                                target.form.submit();
                            }
                        } catch(e){}
                    }
                }

                function isVisible(el) {
                    return !!(el.offsetWidth || el.offsetHeight || el.getClientRects().length);
                }

                function getElementText(el) {
                    var raw = (el.innerText || el.textContent || el.value || el.getAttribute('aria-label') || el.getAttribute('title') || el.getAttribute('data-original-title') || el.getAttribute('data-title') || '');
                    return raw.toLowerCase().replace(/\s+/g, ' ').trim();
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

                function tryAutoPunch(attemptNum, maxAttemptsTotal) {
                    if (!${autoPunch}) return { success: false, reason: 'disabled', visibleButtons: [] };

                    var blacklist = isCheckIn ? [
                        'log in', 'login', 'sign in', 'signin', 'log out', 'logout', 'sign out', 'signout',
                        'check out', 'checkout', 'clock out', 'clockout', 'punch out', 'punchout', 'swipe out',
                        'history', 'records', 'report', 'reports', 'summary', 'regularization', 'regularize', 'request',
                        'register', 'forgot password', 'user', 'email', 'password'
                    ] : [
                        'log in', 'login', 'sign in', 'signin', 'log out', 'logout', 'sign out', 'signout',
                        'check in', 'checkin', 'clock in', 'clockin', 'punch in', 'punchin', 'swipe in',
                        'history', 'records', 'report', 'reports', 'summary', 'regularization', 'regularize', 'request',
                        'register', 'forgot password', 'user', 'email', 'password'
                    ];

                    var docs = [document];
                    try {
                        var frames = document.querySelectorAll('iframe, frame');
                        for (var f = 0; f < frames.length; f++) {
                            try {
                                var fDoc = frames[f].contentDocument || (frames[f].contentWindow && frames[f].contentWindow.document);
                                if (fDoc && fDoc.body) docs.push(fDoc);
                            } catch(e){}
                        }
                    } catch(e){}

                    var visibleButtonLabels = [];
                    var totalCandidates = 0;

                    for (var d = 0; d < docs.length; d++) {
                        var doc = docs[d];
                        var candidateElements = Array.from(doc.querySelectorAll(
                            'button, input[type="button"], input[type="submit"], input[type="image"], a, div[role="button"], span[role="button"], div[onclick], span[onclick], .btn, .button, [class*="btn"], [class*="button"], [class*="punch"], [class*="checkin"], [class*="checkout"], [class*="attendance"], [class*="action"]'
                        ));
                        totalCandidates += candidateElements.length;

                        for (var i = 0; i < candidateElements.length; i++) {
                            var el = candidateElements[i];
                            if (!isVisible(el)) continue;

                            // Skip container cards/sections that wrap nested buttons or links
                            var tag = (el.tagName || '').toLowerCase();
                            var isRealButton = (tag === 'button' || tag === 'a' || tag === 'input' || el.getAttribute('role') === 'button');
                            if (!isRealButton && el.querySelector('button, a, input[type="button"], input[type="submit"], [role="button"]')) {
                                continue;
                            }

                            var text = getElementText(el);
                            if (!text) continue;

                            // Collect distinct visible button labels for user diagnostics
                            if (text.length <= 40 && visibleButtonLabels.indexOf(text) === -1 && visibleButtonLabels.length < 8) {
                                visibleButtonLabels.push(text);
                            }

                            if (text.length > 80) continue;

                            // Check blacklist
                            var isBlacklisted = false;
                            for (var b = 0; b < blacklist.length; b++) {
                                var blItem = blacklist[b];
                                if (text === blItem || text.startsWith(blItem + ' ') || text.endsWith(' ' + blItem)) {
                                    isBlacklisted = true;
                                    break;
                                }
                            }
                            if (isBlacklisted) continue;

                            // Check target keywords
                            var matchedKeyword = null;
                            for (var k = 0; k < targetKeywords.length; k++) {
                                var kw = targetKeywords[k];
                                if (kw === 'punch' || kw === 'mark in' || kw === 'in punch' || kw === 'out punch') {
                                    if (text === kw || text === kw + ' in' || text === kw + ' out' || text === 'web ' + kw || text === kw + ' now' || text.startsWith(kw + ' ') || text.endsWith(' ' + kw)) {
                                        matchedKeyword = kw;
                                        break;
                                    }
                                } else {
                                    if (text === kw || text.startsWith(kw + ' ') || text.endsWith(' ' + kw) || text.includes(kw)) {
                                        matchedKeyword = kw;
                                        break;
                                    }
                                }
                            }

                            if (matchedKeyword) {
                                var elId = el.id ? '#' + el.id : '';
                                var elClass = (el.className && typeof el.className === 'string') ? '.' + el.className.trim().split(/\s+/).slice(0, 2).join('.') : '';
                                var elDescriptor = '<' + tag + elId + elClass + '>';
                                var initialBodyText = (document.body ? document.body.innerText : '').toLowerCase();

                                notifyStatus("🎯 Found " + actionLabel + " button: '" + text + "' on " + elDescriptor + " (matched '" + matchedKeyword + "'). Clicking...");

                                if (window.PingPinBridge && window.PingPinBridge.punchAttempted) {
                                    window.PingPinBridge.punchAttempted('$actionType');
                                }

                                clickElement(el);
                                verifyPunchSuccess(initialBodyText, text, elDescriptor);
                                return { success: true, visibleButtons: visibleButtonLabels };
                            }
                        }
                    }

                    return {
                        success: false,
                        totalCandidates: totalCandidates,
                        visibleButtons: visibleButtonLabels
                    };
                }

                function verifyPunchSuccess(initialBodyText, clickedButtonText, elDesc) {
                    notifyStatus("🖱️ Clicked " + (clickedButtonText ? "'" + clickedButtonText + "'" : "button") + " (" + (elDesc || "element") + "). Waiting for confirmation...");

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
                        try {
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
                                            notifyStatus("💬 Found confirmation popup: '" + mText + "'. Clicking to confirm...");
                                            clickElement(mEl);
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
                                    notifyStatus("🎉 " + actionLabel + " confirmed! Server response: '" + sk + "'");
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
                                notifyStatus("✅ Clicked '" + (clickedButtonText || actionLabel) + "'. No server errors reported. Auto punch completed.");
                                if (window.PingPinBridge && window.PingPinBridge.punchSuccess) {
                                    window.PingPinBridge.punchSuccess('$actionType');
                                }
                            }
                        } catch(e) {
                            clearInterval(vTimer);
                            window.__pingpin_automation_active = false;
                            notifyStatus("❌ Error during verification: " + (e && e.message ? e.message : e));
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
                    try {
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

                        if (!${autoPunch}) {
                            notifyStatus("ℹ️ Auto-punch is disabled in settings. You can mark attendance manually on the portal.");
                            clearInterval(intervalTimer);
                            window.__pingpin_automation_active = false;
                            return;
                        }

                        var punchResult = tryAutoPunch(attempts, maxAttempts);
                        if (punchResult.success) {
                            clearInterval(intervalTimer);
                            return;
                        }

                        if (attempts >= maxAttempts) {
                            clearInterval(intervalTimer);
                            window.__pingpin_automation_active = false;
                            var buttonsFoundStr = (punchResult.visibleButtons && punchResult.visibleButtons.length > 0)
                                ? punchResult.visibleButtons.map(function(b){ return "'" + b + "'"; }).join(', ')
                                : "none detected";
                            var kwListStr = targetKeywords.map(function(k){ return "'" + k + "'"; }).join(', ');

                            notifyStatus(
                                "⚠️ Could not find " + actionLabel + " button after " + maxAttempts + " attempts (12s).\n" +
                                "• Buttons detected on page: [" + buttonsFoundStr + "]\n" +
                                "• Searched for keywords: [" + kwListStr + "]\n" +
                                "• Tip: If your portal uses a different button name, add it in Settings > Custom Keywords or tap manually."
                            );
                        } else {
                            var buttonsSummary = (punchResult.visibleButtons && punchResult.visibleButtons.length > 0)
                                ? punchResult.visibleButtons.slice(0, 5).map(function(b){ return "'" + b + "'"; }).join(', ')
                                : "searching DOM";
                            notifyStatus("🔍 Scanning for " + actionLabel + " button (attempt " + attempts + "/" + maxAttempts + ")... Visible buttons: [" + buttonsSummary + "]");
                        }
                    } catch(err) {
                        clearInterval(intervalTimer);
                        window.__pingpin_automation_active = false;
                        notifyStatus("❌ Engine script error: " + (err && err.message ? err.message : err));
                    }
                }

                intervalTimer = setInterval(pollEngine, 800);
                pollEngine();
            })();
            """.trimIndent()
        }
    }
}
