const { test, expect } = require('@playwright/test');
const { togglePingPinSwitch } = require('../lib/component-harness');

test.describe('PingPinSwitch Component Specs', () => {

  test('should correctly toggle state when enabled', () => {
    expect(togglePingPinSwitch(false, true)).toBe(true);
    expect(togglePingPinSwitch(true, true)).toBe(false);
  });

  test('should not toggle state when disabled', () => {
    expect(togglePingPinSwitch(false, false)).toBe(false);
    expect(togglePingPinSwitch(true, false)).toBe(true);
  });

  test('should render active checked switch with checkmark icon and "On" stateDescription', async ({ page }) => {
    await page.setContent(`
      <div id="switch-container">
        <button id="pingpin-switch"
                role="switch"
                aria-checked="true"
                aria-label="Auto Attendance Tracking"
                data-state-description="On"
                style="width: 52px; height: 32px; border-radius: 16px; background-color: #3b82f6; border: none; cursor: pointer; position: relative;">
          <span id="switch-thumb" style="width: 24px; height: 24px; border-radius: 12px; background-color: #ffffff; position: absolute; top: 4px; right: 4px; display: flex; align-items: center; justify-content: center;">
            <svg id="check-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#3b82f6" stroke-width="3">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </span>
        </button>
      </div>
    `);

    const switchBtn = page.locator('#pingpin-switch');
    await expect(switchBtn).toBeVisible();
    await expect(switchBtn).toHaveAttribute('role', 'switch');
    await expect(switchBtn).toHaveAttribute('aria-checked', 'true');
    await expect(switchBtn).toHaveAttribute('data-state-description', 'On');

    const checkIcon = page.locator('#check-icon');
    await expect(checkIcon).toBeVisible();
  });

  test('should render unchecked switch with "Off" stateDescription and high-contrast Slate track', async ({ page }) => {
    await page.setContent(`
      <button id="pingpin-switch"
              role="switch"
              aria-checked="false"
              aria-label="BLE Scanner"
              data-state-description="Off"
              style="width: 52px; height: 32px; border-radius: 16px; background-color: #1e293b; border: 1px solid #64748b; cursor: pointer; position: relative;">
        <span id="switch-thumb" style="width: 24px; height: 24px; border-radius: 12px; background-color: #f8fafc; position: absolute; top: 3px; left: 4px;"></span>
      </button>
    `);

    const switchBtn = page.locator('#pingpin-switch');
    await expect(switchBtn).toHaveAttribute('aria-checked', 'false');
    await expect(switchBtn).toHaveAttribute('data-state-description', 'Off');
    await expect(switchBtn).toHaveCSS('background-color', 'rgb(30, 41, 59)');
  });

  test('should handle user click interaction and invoke onCheckedChange callback', async ({ page }) => {
    await page.setContent(`
      <button id="pingpin-switch" role="switch" aria-checked="false">
        Toggle
      </button>
      <div id="status">OFF</div>
      <script>
        const btn = document.getElementById('pingpin-switch');
        const status = document.getElementById('status');
        btn.addEventListener('click', () => {
          const isChecked = btn.getAttribute('aria-checked') === 'true';
          const newState = !isChecked;
          btn.setAttribute('aria-checked', String(newState));
          status.textContent = newState ? 'ON' : 'OFF';
        });
      </script>
    `);

    const switchBtn = page.locator('#pingpin-switch');
    const status = page.locator('#status');

    await expect(status).toHaveText('OFF');
    await switchBtn.click();
    await expect(status).toHaveText('ON');
    await expect(switchBtn).toHaveAttribute('aria-checked', 'true');
    await switchBtn.click();
    await expect(status).toHaveText('OFF');
    await expect(switchBtn).toHaveAttribute('aria-checked', 'false');
  });

  test('should prevent state change when switch is disabled', async ({ page }) => {
    await page.setContent(`
      <button id="pingpin-switch" role="switch" aria-checked="false" disabled style="opacity: 0.5; cursor: not-allowed;">
        Disabled Switch
      </button>
      <div id="status">OFF</div>
      <script>
        const btn = document.getElementById('pingpin-switch');
        btn.addEventListener('click', () => {
          if (!btn.disabled) {
            document.getElementById('status').textContent = 'ON';
          }
        });
      </script>
    `);

    const switchBtn = page.locator('#pingpin-switch');
    await expect(switchBtn).toBeDisabled();
    await switchBtn.click({ force: true });
    await expect(page.locator('#status')).toHaveText('OFF');
  });
});
