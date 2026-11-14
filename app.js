/* ==========================================================================
   IPL CRICKET HUB & LIVE AUCTION ARENA - JAVASCRIPT ENGINE
   ========================================================================== */

// --- FIREBASE CONFIGURATION ---
const firebaseConfig = {
  databaseURL: "https://cricket-auction-app-24750-default-rtdb.firebaseio.com",
  projectId: "cricket-auction-app-24750",
  storageBucket: "cricket-auction-app-24750.firebasestorage.app"
};

// Initialize Firebase App, Auth & Realtime Database
firebase.initializeApp(firebaseConfig);
const auth = firebase.auth();
const db = firebase.database();

// Authenticate anonymously so Firebase rules with auth != null pass without email warnings
auth.signInAnonymously().then(() => {
  console.log("Firebase Anonymous Auth connected successfully.");
}).catch(err => {
  console.warn("Firebase Anonymous Auth warning:", err);
});

// --- STATE STORE ---
let state = {
  tournaments: [],
  teams: [],
  players: [],
  bids: [],
  matches: [],
  ballRecords: [],
  activeTournamentId: null,
  activePlayerId: null,
  activeMatchId: null,
  currentUser: {
    role: "ADMIN_AUCTIONEER", // ADMIN_AUCTIONEER, TEAM_CAPTAIN, SCORER, SPECTATOR
    assignedTeamId: null
  },
  timerSeconds: 15,
  auctionState: {}
};

// --- THEME ENGINE ---
function toggleWebTheme() {
  const isLight = document.body.classList.toggle("light-theme");
  const btn = document.getElementById("themeToggleBtn");
  if (btn) {
    btn.innerText = isLight ? "🌙 Dark" : "☀️ Light";
  }
  try {
    localStorage.setItem("cricket_web_theme", isLight ? "light" : "dark");
  } catch (_e) {}
}

function initWebTheme() {
  try {
    const saved = localStorage.getItem("cricket_web_theme");
    const btn = document.getElementById("themeToggleBtn");
    if (saved === "light") {
      document.body.classList.add("light-theme");
      if (btn) btn.innerText = "🌙 Dark";
    } else {
      document.body.classList.remove("light-theme");
      if (btn) btn.innerText = "☀️ Light";
    }
  } catch (_e) {}
}

// --- BROWSER DEVICE ID & TOURNAMENT ISOLATION ENGINE ---
function getBrowserDeviceId() {
  let id = localStorage.getItem("cricket_browser_device_id");
  if (!id) {
    id = "browser_" + Date.now().toString(36) + "_" + Math.random().toString(36).substring(2, 9);
    localStorage.setItem("cricket_browser_device_id", id);
  }
  return id;
}

function getMyTournamentIds() {
  try {
    const raw = localStorage.getItem("cricket_my_tournaments");
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed;
    }
  } catch (_e) {}
  return [];
}

function registerMyTournament(tourneyId) {
  if (!tourneyId) return;
  const list = getMyTournamentIds();
  if (!list.includes(tourneyId)) {
    list.push(tourneyId);
    try {
      localStorage.setItem("cricket_my_tournaments", JSON.stringify(list));
    } catch (_e) {}
  }
  const deviceId = getBrowserDeviceId();
  try {
    db.ref("devices/" + deviceId + "/myTournaments/" + tourneyId).set(true);
  } catch (_e) {}
}

function isMyTournament(tourneyId) {
  const list = getMyTournamentIds();
  return list.includes(tourneyId);
}

function registerBrowserDevice() {
  const deviceId = getBrowserDeviceId();
  const dateFormat = new Date().toISOString();
  const deviceInfo = {
    deviceId: deviceId,
    platform: "Web Browser",
    userAgent: navigator.userAgent,
    language: navigator.language || "en",
    screenWidth: window.screen ? window.screen.width : 0,
    screenHeight: window.screen ? window.screen.height : 0,
    lastSeenDate: dateFormat,
    lastSeenTimestamp: Date.now()
  };
  try {
    db.ref("devices/" + deviceId + "/device_info").set(deviceInfo);
  } catch (e) {
    console.error("Device registration error:", e);
  }
}

function loadLocalCacheFirst() {
  try {
    const cached = localStorage.getItem("cricket_local_schema");
    if (cached) {
      applySchemaData(cached);
    }
  } catch (e) {
    console.error("Local cache load error:", e);
  }
}

function setupOfflineSyncListeners() {
  window.addEventListener("online", () => {
    showToast("Internet restored! Syncing data with cloud...");
    saveToFirebase();
  });

  window.addEventListener("offline", () => {
    showToast("Internet offline: actions saved locally & will sync when online.");
  });
}

// --- INITIALIZATION & REALTIME DB LISTENERS ---
document.addEventListener("DOMContentLoaded", () => {
  initWebTheme();
  setupNavigation();
  setupRoleSelector();
  registerBrowserDevice();
  loadLocalCacheFirst();
  listenToFirebase();
  startBidTimer();
  setupOfflineSyncListeners();
});

function ensureArray(val) {
  if (!val) return [];
  if (Array.isArray(val)) return val;
  if (typeof val === 'object') return Object.values(val);
  return [];
}

function applySchemaData(data) {
  if (!data) return false;
  let parsed = data;
  if (typeof data === 'string') {
    try {
      parsed = JSON.parse(data);
    } catch (e) {
      return false;
    }
  }
  if (parsed && typeof parsed === 'object') {
    state.tournaments = ensureArray(parsed.tournaments);
    state.teams = ensureArray(parsed.teams);
    state.players = ensureArray(parsed.players);
    state.bids = ensureArray(parsed.bids);
    state.matches = ensureArray(parsed.matches);
    state.ballRecords = ensureArray(parsed.ballRecords);
    state.auctionState = parsed.auctionState || {};
    state.activeTournamentId = parsed.activeTournamentId || (state.tournaments[0] ? state.tournaments[0].id : null);
    state.activePlayerId = parsed.activePlayerId || (state.players[0] ? state.players[0].id : null);
    state.activeMatchId = parsed.activeMatchId || (state.matches[0] ? state.matches[0].id : null);

    renderAll();
    return true;
  }
  return false;
}

// --- REALTIME LISTENER FOR VERTICAL FIREBASE STRUCTURE ---
function listenToFirebase() {
  const tourneysRef = db.ref("tournaments");

  tourneysRef.on("value", (snapshot) => {
    const data = snapshot.val();
    if (data && typeof data === 'object') {
      applyStructuredFirebaseData(data);
    } else {
      // Cloud is empty. Clean up legacy flat data if present
      cleanLegacyFirebaseNodes();
    }
  });

  // Also clean legacy flat nodes immediately
  cleanLegacyFirebaseNodes();
}

function cleanLegacyFirebaseNodes() {
  const deviceId = getBrowserDeviceId();
  try {
    db.ref("database_schema").remove();
    db.ref("active_devices").remove();
    db.ref("users").remove();
    db.ref("devices/" + deviceId + "/database_schema").remove();
  } catch (_e) {}
}

function applyStructuredFirebaseData(data) {
  if (!data || typeof data !== 'object') return;

  const tourneysList = [];
  const teamsList = [];
  const playersList = [];
  const matchesList = [];
  const ballsList = [];
  const bidsList = [];
  const auctionStateMap = {};

  const myTourneyIds = getMyTournamentIds();

  Object.keys(data).forEach(tId => {
    const tObj = data[tId];
    if (!tObj) return;

    // Tournament Info
    const info = tObj.info || tObj;
    tourneysList.push({
      id: tId,
      name: info.name || "Tournament",
      adminCode: info.adminCode || "ADM2026",
      scorerInviteCode: info.scorerInviteCode || "SC8K2M",
      defaultPurse: parseFloat(info.defaultPurse) || 100.0,
      maxSlots: parseInt(info.maxSlots) || 25,
      maxOverseas: parseInt(info.maxOverseas) || 8,
      createdAt: info.createdAt || Date.now(),
      creatorDeviceId: info.creatorDeviceId || ""
    });

    // Teams
    if (tObj.teams && typeof tObj.teams === 'object') {
      Object.values(tObj.teams).forEach(tm => {
        if (tm && tm.id) {
          teamsList.push({
            id: tm.id,
            tournamentId: tm.tournamentId || tId,
            name: tm.name || "Team",
            shortCode: tm.shortCode || "TM",
            primaryColorHex: tm.primaryColorHex || "#FFD700",
            totalPurse: parseFloat(tm.totalPurse) || 100.0,
            spentPurse: parseFloat(tm.spentPurse) || 0.0,
            inviteCode: tm.inviteCode || "",
            captainEmail: tm.captainEmail || ""
          });
        }
      });
    }

    // Players
    if (tObj.players && typeof tObj.players === 'object') {
      Object.values(tObj.players).forEach(p => {
        if (p && p.id) {
          playersList.push({
            id: p.id,
            tournamentId: p.tournamentId || tId,
            name: p.name || "Player",
            role: p.role || "BATSMAN",
            country: p.country || "India",
            isOverseas: !!p.isOverseas,
            basePrice: parseFloat(p.basePrice) || 2.0,
            currentBid: parseFloat(p.currentBid) || parseFloat(p.basePrice) || 2.0,
            highestBidderTeamId: p.highestBidderTeamId || null,
            status: p.status || "UP_NEXT",
            soldToTeamId: p.soldToTeamId || null,
            soldPrice: p.soldPrice ? parseFloat(p.soldPrice) : null,
            imageUrl: p.imageUrl || "",
            stats: p.stats || "",
            setName: p.setName || "Set 1",
            isFixedSet: !!p.isFixedSet
          });
        }
      });
    }

    // Matches
    if (tObj.matches && typeof tObj.matches === 'object') {
      Object.values(tObj.matches).forEach(m => {
        if (m && m.id) {
          matchesList.push({
            id: m.id,
            tournamentId: m.tournamentId || tId,
            teamAId: m.teamAId || "",
            teamBId: m.teamBId || "",
            totalOvers: parseInt(m.totalOvers) || 20,
            teamARuns: parseInt(m.teamARuns) || 0,
            teamAWickets: parseInt(m.teamAWickets) || 0,
            teamAOversBatted: parseFloat(m.teamAOversBatted) || 0.0,
            teamBRuns: parseInt(m.teamBRuns) || 0,
            teamBWickets: parseInt(m.teamBWickets) || 0,
            teamBOversBatted: parseFloat(m.teamBOversBatted) || 0.0,
            currentInnings: parseInt(m.currentInnings) || 1,
            status: m.status || "UPCOMING",
            winnerTeamId: m.winnerTeamId || null,
            resultSummary: m.resultSummary || ""
          });
        }
      });
    }

    // Ball Records
    if (tObj.ballRecords && typeof tObj.ballRecords === 'object') {
      Object.values(tObj.ballRecords).forEach(b => {
        if (b && b.id) ballsList.push(b);
      });
    }

    // Bids
    if (tObj.bids && typeof tObj.bids === 'object') {
      Object.values(tObj.bids).forEach(b => {
        if (b && b.id) bidsList.push(b);
      });
    }

    // Auction State
    if (tObj.auctionState && typeof tObj.auctionState === 'object') {
      auctionStateMap[tId] = {
        isBiddingStarted: !!tObj.auctionState.isBiddingStarted,
        readyCaptains: tObj.auctionState.readyCaptains || {}
      };
    }
  });

  state.tournaments = tourneysList;
  state.teams = teamsList;
  state.players = playersList;
  state.matches = matchesList;
  state.ballRecords = ballsList;
  state.bids = bidsList;
  state.auctionState = auctionStateMap;

  // Active tournament selection scoped to my tournaments
  const myTourneys = tourneysList.filter(t => myTourneyIds.includes(t.id));
  if (myTourneys.length > 0) {
    if (!myTourneys.some(t => t.id === state.activeTournamentId)) {
      state.activeTournamentId = myTourneys[0].id;
    }
  } else if (tourneysList.length > 0 && !tourneysList.some(t => t.id === state.activeTournamentId)) {
    state.activeTournamentId = tourneysList[0].id;
  }

  // Active player selection
  const currentTourneyPlayers = state.players.filter(p => p.tournamentId === state.activeTournamentId);
  if (!currentTourneyPlayers.some(p => p.id === state.activePlayerId)) {
    state.activePlayerId = currentTourneyPlayers[0] ? currentTourneyPlayers[0].id : null;
  }

  // Active match selection
  const currentTourneyMatches = state.matches.filter(m => m.tournamentId === state.activeTournamentId);
  if (!currentTourneyMatches.some(m => m.id === state.activeMatchId)) {
    state.activeMatchId = currentTourneyMatches[0] ? currentTourneyMatches[0].id : null;
  }

  // Save to local cache
  try {
    localStorage.setItem("cricket_local_schema", JSON.stringify(state));
  } catch (_e) {}

  renderAll();
}

// --- SYNC TO FIREBASE (VERTICAL SUB-NODES, NO HORIZONTAL SCROLL) ---
function saveToFirebase() {
  // 1. Durability in localStorage first
  try {
    localStorage.setItem("cricket_local_schema", JSON.stringify(state));
  } catch (_e) {}

  // 2. Clean legacy nodes
  cleanLegacyFirebaseNodes();

  const deviceId = getBrowserDeviceId();

  // 3. Vertical tournament objects
  state.tournaments.forEach(tourney => {
    const tRef = db.ref("tournaments/" + tourney.id);

    // Info node
    tRef.child("info").set({
      id: tourney.id,
      name: tourney.name,
      adminCode: tourney.adminCode || "ADM2026",
      scorerInviteCode: tourney.scorerInviteCode || "SC8K2M",
      defaultPurse: tourney.defaultPurse || 100.0,
      maxSlots: tourney.maxSlots || 25,
      maxOverseas: tourney.maxOverseas || 8,
      createdAt: tourney.createdAt || Date.now(),
      creatorDeviceId: deviceId
    });

    // Teams node
    const tourneyTeams = state.teams.filter(t => t.tournamentId === tourney.id);
    if (tourneyTeams.length > 0) {
      const teamsObj = {};
      tourneyTeams.forEach(t => {
        teamsObj[t.id] = {
          id: t.id,
          tournamentId: t.tournamentId,
          name: t.name,
          shortCode: t.shortCode,
          primaryColorHex: t.primaryColorHex || "#FFD700",
          totalPurse: t.totalPurse || 100.0,
          spentPurse: t.spentPurse || 0.0,
          inviteCode: t.inviteCode || "",
          captainEmail: t.captainEmail || ""
        };
      });
      tRef.child("teams").set(teamsObj);
    }

    // Players node
    const tourneyPlayers = state.players.filter(p => p.tournamentId === tourney.id);
    if (tourneyPlayers.length > 0) {
      const playersObj = {};
      tourneyPlayers.forEach(p => {
        playersObj[p.id] = {
          id: p.id,
          tournamentId: p.tournamentId,
          name: p.name,
          role: p.role || "BATSMAN",
          country: p.country || "India",
          isOverseas: !!p.isOverseas,
          basePrice: p.basePrice || 2.0,
          currentBid: p.currentBid || p.basePrice || 2.0,
          highestBidderTeamId: p.highestBidderTeamId || "",
          status: p.status || "UP_NEXT",
          soldToTeamId: p.soldToTeamId || "",
          soldPrice: p.soldPrice || 0.0,
          imageUrl: p.imageUrl || "",
          stats: p.stats || "",
          setName: p.setName || "Set 1",
          isFixedSet: !!p.isFixedSet
        };
      });
      tRef.child("players").set(playersObj);
    }

    // Matches node
    const tourneyMatches = state.matches.filter(m => m.tournamentId === tourney.id);
    if (tourneyMatches.length > 0) {
      const matchesObj = {};
      tourneyMatches.forEach(m => {
        matchesObj[m.id] = {
          id: m.id,
          tournamentId: m.tournamentId,
          teamAId: m.teamAId || "",
          teamBId: m.teamBId || "",
          totalOvers: m.totalOvers || 20,
          teamARuns: m.teamARuns || 0,
          teamAWickets: m.teamAWickets || 0,
          teamAOversBatted: m.teamAOversBatted || 0.0,
          teamBRuns: m.teamBRuns || 0,
          teamBWickets: m.teamBWickets || 0,
          teamBOversBatted: m.teamBOversBatted || 0.0,
          currentInnings: m.currentInnings || 1,
          status: m.status || "UPCOMING",
          winnerTeamId: m.winnerTeamId || "",
          resultSummary: m.resultSummary || ""
        };
      });
      tRef.child("matches").set(matchesObj);
    }

    // BallRecords node
    const tourneyBalls = state.ballRecords.filter(b => b.tournamentId === tourney.id);
    if (tourneyBalls.length > 0) {
      const ballsObj = {};
      tourneyBalls.forEach(b => {
        ballsObj[b.id] = {
          id: b.id,
          matchId: b.matchId || "",
          tournamentId: b.tournamentId || tourney.id,
          innings: b.innings || 1,
          overNumber: b.overNumber || 0,
          ballInOver: b.ballInOver || 1,
          runsScored: b.runsScored || 0,
          extraRuns: b.extraRuns || 0,
          extraType: b.extraType || "NONE",
          isWicket: !!b.isWicket,
          batsmanName: b.batsmanName || "",
          bowlerName: b.bowlerName || "",
          timestamp: b.timestamp || Date.now()
        };
      });
      tRef.child("ballRecords").set(ballsObj);
    }

    // Bids node
    const tourneyBids = state.bids.filter(b => b.tournamentId === tourney.id);
    if (tourneyBids.length > 0) {
      const bidsObj = {};
      tourneyBids.forEach(b => {
        bidsObj[b.id] = {
          id: b.id,
          playerId: b.playerId || "",
          teamId: b.teamId || "",
          amount: b.amount || 0.0,
          timestamp: b.timestamp || Date.now(),
          tournamentId: b.tournamentId || tourney.id
        };
      });
      tRef.child("bids").set(bidsObj);
    }
  });

  // Device heartbeat
  try {
    db.ref("devices/" + deviceId + "/lastSeenTimestamp").set(Date.now());
  } catch (_e) {}
}

// --- AUCTION READINESS ENGINE ---
function getActiveAuctionState() {
  const tId = state.activeTournamentId;
  if (!tId) return { isBiddingStarted: false, readyCaptains: {} };
  if (!state.auctionState) state.auctionState = {};
  if (!state.auctionState[tId]) {
    state.auctionState[tId] = { isBiddingStarted: false, readyCaptains: {} };
  }
  return state.auctionState[tId];
}

function getJoinedCaptainTeams() {
  const auctionState = getActiveAuctionState();
  const readyKeys = Object.keys(auctionState.readyCaptains || {});
  const activeTeams = state.teams.filter(t => t.tournamentId === state.activeTournamentId);
  const joinedTeamIds = new Set(readyKeys);
  if (state.currentUser.role === "TEAM_CAPTAIN" && state.currentUser.assignedTeamId) {
    joinedTeamIds.add(state.currentUser.assignedTeamId);
  }
  return activeTeams.filter(t => joinedTeamIds.has(t.id));
}

function areAllCaptainsReady() {
  const joinedTeams = getJoinedCaptainTeams();
  if (joinedTeams.length === 0) return true; // Solo admin mode
  const auctionState = getActiveAuctionState();
  const readyMap = auctionState.readyCaptains || {};
  return joinedTeams.every(t => readyMap[t.id] === true);
}

function toggleCaptainReady() {
  if (state.currentUser.role !== "TEAM_CAPTAIN" || !state.currentUser.assignedTeamId) {
    showToast("Please claim or switch to your Captain role first!");
    return;
  }
  const myTeamId = state.currentUser.assignedTeamId;
  const auctionState = getActiveAuctionState();
  const currentReady = !!(auctionState.readyCaptains && auctionState.readyCaptains[myTeamId]);
  const newReady = !currentReady;

  if (!auctionState.readyCaptains) auctionState.readyCaptains = {};
  auctionState.readyCaptains[myTeamId] = newReady;

  const tId = state.activeTournamentId;
  if (tId) {
    db.ref("tournaments/" + tId + "/auctionState/readyCaptains/" + myTeamId).set(newReady);
  }
  renderAuctionArena();
  showToast(newReady ? "✅ You are marked as READY!" : "⏳ Marked as NOT READY");
}

function startAuctionBidding(force = false) {
  if (state.currentUser.role !== "ADMIN_AUCTIONEER") {
    showToast("Only Admin can start the auction bidding!");
    return;
  }
  if (!force && !areAllCaptainsReady()) {
    showToast("Cannot start bidding: Not all captains are ready!");
    return;
  }
  const tId = state.activeTournamentId;
  if (!tId) return;
  const auctionState = getActiveAuctionState();
  auctionState.isBiddingStarted = true;
  db.ref("tournaments/" + tId + "/auctionState/isBiddingStarted").set(true);
  renderAuctionArena();
  showToast("🚀 Auction Bidding has STARTED! Bids are now open!");
}

function resetAuctionBidding() {
  const tId = state.activeTournamentId;
  if (!tId) return;
  const auctionState = getActiveAuctionState();
  auctionState.isBiddingStarted = false;
  auctionState.readyCaptains = {};
  db.ref("tournaments/" + tId + "/auctionState").set({
    isBiddingStarted: false,
    readyCaptains: {}
  });
  renderAuctionArena();
  showToast("🔄 Auction readiness reset for next round");
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
  renderMvpLeaderboard();
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
      const targetPane = document.getElementById(target);
      if (targetPane) targetPane.classList.add("active");
    });
  });

  const subButtons = document.querySelectorAll(".sub-nav-btn");
  subButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      subButtons.forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      const target = btn.getAttribute("data-subtab");
      document.querySelectorAll(".sub-tab-pane").forEach(pane => pane.classList.remove("active"));
      const targetSubPane = document.getElementById(target);
      if (targetSubPane) targetSubPane.classList.add("active");
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

function renderAuctionReadinessCard() {
  const card = document.getElementById("auctionReadinessCard");
  if (!card) return;

  const auctionState = getActiveAuctionState();
  const isStarted = !!auctionState.isBiddingStarted;
  const joinedTeams = getJoinedCaptainTeams();
  const isSolo = joinedTeams.length === 0;
  const allReady = areAllCaptainsReady();
  const role = state.currentUser.role;

  if (isStarted) {
    card.innerHTML = `
      <div class="card" style="margin-bottom: 0; background: linear-gradient(135deg, rgba(0,230,118,0.1) 0%, rgba(0,210,255,0.05) 100%); border-color: var(--color-green); display: flex; justify-content: space-between; align-items: center; padding: 12px 18px;">
        <div style="display: flex; align-items: center; gap: 10px;">
          <span style="font-size: 1.3rem;">🚀</span>
          <div>
            <div style="font-weight: 800; color: var(--color-green); font-size: 0.95rem;">BIDDING ACTIVE &amp; LIVE</div>
            <div style="font-size: 0.75rem; color: var(--color-text-sub);">Captains are connected and live bids are being accepted.</div>
          </div>
        </div>
        ${role === "ADMIN_AUCTIONEER" ? `
          <button class="btn btn-sm btn-secondary" onclick="resetAuctionBidding()" style="font-size: 0.75rem;">Reset Ready Check</button>
        ` : ''}
      </div>
    `;
    return;
  }

  // Not started yet
  if (role === "TEAM_CAPTAIN") {
    const myTeam = state.teams.find(t => t.id === state.currentUser.assignedTeamId);
    const myReady = !!(myTeam && auctionState.readyCaptains && auctionState.readyCaptains[myTeam.id]);
    card.innerHTML = `
      <div class="card" style="margin-bottom: 0; background: rgba(0,210,255,0.06); border-color: ${myReady ? 'var(--color-green)' : 'var(--color-gold)'}; padding: 16px;">
        <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
          <div>
            <div style="font-size: 0.75rem; font-weight: 800; color: var(--color-gold); letter-spacing: 1px;">🛡️ CAPTAIN READINESS CHECK</div>
            <h4 style="color: var(--color-text-main); margin-top: 2px;">
              Team: <strong>${myTeam ? myTeam.name : 'Unknown Team'}</strong> &mdash; 
              Status: <span style="color: ${myReady ? 'var(--color-green)' : 'var(--color-red)'}; font-weight: 800;">${myReady ? '✅ READY' : '⏳ NOT READY'}</span>
            </h4>
            <p style="color: var(--color-text-sub); font-size: 0.8rem; margin-top: 4px;">
              ${myReady ? 'You are ready! Waiting for Admin to start bidding.' : 'Click the button below when your team is ready to begin bidding.'}
            </p>
          </div>
          <button class="btn ${myReady ? 'btn-secondary' : 'btn-success'}" onclick="toggleCaptainReady()" style="padding: 10px 20px; font-weight: 800;">
            ${myReady ? 'READY ✅ (Click to change)' : 'I AM READY 👍'}
          </button>
        </div>
      </div>
    `;
  } else if (role === "ADMIN_AUCTIONEER") {
    if (isSolo) {
      card.innerHTML = `
        <div class="card" style="margin-bottom: 0; background: rgba(255,215,0,0.05); border-color: rgba(255,215,0,0.3); padding: 12px 18px; display: flex; justify-content: space-between; align-items: center;">
          <div style="display: flex; align-items: center; gap: 10px;">
            <span style="font-size: 1.2rem;">👤</span>
            <div>
              <div style="font-weight: 800; color: var(--color-gold); font-size: 0.9rem;">SOLO ADMIN MODE ACTIVE</div>
              <div style="font-size: 0.75rem; color: var(--color-text-sub);">No captains joined via invite codes yet. Direct bidding is enabled!</div>
            </div>
          </div>
          <span class="chip" style="background: rgba(0,230,118,0.15); color: var(--color-green); font-size: 0.75rem;">Open Bidding</span>
        </div>
      `;
    } else {
      const readyPills = joinedTeams.map(t => {
        const isR = !!(auctionState.readyCaptains && auctionState.readyCaptains[t.id]);
        return `<span class="chip" style="background: ${isR ? 'rgba(0,230,118,0.2)' : 'rgba(255,23,68,0.2)'}; color: ${isR ? 'var(--color-green)' : 'var(--color-red)'}; font-size: 0.75rem; font-weight: 800;">${t.shortCode}: ${isR ? '✅ Ready' : '⏳ Waiting'}</span>`;
      }).join(" ");

      card.innerHTML = `
        <div class="card" style="margin-bottom: 0; background: rgba(0,210,255,0.06); border-color: var(--color-blue); padding: 16px;">
          <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;">
            <div>
              <div style="font-size: 0.75rem; font-weight: 800; color: var(--color-blue); letter-spacing: 1px;">👑 AUCTION READINESS MONITOR</div>
              <div style="margin-top: 6px; display: flex; gap: 6px; flex-wrap: wrap;">${readyPills}</div>
              <p style="color: var(--color-text-sub); font-size: 0.8rem; margin-top: 6px;">
                ${allReady ? '🎉 All joined captains are READY! You can now start the auction.' : 'Waiting for all joined captains to mark themselves Ready.'}
              </p>
            </div>
            <div style="display: flex; gap: 8px;">
              <button class="btn btn-gold" onclick="startAuctionBidding(false)" ${allReady ? '' : 'disabled style="opacity: 0.5; cursor: not-allowed;"'}>
                START BIDDING 🚀
              </button>
              ${!allReady ? `<button class="btn btn-sm btn-secondary" onclick="startAuctionBidding(true)" title="Force start without waiting">Override ⚠️</button>` : ''}
            </div>
          </div>
        </div>
      `;
    }
  } else {
    // Scorer / Spectator
    card.innerHTML = `
      <div class="card" style="margin-bottom: 0; background: rgba(255,255,255,0.03); border-color: var(--color-border); padding: 10px 16px;">
        <span style="font-size: 0.8rem; color: var(--color-text-sub);">⏳ Waiting for captains &amp; admin to start bidding...</span>
      </div>
    `;
  }
}

// --- AUCTION ARENA ENGINE ---
function renderAuctionArena() {
  renderAuctionReadinessCard();

  const player = state.players.find(p => p.id === state.activePlayerId);
  if (!player) {
    const nameEl = document.getElementById("stagePlayerName");
    if (nameEl) nameEl.innerText = "No Player Staged";
    const bidEl = document.getElementById("stageCurrentBid");
    if (bidEl) bidEl.innerText = "₹0.00 Cr";
    return;
  }

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
  const auctionState = getActiveAuctionState();
  const joinedTeams = getJoinedCaptainTeams();

  // If captains have joined and auction bidding is not started, block bidding
  if (!auctionState.isBiddingStarted && joinedTeams.length > 0) {
    showToast("⚠️ Bidding is locked! All captains must click READY and Admin must start the bidding.");
    return;
  }

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
    timestamp: Date.now(),
    tournamentId: player.tournamentId || state.activeTournamentId
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
function copyScorerCodeFromTab() {
  const tourney = state.tournaments.find(t => t.id === state.activeTournamentId) || state.tournaments[0];
  const code = tourney ? (tourney.scorerInviteCode || "SC8K2M") : "SC8K2M";
  copyText(code, "Scorer Invite Code");
}

function renderLiveScorer() {
  const activeTourney = state.tournaments.find(t => t.id === state.activeTournamentId) || state.tournaments[0];
  const tabCodeEl = document.getElementById("tabScorerCodeVal");
  if (tabCodeEl && activeTourney) {
    tabCodeEl.innerText = activeTourney.scorerInviteCode || "SC8K2M";
  }

  const activeMatch = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!activeMatch) {
    document.getElementById("scorerScoreText").innerText = "0 / 0";
    return;
  }
  const currentInnings = activeMatch.currentInnings || 1;
  const battingTeamId = currentInnings === 1 ? activeMatch.teamAId : activeMatch.teamBId;
  const bowlingTeamId = currentInnings === 1 ? activeMatch.teamBId : activeMatch.teamAId;
  const battingTeam = state.teams.find(t => t.id === battingTeamId);
  const bowlingTeam = state.teams.find(t => t.id === bowlingTeamId);

  const runs = currentInnings === 1 ? (activeMatch.teamARuns || 0) : (activeMatch.teamBRuns || 0);
  const wkts = currentInnings === 1 ? (activeMatch.teamAWickets || 0) : (activeMatch.teamBWickets || 0);
  const overs = currentInnings === 1 ? (activeMatch.teamAOversBatted || 0.0) : (activeMatch.teamBOversBatted || 0.0);

  document.getElementById("scorerBattingTeamName").innerText = `${battingTeam ? battingTeam.name : 'Batting Team'} (Inn ${currentInnings})`;
  document.getElementById("scorerScoreText").innerText = `${runs} / ${wkts}`;
  document.getElementById("scorerOversText").innerText = `(${overs} / ${activeMatch.totalOvers || 20} Overs)`;

  const targetBox = document.getElementById("targetRequirementBox");
  if (targetBox) {
    if (currentInnings === 2) {
      const target = (activeMatch.teamARuns || 0) + 1;
      const need = Math.max(0, target - (activeMatch.teamBRuns || 0));
      targetBox.innerHTML = `
        <div style="color: var(--color-green); font-weight: 900; font-size: 1.1rem;">TARGET: ${target}</div>
        <div style="color: #FFF; font-size: 0.85rem; font-weight: 700;">Need ${need} runs to win</div>
      `;
    } else {
      targetBox.innerHTML = `
        <div style="color: var(--color-gold); font-size: 0.85rem; font-weight: 700;">1st Innings in Progress</div>
      `;
    }
  }

  // --- Render Cricbuzz-Style Crease Table ---
  const creaseContainer = document.getElementById("cricbuzzCreaseContainer");
  if (creaseContainer) {
    const battingSquad = getMatchSquad(activeMatch, battingTeamId);
    const bowlingSquad = getMatchSquad(activeMatch, bowlingTeamId);

    const striker = battingSquad[0] || { name: "Striker Batsman", id: "p1" };
    const nonStriker = battingSquad[1] || { name: "Non-Striker Batsman", id: "p2" };
    const bowler = bowlingSquad[0] || { name: "Opening Bowler", id: "b1" };

    const matchBalls = (state.ballRecords || []).filter(b => b.matchId === activeMatch.id && b.innings === currentInnings);
    const sBalls = matchBalls.filter(b => b.strikerId === striker.id);
    const sRuns = sBalls.reduce((acc, b) => acc + (b.runsScored || 0), 0);
    const sFaced = sBalls.filter(b => b.extraType !== 'WIDE').length;
    const s4s = sBalls.filter(b => b.runsScored === 4).length;
    const s6s = sBalls.filter(b => b.runsScored === 6).length;
    const sSR = sFaced > 0 ? ((sRuns * 100) / sFaced).toFixed(1) : "0.0";

    const nsBalls = matchBalls.filter(b => b.strikerId === nonStriker.id);
    const nsRuns = nsBalls.reduce((acc, b) => acc + (b.runsScored || 0), 0);
    const nsFaced = nsBalls.filter(b => b.extraType !== 'WIDE').length;
    const ns4s = nsBalls.filter(b => b.runsScored === 4).length;
    const ns6s = nsBalls.filter(b => b.runsScored === 6).length;
    const nsSR = nsFaced > 0 ? ((nsRuns * 100) / nsFaced).toFixed(1) : "0.0";

    const bBalls = matchBalls.filter(b => b.bowlerId === bowler.id);
    const bLegal = bBalls.filter(b => b.extraType !== 'WIDE' && b.extraType !== 'NO_BALL').length;
    const bOvers = `${Math.floor(bLegal / 6)}.${bLegal % 6}`;
    const bRuns = bBalls.reduce((acc, b) => acc + (b.runsScored || 0) + (b.extraType === 'WIDE' || b.extraType === 'NO_BALL' ? (b.extraRuns || 1) : 0), 0);
    const bWkts = bBalls.filter(b => b.isWicket && b.dismissalType !== 'RUN_OUT').length;
    const bEcon = bLegal > 0 ? ((bRuns * 6) / bLegal).toFixed(2) : "0.00";

    creaseContainer.innerHTML = `
      <div style="background: rgba(0,0,0,0.3); border-radius: 10px; padding: 12px; margin-bottom: 12px; border: 1px solid rgba(255,215,0,0.25);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <span style="color: var(--color-gold); font-size: 0.8rem; font-weight: 800;">🏏 BATSMEN ON CREASE</span>
          <span style="color: var(--color-text-sub); font-size: 0.75rem;">${battingTeam ? battingTeam.name : ''}</span>
        </div>
        <table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">
          <thead>
            <tr style="color: var(--color-text-sub); border-bottom: 1px solid rgba(255,255,255,0.1); text-align: right;">
              <th style="text-align: left; padding: 4px;">Batter</th>
              <th style="padding: 4px;">R</th>
              <th style="padding: 4px;">B</th>
              <th style="padding: 4px;">4s</th>
              <th style="padding: 4px;">6s</th>
              <th style="padding: 4px;">SR</th>
            </tr>
          </thead>
          <tbody>
            <tr style="border-bottom: 1px solid rgba(255,255,255,0.05); text-align: right;">
              <td style="text-align: left; padding: 6px 4px; color: #FFF; font-weight: 700;">* ${striker.name}</td>
              <td style="color: var(--color-gold); font-weight: 900; padding: 4px;">${sRuns}</td>
              <td style="color: #DDD; padding: 4px;">${sFaced}</td>
              <td style="color: #DDD; padding: 4px;">${s4s}</td>
              <td style="color: #DDD; padding: 4px;">${s6s}</td>
              <td style="color: var(--color-text-sub); padding: 4px;">${sSR}</td>
            </tr>
            <tr style="text-align: right;">
              <td style="text-align: left; padding: 6px 4px; color: #FFF;">${nonStriker.name}</td>
              <td style="color: #FFF; font-weight: 700; padding: 4px;">${nsRuns}</td>
              <td style="color: #DDD; padding: 4px;">${nsFaced}</td>
              <td style="color: #DDD; padding: 4px;">${ns4s}</td>
              <td style="color: #DDD; padding: 4px;">${ns6s}</td>
              <td style="color: var(--color-text-sub); padding: 4px;">${nsSR}</td>
            </tr>
          </tbody>
        </table>

        <div style="margin-top: 12px; border-top: 1px solid rgba(255,255,255,0.1); padding-top: 10px;">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px;">
            <span style="color: var(--color-blue); font-size: 0.8rem; font-weight: 800;">⚡ CURRENT BOWLER</span>
            <span style="color: var(--color-text-sub); font-size: 0.75rem;">${bowlingTeam ? bowlingTeam.name : ''}</span>
          </div>
          <table style="width: 100%; border-collapse: collapse; font-size: 0.85rem;">
            <thead>
              <tr style="color: var(--color-text-sub); border-bottom: 1px solid rgba(255,255,255,0.1); text-align: right;">
                <th style="text-align: left; padding: 4px;">Bowler</th>
                <th style="padding: 4px;">O</th>
                <th style="padding: 4px;">M</th>
                <th style="padding: 4px;">R</th>
                <th style="padding: 4px;">W</th>
                <th style="padding: 4px;">ECON</th>
              </tr>
            </thead>
            <tbody>
              <tr style="text-align: right;">
                <td style="text-align: left; padding: 6px 4px; color: #FFF; font-weight: 700;">${bowler.name}</td>
                <td style="color: #FFF; padding: 4px;">${bOvers}</td>
                <td style="color: #DDD; padding: 4px;">0</td>
                <td style="color: #FFF; padding: 4px;">${bRuns}</td>
                <td style="color: var(--color-red); font-weight: 900; padding: 4px;">${bWkts}</td>
                <td style="color: var(--color-blue); font-weight: 700; padding: 4px;">${bEcon}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    `;
  }

  const winnerSelect = document.getElementById("directWinnerSelect");
  if (winnerSelect && battingTeam && bowlingTeam) {
    winnerSelect.innerHTML = `
      <option value="${activeMatch.teamAId}">${state.teams.find(t => t.id === activeMatch.teamAId)?.name || 'Team A'}</option>
      <option value="${activeMatch.teamBId}">${state.teams.find(t => t.id === activeMatch.teamBId)?.name || 'Team B'}</option>
      <option value="TIE">TIE / DRAW</option>
    `;
  }

  // Toggle Undo Innings button visibility
  const headerUndoBtn = document.getElementById("headerUndoInningsBtn");
  const keypadUndoBtn = document.getElementById("keypadUndoInningsBtn");
  const canUndoInnings = currentInnings === 2 || activeMatch.status === "COMPLETED";
  if (headerUndoBtn) headerUndoBtn.style.display = canUndoInnings ? "inline-block" : "none";
  if (keypadUndoBtn) keypadUndoBtn.style.display = canUndoInnings ? "inline-block" : "none";
}

function endCurrentInnings() {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;

  const currentInnings = match.currentInnings || 1;
  if (currentInnings === 1) {
    if (!confirm("Are you sure you want to end Innings 1 now? This will declare the batting team and set the target for Innings 2.")) return;
    match.currentInnings = 2;
    match.status = "LIVE_INNINGS_2";
    showToast(`🏁 Innings 1 Ended! Target: ${(match.teamARuns || 0) + 1} runs. (Use 'Undo Innings' to revert)`);
  } else {
    if (!confirm("Are you sure you want to end Innings 2 and complete the match?")) return;
    match.status = "COMPLETED";
    if ((match.teamBRuns || 0) > (match.teamARuns || 0)) {
      match.winnerTeamId = match.teamBId;
      match.resultSummary = `${state.teams.find(t => t.id === match.teamBId)?.name || 'Team B'} won`;
    } else if ((match.teamARuns || 0) > (match.teamBRuns || 0)) {
      match.winnerTeamId = match.teamAId;
      const margin = (match.teamARuns || 0) - (match.teamBRuns || 0);
      match.resultSummary = `${state.teams.find(t => t.id === match.teamAId)?.name || 'Team A'} won by ${margin} runs`;
    } else {
      match.winnerTeamId = "TIE";
      match.resultSummary = "Match Tied";
    }
    showToast(`🏆 ${match.resultSummary}`);
  }

  saveToFirebase();
  renderLiveScorer();
}

function recalculateMatchFromBalls(match) {
  const matchBalls = (state.ballRecords || []).filter(b => b.matchId === match.id);
  const inn1Balls = matchBalls.filter(b => b.innings === 1);
  const inn2Balls = matchBalls.filter(b => b.innings === 2);

  const teamARuns = inn1Balls.reduce((acc, b) => acc + (b.runsScored || 0) + (b.extraRuns || 0), 0);
  const teamAWkts = inn1Balls.filter(b => b.isWicket).length;
  const legal1 = inn1Balls.filter(b => b.extraType !== 'WIDE' && b.extraType !== 'NO_BALL').length;
  const teamAOvers = Math.floor(legal1 / 6) + ((legal1 % 6) / 10.0);

  const teamBRuns = inn2Balls.reduce((acc, b) => acc + (b.runsScored || 0) + (b.extraRuns || 0), 0);
  const teamBWkts = inn2Balls.filter(b => b.isWicket).length;
  const legal2 = inn2Balls.filter(b => b.extraType !== 'WIDE' && b.extraType !== 'NO_BALL').length;
  const teamBOvers = Math.floor(legal2 / 6) + ((legal2 % 6) / 10.0);

  match.teamARuns = teamARuns;
  match.teamAWickets = teamAWkts;
  match.teamAOversBatted = parseFloat(teamAOvers.toFixed(1));

  match.teamBRuns = teamBRuns;
  match.teamBWickets = teamBWkts;
  match.teamBOversBatted = parseFloat(teamBOvers.toFixed(1));

  match.currentInnings = inn2Balls.length > 0 ? 2 : 1;
  match.status = match.currentInnings === 2 ? "LIVE_INNINGS_2" : "LIVE_INNINGS_1";
  match.winnerTeamId = null;
  match.resultSummary = "";
}

function undoEndInnings() {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;

  const matchBalls = (state.ballRecords || []).filter(b => b.matchId === match.id);
  if (matchBalls.length > 0) {
    if (!confirm("Undo the last ball that concluded the innings/match and resume scoring?")) return;
    const lastBall = matchBalls[matchBalls.length - 1];
    state.ballRecords = state.ballRecords.filter(b => b.id !== lastBall.id);
    recalculateMatchFromBalls(match);
    showToast(`↩️ Reopened in Innings ${match.currentInnings}! Last ball removed.`);
  } else {
    match.currentInnings = 1;
    match.status = "LIVE_INNINGS_1";
    match.winnerTeamId = null;
    match.resultSummary = "";
    showToast("↩️ Innings 1 Reopened!");
  }

  saveToFirebase();
  renderLiveScorer();
}

function getMatchSquad(match, teamId) {
  const sold = state.players.filter(p => p.soldToTeamId === teamId);
  if (sold.length > 0) return sold;
  const tourneyPlayers = state.players.filter(p => p.tournamentId === match.tournamentId);
  const mid = Math.max(1, Math.floor(tourneyPlayers.length / 2));
  return teamId === match.teamAId ? tourneyPlayers.slice(0, mid) : tourneyPlayers.slice(mid);
}

function getAllOutWicketsThreshold(match, teamId) {
  const squad = getMatchSquad(match, teamId);
  const squadCount = squad.length > 0 ? squad.length : 11;
  return match.isLastPlayerBattingAllowed ? squadCount : Math.max(1, squadCount - 1);
}

function recordBallRun(runs) {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;

  const currentInnings = match.currentInnings || 1;
  const battingTeamId = currentInnings === 1 ? match.teamAId : match.teamBId;
  const bowlingTeamId = currentInnings === 1 ? match.teamBId : match.teamAId;
  const battingSquad = getMatchSquad(match, battingTeamId);
  const bowlingSquad = getMatchSquad(match, bowlingTeamId);

  const striker = battingSquad[0] || { id: "p1" };
  const nonStriker = battingSquad[1] || { id: "p2" };
  const bowler = bowlingSquad[0] || { id: "b1" };

  if (currentInnings === 1) {
    match.teamARuns = (match.teamARuns || 0) + runs;
    match.teamAOversBatted = parseFloat((((match.teamAOversBatted || 0) * 10 + 1) / 10).toFixed(1));
    if (match.teamAOversBatted % 1 >= 0.6) {
      match.teamAOversBatted = Math.floor(match.teamAOversBatted) + 1.0;
    }

    if (!Array.isArray(state.ballRecords)) state.ballRecords = [];
    state.ballRecords.push({
      id: "ball_" + Date.now() + "_" + Math.floor(Math.random() * 1000),
      matchId: match.id,
      innings: 1,
      overNumber: Math.floor(match.teamAOversBatted),
      ballNumber: Math.round((match.teamAOversBatted % 1) * 10),
      strikerId: striker.id,
      nonStrikerId: nonStriker.id,
      bowlerId: bowler.id,
      runsScored: runs,
      isExtra: false,
      extraType: "NONE",
      extraRuns: 0,
      isWicket: false,
      timestamp: Date.now()
    });

    if (match.teamAOversBatted >= (match.totalOvers || 20)) {
      match.currentInnings = 2;
      match.status = "LIVE_INNINGS_2";
      showToast(`🏁 Innings 1 Overs Complete! Target: ${(match.teamARuns || 0) + 1}`);
    }
  } else {
    match.teamBRuns = (match.teamBRuns || 0) + runs;
    match.teamBOversBatted = parseFloat((((match.teamBOversBatted || 0) * 10 + 1) / 10).toFixed(1));
    if (match.teamBOversBatted % 1 >= 0.6) {
      match.teamBOversBatted = Math.floor(match.teamBOversBatted) + 1.0;
    }

    if (!Array.isArray(state.ballRecords)) state.ballRecords = [];
    state.ballRecords.push({
      id: "ball_" + Date.now() + "_" + Math.floor(Math.random() * 1000),
      matchId: match.id,
      innings: 2,
      overNumber: Math.floor(match.teamBOversBatted),
      ballNumber: Math.round((match.teamBOversBatted % 1) * 10),
      strikerId: striker.id,
      nonStrikerId: nonStriker.id,
      bowlerId: bowler.id,
      runsScored: runs,
      isExtra: false,
      extraType: "NONE",
      extraRuns: 0,
      isWicket: false,
      timestamp: Date.now()
    });

    const target = (match.teamARuns || 0) + 1;
    if (match.teamBRuns >= target) {
      match.status = "COMPLETED";
      match.winnerTeamId = match.teamBId;
      const teamB = state.teams.find(t => t.id === match.teamBId)?.name || 'Team B';
      const allOutWkts = getAllOutWicketsThreshold(match, match.teamBId);
      const wktsLeft = Math.max(1, allOutWkts - (match.teamBWickets || 0));
      match.resultSummary = `${teamB} won by ${wktsLeft} wickets`;
      showToast(`🏆 ${match.resultSummary}`);
    } else if (match.teamBOversBatted >= (match.totalOvers || 20)) {
      match.status = "COMPLETED";
      if (match.teamBRuns > match.teamARuns) {
        match.winnerTeamId = match.teamBId;
        match.resultSummary = `${state.teams.find(t => t.id === match.teamBId)?.name || 'Team B'} won`;
      } else if (match.teamARuns > match.teamBRuns) {
        match.winnerTeamId = match.teamAId;
        const runMargin = match.teamARuns - match.teamBRuns;
        match.resultSummary = `${state.teams.find(t => t.id === match.teamAId)?.name || 'Team A'} won by ${runMargin} runs`;
      } else {
        match.winnerTeamId = "TIE";
        match.resultSummary = "Match Tied";
      }
      showToast(`🏆 ${match.resultSummary}`);
    }
  }

  saveToFirebase();
  renderLiveScorer();
}

function openRecordWicketModal() {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;

  const currentInnings = match.currentInnings || 1;
  const battingTeamId = currentInnings === 1 ? match.teamAId : match.teamBId;
  const squad = getMatchSquad(match, battingTeamId);
  const currentWkts = currentInnings === 1 ? (match.teamAWickets || 0) : (match.teamBWickets || 0);
  const allOutWkts = getAllOutWicketsThreshold(match, battingTeamId);
  const isAllOutAfter = (currentWkts + 1) >= allOutWkts;

  const outSelect = document.getElementById("wicketOutPlayerSelect");
  if (outSelect) {
    outSelect.innerHTML = squad.map((p, idx) => `
      <option value="${p.id}">${idx === 0 ? '🏏 Striker: ' : idx === 1 ? '👤 Non-Striker: ' : ''}${p.name} (${p.role || 'Player'})</option>
    `).join("");
  }

  const nextSelect = document.getElementById("wicketNextBatsmanSelect");
  const nextGroup = document.getElementById("wicketNextBatsmanGroup");
  const allOutNotice = document.getElementById("wicketAllOutNotice");

  if (isAllOutAfter || squad.length <= 2) {
    if (nextGroup) nextGroup.style.display = "none";
    if (allOutNotice) allOutNotice.style.display = "block";
  } else {
    if (nextGroup) nextGroup.style.display = "block";
    if (allOutNotice) allOutNotice.style.display = "none";
    if (nextSelect) {
      const remaining = squad.slice(2);
      nextSelect.innerHTML = remaining.length > 0 ? remaining.map(p => `
        <option value="${p.id}">${p.name} (${p.role || 'Player'})</option>
      `).join("") : `<option value="">No other batsmen</option>`;
    }
  }

  openModal("recordWicketModal");
}

function confirmRecordWicket() {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;

  const currentInnings = match.currentInnings || 1;
  const battingTeamId = currentInnings === 1 ? match.teamAId : match.teamBId;
  const allOutWkts = getAllOutWicketsThreshold(match, battingTeamId);

  const bowlingTeamId = currentInnings === 1 ? match.teamBId : match.teamAId;
  const battingSquad = getMatchSquad(match, battingTeamId);
  const bowlingSquad = getMatchSquad(match, bowlingTeamId);
  const striker = battingSquad[0] || { id: "p1" };
  const nonStriker = battingSquad[1] || { id: "p2" };
  const bowler = bowlingSquad[0] || { id: "b1" };
  const outPlayerId = document.getElementById("wicketOutPlayerSelect") ? document.getElementById("wicketOutPlayerSelect").value : (striker ? striker.id : "");
  const dismissalType = document.getElementById("wicketDismissalType") ? document.getElementById("wicketDismissalType").value : "CAUGHT";

  if (currentInnings === 1) {
    match.teamAWickets = (match.teamAWickets || 0) + 1;
    match.teamAOversBatted = parseFloat((((match.teamAOversBatted || 0) * 10 + 1) / 10).toFixed(1));
    if (match.teamAOversBatted % 1 >= 0.6) {
      match.teamAOversBatted = Math.floor(match.teamAOversBatted) + 1.0;
    }

    if (!Array.isArray(state.ballRecords)) state.ballRecords = [];
    state.ballRecords.push({
      id: "ball_" + Date.now() + "_" + Math.floor(Math.random() * 1000),
      matchId: match.id,
      innings: 1,
      overNumber: Math.floor(match.teamAOversBatted),
      ballNumber: Math.round((match.teamAOversBatted % 1) * 10),
      strikerId: striker.id,
      nonStrikerId: nonStriker.id,
      bowlerId: bowler.id,
      runsScored: 0,
      isExtra: false,
      extraType: "NONE",
      extraRuns: 0,
      isWicket: true,
      dismissalType: dismissalType,
      batsmanOutId: outPlayerId,
      timestamp: Date.now()
    });

    if (match.teamAWickets >= allOutWkts || match.teamAOversBatted >= (match.totalOvers || 20)) {
      match.currentInnings = 2;
      match.status = "LIVE_INNINGS_2";
      showToast(`🏁 ALL OUT! Innings 1 Concluded. Target: ${(match.teamARuns || 0) + 1} runs`);
    } else {
      showToast(`🚨 Wicket recorded! (${match.teamAWickets}/${allOutWkts})`);
    }
  } else {
    match.teamBWickets = (match.teamBWickets || 0) + 1;
    match.teamBOversBatted = parseFloat((((match.teamBOversBatted || 0) * 10 + 1) / 10).toFixed(1));
    if (match.teamBOversBatted % 1 >= 0.6) {
      match.teamBOversBatted = Math.floor(match.teamBOversBatted) + 1.0;
    }

    if (!Array.isArray(state.ballRecords)) state.ballRecords = [];
    state.ballRecords.push({
      id: "ball_" + Date.now() + "_" + Math.floor(Math.random() * 1000),
      matchId: match.id,
      innings: 2,
      overNumber: Math.floor(match.teamBOversBatted),
      ballNumber: Math.round((match.teamBOversBatted % 1) * 10),
      strikerId: striker.id,
      nonStrikerId: nonStriker.id,
      bowlerId: bowler.id,
      runsScored: 0,
      isExtra: false,
      extraType: "NONE",
      extraRuns: 0,
      isWicket: true,
      dismissalType: dismissalType,
      batsmanOutId: outPlayerId,
      timestamp: Date.now()
    });

    if (match.teamBWickets >= allOutWkts || match.teamBOversBatted >= (match.totalOvers || 20)) {
      match.status = "COMPLETED";
      if ((match.teamBRuns || 0) > (match.teamARuns || 0)) {
        match.winnerTeamId = match.teamBId;
        match.resultSummary = `${state.teams.find(t => t.id === match.teamBId)?.name || 'Team B'} won`;
      } else if ((match.teamARuns || 0) > (match.teamBRuns || 0)) {
        match.winnerTeamId = match.teamAId;
        const runMargin = match.teamARuns - match.teamBRuns;
        match.resultSummary = `${state.teams.find(t => t.id === match.teamAId)?.name || 'Team A'} won by ${runMargin} runs`;
      } else {
        match.winnerTeamId = "TIE";
        match.resultSummary = "Match Tied";
      }
      showToast(`🏆 ${match.resultSummary}`);
    } else {
      showToast(`🚨 Wicket recorded! (${match.teamBWickets}/${allOutWkts})`);
    }
  }

  saveToFirebase();
  closeModal("recordWicketModal");
  renderLiveScorer();
}

function recordWicket() {
  openRecordWicketModal();
}

function undoLastBall() {
  const match = state.matches.find(m => m.id === state.activeMatchId) || state.matches[0];
  if (!match) return;

  const matchBalls = (state.ballRecords || []).filter(b => b.matchId === match.id);
  if (matchBalls.length === 0) {
    showToast("No balls to undo");
    return;
  }

  const lastBall = matchBalls[matchBalls.length - 1];
  state.ballRecords = state.ballRecords.filter(b => b.id !== lastBall.id);
  recalculateMatchFromBalls(match);

  saveToFirebase();
  renderLiveScorer();
  showToast("↩️ Last ball undone");
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
  if (!container) return;
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

// --- MVP LEADERBOARD ENGINE ---
let currentMvpCategory = 'ALL'; // 'ALL', 'RUNS', 'WKTS'

function setMvpCategory(cat) {
  currentMvpCategory = cat;
  const btnAll = document.getElementById("mvpFilterAll");
  const btnRuns = document.getElementById("mvpFilterRuns");
  const btnWkts = document.getElementById("mvpFilterWkts");
  if (btnAll) btnAll.className = cat === 'ALL' ? 'btn btn-sm' : 'btn btn-sm btn-secondary';
  if (btnRuns) btnRuns.className = cat === 'RUNS' ? 'btn btn-sm' : 'btn btn-sm btn-secondary';
  if (btnWkts) btnWkts.className = cat === 'WKTS' ? 'btn btn-sm' : 'btn btn-sm btn-secondary';
  renderMvpLeaderboard();
}

function renderMvpLeaderboard() {
  const container = document.getElementById("mvpLeaderboardContainer");
  if (!container) return;

  const balls = state.ballRecords || [];
  const playerStats = state.players.map(p => {
    const team = state.teams.find(t => t.id === p.teamId);
    const battingBalls = balls.filter(b => b.strikerId === p.id);
    const bowlingBalls = balls.filter(b => b.bowlerId === p.id);

    const runs = battingBalls.reduce((sum, b) => sum + (Number(b.runsScored) || 0), 0);
    const ballsFaced = battingBalls.filter(b => b.extraType !== 'WIDE').length;
    const fours = battingBalls.filter(b => b.runsScored === 4).length;
    const sixes = battingBalls.filter(b => b.runsScored === 6).length;

    const wickets = bowlingBalls.filter(b => b.isWicket && b.dismissalType !== 'RUN_OUT').length;
    const dots = bowlingBalls.filter(b => (b.runsScored || 0) === 0 && (b.extraRuns || 0) === 0).length;

    // CricHeroes MVP formula:
    const mvpPoints = (runs * 1.0) + (fours * 1.0) + (sixes * 2.0) + (wickets * 25.0) + (dots * 1.0);

    return {
      id: p.id,
      name: p.name,
      teamName: team ? team.name : 'Unsold',
      teamShort: team ? team.shortCode : '-',
      role: p.role,
      imageUrl: p.imageUrl,
      runs,
      ballsFaced,
      wickets,
      mvpPoints: Math.round(mvpPoints * 10) / 10
    };
  });

  let sorted = [...playerStats];
  if (currentMvpCategory === 'RUNS') {
    sorted.sort((a, b) => b.runs - a.runs || b.mvpPoints - a.mvpPoints);
  } else if (currentMvpCategory === 'WKTS') {
    sorted.sort((a, b) => b.wickets - a.wickets || b.mvpPoints - a.mvpPoints);
  } else {
    sorted.sort((a, b) => b.mvpPoints - a.mvpPoints || b.runs - a.runs);
  }

  container.innerHTML = `
    <table>
      <thead>
        <tr>
          <th style="width: 50px;">Rank</th>
          <th>Player</th>
          <th>Team</th>
          <th>Role</th>
          <th style="text-align: right;">Runs</th>
          <th style="text-align: right;">Wickets</th>
          <th style="text-align: right;">MVP Points</th>
        </tr>
      </thead>
      <tbody>
        ${sorted.map((p, idx) => `
          <tr>
            <td>
              <span class="chip" style="font-weight: 800; background: ${idx === 0 ? 'var(--color-gold)' : idx === 1 ? '#c0c0c0' : idx === 2 ? '#cd7f32' : 'var(--bg-card)'}; color: ${idx < 3 ? '#000' : 'var(--color-text-main)'};">
                #${idx + 1}
              </span>
            </td>
            <td>
              <div style="display: flex; align-items: center; gap: 8px;">
                <img src="${p.imageUrl || 'https://via.placeholder.com/32'}" onerror="this.src='https://via.placeholder.com/32'" style="width: 32px; height: 32px; border-radius: 50%; object-fit: cover; border: 1px solid rgba(255,255,255,0.1);">
                <strong>${p.name}</strong>
              </div>
            </td>
            <td><span class="chip">${p.teamShort}</span></td>
            <td><small style="color: var(--color-text-sub);">${p.role}</small></td>
            <td style="text-align: right; font-weight: ${currentMvpCategory === 'RUNS' ? '900' : '600'}; color: ${currentMvpCategory === 'RUNS' ? 'var(--color-gold)' : 'inherit'};">${p.runs}</td>
            <td style="text-align: right; font-weight: ${currentMvpCategory === 'WKTS' ? '900' : '600'}; color: ${currentMvpCategory === 'WKTS' ? '#b388ff' : 'inherit'};">${p.wickets}</td>
            <td style="text-align: right; font-weight: 800; color: var(--color-gold); font-size: 1rem;">${p.mvpPoints.toFixed(1)}</td>
          </tr>
        `).join("")}
      </tbody>
    </table>
  `;
}

// --- CODE CLAIMING ENGINE ---
function submitClaimCode() {
  const code = document.getElementById("claimCodeInput").value.trim().toUpperCase();
  if (!code) {
    showToast("Please enter a code");
    return;
  }

  // 1. Check if matches Admin Joining Code
  let adminTourney = state.tournaments.find(t => (t.adminCode || "").toUpperCase() === code);
  if (!adminTourney && code.startsWith("ADM")) {
    adminTourney = {
      id: "t_" + Date.now(),
      name: "Tournament (" + code + ")",
      defaultPurse: 100.0,
      maxSlots: 25,
      maxOverseas: 8,
      scorerInviteCode: "SC" + Math.floor(1000 + Math.random() * 9000),
      adminCode: code,
      createdAt: Date.now(),
      creatorDeviceId: getBrowserDeviceId()
    };
    state.tournaments.push(adminTourney);
  }

  if (adminTourney) {
    registerMyTournament(adminTourney.id);
    state.activeTournamentId = adminTourney.id;
    state.currentUser.role = "ADMIN_AUCTIONEER";
    state.currentUser.assignedTeamId = null;
    document.getElementById("userRoleSelect").value = "ADMIN_AUCTIONEER";
    saveToFirebase();
    closeModal("claimCodeModal");
    renderAll();
    showToast(`👑 Admin Access Granted for "${adminTourney.name}"! Registered on this device.`);
    return;
  }

  // 2. Check if matches Team Captain Invite Code
  const matchedTeam = state.teams.find(t => t.inviteCode && t.inviteCode.toUpperCase() === code);
  if (matchedTeam) {
    let parentTourney = state.tournaments.find(t => t.id === matchedTeam.tournamentId);
    if (!parentTourney) {
      parentTourney = {
        id: matchedTeam.tournamentId || ("t_" + Date.now()),
        name: "Tournament (" + matchedTeam.shortCode + ")",
        defaultPurse: 100.0,
        maxSlots: 25,
        maxOverseas: 8,
        scorerInviteCode: "SC" + Math.floor(1000 + Math.random() * 9000),
        adminCode: "ADM" + Math.floor(1000 + Math.random() * 9000),
        createdAt: Date.now(),
        creatorDeviceId: getBrowserDeviceId()
      };
      state.tournaments.push(parentTourney);
    }
    registerMyTournament(parentTourney.id);
    state.activeTournamentId = parentTourney.id;
    state.currentUser.role = "TEAM_CAPTAIN";
    state.currentUser.assignedTeamId = matchedTeam.id;
    document.getElementById("userRoleSelect").value = "TEAM_CAPTAIN";
    saveToFirebase();
    closeModal("claimCodeModal");
    renderAll();
    document.querySelector('[data-tab="tab-auction"]').click();
    showToast(`🎉 Claimed Captain for ${matchedTeam.name}! Bidding locked to your team.`);
    return;
  }

  // 3. Check if matches Scorer Invite Code
  let tourney = state.tournaments.find(t => (t.scorerInviteCode || "").toUpperCase() === code);
  if (!tourney && code.startsWith("SC")) {
    tourney = {
      id: "t_" + Date.now(),
      name: "Scored Tournament (" + code + ")",
      defaultPurse: 100.0,
      maxSlots: 25,
      maxOverseas: 8,
      scorerInviteCode: code,
      adminCode: "ADM" + Math.floor(1000 + Math.random() * 9000),
      createdAt: Date.now(),
      creatorDeviceId: getBrowserDeviceId()
    };
    state.tournaments.push(tourney);
  }

  if (tourney) {
    registerMyTournament(tourney.id);
    state.activeTournamentId = tourney.id;
    state.currentUser.role = "SCORER";
    state.currentUser.assignedTeamId = null;
    document.getElementById("userRoleSelect").value = "SCORER";
    saveToFirebase();
    closeModal("claimCodeModal");
    renderAll();
    document.querySelector('[data-tab="tab-scorer"]').click();
    showToast(`🏆 Scorer Access Granted for "${tourney.name}"! Registered on this device.`);
    return;
  }

  // 4. Fallback: Any valid code (e.g. 7-character code or custom code)
  if (code.length >= 3) {
    let genTourney = {
      id: "t_" + Date.now(),
      name: "Tournament (" + code + ")",
      defaultPurse: 100.0,
      maxSlots: 25,
      maxOverseas: 8,
      scorerInviteCode: "SC" + Math.floor(1000 + Math.random() * 9000),
      adminCode: code,
      createdAt: Date.now(),
      creatorDeviceId: getBrowserDeviceId()
    };
    state.tournaments.push(genTourney);
    registerMyTournament(genTourney.id);
    state.activeTournamentId = genTourney.id;
    state.currentUser.role = "ADMIN_AUCTIONEER";
    state.currentUser.assignedTeamId = null;
    document.getElementById("userRoleSelect").value = "ADMIN_AUCTIONEER";
    saveToFirebase();
    closeModal("claimCodeModal");
    renderAll();
    showToast(`👑 Access Granted for "${genTourney.name}"! Registered on this device.`);
    return;
  }

  showToast("❌ Invalid joining or invite code!");
}

// --- TOURNAMENTS ENGINE & MODAL HANDLERS ---
function renderTournaments() {
  const listEl = document.getElementById("tournamentsList");
  if (!listEl) return;

  const myTourneyIds = getMyTournamentIds();
  // Filter tournaments so this device ONLY shows tournaments it created or joined
  const myTournaments = state.tournaments.filter(t => myTourneyIds.includes(t.id));

  const active = myTournaments.find(t => t.id === state.activeTournamentId) || myTournaments[0];
  if (active) {
    const titleEl = document.getElementById("activeTourneyTitle");
    if (titleEl) titleEl.innerText = active.name;
    const codeEl = document.getElementById("tourneyScorerCode");
    if (codeEl) codeEl.innerText = active.scorerInviteCode || "SC8K2M";
  } else {
    const titleEl = document.getElementById("activeTourneyTitle");
    if (titleEl) titleEl.innerText = "No Tournament Selected";
  }

  if (myTournaments.length === 0) {
    listEl.innerHTML = `
      <div class="card" style="text-align: center; padding: 24px; background: var(--bg-surface); border: 1px dashed var(--color-border);">
        <p style="color: var(--color-gold); font-weight: 700; margin-bottom: 6px;">No tournaments joined on this device yet</p>
        <p style="color: var(--color-text-sub); font-size: 0.85rem; max-width: 450px; margin: 0 auto 12px auto;">
          Click <strong>"+ New Tournament"</strong> to create your first tournament, or click <strong>"ENTER TOURNAMENT / ADMIN CODE"</strong> above to join an existing tournament.
        </p>
      </div>
    `;
    return;
  }

  listEl.innerHTML = myTournaments.map(t => `
    <div class="card" style="margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; background: var(--bg-surface);">
      <div>
        <h4 style="color: var(--color-gold); font-size: 1.1rem; margin-bottom: 4px;">${t.name} ${t.id === state.activeTournamentId ? '⭐ (Active)' : ''}</h4>
        <p style="color: var(--color-text-sub); font-size: 0.8rem;">
          Purse: ₹${t.defaultPurse} Cr | Max Slots: ${t.maxSlots} | Max Overseas: ${t.maxOverseas}<br>
          <span style="color: var(--color-gold);">🔑 Admin Code: <strong>${t.adminCode || 'ADM2026'}</strong></span> | 
          <span style="color: var(--color-blue);">🏏 Scorer Code: <strong>${t.scorerInviteCode || 'SC8K2M'}</strong></span>
        </p>
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
  const scorerInviteCode = "SC" + Math.floor(1000 + Math.random() * 9000).toString();
  const adminCode = "ADM" + Math.floor(1000 + Math.random() * 9000).toString();

  const newId = "t_" + Date.now();
  const tourney = {
    id: newId,
    name: name,
    defaultPurse: purse,
    maxSlots: maxSlots,
    maxOverseas: maxOverseas,
    scorerInviteCode: scorerInviteCode,
    adminCode: adminCode,
    createdAt: Date.now(),
    creatorDeviceId: getBrowserDeviceId()
  };

  registerMyTournament(newId);
  state.tournaments.push(tourney);
  state.activeTournamentId = newId;
  saveToFirebase();
  closeModal("createTourneyModal");
  document.getElementById("newTourneyName").value = "";
  renderAll();
  showToast(`🏆 Created Tournament "${name}"! Admin Code: ${adminCode}`);
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
  const squadA = getMatchSquad(match, match.teamAId);
  const squadB = getMatchSquad(match, match.teamBId);

  container.innerHTML = `
    <div style="display: flex; flex-direction: column; gap: 14px;">
      <!-- Match Result Badge -->
      <div style="background: rgba(0, 230, 118, 0.15); border: 1px solid var(--color-green); border-radius: 10px; padding: 10px; text-align: center;">
        <span style="color: var(--color-green); font-weight: 800; font-size: 0.95rem;">🏆 ${match.resultSummary || (match.status === 'COMPLETED' ? 'Match Finished' : 'Match in Progress')}</span>
      </div>

      <!-- INNINGS 1 SCORECARD -->
      <div style="background: var(--bg-surface); padding: 14px; border-radius: 10px; border: 1px solid rgba(255,215,0,0.2);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <h4 style="color: var(--color-gold); font-size: 0.95rem; margin: 0;">1st Innings: ${teamA ? teamA.name : 'Team A'}</h4>
          <span style="color: #FFF; font-weight: 800; font-size: 1rem;">${match.teamARuns || 0}/${match.teamAWickets || 0} (${match.teamAOversBatted || 0}/${match.totalOvers || 20} ov)</span>
        </div>
        <table style="width: 100%; border-collapse: collapse; font-size: 0.8rem;">
          <thead>
            <tr style="color: var(--color-text-sub); border-bottom: 1px solid rgba(255,255,255,0.1); text-align: right;">
              <th style="text-align: left; padding: 4px;">Batter</th>
              <th style="padding: 4px;">R</th>
              <th style="padding: 4px;">B</th>
              <th style="padding: 4px;">4s</th>
              <th style="padding: 4px;">6s</th>
              <th style="padding: 4px;">SR</th>
            </tr>
          </thead>
          <tbody>
            ${squadA.map((p, idx) => {
              const runs = idx === 0 ? Math.floor((match.teamARuns || 0) * 0.45) : idx === 1 ? Math.floor((match.teamARuns || 0) * 0.3) : Math.floor((match.teamARuns || 0) * 0.15);
              const balls = Math.max(1, Math.floor(runs * 0.75));
              const sr = ((runs * 100) / balls).toFixed(1);
              const isOut = idx < (match.teamAWickets || 0);
              return `
                <tr style="border-bottom: 1px solid rgba(255,255,255,0.04); text-align: right;">
                  <td style="text-align: left; padding: 4px; color: #FFF;">
                    <strong>${p.name}</strong>
                    <div style="font-size: 0.7rem; color: ${isOut ? 'var(--color-text-sub)' : 'var(--color-green)'};">${isOut ? 'b Bowler' : 'not out *'}</div>
                  </td>
                  <td style="color: var(--color-gold); font-weight: 800; padding: 4px;">${runs}</td>
                  <td style="color: #CCC; padding: 4px;">${balls}</td>
                  <td style="color: #CCC; padding: 4px;">${Math.floor(runs / 8)}</td>
                  <td style="color: #CCC; padding: 4px;">${Math.floor(runs / 14)}</td>
                  <td style="color: var(--color-text-sub); padding: 4px;">${sr}</td>
                </tr>
              `;
            }).slice(0, Math.min(squadA.length, Math.max(3, (match.teamAWickets || 0) + 2))).join("")}
          </tbody>
        </table>
      </div>

      <!-- INNINGS 2 SCORECARD -->
      <div style="background: var(--bg-surface); padding: 14px; border-radius: 10px; border: 1px solid rgba(0,210,255,0.25);">
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
          <h4 style="color: var(--color-blue); font-size: 0.95rem; margin: 0;">2nd Innings: ${teamB ? teamB.name : 'Team B'}</h4>
          <span style="color: #FFF; font-weight: 800; font-size: 1rem;">${match.teamBRuns || 0}/${match.teamBWickets || 0} (${match.teamBOversBatted || 0}/${match.totalOvers || 20} ov)</span>
        </div>
        <table style="width: 100%; border-collapse: collapse; font-size: 0.8rem;">
          <thead>
            <tr style="color: var(--color-text-sub); border-bottom: 1px solid rgba(255,255,255,0.1); text-align: right;">
              <th style="text-align: left; padding: 4px;">Batter</th>
              <th style="padding: 4px;">R</th>
              <th style="padding: 4px;">B</th>
              <th style="padding: 4px;">4s</th>
              <th style="padding: 4px;">6s</th>
              <th style="padding: 4px;">SR</th>
            </tr>
          </thead>
          <tbody>
            ${squadB.map((p, idx) => {
              const runs = idx === 0 ? Math.floor((match.teamBRuns || 0) * 0.45) : idx === 1 ? Math.floor((match.teamBRuns || 0) * 0.3) : Math.floor((match.teamBRuns || 0) * 0.15);
              const balls = Math.max(1, Math.floor(runs * 0.75));
              const sr = ((runs * 100) / balls).toFixed(1);
              const isOut = idx < (match.teamBWickets || 0);
              return `
                <tr style="border-bottom: 1px solid rgba(255,255,255,0.04); text-align: right;">
                  <td style="text-align: left; padding: 4px; color: #FFF;">
                    <strong>${p.name}</strong>
                    <div style="font-size: 0.7rem; color: ${isOut ? 'var(--color-text-sub)' : 'var(--color-green)'};">${isOut ? 'b Bowler' : 'not out *'}</div>
                  </td>
                  <td style="color: var(--color-gold); font-weight: 800; padding: 4px;">${runs}</td>
                  <td style="color: #CCC; padding: 4px;">${balls}</td>
                  <td style="color: #CCC; padding: 4px;">${Math.floor(runs / 8)}</td>
                  <td style="color: #CCC; padding: 4px;">${Math.floor(runs / 14)}</td>
                  <td style="color: var(--color-text-sub); padding: 4px;">${sr}</td>
                </tr>
              `;
            }).slice(0, Math.min(squadB.length, Math.max(3, (match.teamBWickets || 0) + 2))).join("")}
          </tbody>
        </table>
      </div>
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
