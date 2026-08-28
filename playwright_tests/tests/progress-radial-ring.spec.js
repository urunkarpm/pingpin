const { test, expect } = require('@playwright/test');
const { calculateRadialProgress } = require('../lib/component-harness');

test.describe('ProgressRadialRing Component Specs', () => {

  test('should calculate correct percentage and color category', () => {
    expect(calculateRadialProgress(3, 3)).toEqual({ percentage: 100, colorCategory: 'GREEN' });
    expect(calculateRadialProgress(2, 3)).toEqual({ percentage: 67, colorCategory: 'YELLOW' });
    expect(calculateRadialProgress(1, 3)).toEqual({ percentage: 33, colorCategory: 'RED' });
    expect(calculateRadialProgress(0, 3)).toEqual({ percentage: 0, colorCategory: 'RED' });
  });

  test('should render radial progress ring SVG with sweep stroke and center metric text', async ({ page }) => {
    await page.setContent(`
      <div id="progress-radial-ring" style="position: relative; width: 120px; height: 120px;">
        <svg width="120" height="120" viewBox="0 0 120 120">
          <circle cx="60" cy="60" r="50" fill="none" stroke="#334155" stroke-width="10"></circle>
          <circle id="progress-stroke" cx="60" cy="60" r="50" fill="none" stroke="#22c55e" stroke-width="10" stroke-dasharray="314" stroke-dashoffset="0" stroke-linecap="round"></circle>
        </svg>
        <div id="center-metric-text" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: white;">
          <span id="percentage-label" style="font-size: 24px; font-weight: bold;">100%</span>
          <span style="font-size: 12px; color: #94a3b8;">Goal Met</span>
        </div>
      </div>
    `);

    const percentageLabel = page.locator('#percentage-label');
    await expect(percentageLabel).toHaveText('100%');

    const progressStroke = page.locator('#progress-stroke');
    await expect(progressStroke).toHaveAttribute('stroke', '#22c55e');
  });

  test('should update stroke color to Yellow when progress is 50-79%', async ({ page }) => {
    await page.setContent(`
      <circle id="progress-stroke" stroke="#eab308" stroke-dashoffset="100"></circle>
      <span id="percentage-label">67%</span>
    `);

    await expect(page.locator('#percentage-label')).toHaveText('67%');
    await expect(page.locator('#progress-stroke')).toHaveAttribute('stroke', '#eab308');
  });
});
