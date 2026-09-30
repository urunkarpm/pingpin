const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

const ARTIFACT_DIR = 'C:\\Users\\uprasenjeet\\.gemini\\antigravity-cli\\brain\\feb4541e-b989-44ad-a4a7-2c5d611ae165';

if (!fs.existsSync(ARTIFACT_DIR)) {
  fs.mkdirSync(ARTIFACT_DIR, { recursive: true });
}

async function renderSnapshots() {
  const browser = await chromium.launch();
  const context = await browser.newContext({
    viewport: { width: 412, height: 915 },
    deviceScaleFactor: 2
  });
  const page = await context.newPage();

  // 1. HOME SCREEN SNAPSHOT
  await page.setContent(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #000000; color: #F8FAFC; width: 412px; min-height: 915px; padding: 20px 16px 90px 16px; position: relative; }
        
        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .app-title { font-size: 22px; font-weight: 800; letter-spacing: -0.5px; display: flex; align-items: center; gap: 8px; }
        .logo-dot { width: 10px; height: 10px; border-radius: 50%; background: #3B82F6; box-shadow: 0 0 10px #3B82F6; }
        .streak-badge { background: rgba(245, 158, 11, 0.15); border: 1px solid rgba(245, 158, 11, 0.4); padding: 6px 14px; border-radius: 20px; display: flex; align-items: center; gap: 6px; font-weight: 700; font-size: 13px; color: #F59E0B; }
        
        .glass-card { background: #10141D; border: 1px solid #242A38; border-radius: 22px; padding: 18px; margin-bottom: 16px; box-shadow: 0 4px 20px rgba(0,0,0,0.5); }
        
        .today-card { border: 1px solid rgba(16, 185, 129, 0.35); background: linear-gradient(135deg, rgba(16, 185, 129, 0.08) 0%, #10141D 100%); }
        .card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
        .status-chip { background: #064E3B; color: #10B981; padding: 5px 12px; border-radius: 12px; font-size: 12px; font-weight: 800; text-transform: uppercase; letter-spacing: 0.8px; }
        .time-tag { font-size: 13px; color: #94A3B8; font-weight: 500; }
        .status-main { font-size: 24px; font-weight: 800; color: #F8FAFC; margin-bottom: 6px; }
        .status-sub { font-size: 13px; color: #94A3B8; margin-bottom: 16px; display: flex; align-items: center; gap: 6px; }
        .wifi-icon { color: #10B981; }
        
        .action-row { display: flex; gap: 10px; }
        .btn-punch { flex: 1; background: #3B82F6; color: white; padding: 12px; border-radius: 14px; border: none; font-weight: 700; font-size: 14px; display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; }
        .btn-secondary { background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.1); color: #F8FAFC; padding: 12px 16px; border-radius: 14px; font-weight: 600; font-size: 14px; }
        
        .section-title { font-size: 15px; font-weight: 700; color: #94A3B8; margin: 16px 0 10px 4px; text-transform: uppercase; letter-spacing: 1px; }
        
        .week-strip { display: flex; justify-content: space-between; gap: 6px; }
        .day-cell { flex: 1; display: flex; flex-direction: column; align-items: center; padding: 10px 4px; border-radius: 14px; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); }
        .day-cell.today { border-color: #3B82F6; background: rgba(59, 130, 246, 0.12); }
        .day-cell.attended { border-color: rgba(16, 185, 129, 0.4); background: rgba(16, 185, 129, 0.1); }
        .day-name { font-size: 11px; color: #64748B; font-weight: 600; margin-bottom: 4px; }
        .day-num { font-size: 15px; font-weight: 700; color: #F8FAFC; }
        .day-dot { width: 6px; height: 6px; border-radius: 50%; margin-top: 6px; }
        .dot-green { background: #10B981; }
        .dot-purple { background: #8B5CF6; }
        .dot-gray { background: #334155; }
        
        .weather-card { display: flex; align-items: center; gap: 14px; }
        .weather-icon-box { width: 50px; height: 50px; border-radius: 16px; background: rgba(59, 130, 246, 0.15); display: flex; align-items: center; justify-content: center; font-size: 26px; }
        .weather-info { flex: 1; }
        .weather-temp { font-size: 20px; font-weight: 800; color: #F8FAFC; }
        .weather-desc { font-size: 12px; color: #94A3B8; }
        .weather-tag { background: rgba(59, 130, 246, 0.2); color: #60A5FA; padding: 4px 10px; border-radius: 10px; font-size: 11px; font-weight: 700; }
        
        .nav-bar { position: fixed; bottom: 0; left: 0; right: 0; height: 74px; background: rgba(10, 12, 18, 0.85); backdrop-filter: blur(20px); border-top: 1px solid rgba(255,255,255,0.08); display: flex; justify-content: space-around; align-items: center; padding: 0 16px; z-index: 100; }
        .nav-item { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #64748B; text-decoration: none; font-size: 12px; font-weight: 600; }
        .nav-item.active { color: #3B82F6; }
        .nav-pill { background: rgba(59, 130, 246, 0.18); color: #3B82F6; padding: 6px 20px; border-radius: 20px; display: flex; align-items: center; gap: 6px; font-weight: 700; }
      </style>
    </head>
    <body>
      <div class="header">
        <div class="app-title"><div class="logo-dot"></div>PingPin</div>
        <div class="streak-badge">🔥 14-Day Streak</div>
      </div>

      <div class="glass-card today-card">
        <div class="card-top">
          <span class="status-chip">Present (WFO)</span>
          <span class="time-tag">Check-In: 09:14 AM</span>
        </div>
        <div class="status-main">On-Site Office Day</div>
        <div class="status-sub">
          <span class="wifi-icon">📶</span> Connected to <strong>Corp_Guest_5G</strong>
        </div>
        <div class="action-row">
          <button class="btn-punch">🌐 Auto-Punch Portal</button>
          <button class="btn-secondary">⚙️ Log Notes</button>
        </div>
      </div>

      <div class="section-title">Weekly Attendance (Target: 3 WFO)</div>
      <div class="glass-card">
        <div class="week-strip">
          <div class="day-cell attended">
            <span class="day-name">MON</span>
            <span class="day-num">28</span>
            <div class="day-dot dot-green"></div>
          </div>
          <div class="day-cell attended">
            <span class="day-name">TUE</span>
            <span class="day-num">29</span>
            <div class="day-dot dot-green"></div>
          </div>
          <div class="day-cell today attended">
            <span class="day-name">WED</span>
            <span class="day-num">30</span>
            <div class="day-dot dot-green"></div>
          </div>
          <div class="day-cell">
            <span class="day-name">THU</span>
            <span class="day-num">01</span>
            <div class="day-dot dot-purple"></div>
          </div>
          <div class="day-cell">
            <span class="day-name">FRI</span>
            <span class="day-num">02</span>
            <div class="day-dot dot-purple"></div>
          </div>
          <div class="day-cell">
            <span class="day-name">SAT</span>
            <span class="day-num">03</span>
            <div class="day-dot dot-gray"></div>
          </div>
          <div class="day-cell">
            <span class="day-name">SUN</span>
            <span class="day-num">04</span>
            <div class="day-dot dot-gray"></div>
          </div>
        </div>
        <div style="margin-top: 14px; font-size: 13px; color: #10B981; font-weight: 600; display: flex; justify-content: space-between;">
          <span>Weekly Target: 3/3 Days Completed 🎉</span>
          <span>100%</span>
        </div>
      </div>

      <div class="section-title">Commute Insights</div>
      <div class="glass-card weather-card">
        <div class="weather-icon-box">🌦️</div>
        <div class="weather-info">
          <div class="weather-temp">27°C • Light Showers</div>
          <div class="weather-desc">Evening commute: 45% rain chance at 6 PM</div>
        </div>
        <span class="weather-tag">Umbrella Advised</span>
      </div>

      <div class="section-title">Upcoming Holiday</div>
      <div class="glass-card" style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <div style="font-size: 16px; font-weight: 700;">Mahatma Gandhi Jayanti</div>
          <div style="font-size: 13px; color: #94A3B8; margin-top: 2px;">Friday, October 2 • National Holiday</div>
        </div>
        <div style="background: rgba(139, 92, 246, 0.2); color: #A78BFA; padding: 6px 12px; border-radius: 12px; font-size: 12px; font-weight: 800;">
          3-DAY LONG WEEKEND
        </div>
      </div>

      <div class="nav-bar">
        <div class="nav-pill">🏠 Home</div>
        <div class="nav-item">📊 Insights</div>
        <div class="nav-item">⚙️ Settings</div>
      </div>
    </body>
    </html>
  `);
  const homePath = path.join(ARTIFACT_DIR, 'ui_home_screen.png');
  await page.screenshot({ path: homePath });
  console.log(`Saved Home Screen screenshot: ${homePath}`);

  // 2. INSIGHTS SCREEN SNAPSHOT
  await page.setContent(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { background: #000000; color: #F8FAFC; width: 412px; min-height: 915px; padding: 20px 16px 90px 16px; }
        
        .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
        .month-selector { display: flex; align-items: center; gap: 12px; font-size: 20px; font-weight: 800; }
        .arrow-btn { background: #10141D; border: 1px solid #242A38; color: #F8FAFC; width: 34px; height: 34px; border-radius: 10px; display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 14px; }
        
        .glass-card { background: #10141D; border: 1px solid #242A38; border-radius: 22px; padding: 18px; margin-bottom: 16px; }
        
        .radial-row { display: flex; align-items: center; gap: 20px; }
        .radial-circle { width: 90px; height: 90px; border-radius: 50%; border: 8px solid rgba(16, 185, 129, 0.2); border-top-color: #10B981; border-right-color: #10B981; border-bottom-color: #10B981; display: flex; flex-direction: column; align-items: center; justify-content: center; }
        .radial-val { font-size: 22px; font-weight: 800; color: #10B981; }
        .radial-lbl { font-size: 10px; color: #94A3B8; text-transform: uppercase; font-weight: 700; }
        .stats-grid { flex: 1; display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
        .stat-box { background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); padding: 8px 12px; border-radius: 12px; }
        .stat-val { font-size: 16px; font-weight: 800; color: #F8FAFC; }
        .stat-lbl { font-size: 11px; color: #94A3B8; }
        
        .calendar-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 6px; margin-top: 14px; }
        .weekday-hdr { text-align: center; font-size: 11px; font-weight: 700; color: #64748B; padding-bottom: 6px; }
        .cal-cell { aspect-ratio: 1; border-radius: 10px; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; background: rgba(255,255,255,0.03); border: 1px solid rgba(255,255,255,0.06); position: relative; }
        .cal-cell.present { background: rgba(16, 185, 129, 0.2); border-color: rgba(16, 185, 129, 0.5); color: #10B981; }
        .cal-cell.wfo-target { border: 1px dashed rgba(139, 92, 246, 0.6); color: #C4B5FD; }
        .cal-cell.extra-wfo { background: rgba(59, 130, 246, 0.2); border-color: rgba(59, 130, 246, 0.6); color: #60A5FA; }
        .cal-cell.today { border: 2px solid #3B82F6; }
        .cal-cell.off { color: #334155; }
        
        .legend-row { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 14px; }
        .legend-pill { display: flex; align-items: center; gap: 6px; background: rgba(255,255,255,0.04); border: 1px solid rgba(255,255,255,0.08); padding: 4px 10px; border-radius: 12px; font-size: 11px; font-weight: 600; color: #94A3B8; }
        .legend-dot { width: 8px; height: 8px; border-radius: 50%; }
        
        .btn-export { width: 100%; background: linear-gradient(135deg, #1E293B, #0F172A); border: 1px solid rgba(59,130,246,0.3); color: #60A5FA; padding: 14px; border-radius: 16px; font-size: 14px; font-weight: 700; display: flex; align-items: center; justify-content: center; gap: 8px; cursor: pointer; }
      </style>
    </head>
    <body>
      <div class="header">
        <div class="month-selector">
          <div class="arrow-btn">◀</div>
          <span>September 2026</span>
          <div class="arrow-btn">▶</div>
        </div>
      </div>

      <div class="glass-card">
        <div class="radial-row">
          <div class="radial-circle">
            <span class="radial-val">92%</span>
            <span class="radial-lbl">Compliance</span>
          </div>
          <div class="stats-grid">
            <div class="stat-box">
              <div class="stat-val">12 / 13</div>
              <div class="stat-lbl">WFO Attended</div>
            </div>
            <div class="stat-box">
              <div class="stat-val">+2 Days</div>
              <div class="stat-lbl">Extra WFO</div>
            </div>
            <div class="stat-box">
              <div class="stat-val">14 Days</div>
              <div class="stat-lbl">Active Streak</div>
            </div>
            <div class="stat-box">
              <div class="stat-val">0 Missed</div>
              <div class="stat-lbl">Recovery Needed</div>
            </div>
          </div>
        </div>
      </div>

      <div class="glass-card">
        <div style="font-size: 15px; font-weight: 700; color: #F8FAFC; margin-bottom: 6px;">Monthly Log Grid</div>
        <div class="calendar-grid">
          <div class="weekday-hdr">M</div><div class="weekday-hdr">T</div><div class="weekday-hdr">W</div><div class="weekday-hdr">T</div><div class="weekday-hdr">F</div><div class="weekday-hdr">S</div><div class="weekday-hdr">S</div>
          <div class="cal-cell present">1</div><div class="cal-cell present">2</div><div class="cal-cell present">3</div><div class="cal-cell present">4</div><div class="cal-cell off">5</div><div class="cal-cell off">6</div><div class="cal-cell present">7</div>
          <div class="cal-cell present">8</div><div class="cal-cell present">9</div><div class="cal-cell present">10</div><div class="cal-cell extra-wfo">11</div><div class="cal-cell off">12</div><div class="cal-cell off">13</div><div class="cal-cell present">14</div>
          <div class="cal-cell present">15</div><div class="cal-cell present">16</div><div class="cal-cell present">17</div><div class="cal-cell off">18</div><div class="cal-cell off">19</div><div class="cal-cell off">20</div><div class="cal-cell present">21</div>
          <div class="cal-cell present">22</div><div class="cal-cell present">23</div><div class="cal-cell present">24</div><div class="cal-cell extra-wfo">25</div><div class="cal-cell off">26</div><div class="cal-cell off">27</div><div class="cal-cell present">28</div>
          <div class="cal-cell present">29</div><div class="cal-cell present today">30</div><div class="cal-cell wfo-target">1</div><div class="cal-cell wfo-target">2</div><div class="cal-cell off">3</div><div class="cal-cell off">4</div><div class="cal-cell off">5</div>
        </div>

        <div class="legend-row">
          <div class="legend-pill"><div class="legend-dot" style="background:#10B981;"></div>Present (12)</div>
          <div class="legend-pill"><div class="legend-dot" style="background:#60A5FA;"></div>Extra WFO (2)</div>
          <div class="legend-pill"><div class="legend-dot" style="background:#8B5CF6;"></div>Target WFO</div>
          <div class="legend-pill"><div class="legend-dot" style="background:#EF4444;"></div>Missed (0)</div>
        </div>
      </div>

      <button class="btn-export">
        <span>📄</span> Export Official PDF Attendance Statement
      </button>
    </body>
    </html>
  `);
  const insightsPath = path.join(ARTIFACT_DIR, 'ui_insights_screen.png');
  await page.screenshot({ path: insightsPath });
  console.log(`Saved Insights Screen screenshot: ${insightsPath}`);

  // 3. ALARM ACTIVITY FULL-SCREEN SNAPSHOT
  await page.setContent(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8">
      <style>
        * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
        body { width: 412px; height: 915px; background: linear-gradient(180deg, #020617 0%, #000000 100%); color: #F8FAFC; display: flex; flex-direction: column; justify-content: space-between; align-items: center; padding: 48px 24px 36px 24px; }
        
        .top-status { display: flex; flex-direction: column; align-items: center; gap: 10px; }
        .alarm-chip { background: rgba(59, 130, 246, 0.15); border: 1px solid rgba(59, 130, 246, 0.4); padding: 8px 18px; border-radius: 50px; display: flex; align-items: center; gap: 8px; font-size: 13px; font-weight: 800; color: #60A5FA; letter-spacing: 1.5px; }
        .pulse-dot { width: 8px; height: 8px; border-radius: 50%; background: #3B82F6; box-shadow: 0 0 8px #3B82F6; }
        .portal-url { font-size: 12px; color: #64748B; background: rgba(255,255,255,0.04); padding: 4px 12px; border-radius: 20px; }

        .hero-section { display: flex; flex-direction: column; align-items: center; }
        .sonar-orb { width: 120px; height: 120px; border-radius: 50%; border: 2px solid rgba(59, 130, 246, 0.6); background: radial-gradient(circle, rgba(59, 130, 246, 0.25) 0%, transparent 70%); display: flex; align-items: center; justify-content: center; font-size: 42px; margin-bottom: 24px; box-shadow: 0 0 35px rgba(59, 130, 246, 0.3); }
        .clock-display { display: flex; align-items: baseline; gap: 8px; }
        .giant-time { font-size: 76px; font-weight: 900; letter-spacing: -3px; color: #F8FAFC; line-height: 1; }
        .ampm-pill { background: rgba(59, 130, 246, 0.2); color: #3B82F6; font-size: 16px; font-weight: 900; padding: 4px 10px; border-radius: 10px; }
        .alarm-date { font-size: 16px; color: #94A3B8; margin-top: 10px; font-weight: 500; }

        .actions-col { width: 100%; display: flex; flex-direction: column; gap: 12px; }
        .btn-checkin { height: 62px; border-radius: 20px; background: linear-gradient(90deg, #2563EB, #06B6D4); color: white; font-size: 16px; font-weight: 800; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; gap: 8px; box-shadow: 0 4px 20px rgba(37, 99, 235, 0.4); }
        .btn-snooze { height: 52px; border-radius: 18px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.12); color: #F8FAFC; font-size: 14px; font-weight: 700; cursor: pointer; }
        .btn-leave { height: 50px; border-radius: 18px; background: transparent; border: 1px solid rgba(239, 68, 68, 0.3); color: #F87171; font-size: 14px; font-weight: 700; cursor: pointer; }
      </style>
    </head>
    <body>
      <div class="top-status">
        <div class="alarm-chip"><div class="pulse-dot"></div>CHECK-IN REMINDER</div>
        <div class="portal-url">Target: https://company.hrms.portal/punch</div>
      </div>

      <div class="hero-section">
        <div class="sonar-orb">🔔</div>
        <div class="clock-display">
          <span class="giant-time">09:30</span>
          <span class="ampm-pill">AM</span>
        </div>
        <div class="alarm-date">Wednesday, September 30</div>
      </div>

      <div class="actions-col">
        <button class="btn-checkin">⚡ CHECK-IN & AUTO-PUNCH →</button>
        <button class="btn-snooze">⏱️ SNOOZE FOR 10 MINS</button>
        <button class="btn-leave">✉️ APPLY FOR LEAVE TODAY</button>
      </div>
    </body>
    </html>
  `);
  const alarmPath = path.join(ARTIFACT_DIR, 'ui_alarm_screen.png');
  await page.screenshot({ path: alarmPath });
  console.log(`Saved Alarm Screen screenshot: ${alarmPath}`);

  await browser.close();
}

renderSnapshots().catch(err => {
  console.error('Error rendering snapshots:', err);
  process.exit(1);
});
