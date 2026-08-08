// ─────────────────────────────────────────────────────────────────────────────
// tests/alarm_settings.test.js
//
// Suite: Alarm Settings UI tests
//
// Verifies that the Settings screen correctly:
//   • Displays check-in and check-out time tiles
//   • Allows changing alarm times via the time picker dialog
//   • Saves and persists the changed times
//   • Shows the new time after a page reload
//
// Run with:  npm run test:settings
// ─────────────────────────────────────────────────────────────────────────────

const {
  navigateToSettings,
  navigateToHome,
  waitForElementByUI2,
  launchApp,
} = require('../helpers');

// ── Utilities ─────────────────────────────────────────────────────────────────

/**
 * Open the time-picker for a tile labelled with `tileTextContains`,
 * set it to `hhmm`, and confirm.
 */
async function setTimePicker(tileTextContains, hhmm) {
  const tile = await waitForElementByUI2(
    `new UiSelector().textContains("${tileTextContains}")`,
    10000,
  );
  await tile.click();
  await driver.pause(800);

  const [hh, mm] = hhmm.split(':');

  // Switch to keyboard/text mode if the dial is shown
  try {
    const kbToggle = await driver.$(
      'android=new UiSelector().descriptionContains("keyboard")',
    );
    await kbToggle.waitForDisplayed({ timeout: 3000 });
    await kbToggle.click();
    await driver.pause(300);
  } catch {
    /* already in text input mode */
  }

  // Set hour
  const hourField = await driver.$(
    'android=new UiSelector().className("android.widget.EditText").instance(0)',
  );
  await hourField.clearValue();
  await hourField.setValue(hh);
  await driver.hideKeyboard();

  // Set minute
  const minField = await driver.$(
    'android=new UiSelector().className("android.widget.EditText").instance(1)',
  );
  await minField.clearValue();
  await minField.setValue(mm);
  await driver.hideKeyboard();

  // Confirm
  const okBtn = await waitForElementByUI2('new UiSelector().text("OK")', 5000);
  await okBtn.click();
  await driver.pause(600);
}

async function saveSettings() {
  const saveBtn = await waitForElementByUI2('new UiSelector().textContains("Save")', 10000);
  await saveBtn.click();
  await driver.pause(1500);
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('PingPin Alarm Settings Tests', () => {
  before(async () => {
    await launchApp();
    await navigateToSettings();
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 1 – Settings screen opens and alarm tiles are visible
  // ──────────────────────────────────────────────────────────────────────────
  it('should display Check-in Time and Check-out Time tiles', async () => {
    const checkInTile = await waitForElementByUI2(
      'new UiSelector().textContains("Check-in Time")',
      10000,
    );
    expect(await checkInTile.isDisplayed()).toBe(true);
    console.log('  ✓ Check-in Time tile visible');

    const checkOutTile = await waitForElementByUI2(
      'new UiSelector().textContains("Check-out Time")',
      10000,
    );
    expect(await checkOutTile.isDisplayed()).toBe(true);
    console.log('  ✓ Check-out Time tile visible');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 2 – Change check-in time and verify it is reflected in the tile
  // ──────────────────────────────────────────────────────────────────────────
  it('should update check-in time when changed via time picker', async () => {
    const newTime = '08:45';
    await setTimePicker('Check-in Time', newTime);

    // The tile subtitle or value should now show 08:45
    const updatedTile = await waitForElementByUI2(
      `new UiSelector().textContains("${newTime}")`,
      8000,
    );
    expect(await updatedTile.isDisplayed()).toBe(true);
    console.log(`  ✓ Check-in time updated to ${newTime} in tile`);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 3 – Change check-out time
  // ──────────────────────────────────────────────────────────────────────────
  it('should update check-out time when changed via time picker', async () => {
    const newTime = '17:17';
    await setTimePicker('Check-out Time', newTime);

    const updatedTile = await waitForElementByUI2(
      `new UiSelector().textContains("${newTime}")`,
      8000,
    );
    expect(await updatedTile.isDisplayed()).toBe(true);
    console.log(`  ✓ Check-out time updated to ${newTime} in tile`);
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 4 – Save settings and verify they persist after navigation
  // ──────────────────────────────────────────────────────────────────────────
  it('should persist alarm times after saving and reopening Settings', async () => {
    await saveSettings();
    console.log('  ✓ Settings saved');

    // Navigate away then back
    await navigateToHome();
    await driver.pause(500);
    await navigateToSettings();

    // Verify saved times are still shown
    const checkInTile = await waitForElementByUI2(
      'new UiSelector().textContains("08:45")',
      10000,
    );
    expect(await checkInTile.isDisplayed()).toBe(true);
    console.log('  ✓ Check-in time 08:45 persisted after navigation');

    const checkOutTile = await waitForElementByUI2(
      'new UiSelector().textContains("17:17")',
      10000,
    );
    expect(await checkOutTile.isDisplayed()).toBe(true);
    console.log('  ✓ Check-out time 17:17 persisted after navigation');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 5 – Cancel time picker (press Cancel) leaves the time unchanged
  // ──────────────────────────────────────────────────────────────────────────
  it('should not change check-in time when time picker is cancelled', async () => {
    // Open picker
    const tile = await waitForElementByUI2(
      'new UiSelector().textContains("Check-in Time")',
      10000,
    );
    await tile.click();
    await driver.pause(800);

    // Try to set a different time
    try {
      const kbToggle = await driver.$('android=new UiSelector().descriptionContains("keyboard")');
      await kbToggle.click();
      await driver.pause(300);
    } catch { /* ok */ }

    const hourField = await driver.$('android=new UiSelector().className("android.widget.EditText").instance(0)');
    await hourField.clearValue();
    await hourField.setValue('12');

    // Press Cancel
    const cancelBtn = await waitForElementByUI2('new UiSelector().text("Cancel")', 5000);
    await cancelBtn.click();
    await driver.pause(500);

    // Time should still be 08:45
    const unchangedTile = await waitForElementByUI2(
      'new UiSelector().textContains("08:45")',
      5000,
    );
    expect(await unchangedTile.isDisplayed()).toBe(true);
    console.log('  ✓ Check-in time unchanged (08:45) after Cancel');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // Test 6 – Midnight-spanning time (23:50) is accepted
  // ──────────────────────────────────────────────────────────────────────────
  it('should accept edge-case time 23:50 for check-in', async () => {
    await setTimePicker('Check-in Time', '23:50');

    const tile = await waitForElementByUI2(
      'new UiSelector().textContains("23:50")',
      8000,
    );
    expect(await tile.isDisplayed()).toBe(true);
    console.log('  ✓ Edge-case time 23:50 accepted');
  });

  // ──────────────────────────────────────────────────────────────────────────
  // After all – restore defaults so other test suites start clean
  // ──────────────────────────────────────────────────────────────────────────
  after(async () => {
    try {
      await setTimePicker('Check-in Time', '09:30');
      await setTimePicker('Check-out Time', '18:02');
      await saveSettings();
      console.log('  ✓ Alarm times restored to defaults (09:30 / 18:02)');
    } catch (e) {
      console.warn('  ⚠ Could not restore alarm defaults:', e.message);
    }
  });
});
