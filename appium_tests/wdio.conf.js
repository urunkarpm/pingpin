// ─────────────────────────────────────────────────────────────────────────────
// wdio.conf.js  –  WebdriverIO + Appium configuration for PingPin alarm tests
// ─────────────────────────────────────────────────────────────────────────────
// Prerequisites (run once):
//   npm install
//   npx appium driver install uiautomator2
//
// Then run:
//   npm test                           (all suites)
//   npm run test:alarm                 (quick 5-second alarm trigger)
//   npm run test:checkin               (check-in alarm flow)
//   npm run test:checkout              (check-out alarm flow)
//   npm run test:settings              (change alarm times in Settings)
// ─────────────────────────────────────────────────────────────────────────────

exports.config = {
  runner: 'local',

  // ── Appium server (started automatically by the @wdio/appium-service) ──────
  services: [
    [
      'appium',
      {
        args: {
          relaxedSecurity: true,
          address: '127.0.0.1',
          port: 4723,
        },
        logFileName: 'appium.log',
      },
    ],
  ],
  port: 4723,
  hostname: '127.0.0.1',
  path: '/',

  // ── Android device capabilities ─────────────────────────────────────────────
  capabilities: [
    {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',

      // ── App identification ──────────────────────────────────────────────────
      // The app is pre-installed on the device; use appPackage + appActivity
      // so Appium launches it without reinstalling.
      'appium:appPackage': 'com.urunkarpm.pingpin',
      'appium:appActivity': 'com.urunkarpm.pingpin.MainActivity',
      'appium:noReset': true,     // keep app data between runs
      'appium:fullReset': false,

      // ── Device targeting ────────────────────────────────────────────────────
      // Leave 'appium:udid' blank to auto-select the first USB-connected device.
      // If you have multiple devices, set it explicitly, e.g.:
      //   'appium:udid': 'emulator-5554',
      // 'appium:udid': '',

      // ── Timeouts ────────────────────────────────────────────────────────────
      'appium:newCommandTimeout': 120,
      'appium:androidInstallTimeout': 90000,

      // ── Allow granting permissions on launch ────────────────────────────────
      'appium:autoGrantPermissions': true,
    },
  ],

  // ── Test files ──────────────────────────────────────────────────────────────
  specs: ['./tests/**/*.test.js'],
  exclude: [],

  // ── Framework ───────────────────────────────────────────────────────────────
  framework: 'mocha',
  mochaOpts: {
    ui: 'bdd',
    timeout: 120000,   // 2 min – alarm tests need up to 30 s to fire
  },

  // ── Reporters ───────────────────────────────────────────────────────────────
  reporters: ['spec'],

  // ── Concurrency: 1 for device tests (sequential) ────────────────────────────
  maxInstances: 1,

  // ── Global before/after hooks ────────────────────────────────────────────────
  before: async () => {
    // Give the app a moment to stabilise after launch
    await driver.pause(2000);
  },
};
