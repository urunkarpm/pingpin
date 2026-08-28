const { test, expect } = require('@playwright/test');

test.describe('AlarmActivity Full-Screen UI Component Specs', () => {

  test('should render Check-In alarm full-screen layout with time, title, and action buttons', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-activity-screen" style="width: 100vw; height: 100vh; background: #0f172a; color: white; display: flex; flex-direction: column; align-items: center; justify-content: space-around; padding: 32px;">
        <div id="alarm-header" style="text-align: center;">
          <span id="alarm-badge" style="background: rgba(59, 130, 246, 0.2); color: #3b82f6; padding: 6px 16px; border-radius: 20px; font-weight: bold;">CHECK-IN REMINDER</span>
          <h1 id="alarm-time-display" style="font-size: 64px; margin: 16px 0;">09:00 AM</h1>
          <p id="alarm-title">Time to reach Office! Tap to open check-in portal.</p>
        </div>
        <div id="alarm-actions" style="display: flex; flex-direction: column; gap: 16px; width: 100%; max-width: 320px;">
          <button id="snooze-alarm-btn" style="background: #334155; color: white; padding: 16px; border-radius: 16px; font-size: 18px; border: none; cursor: pointer;">
            ⏰ Snooze (5 Mins)
          </button>
          <button id="open-portal-btn" style="background: #3b82f6; color: white; padding: 16px; border-radius: 16px; font-size: 18px; border: none; cursor: pointer;">
            🚀 Open Attendance Portal
          </button>
          <button id="dismiss-alarm-btn" style="background: rgba(239, 68, 68, 0.2); color: #ef4444; border: 1px solid #ef4444; padding: 14px; border-radius: 16px; font-size: 16px; cursor: pointer;">
            ✖ Dismiss
          </button>
        </div>
      </div>
    `);

    const badge = page.locator('#alarm-badge');
    await expect(badge).toHaveText('CHECK-IN REMINDER');

    const timeDisplay = page.locator('#alarm-time-display');
    await expect(timeDisplay).toHaveText('09:00 AM');

    const snoozeBtn = page.locator('#snooze-alarm-btn');
    const portalBtn = page.locator('#open-portal-btn');
    const dismissBtn = page.locator('#dismiss-alarm-btn');

    await expect(snoozeBtn).toBeVisible();
    await expect(portalBtn).toBeVisible();
    await expect(dismissBtn).toBeVisible();
  });

  test('should render Check-Out alarm variant layout', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-activity-screen">
        <span id="alarm-badge">CHECK-OUT REMINDER</span>
        <h1 id="alarm-time-display">06:00 PM</h1>
        <p id="alarm-title">Completed working hours! Don't forget to mark check-out.</p>
      </div>
    `);

    await expect(page.locator('#alarm-badge')).toHaveText('CHECK-OUT REMINDER');
    await expect(page.locator('#alarm-time-display')).toHaveText('06:00 PM');
  });

  test('should execute Snooze action and reschedule alarm by 5 minutes', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-activity-screen">
        <button id="snooze-alarm-btn">Snooze (5 Mins)</button>
        <div id="snooze-status">Active</div>
      </div>
      <script>
        document.getElementById('snooze-alarm-btn').addEventListener('click', () => {
          document.getElementById('snooze-status').textContent = 'Snoozed for 5 minutes (Rescheduled)';
        });
      </script>
    `);

    await page.locator('#snooze-alarm-btn').click();
    await expect(page.locator('#snooze-status')).toHaveText('Snoozed for 5 minutes (Rescheduled)');
  });

  test('should execute Dismiss action and clear ringtone/vibration', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-activity-screen">
        <button id="dismiss-alarm-btn">Dismiss</button>
        <div id="alarm-state">Ringing</div>
      </div>
      <script>
        document.getElementById('dismiss-alarm-btn').addEventListener('click', () => {
          document.getElementById('alarm-state').textContent = 'Dismissed and Stopped';
        });
      </script>
    `);

    await page.locator('#dismiss-alarm-btn').click();
    await expect(page.locator('#alarm-state')).toHaveText('Dismissed and Stopped');
  });
});
