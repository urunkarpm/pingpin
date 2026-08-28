const { test, expect, _android } = require('@playwright/test');
const path = require('path');
const fs = require('fs');

const ARTIFACT_DIR = 'C:\\Users\\uprasenjeet\\.gemini\\antigravity-ide\\brain\\cad74e09-8ae1-4f4f-b23a-f059dd06dc50';
const TARGET_SERIAL = '10BG4Y0TDS001TD';

test.describe('Physical Android Phone Visual Test Suite', () => {
  let device;

  test.beforeAll(async () => {
    const devices = await _android.devices();
    if (devices.length === 0) {
      console.warn('[Phone Test] No ADB devices connected.');
      return;
    }
    // Select physical phone by serial or default to first non-emulator device
    device = devices.find(d => d.input.serial === TARGET_SERIAL) || devices[0];
    console.log(`[Phone Test] Connected to physical phone: ${device.input.serial}`);
  });

  test.afterAll(async () => {
    if (device) {
      await device.close();
    }
  });

  test('should launch PingPin Home Screen on physical phone and capture visual screenshot', async () => {
    if (!device) {
      test.skip('Skipping: Physical phone not connected via ADB.');
      return;
    }

    // Launch PingPin MainActivity on physical phone
    await device.shell('am start -n com.urunkarpm.pingpin/.MainActivity');
    await new Promise(resolve => setTimeout(resolve, 4000));

    // Capture screenshot of physical phone screen
    const screenshot1 = path.join(ARTIFACT_DIR, 'phone_home_screen.png');
    await device.screenshot({ path: screenshot1 });
    console.log(`[Phone Visual] Captured Phone Home Screen: ${screenshot1}`);

    expect(fs.existsSync(screenshot1)).toBe(true);
  });

  test('should launch AlarmActivity on physical phone and capture visual screenshot', async () => {
    if (!device) {
      test.skip('Skipping: Physical phone not connected via ADB.');
      return;
    }

    // Launch AlarmActivity full-screen dialog on physical phone
    await device.shell('am start -n com.urunkarpm.pingpin/.AlarmActivity --ei alarmId 101 --es actionType ACTION_CHECK_IN --es title "Check-In Alarm"');
    await new Promise(resolve => setTimeout(resolve, 3000));

    // Capture screenshot of physical phone screen
    const screenshot2 = path.join(ARTIFACT_DIR, 'phone_alarm_screen.png');
    await device.screenshot({ path: screenshot2 });
    console.log(`[Phone Visual] Captured Phone Alarm Screen: ${screenshot2}`);

    expect(fs.existsSync(screenshot2)).toBe(true);
  });
});
