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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

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

    var showCreateMatchModal by remember { mutableStateOf(false) }
    var showCoinTossModal by remember { mutableStateOf(false) }
    var showWicketModal by remember { mutableStateOf(false) }
    var showScorecardModal by remember { mutableStateOf(false) }
    var showTargetModal by remember { mutableStateOf(false) }
    var showEndInningsConfirmModal by remember { mutableStateOf(false) }
    var showUndoEndInningsConfirmModal by remember { mutableStateOf(false) }
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

    // Auto-assign initial players & update when innings switches
    var lastObservedInnings by remember { mutableStateOf(currentInnings) }
    LaunchedEffect(currentInnings, battingTeamId, bowlingTeamId, activeMatch?.id) {
        if (currentInnings != lastObservedInnings) {
            lastObservedInnings = currentInnings
            val lastBallOfInnings = matchBalls.filter { it.innings == currentInnings }.lastOrNull()
            if (lastBallOfInnings != null) {
                strikerId = lastBallOfInnings.strikerId
                nonStrikerId = lastBallOfInnings.nonStrikerId
                bowlerId = lastBallOfInnings.bowlerId
            } else {
                // Reset players on crease when starting fresh innings
                strikerId = battingPlayers.firstOrNull()?.id
                nonStrikerId = battingPlayers.getOrNull(1)?.id ?: battingPlayers.firstOrNull()?.id
                bowlerId = bowlingPlayers.firstOrNull()?.id
            }
            if (currentInnings == 2) {
                showTargetModal = true
                Toast.makeText(context, "🏁 Innings 1 Ended! 2nd Innings Started. Target: ${(activeMatch?.teamARuns ?: 0) + 1}", Toast.LENGTH_LONG).show()
            }
        } else if (activeMatch != null) {
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
                                Text(if (currentInnings == 1) "End Inn 1" else "End Inn 2", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (currentInnings == 2) {
                                Button(
                                    onClick = { showUndoEndInningsConfirmModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = StadiumCardDark, contentColor = NeonBlue),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Undo Inn 1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else if (mStatus == MatchStatus.COMPLETED) {
                            Button(
                                onClick = { showUndoEndInningsConfirmModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Reopen Match ↩️", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Scorer Invite Code Copy Banner Card
            val scorerCode = activeTournament?.scorerInviteCode ?: ""
            if (scorerCode.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable {
                            clipboardManager.setText(AnnotatedString(scorerCode))
                            Toast.makeText(context, "📋 Scorer Code '$scorerCode' copied!", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SCORER INVITE CODE: $scorerCode",
                                color = NeonBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            color = NeonBlue.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("COPY CODE 📋", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
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

                    // --- Cricbuzz-Style Batsmen Scorecard Card ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🏏 BATSMEN ON CREASE (${battingTeam?.name ?: "Batting Team"})",
                                        color = IplGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Swap strike button
                                    Surface(
                                        onClick = {
                                            if (nonStrikerId != null) {
                                                val temp = strikerId
                                                strikerId = nonStrikerId
                                                nonStrikerId = temp
                                            }
                                        },
                                        color = StadiumSurface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, tint = IplGold, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Swap Strike", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Cricbuzz Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StadiumSurface.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("BATTER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                                    Text("R", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("B", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("4s", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("6s", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("SR", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Striker Row with Stats
                                if (striker != null) {
                                    val sBalls = currentInningsBalls.filter { it.strikerId == striker.id }
                                    val sRuns = sBalls.sumOf { it.runsScored }
                                    val sFaced = sBalls.count { it.extraType != ExtraType.WIDE }
                                    val s4s = sBalls.count { it.runsScored == 4 }
                                    val s6s = sBalls.count { it.runsScored == 6 }
                                    val sSR = if (sFaced > 0) String.format("%.1f", sRuns * 100.0 / sFaced) else "0.0"

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(2.3f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, IplGold, CircleShape).background(StadiumDark)) {
                                                DriveImage(url = striker.imageUrl, contentDescription = striker.name, modifier = Modifier.fillMaxSize())
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(striker.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text("*", color = IplGold, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                                }
                                                Text("Striker", color = IplGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text("$sRuns", color = IplGold, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$sFaced", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$s4s", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text("$s6s", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(sSR, color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                                    }
                                }

                                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                                    DropdownPlayerPicker(
                                        selectedPlayer = striker,
                                        players = battingPlayers,
                                        title = "Select Striker",
                                        onPlayerSelected = { strikerId = it.id }
                                    )
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                // Non-Striker Row with Stats
                                val isLastBatsmanSolo = match.isLastPlayerBattingAllowed &&
                                        ((currentInnings == 1 && match.teamAWickets >= (battingPlayers.size.coerceAtLeast(10) - 1)) ||
                                         (currentInnings == 2 && match.teamBWickets >= (battingPlayers.size.coerceAtLeast(10) - 1)))

                                if (isLastBatsmanSolo) {
                                    Text("⚡ [SOLO BATTING - LAST REMAINING BATSMAN]", color = NeonBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                } else if (nonStriker != null) {
                                    val nsBalls = currentInningsBalls.filter { it.strikerId == nonStriker.id }
                                    val nsRuns = nsBalls.sumOf { it.runsScored }
                                    val nsFaced = nsBalls.count { it.extraType != ExtraType.WIDE }
                                    val ns4s = nsBalls.count { it.runsScored == 4 }
                                    val ns6s = nsBalls.count { it.runsScored == 6 }
                                    val nsSR = if (nsFaced > 0) String.format("%.1f", nsRuns * 100.0 / nsFaced) else "0.0"

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(2.3f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, NeonBlue, CircleShape).background(StadiumDark)) {
                                                DriveImage(url = nonStriker.imageUrl, contentDescription = nonStriker.name, modifier = Modifier.fillMaxSize())
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(nonStriker.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("Non-Striker", color = Color.Gray, fontSize = 9.sp)
                                            }
                                        }
                                        Text("$nsRuns", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$nsFaced", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$ns4s", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text("$ns6s", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(nsSR, color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                                    }

                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.End) {
                                        DropdownPlayerPicker(
                                            selectedPlayer = nonStriker,
                                            players = battingPlayers.filter { it.id != strikerId },
                                            title = "Select Non-Striker",
                                            onPlayerSelected = { nonStrikerId = it.id }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- Cricbuzz-Style Bowler Card ---
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ CURRENT BOWLER (${bowlingTeam?.name ?: "Opponent Team"})",
                                        color = NeonBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    DropdownPlayerPicker(
                                        selectedPlayer = bowler,
                                        players = bowlingPlayers,
                                        title = "Select Bowler",
                                        onPlayerSelected = { bowlerId = it.id }
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Cricbuzz Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(StadiumSurface.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("BOWLER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                                    Text("O", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("M", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("R", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("W", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("ECON", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                                }

                                Spacer(modifier = Modifier.height(6.dp))

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
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(2.3f), verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(24.dp).clip(CircleShape).border(1.5.dp, NeonBlue, CircleShape).background(StadiumDark)) {
                                                DriveImage(url = bowler.imageUrl, contentDescription = bowler.name, modifier = Modifier.fillMaxSize())
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(bowler.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(bOvers, color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$bMaidens", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text("$bRuns", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                        Text("$bWkts", color = UnsoldRed, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(bEcon, color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                                    }
                                } else {
                                    Text("Select Bowler", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(6.dp))
                                }
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Wicket Button
                                    Button(
                                        onClick = { showWicketModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1.1f),
                                        enabled = match.status != MatchStatus.COMPLETED
                                    ) {
                                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("WICKET!", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }

                                    // End Innings Button (directly on keypad)
                                    Button(
                                        onClick = { showEndInningsConfirmModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed.copy(alpha = 0.85f), contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1.2f),
                                        enabled = match.status != MatchStatus.COMPLETED
                                    ) {
                                        Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(if (currentInnings == 1) "END INN 1" else "END INN 2", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }

                                    // Undo Ball Button
                                    Button(
                                        onClick = {
                                            val undone = viewModel.undoLastBall(match.id)
                                            if (undone != null) {
                                                strikerId = undone.strikerId
                                                nonStrikerId = undone.nonStrikerId
                                                bowlerId = undone.bowlerId
                                            }
                                            showTargetModal = false
                                            Toast.makeText(context, "↩️ Last ball undone", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StadiumSurface, contentColor = Color.LightGray),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("UNDO", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }

                                    // Undo End Innings Button (visible when Innings 2 is active or match completed)
                                    if (currentInnings == 2 || match.status == MatchStatus.COMPLETED) {
                                        Button(
                                            onClick = { showUndoEndInningsConfirmModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue.copy(alpha = 0.2f), contentColor = NeonBlue),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Undo Inn", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                        }
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
                        Toast.makeText(context, if (currentInnings == 1) "Innings 1 Ended! Target set. (Tap 'Undo Inn 1' to revert)" else "Innings 2 Ended!", Toast.LENGTH_LONG).show()
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
                    color = Color.LightGray,
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
                    Text("1. Which Batsman Got Out? (Tap to select)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

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
                                Text(striker?.name ?: "Striker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
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
                                Text(nonStriker?.name ?: "Non-Striker", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }

                    // 2. DISMISSAL METHOD
                    Text("2. Dismissal Method", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

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
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Text("3. Select Incoming Batsman", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

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
                                                .background(StadiumDark)
                                        ) {
                                            DriveImage(url = player.imageUrl, contentDescription = player.name, modifier = Modifier.fillMaxSize())
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                        if (match != null && bowlerId != null) {
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
                                showChangeBowlerModal = true
                                bowlerId = null
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
            containerColor = StadiumCardDark
        )
    }

    // --- Cricbuzz-Style Full Scorecard Modal ---
    if (showScorecardModal) {
        var selectedScorecardInnings by remember { mutableStateOf(currentInnings) }
        val innBalls = matchBalls.filter { it.innings == selectedScorecardInnings }
        val innBattingTeamId = if (selectedScorecardInnings == 1) activeMatch?.teamAId else activeMatch?.teamBId
        val innBowlingTeamId = if (selectedScorecardInnings == 1) activeMatch?.teamBId else activeMatch?.teamAId
        val innBattingTeam = teams.find { it.id == innBattingTeamId }
        val innBowlingTeam = teams.find { it.id == innBowlingTeamId }

        AlertDialog(
            onDismissRequest = { showScorecardModal = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CRICBUZZ SCORECARD", color = IplGold, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedScorecardInnings == 1,
                            onClick = { selectedScorecardInnings = 1 },
                            label = { Text("Inn 1", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IplGold,
                                selectedLabelColor = Color.Black,
                                containerColor = StadiumSurface,
                                labelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedScorecardInnings == 2,
                            onClick = { selectedScorecardInnings = 2 },
                            label = { Text("Inn 2", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = IplGold,
                                selectedLabelColor = Color.Black,
                                containerColor = StadiumSurface,
                                labelColor = Color.White
                            )
                        )
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header banner
                    item {
                        val innRuns = if (selectedScorecardInnings == 1) activeMatch?.teamARuns ?: 0 else activeMatch?.teamBRuns ?: 0
                        val innWkts = if (selectedScorecardInnings == 1) activeMatch?.teamAWickets ?: 0 else activeMatch?.teamBWickets ?: 0
                        val innOvers = if (selectedScorecardInnings == 1) activeMatch?.teamAOversBatted ?: 0.0 else activeMatch?.teamBOversBatted ?: 0.0

                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumSurface),
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
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$innRuns / $innWkts ($innOvers ov)",
                                    color = IplGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // --- BATTING SECTION ---
                    item {
                        Text("BATTING", color = IplGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StadiumSurface.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Batter", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                            Text("R", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("B", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("4s", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("6s", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("SR", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                        }
                    }

                    // List each batsman who batted
                    val battedPlayerIds = innBalls.map { it.strikerId }.distinct()
                    val dismissedPlayerIds = innBalls.filter { it.isWicket }.mapNotNull { it.dismissedPlayerId }.distinct()
                    val allInningsBatsmenIds = (battedPlayerIds + dismissedPlayerIds).distinct()

                    if (allInningsBatsmenIds.isEmpty()) {
                        item {
                            Text("No batting records yet for this innings.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
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
                                        Text(p?.name ?: pId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                        Text(statusStr, color = if (isOut) Color.Gray else BidGreen, fontSize = 9.sp)
                                    }
                                    Text("$pRuns", color = if (pRuns >= 50) IplGold else Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$pFaced", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$p4s", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("$p6s", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text(pSR, color = Color.LightGray, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(top = 4.dp))
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
                            color = StadiumDark,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Extras", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("$totalExtras (wd $wides, nb $noBalls, b $byes, lb $legByes)", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }

                    // --- BOWLING SECTION ---
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("BOWLING", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(StadiumSurface.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bowler", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.3f))
                            Text("O", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("M", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("R", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                            Text("W", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text("ECON", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                        }
                    }

                    val bowlingPlayerIds = innBalls.map { it.bowlerId }.distinct()
                    if (bowlingPlayerIds.isEmpty()) {
                        item {
                            Text("No bowling records yet for this innings.", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(6.dp))
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
                                Text(b?.name ?: bId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2.3f), maxLines = 1)
                                Text(bOvers, color = Color.White, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("$bMaidens", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("$bRuns", color = Color.White, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("$bWkts", color = UnsoldRed, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text(bEcon, color = NeonBlue, fontSize = 10.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1.1f))
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 2.dp))
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
                                color = StadiumDark,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(fowString, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.padding(8.dp))
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
    title: String = "Select Player",
    onPlayerSelected: (Player) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        onClick = { showDialog = true },
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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title, color = IplGold, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                if (players.isEmpty()) {
                    Text("No players available for selection.", color = Color.Gray, fontSize = 13.sp)
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
                                color = if (isSelected) IplGold.copy(alpha = 0.2f) else StadiumSurface,
                                shape = RoundedCornerShape(8.dp),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, IplGold) else null,
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
                                            .background(StadiumDark)
                                    ) {
                                        DriveImage(url = player.imageUrl, contentDescription = player.name, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(player.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(player.role.label, color = Color.Gray, fontSize = 10.sp)
                                    }
                                    if (isSelected) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = IplGold, modifier = Modifier.size(18.dp))
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
                    Text("Close", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
        )
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
