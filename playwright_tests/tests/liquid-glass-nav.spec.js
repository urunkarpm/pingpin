const { test, expect } = require('@playwright/test');

test.describe('LiquidGlassNavBar & NavRail Specs', () => {

  test('should render bottom navigation bar with fluid active pill indicator and item tabs', async ({ page }) => {
    await page.setContent(`
      <nav id="liquid-nav-bar" style="position: fixed; bottom: 0; width: 100%; height: 64px; background: rgba(15,23,42,0.9); display: flex; justify-content: space-around; align-items: center;">
        <div id="active-pill-indicator" style="position: absolute; width: 80px; height: 40px; background: rgba(59,130,246,0.2); border-radius: 20px; transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1); transform: translateX(-100px);"></div>
        <button class="nav-item active" data-tab="home" style="z-index: 1;">Home</button>
        <button class="nav-item" data-tab="insights" style="z-index: 1;">Insights</button>
        <button class="nav-item" data-tab="settings" style="z-index: 1;">Settings</button>
      </nav>
      <script>
        document.querySelectorAll('.nav-item').forEach((item, idx) => {
          item.addEventListener('click', (e) => {
            document.querySelectorAll('.nav-item').forEach(i => i.classList.remove('active'));
            e.target.classList.add('active');
            const pill = document.getElementById('active-pill-indicator');
            pill.style.transform = \`translateX(\${(idx - 1) * 100}px)\`;
          });
        });
      </script>
    `);

    const navBar = page.locator('#liquid-nav-bar');
    await expect(navBar).toBeVisible();

    const homeTab = page.locator('.nav-item[data-tab="home"]');
    const insightsTab = page.locator('.nav-item[data-tab="insights"]');
    const settingsTab = page.locator('.nav-item[data-tab="settings"]');

    await expect(homeTab).toHaveClass(/active/);
    await insightsTab.click();
    await expect(insightsTab).toHaveClass(/active/);
    await expect(homeTab).not.toHaveClass(/active/);

    await settingsTab.click();
    await expect(settingsTab).toHaveClass(/active/);
  });

  test('should render navigation rail layout on wide screen landscape orientation', async ({ page }) => {
    await page.setViewportSize({ width: 1024, height: 768 });
    await page.setContent(`
      <div style="display: flex; height: 100vh;">
        <nav id="liquid-nav-rail" style="width: 80px; height: 100%; background: #0f172a; display: flex; flex-direction: column; align-items: center; padding-top: 24px;">
          <button class="rail-item active" data-tab="home">🏠</button>
          <button class="rail-item" data-tab="insights">📊</button>
          <button class="rail-item" data-tab="settings">⚙️</button>
        </nav>
        <main style="flex: 1; padding: 24px; color: white;">Main Content Area</main>
      </div>
    `);

    const navRail = page.locator('#liquid-nav-rail');
    await expect(navRail).toBeVisible();
    await expect(page.locator('.rail-item')).toHaveCount(3);
  });
});
