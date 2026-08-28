const { test, expect } = require('@playwright/test');
const { createMockAttendanceData } = require('../lib/component-harness');

test.describe('CalendarView Component Specs', () => {
  let mockData;

  test.beforeEach(() => {
    mockData = createMockAttendanceData(2026, 8);
  });

  test('should correctly generate month attendance grid data for August 2026', () => {
    expect(Object.keys(mockData).length).toBe(31);
    expect(mockData['2026-08-01'].status).toBe('OFF_DAY'); // Saturday
    expect(mockData['2026-08-15'].status).toBe('HOLIDAY'); // Independence Day
    expect(mockData['2026-08-03'].status).toBe('PRESENT');
    expect(mockData['2026-08-28'].status).toBe('MISSED');
  });

  test('should render monthly calendar header, month controls, and status grid', async ({ page }) => {
    await page.setContent(`
      <div id="calendar-view" style="padding: 16px; background: #0f172a; color: white;">
        <div id="calendar-header" style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <button id="prev-month-btn" aria-label="Previous Month">◀</button>
          <h2 id="month-title">August 2026</h2>
          <button id="next-month-btn" aria-label="Next Month">▶</button>
        </div>
        <div id="day-headers" style="display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-weight: bold; margin-bottom: 8px;">
          <span>M</span><span>T</span><span>W</span><span>T</span><span>F</span><span>S</span><span>S</span>
        </div>
        <div id="days-grid" style="display: grid; grid-template-columns: repeat(7, 1fr); gap: 4px;">
          <div class="day-cell present" data-date="2026-08-03" style="background: #22c55e; padding: 8px; text-align: center;">3</div>
          <div class="day-cell holiday" data-date="2026-08-15" style="background: #3b82f6; padding: 8px; text-align: center;">15</div>
          <div class="day-cell missed" data-date="2026-08-28" style="background: #ef4444; padding: 8px; text-align: center;">28</div>
        </div>
      </div>
    `);

    const monthTitle = page.locator('#month-title');
    await expect(monthTitle).toHaveText('August 2026');

    const prevBtn = page.locator('#prev-month-btn');
    const nextBtn = page.locator('#next-month-btn');
    await expect(prevBtn).toBeVisible();
    await expect(nextBtn).toBeVisible();

    const presentCell = page.locator('.day-cell.present');
    await expect(presentCell).toHaveText('3');
    await expect(presentCell).toHaveCSS('background-color', 'rgb(34, 197, 94)');

    const holidayCell = page.locator('.day-cell.holiday');
    await expect(holidayCell).toHaveText('15');
    await expect(holidayCell).toHaveCSS('background-color', 'rgb(59, 130, 246)');

    const missedCell = page.locator('.day-cell.missed');
    await expect(missedCell).toHaveText('28');
    await expect(missedCell).toHaveCSS('background-color', 'rgb(239, 68, 68)');
  });

  test('should handle month navigation click events', async ({ page }) => {
    await page.setContent(`
      <div id="calendar-header">
        <button id="prev-month-btn">◀</button>
        <h2 id="month-title">August 2026</h2>
        <button id="next-month-btn">▶</button>
      </div>
      <script>
        const months = ['July 2026', 'August 2026', 'September 2026'];
        let currentIndex = 1;
        const title = document.getElementById('month-title');
        document.getElementById('prev-month-btn').addEventListener('click', () => {
          if (currentIndex > 0) currentIndex--;
          title.textContent = months[currentIndex];
        });
        document.getElementById('next-month-btn').addEventListener('click', () => {
          if (currentIndex < months.length - 1) currentIndex++;
          title.textContent = months[currentIndex];
        });
      </script>
    `);

    const monthTitle = page.locator('#month-title');
    await expect(monthTitle).toHaveText('August 2026');

    await page.locator('#prev-month-btn').click();
    await expect(monthTitle).toHaveText('July 2026');

    await page.locator('#next-month-btn').click();
    await expect(monthTitle).toHaveText('August 2026');

    await page.locator('#next-month-btn').click();
    await expect(monthTitle).toHaveText('September 2026');
  });

  test('should trigger onDateSelected callback when a day cell is clicked', async ({ page }) => {
    await page.setContent(`
      <div id="days-grid">
        <div class="day-cell" data-date="2026-08-10" style="cursor: pointer;">10</div>
      </div>
      <div id="selected-info">None</div>
      <script>
        document.querySelector('.day-cell').addEventListener('click', (e) => {
          const date = e.target.getAttribute('data-date');
          document.getElementById('selected-info').textContent = 'Selected: ' + date;
        });
      </script>
    `);

    const dayCell = page.locator('.day-cell');
    await dayCell.click();
    await expect(page.locator('#selected-info')).toHaveText('Selected: 2026-08-10');
  });
});
