// diagnostic.js — dumps the page source of the PingPin app so we can see real element labels
// Run with: node diagnostic.js

const { remote } = require('webdriverio');
const fs = require('fs');

async function main() {
  console.log('Connecting to Appium...');
  const driver = await remote({
    hostname: '127.0.0.1',
    port: 4723,
    path: '/',
    capabilities: {
      platformName: 'Android',
      'appium:automationName': 'UiAutomator2',
      'appium:appPackage': 'com.urunkarpm.pingpin',
      'appium:appActivity': 'com.urunkarpm.pingpin.MainActivity',
      'appium:noReset': true,
      'appium:fullReset': false,
      'appium:autoGrantPermissions': true,
      'appium:newCommandTimeout': 60,
    },
  });

  try {
    console.log('Session started. Waiting 3s for app to load...');
    await driver.pause(3000);

    // Dump the page source
    console.log('Getting page source...');
    const source = await driver.getPageSource();
    fs.writeFileSync('page_source_home.xml', source);
    console.log('Saved: page_source_home.xml');

    // Print all text / content-desc attributes
    const elements = await driver.$$('//*[@text!="" or @content-desc!=""]');
    console.log(`\nFound ${elements.length} elements with text/content-desc:\n`);
    for (const el of elements) {
      try {
        const text = await el.getAttribute('text');
        const desc = await el.getAttribute('content-desc');
        const cls  = await el.getAttribute('class');
        if (text || desc) {
          console.log(`  class=${cls}  text="${text}"  desc="${desc}"`);
        }
      } catch { /* skip stale */ }
    }

  } finally {
    await driver.deleteSession();
    console.log('\nSession closed.');
  }
}

main().catch(console.error);
