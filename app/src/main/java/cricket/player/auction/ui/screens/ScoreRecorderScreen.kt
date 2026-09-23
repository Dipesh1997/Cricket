package cricket.player.auction.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.*
import cricket.player.auction.ui.components.DriveImage
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreRecorderScreen(viewModel: AuctionViewModel) {
    val context = LocalContext.current
    val teams by viewModel.teams.collectAsState()
    val players by viewModel.players.collectAsState()
    val activeMatchState by viewModel.activeMatch.collectAsState()
    val activeMatch = activeMatchState
    val ballRecords by viewModel.ballRecords.collectAsState()

    var showCreateMatchModal by remember { mutableStateOf(false) }
    var showCoinTossModal by remember { mutableStateOf(false) }
    var showWicketModal by remember { mutableStateOf(false) }
    var showScorecardModal by remember { mutableStateOf(false) }
    var showTargetModal by remember { mutableStateOf(false) }
    var showEndInningsConfirmModal by remember { mutableStateOf(false) }
    var showChangeBowlerModal by remember { mutableStateOf(false) }
    var showDirectEndMatchModal by remember { mutableStateOf(false) }

    // Match Setup Inputs
    var selectedTeamAId by remember { mutableStateOf("") }
    var selectedTeamBId by remember { mutableStateOf("") }
    var matchOversInput by remember { mutableStateOf("5") }
    var isLastPlayerBattingAllowed by remember { mutableStateOf(true) }

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
    val soldBatting = players.filter { it.soldToTeamId == battingTeamId }
    val battingPlayers = if (soldBatting.isNotEmpty()) soldBatting else players.filter { it.tournamentId == activeMatch?.tournamentId }

    val soldBowling = players.filter { it.soldToTeamId == bowlingTeamId }
    val bowlingPlayers = if (soldBowling.isNotEmpty()) soldBowling else players.filter { it.tournamentId == activeMatch?.tournamentId }

    val striker = players.find { it.id == strikerId }
    val nonStriker = players.find { it.id == nonStrikerId }
    val bowler = players.find { it.id == bowlerId }

    // Auto-assign initial players & update dropdowns when team switches
    LaunchedEffect(activeMatch?.id, currentInnings, battingTeamId, bowlingTeamId) {
        if (activeMatch != null) {
            if (strikerId == null || battingPlayers.none { it.id == strikerId }) {
                strikerId = battingPlayers.firstOrNull()?.id
            }
            if (nonStrikerId == null || battingPlayers.none { it.id == nonStrikerId }) {
                nonStrikerId = battingPlayers.getOrNull(1)?.id ?: battingPlayers.firstOrNull()?.id
            }
            if (bowlerId == null || bowlingPlayers.none { it.id == bowlerId }) {
                bowlerId = bowlingPlayers.firstOrNull()?.id
            }
        }
    }

    // Auto-show Target Modal on Innings 2 start
    LaunchedEffect(currentInnings, activeMatch?.status) {
        if (currentInnings == 2 && activeMatch?.status == MatchStatus.LIVE_INNINGS_2) {
            showTargetModal = true
        }
    }

    Scaffold(
        containerColor = StadiumDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action Bar Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE SCORER & SCORECARD",
                    color = IplGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = { showCreateMatchModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("New Match", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (activeMatch != null) {
                        Button(
                            onClick = { showScorecardModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = StadiumCardDark, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IplGold),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Scorecard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        val mStatus = activeMatch?.status
                        if (mStatus != null && mStatus != MatchStatus.COMPLETED) {
                            Button(
                                onClick = { showDirectEndMatchModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("End Match", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showEndInningsConfirmModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("End Innings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (activeMatch == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.SportsCricket, contentDescription = null, tint = IplGold, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No Active Match Running", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Setup a new match with Coin Toss & Captain Bat/Bowl decisions to start scoring", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { showCreateMatchModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Casino, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TOSS COIN & CREATE MATCH", fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else if (activeMatch != null) {
                val match = activeMatch
                val tossWinnerTeam = teams.find { it.id == match.tossWinnerTeamId }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- Top Main Live Scoreboard ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                            shape = RoundedCornerShape(18.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, IplGold)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatChip(
                                        text = if (match.status == MatchStatus.COMPLETED) "MATCH COMPLETED" else "INNINGS $currentInnings LIVE",
                                        backgroundColor = if (match.status == MatchStatus.COMPLETED) BidGreen.copy(alpha = 0.2f) else StadiumSurface,
                                        textColor = if (match.status == MatchStatus.COMPLETED) BidGreen else IplGold
                                    )

                                    if (match.isLastPlayerBattingAllowed) {
                                        StatChip(
                                            text = "Last Player Batting Allowed",
                                            backgroundColor = NeonBlue.copy(alpha = 0.2f),
                                            textColor = NeonBlue
                                        )
                                    }
                                }

                                // Toss Result Badge
                                if (tossWinnerTeam != null && match.tossDecision != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    StatChip(
                                        text = "🪙 Toss: ${tossWinnerTeam.shortCode} won & elected to ${match.tossDecision}",
                                        backgroundColor = StadiumSurface,
                                        textColor = Color.LightGray
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = battingTeam?.name ?: "Batting Team",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        val runs = if (currentInnings == 1) match.teamARuns else match.teamBRuns
                                        val wkts = if (currentInnings == 1) match.teamAWickets else match.teamBWickets
                                        val overs = if (currentInnings == 1) match.teamAOversBatted else match.teamBOversBatted

                                        Text(
                                            text = "$runs / $wkts",
                                            color = IplGold,
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "($overs / ${match.totalOvers} Overs)",
                                            color = Color.LightGray,
                                            fontSize = 13.sp
                                        )
                                    }

                                    if (currentInnings == 2) {
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
                                            Text(text = "TARGET: $target", color = BidGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                            Text(text = "Need $needed in $remainingBalls b", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = "RRR: $rrrVal", color = IplGold, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }

                                if (match.status == MatchStatus.COMPLETED && match.winnerTeamId != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = BidGreen.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val winnerName = teams.find { it.id == match.winnerTeamId }?.name ?: "TIE"
                                        val summaryStr = if (match.resultSummary.isNotBlank()) {
                                            "🏆 ${match.resultSummary}"
                                        } else if (winnerName == "TIE") {
                                            "MATCH TIED!"
                                        } else {
                                            "🏆 WINNER: $winnerName"
                                        }
                                        Text(
                                            text = summaryStr,
                                            color = BidGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Striker & Non-Striker Cards with Circular Cutouts ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("BATSMEN ON CREASE (${battingTeam?.name ?: "Batting Team"})", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Striker Picker / Display
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, IplGold, CircleShape)
                                                .background(StadiumDark)
                                        ) {
                                            if (striker != null) {
                                                DriveImage(url = striker.imageUrl, contentDescription = striker.name, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Text("🏏", fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = striker?.name ?: "Select Striker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(text = "Striker 🏏", color = IplGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    DropdownPlayerPicker(
                                        selectedPlayer = striker,
                                        players = battingPlayers,
                                        onPlayerSelected = { strikerId = it.id }
                                    )
                                }

                                Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

                                // Non-Striker Picker / Display & Solo Batting Rule
                                val isLastBatsmanSolo = match.isLastPlayerBattingAllowed &&
                                        ((currentInnings == 1 && match.teamAWickets >= (battingPlayers.size.coerceAtLeast(10) - 1)) ||
                                         (currentInnings == 2 && match.teamBWickets >= (battingPlayers.size.coerceAtLeast(10) - 1)))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .border(1.5.dp, NeonBlue, CircleShape)
                                                .background(StadiumDark)
                                        ) {
                                            if (nonStriker != null) {
                                                DriveImage(url = nonStriker.imageUrl, contentDescription = nonStriker.name, modifier = Modifier.fillMaxSize())
                                            } else {
                                                Text("👤", fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isLastBatsmanSolo) "[SOLO BATTING - LAST PLAYER]" else nonStriker?.name ?: "Select Non-Striker",
                                                color = if (isLastBatsmanSolo) NeonBlue else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(text = "Non-Striker", color = Color.Gray, fontSize = 11.sp)
                                        }
                                    }

                                    if (!isLastBatsmanSolo) {
                                        DropdownPlayerPicker(
                                            selectedPlayer = nonStriker,
                                            players = battingPlayers.filter { it.id != strikerId },
                                            onPlayerSelected = { nonStrikerId = it.id }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Bowler Selection Card (Opponent Team) ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, NeonBlue, CircleShape)
                                            .background(StadiumDark)
                                    ) {
                                        if (bowler != null) {
                                            DriveImage(url = bowler.imageUrl, contentDescription = bowler.name, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Text("⚡", fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = bowler?.name ?: "Select Bowler", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(text = "Current Bowler (${bowlingTeam?.name ?: "Opponent Team"})", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                DropdownPlayerPicker(
                                    selectedPlayer = bowler,
                                    players = bowlingPlayers,
                                    onPlayerSelected = { bowlerId = it.id }
                                )
                            }
                        }
                    }

                    // --- Ball-by-Ball Instant Scoring Keypad ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("SCORING KEYPAD", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                                Spacer(modifier = Modifier.height(10.dp))

                                // Extras Selector
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ExtraType.values().forEach { extra ->
                                        FilterChip(
                                            selected = selectedExtraType == extra,
                                            onClick = { selectedExtraType = if (selectedExtraType == extra) ExtraType.NONE else extra },
                                            label = { Text(extra.name, fontSize = 10.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = NeonBlue,
                                                selectedLabelColor = Color.Black,
                                                containerColor = StadiumSurface,
                                                labelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Runs Buttons: 0, 1, 2, 3, 4, 6
                                val runsList = listOf(0, 1, 2, 3, 4, 6)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(runsList) { run ->
                                        Button(
                                            onClick = {
                                                if (strikerId != null && bowlerId != null && match.status != MatchStatus.COMPLETED) {
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

                                                    // End of Over (6 legal balls completed): swap strike & prompt bowler change!
                                                    val isOverComplete = isLegal && (ballNum == 6)
                                                    if (isOverComplete && newNonStriker != null) {
                                                        val temp = newStriker
                                                        newStriker = newNonStriker
                                                        newNonStriker = temp

                                                        showChangeBowlerModal = true
                                                        bowlerId = null
                                                    }

                                                    strikerId = newStriker
                                                    nonStrikerId = newNonStriker
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (run == 4 || run == 6) IplGold else StadiumSurface,
                                                contentColor = if (run == 4 || run == 6) Color.Black else Color.White
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(52.dp),
                                            enabled = match.status != MatchStatus.COMPLETED
                                        ) {
                                            Text("$run", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Wicket Button
                                    Button(
                                        onClick = { showWicketModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f),
                                        enabled = match.status != MatchStatus.COMPLETED
                                    ) {
                                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("WICKET!", fontWeight = FontWeight.ExtraBold)
                                    }

                                    // Undo Ball Button
                                    Button(
                                        onClick = { viewModel.undoLastBall(match.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = StadiumSurface, contentColor = Color.LightGray),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("UNDO BALL", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // --- Over Commentary Timeline ---
                    item {
                        Text("CURRENT OVER TIMELINE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val currentOverBalls = currentInningsBalls.takeLast(6)
                            items(currentOverBalls) { ball ->
                                val text = if (ball.isWicket) "W" else if (ball.extraType != ExtraType.NONE) "${ball.runsScored}${ball.extraType.name.take(1)}" else "${ball.runsScored}"
                                val color = if (ball.isWicket) UnsoldRed else if (ball.runsScored == 4 || ball.runsScored == 6) IplGold else StadiumSurface

                                Surface(
                                    color = color,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = text,
                                            color = if (ball.runsScored == 4 || ball.runsScored == 6) Color.Black else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Direct End Match / Declare Result Option ---
                    if (match.status != MatchStatus.COMPLETED) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "DIRECT END MATCH / DECLARE RESULT",
                                            color = IplGold,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Declare winner directly & update NRR without completing full overs",
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = { showDirectEndMatchModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("DECLARE WINNER", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
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
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.endActiveInnings()
                        showEndInningsConfirmModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("END INNINGS NOW", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndInningsConfirmModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
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
            title = { Text("Declare Match Winner & NRR", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("SELECT WINNING TEAM", color = IplGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            Text("VICTORY MARGIN TYPE", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        Text("${teamA?.name ?: "Team A"} Final Runs & Overs:", color = Color.LightGray, fontSize = 11.sp)
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
            containerColor = StadiumCardDark
        )
    }

    // --- Over Completed / Bowler Change Modal ---
    if (showChangeBowlerModal) {
        AlertDialog(
            onDismissRequest = { showChangeBowlerModal = false },
            title = { Text("OVER COMPLETED! ⚡", color = IplGold, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("The over is complete! Strike has been rotated automatically.", color = Color.White, fontSize = 13.sp)
                    Text("Select new bowler for the next over from ${bowlingTeam?.name ?: "opponent team"}:", color = Color.LightGray, fontSize = 12.sp)

                    DropdownPlayerPicker(
                        selectedPlayer = bowler,
                        players = bowlingPlayers,
                        onPlayerSelected = {
                            bowlerId = it.id
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showChangeBowlerModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = bowlerId != null
                ) {
                    Text("CONTINUE MATCH", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = StadiumCardDark
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
                    Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = IplGold)
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
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )

                    Surface(
                        color = StadiumSurface,
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
            containerColor = StadiumCardDark
        )
    }

    // --- Create Match Setup Dialog ---
    if (showCreateMatchModal) {
        AlertDialog(
            onDismissRequest = { showCreateMatchModal = false },
            title = { Text("Setup New Cricket Match", color = Color.White, fontWeight = FontWeight.Bold) },
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

                    OutlinedTextField(
                        value = matchOversInput,
                        onValueChange = { matchOversInput = it },
                        label = { Text("Match Overs Limit (e.g. 5, 10, 20)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = StadiumSurface),
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
                                    color = IplGold,
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
                        if (selectedTeamAId.isNotBlank() && selectedTeamBId.isNotBlank()) {
                            showCreateMatchModal = false
                            showCoinTossModal = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = selectedTeamAId.isNotBlank() && selectedTeamBId.isNotBlank()
                ) {
                    Text("TOSS COIN & DECIDE 🪙", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateMatchModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
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
                }
            )
        }
    }

    // --- Wicket Modal Dialog ---
    if (showWicketModal) {
        var dismissalType by remember { mutableStateOf(DismissalType.BOWLED) }
        var nextBatsmanId by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showWicketModal = false },
            title = { Text("RECORD WICKET", color = UnsoldRed, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select Dismissal Type", color = Color.LightGray, fontSize = 12.sp)

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

                    Text("Select Next Batsman", color = Color.LightGray, fontSize = 12.sp)
                    DropdownPlayerPicker(
                        selectedPlayer = players.find { it.id == nextBatsmanId },
                        players = battingPlayers.filter { it.id != strikerId && it.id != nonStrikerId },
                        onPlayerSelected = { nextBatsmanId = it.id }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val match = activeMatch
                        if (match != null && strikerId != null && bowlerId != null) {
                            val legalBallsBefore = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
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
                                dismissedPlayerId = strikerId,
                                commentary = "WICKET! ${striker?.name} ${dismissalType.label}"
                            )
                            viewModel.addBallRecord(ball)
                            if (nextBatsmanId != null) {
                                strikerId = nextBatsmanId
                            }

                            // Check end of over on wicket ball
                            if ((legalBallsBefore + 1) % 6 == 0 && nonStrikerId != null) {
                                val temp = strikerId
                                strikerId = nonStrikerId
                                nonStrikerId = temp
                                showChangeBowlerModal = true
                                bowlerId = null
                            }

                            showWicketModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("RECORD DISMISSAL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWicketModal = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
        )
    }

    // --- Full Scorecard Modal ---
    if (showScorecardModal) {
        AlertDialog(
            onDismissRequest = { showScorecardModal = false },
            title = { Text("FULL MATCH SCORECARD", color = IplGold, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentInningsBalls.reversed()) { ball ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Over ${ball.overNumber}.${ball.ballNumber}",
                                    color = IplGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = ball.commentary,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text(
                                    text = if (ball.isWicket) "W" else "${ball.runsScored}",
                                    color = if (ball.isWicket) UnsoldRed else Color.LightGray,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showScorecardModal = false },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = StadiumCardDark
        )
    }
}

// --- Coin Flip & Captain Decision Component ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinTossModal(
    teamA: Team,
    teamB: Team,
    onDismiss: () -> Unit,
    onTossCompleted: (tossWinnerTeam: Team, decision: String) -> Unit
) {
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
                    Text("🪙 COIN TOSS ARENA", color = IplGold, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Flip the coin to determine Batting or Bowling choice", color = Color.Gray, fontSize = 12.sp)
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
                                color = BidGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Captain, choose to Bat or Bowl first:", color = Color.White, fontSize = 12.sp)
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
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = StadiumCardDark
    )
}

@Composable
fun DropdownPlayerPicker(
    selectedPlayer: Player?,
    players: List<Player>,
    onPlayerSelected: (Player) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            onClick = { expanded = true },
            color = StadiumSurface,
            shape = RoundedCornerShape(10.dp)
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
                            .border(1.dp, IplGold, CircleShape)
                            .background(StadiumDark)
                    ) {
                        DriveImage(url = selectedPlayer.imageUrl, contentDescription = selectedPlayer.name, modifier = Modifier.fillMaxSize())
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = selectedPlayer?.name ?: "Select Player",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = IplGold)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(StadiumCardDark)
        ) {
            players.forEach { player ->
                DropdownMenuItem(
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .border(1.dp, IplGold, CircleShape)
                                .background(StadiumDark)
                        ) {
                            DriveImage(url = player.imageUrl, contentDescription = player.name, modifier = Modifier.fillMaxSize())
                        }
                    },
                    text = { Text(player.name, color = Color.White) },
                    onClick = {
                        onPlayerSelected(player)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DropdownTeamPicker(
    selectedTeamId: String,
    teams: List<Team>,
    onTeamSelected: (Team) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTeam = teams.find { it.id == selectedTeamId }

    Box {
        Surface(
            onClick = { expanded = true },
            color = StadiumSurface,
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = IplGold)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(StadiumCardDark)
        ) {
            teams.forEach { team ->
                DropdownMenuItem(
                    text = { Text(team.name, color = Color.White) },
                    onClick = {
                        onTeamSelected(team)
                        expanded = false
                    }
                )
            }
        }
    }
}
