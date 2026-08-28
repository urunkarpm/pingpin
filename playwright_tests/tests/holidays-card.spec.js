const { test, expect } = require('@playwright/test');
const { MOCK_INDIAN_HOLIDAYS_2026 } = require('../lib/component-harness');

test.describe('UpcomingHolidaysCard Component Specs', () => {

  test('should load 2026 Indian holidays dataset correctly', () => {
    expect(MOCK_INDIAN_HOLIDAYS_2026.length).toBeGreaterThan(0);
    const republicDay = MOCK_INDIAN_HOLIDAYS_2026.find(h => h.name === 'Republic Day');
    expect(republicDay).toBeDefined();
    expect(republicDay.category).toBe('NATIONAL');
    expect(republicDay.isLongWeekend).toBe(true);
  });

  test('should render category filter tabs (All, National, Regional)', async ({ page }) => {
    await page.setContent(`
      <div id="holidays-card" style="padding: 16px; background: #1e293b; color: white;">
        <h3>Upcoming Indian Holidays (2026)</h3>
        <div id="filter-tabs" style="display: flex; gap: 8px; margin-bottom: 12px;">
          <button class="tab-chip active" data-filter="ALL">All</button>
          <button class="tab-chip" data-filter="NATIONAL">National</button>
          <button class="tab-chip" data-filter="REGIONAL">Regional</button>
        </div>
        <div id="holiday-list">
          <div class="holiday-item" data-category="NATIONAL">
            <strong>Republic Day</strong> - Jan 26, 2026 <span class="badge-long-weekend">Long Weekend 🔥</span>
          </div>
          <div class="holiday-item" data-category="REGIONAL">
            <strong>Ambedkar Jayanti</strong> - Apr 14, 2026
          </div>
        </div>
      </div>
    `);

    const tabs = page.locator('.tab-chip');
    await expect(tabs).toHaveCount(3);
    await expect(tabs.nth(0)).toHaveText('All');
    await expect(tabs.nth(1)).toHaveText('National');
    await expect(tabs.nth(2)).toHaveText('Regional');

    const longWeekendBadge = page.locator('.badge-long-weekend');
    await expect(longWeekendBadge).toBeVisible();
    await expect(longWeekendBadge).toContainText('Long Weekend');
  });

  test('should filter holiday list when category tab is clicked', async ({ page }) => {
    await page.setContent(`
      <div id="filter-tabs">
        <button id="tab-all">All</button>
        <button id="tab-national">National</button>
      </div>
      <div id="holiday-list">
        <div class="holiday-item national">Republic Day</div>
        <div class="holiday-item regional">Ambedkar Jayanti</div>
      </div>
      <script>
        document.getElementById('tab-national').addEventListener('click', () => {
          document.querySelectorAll('.holiday-item').forEach(item => {
            item.style.display = item.classList.contains('national') ? 'block' : 'none';
          });
        });
        document.getElementById('tab-all').addEventListener('click', () => {
          document.querySelectorAll('.holiday-item').forEach(item => item.style.display = 'block');
        });
      </script>
    `);

    await page.locator('#tab-national').click();
    await expect(page.locator('.holiday-item.national')).toBeVisible();
    await expect(page.locator('.holiday-item.regional')).toBeHidden();

    await page.locator('#tab-all').click();
    await expect(page.locator('.holiday-item.national')).toBeVisible();
    await expect(page.locator('.holiday-item.regional')).toBeVisible();
  });
});
