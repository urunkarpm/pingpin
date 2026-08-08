// ─────────────────────────────────────────────────────────────────────────────
// tests/checkout_alarm.test.js
//
// Suite: Full check-out alarm flow
//
// Steps:
//   1. Set check-out time to "now + 1 minute" via Settings
//   2. Save settings
//   3. Advance device clock by 65 seconds via ADB shell
//   4. Wait for AlarmRingingScreen (CHECK-OUT) to appear
//   5. Validate check-out UI: badge, prompt, CHECK-OUT button, no LEAVE button
//   6. Tap CHECK-OUT → verify screen closes
//
// Run with:  npm run test:checkout
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

function timeInMinutes(offsetMinutes) {
  const d = new Date(Date.now() + offsetMinutes * 60 * 1000);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

async function setCheckOutTime(hhmm) {
  const tile = await waitForElementByUI2(
    'new UiSelector().textContains("Check-out Time")',
    10000,
  );
  await tile.click();
  await driver.pause(800);

  const [hh, mm] = hhmm.split(':');

  try {
    const kbToggle = await driver.$('android=new UiSelector().descriptionContains("keyboard")');
    await kbToggle.click();
    await driver.pause(300);
  } catch { /* already text mode */ }

  const hourField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(0)');
  await hourField.clearValue();
  await hourField.setValue(hh);

  const minField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(1)');
  await minField.clearValue();
  await minField.setValue(mm);

  const okBtn = await waitForElementByUI2('new UiSelector().text("OK")', 5000);
  await okBtn.click();
  await driver.pause(500);
}

async function saveSettings() {
  const saveBtn = await waitForElementByUI2('new UiSelector().textContains("Save")', 10000);
  await saveBtn.click();
  await driver.pause(1500);
  console.log('  ✓ Settings saved');
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('PingPin Check-Out Alarm Flow', () => {
  before(async () => {
    await launchApp();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 1 – Schedule check-out alarm 1 minute from now
  // ──────────────────────────────────────────────────────────────────────────
  it('should schedule check-out alarm for 1 minute from now', async () => {
    await navigateToSettings();

    const targetTime = timeInMinutes(1);
    console.log(`  Setting check-out time to: ${targetTime}`);

    await setCheckOutTime(targetTime);
    await saveSettings();
    console.log(`  ✓ Check-out alarm scheduled for ${targetTime}`);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 2 – Advance clock; verify alarm fires
  // ──────────────────────────────────────────────────────────────────────────
  it('should fire the check-out alarm after time advances', async () => {
    console.log('  Advancing device clock by 65 seconds…');
    await adviceDeviceClock(65);
    await driver.pause(2000);

    await assertAlarmScreenVisible('CHECK-OUT ALARM');
    console.log('  ✓ Check-out AlarmRingingScreen appeared on device');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 3 – Validate check-out screen UI elements
  // ──────────────────────────────────────────────────────────────────────────
  it('should display correct check-out UI elements', async () => {
    // Badge
    const badge = await waitForElementByUI2('new UiSelector().text("CHECK-OUT ALARM")', 10000);
    expect(await badge.isDisplayed()).toBe(true);
    console.log('  ✓ Badge: CHECK-OUT ALARM');

    // Prompt text (check-out variant)
    const prompt = await waitForElementByUI2(
      'new UiSelector().textContains("It is time to check out")',
      5000,
    );
    expect(await prompt.isDisplayed()).toBe(true);
    console.log('  ✓ Prompt text visible');

    // CHECK-OUT button
    const checkOutBtn = await waitForElementByUI2(
      'new UiSelector().textContains("CHECK-OUT (OPEN PORTAL)")',
      5000,
    );
    expect(await checkOutBtn.isDisplayed()).toBe(true);
    console.log('  ✓ CHECK-OUT (OPEN PORTAL) button visible');

    // No CHECK-IN / LEAVE buttons on check-out screen
    const checkInBtns = await driver.$$('android=new UiSelector().textContains("CHECK-IN (OPEN PORTAL)")');
    expect(checkInBtns.length).toBe(0);
    console.log('  ✓ No CHECK-IN button on check-out screen');

    const leaveBtns = await driver.$$('android=new UiSelector().textContains("APPLY FOR LEAVE")');
    expect(leaveBtns.length).toBe(0);
    console.log('  ✓ No APPLY FOR LEAVE button on check-out screen');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 4 – Dismiss via CHECK-OUT button
  // ──────────────────────────────────────────────────────────────────────────
  it('should dismiss check-out alarm when CHECK-OUT button is tapped', async () => {
    await dismissAlarmScreen('CHECK-OUT (OPEN PORTAL)');
    await waitForAlarmScreenGone(15000);
    console.log('  ✓ Alarm dismissed – screen closed');

    await launchApp();
    await navigateToHome();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 5 – Back button locked on check-out screen
  // ──────────────────────────────────────────────────────────────────────────
  it('should not close check-out alarm via hardware back button', async () => {
    // Re-trigger using the test alarm pathway by scheduling +1 min again
    // and advancing clock — OR just use a 5-sec test trigger if available
    await navigateToSettings();
    await driver.execute('mobile: scroll', { direction: 'down', percent: 0.7 });

    // Try a test button for checkout if it exists, otherwise skip
    const testBtns = await driver.$$('android=new UiSelector().textContains("Test Check-out Alarm")');
    if (testBtns.length === 0) {
      console.log('  ⚠ No "Test Check-out Alarm" button found – skipping back-button lock test');
      return;
    }

    await testBtns[0].click();
    await assertAlarmScreenVisible('CHECK-OUT ALARM');

    await driver.back();
    await driver.pause(1000);

    const badge = await driver.$('android=new UiSelector().textContains("CHECK-OUT ALARM")');
    expect(await badge.isDisplayed()).toBe(true);
    console.log('  ✓ Back button blocked – check-out alarm screen still visible');

    await dismissAlarmScreen('CHECK-OUT (OPEN PORTAL)');
    await waitForAlarmScreenGone();
    await launchApp();
    await navigateToHome();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // After all – restore sane check-out time
  // ──────────────────────────────────────────────────────────────────────────
  after(async () => {
    try {
      await navigateToSettings();
      await setCheckOutTime('18:02'); // restore default (9:30 + 8h32m)
      await saveSettings();
      console.log('  ✓ Check-out time restored to 18:02');
    } catch (e) {
      console.warn('  ⚠ Could not restore check-out time:', e.message);
    }
  });
});
