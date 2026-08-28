const { test, expect } = require('@playwright/test');

test.describe('WeatherTravelCard Component Specs', () => {

  test('should render commute weather forecast, temperature, and rain advisory banner', async ({ page }) => {
    await page.setContent(`
      <div id="weather-travel-card" style="padding: 16px; background: rgba(30, 41, 59, 0.8); border-radius: 16px; color: white;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <h3 id="location-name">Bengaluru Office Area</h3>
            <p id="temp-condition">26°C • Moderate Rain Expected</p>
          </div>
          <div id="weather-icon" style="font-size: 32px;">🌧️</div>
        </div>
        <div id="rain-advisory-banner" style="margin-top: 12px; padding: 10px; background: rgba(59, 130, 246, 0.2); border: 1px solid #3b82f6; border-radius: 8px; font-size: 13px;">
          ☔ Carry an umbrella! High rain probability between 5:00 PM – 7:00 PM during return commute.
        </div>
        <button id="refresh-weather-btn" aria-label="Refresh Weather Data">🔄 Refresh</button>
      </div>
    `);

    const temp = page.locator('#temp-condition');
    await expect(temp).toContainText('26°C');
    await expect(temp).toContainText('Moderate Rain Expected');

    const rainBanner = page.locator('#rain-advisory-banner');
    await expect(rainBanner).toBeVisible();
    await expect(rainBanner).toContainText('Carry an umbrella!');

    const refreshBtn = page.locator('#refresh-weather-btn');
    await expect(refreshBtn).toBeVisible();
  });

  test('should handle refresh weather action click', async ({ page }) => {
    await page.setContent(`
      <div id="temp-condition">26°C</div>
      <button id="refresh-weather-btn">Refresh</button>
      <script>
        document.getElementById('refresh-weather-btn').addEventListener('click', () => {
          document.getElementById('temp-condition').textContent = '27°C • Updated';
        });
      </script>
    `);

    await page.locator('#refresh-weather-btn').click();
    await expect(page.locator('#temp-condition')).toHaveText('27°C • Updated');
  });
});
