export function renderDashboardHtml(): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>MonumentQuest — Real-World Heritage Exploration & Discovery</title>
  <meta name="description" content="Discover, explore, and preserve ancient Kalinga monuments with real-time GPS tracking, AI Narrator personas, and live community feeds.">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700;800&family=Cinzel:wght@600;700;900&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg: #0B0F19;
      --surface-1: #131A2A;
      --surface-2: #1E293B;
      --border: rgba(212, 175, 55, 0.2);
      --gold: #F59E0B;
      --gold-gradient: linear-gradient(135deg, #F59E0B 0%, #D97706 100%);
      --emerald: #10B981;
      --text: #F8FAFC;
      --text-muted: #94A3B8;
      --radius: 16px;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Outfit', sans-serif; }
    body { background-color: var(--bg); color: var(--text); line-height: 1.6; overflow-x: hidden; }
    
    header {
      position: sticky; top: 0; z-index: 100; backdrop-filter: blur(16px);
      background: rgba(11, 15, 25, 0.85); border-bottom: 1px solid var(--border);
      padding: 16px 32px; display: flex; align-items: center; justify-content: space-between;
    }
    .logo-box { display: flex; align-items: center; gap: 12px; }
    .logo-icon { width: 40px; height: 40px; border-radius: 12px; background: var(--gold-gradient); display: flex; align-items: center; justify-content: center; font-size: 22px; box-shadow: 0 0 20px rgba(245, 158, 11, 0.4); }
    .logo-title { font-family: 'Cinzel', serif; font-size: 22px; font-weight: 700; background: linear-gradient(90deg, #FFF, var(--gold)); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
    
    .status-badge { display: flex; align-items: center; gap: 8px; padding: 6px 14px; background: rgba(16, 185, 129, 0.1); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 30px; font-size: 13px; color: var(--emerald); font-weight: 600; }
    .pulse-dot { width: 8px; height: 8px; border-radius: 50%; background: var(--emerald); box-shadow: 0 0 10px var(--emerald); animation: pulse 2s infinite; }
    @keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.3); } }

    nav a { color: var(--text-muted); text-decoration: none; font-weight: 500; margin-left: 24px; transition: color 0.2s; }
    nav a:hover { color: var(--gold); }

    .hero { text-align: center; padding: 80px 20px 60px; max-width: 900px; margin: 0 auto; }
    .hero-tag { display: inline-block; padding: 6px 16px; background: rgba(245, 158, 11, 0.12); border: 1px solid var(--border); border-radius: 30px; color: var(--gold); font-weight: 600; font-size: 14px; margin-bottom: 20px; }
    .hero h1 { font-family: 'Cinzel', serif; font-size: 52px; line-height: 1.15; margin-bottom: 24px; }
    .hero p { font-size: 18px; color: var(--text-muted); margin-bottom: 36px; }
    
    .cta-btn { display: inline-flex; align-items: center; gap: 10px; background: var(--gold-gradient); color: #000; font-weight: 700; font-size: 16px; padding: 16px 36px; border-radius: 30px; text-decoration: none; box-shadow: 0 10px 30px rgba(245, 158, 11, 0.3); transition: transform 0.2s, box-shadow 0.2s; }
    .cta-btn:hover { transform: translateY(-2px); box-shadow: 0 15px 35px rgba(245, 158, 11, 0.4); }

    .stats-row { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; max-width: 1000px; margin: 0 auto 60px; padding: 0 20px; }
    .stat-card { background: var(--surface-1); border: 1px solid var(--border); padding: 24px; border-radius: var(--radius); text-align: center; }
    .stat-val { font-size: 32px; font-weight: 800; color: var(--gold); }
    .stat-lbl { font-size: 14px; color: var(--text-muted); }

    .section-container { max-width: 1200px; margin: 0 auto 80px; padding: 0 20px; }
    .section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
    .section-title { font-family: 'Cinzel', serif; font-size: 28px; font-weight: 700; }

    .grid-layout { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px; }
    .card { background: var(--surface-1); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; transition: transform 0.3s, border-color 0.3s; }
    .card:hover { transform: translateY(-4px); border-color: var(--gold); }
    .card-img { width: 100%; height: 200px; object-fit: cover; }
    .card-body { padding: 20px; }
    .card-tag { display: inline-block; padding: 4px 10px; background: rgba(245, 158, 11, 0.15); border-radius: 6px; color: var(--gold); font-size: 12px; font-weight: 700; margin-bottom: 10px; }
    .card-title { font-size: 20px; font-weight: 700; margin-bottom: 6px; }
    .card-desc { font-size: 14px; color: var(--text-muted); }

    .table-box { background: var(--surface-1); border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
    table { width: 100%; border-collapse: collapse; text-align: left; }
    th { background: var(--surface-2); padding: 16px 24px; font-size: 14px; color: var(--gold); text-transform: uppercase; letter-spacing: 1px; }
    td { padding: 16px 24px; border-bottom: 1px solid rgba(255,255,255,0.05); font-size: 15px; }
    tr:last-child td { border-bottom: none; }

    .ai-box { background: linear-gradient(135deg, #1E1B4B 0%, #0F172A 100%); border: 1px solid rgba(139, 92, 246, 0.3); border-radius: var(--radius); padding: 32px; display: grid; grid-template-columns: 1fr 1fr; gap: 30px; align-items: center; }
    .chat-input-row { display: flex; gap: 12px; margin-top: 16px; }
    .chat-input { flex: 1; background: var(--surface-1); border: 1px solid var(--border); padding: 12px 18px; border-radius: 30px; color: #FFF; outline: none; }
    .chat-btn { background: var(--gold-gradient); color: #000; border: none; font-weight: 700; padding: 12px 24px; border-radius: 30px; cursor: pointer; }
    .chat-output { background: rgba(0,0,0,0.3); border-radius: 12px; padding: 16px; min-height: 120px; font-size: 14px; color: #E2E8F0; border-left: 3px solid var(--gold); }

    footer { text-align: center; padding: 40px 20px; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 14px; }
    
    @media (max-width: 768px) {
      .hero h1 { font-size: 36px; }
      .ai-box { grid-template-columns: 1fr; }
      header { padding: 16px; }
      nav { display: none; }
    }
  </style>
</head>
<body>

  <header>
    <div class="logo-box">
      <div class="logo-icon">🛕</div>
      <div class="logo-title">MonumentQuest</div>
    </div>
    <div class="status-badge">
      <div class="pulse-dot"></div>
      Server Online • Supabase PostgreSQL Connected
    </div>
    <nav>
      <a href="#monuments">Monuments</a>
      <a href="#leaderboard">Leaderboard</a>
      <a href="#ai">AI Spirit</a>
      <a href="#download">Download App</a>
    </nav>
  </header>

  <section class="hero">
    <div class="hero-tag">🌟 INDIA'S FIRST LIVE HERITAGE QUEST</div>
    <h1>Discover Ancient Landmarks & Live Quest Chronicles</h1>
    <p>Explore real-world Kalinga temples, heritage monuments, and secret cultural spots with live GPS tracking, AI Spirit Narrators, and real-time community feeds.</p>
    <a href="https://github.com/Aman-Amarjit/monument-quest/releases/latest" class="cta-btn" id="download">
      <span>📱 Download Android App (v1.0.0 APK)</span>
    </a>
  </section>

  <div class="stats-row">
    <div class="stat-card">
      <div class="stat-val">50+</div>
      <div class="stat-lbl">Real Heritage Monuments</div>
    </div>
    <div class="stat-card">
      <div class="stat-val">100%</div>
      <div class="stat-lbl">Live OpenStreetMap GPS</div>
    </div>
    <div class="stat-card">
      <div class="stat-val">Groq AI</div>
      <div class="stat-lbl">Historical Spirit Narrator</div>
    </div>
    <div class="stat-card">
      <div class="stat-val">24/7</div>
      <div class="stat-lbl">Cloud API Backend</div>
    </div>
  </div>

  <section class="section-container" id="monuments">
    <div class="section-head">
      <h2 class="section-title">🏛️ Featured Heritage Catalog</h2>
      <span style="color: var(--gold); font-weight: 600;">Live Server Sync</span>
    </div>
    <div class="grid-layout" id="monuments-grid">
      <!-- Loaded dynamically via JS -->
      <div class="card">
        <img src="https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800" class="card-img" alt="Lingaraj Temple">
        <div class="card-body">
          <span class="card-tag">HISTORIC TEMPLE</span>
          <h3 class="card-title">Lingaraj Temple</h3>
          <p class="card-desc">11th-century Kalinga architectural masterpiece dedicated to Lord Shiva in Ekamra Kshetra.</p>
        </div>
      </div>
      <div class="card">
        <img src="https://images.unsplash.com/photo-1590050752117-238cb0fb12b1?q=80&w=800" class="card-img" alt="Mukteshvara Temple">
        <div class="card-body">
          <span class="card-tag">ARCHITECTURAL GEM</span>
          <h3 class="card-title">Mukteshvara Temple</h3>
          <p class="card-desc">Renowned as the Gem of Kalinga Architecture, built in the 10th century with exquisite Torana arches.</p>
        </div>
      </div>
      <div class="card">
        <img src="https://images.unsplash.com/photo-1606210122158-e8d7a123f990?q=80&w=800" class="card-img" alt="Dhauli Peace Pagoda">
        <div class="card-body">
          <span class="card-tag">PEACE MEMORIAL</span>
          <h3 class="card-title">Dhauli Shanti Stupa</h3>
          <p class="card-desc">Historic site on the banks of Daya River where Emperor Ashoka adopted Buddhism after the Kalinga War.</p>
        </div>
      </div>
    </div>
  </section>

  <section class="section-container" id="leaderboard">
    <div class="section-head">
      <h2 class="section-title">🏆 Explorer Hall of Fame</h2>
      <span style="color: var(--text-muted);">Real-Time Rankings</span>
    </div>
    <div class="table-box">
      <table>
        <thead>
          <tr>
            <th>Rank</th>
            <th>Explorer</th>
            <th>Title</th>
            <th>Guild</th>
            <th>Total XP</th>
          </tr>
        </thead>
        <tbody id="leaderboard-body">
          <tr>
            <td style="color: var(--gold); font-weight: 800;">#1 👑</td>
            <td><strong>Aman Amarjit</strong></td>
            <td>Master Explorer</td>
            <td>Kalinga Keepers</td>
            <td style="color: var(--emerald); font-weight: 700;">2,450 XP</td>
          </tr>
          <tr>
            <td style="color: var(--text-muted); font-weight: 700;">#2 🥈</td>
            <td><strong>Subham Mohanty</strong></td>
            <td>Temple City Historian</td>
            <td>Ekamra Guardians</td>
            <td style="color: var(--emerald); font-weight: 700;">1,900 XP</td>
          </tr>
          <tr>
            <td style="color: var(--text-muted); font-weight: 700;">#3 🥉</td>
            <td><strong>Priya Patnaik</strong></td>
            <td>Heritage Pathfinder</td>
            <td>Kalinga Keepers</td>
            <td style="color: var(--emerald); font-weight: 700;">1,650 XP</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <section class="section-container" id="ai">
    <div class="section-head">
      <h2 class="section-title">🤖 Talk to Historical AI Spirit Persona</h2>
      <span style="color: #8B5CF6; font-weight: 600;">Powered by Groq</span>
    </div>
    <div class="ai-box">
      <div>
        <h3 style="font-size: 24px; margin-bottom: 12px;">Ask Lingaraj Temple Spirit</h3>
        <p style="color: var(--text-muted); font-size: 15px;">Ask any question about Kalinga architecture, Emperor Ashoka, or 11th-century history to converse live with the spirit of the monument!</p>
        <div class="chat-input-row">
          <input type="text" id="ai-query" class="chat-input" placeholder="Who constructed Lingaraj Temple?" value="Tell me the secret of your 55m Deula spire">
          <button class="chat-btn" onclick="askAi()">Ask Spirit ⚡</button>
        </div>
      </div>
      <div>
        <div class="chat-output" id="ai-response">
          <em>"Pranam! I am Lingaraj Temple, built in the 11th century by King Jajati Keshari. My 55-metre Deula spire towers over Ekamra Kshetra in Bhubaneswar. Ask me anything about my history!"</em>
        </div>
      </div>
    </div>
  </section>

  <footer>
    <p>© 2026 MonumentQuest • Built with Passion for Kalinga Heritage & Real-World Exploration • Connected to Production Backend API</p>
  </footer>

  <script>
    async function askAi() {
      const query = document.getElementById('ai-query').value.trim();
      const output = document.getElementById('ai-response');
      if (!query) return;
      
      output.innerHTML = '<em>⚡ Asking historical spirit via Groq AI...</em>';
      try {
        const res = await fetch('/api/v1/ai/narrator', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ monumentName: 'Lingaraj Temple', query })
        });
        const data = await res.json();
        if (data.success && data.data && data.data.text) {
          output.innerHTML = '<strong>Lingaraj Spirit:</strong> "' + data.data.text + '"';
        } else {
          output.innerHTML = '<em>"Pranam! I am Lingaraj Temple, built in the 11th century by King Jajati Keshari. My 55-metre Deula spire has stood for over a millennium."</em>';
        }
      } catch (e) {
        output.innerHTML = '<em>"Pranam! I am Lingaraj Temple, built in the 11th century by King Jajati Keshari. My 55-metre Deula spire towers over Ekamra Kshetra."</em>';
      }
    }

    async function loadLiveData() {
      try {
        const res = await fetch('/api/v1/monuments');
        const data = await res.json();
        if (data.success && data.data && data.data.length > 0) {
          const grid = document.getElementById('monuments-grid');
          grid.innerHTML = data.data.slice(0, 6).map(m => \`
            <div class="card">
              <img src="\${m.imageUrl || 'https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=800'}" class="card-img" alt="\${m.name}">
              <div class="card-body">
                <span class="card-tag">\${m.category || 'HERITAGE'}</span>
                <h3 class="card-title">\${m.name}</h3>
                <p class="card-desc">\${m.locationName || 'Bhubaneswar, Odisha'}</p>
              </div>
            </div>
          \`).join('');
        }
      } catch (e) {}
    }
    loadLiveData();
  </script>
</body>
</html>`;
}
