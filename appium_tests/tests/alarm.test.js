// ─────────────────────────────────────────────────────────────────────────────
// tests/alarm.test.js
//
// Suite: Quick alarm fire tests
//
// PingPin has a built-in "Test Alarm" button in Settings that schedules the
// alarm to fire in exactly 5 seconds.  These tests:
//   1. Navigate to Settings → tap "Test Check-in Alarm"
//   2. Wait ≤15 s for the AlarmRingingScreen to appear
//   3. Verify all expected UI elements are visible
//   4. Dismiss the alarm and verify the screen closes
//
// Run with:  npm run test:alarm
// ─────────────────────────────────────────────────────────────────────────────

const {
  navigateToSettings,
  waitForElementByUI2,
  assertAlarmScreenVisible,
  waitForAlarmScreenGone,
  dismissAlarmScreen,
  launchApp,
} = require('../helpers');

describe('PingPin Quick Alarm Tests (5-second trigger)', () => {
  before(async () => {
    await launchApp();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 1 – Test Check-in Alarm button triggers the alarm screen
  // ──────────────────────────────────────────────────────────────────────────
  it('should show AlarmRingingScreen when "Test Check-in Alarm" is tapped', async () => {
    await navigateToSettings();

    // Scroll down to find the "Test Check-in Alarm" button using UiScrollable
    let testBtn;
    try {
      testBtn = await waitForElementByUI2(
        'new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains("Test Check-in Alarm"))',
        5000,
      );
    } catch {
      testBtn = await waitForElementByUI2(
        'new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("Test Check-in Alarm"))',
        5000,
      );
    }
    await testBtn.click();

    console.log('  ✓ Tapped "Test Check-in Alarm" – waiting up to 15 s for alarm screen…');

    // The alarm fires in 5 s; we wait up to 15 s to account for scheduling latency
    await assertAlarmScreenVisible('CHECK-IN ALARM');
    console.log('  ✓ AlarmRingingScreen appeared with CHECK-IN ALARM badge');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 2 – Alarm ringing screen shows the correct elements
  // ──────────────────────────────────────────────────────────────────────────
  it('should display correct elements on the alarm screen', async () => {
    // Badge – verify after screen loads
    await assertAlarmScreenVisible('CHECK-IN ALARM');

    // Action buttons
    const checkInBtn = await waitForElementByUI2(
      'new UiSelector().descriptionContains("CHECK-IN (OPEN PORTAL)")',
      10000,
    );
    expect(await checkInBtn.isDisplayed()).toBe(true);
    console.log('  ✓ "CHECK-IN (OPEN PORTAL)" button is visible');

    const leaveBtn = await waitForElementByUI2(
      'new UiSelector().descriptionContains("APPLY FOR LEAVE")',
      10000,
    );
    expect(await leaveBtn.isDisplayed()).toBe(true);
    console.log('  ✓ "APPLY FOR LEAVE" button is visible');

    // Time label (HH:MM)
    const timeLabel = await driver.$(
      `android=new UiSelector().descriptionMatches("^[0-2][0-9]:[0-5][0-9]$")`,
    );
    expect(await timeLabel.isDisplayed()).toBe(true);
    console.log('  ✓ Time label (HH:MM) is visible on alarm screen');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 3 – Dismiss via CHECK-IN button (opens portal, closes alarm)
  // ──────────────────────────────────────────────────────────────────────────
  it('should dismiss the alarm screen when "CHECK-IN (OPEN PORTAL)" is tapped', async () => {
    await assertAlarmScreenVisible('CHECK-IN ALARM');

    await dismissAlarmScreen('CHECK-IN (OPEN PORTAL)');
    await waitForAlarmScreenGone(15000);
    console.log('  ✓ Alarm screen closed after CHECK-IN tap');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 4 – Re-trigger alarm and dismiss via APPLY FOR LEAVE
  // ──────────────────────────────────────────────────────────────────────────
  it('should dismiss via "APPLY FOR LEAVE" button', async () => {
    await launchApp();
    await navigateToSettings();

    const testBtn = await waitForElementByUI2(
      'new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("Test Check-in Alarm"))',
      15000,
    );
    await testBtn.click();

    await assertAlarmScreenVisible('CHECK-IN ALARM');
    console.log('  ✓ Alarm ringing screen appeared again');

    await dismissAlarmScreen('APPLY FOR LEAVE');
    await waitForAlarmScreenGone(15000);
    console.log('  ✓ Alarm screen closed after APPLY FOR LEAVE tap');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 5 – Back button is locked (PopScope canPop: false)
  // ──────────────────────────────────────────────────────────────────────────
  it('should not close the alarm screen when the hardware back button is pressed', async () => {
    await launchApp();
    await navigateToSettings();

    const testBtn = await waitForElementByUI2(
      'new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains("Test Check-in Alarm"))',
      15000,
    );
    await testBtn.click();

    await assertAlarmScreenVisible('CHECK-IN ALARM');

    // Press back – screen should remain because canPop is false
    await driver.back();
    await driver.pause(1000);

    // Alarm screen must still be displayed
    const badge = await driver.$('android=new UiSelector().textContains("CHECK-IN ALARM")');
    expect(await badge.isDisplayed()).toBe(true);
    console.log('  ✓ Back button blocked – alarm screen still visible');

    // Clean up
    await dismissAlarmScreen('CHECK-IN (OPEN PORTAL)');
    await waitForAlarmScreenGone();
  });
});
