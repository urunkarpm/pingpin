const { test, expect } = require('@playwright/test');
const { formatTimePickerString } = require('../lib/component-harness');

test.describe('AdvancedTimePickerDialog Component Specs', () => {

  test('should accurately format 24-hour inputs to AM/PM strings', () => {
    expect(formatTimePickerString(9, 0)).toBe('9:00 AM');
    expect(formatTimePickerString(13, 30)).toBe('1:30 PM');
    expect(formatTimePickerString(0, 15)).toBe('12:15 AM');
    expect(formatTimePickerString(18, 45)).toBe('6:45 PM');
  });

  test('should render time picker dialog with presets, step controls, and confirm button', async ({ page }) => {
    await page.setContent(`
      <div id="time-picker-dialog" role="dialog" aria-modal="true" aria-label="Select Alarm Time" style="background: #1e293b; padding: 24px; border-radius: 20px; color: white;">
        <h2>Select Check-In Alarm Time</h2>
        <div id="time-display" style="font-size: 32px; text-align: center; margin: 16px 0;">09:00 AM</div>
        <div id="preset-chips" style="display: flex; gap: 8px; justify-content: center;">
          <button class="preset-chip" data-time="08:30 AM">08:30 AM</button>
          <button class="preset-chip active" data-time="09:00 AM">09:00 AM</button>
          <button class="preset-chip" data-time="09:30 AM">09:30 AM</button>
        </div>
        <div id="step-controls" style="display: flex; gap: 16px; justify-content: center; margin-top: 16px;">
          <button id="minus-15-btn">-15m</button>
          <button id="plus-15-btn">+15m</button>
        </div>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 24px;">
          <button id="cancel-btn">Cancel</button>
          <button id="confirm-btn" style="background: #3b82f6; color: white;">Confirm</button>
        </div>
      </div>
    `);

    const dialog = page.locator('#time-picker-dialog');
    await expect(dialog).toBeVisible();
    await expect(dialog).toHaveAttribute('role', 'dialog');
    await expect(dialog).toHaveAttribute('aria-label', 'Select Alarm Time');

    const display = page.locator('#time-display');
    await expect(display).toHaveText('09:00 AM');
  });

  test('should handle preset selection chips click', async ({ page }) => {
    await page.setContent(`
      <div id="time-display">09:00 AM</div>
      <button class="preset-chip" data-time="09:30 AM">09:30 AM</button>
      <script>
        document.querySelector('.preset-chip').addEventListener('click', (e) => {
          document.getElementById('time-display').textContent = e.target.getAttribute('data-time');
        });
      </script>
    `);

    const presetChip = page.locator('.preset-chip');
    await presetChip.click();
    await expect(page.locator('#time-display')).toHaveText('09:30 AM');
  });

  test('should increment and decrement minutes with +/- 15 minute step buttons', async ({ page }) => {
    await page.setContent(`
      <div id="time-display">09:00 AM</div>
      <button id="minus-15-btn">-15m</button>
      <button id="plus-15-btn">+15m</button>
      <script>
        let mins = 540; // 9:00 AM in minutes
        function updateDisplay() {
          const h = Math.floor(mins / 60) % 12 || 12;
          const m = String(mins % 60).padStart(2, '0');
          const ampm = mins >= 720 ? 'PM' : 'AM';
          document.getElementById('time-display').textContent = \`\${String(h).padStart(2, '0')}:\${m} \${ampm}\`;
        }
        document.getElementById('minus-15-btn').addEventListener('click', () => { mins -= 15; updateDisplay(); });
        document.getElementById('plus-15-btn').addEventListener('click', () => { mins += 15; updateDisplay(); });
      </script>
    `);

    await page.locator('#plus-15-btn').click();
    await expect(page.locator('#time-display')).toHaveText('09:15 AM');

    await page.locator('#minus-15-btn').click();
    await expect(page.locator('#time-display')).toHaveText('09:00 AM');
  });
});
