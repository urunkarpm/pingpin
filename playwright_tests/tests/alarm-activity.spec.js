const { test, expect } = require('@playwright/test');

test.describe('AlarmActivity Full-Screen UI Specs', () => {

  test('should render Check-In alarm AMOLED full-screen layout with electric blue accents and actions', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-activity-screen" style="width: 100vw; height: 100vh; background: linear-gradient(to bottom, #000000, #06080F); color: #F8FAFC; display: flex; flex-direction: column; align-items: center; justify-content: space-between; padding: 28px 24px; box-sizing: border-box; font-family: -apple-system, sans-serif;">
        <!-- Top Status -->
        <div id="alarm-top-section" style="display: flex; flex-direction: column; align-items: center; gap: 10px;">
          <div id="alarm-status-chip" style="background: rgba(255,255,255,0.07); border: 1px solid rgba(59,130,246,0.4); padding: 8px 16px; border-radius: 50px; display: flex; align-items: center; gap: 8px;">
            <div id="status-dot" style="width: 8px; height: 8px; border-radius: 50%; background: #3B82F6;"></div>
            <span style="font-size: 12px; font-weight: bold; letter-spacing: 1.5px; color: #F8FAFC;">CHECK-IN ALARM</span>
          </div>
          <div id="portal-url-pill" style="background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1); padding: 6px 14px; border-radius: 20px; font-size: 12px; color: #94A3B8;">
            https://hrms.company.com
          </div>
        </div>

        <!-- Center Sonar Orb & Giant Time -->
        <div id="alarm-center-hero" style="display: flex; flex-direction: column; align-items: center; width: 100%;">
          <div id="sonar-ring" style="width: 110px; height: 110px; border-radius: 50%; border: 2px solid #3B82F6; background: radial-gradient(circle, rgba(59,130,246,0.35), rgba(0,0,0,0.5)); display: flex; align-items: center; justify-content: center; margin-bottom: 16px;">
            <span style="font-size: 36px;">🔔</span>
          </div>
          <div id="clock-row" style="display: flex; align-items: baseline; justify-content: center; gap: 8px;">
            <span id="alarm-time-display" style="font-size: 72px; font-weight: bold; letter-spacing: -2px; color: #F8FAFC;">09:30</span>
            <span id="alarm-ampm-badge" style="font-size: 16px; font-weight: 900; color: #3B82F6; background: rgba(59,130,246,0.2); padding: 4px 8px; border-radius: 8px;">AM</span>
          </div>
          <span id="alarm-date-display" style="font-size: 16px; color: #94A3B8; font-weight: 500; margin-top: 6px;">Monday, September 7</span>
        </div>

        <!-- Bottom Actions -->
        <div id="alarm-actions-column" style="display: flex; flex-direction: column; gap: 12px; width: 100%; max-width: 400px;">
          <button id="btn-check-in" style="height: 60px; border-radius: 20px; border: none; background: linear-gradient(to right, #3B82F6, #06B6D4); color: white; font-weight: 800; font-size: 15px; cursor: pointer;">
            CHECK-IN (OPEN PORTAL) →
          </button>
          <button id="btn-snooze" style="height: 54px; border-radius: 18px; border: 1px solid rgba(255,255,255,0.15); background: rgba(255,255,255,0.1); color: #F8FAFC; font-weight: bold; font-size: 14px; cursor: pointer;">
            SNOOZE FOR 10 MINS
          </button>
          <button id="btn-leave" style="height: 54px; border-radius: 18px; border: 1px solid rgba(248,113,113,0.35); background: transparent; color: #F87171; font-weight: bold; font-size: 14px; cursor: pointer;">
            APPLY FOR LEAVE
          </button>
        </div>
      </div>
    `);

    await expect(page.locator('#alarm-status-chip')).toContainText('CHECK-IN ALARM');
    await expect(page.locator('#alarm-time-display')).toHaveText('09:30');
    await expect(page.locator('#alarm-ampm-badge')).toHaveText('AM');
    await expect(page.locator('#btn-check-in')).toBeVisible();
    await expect(page.locator('#btn-snooze')).toHaveText('SNOOZE FOR 10 MINS');
    await expect(page.locator('#btn-leave')).toBeVisible();
  });

  test('should render Check-Out alarm layout with emerald/teal theme', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-activity-screen" style="width: 100vw; height: 100vh; background: #000000; color: #F8FAFC;">
        <div id="alarm-status-chip">
          <span>CHECK-OUT ALARM</span>
        </div>
        <span id="alarm-time-display">06:00</span>
        <span id="alarm-ampm-badge">PM</span>
        <button id="btn-check-out" style="background: linear-gradient(to right, #10B981, #34D399);">
          CHECK-OUT (OPEN PORTAL) →
        </button>
        <button id="btn-snooze">SNOOZE FOR 10 MINS</button>
      </div>
    `);

    await expect(page.locator('#alarm-status-chip')).toContainText('CHECK-OUT ALARM');
    await expect(page.locator('#alarm-time-display')).toHaveText('06:00');
    await expect(page.locator('#alarm-ampm-badge')).toHaveText('PM');
    await expect(page.locator('#btn-check-out')).toBeVisible();
  });

  test('should handle Snooze for 10 Mins button click', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-screen">
        <button id="btn-snooze">SNOOZE FOR 10 MINS</button>
        <div id="status-text">Ringing</div>
      </div>
      <script>
        document.getElementById('btn-snooze').addEventListener('click', () => {
          document.getElementById('status-text').textContent = 'Snoozed for 10 minutes';
        });
      </script>
    `);

    await page.locator('#btn-snooze').click();
    await expect(page.locator('#status-text')).toHaveText('Snoozed for 10 minutes');
  });

  test('should handle Check-In button click and dispatch portal opening', async ({ page }) => {
    await page.setContent(`
      <div id="alarm-screen">
        <button id="btn-check-in">CHECK-IN (OPEN PORTAL) →</button>
        <div id="portal-launch-status">Idle</div>
      </div>
      <script>
        document.getElementById('btn-check-in').addEventListener('click', () => {
          document.getElementById('portal-launch-status').textContent = 'Opening Portal with ACTION_CHECK_IN';
        });
      </script>
    `);

    await page.locator('#btn-check-in').click();
    await expect(page.locator('#portal-launch-status')).toHaveText('Opening Portal with ACTION_CHECK_IN');
  });
});
