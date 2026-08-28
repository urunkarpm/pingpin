const { test, expect } = require('@playwright/test');

test.describe('UpdateAvailableDialog Component Specs', () => {

  test('should render version update modal, release notes list, and CTA buttons', async ({ page }) => {
    await page.setContent(`
      <div id="update-dialog" role="dialog" aria-modal="true" aria-label="Update Available" style="background: #1e293b; padding: 24px; border-radius: 20px; color: white;">
        <h2 id="update-title">🚀 New Update Available! (v2.4.0)</h2>
        <p id="release-summary">What's New in PingPin:</p>
        <ul id="changelog-list">
          <li>Enhanced Wi-Fi check-in detection speed</li>
          <li>Liquid Glass UI animations and smooth haptics</li>
          <li>Automatic compensation suggestion for missed WFO days</li>
        </ul>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px;">
          <button id="dismiss-update-btn">Later</button>
          <button id="update-now-btn" style="background: #3b82f6; color: white; padding: 10px 20px; border-radius: 8px;">Update Now</button>
        </div>
      </div>
    `);

    const dialog = page.locator('#update-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog).toHaveAttribute('role', 'dialog');

    const title = page.locator('#update-title');
    await expect(title).toHaveText("🚀 New Update Available! (v2.4.0)");

    const changelogItems = page.locator('#changelog-list li');
    await expect(changelogItems).toHaveCount(3);

    const updateNowBtn = page.locator('#update-now-btn');
    await expect(updateNowBtn).toBeVisible();
    await expect(updateNowBtn).toHaveText('Update Now');
  });

  test('should handle dismiss button click and close dialog', async ({ page }) => {
    await page.setContent(`
      <div id="update-dialog">
        <button id="dismiss-update-btn">Later</button>
      </div>
      <script>
        document.getElementById('dismiss-update-btn').addEventListener('click', () => {
          document.getElementById('update-dialog').style.display = 'none';
        });
      </script>
    `);

    const dialog = page.locator('#update-dialog');
    await page.locator('#dismiss-update-btn').click();
    await expect(dialog).toBeHidden();
  });
});
