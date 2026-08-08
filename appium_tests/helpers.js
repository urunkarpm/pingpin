// ─────────────────────────────────────────────────────────────────────────────
// helpers.js  –  Shared Appium/WebdriverIO utilities for PingPin alarm tests
// ─────────────────────────────────────────────────────────────────────────────

const PACKAGE = 'com.urunkarpm.pingpin';

/**
 * Wait for an element identified by its accessibility-id (content-desc on Android).
 *
 * @param {string} label  - text / content-description of the element
 * @param {number} timeout - ms to wait (default 3000 ms for fast execution)
 */
async function waitForElementByAccessibility(label, timeout = 3000) {
  const el = await driver.$(`~${label}`);
  await el.waitForDisplayed({ timeout });
  return el;
}

/**
 * Wait for an element by its UiAutomator2 locator string.
 *
 * @param {string} uiSelector  e.g. 'new UiSelector().text("CHECK-IN (OPEN PORTAL)")'
 * @param {number} timeout
 */
async function waitForElementByUI2(uiSelector, timeout = 3000) {
  const el = await driver.$(`android=${uiSelector}`);
  await el.waitForDisplayed({ timeout });
  return el;
}

/**
 * Wait for an element by XPath — last resort when no label or text is available.
 */
async function waitForElementByXPath(xpath, timeout = 3000) {
  const el = await driver.$(xpath);
  await el.waitForDisplayed({ timeout });
  return el;
}

/**
 * Navigate to the PingPin Settings tab using the bottom-navigation bar.
 */
async function navigateToSettings() {
  try {
    const settingsTab = await driver.$('android=new UiSelector().descriptionContains("Settings")');
    await settingsTab.waitForDisplayed({ timeout: 3000 });
    await settingsTab.click();
  } catch {
    // Tap at bottom right capsule coordinates (860, 2220)
    const { width, height } = await driver.getWindowSize();
    await driver.action('pointer')
      .move({ duration: 0, x: Math.floor(width * 0.83), y: Math.floor(height * 0.95) })
      .down({ button: 0 })
      .pause(100)
      .up({ button: 0 })
      .perform();
  }
}

/**
 * Navigate back to the Home tab.
 */
async function navigateToHome() {
  const homeTab = await driver.$('~Calendar');
  await homeTab.click();
}

/**
 * Press the Android back button.
 */
async function pressBack() {
  await driver.back();
}

/**
 * Launch PingPin from the device home screen if it is not already running.
 */
async function launchApp() {
  await driver.activateApp(PACKAGE);
}

/**
 * Dismiss the alarm ringing screen by pressing the primary action button label.
 *
 * @param {'CHECK-IN (OPEN PORTAL)'|'CHECK-OUT (OPEN PORTAL)'|'APPLY FOR LEAVE'} buttonLabel
 */
async function dismissAlarmScreen(buttonLabel) {
  const btn = await waitForElementByUI2(
    `new UiSelector().descriptionContains("${buttonLabel}")`,
    10000,
  );
  await btn.click();
}

/**
 * Verify that the Alarm Ringing Screen is currently displayed.
 */
async function assertAlarmScreenVisible(expectedBadge) {
  const badge = await waitForElementByUI2(
    `new UiSelector().descriptionContains("${expectedBadge}")`,
    10000,
  );
  const displayed = await badge.isDisplayed();
  if (!displayed) throw new Error(`Expected alarm badge "${expectedBadge}" to be visible`);
}

/**
 * Wait for the alarm ringing screen to disappear (i.e., after dismiss).
 */
async function waitForAlarmScreenGone(timeout = 5000) {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    try {
      const el = await driver.$('android=new UiSelector().descriptionContains("ALARM")');
      const visible = await el.isDisplayed();
      if (!visible) return;
    } catch {
      return; // element gone from DOM
    }
    await driver.pause(200);
  }
  throw new Error('Alarm screen did not disappear within timeout');
}

/**
 * Use ADB to advance device clock.
 */
async function adviceDeviceClock(seconds) {
  const result = await driver.executeScript('mobile: shell', [
    {
      command: 'date',
      args: ['-s', `@$(($(date +%s) + ${seconds}))`],
    },
  ]);
  console.log(`  [clock] Advanced device clock by ${seconds}s → ${result}`);
}

module.exports = {
  PACKAGE,
  waitForElementByAccessibility,
  waitForElementByUI2,
  waitForElementByXPath,
  navigateToSettings,
  navigateToHome,
  pressBack,
  launchApp,
  dismissAlarmScreen,
  assertAlarmScreenVisible,
  waitForAlarmScreenGone,
  adviceDeviceClock,
};
