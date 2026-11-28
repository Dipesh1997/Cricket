package cricket.player.auction.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.data.remote.LiveStreamSessionManager
import cricket.player.auction.model.*
import cricket.player.auction.ui.components.*
import cricket.player.auction.ui.theme.*
import cricket.player.auction.util.CricketShareHelper
import cricket.player.auction.viewmodel.AuctionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

fun extractYouTubeVideoId(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.length == 11 && !trimmed.contains("/") && !trimmed.contains("?") && !trimmed.contains("&")) {
        return trimmed
    }
    val youtuBeRegex = Regex("""(?:https?://)?(?:www\.)?youtu\.be/([a-zA-Z0-9_-]{11})""")
    youtuBeRegex.find(trimmed)?.let { return it.groupValues[1] }

    val watchRegex = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""")
    watchRegex.find(trimmed)?.let { return it.groupValues[1] }

    val liveRegex = Regex("""(?:live|embed)/([a-zA-Z0-9_-]{11})""")
    liveRegex.find(trimmed)?.let { return it.groupValues[1] }

    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreRecorderScreen(viewModel: AuctionViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activeTournament by viewModel.activeTournament.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val players by viewModel.players.collectAsState()
    val activeMatchState by viewModel.activeMatch.collectAsState()
    val activeMatch = activeMatchState
    val ballRecords by viewModel.ballRecords.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val isSpectator = currentUser?.role == UserRole.SPECTATOR
    val canScore = remember(currentUser) {
        currentUser?.role == UserRole.ADMIN_AUCTIONEER || currentUser?.role == UserRole.SCORER
    }

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary
    val textGold = if (isDarkTheme) IplGold else LightPitchText
    val textBlue = if (isDarkTheme) NeonBlue else LightPitchText
    val accentNavy = if (isDarkTheme) NeonBlue else LightPitchPrimary

    val matches by viewModel.matches.collectAsState()
    val pendingScorerLaunch by viewModel.pendingScorerLaunch.collectAsState()
    val tourneyMatches = remember(matches, activeTournament) {
        matches.filter { it.tournamentId == activeTournament?.id }
    }
    val isEditable = remember(activeMatch, matches) {
        activeMatch?.let { viewModel.isMatchEditable(it) } ?: true
    }

    var isViewingLiveMatch by remember { mutableStateOf(false) }
    var scorerUiMode by remember { mutableStateOf("CLASSIC") } // "CLASSIC" (Crichunt), "MODERN" (Current), "SPECTATOR" (Cricbuzz Preview)
    var matchFilter by remember { mutableStateOf("ALL") }
    var showCreateMatchModal by remember { mutableStateOf(false) }
    var showCoinTossModal by remember { mutableStateOf(false) }
    var showWicketModal by remember { mutableStateOf(false) }
    var selectedMatchForScorecard by remember { mutableStateOf<Match?>(null) }
    var showDeclareWinnerPopup by remember { mutableStateOf(false) }
    var lastConcludedMatchId by remember { mutableStateOf<String?>(null) }
    var showTargetModal by remember { mutableStateOf(false) }
    var showEndInningsConfirmModal by remember { mutableStateOf(false) }
    var showUndoEndInningsConfirmModal by remember { mutableStateOf(false) }
    var showChangeBowlerModal by remember { mutableStateOf(false) }
    var lastCompletedOverBowlerId by remember { mutableStateOf<String?>(null) }
    var showLiveStreamDialog by remember { mutableStateOf(false) }
    var showLiveStreamCard by remember { mutableStateOf(true) }
    var streamUrlInput by remember { mutableStateOf("") }
    var showCameraStreamPreview by remember { mutableStateOf(false) }
    var isBackgroundCameraOn by remember { mutableStateOf(false) }
    var showDirectEndMatchModal by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val camGranted = permissions[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (camGranted) {
            showLiveStreamDialog = false
            showCameraStreamPreview = true
        } else {
            Toast.makeText(context, "📷 Camera permission is required to start live camera streaming.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(pendingScorerLaunch) {
        val req = pendingScorerLaunch
        if (req != null) {
            scorerUiMode = req.mode
            if (req.openLive && activeMatch != null) {
                isViewingLiveMatch = true
            }
            if (req.openYouTubeDialog) {
                streamUrlInput = activeMatch?.liveStreamUrl ?: ""
                showLiveStreamDialog = true
            }
            viewModel.consumeScorerLaunch()
        }
    }

    // Match Setup Inputs
    var selectedTeamAId by remember { mutableStateOf("") }
    var selectedTeamBId by remember { mutableStateOf("") }
    var matchOversInput by remember { mutableStateOf("5") }
    var isLastPlayerBattingAllowed by remember { mutableStateOf(true) }

    val attemptCreateMatch: () -> Unit = {
        when {
            teams.size < 2 -> {
                Toast.makeText(context, "⚠️ Create at least 2 teams before starting a match.", Toast.LENGTH_LONG).show()
            }
            players.size < 2 -> {
                Toast.makeText(context, "⚠️ Add at least 2 players to the tournament before starting a match.", Toast.LENGTH_LONG).show()
            }
            else -> {
                showCreateMatchModal = true
            }
        }
    }

    // Player Selection for Scorer
    var strikerId by remember { mutableStateOf<String?>(null) }
    var nonStrikerId by remember { mutableStateOf<String?>(null) }
    var bowlerId by remember { mutableStateOf<String?>(null) }
    var selectedExtraType by remember { mutableStateOf(ExtraType.NONE) }

    val matchBalls = remember(activeMatch, ballRecords) {
        ballRecords.filter { it.matchId == activeMatch?.id }
    }

    val currentInnings = activeMatch?.currentInnings ?: 1
    val currentInningsBalls = matchBalls.filter { it.innings == currentInnings }

    val battingTeamId = activeMatch?.let {
        if (currentInnings == 1) it.teamAId else it.teamBId
    } ?: ""

    val bowlingTeamId = activeMatch?.let {
        if (currentInnings == 1) it.teamBId else it.teamAId
    } ?: ""

    val battingTeam = teams.find { it.id == battingTeamId }
    val bowlingTeam = teams.find { it.id == bowlingTeamId }

    // Strictly filter players by team! Batting batsmen from batting team, Bowler from opponent bowling team!
    val allTourneyPlayers = players.filter { it.tournamentId == activeMatch?.tournamentId }
    val soldBatting = players.filter { it.soldToTeamId == battingTeamId }
    val battingPlayers = if (soldBatting.isNotEmpty()) {
        soldBatting
    } else {
        if (battingTeamId == activeMatch?.teamAId) {
            allTourneyPlayers.take((allTourneyPlayers.size / 2).coerceAtLeast(1)).ifEmpty { allTourneyPlayers }
        } else {
            allTourneyPlayers.drop((allTourneyPlayers.size / 2).coerceAtLeast(1)).ifEmpty { allTourneyPlayers }
        }
    }

    val soldBowling = players.filter { it.soldToTeamId == bowlingTeamId }
    val bowlingPlayers = if (soldBowling.isNotEmpty()) {
        soldBowling
    } else {
        if (bowlingTeamId == activeMatch?.teamAId) {
            allTourneyPlayers.take((allTourneyPlayers.size / 2).coerceAtLeast(1)).ifEmpty { allTourneyPlayers }
        } else {
            allTourneyPlayers.drop((allTourneyPlayers.size / 2).coerceAtLeast(1)).ifEmpty { allTourneyPlayers }
        }
    }

    val striker = players.find { it.id == strikerId }
    val nonStriker = players.find { it.id == nonStrikerId }
    val bowler = players.find { it.id == bowlerId }

    // Restore playing batters & bowler from ball history on resume/switch, or require explicit selection
    var lastObservedInnings by remember { mutableStateOf(currentInnings) }
    var lastObservedMatchId by remember { mutableStateOf(activeMatch?.id) }
    LaunchedEffect(currentInnings, battingTeamId, bowlingTeamId, activeMatch?.id, isViewingLiveMatch) {
        val matchChanged = activeMatch?.id != lastObservedMatchId
        val inningsChanged = currentInnings != lastObservedInnings
        if (matchChanged || inningsChanged) {
            lastObservedMatchId = activeMatch?.id
            lastObservedInnings = currentInnings
            lastCompletedOverBowlerId = null
            strikerId = null
            nonStrikerId = null
            bowlerId = null
            if (inningsChanged && currentInnings == 2) {
                showTargetModal = true
                Toast.makeText(context, "🏁 Innings 1 Ended! 2nd Innings Started. Target: ${(activeMatch?.teamARuns ?: 0) + 1}", Toast.LENGTH_LONG).show()
            }
        }
        if (activeMatch != null && (strikerId == null || nonStrikerId == null || bowlerId == null)) {
            val lastBallOfInnings = currentInningsBalls.lastOrNull()
            if (lastBallOfInnings != null) {
                var restoredStriker: String? = lastBallOfInnings.strikerId.takeIf { it.isNotBlank() }
                var restoredNonStriker: String? = lastBallOfInnings.nonStrikerId?.takeIf { it.isNotBlank() }
                val isLegal = lastBallOfInnings.extraType != ExtraType.WIDE && lastBallOfInnings.extraType != ExtraType.NO_BALL
                val overEndedOnLastBall = isLegal && (lastBallOfInnings.ballNumber == 6)

                if (lastBallOfInnings.runsScored % 2 != 0 && restoredNonStriker != null) {
                    val tmp = restoredStriker
                    restoredStriker = restoredNonStriker
                    restoredNonStriker = tmp
                }
                if (overEndedOnLastBall && restoredNonStriker != null) {
                    val tmp = restoredStriker
                    restoredStriker = restoredNonStriker
                    restoredNonStriker = tmp
                }
                if (strikerId == null) strikerId = restoredStriker ?: battingPlayers.firstOrNull()?.id
                if (nonStrikerId == null) nonStrikerId = restoredNonStriker ?: battingPlayers.getOrNull(1)?.id ?: battingPlayers.firstOrNull()?.id

                val legalTotal = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                val oversDone = legalTotal >= activeMatch.totalOvers * 6
                if (overEndedOnLastBall && !oversDone) {
                    lastCompletedOverBowlerId = lastBallOfInnings.bowlerId
                    bowlerId = null
                } else if (bowlerId == null) {
                    bowlerId = lastBallOfInnings.bowlerId.takeIf { it.isNotBlank() }
                }
            } else {
                if (strikerId == null || battingPlayers.none { it.id == strikerId }) {
                    strikerId = battingPlayers.firstOrNull()?.id
                }
                if (nonStrikerId == null || battingPlayers.none { it.id == nonStrikerId }) {
                    nonStrikerId = battingPlayers.getOrNull(1)?.id ?: battingPlayers.firstOrNull()?.id
                }
                // Do NOT auto-assign bowlerId when no balls bowled yet — force explicit bowler selection
            }
        }
    }

    // Auto-show Target Modal on Innings 2 start
    LaunchedEffect(currentInnings, activeMatch?.status) {
        if (currentInnings == 2 && activeMatch?.status == MatchStatus.LIVE_INNINGS_2) {
            showTargetModal = true
        }
    }

    // Auto-show Declare Winner Modal when Match Completes naturally (and reset when resumed)
    LaunchedEffect(activeMatch?.id, activeMatch?.status) {
        if (activeMatch?.status == MatchStatus.COMPLETED && lastConcludedMatchId != activeMatch?.id) {
            showDeclareWinnerPopup = true
            lastConcludedMatchId = activeMatch?.id
        } else if (activeMatch?.status != MatchStatus.COMPLETED) {
            lastConcludedMatchId = null
        }
    }

    val isRtmpStreaming by LiveStreamSessionManager.isRtmpStreaming.collectAsState()

    LaunchedEffect(activeMatch, currentInningsBalls, striker, nonStriker, bowler, isRtmpStreaming) {
        val m = activeMatch
        if (m != null && isRtmpStreaming) {
            LiveStreamSessionManager.updateOverlay(
                buildMatchOverlayLines(
                    match = m,
                    teams = teams,
                    currentInningsBalls = currentInningsBalls,
                    striker = striker,
                    nonStriker = nonStriker,
                    bowler = bowler
                )
            )
        }
    }

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isBackgroundCameraOn && !showCameraStreamPreview && isViewingLiveMatch && activeMatch != null) {
                if (isRtmpStreaming) {
                    RtmpBackgroundPreviewLayer(modifier = Modifier.fillMaxSize())
                } else {
                    BackgroundCameraPreviewLayer(modifier = Modifier.fillMaxSize())
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f))
                )
            }
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            if (!isViewingLiveMatch || activeMatch == null) {
                // ==========================================
                // 🏏 CRICBUZZ SCORECARD MASTER PAGE
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CRICBUZZ MATCHES 🏏",
                            color = themeAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${tourneyMatches.size} ${if (tourneyMatches.size == 1) "Match" else "Matches"} in Tournament",
                            color = textSub,
                            fontSize = 11.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val scorerCode = activeTournament?.scorerInviteCode ?: ""
                        if (scorerCode.isNotBlank()) {
                            Surface(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(scorerCode))
                                    Toast.makeText(context, "📋 Scorer Code '$scorerCode' copied!", Toast.LENGTH_SHORT).show()
                                },
                                color = (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = if (isDarkTheme) NeonBlue else LightPitchPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(scorerCode, color = textMain, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (canScore) {
                            Button(
                                onClick = { attemptCreateMatch() },
                                colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Match", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Filter Tabs: All, Live, Completed
                if (tourneyMatches.isNotEmpty()) {
                    val liveCount = tourneyMatches.count { it.status != MatchStatus.COMPLETED }
                    val completedCount = tourneyMatches.count { it.status == MatchStatus.COMPLETED }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = matchFilter == "ALL",
                            onClick = { matchFilter = "ALL" },
                            label = { Text("All (${tourneyMatches.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = if (isDarkTheme) Color.Black else Color.White,
                                containerColor = surfaceBg,
                                labelColor = textMain
                            )
                        )
                        FilterChip(
                            selected = matchFilter == "LIVE",
                            onClick = { matchFilter = "LIVE" },
                            label = { Text("Live ($liveCount) 🔴", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BidGreen,
                                selectedLabelColor = Color.White,
                                containerColor = surfaceBg,
                                labelColor = textMain
                            )
                        )
                        FilterChip(
                            selected = matchFilter == "COMPLETED",
                            onClick = { matchFilter = "COMPLETED" },
                            label = { Text("Completed ($completedCount) 🏆", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = surfaceBg,
                                labelColor = textMain
                            )
                        )
                    }
                }

                // Master List / Cards View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (tourneyMatches.isEmpty()) {
                        item {
                            val needsTeams = teams.size < 2
                            val needsPlayers = players.size < 2
                            EmptyStateGuideCard(
                                mainIcon = if (needsTeams) Icons.Default.Shield else if (needsPlayers) Icons.Default.Person else Icons.Default.SportsCricket,
                                badgeText = if (needsTeams) "STEP 1: 2 TEAMS REQUIRED" else if (needsPlayers) "STEP 2: 2 PLAYERS REQUIRED" else "MATCH SCORER & SCORECARDS",
                                title = if (needsTeams) "Create 2 Teams First 🛡️" else if (needsPlayers) "Add Players First 🏏" else "No Matches Scheduled Yet ⚡",
                                description = if (needsTeams) {
                                    "Create at least 2 franchise teams in the Teams tab before you can schedule and start a cricket match."
                                } else if (needsPlayers) {
                                    "Add at least 2 players to the tournament before scheduling and scoring a cricket match."
                                } else {
                                    "Schedule cricket matches between franchises with coin toss, live ball-by-ball commentary, and Cricbuzz-style scorecards."
                                },
                                steps = listOf(
                                    GuideStep(
                                        stepNumber = 1,
                                        title = "Setup Match & Coin Toss",
                                        description = "Choose Team A vs Team B, set overs (5, 10, 20 overs), and flip coin for live toss.",
                                        icon = Icons.Default.Casino
                                    ),
                                    GuideStep(
                                        stepNumber = 2,
                                        title = "Live Ball-by-Ball Scoring",
                                        description = "Record 0, 1, 2, 4, 6, Wides, No-Balls, and Wickets with undo support.",
                                        icon = Icons.Default.SportsCricket
                                    ),
                                    GuideStep(
                                        stepNumber = 3,
                                        title = "Cricbuzz Scorecard & Standings",
                                        description = "Full batter & bowler tables, partnerships, Net Run Rate, and Points Table update live.",
                                        icon = Icons.Default.Assessment
                                    )
                                ),
                                primaryButtonText = if (needsTeams) {
                                    "CREATE 2 TEAMS FIRST (${teams.size}/2) 🛡️"
                                } else if (needsPlayers) {
                                    "ADD PLAYERS FIRST (${players.size}/2) 👤"
                                } else if (canScore) {
                                    "+ TOSS & START MATCH 🏏"
                                } else null,
                                onPrimaryClick = if (canScore) attemptCreateMatch else null,
                                isDarkTheme = isDarkTheme,
                                accentColor = themeAccent
                            )
                        }
                    } else {
                        val filteredList = when (matchFilter) {
                            "LIVE" -> tourneyMatches.filter { it.status != MatchStatus.COMPLETED }
                            "COMPLETED" -> tourneyMatches.filter { it.status == MatchStatus.COMPLETED }
                            else -> tourneyMatches
                        }

                        if (filteredList.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (matchFilter == "LIVE") "No Live Matches Right Now 🔴" else "No Completed Matches Yet 🏆",
                                        color = textSub,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            items(filteredList) { m ->
                                val mEditable = viewModel.isMatchEditable(m)
                                val tA = teams.find { it.id == m.teamAId }
                                val tB = teams.find { it.id == m.teamBId }
                                MatchHistoryCard(
                                    match = m,
                                    teams = teams,
                                    isEditable = mEditable,
                                    isDarkTheme = isDarkTheme,
                                    cardBg = cardBg,
                                    surfaceBg = surfaceBg,
                                    textMain = textMain,
                                    textSub = textSub,
                                    themeAccent = themeAccent,
                                    onEnterLiveClick = {
                                        viewModel.selectActiveMatch(m.id)
                                        strikerId = null
                                        nonStrikerId = null
                                        bowlerId = null
                                        isViewingLiveMatch = true
                                    },
                                    onScorecardClick = {
                                        selectedMatchForScorecard = m
                                    },
                                    onShareClick = {
                                        CricketShareHelper.shareMatchScorecard(context, m, tA, tB, ballRecords, players)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // 🔴 LIVE MATCH SCORER (ISOLATED TO 2 PLAYING TEAMS)
                // ==========================================
                val topBarScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(topBarScrollState)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Back to Master Page Button
                    Surface(
                        onClick = { isViewingLiveMatch = false },
                        color = (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "All Matches",
                                tint = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "All Matches",
                                color = textMain,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 2. Match Selector Dropdown
                    var showMatchDropdown by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { showMatchDropdown = true },
                            color = surfaceBg,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isEditable && activeMatch.status != MatchStatus.COMPLETED) BidGreen else if (isEditable) themeAccent else Color.Gray.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tA = teams.find { it.id == activeMatch.teamAId }
                                val tB = teams.find { it.id == activeMatch.teamBId }
                                val allTourneyList = if (tourneyMatches.isNotEmpty()) tourneyMatches else matches
                                val mIdx = allTourneyList.indexOfFirst { it.id == activeMatch.id }
                                val matchNumStr = if (mIdx >= 0) "Match ${mIdx + 1}" else "Match"
                                val mLabel = "$matchNumStr: ${tA?.shortCode ?: "A"} v ${tB?.shortCode ?: "B"}"
                                Text(
                                    text = mLabel,
                                    color = textMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = themeAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMatchDropdown,
                            onDismissRequest = { showMatchDropdown = false },
                            modifier = Modifier.background(cardBg)
                        ) {
                            if (canScore) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = null,
                                                tint = themeAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "+ Create New Match",
                                                color = themeAccent,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMatchDropdown = false
                                        attemptCreateMatch()
                                    }
                                )
                                HorizontalDivider(color = surfaceBg, thickness = 1.dp)
                            }

                            if (tourneyMatches.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No matches created yet", color = textSub, fontSize = 11.sp) },
                                    onClick = { showMatchDropdown = false }
                                )
                            } else {
                                tourneyMatches.forEachIndexed { index, m ->
                                    val mA = teams.find { it.id == m.teamAId }
                                    val mB = teams.find { it.id == m.teamBId }
                                    val mEditable = viewModel.isMatchEditable(m)
                                    val isSelected = m.id == activeMatch.id
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Match ${index + 1}: ${mA?.shortCode ?: "T1"} vs ${mB?.shortCode ?: "T2"}",
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) themeAccent else textMain,
                                                    fontSize = 12.sp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                val statusBadge = when {
                                                    m.status == MatchStatus.COMPLETED && mEditable -> "Editable ✏️"
                                                    m.status == MatchStatus.COMPLETED -> "Locked 🔒"
                                                    m.status == MatchStatus.INNINGS_BREAK -> "BREAK ⏸"
                                                    else -> "LIVE 🔴"
                                                }
                                                Text(
                                                    text = statusBadge,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        m.status == MatchStatus.COMPLETED && mEditable -> themeAccent
                                                        m.status == MatchStatus.COMPLETED -> Color.Gray
                                                        m.status == MatchStatus.INNINGS_BREAK -> IplGold
                                                        else -> BidGreen
                                                    }
                                                )
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectActiveMatch(m.id)
                                            strikerId = null
                                            nonStrikerId = null
                                            bowlerId = null
                                            showMatchDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Scorer Invite Code Mini Chip
                    val scorerCode = activeTournament?.scorerInviteCode ?: ""
                    if (scorerCode.isNotBlank()) {
                        Surface(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(scorerCode))
                                Toast.makeText(context, "📋 Scorer Code '$scorerCode' copied!", Toast.LENGTH_SHORT).show()
                            },
                            color = if (isDarkTheme) NeonBlue.copy(alpha = 0.15f) else LightPitchSurface,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentNavy.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = accentNavy, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(scorerCode, color = textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // 4. + New Match Button (Scorers/Admins) or Spectator Tag
                    if (canScore) {
                        Surface(
                            onClick = { attemptCreateMatch() },
                            color = themeAccent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = if (isDarkTheme) Color.Black else Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("New Match", color = if (isDarkTheme) Color.Black else Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                            }
                        }
                    } else if (isSpectator) {
                        Surface(
                            color = if (isDarkTheme) NeonBlue.copy(alpha = 0.15f) else LightPitchSurface,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentNavy.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = accentNavy, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Spectator", color = textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // 5. Scorecard Button
                    Surface(
                        onClick = { selectedMatchForScorecard = activeMatch },
                        color = surfaceBg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = themeAccent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Scorecard", color = textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    // 6. Share Button
                    Surface(
                        onClick = {
                            val tA = teams.find { it.id == activeMatch.teamAId }
                            val tB = teams.find { it.id == activeMatch.teamBId }
                            CricketShareHelper.shareMatchScorecard(context, activeMatch, tA, tB, ballRecords, players)
                        },
                        color = surfaceBg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = themeAccent, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Share", color = textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    // 7. YouTube Live Stream Action Chip (Single-Line, Non-wrapping)
                    val hasStream = activeMatch.liveStreamUrl.isNotBlank()
                    Surface(
                        onClick = {
                            if (canScore) {
                                streamUrlInput = activeMatch.liveStreamUrl
                                showLiveStreamDialog = true
                            } else if (hasStream) {
                                showLiveStreamCard = !showLiveStreamCard
                            }
                        },
                        color = if (hasStream) Color(0xFFFF0000).copy(alpha = 0.15f) else surfaceBg,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (hasStream) Color(0xFFFF0000) else themeAccent),
                        modifier = Modifier.wrapContentWidth().heightIn(min = 28.dp, max = 34.dp)
                    ) {
                        Row(
                            modifier = Modifier.wrapContentWidth().padding(horizontal = 7.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = if (hasStream) Color(0xFFFF0000) else themeAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (hasStream) "Live Stream 🔴" else "YouTube Live",
                                color = textMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.wrapContentWidth()
                            )
                        }
                    }

                    // 8. UI Mode Switcher (Classic Crichunt / Modern / Spectator Preview) + BG Cam + Status actions
                    if (canScore) {
                        Surface(
                            onClick = {
                                if (isBackgroundCameraOn) {
                                    isBackgroundCameraOn = false
                                } else {
                                    val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCam) {
                                        isBackgroundCameraOn = true
                                    } else {
                                        cameraPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                                    }
                                }
                            },
                            color = if (isBackgroundCameraOn) Color(0xFFFF0000) else surfaceBg,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isBackgroundCameraOn) Color(0xFFFF0000) else textSub.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = if (isBackgroundCameraOn) "📹 BG ON" else "📹 BG Cam",
                                color = if (isBackgroundCameraOn) Color.White else textMain,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }

                        val modeOptions = listOf(
                            "CLASSIC" to "Classic",
                            "MODERN" to "Modern",
                            "SPECTATOR" to "👁 Spectator"
                        )
                        modeOptions.forEach { (modeKey, modeLabel) ->
                            val isSelectedMode = scorerUiMode == modeKey
                            Surface(
                                onClick = { scorerUiMode = modeKey },
                                color = if (isSelectedMode) themeAccent else surfaceBg,
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelectedMode) themeAccent else textSub.copy(alpha = 0.35f)
                                )
                            ) {
                                Text(
                                    text = modeLabel,
                                    color = if (isSelectedMode) (if (isDarkTheme) Color.Black else Color.White) else textMain,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }

                        if (activeMatch.status == MatchStatus.COMPLETED) {
                            if (isEditable) {
                                Surface(
                                    onClick = {
                                        viewModel.resumeMatch(activeMatch.id)
                                        Toast.makeText(context, "✏️ Match reopened for editing!", Toast.LENGTH_SHORT).show()
                                    },
                                    color = themeAccent.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = themeAccent, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Edit ✏️", color = themeAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                }
                            } else {
                                Surface(
                                    color = Color.Gray.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Locked 🔒", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        } else {
                            // Live match actions
                            Surface(
                                onClick = { showEndInningsConfirmModal = true },
                                color = UnsoldRed.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.heightIn(min = 32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (currentInnings == 1) "End Innings" else "End Innings 2",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
                val match = activeMatch
                val tossWinnerTeam = teams.find { it.id == match.tossWinnerTeamId }
                val winnerTeam = teams.find { it.id == match.winnerTeamId }

                if (isSpectator || !canScore || scorerUiMode == "SPECTATOR") {
                    cricket.player.auction.ui.components.CricbuzzSpectatorView(
                        match = match,
                        teams = teams,
                        players = players,
                        matchBalls = matchBalls,
                        striker = striker,
                        nonStriker = nonStriker,
                        bowler = bowler,
                        isDarkTheme = isDarkTheme,
                        isPreviewByScorer = canScore && !isSpectator && scorerUiMode == "SPECTATOR",
                        onExitPreview = { scorerUiMode = "CLASSIC" }
                    )
                } else if (scorerUiMode == "CLASSIC") {
                    cricket.player.auction.ui.components.ClassicCrichuntScorerView(
                        match = match,
                        teams = teams,
                        players = players,
                        currentInningsBalls = currentInningsBalls,
                        battingTeam = battingTeam,
                        bowlingTeam = bowlingTeam,
                        striker = striker,
                        nonStriker = nonStriker,
                        bowler = bowler,
                        battingPlayers = battingPlayers,
                        selectedExtraType = selectedExtraType,
                        isEditable = isEditable,
                        isDarkTheme = isDarkTheme,
                        isBackgroundCameraOn = isBackgroundCameraOn,
                        onSelectExtra = { extra ->
                            val legalBallsNow = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                            val isOverEndedWaitingForBowler = legalBallsNow > 0 && (legalBallsNow % 6 == 0) && (bowlerId == null || bowlerId == lastCompletedOverBowlerId)
                            if (bowlerId == null || isOverEndedWaitingForBowler) {
                                showChangeBowlerModal = true
                                Toast.makeText(context, "⚠️ Over completed! Please select the bowler for the next over.", Toast.LENGTH_SHORT).show()
                            } else {
                                selectedExtraType = extra
                            }
                        },
                        onScoreRuns = { run ->
                            val legalBallsNow = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                            val oversAlreadyDone = legalBallsNow >= match.totalOvers * 6
                            val isOverEndedWaitingForBowler = !oversAlreadyDone && legalBallsNow > 0 && (legalBallsNow % 6 == 0) && (bowlerId == null || bowlerId == lastCompletedOverBowlerId)
                            if (strikerId.isNullOrBlank()) {
                                Toast.makeText(context, "⚠️ Please select a Striker batter first!", Toast.LENGTH_SHORT).show()
                            } else if (bowlerId.isNullOrBlank() || isOverEndedWaitingForBowler) {
                                showChangeBowlerModal = true
                                Toast.makeText(context, "⚠️ Please select the bowler before recording runs!", Toast.LENGTH_SHORT).show()
                            } else if (isEditable) {
                                if (match.status == MatchStatus.COMPLETED) {
                                    viewModel.resumeMatch(match.id)
                                }
                                val extraRuns = if (selectedExtraType == ExtraType.WIDE || selectedExtraType == ExtraType.NO_BALL) 1 else 0
                                val isLegal = selectedExtraType != ExtraType.WIDE && selectedExtraType != ExtraType.NO_BALL
                                val legalBallsBefore = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                val overNum = legalBallsBefore / 6
                                val ballNum = (legalBallsBefore % 6) + 1
                                val ball = BallRecord(
                                    id = "b_${System.currentTimeMillis()}",
                                    matchId = match.id,
                                    innings = currentInnings,
                                    overNumber = overNum,
                                    ballNumber = ballNum,
                                    strikerId = strikerId ?: "",
                                    nonStrikerId = nonStrikerId,
                                    bowlerId = bowlerId ?: "",
                                    runsScored = run,
                                    extraType = selectedExtraType,
                                    extraRuns = extraRuns,
                                    commentary = "$run runs scored"
                                )
                                viewModel.addBallRecord(ball)
                                selectedExtraType = ExtraType.NONE

                                var newStriker = strikerId
                                var newNonStriker = nonStrikerId
                                if (run % 2 != 0 && newNonStriker != null) {
                                    val temp = newStriker
                                    newStriker = newNonStriker
                                    newNonStriker = temp
                                }
                                val newLegalTotal = legalBallsBefore + (if (isLegal) 1 else 0)
                                val oversExhausted = newLegalTotal >= match.totalOvers * 6
                                val currentRunsBefore = if (currentInnings == 1) match.teamARuns else match.teamBRuns
                                val targetChased = currentInnings == 2 && (currentRunsBefore + run + extraRuns) >= (match.teamARuns + 1)
                                val isOverComplete = isLegal && (ballNum == 6)
                                if (isOverComplete && !oversExhausted && !targetChased) {
                                    if (newNonStriker != null) {
                                        val temp = newStriker
                                        newStriker = newNonStriker
                                        newNonStriker = temp
                                    }
                                    lastCompletedOverBowlerId = bowlerId
                                    bowlerId = null
                                    showChangeBowlerModal = true
                                }
                                strikerId = newStriker
                                nonStrikerId = newNonStriker
                            }
                        },
                        onRecordExtraBall = { exType, addRuns ->
                            val legalBallsNow = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                            val oversAlreadyDone = legalBallsNow >= match.totalOvers * 6
                            val isOverEndedWaitingForBowler = !oversAlreadyDone && legalBallsNow > 0 && (legalBallsNow % 6 == 0) && (bowlerId == null || bowlerId == lastCompletedOverBowlerId)
                            if (strikerId.isNullOrBlank()) {
                                Toast.makeText(context, "⚠️ Please select a Striker batter first!", Toast.LENGTH_SHORT).show()
                            } else if (bowlerId.isNullOrBlank() || isOverEndedWaitingForBowler) {
                                showChangeBowlerModal = true
                                Toast.makeText(context, "⚠️ Please select the bowler before recording extras!", Toast.LENGTH_SHORT).show()
                            } else if (isEditable) {
                                if (match.status == MatchStatus.COMPLETED) {
                                    viewModel.resumeMatch(match.id)
                                }
                                val isLegal = exType != ExtraType.WIDE && exType != ExtraType.NO_BALL
                                val legalBallsBefore = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                val overNum = legalBallsBefore / 6
                                val ballNum = (legalBallsBefore % 6) + 1
                                val batRuns = if (exType == ExtraType.NO_BALL) addRuns else 0
                                val exRuns = when (exType) {
                                    ExtraType.WIDE -> 1 + addRuns
                                    ExtraType.NO_BALL -> 1
                                    ExtraType.BYE, ExtraType.LEG_BYE -> addRuns
                                    else -> addRuns
                                }
                                val ball = BallRecord(
                                    id = "b_${System.currentTimeMillis()}",
                                    matchId = match.id,
                                    innings = currentInnings,
                                    overNumber = overNum,
                                    ballNumber = ballNum,
                                    strikerId = strikerId ?: "",
                                    nonStrikerId = nonStrikerId,
                                    bowlerId = bowlerId ?: "",
                                    runsScored = batRuns,
                                    extraType = exType,
                                    extraRuns = exRuns,
                                    commentary = "${exType.name} +$addRuns"
                                )
                                viewModel.addBallRecord(ball)
                                selectedExtraType = ExtraType.NONE

                                var newStriker = strikerId
                                var newNonStriker = nonStrikerId
                                if (addRuns % 2 != 0 && newNonStriker != null) {
                                    val temp = newStriker
                                    newStriker = newNonStriker
                                    newNonStriker = temp
                                }
                                val newLegalTotal = legalBallsBefore + (if (isLegal) 1 else 0)
                                val oversExhausted = newLegalTotal >= match.totalOvers * 6
                                val currentRunsBefore = if (currentInnings == 1) match.teamARuns else match.teamBRuns
                                val targetChased = currentInnings == 2 && (currentRunsBefore + batRuns + exRuns) >= (match.teamARuns + 1)
                                val isOverComplete = isLegal && (ballNum == 6)
                                if (isOverComplete && !oversExhausted && !targetChased) {
                                    if (newNonStriker != null) {
                                        val temp = newStriker
                                        newStriker = newNonStriker
                                        newNonStriker = temp
                                    }
                                    lastCompletedOverBowlerId = bowlerId
                                    bowlerId = null
                                    showChangeBowlerModal = true
                                }
                                strikerId = newStriker
                                nonStrikerId = newNonStriker
                            }
                        },
                        onOpenWicket = {
                            val legalBallsNow = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                            val isOverEndedWaitingForBowler = legalBallsNow > 0 && (legalBallsNow % 6 == 0) && (bowlerId == null || bowlerId == lastCompletedOverBowlerId)
                            if (bowlerId == null || isOverEndedWaitingForBowler) {
                                showChangeBowlerModal = true
                                Toast.makeText(context, "⚠️ Over ended! Please select the bowler for the next over.", Toast.LENGTH_SHORT).show()
                            } else {
                                if (match.status == MatchStatus.COMPLETED) {
                                    viewModel.resumeMatch(match.id)
                                }
                                showWicketModal = true
                            }
                        },
                        onUndoLastBall = {
                            if (match.status == MatchStatus.COMPLETED) {
                                viewModel.resumeMatch(match.id)
                            }
                            val undone = viewModel.undoLastBall(match.id)
                            if (undone != null) {
                                Toast.makeText(context, "↩ Undone last ball", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSwapStrike = {
                            if (nonStrikerId != null) {
                                val temp = strikerId
                                strikerId = nonStrikerId
                                nonStrikerId = temp
                            }
                        },
                        onReplaceBatter = { isStr, newPlayerId ->
                            if (isStr) strikerId = newPlayerId else nonStrikerId = newPlayerId
                        },
                        onChangeBowler = { showChangeBowlerModal = true },
                        onStartChase = { viewModel.endActiveInnings() },
                        onEndInnings = { showEndInningsConfirmModal = true },
                        onDirectEndMatch = { showDirectEndMatchModal = true },
                        onUndoEndInnings = { showUndoEndInningsConfirmModal = true },
                        onOpenYouTubeStream = {
                            streamUrlInput = match.liveStreamUrl
                            showLiveStreamDialog = true
                        }
                    )
                } else {
                    LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // --- Ultra-Compact Main Scoreboard Banner ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (match.status) {
                                    MatchStatus.INNINGS_BREAK -> IplGold.copy(alpha = 0.7f)
                                    MatchStatus.COMPLETED -> Color.Gray.copy(alpha = 0.3f)
                                    else -> themeAccent.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                // Top status bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val statusText = when {
                                            match.status == MatchStatus.INNINGS_BREAK -> "⏸ INNINGS BREAK"
                                            match.status != MatchStatus.COMPLETED -> "● INN $currentInnings LIVE"
                                            isEditable -> "COMPLETED (EDITABLE ✏️)"
                                            else -> "COMPLETED (LOCKED 🔒)"
                                        }
                                        val statusColor = when {
                                            match.status == MatchStatus.INNINGS_BREAK -> IplGold
                                            match.status != MatchStatus.COMPLETED -> BidGreen
                                            isEditable -> themeAccent
                                            else -> Color.Gray
                                        }
                                        Surface(
                                            color = statusColor.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = statusText,
                                                color = statusColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (tossWinnerTeam != null && match.tossDecision != null) {
                                            Text(
                                                text = "🪙 ${tossWinnerTeam.shortCode} (${match.tossDecision.lowercase()})",
                                                color = textSub,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    if (match.isLastPlayerBattingAllowed) {
                                        Text(
                                            text = "1-Batter Allowed",
                                            color = NeonBlue,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Main Score Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val runs = if (currentInnings == 1) match.teamARuns else match.teamBRuns
                                    val wkts = if (currentInnings == 1) match.teamAWickets else match.teamBWickets
                                    val overs = if (currentInnings == 1) match.teamAOversBatted else match.teamBOversBatted

                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = battingTeam?.name ?: "Batting Team",
                                            color = textMain,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$runs/$wkts",
                                            color = themeAccent,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "($overs/${match.totalOvers} ov)",
                                            color = textSub,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }

                                    if (currentInnings == 2 && match.status != MatchStatus.COMPLETED) {
                                        val target = match.teamARuns + 1
                                        val needed = (target - match.teamBRuns).coerceAtLeast(0)
                                        val legalBalls = (match.teamBOversBatted.toInt() * 6) + (((match.teamBOversBatted - match.teamBOversBatted.toInt()) * 10) + 0.5).toInt()
                                        val remainingBalls = ((match.totalOvers * 6) - legalBalls).coerceAtLeast(0)
                                        val remOvers = remainingBalls / 6.0
                                        val rrrVal = if (remOvers > 0) String.format("%.2f", needed / remOvers) else "0.00"

                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.clickable { showTargetModal = true }
                                        ) {
                                            Text(text = "T: $target", color = BidGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                            Text(text = "Need $needed in $remainingBalls b", color = textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "RRR: $rrrVal", color = themeAccent, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    } else if (match.status == MatchStatus.COMPLETED) {
                                        val summaryStr = if (match.resultSummary.isNotBlank()) {
                                            "🏆 ${match.resultSummary}"
                                        } else {
                                            val wName = winnerTeam?.name ?: "TIE"
                                            if (wName == "TIE") "Match Tied!" else "🏆 $wName won"
                                        }
                                        Text(
                                            text = summaryStr,
                                            color = BidGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (match.status == MatchStatus.COMPLETED) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { attemptCreateMatch() },
                                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f).height(36.dp)
                                        ) {
                                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ CREATE NEXT MATCH 🏏", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                                        }

                                        if (isEditable) {
                                            Button(
                                                onClick = {
                                                    viewModel.resumeMatch(match.id)
                                                    Toast.makeText(context, "✏️ Match reopened for editing!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = surfaceBg, contentColor = themeAccent),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Edit Match ✏️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Optional YouTube Live Streaming Video Player Card ---
                    if (activeMatch != null && activeMatch.liveStreamUrl.isNotBlank()) {
                        val streamVideoId = extractYouTubeVideoId(activeMatch.liveStreamUrl)
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0000).copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // Header row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.clickable { showLiveStreamCard = !showLiveStreamCard }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF0000))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "YOUTUBE LIVE STREAM 🔴",
                                                color = textMain,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (showLiveStreamCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = textSub,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // Open external in YouTube App
                                            IconButton(
                                                onClick = {
                                                    val ytUrl = if (streamVideoId != null) "https://www.youtube.com/watch?v=$streamVideoId" else activeMatch.liveStreamUrl
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)).apply {
                                                            setPackage("com.google.android.youtube")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl))
                                                        context.startActivity(webIntent)
                                                    }
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.OpenInNew,
                                                    contentDescription = "Open in YouTube",
                                                    tint = themeAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Edit / Remove Stream (if canScore)
                                            if (canScore) {
                                                IconButton(
                                                    onClick = {
                                                        streamUrlInput = activeMatch.liveStreamUrl
                                                        showLiveStreamDialog = true
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Stream URL",
                                                        tint = textSub,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // If expanded and videoId is valid, show embedded player:
                                    if (showLiveStreamCard && streamVideoId != null) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        AndroidView(
                                            factory = { ctx ->
                                                WebView(ctx).apply {
                                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                    )
                                                    settings.javaScriptEnabled = true
                                                    settings.domStorageEnabled = true
                                                    settings.mediaPlaybackRequiresUserGesture = false
                                                    webChromeClient = WebChromeClient()
                                                    webViewClient = WebViewClient()
                                                    loadUrl("https://www.youtube-nocookie.com/embed/$streamVideoId?autoplay=1&playsinline=1&modestbranding=1&rel=0")
                                                }
                                            },
                                            update = { webView ->
                                                val embedUrl = "https://www.youtube-nocookie.com/embed/$streamVideoId?autoplay=1&playsinline=1&modestbranding=1&rel=0"
                                                if (webView.url != embedUrl) {
                                                    webView.loadUrl(embedUrl)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else if (showLiveStreamCard) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Live Stream Link: ${activeMatch.liveStreamUrl}",
                                            color = textSub,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Cricbuzz-Style Unified On-Crease & Bowler Card ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, surfaceBg)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                // Batters Header & Strike Swap
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏏 BATTERS (${battingTeam?.shortCode ?: "Batting"})",
                                        color = themeAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Surface(
                                        onClick = {
                                            if (nonStrikerId != null) {
                                                val temp = strikerId
                                                strikerId = nonStrikerId
                                                nonStrikerId = temp
                                            }
                                        },
                                        color = surfaceBg,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = themeAccent, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Swap Strike", color = textMain, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Batters Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceBg.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("BATTER", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                                    Text("R", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("B", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("4s", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("6s", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("SR", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
                                }

                                // Striker Row
                                if (striker != null) {
                                    val sBalls = currentInningsBalls.filter { it.strikerId == striker.id }
                                    val sRuns = sBalls.sumOf { it.runsScored }
                                    val sFaced = sBalls.count { it.extraType != ExtraType.WIDE }
                                    val s4s = sBalls.count { it.runsScored == 4 }
                                    val s6s = sBalls.count { it.runsScored == 6 }
                                    val sSR = if (sFaced > 0) String.format("%.1f", sRuns * 100.0 / sFaced) else "0.0"

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(2.3f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).border(1.dp, themeAccent, CircleShape).background(surfaceBg)) {
                                                DriveImage(
                                                    url = striker.imageUrl,
                                                    contentDescription = striker.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    fallbackIcon = getPlayerPlaceholderIcon(striker.id, striker.name)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${striker.name}*",
                                                color = textMain,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            DropdownPlayerPicker(
                                                selectedPlayer = striker,
                                                players = battingPlayers,
                                                title = "Select Striker",
                                                isCompact = true,
                                                isDarkTheme = isDarkTheme,
                                                onPlayerSelected = { strikerId = it.id }
                                            )
                                        }
                                        Text("$sRuns", color = themeAccent, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$sFaced", color = textMain, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$s4s", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text("$s6s", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(sSR, color = textSub, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
                                    }
                                }

                                // Non-Striker Row
                                val isLastBatsmanSolo = match.isLastPlayerBattingAllowed &&
                                        ((currentInnings == 1 && match.teamAWickets >= (battingPlayers.size.coerceAtLeast(10) - 1)) ||
                                         (currentInnings == 2 && match.teamBWickets >= (battingPlayers.size.coerceAtLeast(10) - 1)))

                                if (isLastBatsmanSolo) {
                                    Text("⚡ [SOLO BATTING - LAST BATSMAN]", color = NeonBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                } else if (nonStriker != null) {
                                    val nsBalls = currentInningsBalls.filter { it.strikerId == nonStriker.id }
                                    val nsRuns = nsBalls.sumOf { it.runsScored }
                                    val nsFaced = nsBalls.count { it.extraType != ExtraType.WIDE }
                                    val ns4s = nsBalls.count { it.runsScored == 4 }
                                    val ns6s = nsBalls.count { it.runsScored == 6 }
                                    val nsSR = if (nsFaced > 0) String.format("%.1f", nsRuns * 100.0 / nsFaced) else "0.0"

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(2.3f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).border(1.dp, accentNavy, CircleShape).background(surfaceBg)) {
                                                DriveImage(
                                                    url = nonStriker.imageUrl,
                                                    contentDescription = nonStriker.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    fallbackIcon = getPlayerPlaceholderIcon(nonStriker.id, nonStriker.name)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = nonStriker.name,
                                                color = textMain,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                            DropdownPlayerPicker(
                                                selectedPlayer = nonStriker,
                                                players = battingPlayers.filter { it.id != strikerId },
                                                title = "Select Non-Striker",
                                                isCompact = true,
                                                isDarkTheme = isDarkTheme,
                                                onPlayerSelected = { nonStrikerId = it.id }
                                            )
                                        }
                                        Text("$nsRuns", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$nsFaced", color = textMain, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$ns4s", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text("$ns6s", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(nsSR, color = textSub, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
                                    }
                                }

                                HorizontalDivider(color = surfaceBg, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

                                // Bowler Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ BOWLER (${bowlingTeam?.shortCode ?: "Bowling"})",
                                        color = NeonBlue,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    DropdownPlayerPicker(
                                        selectedPlayer = bowler,
                                        players = bowlingPlayers,
                                        title = "Select Bowler",
                                        isCompact = true,
                                        isDarkTheme = isDarkTheme,
                                        onPlayerSelected = { bowlerId = it.id }
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                // Bowler Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceBg.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("BOWLER", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                                    Text("O", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("M", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("R", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("W", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("ECON", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
                                }

                                if (bowler != null) {
                                    val bBalls = currentInningsBalls.filter { it.bowlerId == bowler.id }
                                    val bLegal = bBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                    val bOvers = "${bLegal / 6}.${bLegal % 6}"
                                    val bRuns = bBalls.sumOf { it.runsScored + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.extraRuns else 0) }
                                    val bWkts = bBalls.count { it.isWicket && it.dismissalType != DismissalType.RUN_OUT }
                                    val bEcon = if (bLegal > 0) String.format("%.2f", bRuns * 6.0 / bLegal) else "0.00"

                                    val oversMap = bBalls.groupBy { it.overNumber }
                                    val bMaidens = oversMap.count { (_, overBalls) ->
                                        val legals = overBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                        val rInOver = overBalls.sumOf { it.runsScored + it.extraRuns }
                                        legals == 6 && rInOver == 0
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(2.3f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).border(1.dp, accentNavy, CircleShape).background(surfaceBg)) {
                                                DriveImage(
                                                    url = bowler.imageUrl,
                                                    contentDescription = bowler.name,
                                                    modifier = Modifier.fillMaxSize(),
                                                    fallbackIcon = getPlayerPlaceholderIcon(bowler.id, bowler.name)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(bowler.name, color = textMain, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                                        }
                                        Text(bOvers, color = textMain, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$bMaidens", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text("$bRuns", color = textMain, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$bWkts", color = UnsoldRed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(bEcon, color = textMain, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.0f))
                                    }
                                } else {
                                    Surface(
                                        onClick = { showChangeBowlerModal = true },
                                        color = IplGold.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.6f)),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(imageVector = Icons.Default.PauseCircle, contentDescription = null, tint = IplGold, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "⏸️ Scoring Paused (Over Ended)",
                                                    color = themeAccent,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                text = "SELECT BOWLER 🏏",
                                                color = if (isDarkTheme) IplGold else LightPitchPrimary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Innings Break Screen (shown after Innings 1 overs / all-out) ---
                    if (match.status == MatchStatus.INNINGS_BREAK) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(2.dp, themeAccent),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "🏁 INNINGS 1 COMPLETE",
                                        color = themeAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val teamAName = teams.find { it.id == match.teamAId }?.name ?: "Team A"
                                    val teamBName = teams.find { it.id == match.teamBId }?.name ?: "Team B"
                                    Text(
                                        text = "$teamAName scored ${match.teamARuns}/${match.teamAWickets}",
                                        color = textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$teamBName needs ${match.teamARuns + 1} runs to win",
                                        color = textSub,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (isEditable) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.endActiveInnings()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BidGreen),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("🏏 Start Chase", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 13.sp, maxLines = 1)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    val undone = viewModel.undoLastBall(match.id)
                                                    if (undone != null) {
                                                        Toast.makeText(context, "↩ Undone last ball", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("↩ Undo Last Ball", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Ball-by-Ball Instant Scoring Keypad ---
                    item {
                        if (!canScore || isSpectator || match.status == MatchStatus.INNINGS_BREAK) {
                            Surface(
                                color = NeonBlue.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = NeonBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SPECTATOR LIVE MATCH VIEW",
                                            color = NeonBlue,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "You are watching live scores in read-only mode. All ball recordings, wickets, and scorecards update in real time.",
                                        color = textSub,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, surfaceBg)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    // Extras Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("EXTRAS", color = textSub, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            ExtraType.values().forEach { extra ->
                                                val isSel = selectedExtraType == extra
                                                Surface(
                                                    onClick = {
                                                        val legalBallsNow = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                                        val isOverEndedWaitingForBowler = legalBallsNow > 0 && (legalBallsNow % 6 == 0) && (bowlerId == null || bowlerId == lastCompletedOverBowlerId)
                                                        if (bowlerId == null || isOverEndedWaitingForBowler) {
                                                            showChangeBowlerModal = true
                                                            Toast.makeText(context, "⚠️ Over completed! Please select the bowler for the next over.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            selectedExtraType = if (isSel) ExtraType.NONE else extra
                                                        }
                                                    },
                                                    color = if (isSel) NeonBlue else surfaceBg,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = extra.name,
                                                        color = if (isSel) Color.Black else textMain,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Runs Row: 0, 1, 2, 3, 4, 6
                                    val runsList = listOf(0, 1, 2, 3, 4, 6)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        runsList.forEach { run ->
                                            Button(
                                                onClick = {
                                                    val legalBallsNow = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                                    val isOverEndedWaitingForBowler = legalBallsNow > 0 && (legalBallsNow % 6 == 0) && (bowlerId == null || bowlerId == lastCompletedOverBowlerId)
                                                    if (bowlerId == null || isOverEndedWaitingForBowler) {
                                                        showChangeBowlerModal = true
                                                        Toast.makeText(context, "⚠️ Over completed! Please select the bowler for the next over.", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    if (strikerId != null && isEditable) {
                                                        // Auto-resume if match was completed
                                                        if (match.status == MatchStatus.COMPLETED) {
                                                            viewModel.resumeMatch(match.id)
                                                        }
                                                        val extraRuns = if (selectedExtraType == ExtraType.WIDE || selectedExtraType == ExtraType.NO_BALL) 1 else 0
                                                        val isLegal = selectedExtraType != ExtraType.WIDE && selectedExtraType != ExtraType.NO_BALL
                                                        val legalBallsBefore = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                                        val overNum = legalBallsBefore / 6
                                                        val ballNum = (legalBallsBefore % 6) + 1

                                                        val ball = BallRecord(
                                                            id = "b_${System.currentTimeMillis()}",
                                                            matchId = match.id,
                                                            innings = currentInnings,
                                                            overNumber = overNum,
                                                            ballNumber = ballNum,
                                                            strikerId = strikerId ?: "",
                                                            nonStrikerId = nonStrikerId,
                                                            bowlerId = bowlerId ?: "",
                                                            runsScored = run,
                                                            extraType = selectedExtraType,
                                                            extraRuns = extraRuns,
                                                            commentary = "$run runs scored"
                                                        )
                                                        viewModel.addBallRecord(ball)
                                                        selectedExtraType = ExtraType.NONE

                                                        // Strike swap on odd runs:
                                                        var newStriker = strikerId
                                                        var newNonStriker = nonStrikerId
                                                        if (run % 2 != 0 && newNonStriker != null) {
                                                            val temp = newStriker
                                                            newStriker = newNonStriker
                                                            newNonStriker = temp
                                                        }

                                                        // End of Over (6 legal balls completed): swap strike & prompt bowler change ONLY if innings/match continues!
                                                        val newLegalTotal = legalBallsBefore + (if (isLegal) 1 else 0)
                                                        val oversExhausted = newLegalTotal >= match.totalOvers * 6
                                                        val currentRunsBefore = if (currentInnings == 1) match.teamARuns else match.teamBRuns
                                                        val targetChased = currentInnings == 2 && (currentRunsBefore + run + extraRuns) >= (match.teamARuns + 1)
                                                        val isOverComplete = isLegal && (ballNum == 6)
                                                        if (isOverComplete && !oversExhausted && !targetChased) {
                                                            if (newNonStriker != null) {
                                                                val temp = newStriker
                                                                newStriker = newNonStriker
                                                                newNonStriker = temp
                                                            }

                                                            lastCompletedOverBowlerId = bowlerId
                                                            bowlerId = null
                                                            showChangeBowlerModal = true
                                                        }

                                                        strikerId = newStriker
                                                        nonStrikerId = newNonStriker
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (run == 4 || run == 6) themeAccent else surfaceBg,
                                                    contentColor = if (run == 4 || run == 6) Color.Black else textMain
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(42.dp),
                                                enabled = isEditable
                                            ) {
                                                Text("$run", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Action Buttons Row: WICKET, END INN, UNDO, REOPEN
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Wicket Button
                                        Button(
                                            onClick = {
                                                val legalBallsNow = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                                val isOverEndedWaitingForBowler = legalBallsNow > 0 && (legalBallsNow % 6 == 0) && (bowlerId == null || bowlerId == lastCompletedOverBowlerId)
                                                if (bowlerId == null || isOverEndedWaitingForBowler) {
                                                    showChangeBowlerModal = true
                                                    Toast.makeText(context, "⚠️ Over ended! Please select the bowler for the next over.", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                if (match.status == MatchStatus.COMPLETED) {
                                                    viewModel.resumeMatch(match.id)
                                                }
                                                showWicketModal = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1.05f).height(42.dp),
                                            enabled = isEditable
                                        ) {
                                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("WICKET", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        }

                                        // End Innings Button
                                        Button(
                                            onClick = { showEndInningsConfirmModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed.copy(alpha = 0.85f), contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp),
                                            modifier = Modifier.weight(1.2f).height(42.dp),
                                            enabled = isEditable && match.status != MatchStatus.COMPLETED
                                        ) {
                                            Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = if (currentInnings == 1) "END INNINGS" else "END INN 2",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        // Undo Ball Button
                                        Button(
                                            onClick = {
                                                if (match.status == MatchStatus.COMPLETED) {
                                                    viewModel.resumeMatch(match.id)
                                                }
                                                val undone = viewModel.undoLastBall(match.id)
                                                if (undone != null) {
                                                    strikerId = undone.strikerId
                                                    nonStrikerId = undone.nonStrikerId
                                                    bowlerId = undone.bowlerId
                                                }
                                                showTargetModal = false
                                                Toast.makeText(context, "↩️ Last ball undone", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = surfaceBg, contentColor = textMain),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                                            modifier = Modifier.weight(1f).height(42.dp),
                                            enabled = isEditable && matchBalls.isNotEmpty()
                                        ) {
                                            Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, softWrap = false)
                                        }

                                        // Undo End Innings or Reopen
                                        if (currentInnings == 2 || match.status == MatchStatus.COMPLETED) {
                                            Button(
                                                onClick = { showUndoEndInningsConfirmModal = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue.copy(alpha = 0.2f), contentColor = NeonBlue),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                                modifier = Modifier.weight(0.9f).height(42.dp),
                                                enabled = isEditable
                                            ) {
                                                Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("Undo Inn", fontWeight = FontWeight.Bold, fontSize = 9.sp, maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }

                                    if (!isEditable) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "🔒 Match is locked because a subsequent match has completed.",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Over Timeline Row ---
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("THIS OVER", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                            val currentOverBalls = run {
                                    val legalBallsInInnings = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                    val currentOverIndex = legalBallsInInnings / 6
                                    currentInningsBalls.filter { it.overNumber == currentOverIndex }
                                }
                            if (currentOverBalls.isEmpty()) {
                                Text("No balls bowled yet", color = textSub, fontSize = 10.sp)
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(currentOverBalls) { ball ->
                                        val text = if (ball.isWicket) "W" else if (ball.extraType != ExtraType.NONE) "${ball.runsScored}${ball.extraType.name.take(1)}" else "${ball.runsScored}"
                                        val color = if (ball.isWicket) UnsoldRed else if (ball.runsScored == 4 || ball.runsScored == 6) themeAccent else surfaceBg

                                        Surface(
                                            color = color,
                                            shape = CircleShape,
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = text,
                                                    color = if (ball.runsScored == 4 || ball.runsScored == 6) Color.Black else if (ball.isWicket) Color.White else textMain,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- Compact Direct End Match / Declare Result Option ---
                    if (match.status != MatchStatus.COMPLETED) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, surfaceBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DIRECT END MATCH / DECLARE RESULT",
                                            color = themeAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "Declare winner directly & update NRR",
                                            color = textSub,
                                            fontSize = 9.sp
                                        )
                                    }

                                    Button(
                                        onClick = { showDirectEndMatchModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("DECLARE", fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                }
            }
        }
        }
    }

    // --- End Innings Confirmation Dialog ---
    if (showEndInningsConfirmModal) {
        AlertDialog(
            onDismissRequest = { showEndInningsConfirmModal = false },
            title = { Text("End Innings $currentInnings?", color = UnsoldRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = if (currentInnings == 1)
                        "Are you sure you want to end Innings 1 now? This will declare the batting team and set the target for Innings 2."
                    else
                        "Are you sure you want to end Innings 2 now? This will complete the match.",
                    color = textSub
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.endActiveInnings()
                        showEndInningsConfirmModal = false
                        Toast.makeText(context, if (currentInnings == 1) "Innings 1 Ended! Target set. (Tap 'Undo Inn 1' to revert)" else "Innings 2 Ended!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("END INNINGS NOW", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndInningsConfirmModal = false }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Undo End Innings Confirmation Dialog ---
    if (showUndoEndInningsConfirmModal) {
        AlertDialog(
            onDismissRequest = { showUndoEndInningsConfirmModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, tint = NeonBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Undo End Innings / Reopen?", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    text = if (activeMatch?.status == MatchStatus.COMPLETED)
                        "Are you sure you want to reopen this match? This will restore scoring mode so you can continue recording balls."
                    else
                        "Are you sure you want to reopen Innings 1? This will take the match back to the 1st innings so you can continue batting or make corrections.",
                    color = textSub,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val undone = viewModel.undoEndInnings()
                        if (undone != null) {
                            strikerId = undone.strikerId
                            nonStrikerId = undone.nonStrikerId
                            bowlerId = undone.bowlerId
                        }
                        showTargetModal = false
                        showUndoEndInningsConfirmModal = false
                        Toast.makeText(context, "↩️ Innings re-opened & last ball restored!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black)
                ) {
                    Text("REOPEN INNINGS ↩️", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoEndInningsConfirmModal = false }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Direct End Match / Declare Result Dialog ---
    if (showDirectEndMatchModal && activeMatch != null) {
        val match = activeMatch!!
        val teamA = teams.find { it.id == match.teamAId }
        val teamB = teams.find { it.id == match.teamBId }

        var selectedWinnerId by remember { mutableStateOf(match.teamAId) }
        var marginType by remember { mutableStateOf("RUNS") } // RUNS, WICKETS, BALLS
        var marginInput by remember { mutableStateOf("10") }

        var teamARunsInput by remember { mutableStateOf(match.teamARuns.toString()) }
        var teamAOversInput by remember { mutableStateOf(match.teamAOversBatted.toString()) }
        var teamBRunsInput by remember { mutableStateOf(match.teamBRuns.toString()) }
        var teamBOversInput by remember { mutableStateOf(match.teamBOversBatted.toString()) }

        AlertDialog(
            onDismissRequest = { showDirectEndMatchModal = false },
            title = { Text("Declare Match Winner & NRR", color = textMain, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("SELECT WINNING TEAM", color = themeAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedWinnerId == match.teamAId,
                                onClick = { selectedWinnerId = match.teamAId },
                                label = { Text(teamA?.shortCode ?: "Team A", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IplGold, selectedLabelColor = Color.Black)
                            )
                            FilterChip(
                                selected = selectedWinnerId == match.teamBId,
                                onClick = { selectedWinnerId = match.teamBId },
                                label = { Text(teamB?.shortCode ?: "Team B", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IplGold, selectedLabelColor = Color.Black)
                            )
                            FilterChip(
                                selected = selectedWinnerId == "TIE",
                                onClick = { selectedWinnerId = "TIE" },
                                label = { Text("TIE / DRAW", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonBlue, selectedLabelColor = Color.Black)
                            )
                        }
                    }

                    if (selectedWinnerId != "TIE") {
                        item {
                            Text("VICTORY MARGIN TYPE", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = marginType == "RUNS",
                                    onClick = { marginType = "RUNS" },
                                    label = { Text("By Runs", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = marginType == "WICKETS",
                                    onClick = { marginType = "WICKETS" },
                                    label = { Text("By Wickets", fontSize = 10.sp) }
                                )
                                FilterChip(
                                    selected = marginType == "BALLS",
                                    onClick = { marginType = "BALLS" },
                                    label = { Text("By Balls Left", fontSize = 10.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = marginInput,
                                onValueChange = { marginInput = it },
                                label = { Text(when (marginType) { "RUNS" -> "Winning Margin (Runs)"; "WICKETS" -> "Wickets Left"; else -> "Balls Remaining" }) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                        Text("FINAL SCORE & OVERS FOR NRR CALCULATION", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    item {
                        Text("${teamA?.name ?: "Team A"} Final Runs & Overs:", color = textSub, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = teamARunsInput,
                                onValueChange = { teamARunsInput = it },
                                label = { Text("Runs") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = teamAOversInput,
                                onValueChange = { teamAOversInput = it },
                                label = { Text("Overs (e.g. 20.0)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text("${teamB?.name ?: "Team B"} Final Runs & Overs:", color = Color.LightGray, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = teamBRunsInput,
                                onValueChange = { teamBRunsInput = it },
                                label = { Text("Runs") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = teamBOversInput,
                                onValueChange = { teamBOversInput = it },
                                label = { Text("Overs (e.g. 18.2)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val winnerTeam = teams.find { it.id == selectedWinnerId }
                        val winnerName = winnerTeam?.name ?: if (selectedWinnerId == "TIE") "Match Tied" else "Team"
                        val valInt = marginInput.toIntOrNull() ?: 0

                        val summaryText = if (selectedWinnerId == "TIE") {
                            "Match Tied!"
                        } else {
                            when (marginType) {
                                "RUNS" -> "$winnerName won by $valInt runs"
                                "WICKETS" -> "$winnerName won by $valInt wickets"
                                else -> "$winnerName won by $valInt balls remaining"
                            }
                        }

                        val tARuns = teamARunsInput.toIntOrNull() ?: match.teamARuns
                        val tAOvers = teamAOversInput.toDoubleOrNull() ?: match.teamAOversBatted
                        val tBRuns = teamBRunsInput.toIntOrNull() ?: match.teamBRuns
                        val tBOvers = teamBOversInput.toDoubleOrNull() ?: match.teamBOversBatted

                        viewModel.endMatchDirectly(
                            matchId = match.id,
                            winnerTeamId = selectedWinnerId,
                            resultSummary = summaryText,
                            teamARuns = tARuns,
                            teamAOvers = tAOvers,
                            teamBRuns = tBRuns,
                            teamBOvers = tBOvers
                        )
                        showDirectEndMatchModal = false
                        Toast.makeText(context, "🏆 Match Completed: $summaryText", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BidGreen, contentColor = Color.Black)
                ) {
                    Text("DECLARE WINNER & UPDATE NRR 🏆", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectEndMatchModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Over Completed / Bowler Change Modal ---
    if (showChangeBowlerModal) {
        val eligibleBowlers = remember(bowlingPlayers, lastCompletedOverBowlerId) {
            val filtered = bowlingPlayers.filter { it.id != lastCompletedOverBowlerId }
            if (filtered.isNotEmpty()) filtered else bowlingPlayers
        }
        AlertDialog(
            onDismissRequest = {
                // Pause scoring on dismiss / back press
                showChangeBowlerModal = false
                bowlerId = null
                Toast.makeText(context, "⏸️ Scoring paused. Select bowler when ready to resume.", Toast.LENGTH_SHORT).show()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            ),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SportsCricket, contentDescription = null, tint = IplGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OVER COMPLETED! ⚡", color = IplGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("The over is complete! Strike has been rotated automatically.", color = textMain, fontSize = 13.sp)
                    Text("Select new bowler for the next over from ${bowlingTeam?.name ?: "bowling team"}:", color = textSub, fontSize = 12.sp)

                    DropdownPlayerPicker(
                        selectedPlayer = bowler,
                        players = eligibleBowlers,
                        onPlayerSelected = {
                            bowlerId = it.id
                        }
                    )
                    if (lastCompletedOverBowlerId != null) {
                        val prevBowlerName = bowlingPlayers.find { it.id == lastCompletedOverBowlerId }?.name
                        if (prevBowlerName != null) {
                            Text(
                                text = "ℹ️ $prevBowlerName just bowled the last over (cannot bowl consecutive overs).",
                                color = textSub,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bowlerId != null) {
                            showChangeBowlerModal = false
                            Toast.makeText(context, "🎯 Bowler set: ${bowler?.name ?: "New Bowler"}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "⚠️ Please select a bowler first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = bowlerId != null
                ) {
                    Text("CONTINUE MATCH", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showChangeBowlerModal = false
                        bowlerId = null
                        Toast.makeText(context, "⏸️ Scoring paused. Select bowler when ready to resume.", Toast.LENGTH_SHORT).show()
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("Back / Pause ⏸️", color = textSub, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Optional YouTube Live Stream Setup Dialog ---
    if (showLiveStreamDialog && activeMatch != null) {
        val currentMatch = activeMatch
        AlertDialog(
            onDismissRequest = { showLiveStreamDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = Color(0xFFFF0000))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("YouTube Live Streaming", color = textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Broadcast your cricket match on YouTube and display the live stream right next to real-time scores.",
                        color = textSub,
                        fontSize = 12.sp
                    )

                    // Button to request Camera permission and start direct stream + score overlay
                    Button(
                        onClick = {
                            val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                showLiveStreamDialog = false
                                showCameraStreamPreview = true
                            } else {
                                cameraPermissionLauncher.launch(
                                    arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Start Camera Stream & Go Live 🔴", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }

                    HorizontalDivider(color = surfaceBg, thickness = 1.dp)

                    Text(
                        text = "Once live, copy & paste the YouTube Live URL or Video ID below:",
                        color = textSub,
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = streamUrlInput,
                        onValueChange = { streamUrlInput = it },
                        label = { Text("YouTube Live URL or Video ID") },
                        placeholder = { Text("https://youtube.com/live/...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeAccent,
                            unfocusedBorderColor = surfaceBg
                        )
                    )
                    val parsedId = extractYouTubeVideoId(streamUrlInput)
                    if (parsedId != null) {
                        Text(
                            text = "✅ Valid YouTube Video ID: $parsedId",
                            color = BidGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (streamUrlInput.isNotBlank()) {
                        Text(
                            text = "ℹ️ Will save link and open in YouTube browser/app.",
                            color = themeAccent,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMatchLiveStreamUrl(currentMatch.id, streamUrlInput.trim())
                        showLiveStreamDialog = false
                        showLiveStreamCard = true
                        Toast.makeText(context, "📺 YouTube Live Stream updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White)
                ) {
                    Text("Save Stream", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (currentMatch.liveStreamUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.updateMatchLiveStreamUrl(currentMatch.id, "")
                                showLiveStreamDialog = false
                                Toast.makeText(context, "🗑️ Live stream removed", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Remove", color = UnsoldRed)
                        }
                    }
                    TextButton(onClick = { showLiveStreamDialog = false }) {
                        Text("Cancel", color = textSub)
                    }
                }
            },
            containerColor = cardBg
        )
    }

    if (showCameraStreamPreview && activeMatch != null) {
        CameraLiveBroadcastOverlayDialog(
            viewModel = viewModel,
            match = activeMatch,
            teams = teams,
            currentInningsBalls = currentInningsBalls,
            striker = striker,
            nonStriker = nonStriker,
            bowler = bowler,
            initialStreamUrl = activeMatch.liveStreamUrl,
            onSaveStreamUrl = { newUrl ->
                viewModel.updateMatchLiveStreamUrl(activeMatch.id, newUrl)
                streamUrlInput = newUrl
                showLiveStreamCard = newUrl.isNotBlank()
                Toast.makeText(
                    context,
                    if (newUrl.isNotBlank()) "📺 Live stream overlay published for Spectators!" else "⏹ Live stream ended.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onEnableBackgroundCamera = { isBackgroundCameraOn = true },
            onDismiss = { showCameraStreamPreview = false }
        )
    }

    // --- Target & Required Run Rate (RRR) Popup Modal ---
    val targetMatch = activeMatch
    if (showTargetModal && targetMatch != null) {
        val match = targetMatch
        val target = match.teamARuns + 1
        val rrr = String.format("%.2f", target.toDouble() / match.totalOvers)

        AlertDialog(
            onDismissRequest = { showTargetModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = themeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TARGET & REQUIRED RUN RATE", color = IplGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "${teams.find { it.id == match.teamAId }?.name ?: "Team A"} scored ${match.teamARuns}/${match.teamAWickets} in ${match.teamAOversBatted} overs",
                        color = textSub,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TARGET FOR ${teams.find { it.id == match.teamBId }?.name ?: "TEAM B"}", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("$target RUNS", color = BidGreen, fontSize = 32.sp, fontWeight = FontWeight.Black)
                            Text("in ${match.totalOvers} Overs (${match.totalOvers * 6} Balls)", color = Color.White, fontSize = 13.sp)

                            Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                            Text("REQUIRED RUN RATE (RRR)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("$rrr runs/over", color = IplGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTargetModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black)
                ) {
                    Text("START CHASE!", fontWeight = FontWeight.ExtraBold)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Create Match Setup Dialog ---
    if (showCreateMatchModal) {
        val teamAPlayers = players.filter { it.soldToTeamId == selectedTeamAId }
        val teamBPlayers = players.filter { it.soldToTeamId == selectedTeamBId }
        val hasSquadPlayers = teamAPlayers.isNotEmpty() && teamBPlayers.isNotEmpty()

        AlertDialog(
            onDismissRequest = { showCreateMatchModal = false },
            title = { Text("Setup New Cricket Match", color = textMain, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Team 1", color = Color.LightGray, fontSize = 12.sp)
                    DropdownTeamPicker(
                        selectedTeamId = selectedTeamAId,
                        teams = teams,
                        onTeamSelected = { selectedTeamAId = it.id }
                    )

                    Text("Select Team 2 (Opponent Team)", color = Color.LightGray, fontSize = 12.sp)
                    DropdownTeamPicker(
                        selectedTeamId = selectedTeamBId,
                        teams = teams.filter { it.id != selectedTeamAId },
                        onTeamSelected = { selectedTeamBId = it.id }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Team 1 Squad: ${teamAPlayers.size} players",
                            color = if (teamAPlayers.isNotEmpty()) BidGreen else UnsoldRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Team 2 Squad: ${teamBPlayers.size} players",
                            color = if (teamBPlayers.isNotEmpty()) BidGreen else UnsoldRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!hasSquadPlayers && selectedTeamAId.isNotBlank() && selectedTeamBId.isNotBlank()) {
                        Surface(
                            color = UnsoldRed.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, UnsoldRed.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = UnsoldRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Each team must have at least 1 player in its squad before starting the match. Draft players in auction or add players to teams in the Teams tab.",
                                    color = UnsoldRed,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = matchOversInput,
                        onValueChange = { matchOversInput = it },
                        label = { Text("Match Overs Limit (e.g. 5, 10, 20)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = surfaceBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Last Player Batting Allowed",
                                    color = themeAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Allows the last remaining player to keep batting solo after 10 wickets fall",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = isLastPlayerBattingAllowed,
                                onCheckedChange = { isLastPlayerBattingAllowed = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = IplGold, checkedTrackColor = StadiumDark)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTeamAId.isNotBlank() && selectedTeamBId.isNotBlank() && hasSquadPlayers) {
                            showCreateMatchModal = false
                            showCoinTossModal = true
                        } else if (!hasSquadPlayers) {
                            Toast.makeText(context, "⚠️ Both teams must have at least 1 player to start a match!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = selectedTeamAId.isNotBlank() && selectedTeamBId.isNotBlank() && hasSquadPlayers
                ) {
                    Text("TOSS COIN & DECIDE 🪙", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateMatchModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Interactive Coin Toss & Captain Decision Modal ---
    if (showCoinTossModal) {
        val team1 = teams.find { it.id == selectedTeamAId }
        val team2 = teams.find { it.id == selectedTeamBId }

        if (team1 != null && team2 != null) {
            CoinTossModal(
                teamA = team1,
                teamB = team2,
                isDarkTheme = isDarkTheme,
                onDismiss = { showCoinTossModal = false },
                onTossCompleted = { winnerTeam, decision ->
                    val overs = matchOversInput.toIntOrNull() ?: 5

                    // Determine Batting Team (teamAId) and Bowling Team (teamBId) based on toss winner decision
                    val (battingFirstTeamId, bowlingFirstTeamId) = if (winnerTeam.id == team1.id) {
                        if (decision == "BAT") Pair(team1.id, team2.id) else Pair(team2.id, team1.id)
                    } else {
                        if (decision == "BAT") Pair(team2.id, team1.id) else Pair(team1.id, team2.id)
                    }

                    viewModel.createMatch(
                        teamAId = battingFirstTeamId,
                        teamBId = bowlingFirstTeamId,
                        totalOvers = overs,
                        isLastPlayerBattingAllowed = isLastPlayerBattingAllowed,
                        tossWinnerTeamId = winnerTeam.id,
                        tossDecision = decision
                    )
                    showCoinTossModal = false
                    isViewingLiveMatch = true
                    strikerId = null
                    nonStrikerId = null
                    bowlerId = null
                }
            )
        }
    }

    // --- Wicket Modal Dialog ---
    if (showWicketModal) {
        var dismissalType by remember { mutableStateOf(DismissalType.BOWLED) }
        var dismissedPlayerId by remember { mutableStateOf(strikerId ?: "") }
        var selectedNextBatsmanId by remember { mutableStateOf<String?>(null) }

        val currentWkts = if (currentInnings == 1) (activeMatch?.teamAWickets ?: 0) else (activeMatch?.teamBWickets ?: 0)
        val wicketsAfterThis = currentWkts + 1
        val squadCount = battingPlayers.size.coerceAtLeast(2)
        val allOutWickets = if (activeMatch?.isLastPlayerBattingAllowed == true) squadCount else (squadCount - 1).coerceAtLeast(1)
        val isAllOut = wicketsAfterThis >= allOutWickets

        val alreadyOutIds = currentInningsBalls.filter { it.isWicket }.mapNotNull { it.dismissedPlayerId }.toSet()
        val outIdsNow = alreadyOutIds + dismissedPlayerId
        val availableNextBatsmen = battingPlayers.filter {
            it.id != strikerId && it.id != nonStrikerId && !outIdsNow.contains(it.id)
        }

        // Initialize default next batsman
        LaunchedEffect(availableNextBatsmen) {
            if (selectedNextBatsmanId == null && availableNextBatsmen.isNotEmpty()) {
                selectedNextBatsmanId = availableNextBatsmen.firstOrNull()?.id
            }
        }

        AlertDialog(
            onDismissRequest = { showWicketModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = UnsoldRed, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RECORD WICKET / OUT", color = UnsoldRed, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. WHICH BATSMAN GOT OUT?
                    Text("1. Which Batsman Got Out? (Tap to select)", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Striker option
                        val isStrikerSelected = dismissedPlayerId == strikerId
                        Surface(
                            onClick = { dismissedPlayerId = strikerId ?: "" },
                            color = if (isStrikerSelected) UnsoldRed.copy(alpha = 0.25f) else StadiumSurface,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isStrikerSelected) UnsoldRed else Color.Transparent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🏏 STRIKER", color = if (isStrikerSelected) UnsoldRed else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(striker?.name ?: "Striker", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }

                        // Non-Striker option
                        val isNonStrikerSelected = dismissedPlayerId == nonStrikerId
                        Surface(
                            onClick = { dismissedPlayerId = nonStrikerId ?: "" },
                            color = if (isNonStrikerSelected) UnsoldRed.copy(alpha = 0.25f) else StadiumSurface,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isNonStrikerSelected) UnsoldRed else Color.Transparent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("👤 NON-STRIKER", color = if (isNonStrikerSelected) UnsoldRed else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(nonStriker?.name ?: "Non-Striker", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    // 2. DISMISSAL METHOD
                    Text("2. Dismissal Method", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(DismissalType.values()) { type ->
                            FilterChip(
                                selected = dismissalType == type,
                                onClick = { dismissalType = type },
                                label = { Text(type.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UnsoldRed,
                                    selectedLabelColor = Color.White,
                                    containerColor = StadiumSurface,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    // 3. ALL OUT OR NEXT BATSMAN
                    if (isAllOut || availableNextBatsmen.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = UnsoldRed.copy(alpha = 0.2f)),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, UnsoldRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("🏁 ALL OUT! ($wicketsAfterThis / $allOutWickets Wickets)", color = UnsoldRed, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (currentInnings == 1) {
                                        "Innings 1 will conclude immediately and ${bowlingTeam?.name ?: "Opponent"} will begin batting to chase the target!"
                                    } else {
                                        "Innings 2 concluded! Match result will be calculated automatically."
                                    },
                                    color = textMain,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Text("3. Select Incoming Batsman", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 140.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableNextBatsmen) { player ->
                                val isSelected = selectedNextBatsmanId == player.id
                                Surface(
                                    onClick = { selectedNextBatsmanId = player.id },
                                    color = if (isSelected) IplGold.copy(alpha = 0.25f) else StadiumSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) IplGold else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isDarkTheme) StadiumDark else LightPitchSurface)
                                        ) {
                                            DriveImage(
                                                url = player.imageUrl,
                                                contentDescription = player.name,
                                                modifier = Modifier.fillMaxSize(),
                                                fallbackIcon = getPlayerPlaceholderIcon(player.id, player.name)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(player.name, color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(player.role.label, color = Color.Gray, fontSize = 10.sp)
                                        }
                                        if (isSelected) {
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IplGold, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val match = activeMatch
                        if (match != null && bowlerId != null && isEditable) {
                            if (match.status == MatchStatus.COMPLETED) {
                                viewModel.resumeMatch(match.id)
                            }
                            val legalBallsBefore = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                            val effectiveDismissedId = if (dismissedPlayerId.isNotBlank()) dismissedPlayerId else (strikerId ?: "")
                            val dismissedPlayerObj = players.find { it.id == effectiveDismissedId }

                            val ball = BallRecord(
                                id = "b_${System.currentTimeMillis()}",
                                matchId = match.id,
                                innings = currentInnings,
                                overNumber = legalBallsBefore / 6,
                                ballNumber = (legalBallsBefore % 6) + 1,
                                strikerId = strikerId ?: "",
                                nonStrikerId = nonStrikerId,
                                bowlerId = bowlerId ?: "",
                                runsScored = 0,
                                isWicket = true,
                                dismissalType = dismissalType,
                                dismissedPlayerId = effectiveDismissedId,
                                commentary = "WICKET! ${dismissedPlayerObj?.name ?: "Batsman"} ${dismissalType.label}"
                            )
                            viewModel.addBallRecord(ball)

                            if (!isAllOut && availableNextBatsmen.isNotEmpty()) {
                                val nextId = selectedNextBatsmanId ?: availableNextBatsmen.firstOrNull()?.id
                                if (effectiveDismissedId == strikerId) {
                                    strikerId = nextId
                                } else if (effectiveDismissedId == nonStrikerId) {
                                    nonStrikerId = nextId
                                }
                            }

                            // Check end of over on wicket ball
                            if ((legalBallsBefore + 1) % 6 == 0 && nonStrikerId != null && !isAllOut) {
                                val temp = strikerId
                                strikerId = nonStrikerId
                                nonStrikerId = temp
                                lastCompletedOverBowlerId = bowlerId
                                bowlerId = null
                                showChangeBowlerModal = true
                            }

                            showWicketModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text(if (isAllOut) "RECORD WICKET & END INNINGS 🏁" else "RECORD DISMISSAL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWicketModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Declare Winner Modal (Concluded match pop-up) ---
    if (showDeclareWinnerPopup && activeMatch != null && activeMatch.status == MatchStatus.COMPLETED) {
        val match = activeMatch
        val winnerTeam = teams.find { it.id == match.winnerTeamId }
        val teamA = teams.find { it.id == match.teamAId }
        val teamB = teams.find { it.id == match.teamBId }

        AlertDialog(
            onDismissRequest = { showDeclareWinnerPopup = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = themeAccent,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🏆 MATCH CONCLUDED!",
                        color = themeAccent,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = match.resultSummary.ifBlank { if (winnerTeam != null) "${winnerTeam.name} won!" else "Match Tied!" },
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = surfaceBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(teamA?.name ?: "Team A", color = textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${match.teamARuns}/${match.teamAWickets} (${match.teamAOversBatted} ov)", color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(teamB?.name ?: "Team B", color = textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${match.teamBRuns}/${match.teamBWickets} (${match.teamBOversBatted} ov)", color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                        }
                    }

                    Text(
                        text = "Match is saved in match history cards. You can inspect full scorecards of both teams or edit/undo if needed.",
                        color = textSub,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            showDeclareWinnerPopup = false
                            selectedMatchForScorecard = match
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("VIEW CRICBUZZ SCORECARD 📊", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    if (canScore) {
                        Button(
                            onClick = {
                                showDeclareWinnerPopup = false
                                attemptCreateMatch()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = surfaceBg, contentColor = textMain),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ CREATE NEXT MATCH 🏏", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        if (isEditable) {
                            OutlinedButton(
                                onClick = {
                                    showDeclareWinnerPopup = false
                                    viewModel.resumeMatch(match.id)
                                    viewModel.undoLastBall(match.id)
                                    Toast.makeText(context, "✏️ Last ball undone! Match reopened for editing.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeAccent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("UNDO LAST BALL & EDIT ✏️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    TextButton(
                        onClick = { showDeclareWinnerPopup = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DISMISS", color = textSub, fontSize = 11.sp)
                    }
                }
            },
            containerColor = cardBg
        )
    }

    // --- Cricbuzz-Style Full Scorecard Modal for Any Clicked Match ---
    if (selectedMatchForScorecard != null) {
        val targetMatch = selectedMatchForScorecard!!
        val targetMatchBalls = remember(targetMatch, ballRecords) {
            ballRecords.filter { it.matchId == targetMatch.id }
        }
        val isTargetEditable = viewModel.isMatchEditable(targetMatch)
        val teamA = teams.find { it.id == targetMatch.teamAId }
        val teamB = teams.find { it.id == targetMatch.teamBId }
        var selectedScorecardInnings by remember { mutableStateOf(targetMatch.currentInnings) }
        val innBalls = targetMatchBalls.filter { it.innings == selectedScorecardInnings }
        val innBattingTeamId = if (selectedScorecardInnings == 1) targetMatch.teamAId else targetMatch.teamBId
        val innBowlingTeamId = if (selectedScorecardInnings == 1) targetMatch.teamBId else targetMatch.teamAId
        val innBattingTeam = teams.find { it.id == innBattingTeamId }
        val innBowlingTeam = teams.find { it.id == innBowlingTeamId }

        AlertDialog(
            onDismissRequest = { selectedMatchForScorecard = null },
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CRICBUZZ SCORECARD",
                            color = themeAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    CricketShareHelper.shareMatchScorecard(context, targetMatch, teamA, teamB, ballRecords, players)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Share, contentDescription = "Share Scorecard", tint = themeAccent, modifier = Modifier.size(16.dp))
                            }
                            FilterChip(
                                selected = selectedScorecardInnings == 1,
                                onClick = { selectedScorecardInnings = 1 },
                                label = { Text("Inn 1: ${teamA?.shortCode ?: "T1"}", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = themeAccent,
                                    selectedLabelColor = if (isDarkTheme) Color.Black else Color.White,
                                    containerColor = surfaceBg,
                                    labelColor = textMain
                                )
                            )
                            FilterChip(
                                selected = selectedScorecardInnings == 2,
                                onClick = { selectedScorecardInnings = 2 },
                                label = { Text("Inn 2: ${teamB?.shortCode ?: "T2"}", fontSize = 10.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = themeAccent,
                                    selectedLabelColor = if (isDarkTheme) Color.Black else Color.White,
                                    containerColor = surfaceBg,
                                    labelColor = textMain
                                )
                            )
                        }
                    }
                    if (targetMatch.resultSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🏆 ${targetMatch.resultSummary}",
                            color = BidGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header banner for current innings
                    item {
                        val innRuns = if (selectedScorecardInnings == 1) targetMatch.teamARuns else targetMatch.teamBRuns
                        val innWkts = if (selectedScorecardInnings == 1) targetMatch.teamAWickets else targetMatch.teamBWickets
                        val innOvers = if (selectedScorecardInnings == 1) targetMatch.teamAOversBatted else targetMatch.teamBOversBatted

                        Card(
                            colors = CardDefaults.cardColors(containerColor = surfaceBg),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${innBattingTeam?.name ?: "Batting"} Innings",
                                    color = textMain,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$innRuns / $innWkts ($innOvers ov)",
                                    color = themeAccent,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // --- BATTING SECTION ---
                    item {
                        Text("BATTING TABLE", color = themeAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceBg.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Batter", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                            Text("R", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("B", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("4s", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("6s", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("SR", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                        }
                    }

                    // List each batsman who batted
                    val battedPlayerIds = innBalls.map { it.strikerId }.distinct()
                    val dismissedPlayerIds = innBalls.filter { it.isWicket }.mapNotNull { it.dismissedPlayerId }.distinct()
                    val allInningsBatsmenIds = (battedPlayerIds + dismissedPlayerIds).distinct()

                    if (allInningsBatsmenIds.isEmpty()) {
                        item {
                            Text("No batting records yet for this innings.", color = textSub, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                        }
                    } else {
                        items(allInningsBatsmenIds) { pId ->
                            val p = players.find { it.id == pId }
                            val pBalls = innBalls.filter { it.strikerId == pId }
                            val pRuns = pBalls.sumOf { it.runsScored }
                            val pFaced = pBalls.count { it.extraType != ExtraType.WIDE }
                            val p4s = pBalls.count { it.runsScored == 4 }
                            val p6s = pBalls.count { it.runsScored == 6 }
                            val pSR = if (pFaced > 0) String.format("%.1f", pRuns * 100.0 / pFaced) else "0.0"
                            val wicketBall = innBalls.find { it.isWicket && it.dismissedPlayerId == pId }
                            val isOut = wicketBall != null
                            val statusStr = if (isOut) {
                                val bName = players.find { it.id == wicketBall?.bowlerId }?.name ?: ""
                                "${wicketBall?.dismissalType?.label ?: "Out"} ${if (bName.isNotBlank()) "b $bName" else ""}"
                            } else {
                                "not out *"
                            }

                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(2.3f)) {
                                        Text(p?.name ?: pId, color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(statusStr, color = if (isOut) textSub else BidGreen, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text("$pRuns", color = if (pRuns >= 50) themeAccent else textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$pFaced", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$p4s", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("$p6s", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text(pSR, color = textSub, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                                }
                                HorizontalDivider(color = textMain.copy(alpha = 0.05f), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    // Extras
                    item {
                        val wides = innBalls.count { it.extraType == ExtraType.WIDE }
                        val noBalls = innBalls.count { it.extraType == ExtraType.NO_BALL }
                        val byes = innBalls.sumOf { if (it.extraType == ExtraType.BYE) it.extraRuns else 0 }
                        val legByes = innBalls.sumOf { if (it.extraType == ExtraType.LEG_BYE) it.extraRuns else 0 }
                        val totalExtras = innBalls.sumOf { it.extraRuns }

                        Surface(
                            color = surfaceBg,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Extras", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("$totalExtras (wd $wides, nb $noBalls, b $byes, lb $legByes)", color = textMain, fontSize = 11.sp)
                            }
                        }
                    }

                    // --- BOWLING SECTION ---
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("BOWLING TABLE", color = if (isDarkTheme) NeonBlue else LightPitchText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceBg.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bowler", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                            Text("O", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("M", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("R", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("W", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("ECON", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                        }
                    }

                    val bowlingPlayerIds = innBalls.map { it.bowlerId }.distinct()
                    if (bowlingPlayerIds.isEmpty()) {
                        item {
                            Text("No bowling records yet for this innings.", color = textSub, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
                        }
                    } else {
                        items(bowlingPlayerIds) { bId ->
                            val b = players.find { it.id == bId }
                            val bBalls = innBalls.filter { it.bowlerId == bId }
                            val bLegal = bBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                            val bOvers = "${bLegal / 6}.${bLegal % 6}"
                            val bRuns = bBalls.sumOf { it.runsScored + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.extraRuns else 0) }
                            val bWkts = bBalls.count { it.isWicket && it.dismissalType != DismissalType.RUN_OUT }
                            val bEcon = if (bLegal > 0) String.format("%.2f", bRuns * 6.0 / bLegal) else "0.00"

                            val oversMap = bBalls.groupBy { it.overNumber }
                            val bMaidens = oversMap.count { (_, overBalls) ->
                                val legals = overBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                val rInOver = overBalls.sumOf { it.runsScored + it.extraRuns }
                                legals == 6 && rInOver == 0
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(b?.name ?: bId, color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2.3f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(bOvers, color = textMain, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("$bMaidens", color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("$bRuns", color = textMain, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("$bWkts", color = UnsoldRed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text(bEcon, color = if (isDarkTheme) NeonBlue else LightPitchText, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                            }
                            HorizontalDivider(color = textMain.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 2.dp))
                        }
                    }

                    // Fall of Wickets
                    val wicketsList = innBalls.filter { it.isWicket }
                    if (wicketsList.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("FALL OF WICKETS", color = UnsoldRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        item {
                            val fowString = wicketsList.mapIndexed { index, wBall ->
                                val outP = players.find { it.id == wBall.dismissedPlayerId }?.name ?: "Batter"
                                val runsAtWicket = innBalls.takeWhile { it.id != wBall.id }.sumOf { it.runsScored + it.extraRuns } + wBall.runsScored + wBall.extraRuns
                                "${index + 1}-$runsAtWicket ($outP, ${wBall.overNumber}.${wBall.ballNumber} ov)"
                            }.joinToString(", ")

                            Surface(
                                color = surfaceBg,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(fowString, color = textSub, fontSize = 10.sp, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (canScore && isTargetEditable) {
                        Button(
                            onClick = {
                                viewModel.selectActiveMatch(targetMatch.id)
                                viewModel.resumeMatch(targetMatch.id)
                                viewModel.undoLastBall(targetMatch.id)
                                selectedMatchForScorecard = null
                                Toast.makeText(context, "✏️ Last ball undone! Match reopened for live scoring.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("UNDO LAST BALL & EDIT MATCH ✏️", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            CricketShareHelper.shareMatchScorecard(context, targetMatch, teamA, teamB, ballRecords, players)
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = themeAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SHARE SCORECARD 📤", fontWeight = FontWeight.Bold, color = themeAccent, fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = { selectedMatchForScorecard = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, color = textSub)
                    }
                }
            },
            containerColor = cardBg
        )
    }
}

// --- Clickable Cricbuzz-Style Match History Card ---
@Composable
fun MatchHistoryCard(
    match: Match,
    teams: List<Team>,
    isEditable: Boolean,
    isDarkTheme: Boolean,
    cardBg: Color,
    surfaceBg: Color,
    textMain: Color,
    textSub: Color,
    themeAccent: Color,
    onClick: () -> Unit = {},
    onEnterLiveClick: () -> Unit = onClick,
    onScorecardClick: () -> Unit = onClick,
    onShareClick: (() -> Unit)? = null
) {
    val teamA = teams.find { it.id == match.teamAId }
    val teamB = teams.find { it.id == match.teamBId }
    val isCompleted = match.status == MatchStatus.COMPLETED

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (!isCompleted) BidGreen.copy(alpha = 0.6f) else surfaceBg
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnterLiveClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Status badge, Overs tag & editable tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = if (!isCompleted) BidGreen.copy(alpha = 0.15f) else themeAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (!isCompleted) "● LIVE (INN ${match.currentInnings})" else "COMPLETED 🏆",
                            color = if (!isCompleted) BidGreen else themeAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "${match.totalOvers} Ov Match",
                            color = textSub,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    if (isCompleted && isEditable) {
                        Surface(
                            color = (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "EDITABLE ✏️",
                                color = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (onShareClick != null) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share Match", tint = themeAccent, modifier = Modifier.size(14.dp))
                    }
                } else {
                    Text(
                        text = "Tap for Live →",
                        color = textSub,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scores for both teams:
            // Team A
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(parseColorHex(teamA?.primaryColorHex.orEmpty())),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(teamA?.shortCode?.take(2) ?: "T1", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = teamA?.name ?: "Team A",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${match.teamARuns}/${match.teamAWickets} (${match.teamAOversBatted} ov)",
                    color = textMain,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Team B
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(parseColorHex(teamB?.primaryColorHex.orEmpty())),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(teamB?.shortCode?.take(2) ?: "T2", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = teamB?.name ?: "Team B",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${match.teamBRuns}/${match.teamBWickets} (${match.teamBOversBatted} ov)",
                    color = textMain,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }

            // Result or Live Equation Summary
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = surfaceBg, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            val statusMessage = when {
                match.resultSummary.isNotBlank() -> "🏆 ${match.resultSummary}"
                !isCompleted && match.currentInnings == 2 -> {
                    val target = match.teamARuns + 1
                    val needed = (target - match.teamBRuns).coerceAtLeast(0)
                    val totalBalls = match.totalOvers * 6
                    val fullOvers = match.teamBOversBatted.toInt()
                    val partialBalls = ((match.teamBOversBatted * 10).toInt() % 10)
                    val ballsBowled = (fullOvers * 6) + partialBalls
                    val ballsLeft = (totalBalls - ballsBowled).coerceAtLeast(0)
                    "🎯 ${teamB?.name ?: "Team B"} need $needed runs in $ballsLeft balls"
                }
                !isCompleted -> "⚡ Innings 1: ${teamA?.name ?: "Team A"} batting (${match.teamAOversBatted} ov)"
                else -> "🏆 Match Completed"
            }

            Text(
                text = statusMessage,
                color = if (isCompleted) BidGreen else themeAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Card Action Buttons (Enter Live Scorer, Scorecard, Share)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onEnterLiveClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isCompleted) BidGreen else themeAccent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.SportsCricket, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (!isCompleted) "LIVE SCORER 🏏" else "VIEW MATCH 🏏",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp
                    )
                }

                OutlinedButton(
                    onClick = onScorecardClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent)
                ) {
                    Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = themeAccent, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Scorecard",
                        color = themeAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                if (onShareClick != null) {
                    OutlinedButton(
                        onClick = onShareClick,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.5f))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = if (isDarkTheme) NeonBlue else LightPitchPrimary, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}

// --- Coin Flip & Captain Decision Component ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTossModal(
    teamA: Team,
    teamB: Team,
    isDarkTheme: Boolean = true,
    onDismiss: () -> Unit,
    onTossCompleted: (tossWinnerTeam: Team, decision: String) -> Unit
) {
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary
    var callSelection by remember { mutableStateOf("HEADS") }
    var isFlipping by remember { mutableStateOf(false) }
    var flipResult by remember { mutableStateOf<String?>(null) }
    var tossWinner by remember { mutableStateOf<Team?>(null) }
    var captainDecision by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!isFlipping) onDismiss() },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙 COIN TOSS ARENA", color = themeAccent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Flip the coin to determine Batting or Bowling choice", color = textSub, fontSize = 12.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "${teamA.name}  VS  ${teamB.name}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )

                if (flipResult == null) {
                    Text("Select Toss Call for ${teamA.shortCode}:", color = Color.LightGray, fontSize = 12.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = callSelection == "HEADS",
                            onClick = { callSelection = "HEADS" },
                            label = { Text("🪙 HEADS", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IplGold,
                                selectedLabelColor = Color.Black,
                                containerColor = StadiumSurface,
                                labelColor = Color.White
                            )
                        )

                        FilterChip(
                            selected = callSelection == "TAILS",
                            onClick = { callSelection = "TAILS" },
                            label = { Text("🪙 TAILS", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IplGold,
                                selectedLabelColor = Color.Black,
                                containerColor = StadiumSurface,
                                labelColor = Color.White
                            )
                        )
                    }
                }

                // Coin Graphic
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.5.dp, IplGold, CircleShape)
                        .background(StadiumSurface),
                    contentAlignment = Alignment.Center
                ) {
                    val currentDisplay = flipResult ?: callSelection
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (currentDisplay == "HEADS") "👑" else "🦁", fontSize = 28.sp)
                        Text(
                            text = currentDisplay,
                            color = IplGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }

                if (tossWinner != null) {
                    Surface(
                        color = BidGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 ${tossWinner?.name ?: ""} WON THE TOSS!",
                                color = if (isDarkTheme) BidGreen else Color(0xFF15803D),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Captain, choose to Bat or Bowl first:", color = textMain, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { captainDecision = "BAT" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (captainDecision == "BAT") IplGold else StadiumDark,
                                        contentColor = if (captainDecision == "BAT") Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🏏 ELECT TO BAT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                Button(
                                    onClick = { captainDecision = "BOWL" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (captainDecision == "BOWL") NeonBlue else StadiumDark,
                                        contentColor = if (captainDecision == "BOWL") Color.Black else Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("⚡ ELECT TO BOWL", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (flipResult == null) {
                Button(
                    onClick = {
                        if (!isFlipping) {
                            isFlipping = true
                            scope.launch {
                                delay(1200)
                                val outcomes = listOf("HEADS", "TAILS")
                                val landed = outcomes.random()
                                flipResult = landed
                                tossWinner = if (landed == callSelection) teamA else teamB
                                isFlipping = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = !isFlipping
                ) {
                    Text(if (isFlipping) "FLIPPING..." else "FLIP COIN NOW 🪙", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Button(
                    onClick = {
                        val w = tossWinner
                        val d = captainDecision
                        if (w != null && d != null) {
                            onTossCompleted(w, d)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BidGreen, contentColor = Color.Black),
                    enabled = tossWinner != null && captainDecision != null
                ) {
                    Text("START MATCH 🚀", fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                enabled = !isFlipping
            ) {
                Text("Cancel", color = textSub)
            }
        },
        containerColor = cardBg
    )
}

@Composable
fun DropdownPlayerPicker(
    selectedPlayer: Player?,
    players: List<Player>,
    title: String = "Select Player",
    isCompact: Boolean = false,
    isDarkTheme: Boolean = true,
    onPlayerSelected: (Player) -> Unit
) {
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary
    var showDialog by remember { mutableStateOf(false) }

    if (isCompact) {
        Surface(
            onClick = { showDialog = true },
            color = surfaceBg,
            shape = RoundedCornerShape(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Change", color = themeAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = themeAccent, modifier = Modifier.size(12.dp))
            }
        }
    } else {
    Surface(
        onClick = { showDialog = true },
        color = surfaceBg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedPlayer != null) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.dp, themeAccent, CircleShape)
                        .background(if (isDarkTheme) StadiumDark else LightPitchSurface)
                ) {
                    DriveImage(
                        url = selectedPlayer.imageUrl,
                        contentDescription = selectedPlayer.name,
                        modifier = Modifier.fillMaxSize(),
                        fallbackIcon = getPlayerPlaceholderIcon(selectedPlayer.id, selectedPlayer.name)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = selectedPlayer?.name ?: "Select Player",
                color = textMain,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = themeAccent, modifier = Modifier.size(14.dp))
        }
    }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title, color = textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                if (players.isEmpty()) {
                    Text("No players available for selection.", color = textSub, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(players) { player ->
                            val isSelected = player.id == selectedPlayer?.id
                            Surface(
                                onClick = {
                                    onPlayerSelected(player)
                                    showDialog = false
                                },
                                color = if (isSelected) themeAccent.copy(alpha = 0.2f) else surfaceBg,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, themeAccent) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isDarkTheme) StadiumDark else LightPitchCard)
                                    ) {
                                        DriveImage(
                                            url = player.imageUrl,
                                            contentDescription = player.name,
                                            modifier = Modifier.fillMaxSize(),
                                            fallbackIcon = getPlayerPlaceholderIcon(player.id, player.name)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(player.name, color = textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(player.role.label, color = textSub, fontSize = 10.sp)
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Close", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
fun DropdownTeamPicker(
    selectedTeamId: String,
    teams: List<Team>,
    isDarkTheme: Boolean = true,
    onTeamSelected: (Team) -> Unit
) {
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = teams.find { it.id == selectedTeamId }

    Box {
        Surface(
            onClick = { expanded = true },
            color = surfaceBg,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedTeam?.name ?: "Tap to choose team",
                    color = textMain,
                    fontWeight = FontWeight.Bold
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = themeAccent)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(cardBg)
        ) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name, color = textMain) },
                    onClick = {
                        onTeamSelected(team)
                        expanded = false
                    }
                )
            }
        }
    }
}
