const { test, expect, _android } = require('@playwright/test');
const path = require('path');
const fs = require('fs');

const ARTIFACT_DIR = 'C:\\Users\\uprasenjeet\\.gemini\\antigravity-ide\\brain\\5dffc945-c513-4391-ab17-1a3001f7f002';

test.describe('Live Android Emulator Visual Component Suite', () => {
  let device;

  test.beforeEach(() => {
    test.skip(!process.env.TEST_LIVE_DEVICE, 'Live device visual tests require active ADB daemon and TEST_LIVE_DEVICE=1');
  });

  test.beforeAll(async () => {
    if (!process.env.TEST_LIVE_DEVICE) return;
    try {
      const devices = await _android.devices();
      if (devices.length > 0) {
        device = devices[0];
      }
    } catch (e) {
      console.warn('[Emulator Test] ADB server offline:', e.message);
    }
  });

  test.afterAll(async () => {
    if (device) {
      await device.close();
    }
  });

  test('should launch Home Screen on emulator and capture visual screenshot', async () => {
    if (!device) return;

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
    if (!device) return;

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
