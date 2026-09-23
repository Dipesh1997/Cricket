/* ==========================================================================
   IPL CRICKET HUB & LIVE AUCTION ARENA - JAVASCRIPT ENGINE
   ========================================================================== */

// --- FIREBASE CONFIGURATION ---
const firebaseConfig = {
  databaseURL: "https://cricket-auction-app-24750-default-rtdb.firebaseio.com",
  projectId: "cricket-auction-app-24750",
  storageBucket: "cricket-auction-app-24750.firebasestorage.app"
};

// Initialize Firebase App & Realtime Database
firebase.initializeApp(firebaseConfig);
const db = firebase.database();

// --- STATE STORE ---
let state = {
  tournaments: [],
  teams: [],
  players: [],
  bids: [],
  matches: [],
  ballRecords: [],
  activeTournamentId: "t1",
  activePlayerId: null,
  activeMatchId: null,
  currentUser: {
    role: "ADMIN_AUCTIONEER", // ADMIN_AUCTIONEER, TEAM_CAPTAIN, SCORER, SPECTATOR
    assignedTeamId: null
  },
  timerSeconds: 15
};

// --- INITIALIZATION & REALTIME DB LISTENERS ---
document.addEventListener("DOMContentLoaded", () => {
  setupNavigation();
  setupRoleSelector();
  listenToFirebase();
  startBidTimer();
});

function listenToFirebase() {
  const dbRef = db.ref("users/default_user/database_schema");
  dbRef.on("value", (snapshot) => {
    const data = snapshot.val();
    if (data) {
      state.tournaments = data.tournaments || [];
      state.teams = data.teams || [];
      state.players = data.players || [];
      state.bids = data.bids || [];
      state.matches = data.matches || [];
      state.ballRecords = data.ballRecords || [];
      state.activeTournamentId = data.activeTournamentId || (state.tournaments[0] ? state.tournaments[0].id : "t1");
      state.activePlayerId = data.activePlayerId || (state.players[0] ? state.players[0].id : null);
      state.activeMatchId = data.activeMatchId || (state.matches[0] ? state.matches[0].id : null);
      renderAll();
    } else {
      seedDefaultData();
    }
  });
}

function saveToFirebase() {
  const dbRef = db.ref("users/default_user/database_schema");
  const payload = {
    tournaments: state.tournaments,
    teams: state.teams,
    players: state.players,
    bids: state.bids,
    matches: state.matches,
    ballRecords: state.ballRecords,
    activeTournamentId: state.activeTournamentId,
    activePlayerId: state.activePlayerId,
    activeMatchId: state.activeMatchId
  };
  dbRef.set(payload);
}

function seedDefaultData() {
  state.tournaments = [
    { id: "t1", name: "IPL 2026 Mega Auction", defaultPurse: 100.0, maxSlots: 25, maxOverseas: 8, scorerInviteCode: "SC8K2M" }
  ];
  state.teams = [
    { id: "team_rcb", tournamentId: "t1", name: "Royal Challengers Bengaluru", shortCode: "RCB", primaryColorHex: "#EC1C24", totalPurse: 100.0, spentPurse: 0.0, inviteCode: "RCB2026" },
    { id: "team_csk", tournamentId: "t1", name: "Chennai Super Kings", shortCode: "CSK", primaryColorHex: "#FFD700", totalPurse: 100.0, spentPurse: 0.0, inviteCode: "CSK2026" },
    { id: "team_mi", tournamentId: "t1", name: "Mumbai Indians", shortCode: "MI", primaryColorHex: "#004BA0", totalPurse: 100.0, spentPurse: 0.0, inviteCode: "MI2026" }
  ];
  state.players = [
    { id: "p1", tournamentId: "t1", name: "Virat Kohli", role: "BATSMAN", country: "India", isOverseas: false, basePrice: 2.0, currentBid: 2.0, highestBidderTeamId: null, status: "UP_NEXT", imageUrl: "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=500&auto=format&fit=crop&q=60", stats: "IPL Matches: 237 | Runs: 7263 | SR: 130.0", setName: "Set 1", isFixedSet: true },
    { id: "p2", tournamentId: "t1", name: "MS Dhoni", role: "WICKET_KEEPER", country: "India", isOverseas: false, basePrice: 2.0, currentBid: 2.0, highestBidderTeamId: null, status: "UP_NEXT", imageUrl: "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=500&auto=format&fit=crop&q=60", stats: "IPL Matches: 250 | Runs: 5082 | SR: 135.9", setName: "Set 1", isFixedSet: true },
    { id: "p3", tournamentId: "t1", name: "Pat Cummins", role: "ALL_ROUNDER", country: "Australia", isOverseas: true, basePrice: 2.0, currentBid: 2.0, highestBidderTeamId: null, status: "UP_NEXT", imageUrl: "https://images.unsplash.com/photo-1508801932684-8777e504c356?w=500&auto=format&fit=crop&q=60", stats: "IPL Wickets: 60 | Runs: 520", setName: "Set 1", isFixedSet: false }
  ];
  state.activeTournamentId = "t1";
  state.activePlayerId = "p1";
  saveToFirebase();
}

// --- RENDER ROUTINER ---
function renderAll() {
  renderHeaderRole();
  renderTournaments();
  renderAuctionArena();
  renderTeams();
  renderPlayers();
  renderLiveScorer();
  renderPointsTable();
}

// --- NAVIGATION TABS ---
function setupNavigation() {
  const buttons = document.querySelectorAll(".nav-btn");
  buttons.forEach(btn => {
    btn.addEventListener("click", () => {
      buttons.forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      const target = btn.getAttribute("data-tab");
      document.querySelectorAll(".tab-pane").forEach(pane => pane.classList.remove("active"));
      document.getElementById(target).classList.add("active");
    });
  });
}

function setupRoleSelector() {
  const select = document.getElementById("userRoleSelect");
  select.addEventListener("change", (e) => {
    state.currentUser.role = e.target.value;
    state.currentUser.assignedTeamId = null;
    renderAll();
    showToast("Role switched to: " + e.target.options[e.target.selectedIndex].text);
  });
}

function renderHeaderRole() {
  const badge = document.getElementById("currentRoleBadge");
  const role = state.currentUser.role;
  let text = "👑 Admin Auctioneer";
  if (role === "TEAM_CAPTAIN") {
    const team = state.teams.find(t => t.id === state.currentUser.assignedTeamId);
    text = team ? `🛡️ Captain (${team.shortCode})` : "🛡️ Team Captain";
  } else if (role === "SCORER") {
    text = "📊 Live Scorer";
  } else if (role === "SPECTATOR") {
    text = "👀 Spectator";
  }
  badge.innerHTML = `<span>${text}</span>`;
}

// --- AUCTION ARENA ENGINE ---
function renderAuctionArena() {
  const player = state.players.find(p => p.id === state.activePlayerId);
  if (!player) return;

  document.getElementById("stagePlayerName").innerText = player.name;
  document.getElementById("stageSetTag").innerText = player.setName || "Set 1";
  document.getElementById("stageCountryTag").innerText = player.isOverseas ? `Overseas (${player.country})` : player.country;
  document.getElementById("stageRoleTag").innerText = player.role;
  document.getElementById("stageBasePriceTag").innerText = `Base: ₹${player.basePrice.toFixed(2)} Cr`;
  document.getElementById("stageStatsText").innerText = player.stats || "No career stats recorded";
  document.getElementById("stagePlayerImg").src = getDirectDriveUrl(player.imageUrl);

  const fixedChip = document.getElementById("stageFixedSetChip");
  fixedChip.innerText = player.isFixedSet ? "⭐ Fixed Set" : "+ Fix Set";
  fixedChip.style.backgroundColor = player.isFixedSet ? "var(--color-gold)" : "var(--bg-surface)";
  fixedChip.style.color = player.isFixedSet ? "#000" : "var(--color-text-main)";

  const currentBidEl = document.getElementById("stageCurrentBid");
  currentBidEl.innerText = `₹${(player.currentBid || player.basePrice).toFixed(2)} Cr`;
  currentBidEl.style.color = player.status === "SOLD" ? "var(--color-green)" : "var(--color-gold)";

  const bidderBadge = document.getElementById("stageHighestBidderBadge");
  const highestTeam = state.teams.find(t => t.id === player.highestBidderTeamId);
  if (highestTeam) {
    bidderBadge.style.display = "inline-flex";
    document.getElementById("bidderAvatar").innerText = highestTeam.shortCode;
    document.getElementById("bidderAvatar").style.backgroundColor = highestTeam.primaryColorHex || "var(--color-gold)";
    document.getElementById("bidderTeamName").innerText = highestTeam.name;
    document.getElementById("bidderTeamPurse").innerText = `Purse: ₹${(highestTeam.totalPurse - highestTeam.spentPurse).toFixed(2)} Cr`;
  } else {
    bidderBadge.style.display = "none";
  }

  // Bidding Team Lock Indicator
  const lockLabel = document.getElementById("biddingTeamLockedLabel");
  if (state.currentUser.role === "TEAM_CAPTAIN" && state.currentUser.assignedTeamId) {
    const assignedTeam = state.teams.find(t => t.id === state.currentUser.assignedTeamId);
    lockLabel.innerText = `🔒 ${assignedTeam ? assignedTeam.name : "My Team"}`;
  } else {
    lockLabel.innerText = "⚡ Open Bidding";
  }

  // Admin Controls Visibility
  const adminControls = document.getElementById("adminAuctioneerControls");
  adminControls.style.display = state.currentUser.role === "ADMIN_AUCTIONEER" ? "flex" : "none";

  renderBidsLog(player.id);
}

function renderBidsLog(playerId) {
  const container = document.getElementById("bidsLogContainer");
  const playerBids = state.bids.filter(b => b.playerId === playerId).reverse();
  if (playerBids.length === 0) {
    container.innerHTML = `<p style="color: var(--color-text-sub); font-size: 0.85rem;">No bids placed for this player yet</p>`;
    return;
  }
  container.innerHTML = playerBids.map(b => `
    <div style="display: flex; justify-content: space-between; background: var(--bg-surface); padding: 8px 12px; border-radius: 8px;">
      <span style="font-weight: 700;">${b.teamName}</span>
      <span style="color: var(--color-gold); font-weight: 900;">₹${b.amount.toFixed(2)} Cr</span>
    </div>
  `).join("");
}

function placeBid(increment) {
  const player = state.players.find(p => p.id === state.activePlayerId);
  if (!player || player.status === "SOLD") return;

  let teamId = null;
  if (state.currentUser.role === "TEAM_CAPTAIN" && state.currentUser.assignedTeamId) {
    teamId = state.currentUser.assignedTeamId;
  } else if (state.teams.length > 0) {
    teamId = state.teams[0].id;
  }
  if (!teamId) { showToast("No team available to bid"); return; }

  const team = state.teams.find(t => t.id === teamId);
  const newBidVal = (player.currentBid || player.basePrice) + increment;
  if ((team.totalPurse - team.spentPurse) < newBidVal) {
    showToast("Insufficient purse budget!");
    return;
  }

  player.currentBid = newBidVal;
  player.highestBidderTeamId = team.id;
  player.status = "BIDDING";

  state.bids.push({
    id: "b_" + Date.now(),
    playerId: player.id,
    teamId: team.id,
    teamName: team.name,
    amount: newBidVal,
    timestamp: Date.now()
  });

  state.timerSeconds = 15;
  saveToFirebase();
  showToast(`🚀 ${team.shortCode} bid ₹${newBidVal.toFixed(2)} Cr!`);
}

function sellActivePlayer() {
  const player = state.players.find(p => p.id === state.activePlayerId);
  if (!player || !player.highestBidderTeamId) return;

  const team = state.teams.find(t => t.id === player.highestBidderTeamId);
  player.status = "SOLD";
  player.soldPrice = player.currentBid;
  player.soldToTeamId = team.id;
  team.spentPurse += player.currentBid;

  saveToFirebase();
  showToast(`🎉 ${player.name} SOLD to ${team.name} for ₹${player.currentBid.toFixed(2)} Cr!`);
}

function sellAtBasePrice() {
  const player = state.players.find(p => p.id === state.activePlayerId);
  if (!player || state.teams.length === 0) return;
  const team = state.teams[0];
  player.status = "SOLD";
  player.soldPrice = player.basePrice;
  player.soldToTeamId = team.id;
  team.spentPurse += player.basePrice;
  saveToFirebase();
  showToast(`🎉 ${player.name} SOLD at Base Price to ${team.name}!`);
}

function markUnsold() {
  const player = state.players.find(p => p.id === state.activePlayerId);
  if (!player) return;
  player.status = "UNSOLD";
  saveToFirebase();
  showToast(`❌ ${player.name} Marked UNSOLD`);
}

function toggleFixedSet() {
  if (state.currentUser.role !== "ADMIN_AUCTIONEER") return;
  const player = state.players.find(p => p.id === state.activePlayerId);
  if (player) {
    player.isFixedSet = !player.isFixedSet;
    saveToFirebase();
  }
}

function toggleFullscreenStage() {
  const stage = document.getElementById("stageContainer");
  if (!document.fullscreenElement) {
    stage.requestFullscreen().catch(err => {
      stage.classList.toggle("fullscreen-active");
    });
  } else {
    document.exitFullscreen();
  }
}

function startBidTimer() {
  setInterval(() => {
    if (state.timerSeconds > 0) {
      state.timerSeconds--;
      const el = document.getElementById("timerVal");
      if (el) el.innerText = state.timerSeconds;
    }
  }, 1000);
}

// --- TEAMS & SQUADS ENGINE ---
function renderTeams() {
  const container = document.getElementById("teamsGridList");
  container.innerHTML = state.teams.map(t => {
    const remaining = t.totalPurse - t.spentPurse;
    const spentPct = Math.min(100, (t.spentPurse / t.totalPurse) * 100);
    return `
      <div class="team-card">
        <div class="team-header">
          <div style="display: flex; align-items: center; gap: 10px;">
            <div class="team-avatar" style="background-color: ${t.primaryColorHex || '#FFD700'}">${t.shortCode}</div>
            <div>
              <div style="font-weight: 800; color: #FFF;">${t.name}</div>
              <div class="invite-badge" onclick="copyText('${t.inviteCode}', 'Team Code')">
                <span>🔑 CODE: ${t.inviteCode}</span> 📋
              </div>
            </div>
          </div>
          <div class="chip">Purse: ₹${remaining.toFixed(2)} Cr</div>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" style="width: ${spentPct}%;"></div>
        </div>
      </div>
    `;
  }).join("");
}

// --- PLAYER ROSTER ENGINE ---
function renderPlayers() {
  const container = document.getElementById("playersTableContainer");
  container.innerHTML = `
    <table>
      <thead>
        <tr>
          <th>Player</th>
          <th>Role</th>
          <th>Country</th>
          <th>Base Price</th>
          <th>Status</th>
          <th>Set</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        ${state.players.map(p => `
          <tr>
            <td><strong>${p.name}</strong></td>
            <td>${p.role}</td>
            <td>${p.country}</td>
            <td>₹${p.basePrice.toFixed(2)} Cr</td>
            <td><span class="chip" style="color: ${p.status === 'SOLD' ? 'var(--color-green)' : 'var(--color-gold)'}">${p.status}</span></td>
            <td>${p.setName || 'Set 1'} ${p.isFixedSet ? '⭐' : ''}</td>
            <td><button class="btn btn-sm btn-secondary" onclick="selectActivePlayer('${p.id}')">Stage Player</button></td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

function selectActivePlayer(playerId) {
  state.activePlayerId = playerId;
  state.timerSeconds = 15;
  saveToFirebase();
  document.querySelector('[data-tab="tab-auction"]').click();
  showToast("Player selected on Auction Stage!");
}

// --- LIVE SCORER ENGINE ---
function renderLiveScorer() {
  const activeMatch = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!activeMatch) {
    document.getElementById("scorerScoreText").innerText = "0 / 0";
    return;
  }
  const teamA = state.teams.find(t => t.id === activeMatch.teamAId);
  const teamB = state.teams.find(t => t.id === activeMatch.teamBId);
  document.getElementById("scorerBattingTeamName").innerText = `${teamA ? teamA.shortCode : 'Team A'} vs ${teamB ? teamB.shortCode : 'Team B'}`;
  document.getElementById("scorerScoreText").innerText = `${activeMatch.teamARuns || 0} / ${activeMatch.teamAWickets || 0}`;
  document.getElementById("scorerOversText").innerText = `(${activeMatch.teamAOversBatted || 0.0} / ${activeMatch.totalOvers || 20} Overs)`;

  const winnerSelect = document.getElementById("directWinnerSelect");
  if (winnerSelect && teamA && teamB) {
    winnerSelect.innerHTML = `
      <option value="${teamA.id}">${teamA.name}</option>
      <option value="${teamB.id}">${teamB.name}</option>
      <option value="TIE">TIE / DRAW</option>
    `;
  }
}

function recordBallRun(runs) {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;
  match.teamARuns = (match.teamARuns || 0) + runs;
  saveToFirebase();
}

function recordWicket() {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;
  match.teamAWickets = (match.teamAWickets || 0) + 1;
  saveToFirebase();
}

function undoLastBall() {
  showToast("Last ball undone");
}

function submitDirectEndMatch() {
  const winnerId = document.getElementById("directWinnerSelect").value;
  const marginType = document.getElementById("directMarginType").value;
  const marginVal = document.getElementById("directMarginValue").value;

  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;

  const winnerTeam = state.teams.find(t => t.id === winnerId);
  const winnerName = winnerTeam ? winnerTeam.name : "Match Tied";

  match.status = "COMPLETED";
  match.winnerTeamId = winnerId;
  match.resultSummary = winnerId === "TIE" ? "Match Tied!" : `${winnerName} won by ${marginVal} ${marginType.toLowerCase()}`;
  match.teamARuns = parseInt(document.getElementById("directTeamARuns").value) || match.teamARuns;
  match.teamAOversBatted = parseFloat(document.getElementById("directTeamAOvers").value) || match.teamAOversBatted;
  match.teamBRuns = parseInt(document.getElementById("directTeamBRuns").value) || match.teamBRuns;
  match.teamBOversBatted = parseFloat(document.getElementById("directTeamBOvers").value) || match.teamBOversBatted;

  saveToFirebase();
  closeModal("directEndMatchModal");
  showToast(`🏆 ${match.resultSummary}`);
}

// --- POINTS TABLE ENGINE ---
function renderPointsTable() {
  const container = document.getElementById("pointsTableContainer");
  container.innerHTML = `
    <table>
      <thead>
        <tr>
          <th>Team</th>
          <th>P</th>
          <th>W</th>
          <th>L</th>
          <th>PTS</th>
          <th>NRR</th>
        </tr>
      </thead>
      <tbody>
        ${state.teams.map(t => `
          <tr>
            <td><strong>${t.name}</strong></td>
            <td>0</td>
            <td>0</td>
            <td>0</td>
            <td><strong style="color: var(--color-gold);">0</strong></td>
            <td style="color: var(--color-blue);">+0.000</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

// --- CODE CLAIMING ENGINE ---
function submitClaimCode() {
  const code = document.getElementById("claimCodeInput").value.trim().toUpperCase();
  const matchedTeam = state.teams.find(t => t.inviteCode.toUpperCase() === code);
  if (matchedTeam) {
    state.currentUser.role = "TEAM_CAPTAIN";
    state.currentUser.assignedTeamId = matchedTeam.id;
    document.getElementById("userRoleSelect").value = "TEAM_CAPTAIN";
    closeModal("claimCodeModal");
    renderAll();
    document.querySelector('[data-tab="tab-auction"]').click();
    showToast(`🎉 Claimed ${matchedTeam.name}! Bidding locked to your team.`);
    return;
  }

  const tourney = state.tournaments.find(t => (t.scorerInviteCode || "").toUpperCase() === code);
  if (tourney || code.startsWith("SC")) {
    state.currentUser.role = "SCORER";
    state.currentUser.assignedTeamId = null;
    document.getElementById("userRoleSelect").value = "SCORER";
    closeModal("claimCodeModal");
    renderAll();
    document.querySelector('[data-tab="tab-scorer"]').click();
    showToast("🏆 Scorer Access Granted! Entering Scorer Screen...");
    return;
  }

  showToast("❌ Invalid 6-digit code!");
}

// --- TOURNAMENTS ENGINE & MODAL HANDLERS ---
function renderTournaments() {
  const listEl = document.getElementById("tournamentsList");
  if (!listEl) return;

  const active = state.tournaments.find(t => t.id === state.activeTournamentId) || state.tournaments[0];
  if (active) {
    const titleEl = document.getElementById("activeTourneyTitle");
    if (titleEl) titleEl.innerText = active.name;
    const codeEl = document.getElementById("tourneyScorerCode");
    if (codeEl) codeEl.innerText = active.scorerInviteCode || "SC8K2M";
  }

  if (state.tournaments.length === 0) {
    listEl.innerHTML = `<p style="color: var(--color-text-sub);">No tournaments registered yet.</p>`;
    return;
  }

  listEl.innerHTML = state.tournaments.map(t => `
    <div class="card" style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; background: var(--bg-surface);">
      <div>
        <h4 style="color: var(--color-gold); font-size: 1.1rem; margin-bottom: 4px;">${t.name} ${t.id === state.activeTournamentId ? '⭐ (Active)' : ''}</h4>
        <p style="color: var(--color-text-sub); font-size: 0.8rem;">Purse: ₹${t.defaultPurse} Cr | Max Slots: ${t.maxSlots} | Max Overseas: ${t.maxOverseas} | Scorer Code: <strong>${t.scorerInviteCode || 'SC8K2M'}</strong></p>
      </div>
      <div>
        ${t.id !== state.activeTournamentId ? `<button class="btn btn-sm btn-secondary" onclick="switchActiveTournament('${t.id}')">Select</button>` : ''}
      </div>
    </div>
  `).join("");
}

function switchActiveTournament(tourneyId) {
  state.activeTournamentId = tourneyId;
  saveToFirebase();
  renderAll();
  showToast("Active tournament switched!");
}

function submitCreateTourney() {
  const name = document.getElementById("newTourneyName").value.trim();
  if (!name) { showToast("Please enter a tournament name"); return; }
  const purse = parseFloat(document.getElementById("newTourneyPurse").value) || 100.0;
  const maxSlots = parseInt(document.getElementById("newTourneyMaxSlots").value) || 25;
  const maxOverseas = parseInt(document.getElementById("newTourneyMaxOverseas").value) || 8;
  const scorerInviteCode = "SC" + Math.floor(100000 + Math.random() * 900000).toString().substring(0, 4);

  const newId = "t_" + Date.now();
  const tourney = {
    id: newId,
    name: name,
    defaultPurse: purse,
    maxSlots: maxSlots,
    maxOverseas: maxOverseas,
    scorerInviteCode: scorerInviteCode
  };

  state.tournaments.push(tourney);
  state.activeTournamentId = newId;
  saveToFirebase();
  closeModal("createTourneyModal");
  document.getElementById("newTourneyName").value = "";
  renderAll();
  showToast(`🏆 Created Tournament "${name}"!`);
}

function submitCreateTeam() {
  const name = document.getElementById("newTeamName").value.trim();
  const shortCode = document.getElementById("newTeamShortCode").value.trim().toUpperCase();
  if (!name || !shortCode) { showToast("Please enter team name and short code"); return; }
  const color = document.getElementById("newTeamColor").value || "#FFD700";
  const purse = parseFloat(document.getElementById("newTeamPurse").value) || 100.0;
  const inviteCode = shortCode + Math.floor(1000 + Math.random() * 9000);

  const team = {
    id: "team_" + Date.now(),
    tournamentId: state.activeTournamentId,
    name: name,
    shortCode: shortCode,
    primaryColorHex: color,
    totalPurse: purse,
    spentPurse: 0.0,
    inviteCode: inviteCode
  };

  state.teams.push(team);
  saveToFirebase();
  closeModal("createTeamModal");
  document.getElementById("newTeamName").value = "";
  document.getElementById("newTeamShortCode").value = "";
  renderAll();
  showToast(`🛡️ Team ${name} created! Invite Code: ${inviteCode}`);
}

function submitCreatePlayer() {
  const name = document.getElementById("newPlayerName").value.trim();
  if (!name) { showToast("Please enter player name"); return; }
  const role = document.getElementById("newPlayerRole").value;
  const country = document.getElementById("newPlayerCountry").value.trim() || "India";
  const isOverseas = country.toLowerCase() !== "india";
  const basePrice = parseFloat(document.getElementById("newPlayerBasePrice").value) || 2.0;
  const imageUrl = document.getElementById("newPlayerImageUrl").value.trim() || "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=500&auto=format&fit=crop&q=60";
  const setName = document.getElementById("newPlayerSetName").value.trim() || "Set 1";
  const stats = document.getElementById("newPlayerStats").value.trim() || "";

  const player = {
    id: "p_" + Date.now(),
    tournamentId: state.activeTournamentId,
    name: name,
    role: role,
    country: country,
    isOverseas: isOverseas,
    basePrice: basePrice,
    currentBid: basePrice,
    highestBidderTeamId: null,
    status: "UP_NEXT",
    imageUrl: imageUrl,
    stats: stats,
    setName: setName,
    isFixedSet: false
  };

  state.players.push(player);
  if (!state.activePlayerId) state.activePlayerId = player.id;
  saveToFirebase();
  closeModal("createPlayerModal");
  document.getElementById("newPlayerName").value = "";
  renderAll();
  showToast(`👤 Player ${name} added!`);
}

function populateMatchModalOptions() {
  const teamASelect = document.getElementById("newMatchTeamA");
  const teamBSelect = document.getElementById("newMatchTeamB");
  if (!teamASelect || !teamBSelect) return;
  const options = state.teams.map(t => `<option value="${t.id}">${t.name} (${t.shortCode})</option>`).join("");
  teamASelect.innerHTML = options;
  teamBSelect.innerHTML = options;
}

function submitCreateMatch() {
  const teamAId = document.getElementById("newMatchTeamA").value;
  const teamBId = document.getElementById("newMatchTeamB").value;
  if (!teamAId || !teamBId) { showToast("Create teams first!"); return; }
  if (teamAId === teamBId) { showToast("Select two different teams"); return; }
  const overs = parseInt(document.getElementById("newMatchOvers").value) || 20;

  const match = {
    id: "m_" + Date.now(),
    tournamentId: state.activeTournamentId,
    teamAId: teamAId,
    teamBId: teamBId,
    totalOvers: overs,
    status: "LIVE",
    teamARuns: 0,
    teamAWickets: 0,
    teamAOversBatted: 0.0,
    teamBRuns: 0,
    teamBWickets: 0,
    teamBOversBatted: 0.0
  };

  state.matches.push(match);
  state.activeMatchId = match.id;
  saveToFirebase();
  closeModal("createMatchModal");
  renderAll();
  showToast("🏏 Live Match started!");
}

function doSpinWheel() {
  const availablePlayers = state.players.filter(p => p.status === "UP_NEXT" || p.status === "UNSOLD");
  const displayEl = document.getElementById("spinWheelDisplay");
  if (availablePlayers.length === 0) {
    if (displayEl) displayEl.innerText = "No Available Players!";
    return;
  }
  let count = 0;
  const interval = setInterval(() => {
    const randomP = availablePlayers[Math.floor(Math.random() * availablePlayers.length)];
    if (displayEl) displayEl.innerText = randomP.name;
    count++;
    if (count > 15) {
      clearInterval(interval);
      state.activePlayerId = randomP.id;
      state.timerSeconds = 15;
      saveToFirebase();
      showToast(`🎰 Wheel selected: ${randomP.name}!`);
      setTimeout(() => closeModal("spinWheelModal"), 1000);
    }
  }, 100);
}

function renderScorecardModal() {
  const container = document.getElementById("scorecardContent");
  if (!container) return;
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) {
    container.innerHTML = "<p style='color: var(--color-text-sub);'>No live match records found.</p>";
    return;
  }
  const teamA = state.teams.find(t => t.id === match.teamAId);
  const teamB = state.teams.find(t => t.id === match.teamBId);
  container.innerHTML = `
    <div style="background: var(--bg-surface); padding: 16px; border-radius: 12px; margin-bottom: 12px;">
      <h4 style="color: var(--color-gold); font-size: 1.1rem; margin-bottom: 8px;">${teamA ? teamA.name : 'Team A'} vs ${teamB ? teamB.name : 'Team B'}</h4>
      <p style="font-size: 0.9rem; color: #FFF; font-weight: 700;">Innings 1: ${match.teamARuns || 0}/${match.teamAWickets || 0} (${match.teamAOversBatted || 0}/${match.totalOvers || 20} ov)</p>
      <p style="font-size: 0.9rem; color: #FFF; font-weight: 700;">Innings 2: ${match.teamBRuns || 0}/${match.teamBWickets || 0} (${match.teamBOversBatted || 0}/${match.totalOvers || 20} ov)</p>
      <p style="color: var(--color-green); font-weight: 800; margin-top: 8px;">Status: ${match.resultSummary || match.status || 'LIVE'}</p>
    </div>
  `;
}

// --- HELPER UTILITIES ---
function getDirectDriveUrl(url) {
  if (!url) return "https://images.unsplash.com/photo-1531415074968-036ba1b575da?w=500&auto=format&fit=crop&q=60";
  if (url.includes("drive.google.com")) {
    const match = url.match(/\/d\/([a-zA-Z0-9_-]+)/) || url.match(/id=([a-zA-Z0-9_-]+)/);
    if (match && match[1]) return `https://lh3.googleusercontent.com/d/${match[1]}`;
  }
  return url;
}

function openModal(modalId) {
  if (modalId === "createMatchModal") populateMatchModalOptions();
  if (modalId === "scorecardModal") renderScorecardModal();
  const el = document.getElementById(modalId);
  if (el) {
    el.classList.add("active");
  } else {
    console.error("Modal not found:", modalId);
  }
}

function closeModal(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.remove("active");
}

function copyText(text, label) {
  navigator.clipboard.writeText(text).then(() => {
    showToast(`🔑 ${label} ${text} copied to clipboard!`);
  });
}

function showToast(msg) {
  const container = document.getElementById("toastContainer");
  if (!container) return;
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.innerText = msg;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 3500);
}
