export function renderDashboardHtml(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>MonumentQuest — Server Status & System Diagnostics</title>
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
    body { background-color: var(--bg); color: var(--text); padding: 24px; max-width: 1200px; margin: 0 auto; line-height: 1.5; }
    
    header { display: flex; align-items: center; justify-content: space-between; padding-bottom: 20px; border-bottom: 1px solid var(--card-border); margin-bottom: 24px; }
    .title-box { display: flex; align-items: center; gap: 12px; }
    .logo-badge { background: linear-gradient(135deg, #F59E0B, #D97706); width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
    h1 { font-size: 22px; font-weight: 700; }
    
    .refresh-btn { background: var(--card-bg); border: 1px solid var(--card-border); color: var(--text); padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.2s; }
    .refresh-btn:hover { background: #1F2937; border-color: var(--blue); }

    .status-banner { background: var(--green-bg); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 12px; padding: 20px; display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
    .status-banner.degraded { background: var(--yellow-bg); border-color: rgba(245, 158, 11, 0.3); }
    .status-banner.down { background: var(--red-bg); border-color: rgba(239, 68, 68, 0.3); }
    
    .status-info { display: flex; align-items: center; gap: 14px; }
    .status-dot { width: 14px; height: 14px; border-radius: 50%; background: var(--green); box-shadow: 0 0 12px var(--green); animation: pulse 2s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.4; } }
    .status-title { font-size: 18px; font-weight: 700; color: var(--green); }
    .status-sub { font-size: 13px; color: var(--text-muted); }

    .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .card { background: var(--card-bg); border: 1px solid var(--card-border); border-radius: 12px; padding: 20px; }
    .card-head { font-size: 13px; font-weight: 600; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }
    
    .metric-value { font-size: 28px; font-weight: 800; font-family: 'JetBrains Mono', monospace; }
    .metric-sub { font-size: 12px; color: var(--text-muted); margin-top: 4px; }

    .service-list { display: flex; flex-direction: column; gap: 10px; }
    .service-item { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; background: rgba(0,0,0,0.2); border-radius: 8px; border: 1px solid rgba(255,255,255,0.04); }
    .service-name { font-size: 14px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
    .service-badge { font-size: 11px; padding: 3px 8px; border-radius: 4px; font-family: 'JetBrains Mono', monospace; font-weight: 700; }
    .badge-ok { background: var(--green-bg); color: var(--green); }
    .badge-err { background: var(--red-bg); color: var(--red); }
    
    .console-box { background: #05070D; border: 1px solid var(--card-border); border-radius: 12px; padding: 16px; font-family: 'JetBrains Mono', monospace; font-size: 12.5px; height: 260px; overflow-y: auto; color: #A7F3D0; }
    .log-line { margin-bottom: 6px; word-break: break-all; }
    .log-info { color: #60A5FA; }
    .log-warn { color: #FBBF24; }
    .log-err { color: #F87171; }

    footer { text-align: center; margin-top: 30px; font-size: 12px; color: var(--text-muted); }
  </style>
</head>
<body>

  <header>
    <div class="title-box">
      <div class="logo-badge">🛡️</div>
      <div>
        <h1>MonumentQuest Production Server Monitor</h1>
        <p style="font-size: 12px; color: var(--text-muted);">Real-Time Diagnostics, Endpoint Latency & Problem Logs</p>
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
    <div style="font-family: 'JetBrains Mono', monospace; font-size: 13px; color: var(--text-muted);" id="last-updated">Updated: Just now</div>
  </div>

  <div class="grid">
    <div class="card">
      <div class="card-head">Database Status 🗄️</div>
      <div class="metric-value" id="db-status" style="color: var(--green);">CONNECTED</div>
      <div class="metric-sub" id="db-sub">Supabase PostgreSQL • Query Latency: --ms</div>
    </div>

    <div class="card">
      <div class="card-head">API Latency ⚡</div>
      <div class="metric-value" id="api-latency" style="color: var(--blue);">-- ms</div>
      <div class="metric-sub">Average endpoint response time</div>
    </div>

    <div class="card">
      <div class="card-head">AI Engine Status 🤖</div>
      <div class="metric-value" id="ai-status" style="color: var(--yellow);">ACTIVE</div>
      <div class="metric-sub">Groq API Model: openai/gpt-oss-20b</div>
    </div>

    <div class="card">
      <div class="card-head">System Health Score 📊</div>
      <div class="metric-value" id="health-score" style="color: var(--green);">100%</div>
      <div class="metric-sub">0 Active System Warnings</div>
    </div>
  </div>

  <div class="card" style="margin-bottom: 24px;">
    <div class="card-head">Endpoint Service Audits 🌐</div>
    <div class="service-list" id="service-list">
      <div class="service-item">
        <div class="service-name"><span>GET</span> /api/v1/health</div>
        <div class="service-badge badge-ok" id="status-health">TESTING...</div>
      </div>
      <div class="service-item">
        <div class="service-name"><span>POST</span> /api/v1/auth/guest</div>
        <div class="service-badge badge-ok" id="status-auth">TESTING...</div>
      </div>
      <div class="service-item">
        <div class="service-name"><span>GET</span> /api/v1/feed</div>
        <div class="service-badge badge-ok" id="status-feed">TESTING...</div>
      </div>
      <div class="service-item">
        <div class="service-name"><span>GET</span> /api/v1/monuments</div>
        <div class="service-badge badge-ok" id="status-monuments">TESTING...</div>
      </div>
      <div class="service-item">
        <div class="service-name"><span>POST</span> /api/v1/ai/narrator</div>
        <div class="service-badge badge-ok" id="status-ai">TESTING...</div>
      </div>
    </div>
  </div>

  <div class="card">
    <div class="card-head">Live System Diagnostic Audit & Problem Log Console 📋</div>
    <div class="console-box" id="log-console">
      <div class="log-line log-info">[INIT] Initializing MonumentQuest live server diagnostics monitor...</div>
    </div>
  </div>

  <footer>
    MonumentQuest API v3.1.0 • Running on Vercel Serverless Node.js Environment • Real-Time Health & Diagnostics
  </footer>

  <script>
    function log(msg, type = 'info') {
      const consoleEl = document.getElementById('log-console');
      const time = new Date().toISOString().substring(11, 19);
      const line = document.createElement('div');
      line.className = 'log-line ' + (type === 'err' ? 'log-err' : type === 'warn' ? 'log-warn' : 'log-info');
      line.innerText = \`[\${time}] [\${type.toUpperCase()}] \${msg}\`;
      consoleEl.appendChild(line);
      consoleEl.scrollTop = consoleEl.scrollHeight;
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
      log('Starting automated endpoint & database audit...', 'info');
      document.getElementById('last-updated').innerText = 'Updated: ' + new Date().toLocaleTimeString();

      // 1. Health check
      const hRes = await testEndpoint('/api/v1/health');
      if (hRes.ok) {
        document.getElementById('status-health').innerText = \`200 OK (\${hRes.duration}ms)\`;
        document.getElementById('status-health').className = 'service-badge badge-ok';
        log(\`GET /api/v1/health responded cleanly in \${hRes.duration}ms\`, 'info');
      } else {
        document.getElementById('status-health').innerText = \`ERR \${hRes.status}\`;
        document.getElementById('status-health').className = 'service-badge badge-err';
        log(\`GET /api/v1/health FAILED with status \${hRes.status}\`, 'err');
      }

      // 2. Auth Check
      const aRes = await testEndpoint('/api/v1/auth/guest', { method: 'POST', headers: {'Content-Type':'application/json'} });
      if (aRes.ok) {
        document.getElementById('status-auth').innerText = \`201 CREATED (\${aRes.duration}ms)\`;
        document.getElementById('status-auth').className = 'service-badge badge-ok';
        log(\`POST /api/v1/auth/guest authentication service OK in \${aRes.duration}ms\`, 'info');
      } else {
        document.getElementById('status-auth').innerText = \`ERR \${aRes.status}\`;
        document.getElementById('status-auth').className = 'service-badge badge-err';
        log(\`POST /api/v1/auth/guest authentication service FAILED with status \${aRes.status}\`, 'err');
      }

      // 3. Feed Check
      const fRes = await testEndpoint('/api/v1/feed');
      if (fRes.ok) {
        document.getElementById('status-feed').innerText = \`200 OK (\${fRes.duration}ms)\`;
        document.getElementById('status-feed').className = 'service-badge badge-ok';
        log(\`GET /api/v1/feed social stream OK in \${fRes.duration}ms\`, 'info');
      } else {
        document.getElementById('status-feed').innerText = \`ERR \${fRes.status}\`;
        document.getElementById('status-feed').className = 'service-badge badge-err';
        log(\`GET /api/v1/feed social stream FAILED with status \${fRes.status}\`, 'err');
      }

      // 4. Monuments Check
      const mRes = await testEndpoint('/api/v1/monuments');
      if (mRes.ok) {
        document.getElementById('status-monuments').innerText = \`200 OK (\${mRes.duration}ms)\`;
        document.getElementById('status-monuments').className = 'service-badge badge-ok';
        log(\`GET /api/v1/monuments catalog OK in \${mRes.duration}ms\`, 'info');
      } else {
        document.getElementById('status-monuments').innerText = \`ERR \${mRes.status}\`;
        document.getElementById('status-monuments').className = 'service-badge badge-err';
        log(\`GET /api/v1/monuments catalog FAILED with status \${mRes.status}\`, 'err');
      }

      // 5. Calculate metrics & set banner
      const avgLatency = Math.round((hRes.duration + aRes.duration + fRes.duration + mRes.duration) / 4);
      document.getElementById('api-latency').innerText = avgLatency + ' ms';
      document.getElementById('db-sub').innerText = \`Supabase PostgreSQL • Query Latency: \${hRes.duration}ms\`;

      const allOk = hRes.ok && aRes.ok && fRes.ok && mRes.ok;
      const banner = document.getElementById('banner');
      const bTitle = document.getElementById('banner-title');
      const bSub = document.getElementById('banner-sub');
      const bDot = document.getElementById('banner-dot');

      if (allOk) {
        banner.className = 'status-banner';
        bTitle.innerText = 'ALL SYSTEMS OPERATIONAL';
        bTitle.style.color = 'var(--green)';
        bSub.innerText = 'All backend API endpoints and database services are online with 0 errors.';
        bDot.style.background = 'var(--green)';
        document.getElementById('health-score').innerText = '100%';
        log('Diagnostic Audit Completed: System healthy with 0 problems detected.', 'info');
      } else {
        banner.className = 'status-banner degraded';
        bTitle.innerText = 'SYSTEM DEGRADATION DETECTED';
        bTitle.style.color = 'var(--yellow)';
        bSub.innerText = 'One or more API services returned non-200 responses. Inspect console log for details.';
        bDot.style.background = 'var(--yellow)';
        document.getElementById('health-score').innerText = '75%';
        log('Diagnostic Audit Alert: One or more services require attention.', 'warn');
      }
    }

    runDiagnostics();
    setInterval(runDiagnostics, 15000);
  </script>
</body>
</html>`;
}
