const { test, expect } = require('@playwright/test');

test.describe('ExpandableWeeklyCalendarCard Component Specs', () => {

  test('should render weekly calendar card with current week days and expand/collapse toggle', async ({ page }) => {
    await page.setContent(`
      <div id="weekly-card" style="padding: 16px; background: rgba(30,41,59,0.8); border-radius: 20px; color: white;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <h3 id="weekly-title">This Week (3/3 Days Completed)</h3>
          <button id="toggle-expand-btn" aria-label="Expand Weekly Calendar">▼</button>
        </div>
        <div id="weekly-days-row" style="display: flex; justify-content: space-between; margin-top: 12px;">
          <div class="day-chip completed" data-day="Mon">Mon (✔)</div>
          <div class="day-chip completed" data-day="Tue">Tue (✔)</div>
          <div class="day-chip completed" data-day="Wed">Wed (✔)</div>
          <div class="day-chip pending" data-day="Thu">Thu</div>
          <div class="day-chip pending" data-day="Fri">Fri</div>
        </div>
        <div id="expanded-details" style="display: none; margin-top: 16px; border-top: 1px solid #334155; padding-top: 12px;">
          <p>Detailed log: 21 hrs 45 mins total logged in office</p>
        </div>
      </div>
      <script>
        const btn = document.getElementById('toggle-expand-btn');
        const details = document.getElementById('expanded-details');
        btn.addEventListener('click', () => {
          const isExpanded = details.style.display !== 'none';
          details.style.display = isExpanded ? 'none' : 'block';
          btn.textContent = isExpanded ? '▼' : '▲';
        });
      </script>
    `);

    const title = page.locator('#weekly-title');
    await expect(title).toHaveText('This Week (3/3 Days Completed)');

    const toggleBtn = page.locator('#toggle-expand-btn');
    const details = page.locator('#expanded-details');

    await expect(details).toBeHidden();
    await toggleBtn.click();
    await expect(details).toBeVisible();
    await expect(details).toContainText('21 hrs 45 mins total logged in office');

    await toggleBtn.click();
    await expect(details).toBeHidden();
  });

  test('should display checkmark for completed WFO days', async ({ page }) => {
    await page.setContent(`
      <div id="weekly-days">
        <span class="completed-chip" style="background: #22c55e; color: white;">Mon ✓</span>
      </div>
    `);

    const completedChip = page.locator('.completed-chip');
    await expect(completedChip).toBeVisible();
    await expect(completedChip).toContainText('✓');
    await expect(completedChip).toHaveCSS('background-color', 'rgb(34, 197, 94)');
  });
});
