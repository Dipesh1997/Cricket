const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const outputDir = path.join(__dirname, 'assets', 'play_store_screenshots');
if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

const edgePath = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';

// Helper SVG player portraits
function getPlayerAvatar(color, num, roleIcon) {
  return `
    <svg viewBox="0 0 100 100" style="width: 100%; height: 100%; border-radius: 50%;">
      <defs>
        <radialGradient id="grad_${num}" cx="50%" cy="30%" r="70%">
          <stop offset="0%" stop-color="#fff" stop-opacity="0.25"/>
          <stop offset="100%" stop-color="#000" stop-opacity="0.6"/>
        </radialGradient>
      </defs>
      <rect width="100" height="100" fill="${color}"/>
      <circle cx="50" cy="35" r="22" fill="#ffdbac"/>
      <!-- hair -->
      <path d="M 28 32 C 28 15, 72 15, 72 32 C 72 20, 28 20, 28 32 Z" fill="#2a1d17"/>
      <!-- Jersey body -->
      <path d="M 15 100 L 85 100 L 78 60 L 65 52 L 35 52 L 22 60 Z" fill="${color}"/>
      <path d="M 35 52 L 50 68 L 65 52 Z" fill="#ffffff" opacity="0.8"/>
      <!-- Collar / jersey stripe -->
      <text x="50" y="85" font-family="'Outfit', sans-serif" font-weight="900" font-size="20" fill="#ffffff" text-anchor="middle">${num}</text>
      <!-- Shadow overlay -->
      <rect width="100" height="100" fill="url(#grad_${num})"/>
    </svg>
  `;
}

// Helper SVG Spin Wheel
function getSpinWheelSvg() {
  return `
    <svg viewBox="0 0 400 400" style="width: 340px; height: 340px; filter: drop-shadow(0 15px 35px rgba(0,0,0,0.8));">
      <defs>
        <filter id="glow">
          <feGaussianBlur stdDeviation="3" result="coloredBlur"/>
          <feMerge>
            <feMergeNode in="coloredBlur"/>
            <feMergeNode in="SourceGraphic"/>
          </feMerge>
        </filter>
      </defs>
      <!-- Outer Ring -->
      <circle cx="200" cy="200" r="195" fill="#141824" stroke="#FFD700" stroke-width="8" filter="url(#glow)"/>
      <circle cx="200" cy="200" r="185" fill="#0d111a" stroke="rgba(255,215,0,0.4)" stroke-width="2"/>
      
      <!-- 8 Segments (45 deg each) -->
      <!-- Seg 1: RCB Red -->
      <path d="M 200 200 L 200 20 A 180 180 0 0 1 327.28 72.72 Z" fill="#EC1C24"/>
      <!-- Seg 2: CSK Gold -->
      <path d="M 200 200 L 327.28 72.72 A 180 180 0 0 1 380 200 Z" fill="#FDB913"/>
      <!-- Seg 3: MI Blue -->
      <path d="M 200 200 L 380 200 A 180 180 0 0 1 327.28 327.28 Z" fill="#004BA0"/>
      <!-- Seg 4: KKR Purple -->
      <path d="M 200 200 L 327.28 327.28 A 180 180 0 0 1 200 380 Z" fill="#3A225D"/>
      <!-- Seg 5: SRH Orange -->
      <path d="M 200 200 L 200 380 A 180 180 0 0 1 72.72 327.28 Z" fill="#FF5722"/>
      <!-- Seg 6: RR Pink -->
      <path d="M 200 200 L 72.72 327.28 A 180 180 0 0 1 20 200 Z" fill="#E91E63"/>
      <!-- Seg 7: DC Blue -->
      <path d="M 200 200 L 20 200 A 180 180 0 0 1 72.72 72.72 Z" fill="#0078FF"/>
      <!-- Seg 8: GT Emerald -->
      <path d="M 200 200 L 72.72 72.72 A 180 180 0 0 1 200 20 Z" fill="#1B5E20"/>

      <!-- Labels on Segments -->
      <g fill="#ffffff" font-family="'Outfit', sans-serif" font-weight="800" font-size="12" text-anchor="middle">
        <text transform="translate(250, 95) rotate(22.5)">V. KOHLI</text>
        <text transform="translate(310, 160) rotate(67.5)">MS DHONI</text>
        <text transform="translate(310, 240) rotate(112.5)">R. SHARMA</text>
        <text transform="translate(250, 305) rotate(157.5)">S. NARINE</text>
        <text transform="translate(150, 305) rotate(202.5)">H. KLAASEN</text>
        <text transform="translate(90, 240) rotate(247.5)">S. SAMSON</text>
        <text transform="translate(90, 160) rotate(292.5)">R. PANT</text>
        <text transform="translate(150, 95) rotate(337.5)">R. KHAN</text>
      </g>

      <!-- Center Hub -->
      <circle cx="200" cy="200" r="50" fill="#1b2234" stroke="#FFD700" stroke-width="5"/>
      <circle cx="200" cy="200" r="38" fill="linear-gradient(135deg, #FFD700, #FFA000)"/>
      <text x="200" y="196" font-family="'Outfit', sans-serif" font-weight="900" font-size="14" fill="#000" text-anchor="middle">SPIN</text>
      <text x="200" y="214" font-family="'Outfit', sans-serif" font-weight="900" font-size="12" fill="#000" text-anchor="middle">WHEEL</text>

      <!-- Gold Pointer Triangle at Top -->
      <polygon points="200,32 186,2 214,2" fill="#FFD700" stroke="#000" stroke-width="2" filter="url(#glow)"/>
    </svg>
  `;
}

// Generate base frame wrapper
function wrapInPlayStoreMockup(options) {
  const {
    badge,
    title,
    titleHighlight,
    subtitle,
    activeTab,
    appContentHtml
  } = options;

  return `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@400;600;700;800;900&family=Inter:wght@400;500;600;700;800&display=swap" rel="stylesheet">
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      width: 1080px;
      height: 2280px;
      overflow: hidden;
      font-family: 'Inter', sans-serif;
      background: radial-gradient(circle at 50% 12%, rgba(0, 210, 255, 0.22) 0%, rgba(13, 19, 34, 0.98) 55%), linear-gradient(180deg, #090e1a 0%, #05070d 100%);
      color: #ffffff;
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 60px 40px 0;
      position: relative;
    }

    /* Ambient stadium spotlight effects */
    .stadium-beam-left {
      position: absolute;
      top: -100px;
      left: -100px;
      width: 450px;
      height: 450px;
      background: radial-gradient(circle, rgba(255, 215, 0, 0.15) 0%, transparent 70%);
      pointer-events: none;
    }
    .stadium-beam-right {
      position: absolute;
      top: -100px;
      right: -100px;
      width: 450px;
      height: 450px;
      background: radial-gradient(circle, rgba(0, 210, 255, 0.2) 0%, transparent 70%);
      pointer-events: none;
    }

    /* PROMO HEADER */
    .promo-header {
      text-align: center;
      width: 100%;
      max-width: 960px;
      height: 340px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      z-index: 10;
    }
    .badge-pill {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      background: rgba(255, 215, 0, 0.12);
      border: 1.5px solid rgba(255, 215, 0, 0.6);
      padding: 10px 24px;
      border-radius: 40px;
      color: #FFD700;
      font-family: 'Outfit', sans-serif;
      font-size: 20px;
      font-weight: 800;
      letter-spacing: 2px;
      text-transform: uppercase;
      box-shadow: 0 4px 20px rgba(255, 215, 0, 0.2);
      margin-bottom: 20px;
    }
    .promo-title {
      font-family: 'Outfit', sans-serif;
      font-size: 58px;
      font-weight: 900;
      line-height: 1.15;
      letter-spacing: -0.5px;
      text-transform: uppercase;
      margin-bottom: 14px;
      text-shadow: 0 4px 30px rgba(0,0,0,0.8);
    }
    .promo-title span {
      background: linear-gradient(135deg, #FFD700 0%, #FFA000 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      filter: drop-shadow(0 2px 10px rgba(255, 215, 0, 0.4));
    }
    .promo-sub {
      font-size: 24px;
      font-weight: 500;
      color: #9cb1d1;
      max-width: 860px;
      line-height: 1.4;
    }

    /* PHONE FRAME CONTAINER */
    .phone-container {
      width: 940px;
      height: 1840px;
      background: #0d121f;
      border-radius: 60px 60px 0 0;
      border: 12px solid #202b40;
      border-bottom: none;
      box-shadow: 0 30px 90px rgba(0,0,0,0.9), 0 0 50px rgba(0, 210, 255, 0.12);
      display: flex;
      flex-direction: column;
      position: relative;
      overflow: hidden;
      z-index: 5;
    }

    /* Dynamic Island / Punch Hole */
    .punch-hole {
      width: 140px;
      height: 28px;
      background: #000;
      border-radius: 20px;
      margin: 10px auto 4px;
      z-index: 30;
    }

    /* STATUS BAR */
    .status-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0 36px 8px;
      font-size: 18px;
      font-weight: 700;
      color: #b0c2de;
    }

    /* APP TOP BAR */
    .app-top-bar {
      background: #151d2f;
      padding: 16px 28px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1.5px solid rgba(255, 215, 0, 0.25);
    }
    .app-brand {
      display: flex;
      align-items: center;
      gap: 12px;
      color: #FFD700;
      font-family: 'Outfit', sans-serif;
      font-size: 24px;
      font-weight: 900;
      letter-spacing: 1px;
    }
    .top-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .role-tag {
      background: rgba(255, 215, 0, 0.15);
      border: 1px solid #FFD700;
      color: #FFD700;
      padding: 6px 14px;
      border-radius: 18px;
      font-size: 15px;
      font-weight: 800;
    }

    /* SUB TABS BAR (When inside List Tab) */
    .app-sub-tabs {
      background: #111726;
      display: flex;
      border-bottom: 1.5px solid #202b40;
    }
    .sub-tab-item {
      flex: 1;
      padding: 16px 8px;
      text-align: center;
      font-size: 18px;
      font-weight: 700;
      color: #7d90b2;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .sub-tab-item.active {
      color: #FFD700;
      border-bottom: 3.5px solid #FFD700;
      background: rgba(255, 215, 0, 0.05);
    }

    /* APP CONTENT BODY */
    .app-body {
      flex: 1;
      padding: 24px 28px;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    /* BOTTOM NAV BAR (5 TABS) */
    .bottom-nav {
      background: #131a29;
      border-top: 1.5px solid rgba(255,255,255,0.08);
      display: flex;
      justify-content: space-around;
      padding: 14px 10px 24px;
      z-index: 20;
    }
    .nav-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 6px;
      color: #6e82a3;
      font-size: 16px;
      font-weight: 600;
      flex: 1;
    }
    .nav-item.active {
      color: #FFD700;
      font-weight: 800;
    }
    .nav-item.active .nav-icon {
      background: rgba(255, 215, 0, 0.18);
      color: #FFD700;
      padding: 6px 18px;
      border-radius: 16px;
    }
    .nav-icon {
      font-size: 26px;
    }

    /* SHARED UI COMPONENTS */
    .card {
      background: #151c2e;
      border: 1.5px solid rgba(255, 255, 255, 0.08);
      border-radius: 24px;
      padding: 24px;
    }
    .card-gold {
      border-color: rgba(255, 215, 0, 0.45);
      background: linear-gradient(145deg, #182136 0%, #111726 100%);
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.4);
    }
    .chip {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 6px 14px;
      border-radius: 12px;
      font-size: 15px;
      font-weight: 800;
    }
    .chip-gold { background: rgba(255, 215, 0, 0.18); color: #FFD700; border: 1px solid rgba(255, 215, 0, 0.5); }
    .chip-blue { background: rgba(0, 210, 255, 0.18); color: #00D2FF; border: 1px solid rgba(0, 210, 255, 0.5); }
    .chip-green { background: rgba(0, 230, 118, 0.18); color: #00E676; border: 1px solid rgba(0, 230, 118, 0.5); }
    .chip-red { background: rgba(255, 23, 68, 0.18); color: #FF1744; border: 1px solid rgba(255, 23, 68, 0.5); }
    .chip-purple { background: rgba(186, 104, 200, 0.18); color: #BA68C8; border: 1px solid rgba(186, 104, 200, 0.5); }

    .btn {
      padding: 16px 28px;
      border-radius: 16px;
      font-family: 'Outfit', sans-serif;
      font-size: 20px;
      font-weight: 800;
      border: none;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      text-transform: uppercase;
      box-shadow: 0 6px 20px rgba(0,0,0,0.4);
    }
    .btn-gold { background: linear-gradient(135deg, #FFD700 0%, #FFA000 100%); color: #000; box-shadow: 0 8px 25px rgba(255, 215, 0, 0.35); }
    .btn-green { background: linear-gradient(135deg, #00E676 0%, #00C853 100%); color: #000; }
    .btn-blue { background: linear-gradient(135deg, #00D2FF 0%, #0091EA 100%); color: #000; }
    .btn-red { background: linear-gradient(135deg, #FF1744 0%, #D50000 100%); color: #FFF; }
    .btn-secondary { background: #202b40; color: #FFF; border: 1px solid rgba(255,255,255,0.12); }
  </style>
</head>
<body>

  <div class="stadium-beam-left"></div>
  <div class="stadium-beam-right"></div>

  <!-- PROMOTIONAL BANNER -->
  <div class="promo-header">
    <div class="badge-pill">${badge}</div>
    <h1 class="promo-title">${title} <span>${titleHighlight}</span></h1>
    <p class="promo-sub">${subtitle}</p>
  </div>

  <!-- PHONE HARDWARE FRAME -->
  <div class="phone-container">
    <div class="punch-hole"></div>

    <div class="status-bar">
      <span>19:30</span>
      <div style="display: flex; gap: 8px;">
        <span>5G 📶</span>
        <span>100% 🔋</span>
      </div>
    </div>

    <!-- APP BAR -->
    <div class="app-top-bar">
      <div class="app-brand">
        <span>🏏</span>
        <span>CRICKET HUB</span>
      </div>
      <div class="top-actions">
        <span class="role-tag">👑 Admin Host</span>
        <span style="font-size: 22px;">🌙</span>
      </div>
    </div>

    ${options.subTabsHtml || ''}

    <!-- APP MAIN CONTENT -->
    <div class="app-body">
      ${appContentHtml}
    </div>

    <!-- 5-TAB BOTTOM NAVIGATION -->
    <div class="bottom-nav">
      <div class="nav-item ${activeTab === 'dashboard' ? 'active' : ''}">
        <span class="nav-icon">📊</span>
        <span>Dashboard</span>
      </div>
      <div class="nav-item ${activeTab === 'auction' ? 'active' : ''}">
        <span class="nav-icon">🔨</span>
        <span>Auction</span>
      </div>
      <div class="nav-item ${activeTab === 'list' ? 'active' : ''}">
        <span class="nav-icon">📋</span>
        <span>List</span>
      </div>
      <div class="nav-item ${activeTab === 'scorer' ? 'active' : ''}">
        <span class="nav-icon">🏏</span>
        <span>Scorer</span>
      </div>
      <div class="nav-item ${activeTab === 'settings' ? 'active' : ''}">
        <span class="nav-icon">⚙️</span>
        <span>Settings</span>
      </div>
    </div>

  </div>

</body>
</html>`;
}

// DEFINITION OF 8 PLAY STORE SCREENSHOTS
const screenshots = [
  // SCREENSHOT 1: Live Auction Arena with Bidding
  {
    id: 1,
    filename: 'screenshot_1_auction_arena',
    badge: '⚡ LIVE IPL AUCTION ARENA',
    title: 'REAL-TIME BIDDING &',
    titleHighlight: 'COUNTDOWN TIMER',
    subtitle: 'Experience authentic mega auction thrills with dynamic bids, team purse counters & live bidder console.',
    activeTab: 'auction',
    content: `
      <div class="card card-gold" style="display: flex; flex-direction: column; gap: 16px;">
        <!-- Top Set Header -->
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span class="chip chip-gold">⭐ SET 1: MARQUEE ALL-ROUNDER</span>
          <span class="chip chip-red" style="font-size: 18px; padding: 8px 18px;">⏱️ 12s REMAINING</span>
        </div>

        <!-- Hero Player Card -->
        <div style="display: flex; gap: 24px; align-items: center; background: rgba(0,0,0,0.3); padding: 18px; border-radius: 20px;">
          <div style="width: 140px; height: 140px; flex-shrink: 0; border: 3px solid #FFD700; border-radius: 50%; overflow: hidden; box-shadow: 0 0 25px rgba(255,215,0,0.4);">
            ${getPlayerAvatar('#EC1C24', 18, '🏏')}
          </div>
          <div style="flex: 1;">
            <div style="display: flex; gap: 8px; margin-bottom: 6px;">
              <span class="chip chip-blue">India 🇮🇳</span>
              <span class="chip chip-purple">Batting All-Rounder</span>
            </div>
            <h2 style="font-family: 'Outfit', sans-serif; font-size: 36px; font-weight: 900; color: #FFF; line-height: 1.1;">ARJUN SHARMA</h2>
            <p style="color: #8da1c2; font-size: 16px; margin-top: 4px;">IPL: 148 Matches | 4,210 Runs | 48 Wickets</p>
            <div style="margin-top: 8px; font-size: 16px; color: #FFD700; font-weight: 700;">Base Price: ₹2.00 Cr</div>
          </div>
        </div>

        <!-- Current Highest Bid Banner -->
        <div style="background: linear-gradient(135deg, rgba(255,215,0,0.18) 0%, rgba(236,28,36,0.25) 100%); border: 2px solid #FFD700; border-radius: 20px; padding: 18px 24px; display: flex; justify-content: space-between; align-items: center;">
          <div>
            <div style="font-size: 16px; color: #8da1c2; font-weight: 700;">CURRENT HIGHEST BIDDER</div>
            <div style="font-family: 'Outfit', sans-serif; font-size: 32px; font-weight: 900; color: #FFF;">
              🔴 RCB <span style="font-size: 20px; color: #FFD700;">(Royal Challengers)</span>
            </div>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 16px; color: #8da1c2; font-weight: 700;">BID AMOUNT</div>
            <div style="font-family: 'Outfit', sans-serif; font-size: 44px; font-weight: 900; color: #FFD700; text-shadow: 0 0 20px rgba(255,215,0,0.5);">
              ₹14.50 Cr
            </div>
          </div>
        </div>

        <!-- Bid Increment Quick Buttons -->
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px;">
          <button class="btn btn-secondary" style="font-size: 16px; padding: 12px 6px;">+20 L</button>
          <button class="btn btn-secondary" style="font-size: 16px; padding: 12px 6px;">+50 L</button>
          <button class="btn btn-secondary" style="font-size: 16px; padding: 12px 6px;">+1.0 Cr</button>
          <button class="btn btn-gold" style="font-size: 16px; padding: 12px 6px;">+2.0 Cr</button>
        </div>

        <!-- Host Action Buttons -->
        <div style="display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 12px; margin-top: 4px;">
          <button class="btn btn-green">🔨 SELL BID (₹14.5 Cr)</button>
          <button class="btn btn-blue" style="font-size: 17px;">🎰 SPIN</button>
          <button class="btn btn-red" style="font-size: 17px;">❌ UNSOLD</button>
        </div>
      </div>

      <!-- Live Bidding History Log -->
      <div class="card" style="padding: 18px 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
          <span style="font-family: 'Outfit', sans-serif; font-size: 18px; font-weight: 800; color: #FFD700;">LIVE BIDDING FEED</span>
          <span class="chip chip-green">🟢 3 Teams Bidding</span>
        </div>
        <div style="display: flex; flex-direction: column; gap: 10px;">
          <div style="display: flex; justify-content: space-between; padding: 8px 14px; background: rgba(236,28,36,0.15); border-left: 4px solid #EC1C24; border-radius: 8px;">
            <span>🔴 <strong>RCB</strong> placed bid</span>
            <span style="color: #FFD700; font-weight: 800;">₹14.50 Cr</span>
            <small style="color: #8da1c2;">Just now</small>
          </div>
          <div style="display: flex; justify-content: space-between; padding: 8px 14px; background: rgba(0,75,160,0.15); border-left: 4px solid #004BA0; border-radius: 8px;">
            <span>🔵 <strong>MI</strong> placed bid</span>
            <span style="color: #FFD700; font-weight: 800;">₹14.00 Cr</span>
            <small style="color: #8da1c2;">4s ago</small>
          </div>
          <div style="display: flex; justify-content: space-between; padding: 8px 14px; background: rgba(253,185,19,0.15); border-left: 4px solid #FDB913; border-radius: 8px;">
            <span>🟡 <strong>CSK</strong> placed bid</span>
            <span style="color: #FFD700; font-weight: 800;">₹13.00 Cr</span>
            <small style="color: #8da1c2;">9s ago</small>
          </div>
        </div>
      </div>
    `
  },

  // SCREENSHOT 2: Interactive Lucky Spin Wheel
  {
    id: 2,
    filename: 'screenshot_2_spin_wheel',
    badge: '🎲 RANDOM LUCKY DRAW',
    title: 'ANIMATED LUCKY',
    titleHighlight: 'SPIN WHEEL',
    subtitle: 'Pick unsold stars or select players from mystery pools with colorful, high-stakes spin excitement.',
    activeTab: 'auction',
    content: `
      <div class="card card-gold" style="display: flex; flex-direction: column; align-items: center; text-align: center; gap: 18px; position: relative;">
        <div style="display: flex; justify-content: space-between; width: 100%;">
          <span class="chip chip-gold">🎰 LUCKY DRAW ARENA</span>
          <span class="chip chip-blue">8 Star Candidates</span>
        </div>

        <h3 style="font-family: 'Outfit', sans-serif; font-size: 26px; font-weight: 900; color: #FFF;">
          SPIN TO DRAW NEXT AUCTION PLAYER
        </h3>

        <!-- SVG Wheel Centerpiece -->
        <div style="margin: 10px 0;">
          ${getSpinWheelSvg()}
        </div>

        <!-- Selected Result Banner -->
        <div style="background: linear-gradient(135deg, rgba(27,94,32,0.3) 0%, rgba(255,215,0,0.2) 100%); border: 2px solid #00E676; border-radius: 20px; padding: 16px 28px; width: 100%; display: flex; align-items: center; justify-content: space-between;">
          <div style="text-align: left;">
            <div style="color: #00E676; font-size: 14px; font-weight: 800;">🎯 SELECTED WINNER:</div>
            <div style="font-family: 'Outfit', sans-serif; font-size: 26px; font-weight: 900; color: #FFF;">
              RASHID KHAN <span style="font-size: 16px; color: #FFD700;">(Overseas 🇦🇫)</span>
            </div>
            <div style="color: #9cb1d1; font-size: 15px;">Leg-Spin Bowler | Base: ₹2.00 Cr</div>
          </div>
          <button class="btn btn-green" style="font-size: 17px; padding: 12px 20px;">LOAD TO STAGE 🚀</button>
        </div>

        <div style="display: flex; gap: 14px; width: 100%;">
          <button class="btn btn-gold" style="flex: 2; font-size: 20px;">🎲 SPIN AGAIN (SPACEBAR)</button>
          <button class="btn btn-secondary" style="flex: 1; font-size: 18px;">CLOSE</button>
        </div>
      </div>
    `
  },

  // SCREENSHOT 3: Strategic Teams & Purse Dashboard
  {
    id: 3,
    filename: 'screenshot_3_teams_squads',
    badge: '🛡️ STRATEGIC SQUADS',
    title: 'BUDGET PURSE &',
    titleHighlight: 'SQUAD TRACKER',
    subtitle: 'Manage ₹100 Cr purse allocations, overseas limits, and team rosters with realtime progress visuals.',
    activeTab: 'list',
    subTabsHtml: `
      <div class="app-sub-tabs">
        <div class="sub-tab-item active">🛡️ Teams</div>
        <div class="sub-tab-item">👤 Players</div>
        <div class="sub-tab-item">🏆 Points</div>
        <div class="sub-tab-item">⭐ MVP</div>
      </div>
    `,
    content: `
      <!-- Teams Header Summary -->
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <h2 style="font-family: 'Outfit', sans-serif; font-size: 26px; font-weight: 900; color: #FFD700;">TEAMS & SQUADS</h2>
          <p style="color: #8da1c2; font-size: 15px;">Total Budget: ₹100.00 Cr | Max Slots: 25 | Overseas: 8</p>
        </div>
        <button class="btn btn-gold" style="font-size: 15px; padding: 10px 18px;">+ NEW TEAM</button>
      </div>

      <!-- Teams Grid -->
      <div style="display: flex; flex-direction: column; gap: 14px;">

        <!-- Team 1: RCB -->
        <div class="card" style="border-left: 6px solid #EC1C24; padding: 18px 22px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 14px;">
              <div style="width: 52px; height: 52px; border-radius: 50%; background: #EC1C24; color: #FFF; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 18px; border: 2px solid #FFD700;">RCB</div>
              <div>
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 22px; font-weight: 900; color: #FFF;">Royal Challengers Bengaluru</h3>
                <div style="display: flex; gap: 8px; margin-top: 4px;">
                  <span class="chip chip-blue" style="font-size: 13px;">Squad: 19/25</span>
                  <span class="chip chip-purple" style="font-size: 13px;">Overseas: 6/8</span>
                  <span class="chip chip-gold" style="font-size: 13px;">🔑 RCB2026</span>
                </div>
              </div>
            </div>
            <div style="text-align: right;">
              <div style="font-size: 13px; color: #8da1c2; font-weight: 700;">REMAINING PURSE</div>
              <div style="font-family: 'Outfit', sans-serif; font-size: 28px; font-weight: 900; color: #00E676;">₹32.50 Cr</div>
            </div>
          </div>
          <!-- Purse Bar -->
          <div style="width: 100%; height: 8px; background: #202b40; border-radius: 4px; margin-top: 14px; overflow: hidden;">
            <div style="width: 67.5%; height: 100%; background: linear-gradient(90deg, #EC1C24, #FFD700);"></div>
          </div>
        </div>

        <!-- Team 2: MI -->
        <div class="card" style="border-left: 6px solid #004BA0; padding: 18px 22px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 14px;">
              <div style="width: 52px; height: 52px; border-radius: 50%; background: #004BA0; color: #FFF; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 18px; border: 2px solid #00D2FF;">MI</div>
              <div>
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 22px; font-weight: 900; color: #FFF;">Mumbai Indians</h3>
                <div style="display: flex; gap: 8px; margin-top: 4px;">
                  <span class="chip chip-blue" style="font-size: 13px;">Squad: 18/25</span>
                  <span class="chip chip-purple" style="font-size: 13px;">Overseas: 5/8</span>
                  <span class="chip chip-gold" style="font-size: 13px;">🔑 MI2026</span>
                </div>
              </div>
            </div>
            <div style="text-align: right;">
              <div style="font-size: 13px; color: #8da1c2; font-weight: 700;">REMAINING PURSE</div>
              <div style="font-family: 'Outfit', sans-serif; font-size: 28px; font-weight: 900; color: #00E676;">₹41.00 Cr</div>
            </div>
          </div>
          <div style="width: 100%; height: 8px; background: #202b40; border-radius: 4px; margin-top: 14px; overflow: hidden;">
            <div style="width: 59%; height: 100%; background: linear-gradient(90deg, #004BA0, #00D2FF);"></div>
          </div>
        </div>

        <!-- Team 3: CSK -->
        <div class="card" style="border-left: 6px solid #FDB913; padding: 18px 22px;">
          <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="display: flex; align-items: center; gap: 14px;">
              <div style="width: 52px; height: 52px; border-radius: 50%; background: #FDB913; color: #000; display: flex; align-items: center; justify-content: center; font-weight: 900; font-size: 18px; border: 2px solid #FFF;">CSK</div>
              <div>
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 22px; font-weight: 900; color: #FFF;">Chennai Super Kings</h3>
                <div style="display: flex; gap: 8px; margin-top: 4px;">
                  <span class="chip chip-blue" style="font-size: 13px;">Squad: 20/25</span>
                  <span class="chip chip-purple" style="font-size: 13px;">Overseas: 7/8</span>
                  <span class="chip chip-gold" style="font-size: 13px;">🔑 CSK2026</span>
                </div>
              </div>
            </div>
            <div style="text-align: right;">
              <div style="font-size: 13px; color: #8da1c2; font-weight: 700;">REMAINING PURSE</div>
              <div style="font-family: 'Outfit', sans-serif; font-size: 28px; font-weight: 900; color: #00E676;">₹28.00 Cr</div>
            </div>
          </div>
          <div style="width: 100%; height: 8px; background: #202b40; border-radius: 4px; margin-top: 14px; overflow: hidden;">
            <div style="width: 72%; height: 100%; background: linear-gradient(90deg, #FDB913, #FF9800);"></div>
          </div>
        </div>

      </div>
    `
  },

  // SCREENSHOT 4: Multi-Device Live Cloud Sync & Roles
  {
    id: 4,
    filename: 'screenshot_4_multi_device_sync',
    badge: '📲 MULTI-DEVICE CLOUD SYNC',
    title: 'HOST & BID LIVE FROM',
    titleHighlight: 'ANY PHONE OR PC',
    subtitle: 'Admin, Team Captains & Scorers join the same live tournament seamlessly using 6-character invite codes.',
    activeTab: 'dashboard',
    content: `
      <!-- Active Hub Workspace Card -->
      <div class="card card-gold">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div>
            <span class="chip chip-gold">🏆 ACTIVE WORKSPACE</span>
            <h2 style="font-family: 'Outfit', sans-serif; font-size: 32px; font-weight: 900; color: #FFF; margin-top: 6px;">IPL 2026 MEGA AUCTION</h2>
            <p style="color: #8da1c2; font-size: 16px;">Host: Admin Auctioneer | Location: Stadium Hub</p>
          </div>
          <div class="chip chip-green" style="font-size: 16px; padding: 8px 18px;">
            🟢 REALTIME CLOUD SYNC
          </div>
        </div>
      </div>

      <!-- Join via Code Card -->
      <div class="card" style="background: linear-gradient(135deg, rgba(255,215,0,0.12) 0%, rgba(0,210,255,0.08) 100%); border: 2px dashed #FFD700; text-align: center; padding: 28px;">
        <h3 style="font-family: 'Outfit', sans-serif; font-size: 26px; font-weight: 900; color: #FFD700;">
          🔑 ENTER TOURNAMENT INVITE CODE
        </h3>
        <p style="color: #9cb1d1; font-size: 16px; margin: 8px 0 18px;">
          Enter Admin Code (ADM2026), Captain Code (RCB2026), or Scorer Code (SC8K2M)
        </p>
        <div style="display: flex; gap: 12px; max-width: 500px; margin: 0 auto;">
          <input type="text" value="RCB2026" readonly style="flex: 2; background: #0d121f; border: 2px solid #FFD700; border-radius: 14px; color: #FFF; font-family: 'Outfit', sans-serif; font-size: 22px; font-weight: 900; text-align: center; letter-spacing: 3px;">
          <button class="btn btn-gold" style="flex: 1.5; font-size: 16px;">CLAIM 🚀</button>
        </div>
      </div>

      <!-- Multi-Role Cards Matrix -->
      <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px;">
        <div class="card" style="text-align: center; padding: 18px 12px; border-color: #FFD700;">
          <div style="font-size: 36px; margin-bottom: 6px;">👑</div>
          <div style="font-family: 'Outfit', sans-serif; font-weight: 900; font-size: 18px; color: #FFD700;">ADMIN HOST</div>
          <p style="font-size: 13px; color: #8da1c2; margin-top: 4px;">Presents stage, controls timer & sells bids</p>
        </div>
        <div class="card" style="text-align: center; padding: 18px 12px; border-color: #00D2FF;">
          <div style="font-size: 36px; margin-bottom: 6px;">🛡️</div>
          <div style="font-family: 'Outfit', sans-serif; font-weight: 900; font-size: 18px; color: #00D2FF;">TEAM CAPTAINS</div>
          <p style="font-size: 13px; color: #8da1c2; margin-top: 4px;">Place live bids & monitor remaining purse</p>
        </div>
        <div class="card" style="text-align: center; padding: 18px 12px; border-color: #00E676;">
          <div style="font-size: 36px; margin-bottom: 6px;">📊</div>
          <div style="font-family: 'Outfit', sans-serif; font-weight: 900; font-size: 18px; color: #00E676;">LIVE SCORER</div>
          <p style="font-size: 13px; color: #8da1c2; margin-top: 4px;">Record ball-by-ball matches & auto-NRR</p>
        </div>
      </div>
    `
  },

  // SCREENSHOT 5: Player Roster & Custom Pools
  {
    id: 5,
    filename: 'screenshot_5_player_roster',
    badge: '👥 PLAYER ROSTER & SETS',
    title: 'CUSTOM AUCTION POOLS &',
    titleHighlight: 'PLAYER PHOTOS',
    subtitle: 'Filter by Batsmen, Bowlers, All-rounders, organize fixed sets, and inspect career stats effortlessly.',
    activeTab: 'list',
    subTabsHtml: `
      <div class="app-sub-tabs">
        <div class="sub-tab-item">🛡️ Teams</div>
        <div class="sub-tab-item active">👤 Players</div>
        <div class="sub-tab-item">🏆 Points</div>
        <div class="sub-tab-item">⭐ MVP</div>
      </div>
    `,
    content: `
      <!-- Search & Filter Controls -->
      <div style="display: flex; gap: 10px;">
        <div style="flex: 1; background: #151d2f; border: 1px solid rgba(255,255,255,0.1); border-radius: 14px; padding: 14px 20px; display: flex; align-items: center; gap: 10px; color: #8da1c2; font-size: 17px;">
          <span>🔍</span> <span>Search 150+ auction players...</span>
        </div>
        <button class="btn btn-gold" style="font-size: 15px; padding: 10px 20px;">+ ADD PLAYER</button>
      </div>

      <!-- Category Filter Pills -->
      <div style="display: flex; gap: 8px; overflow-x: auto;">
        <span class="chip chip-gold">All Players (150)</span>
        <span class="chip chip-blue">🏏 Batsmen (45)</span>
        <span class="chip chip-purple">⚡ Bowlers (42)</span>
        <span class="chip chip-green">⭐ All-Rounders (35)</span>
      </div>

      <!-- Player List -->
      <div style="display: flex; flex-direction: column; gap: 12px;">

        <!-- Player 1 -->
        <div class="card" style="display: flex; align-items: center; justify-content: space-between; padding: 16px 20px;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div style="width: 60px; height: 60px; border-radius: 50%; overflow: hidden; border: 2px solid #00E676;">
              ${getPlayerAvatar('#004BA0', 93, '⚡')}
            </div>
            <div>
              <div style="display: flex; gap: 8px; align-items: center;">
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 800; color: #FFF;">Jasprit Bumrah</h3>
                <span class="chip chip-blue" style="font-size: 12px; padding: 2px 8px;">India 🇮🇳</span>
              </div>
              <div style="color: #8da1c2; font-size: 14px; margin-top: 2px;">Fast Bowler | Set 1 | Base: ₹2.00 Cr</div>
            </div>
          </div>
          <div style="text-align: right;">
            <span class="chip chip-green" style="font-size: 14px;">SOLD TO MI</span>
            <div style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 900; color: #FFD700; margin-top: 4px;">₹18.00 Cr</div>
          </div>
        </div>

        <!-- Player 2 -->
        <div class="card" style="display: flex; align-items: center; justify-content: space-between; padding: 16px 20px;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div style="width: 60px; height: 60px; border-radius: 50%; overflow: hidden; border: 2px solid #FFD700;">
              ${getPlayerAvatar('#FF5722', 45, '🧤')}
            </div>
            <div>
              <div style="display: flex; gap: 8px; align-items: center;">
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 800; color: #FFF;">Heinrich Klaasen</h3>
                <span class="chip chip-purple" style="font-size: 12px; padding: 2px 8px;">S. Africa 🇿🇦</span>
              </div>
              <div style="color: #8da1c2; font-size: 14px; margin-top: 2px;">Wicket-Keeper Batsman | Set 2</div>
            </div>
          </div>
          <div style="text-align: right;">
            <span class="chip chip-green" style="font-size: 14px;">SOLD TO SRH</span>
            <div style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 900; color: #FFD700; margin-top: 4px;">₹15.50 Cr</div>
          </div>
        </div>

        <!-- Player 3 -->
        <div class="card" style="display: flex; align-items: center; justify-content: space-between; padding: 16px 20px;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div style="width: 60px; height: 60px; border-radius: 50%; overflow: hidden; border: 2px solid #FFD700;">
              ${getPlayerAvatar('#EC1C24', 62, '🏏')}
            </div>
            <div>
              <div style="display: flex; gap: 8px; align-items: center;">
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 800; color: #FFF;">Travis Head</h3>
                <span class="chip chip-purple" style="font-size: 12px; padding: 2px 8px;">Australia 🇦🇺</span>
              </div>
              <div style="color: #8da1c2; font-size: 14px; margin-top: 2px;">All-Rounder | Set 1</div>
            </div>
          </div>
          <div style="text-align: right;">
            <span class="chip chip-gold" style="font-size: 14px;">UP NEXT</span>
            <div style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 900; color: #FFD700; margin-top: 4px;">₹2.00 Cr Base</div>
          </div>
        </div>

      </div>
    `
  },

  // SCREENSHOT 6: Live Ball-by-Ball Match Scorer
  {
    id: 6,
    filename: 'screenshot_6_live_scorer',
    badge: '🏏 LIVE MATCH SCORER',
    title: 'BALL-BY-BALL SCORING &',
    titleHighlight: 'INSTANT NRR',
    subtitle: 'Score local & tournament matches with solo batsman rules, live overs tracker & auto NRR recalculation.',
    activeTab: 'scorer',
    content: `
      <!-- Match Live Hero Scorecard -->
      <div class="card card-gold" style="padding: 24px;">
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 10px;">
            <span class="chip chip-red" style="font-size: 14px;">🔴 LIVE MATCH 7</span>
            <span style="color: #9cb1d1; font-size: 15px;">Wankhede Stadium</span>
          </div>
          <span class="chip chip-blue" style="font-size: 14px;">⚡ Solo Batsman Rule ON</span>
        </div>

        <div style="display: flex; justify-content: space-between; align-items: flex-end; margin-top: 16px;">
          <div>
            <div style="font-family: 'Outfit', sans-serif; font-size: 26px; font-weight: 900; color: #FFF;">
              🔴 RCB <span style="font-size: 20px; color: #8da1c2;">vs</span> 🟡 CSK
            </div>
            <div style="font-family: 'Outfit', sans-serif; font-size: 64px; font-weight: 900; color: #FFD700; line-height: 1; margin: 8px 0;">
              184 / 4
            </div>
            <div style="font-size: 20px; color: #9cb1d1; font-weight: 700;">(17.4 / 20.0 Overs) • CRR: 10.41</div>
          </div>
          <div style="text-align: right; background: rgba(0,0,0,0.3); padding: 14px 20px; border-radius: 16px; border: 1px solid rgba(255,215,0,0.3);">
            <div style="font-size: 14px; color: #8da1c2; font-weight: 700;">CSK TARGET</div>
            <div style="font-family: 'Outfit', sans-serif; font-size: 28px; font-weight: 900; color: #00E676;">185 RUNS</div>
            <div style="font-size: 14px; color: #FFD700; margin-top: 2px;">Need 34 runs in 14 balls</div>
          </div>
        </div>

        <!-- Batsmen & Bowler Grid -->
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 20px; padding-top: 16px; border-top: 1px solid rgba(255,255,255,0.1);">
          <div>
            <div style="font-size: 13px; color: #8da1c2; font-weight: 700;">BATTING CREASE</div>
            <div style="font-size: 17px; font-weight: 800; color: #FFF; margin-top: 4px;">V. Kohli* <span style="color:#FFD700;">88 (51b, 8x4, 4x6)</span></div>
            <div style="font-size: 15px; color: #9cb1d1;">D. Karthik 24 (11b, 2x4, 2x6)</div>
          </div>
          <div style="text-align: right;">
            <div style="font-size: 13px; color: #8da1c2; font-weight: 700;">CURRENT BOWLER</div>
            <div style="font-size: 17px; font-weight: 800; color: #FFF; margin-top: 4px;">M. Pathirana</div>
            <div style="font-size: 15px; color: #9cb1d1;">3.4-0-34-2 • Econ: 9.27</div>
          </div>
        </div>
      </div>

      <!-- Scorer Keypad -->
      <div class="card" style="padding: 20px;">
        <div style="font-size: 15px; font-weight: 800; color: #FFD700; margin-bottom: 12px;">BALL-BY-BALL RECORD KEYPAD</div>
        <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;">
          <button class="btn btn-secondary">0</button>
          <button class="btn btn-secondary">1</button>
          <button class="btn btn-secondary">2</button>
          <button class="btn btn-secondary">3</button>
          <button class="btn btn-gold" style="font-size: 24px;">4 🎯</button>
          <button class="btn btn-gold" style="font-size: 24px;">6 🚀</button>
          <button class="btn btn-red" style="font-size: 18px;">OUT! ❌</button>
          <button class="btn btn-blue" style="font-size: 18px;">UNDO ↩️</button>
        </div>
        <button class="btn btn-secondary" style="width: 100%; margin-top: 14px; font-size: 16px; border-color: #00E676; color: #00E676;">
          ⚡ DIRECT END MATCH WITH AUTO NRR CALCULATION
        </button>
      </div>
    `
  },

  // SCREENSHOT 7: Automated Points Table & NRR
  {
    id: 7,
    filename: 'screenshot_7_points_table',
    badge: '🏆 AUTOMATED STANDINGS',
    title: 'LIVE POINTS TABLE &',
    titleHighlight: 'NET RUN RATE (NRR)',
    subtitle: 'Standings, wins, losses and official Net Run Rate update automatically from match scorecards.',
    activeTab: 'list',
    subTabsHtml: `
      <div class="app-sub-tabs">
        <div class="sub-tab-item">🛡️ Teams</div>
        <div class="sub-tab-item">👤 Players</div>
        <div class="sub-tab-item active">🏆 Points</div>
        <div class="sub-tab-item">⭐ MVP</div>
      </div>
    `,
    content: `
      <div class="card card-gold">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px;">
          <div>
            <h2 style="font-family: 'Outfit', sans-serif; font-size: 26px; font-weight: 900; color: #FFD700;">TOURNAMENT STANDINGS</h2>
            <p style="color: #8da1c2; font-size: 15px;">Auto-calculated from completed match records</p>
          </div>
          <span class="chip chip-green">🟢 TOP 4 QUALIFY</span>
        </div>

        <!-- Standings Table -->
        <table style="width: 100%; border-collapse: collapse; text-align: left; font-size: 16px;">
          <thead>
            <tr style="border-bottom: 2px solid rgba(255,255,255,0.15); color: #8da1c2; font-weight: 800;">
              <th style="padding: 12px 6px;">#</th>
              <th style="padding: 12px 6px;">TEAM</th>
              <th style="padding: 12px 6px; text-align: center;">P</th>
              <th style="padding: 12px 6px; text-align: center;">W</th>
              <th style="padding: 12px 6px; text-align: center;">L</th>
              <th style="padding: 12px 6px; text-align: center; color: #FFD700;">PTS</th>
              <th style="padding: 12px 6px; text-align: right; color: #00D2FF;">NRR</th>
            </tr>
          </thead>
          <tbody>
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.06); background: rgba(0,230,118,0.08);">
              <td style="padding: 14px 6px; font-weight: 900; color: #00E676;">1</td>
              <td style="padding: 14px 6px; font-weight: 800; color: #FFF;">🔴 RCB</td>
              <td style="padding: 14px 6px; text-align: center;">14</td>
              <td style="padding: 14px 6px; text-align: center;">11</td>
              <td style="padding: 14px 6px; text-align: center;">3</td>
              <td style="padding: 14px 6px; text-align: center; font-weight: 900; color: #FFD700; font-size: 20px;">22</td>
              <td style="padding: 14px 6px; text-align: right; font-weight: 800; color: #00E676;">+1.240</td>
            </tr>
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.06); background: rgba(0,230,118,0.08);">
              <td style="padding: 14px 6px; font-weight: 900; color: #00E676;">2</td>
              <td style="padding: 14px 6px; font-weight: 800; color: #FFF;">🟣 KKR</td>
              <td style="padding: 14px 6px; text-align: center;">14</td>
              <td style="padding: 14px 6px; text-align: center;">10</td>
              <td style="padding: 14px 6px; text-align: center;">4</td>
              <td style="padding: 14px 6px; text-align: center; font-weight: 900; color: #FFD700; font-size: 20px;">20</td>
              <td style="padding: 14px 6px; text-align: right; font-weight: 800; color: #00E676;">+0.985</td>
            </tr>
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.06); background: rgba(0,230,118,0.08);">
              <td style="padding: 14px 6px; font-weight: 900; color: #00E676;">3</td>
              <td style="padding: 14px 6px; font-weight: 800; color: #FFF;">🟡 CSK</td>
              <td style="padding: 14px 6px; text-align: center;">14</td>
              <td style="padding: 14px 6px; text-align: center;">9</td>
              <td style="padding: 14px 6px; text-align: center;">5</td>
              <td style="padding: 14px 6px; text-align: center; font-weight: 900; color: #FFD700; font-size: 20px;">18</td>
              <td style="padding: 14px 6px; text-align: right; font-weight: 800; color: #00E676;">+0.420</td>
            </tr>
            <tr style="border-bottom: 2px dashed #00E676; background: rgba(0,230,118,0.08);">
              <td style="padding: 14px 6px; font-weight: 900; color: #00E676;">4</td>
              <td style="padding: 14px 6px; font-weight: 800; color: #FFF;">🔵 MI</td>
              <td style="padding: 14px 6px; text-align: center;">14</td>
              <td style="padding: 14px 6px; text-align: center;">8</td>
              <td style="padding: 14px 6px; text-align: center;">6</td>
              <td style="padding: 14px 6px; text-align: center; font-weight: 900; color: #FFD700; font-size: 20px;">16</td>
              <td style="padding: 14px 6px; text-align: right; font-weight: 800; color: #00E676;">+0.180</td>
            </tr>
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.06);">
              <td style="padding: 14px 6px; font-weight: 900; color: #8da1c2;">5</td>
              <td style="padding: 14px 6px; font-weight: 800; color: #FFF;">🟠 SRH</td>
              <td style="padding: 14px 6px; text-align: center;">14</td>
              <td style="padding: 14px 6px; text-align: center;">7</td>
              <td style="padding: 14px 6px; text-align: center;">7</td>
              <td style="padding: 14px 6px; text-align: center; font-weight: 900; color: #8da1c2; font-size: 20px;">14</td>
              <td style="padding: 14px 6px; text-align: right; font-weight: 800; color: #FF1744;">-0.210</td>
            </tr>
          </tbody>
        </table>
      </div>
    `
  },

  // SCREENSHOT 8: CricHeroes MVP & Cap Leaderboard
  {
    id: 8,
    filename: 'screenshot_8_mvp_leaderboard',
    badge: '⭐ CRICHEROES MVP STATS',
    title: 'MVP RANKINGS, ORANGE &',
    titleHighlight: 'PURPLE CAP LEADERS',
    subtitle: 'Comprehensive CricHeroes points formula evaluating runs, strike rates, wickets, dot balls & catches.',
    activeTab: 'list',
    subTabsHtml: `
      <div class="app-sub-tabs">
        <div class="sub-tab-item">🛡️ Teams</div>
        <div class="sub-tab-item">👤 Players</div>
        <div class="sub-tab-item">🏆 Points</div>
        <div class="sub-tab-item active">⭐ MVP</div>
      </div>
    `,
    content: `
      <!-- Category Filter Pills -->
      <div style="display: flex; gap: 10px;">
        <button class="btn btn-gold" style="flex: 1; font-size: 16px; padding: 12px 10px;">👑 OVERALL MVP</button>
        <button class="btn btn-secondary" style="flex: 1; font-size: 16px; padding: 12px 10px; border-color: #FFA000; color: #FFA000;">🧡 ORANGE CAP</button>
        <button class="btn btn-secondary" style="flex: 1; font-size: 16px; padding: 12px 10px; border-color: #BA68C8; color: #BA68C8;">💜 PURPLE CAP</button>
      </div>

      <!-- MVP Leaderboard Cards -->
      <div style="display: flex; flex-direction: column; gap: 12px;">

        <!-- Rank 1: MVP Leader -->
        <div class="card card-gold" style="display: flex; align-items: center; justify-content: space-between; padding: 18px 22px; border: 2px solid #FFD700;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div style="font-family: 'Outfit', sans-serif; font-size: 28px; font-weight: 900; color: #FFD700;">#1</div>
            <div style="width: 64px; height: 64px; border-radius: 50%; overflow: hidden; border: 2px solid #FFD700;">
              ${getPlayerAvatar('#3A225D', 74, '⭐')}
            </div>
            <div>
              <div style="display: flex; gap: 8px; align-items: center;">
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 22px; font-weight: 900; color: #FFF;">Sunil Narine</h3>
                <span class="chip chip-purple" style="font-size: 12px; padding: 2px 8px;">KKR</span>
              </div>
              <div style="color: #8da1c2; font-size: 14px; margin-top: 2px;">488 Runs • 17 Wickets • 14 Catches</div>
            </div>
          </div>
          <div style="text-align: right;">
            <span class="chip chip-gold" style="font-size: 13px;">⭐ MVP LEADER</span>
            <div style="font-family: 'Outfit', sans-serif; font-size: 28px; font-weight: 900; color: #FFD700; margin-top: 4px;">628.5 PTS</div>
          </div>
        </div>

        <!-- Rank 2: Orange Cap Leader -->
        <div class="card" style="display: flex; align-items: center; justify-content: space-between; padding: 16px 20px;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div style="font-family: 'Outfit', sans-serif; font-size: 24px; font-weight: 900; color: #C0C0C0;">#2</div>
            <div style="width: 58px; height: 58px; border-radius: 50%; overflow: hidden; border: 2px solid #FFA000;">
              ${getPlayerAvatar('#EC1C24', 18, '🏏')}
            </div>
            <div>
              <div style="display: flex; gap: 8px; align-items: center;">
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 800; color: #FFF;">Virat Kohli</h3>
                <span class="chip chip-gold" style="font-size: 12px; padding: 2px 8px;">RCB</span>
              </div>
              <div style="color: #FFA000; font-size: 14px; margin-top: 2px; font-weight: 700;">🧡 Orange Cap: 741 Runs (Avg 61.8)</div>
            </div>
          </div>
          <div style="text-align: right;">
            <div style="font-family: 'Outfit', sans-serif; font-size: 24px; font-weight: 900; color: #FFF;">592.0 PTS</div>
          </div>
        </div>

        <!-- Rank 3: Purple Cap Leader -->
        <div class="card" style="display: flex; align-items: center; justify-content: space-between; padding: 16px 20px;">
          <div style="display: flex; align-items: center; gap: 16px;">
            <div style="font-family: 'Outfit', sans-serif; font-size: 24px; font-weight: 900; color: #CD7F32;">#3</div>
            <div style="width: 58px; height: 58px; border-radius: 50%; overflow: hidden; border: 2px solid #BA68C8;">
              ${getPlayerAvatar('#004BA0', 93, '⚡')}
            </div>
            <div>
              <div style="display: flex; gap: 8px; align-items: center;">
                <h3 style="font-family: 'Outfit', sans-serif; font-size: 20px; font-weight: 800; color: #FFF;">Jasprit Bumrah</h3>
                <span class="chip chip-blue" style="font-size: 12px; padding: 2px 8px;">MI</span>
              </div>
              <div style="color: #BA68C8; font-size: 14px; margin-top: 2px; font-weight: 700;">💜 Purple Cap: 20 Wickets (Econ 6.4)</div>
            </div>
          </div>
          <div style="text-align: right;">
            <div style="font-family: 'Outfit', sans-serif; font-size: 24px; font-weight: 900; color: #FFF;">565.0 PTS</div>
          </div>
        </div>

      </div>
    `
  }
];

// GENERATE HTML AND SCREENSHOTS
async function generateAll() {
  console.log(`Starting generation of ${screenshots.length} Play Store screenshots...`);

  for (const s of screenshots) {
    const htmlContent = wrapInPlayStoreMockup({
      badge: s.badge,
      title: s.title,
      titleHighlight: s.titleHighlight,
      subtitle: s.subtitle,
      activeTab: s.activeTab,
      subTabsHtml: s.subTabsHtml,
      appContentHtml: s.content
    });

    const htmlPath = path.join(outputDir, `${s.filename}.html`);
    const pngPath = path.join(outputDir, `${s.filename}.png`);

    fs.writeFileSync(htmlPath, htmlContent, 'utf8');

    console.log(`[${s.id}/8] Rendering: ${s.filename}.png (1080x2280)...`);
    const cmd = `"${edgePath}" --headless=new --disable-gpu --hide-scrollbars --screenshot="${pngPath}" --window-size=1080,2280 "file:///${htmlPath.replace(/\\\\/g, '/')}"`;
    
    try {
      execSync(cmd, { stdio: 'pipe' });
      console.log(`✔ Generated: ${s.filename}.png`);
    } catch (e) {
      console.error(`✖ Failed to render ${s.filename}:`, e.message);
    }
  }

  console.log("\nAll 8 Play Store screenshots successfully generated in assets/play_store_screenshots/!");
}

generateAll();
