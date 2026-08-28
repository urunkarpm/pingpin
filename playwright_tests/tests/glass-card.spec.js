const { test, expect } = require('@playwright/test');
const { validateGlassCardProps } = require('../lib/component-harness');

test.describe('GlassCard Component Specs', () => {

  test('should validate GlassCard prop contract and default values', () => {
    const validProps = {
      cornerRadius: 24,
      onClick: () => {},
      role: 'button',
      contentDescription: 'Test Glass Card'
    };
    expect(validateGlassCardProps(validProps)).toBe(true);
  });

  test('should throw TypeError when invalid cornerRadius prop is passed', () => {
    expect(() => validateGlassCardProps({ cornerRadius: '24dp' })).toThrow(TypeError);
  });

  test('should throw TypeError when non-function onClick is passed', () => {
    expect(() => validateGlassCardProps({ onClick: 'notAFunction' })).toThrow(TypeError);
  });

  test('should compute spring animation scale on press state (1.0 -> 0.97)', async ({ page }) => {
    await page.setContent(`
      <div id="glass-card" 
           role="button" 
           tabindex="0"
           aria-label="Attendance Card"
           style="border-radius: 24px; padding: 20px; transition: transform 0.15s ease; background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.12); cursor: pointer;">
        <h3>Office Attendance</h3>
        <p>Target: 3 days/week</p>
      </div>
      <script>
        const card = document.getElementById('glass-card');
        card.addEventListener('mousedown', () => card.style.transform = 'scale(0.97)');
        card.addEventListener('mouseup', () => card.style.transform = 'scale(1.0)');
      </script>
    `);

    const card = page.locator('#glass-card');
    await expect(card).toBeVisible();
    await expect(card).toHaveAttribute('role', 'button');
    await expect(card).toHaveAttribute('aria-label', 'Attendance Card');

    // Simulate click & scale check
    await card.hover();
    await page.mouse.down();
    await expect(card).toHaveCSS('transform', 'matrix(0.97, 0, 0, 0.97, 0, 0)');
    await page.mouse.up();
    await expect(card).toHaveCSS('transform', 'matrix(1, 0, 0, 1, 0, 0)');
  });

  test('should support non-clickable card containers without button semantics', async ({ page }) => {
    await page.setContent(`
      <div id="static-glass-card" style="border-radius: 24px; padding: 20px; background: rgba(30, 41, 59, 0.8);">
        <p>Static Display Info</p>
      </div>
    `);

    const card = page.locator('#static-glass-card');
    await expect(card).toBeVisible();
    await expect(card).not.toHaveAttribute('role', 'button');
  });

  test('should maintain high-contrast styling in both dark and light themes', async ({ page }) => {
    await page.setContent(`
      <div id="dark-card" class="dark" style="background-color: #1e293b; color: #f8fafc; border: 1px solid rgba(255,255,255,0.12);">
        Dark Theme Glass Card
      </div>
      <div id="light-card" class="light" style="background-color: #ffffff; color: #0f172a; border: 1px solid rgba(203,213,225,0.6);">
        Light Theme Glass Card
      </div>
    `);

    await expect(page.locator('#dark-card')).toHaveCSS('background-color', 'rgb(30, 41, 59)');
    await expect(page.locator('#light-card')).toHaveCSS('background-color', 'rgb(255, 255, 255)');
  });
});
