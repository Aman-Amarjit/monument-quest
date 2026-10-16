export function renderDashboardHtml(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>MonumentQuest — Server Status & Diagnostics</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;500;700&family=Outfit:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #090D16;
      --card-bg: #111827;
      --card-border: #1F2937;
      --text: #F9FAFB;
      --text-muted: #9CA3AF;
      --green: #10B981;
      --green-bg: rgba(16, 185, 129, 0.1);
      --red: #EF4444;
      --red-bg: rgba(239, 68, 68, 0.1);
      --yellow: #F59E0B;
      --yellow-bg: rgba(245, 158, 11, 0.1);
      --blue: #3B82F6;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }
    body { background-color: var(--bg); color: var(--text); padding: 16px; max-width: 1200px; margin: 0 auto; line-height: 1.5; }
    
    header { display: flex; flex-direction: column; gap: 12px; padding-bottom: 16px; border-bottom: 1px solid var(--card-border); margin-bottom: 16px; }
    @media (min-width: 640px) {
      header { flex-direction: row; align-items: center; justify-content: space-between; padding-bottom: 20px; margin-bottom: 24px; gap: 16px; }
    }
    .title-box { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
    @media (min-width: 640px) { .title-box { gap: 12px; flex-wrap: nowrap; } }
    .logo-badge { background: linear-gradient(135deg, #F59E0B, #D97706); width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
    @media (min-width: 640px) { .logo-badge { width: 36px; height: 36px; font-size: 20px; border-radius: 10px; } }
    h1 { font-size: 16px; font-weight: 700; }
    @media (min-width: 640px) { h1 { font-size: 22px; } }
    
    .refresh-btn { background: var(--card-bg); border: 1px solid var(--card-border); color: var(--text); padding: 8px 14px; border-radius: 8px; font-size: 12px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.2s; white-space: nowrap; }
    @media (min-width: 640px) { .refresh-btn { padding: 8px 16px; font-size: 13px; } }
    .refresh-btn:hover { background: #1F2937; border-color: var(--blue); }

    .status-banner { background: var(--green-bg); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 10px; padding: 14px; display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; }
    @media (min-width: 640px) { .status-banner { border-radius: 12px; padding: 20px; flex-direction: row; align-items: center; justify-content: space-between; margin-bottom: 24px; gap: 16px; } }
    .status-banner.degraded { background: var(--yellow-bg); border-color: rgba(245, 158, 11, 0.3); }
    .status-banner.down { background: var(--red-bg); border-color: rgba(239, 68, 68, 0.3); }
    
    .status-info { display: flex; align-items: center; gap: 10px; }
    @media (min-width: 640px) { .status-info { gap: 14px; } }
    .status-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--green); box-shadow: 0 0 10px var(--green); animation: pulse 2s infinite; flex-shrink: 0; }
    @media (min-width: 640px) { .status-dot { width: 14px; height: 14px; box-shadow: 0 0 12px var(--green); } }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
    .status-title { font-size: 14px; font-weight: 700; color: var(--green); word-break: break-word; }
    @media (min-width: 640px) { .status-title { font-size: 18px; } }
    .status-sub { font-size: 11px; color: var(--text-muted); word-break: break-word; }
    @media (min-width: 640px) { .status-sub { font-size: 13px; } }

    .grid { display: grid; grid-template-columns: 1fr; gap: 12px; margin-bottom: 16px; }
    @media (min-width: 640px) { .grid { grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); gap: 16px; margin-bottom: 24px; } }
    .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 10px; padding: 14px; margin-bottom: 16px; }
    @media (min-width: 640px) { .card { border-radius: 12px; padding: 20px; } }
    .card-head { font-size: 11px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 6px; }
    @media (min-width: 640px) { .card-head { font-size: 13px; margin-bottom: 12px; flex-wrap: nowrap; } }
    
    .metric-value { font-size: 20px; font-weight: 800; font-family: 'JetBrains Mono', monospace; word-break: break-word; }
    @media (min-width: 640px) { .metric-value { font-size: 28px; } }
    .metric-sub { font-size: 10px; color: var(--text-muted); margin-top: 4px; word-break: break-word; }
    @media (min-width: 640px) { .metric-sub { font-size: 12px; } }

    .service-list { display: flex; flex-direction: column; gap: 8px; }
    @media (min-width: 640px) { .service-list { gap: 10px; } }
    .service-item { display: flex; flex-direction: column; gap: 6px; padding: 10px; background: rgba(0,0,0,0.2); border-radius: 8px; border: 1px solid rgba(255,255,255,0.04); }
    @media (min-width: 640px) { .service-item { flex-direction: row; align-items: center; justify-content: space-between; padding: 10px 14px; gap: 10px; } }
    .service-name { font-size: 12px; font-weight: 600; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; word-break: break-word; }
    @media (min-width: 640px) { .service-name { font-size: 14px; gap: 8px; flex-wrap: nowrap; } }
    .service-badge { font-size: 10px; padding: 4px 8px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-weight: 700; align-self: flex-start; white-space: nowrap; }
    @media (min-width: 640px) { .service-badge { font-size: 11px; padding: 3px 8px; align-self: auto; } }
    .badge-ok { background: var(--green-bg); color: var(--green); }
    .badge-err { background: var(--red-bg); color: var(--red); }
    .badge-warn { background: var(--yellow-bg); color: var(--yellow); }
    
    .console-box { background: #05070D; border: 1px solid var(--card-border); border-radius: 10px; padding: 12px; font-family: 'JetBrains Mono', monospace; font-size: 10.5px; height: 200px; overflow-y: auto; color: #A7F3D0; }
    @media (min-width: 640px) { .console-box { border-radius: 12px; padding: 16px; font-size: 12.5px; height: 260px; } }
    .log-line { margin-bottom: 5px; word-break: break-all; line-height: 1.4; }
    @media (min-width: 640px) { .log-line { margin-bottom: 6px; } }
    .log-info { color: #60A5FA; }
    .log-warn { color: #FBBF24; }
    .log-err { color: #F87171; }
    .log-ok { color: #34D399; }

    footer { text-align: center; margin-top: 20px; font-size: 10px; color: var(--text-muted); word-break: break-word; }
    @media (min-width: 640px) { footer { margin-top: 30px; font-size: 12px; } }
    
    .text-muted { color: var(--text-muted); }
    .mono { font-family: 'JetBrains Mono', monospace; }
    @media (min-width: 640px) { .card { margin-bottom: 20px; } }
  </style>
</head>
<body>

  <header>
    <div class="title-box">
      <div class="logo-badge">🛡️</div>
      <div>
        <h1>MonumentQuest Server Monitor</h1>
        <p style="font-size: 11px; color: var(--text-muted);" id="runtime-info">Loading runtime info...</p>
      </div>
    </div>
    <button class="refresh-btn" onclick="runDiagnostics()">
      <span>🔄 Run Live Diagnostics</span>
    </button>
  </header>

  <div class="status-banner" id="banner">
    <div class="status-info">
      <div class="status-dot" id="banner-dot"></div>
      <div>
        <div class="status-title" id="banner-title">Checking Server Status...</div>
        <div class="status-sub" id="banner-sub">Performing real-time backend API & database diagnostics</div>
      </div>
    </div>
    <div style="font-family: 'JetBrains Mono', monospace; font-size: 11px; color: var(--text-muted);" id="last-updated">Updated: Just now</div>
  </div>

  <div class="grid">
    <div class="card">
      <div class="card-head">Database Status 🗄️</div>
      <div class="metric-value" id="db-status" style="color: var(--green);">--</div>
      <div class="metric-sub" id="db-sub">Loading...</div>
    </div>

    <div class="card">
      <div class="card-head">API Latency ⚡</div>
      <div class="metric-value" id="api-latency" style="color: var(--blue);">-- ms</div>
      <div class="metric-sub">Max endpoint response time</div>
    </div>

    <div class="card">
      <div class="card-head">AI Engine 🤖</div>
      <div class="metric-value" id="ai-status" style="color: var(--yellow);">--</div>
      <div class="metric-sub" id="ai-sub">Checking config...</div>
    </div>

    <div class="card">
      <div class="card-head">System Health 📊</div>
      <div class="metric-value" id="health-score" style="color: var(--green);">--%</div>
      <div class="metric-sub" id="health-sub">0 Active Issues</div>
    </div>

    <div class="card">
      <div class="card-head">Runtime Info 💻</div>
      <div style="font-size: 11px; color: var(--text); line-height: 1.6; word-break: break-word;" id="runtime-details">Loading...</div>
    </div>

    <div class="card">
      <div class="card-head">Memory Usage 🧠</div>
      <div class="metric-value" id="mem-percent" style="color: var(--blue);">--%</div>
      <div class="metric-sub" id="mem-details">Loading...</div>
    </div>
  </div>

  <div class="card">
    <div class="card-head">Endpoint Service Audits 🌐</div>
    <div class="service-list" id="service-list">
      <div class="service-item">
        <div class="service-name"><span class="mono">GET</span> /api/v1/health</div>
        <div class="service-badge badge-ok" id="status-health">TESTING...</div>
      </div>
      <div class="service-item">
        <div class="service-name"><span class="mono">POST</span> /api/v1/auth/guest</div>
        <div class="service-badge badge-ok" id="status-auth">TESTING...</div>
      </div>
      <div class="service-item">
        <div class="service-name"><span class="mono">GET</span> /api/v1/feed</div>
        <div class="service-badge badge-ok" id="status-feed">TESTING...</div>
      </div>
      <div class="service-item">
        <div class="service-name"><span class="mono">GET</span> /api/v1/monuments</div>
        <div class="service-badge badge-ok" id="status-monuments">TESTING...</div>
      </div>
    </div>
  </div>

  <div class="card">
    <div class="card-head">Live Activity Log 📋</div>
    <div class="console-box" id="log-console">
      <div class="log-line log-info">[INIT] Initializing live server diagnostics...</div>
    </div>
  </div>

  <footer>
    MonumentQuest API • Real-Time Health & Diagnostics
  </footer>

  <script>
    let diagData = null;

    function log(msg, type = 'info') {
      const consoleEl = document.getElementById('log-console');
      const time = new Date().toISOString().substring(11, 19);
      const line = document.createElement('div');
      const typeClass = type === 'err' ? 'log-err' : type === 'warn' ? 'log-warn' : type === 'ok' ? 'log-ok' : 'log-info';
      line.className = 'log-line ' + typeClass;
      line.innerText = '[' + time + '] [' + type.toUpperCase() + '] ' + msg;
      consoleEl.appendChild(line);
      consoleEl.scrollTop = consoleEl.scrollHeight;
    }

    async function fetchDiagnostics() {
      try {
        const res = await fetch('/diagnostics');
        if (res.ok) {
          diagData = await res.json();
          populateDiagnostics();
          return diagData;
        }
      } catch (e) {
        log('Failed to fetch diagnostics: ' + e.message, 'err');
      }
      return null;
    }

    function populateDiagnostics() {
      if (!diagData) return;
      
      document.getElementById('runtime-info').innerText = diagData.platform + ' ' + diagData.arch + ' • Node ' + diagData.nodeVersion + ' • Up ' + diagData.uptimeFormatted;
      
      document.getElementById('runtime-details').innerHTML = 
        '<div><span class="text-muted">Platform:</span> <span class="mono">' + diagData.platform + '/' + diagData.arch + '</span></div>' +
        '<div><span class="text-muted">Node:</span> <span class="mono">' + diagData.nodeVersion + '</span></div>' +
        '<div><span class="text-muted">PID:</span> <span class="mono">' + diagData.pid + '</span></div>' +
        '<div><span class="text-muted">Uptime:</span> <span class="mono">' + diagData.uptimeFormatted + '</span></div>' +
        '<div><span class="text-muted">ENV:</span> <span class="mono">' + diagData.env.nodeEnv + '</span></div>' +
        '<div><span class="text-muted">Port:</span> <span class="mono">' + diagData.env.port + '</span></div>';
      
      document.getElementById('db-status').innerText = diagData.db.connected ? 'CONNECTED' : 'DISCONNECTED';
      document.getElementById('db-status').style.color = diagData.db.connected ? 'var(--green)' : 'var(--red)';
      document.getElementById('db-sub').innerText = diagData.env.dbProvider + (diagData.db.pingMs >= 0 ? ' • Ping: ' + diagData.db.pingMs + 'ms' : ' • No DB connection');
      
      if (diagData.env.hasGroq) {
        document.getElementById('ai-status').innerText = 'CONFIGURED';
        document.getElementById('ai-status').style.color = 'var(--green)';
        document.getElementById('ai-sub').innerText = 'GROQ_API_KEY is set';
      } else {
        document.getElementById('ai-status').innerText = 'NOT CONFIGURED';
        document.getElementById('ai-status').style.color = 'var(--yellow)';
        document.getElementById('ai-sub').innerText = 'GROQ_API_KEY not set';
      }
      
      document.getElementById('mem-percent').innerText = diagData.memory.usedPercent + '%';
      document.getElementById('mem-details').innerText = 'Heap: ' + diagData.memory.heapUsed + ' / ' + diagData.memory.heapTotal + ' • RSS: ' + diagData.memory.rss;
      
      log('Diagnostics loaded • DB: ' + (diagData.db.connected ? 'OK (' + diagData.db.pingMs + 'ms)' : 'FAIL') + ' • AI: ' + (diagData.env.hasGroq ? 'OK' : 'MISS'), diagData.db.connected ? 'ok' : 'warn');
    }

    async function testEndpoint(url, options = {}) {
      const start = performance.now();
      try {
        const res = await fetch(url, options);
        const duration = Math.round(performance.now() - start);
        return { ok: res.ok, status: res.status, duration };
      } catch (e) {
        return { ok: false, status: 0, duration: Math.round(performance.now() - start), error: e.message };
      }
    }

    async function runDiagnostics() {
      log('Running endpoint audits...', 'info');
      document.getElementById('last-updated').innerText = 'Updated: ' + new Date().toLocaleTimeString();

      const [hRes, aRes, fRes, mRes] = await Promise.all([
        testEndpoint('/api/v1/health'),
        testEndpoint('/api/v1/auth/guest', { method: 'POST', headers: {'Content-Type':'application/json'} }),
        testEndpoint('/api/v1/feed'),
        testEndpoint('/api/v1/monuments')
      ]);

      setBadge('health', hRes);
      setBadge('auth', aRes);
      setBadge('feed', fRes);
      setBadge('monuments', mRes);

      log('Health: ' + (hRes.ok ? 'OK' : 'ERR') + ' (' + hRes.duration + 'ms)', hRes.ok ? 'ok' : 'err');
      log('Auth Guest: ' + (aRes.ok ? 'OK' : 'ERR') + ' (' + aRes.duration + 'ms)', aRes.ok ? 'ok' : 'err');
      log('Feed: ' + (fRes.ok ? 'OK' : 'ERR') + ' (' + fRes.duration + 'ms)', fRes.ok ? 'ok' : 'err');
      log('Monuments: ' + (mRes.ok ? 'OK' : 'ERR') + ' (' + mRes.duration + 'ms)', mRes.ok ? 'ok' : 'err');

      const durations = [hRes.duration, aRes.duration, fRes.duration, mRes.duration];
      const maxDuration = Math.max(...durations);
      document.getElementById('api-latency').innerText = maxDuration + ' ms';

      const allOk = hRes.ok && aRes.ok && fRes.ok && mRes.ok;
      const banner = document.getElementById('banner');
      const bTitle = document.getElementById('banner-title');
      const bSub = document.getElementById('banner-sub');
      const bDot = document.getElementById('banner-dot');

      if (allOk) {
        banner.className = 'status-banner';
        bTitle.innerText = 'ALL SYSTEMS OPERATIONAL';
        bTitle.style.color = 'var(--green)';
        bSub.innerText = 'All backend endpoints responding normally.';
        bDot.style.background = 'var(--green)';
        document.getElementById('health-score').innerText = '100%';
        document.getElementById('health-sub').innerText = '0 Active Issues';
        log('Audit complete: All systems operational', 'ok');
      } else {
        banner.className = 'status-banner degraded';
        bTitle.innerText = 'SYSTEM DEGRADATION DETECTED';
        bTitle.style.color = 'var(--yellow)';
        bSub.innerText = 'One or more API services returned errors.';
        bDot.style.background = 'var(--yellow)';
        document.getElementById('health-score').innerText = '75%';
        document.getElementById('health-sub').innerText = 'Issues detected';
        log('Audit complete: Issues detected', 'warn');
      }
    }

    function setBadge(id, res) {
      const el = document.getElementById('status-' + id);
      if (res.ok) {
        el.innerText = res.status + ' OK (' + res.duration + 'ms)';
        el.className = 'service-badge badge-ok';
      } else {
        el.innerText = 'ERR ' + res.status + ' (' + res.duration + 'ms)';
        el.className = 'service-badge badge-err';
      }
    }

    async function init() {
      await fetchDiagnostics();
      await runDiagnostics();
      setInterval(async () => {
        await fetchDiagnostics();
        await runDiagnostics();
      }, 15000);
    }

    init();
  </script>
</body>
</html>`;
}
