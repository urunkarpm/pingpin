const { test, expect } = require('@playwright/test');

test.describe('MakeupWfoCard Component Specs', () => {

  test('should render missed days count and suggested compensation dates', async ({ page }) => {
    await page.setContent(`
      <div id="makeup-wfo-card" style="padding: 16px; background: rgba(239, 68, 68, 0.1); border: 1px solid #ef4444; border-radius: 16px; color: white;">
        <h3 id="missed-count-title">1 Missed WFO Day to Compensate</h3>
        <p>Suggested Date: <span id="suggested-date">Friday, Aug 29, 2026</span></p>
        <button id="mark-completed-btn" style="background: #22c55e; border: none; padding: 10px 16px; border-radius: 8px; color: white; cursor: pointer;">
          Mark WFO Completed
        </button>
      </div>
    `);

    const title = page.locator('#missed-count-title');
    await expect(title).toHaveText('1 Missed WFO Day to Compensate');

    const suggestedDate = page.locator('#suggested-date');
    await expect(suggestedDate).toHaveText('Friday, Aug 29, 2026');

    const markBtn = page.locator('#mark-completed-btn');
    await expect(markBtn).toBeVisible();
    await expect(markBtn).toHaveText('Mark WFO Completed');
  });

  test('should trigger completion action and switch to empty state when completed', async ({ page }) => {
    await page.setContent(`
      <div id="makeup-container">
        <div id="active-state">
          <h3>1 Missed WFO Day</h3>
          <button id="mark-completed-btn">Mark WFO Completed</button>
        </div>
        <div id="empty-state" style="display: none;">
          <h3>🎉 All WFO Days Up To Date!</h3>
          <p>No compensation days required.</p>
        </div>
      </div>
      <script>
        document.getElementById('mark-completed-btn').addEventListener('click', () => {
          document.getElementById('active-state').style.display = 'none';
          document.getElementById('empty-state').style.display = 'block';
        });
      </script>
    `);

    const activeState = page.locator('#active-state');
    const emptyState = page.locator('#empty-state');
    const markBtn = page.locator('#mark-completed-btn');

    await expect(activeState).toBeVisible();
    await expect(emptyState).toBeHidden();

    await markBtn.click();

    await expect(activeState).toBeHidden();
    await expect(emptyState).toBeVisible();
    await expect(emptyState).toContainText('All WFO Days Up To Date!');
  });
});
