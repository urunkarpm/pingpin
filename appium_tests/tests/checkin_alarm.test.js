// ─────────────────────────────────────────────────────────────────────────────
// tests/checkin_alarm.test.js
//
// Suite: Full check-in alarm flow
//
// Steps:
//   1. Set check-in time to "now + 1 minute" via Settings
//   2. Save settings (which reschedules alarm via NotificationService)
//   3. Advance device clock by 65 seconds via ADB shell
//   4. Wait for AlarmRingingScreen to appear
//   5. Validate all check-in UI elements
//   6. Tap CHECK-IN → verify alarm stops + screen dismisses
//
// Run with:  npm run test:checkin
// ─────────────────────────────────────────────────────────────────────────────

const {
  navigateToSettings,
  navigateToHome,
  waitForElementByUI2,
  assertAlarmScreenVisible,
  waitForAlarmScreenGone,
  dismissAlarmScreen,
  launchApp,
  adviceDeviceClock,
} = require('../helpers');

// ── Utilities ─────────────────────────────────────────────────────────────────

/**
 * Return an HH:MM string representing now + offsetMinutes (local time).
 */
function timeInMinutes(offsetMinutes) {
  const d = new Date(Date.now() + offsetMinutes * 60 * 1000);
  const hh = String(d.getHours()).padStart(2, '0');
  const mm = String(d.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

/**
 * Set the check-in time picker in the Settings screen.
 * PingPin uses a TimePickerDialog opened by a list tile.
 * We tap the tile, then interact with the time-picker dialog.
 *
 * @param {string} hhmm  – e.g. "09:30"
 */
async function setCheckInTime(hhmm) {
  // Tap "Check-in Time" tile
  const tile = await waitForElementByUI2(
    'new UiSelector().textContains("Check-in Time")',
    10000,
  );
  await tile.click();
  await driver.pause(800);

  const [hh, mm] = hhmm.split(':');

  // Flutter's TimePickerDialog – hour and minute inputs
  // In dial mode, we switch to text (keyboard) mode first for reliability
  try {
    const keyboardToggle = await driver.$('android=new UiSelector().descriptionContains("keyboard")');
    await keyboardToggle.click();
    await driver.pause(300);
  } catch {
    // already in text mode or toggle not found – continue
  }

  // Hour field
  const hourField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(0)');
  await hourField.clearValue();
  await hourField.setValue(hh);

  // Minute field
  const minField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(1)');
  await minField.clearValue();
  await minField.setValue(mm);

  // Confirm
  const okBtn = await waitForElementByUI2('new UiSelector().text("OK")', 5000);
  await okBtn.click();
  await driver.pause(500);
}

/**
 * Save settings by tapping the floating "Save" button.
 */
async function saveSettings() {
  const saveBtn = await waitForElementByUI2(
    'new UiSelector().textContains("Save")',
    10000,
  );
  await saveBtn.click();
  await driver.pause(1500); // wait for snack-bar confirmation
  console.log('  ✓ Settings saved');
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('PingPin Check-In Alarm Flow', () => {
  before(async () => {
    await launchApp();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 1 – Schedule check-in alarm 1 minute from now
  // ──────────────────────────────────────────────────────────────────────────
  it('should schedule the check-in alarm for 1 minute from now', async () => {
    await navigateToSettings();

    const targetTime = timeInMinutes(1);
    console.log(`  Setting check-in time to: ${targetTime}`);

    await setCheckInTime(targetTime);
    await saveSettings();

    console.log(`  ✓ Check-in alarm scheduled for ${targetTime}`);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 2 – Advance clock and verify alarm fires
  // ──────────────────────────────────────────────────────────────────────────
  it('should fire the check-in alarm after time advances', async () => {
    // Advance the device clock by 65 seconds to cross the alarm threshold
    console.log('  Advancing device clock by 65 seconds…');
    await adviceDeviceClock(65);
    await driver.pause(2000); // give AlarmManager time to fire

    // Wait up to 30 s for the alarm screen to appear
    await assertAlarmScreenVisible('CHECK-IN ALARM');
    console.log('  ✓ Check-in AlarmRingingScreen appeared on device');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 3 – Validate all check-in screen elements
  // ──────────────────────────────────────────────────────────────────────────
  it('should display correct check-in UI elements', async () => {
    // Badge
    const badge = await waitForElementByUI2(
      'new UiSelector().text("CHECK-IN ALARM")',
      10000,
    );
    expect(await badge.isDisplayed()).toBe(true);
    console.log('  ✓ Badge: CHECK-IN ALARM');

    // Prompt text
    const prompt = await waitForElementByUI2(
      'new UiSelector().textContains("It is time to check in")',
      5000,
    );
    expect(await prompt.isDisplayed()).toBe(true);
    console.log('  ✓ Prompt text visible');

    // CHECK-IN button
    const checkInBtn = await waitForElementByUI2(
      'new UiSelector().textContains("CHECK-IN (OPEN PORTAL)")',
      5000,
    );
    expect(await checkInBtn.isDisplayed()).toBe(true);
    console.log('  ✓ CHECK-IN (OPEN PORTAL) button visible');

    // APPLY FOR LEAVE button
    const leaveBtn = await waitForElementByUI2(
      'new UiSelector().textContains("APPLY FOR LEAVE")',
      5000,
    );
    expect(await leaveBtn.isDisplayed()).toBe(true);
    console.log('  ✓ APPLY FOR LEAVE button visible');

    // No CHECK-OUT button should appear on check-in screen
    const checkOutBtns = await driver.$$('android=new UiSelector().textContains("CHECK-OUT (OPEN PORTAL)")');
    expect(checkOutBtns.length).toBe(0);
    console.log('  ✓ No CHECK-OUT button on check-in screen');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 4 – Dismiss via CHECK-IN button
  // ──────────────────────────────────────────────────────────────────────────
  it('should dismiss check-in alarm when CHECK-IN button is tapped', async () => {
    await dismissAlarmScreen('CHECK-IN (OPEN PORTAL)');
    await waitForAlarmScreenGone(15000);
    console.log('  ✓ Alarm dismissed – screen closed');

    // Navigate back to app (portal may open browser; bring app to foreground)
    await launchApp();
    await navigateToHome();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 5 – Re-trigger and dismiss via "Apply for Leave"
  // ──────────────────────────────────────────────────────────────────────────
  it('should dismiss check-in alarm via "APPLY FOR LEAVE" button', async () => {
    // Quick 5-second trigger using the built-in test button
    await navigateToSettings();
    await driver.execute('mobile: scroll', { direction: 'down', percent: 0.7 });

    const testBtn = await waitForElementByUI2(
      'new UiSelector().textContains("Test Check-in Alarm")',
      10000,
    );
    await testBtn.click();

    await assertAlarmScreenVisible('CHECK-IN ALARM');
    console.log('  ✓ Alarm appeared for leave-dismissal test');

    await dismissAlarmScreen('APPLY FOR LEAVE');
    await waitForAlarmScreenGone(15000);
    console.log('  ✓ APPLY FOR LEAVE dismissed the alarm screen');

    await launchApp();
    await navigateToHome();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // After all – restore a sane alarm time so daily alarms still fire correctly
  // ──────────────────────────────────────────────────────────────────────────
  after(async () => {
    try {
      await navigateToSettings();
      await setCheckInTime('09:30'); // restore default
      await saveSettings();
      console.log('  ✓ Check-in time restored to 09:30');
    } catch (e) {
      console.warn('  ⚠ Could not restore check-in time:', e.message);
    }
  });
});
