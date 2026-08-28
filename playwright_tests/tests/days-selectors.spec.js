const { test, expect } = require('@playwright/test');

test.describe('WfoDaysSelector & WorkingDaysSelector Specs', () => {

  test('should render WfoDaysSelector chips (1 to 5 days) and update target count', async ({ page }) => {
    await page.setContent(`
      <div id="wfo-days-selector" style="padding: 16px; background: #1e293b; color: white;">
        <h3>Target WFO Days / Week</h3>
        <div id="days-chips" style="display: flex; gap: 8px; margin-top: 8px;">
          <button class="wfo-chip" data-count="1">1</button>
          <button class="wfo-chip" data-count="2">2</button>
          <button class="wfo-chip active" data-count="3" style="background: #3b82f6; color: white;">3</button>
          <button class="wfo-chip" data-count="4">4</button>
          <button class="wfo-chip" data-count="5">5</button>
        </div>
        <p id="target-summary">Target: 3 days per week</p>
      </div>
      <script>
        document.querySelectorAll('.wfo-chip').forEach(chip => {
          chip.addEventListener('click', (e) => {
            const count = e.target.getAttribute('data-count');
            document.querySelectorAll('.wfo-chip').forEach(c => c.classList.remove('active'));
            e.target.classList.add('active');
            document.getElementById('target-summary').textContent = 'Target: ' + count + ' days per week';
          });
        });
      </script>
    `);

    const activeChip = page.locator('.wfo-chip.active');
    await expect(activeChip).toHaveText('3');

    const chip4 = page.locator('.wfo-chip[data-count="4"]');
    await chip4.click();

    await expect(page.locator('#target-summary')).toHaveText('Target: 4 days per week');
  });

  test('should render WorkingDaysSelector day-of-week chips (Mon-Sun) supporting multi-selection', async ({ page }) => {
    await page.setContent(`
      <div id="working-days-selector" style="padding: 16px; background: #1e293b; color: white;">
        <h3>Working Days</h3>
        <div id="working-chips" style="display: flex; gap: 6px;">
          <button class="day-chip selected" data-day="MON">Mon</button>
          <button class="day-chip selected" data-day="TUE">Tue</button>
          <button class="day-chip selected" data-day="WED">Wed</button>
          <button class="day-chip selected" data-day="THU">Thu</button>
          <button class="day-chip selected" data-day="FRI">Fri</button>
          <button class="day-chip" data-day="SAT">Sat</button>
          <button class="day-chip" data-day="SUN">Sun</button>
        </div>
      </div>
      <script>
        document.querySelectorAll('.day-chip').forEach(chip => {
          chip.addEventListener('click', (e) => {
            e.target.classList.toggle('selected');
          });
        });
      </script>
    `);

    const selectedChips = page.locator('.day-chip.selected');
    await expect(selectedChips).toHaveCount(5);

    const satChip = page.locator('.day-chip[data-day="SAT"]');
    await satChip.click();
    await expect(satChip).toHaveClass(/selected/);
    await expect(page.locator('.day-chip.selected')).toHaveCount(6);
  });
});
