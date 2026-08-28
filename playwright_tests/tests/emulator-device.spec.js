const { test, expect, _android } = require('@playwright/test');
const path = require('path');
const fs = require('fs');

const ARTIFACT_DIR = 'C:\\Users\\uprasenjeet\\.gemini\\antigravity-ide\\brain\\cad74e09-8ae1-4f4f-b23a-f059dd06dc50';

test.describe('Live Android Emulator Visual Component Suite', () => {
  let device;

  test.beforeAll(async () => {
    const devices = await _android.devices();
    if (devices.length === 0) {
      console.warn('[Emulator Test] No ADB device connected.');
      return;
    }
    device = devices[0];
  });

  test.afterAll(async () => {
    if (device) {
      await device.close();
    }
  });

  test('should launch Home Screen on emulator and capture visual screenshot', async () => {
    if (!device) {
      test.skip('Skipping: No active Android emulator connected.');
      return;
    }

    // Launch PingPin MainActivity
    await device.shell('am start -n com.urunkarpm.pingpin/.MainActivity');
    await new Promise(resolve => setTimeout(resolve, 4000));

    // Capture screenshot to artifact directory
    const screenshot1 = path.join(ARTIFACT_DIR, 'emulator_home_screen.png');
    await device.screenshot({ path: screenshot1 });
    console.log(`[Emulator Visual] Captured Home Screen: ${screenshot1}`);

    expect(fs.existsSync(screenshot1)).toBe(true);
  });

  test('should launch AlarmActivity full-screen dialog on emulator and capture visual screenshot', async () => {
    if (!device) {
      test.skip('Skipping: No active Android emulator connected.');
      return;
    }

    // Launch AlarmActivity with Check-In alarm payload
    await device.shell('am start -n com.urunkarpm.pingpin/.AlarmActivity --ei alarmId 101 --es actionType ACTION_CHECK_IN --es title "Check-In Alarm"');
    await new Promise(resolve => setTimeout(resolve, 3000));

    // Capture screenshot to artifact directory
    const screenshot2 = path.join(ARTIFACT_DIR, 'emulator_alarm_screen.png');
    await device.screenshot({ path: screenshot2 });
    console.log(`[Emulator Visual] Captured Alarm Screen: ${screenshot2}`);

    expect(fs.existsSync(screenshot2)).toBe(true);
  });
});
