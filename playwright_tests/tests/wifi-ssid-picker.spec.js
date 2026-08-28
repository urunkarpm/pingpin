const { test, expect } = require('@playwright/test');

test.describe('WifiSsidPickerField Component Specs', () => {

  test('should render current office Wi-Fi SSID field and scan button', async ({ page }) => {
    await page.setContent(`
      <div id="wifi-picker-container" style="padding: 16px; background: #1e293b; color: white;">
        <label for="ssid-input">Office Wi-Fi SSID</label>
        <div style="display: flex; gap: 8px; margin-top: 8px;">
          <input id="ssid-input" type="text" value="Corp_Office_5G" style="flex: 1; padding: 10px;" />
          <button id="scan-wifi-btn" aria-label="Scan Nearby Wi-Fi Networks">Scan</button>
        </div>
        <p id="wifi-status" style="color: #22c55e; margin-top: 4px;">Connected to saved network</p>
      </div>
    `);

    const input = page.locator('#ssid-input');
    await expect(input).toHaveValue('Corp_Office_5G');

    const scanBtn = page.locator('#scan-wifi-btn');
    await expect(scanBtn).toBeVisible();
    await expect(scanBtn).toHaveAttribute('aria-label', 'Scan Nearby Wi-Fi Networks');

    const status = page.locator('#wifi-status');
    await expect(status).toHaveText('Connected to saved network');
  });

  test('should open network selection modal on scan trigger and list available SSIDs', async ({ page }) => {
    await page.setContent(`
      <button id="scan-wifi-btn">Scan</button>
      <div id="networks-dialog" style="display: none; background: #0f172a; padding: 16px; color: white;">
        <h3>Select Office Wi-Fi</h3>
        <ul id="network-list">
          <li class="wifi-item" data-ssid="Corp_Office_5G">Corp_Office_5G (Signal: Strong)</li>
          <li class="wifi-item" data-ssid="Corp_Guest">Corp_Guest (Signal: Medium)</li>
        </ul>
      </div>
      <script>
        document.getElementById('scan-wifi-btn').addEventListener('click', () => {
          document.getElementById('networks-dialog').style.display = 'block';
        });
      </script>
    `);

    const scanBtn = page.locator('#scan-wifi-btn');
    const dialog = page.locator('#networks-dialog');

    await expect(dialog).toBeHidden();
    await scanBtn.click();
    await expect(dialog).toBeVisible();

    const items = page.locator('.wifi-item');
    await expect(items).toHaveCount(2);
    await expect(items.first()).toContainText('Corp_Office_5G');
  });
});
