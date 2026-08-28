const { _android } = require('playwright');

/**
 * Android Device Helper for Playwright
 * Handles device discovery, ADB connection, app lifecycle, and element interactions.
 */
class AndroidDriver {
  constructor(packageName = 'com.urunkarpm.pingpin') {
    this.packageName = packageName;
    this.device = null;
    this.app = null;
  }

  /**
   * Connect to connected Android device/emulator
   */
  async connectDevice() {
    try {
      const devices = await _android.devices();
      if (devices.length === 0) {
        throw new Error('No ADB devices found. Make sure an emulator or device is connected.');
      }
      this.device = devices[0];
      return this.device;
    } catch (err) {
      console.warn(`[AndroidDriver] Device connection notice: ${err.message}`);
      return null;
    }
  }

  /**
   * Launch target app package
   */
  async launchApp() {
    if (!this.device) {
      await this.connectDevice();
    }
    if (this.device) {
      this.app = await this.device.launch(this.packageName);
      return this.app;
    }
    return null;
  }

  /**
   * Close app session
   */
  async closeApp() {
    if (this.app) {
      await this.app.close();
      this.app = null;
    }
  }

  /**
   * Tap element by text or content description
   * @param {string} text 
   */
  async tapByText(text) {
    if (this.device) {
      await this.device.tap({ text });
    }
  }

  /**
   * Take screenshot of current device state
   * @param {string} path 
   */
  async screenshot(path) {
    if (this.device) {
      await this.device.screenshot({ path });
    }
  }
}

module.exports = { AndroidDriver };
