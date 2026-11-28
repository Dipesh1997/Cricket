package cricket.player.auction.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.net.Uri
import android.view.TextureView
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cricket.player.auction.data.remote.YouTubeBroadcastResult
import cricket.player.auction.model.*
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@Composable
fun ClassicCrichuntScorerView(
    match: Match,
    teams: List<Team>,
    players: List<Player>,
    currentInningsBalls: List<BallRecord>,
    battingTeam: Team?,
    bowlingTeam: Team?,
    striker: Player?,
    nonStriker: Player?,
    bowler: Player?,
    battingPlayers: List<Player>,
    selectedExtraType: ExtraType,
    isEditable: Boolean,
    isDarkTheme: Boolean,
    isBackgroundCameraOn: Boolean = false,
    onSelectExtra: (ExtraType) -> Unit,
    onScoreRuns: (Int) -> Unit,
    onRecordExtraBall: (ExtraType, Int) -> Unit = { _, _ -> },
    onOpenWicket: () -> Unit,
    onUndoLastBall: () -> Unit,
    onSwapStrike: () -> Unit,
    onReplaceBatter: (isStriker: Boolean, newPlayerId: String) -> Unit,
    onChangeBowler: () -> Unit,
    onStartChase: () -> Unit,
    onEndInnings: () -> Unit,
    onDirectEndMatch: () -> Unit,
    onUndoEndInnings: () -> Unit,
    onOpenYouTubeStream: () -> Unit = {}
) {
    val navyHeaderBg = Color(0xFF0A1D37)
    val crichuntBlue4 = Color(0xFF1976D2)
    val crichuntGreen6 = Color(0xFF388E3C)
    val crichuntOutRed = Color(0xFFD32F2F)
    val padBg = if (isDarkTheme) StadiumDark else Color(0xFFF3F5F8)
    val tileBg = if (isDarkTheme) StadiumCardDark else Color.White
    val tileText = if (isDarkTheme) Color.White else Color(0xFF111827)

    var showReplaceBatterDialog by remember { mutableStateOf(false) }
    var replaceTargetIsStriker by remember { mutableStateOf(true) }
    var showFiveSevenDialog by remember { mutableStateOf(false) }
    var showMoreActionsDialog by remember { mutableStateOf(false) }
    var activeExtraPickerType by remember { mutableStateOf<ExtraType?>(null) }

    val currentInnings = match.currentInnings
    val runs = if (currentInnings == 1) match.teamARuns else match.teamBRuns
    val wkts = if (currentInnings == 1) match.teamAWickets else match.teamBWickets
    val oversDouble = if (currentInnings == 1) match.teamAOversBatted else match.teamBOversBatted
    val legalBallsTotal = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
    val oversStr = "${legalBallsTotal / 6}.${legalBallsTotal % 6}"
    val crr = if (legalBallsTotal > 0) (runs * 6.0) / legalBallsTotal else 0.0
    val projectedScore = (crr * match.totalOvers).toInt()
    val targetRuns = match.teamARuns + 1
    val runsNeeded = (targetRuns - runs).coerceAtLeast(0)
    val ballsRemaining = (match.totalOvers * 6 - legalBallsTotal).coerceAtLeast(0)
    val rrr = if (ballsRemaining > 0) (runsNeeded * 6.0) / ballsRemaining else 0.0

    val tossWinner = teams.find { it.id == match.tossWinnerTeamId }
    val statusLine = when {
        match.status == MatchStatus.COMPLETED && match.resultSummary.isNotBlank() -> match.resultSummary
        currentInnings == 2 -> "Target: $targetRuns • Need $runsNeeded in $ballsRemaining balls"
        tossWinner != null && !match.tossDecision.isNullOrBlank() ->
            "${tossWinner.shortCode} won the toss and elected to ${match.tossDecision.lowercase()}."
        else -> "${battingTeam?.name ?: "Team"} batting • Innings 1"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isBackgroundCameraOn) Color.Black.copy(alpha = 0.25f) else padBg)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Top Dark Navy Crichunt Header Card ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isBackgroundCameraOn) navyHeaderBg.copy(alpha = 0.78f) else navyHeaderBg)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // White Scoreboard Strip
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = battingTeam?.shortCode ?: "TEAM",
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "$runs/$wkts",
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp
                        )
                    }

                    // Center Circular Team Logo Badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(2.dp, IplGold, CircleShape)
                            .background(Color(0xFF0F294A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = battingTeam?.shortCode?.take(3) ?: "CRK",
                            color = IplGold,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "($oversStr/${match.totalOvers})",
                            color = Color(0xFF1F2937),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        if (currentInnings == 2) {
                            Text(
                                text = "Target: $targetRuns",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Toss / Target Summary Line
            Text(
                text = statusLine,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // CRR & Projected Score / RRR Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Text("CRR : ", color = Color.LightGray, fontSize = 11.sp)
                    Text(String.format("%.1f", crr), color = IplGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                if (currentInnings == 2) {
                    Row {
                        Text("RRR : ", color = Color.LightGray, fontSize = 11.sp)
                        Text(String.format("%.2f", rrr), color = IplGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                } else {
                    Row {
                        Text("Projected Score : ", color = Color.LightGray, fontSize = 11.sp)
                        Text("$projectedScore", color = IplGold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // Two Side-by-Side Batter Cards (Striker & Non-Striker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val sBalls = striker?.let { s -> currentInningsBalls.filter { it.strikerId == s.id } } ?: emptyList()
                val sRuns = sBalls.sumOf { it.runsScored }
                val sFaced = sBalls.count { it.extraType != ExtraType.WIDE }

                val nsBalls = nonStriker?.let { ns -> currentInningsBalls.filter { it.strikerId == ns.id } } ?: emptyList()
                val nsRuns = nsBalls.sumOf { it.runsScored }
                val nsFaced = nsBalls.count { it.extraType != ExtraType.WIDE }

                Surface(
                    onClick = {
                        replaceTargetIsStriker = true
                        showReplaceBatterDialog = true
                    },
                    color = Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = striker?.name ?: "Select Striker",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$sRuns($sFaced)",
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Surface(
                    onClick = {
                        replaceTargetIsStriker = false
                        showReplaceBatterDialog = true
                    },
                    color = Color(0xFFBFC5CE),
                    shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = Color(0xFF374151),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = nonStriker?.name ?: "Non-Striker",
                                color = Color(0xFF1F2937),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$nsRuns($nsFaced)",
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Black Action Buttons: Swap Strike | Replace Batter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSwapStrike,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("Swap Strike", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        replaceTargetIsStriker = true
                        showReplaceBatterDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Text("Replace Batter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bowler Strip: Name & O-M-R-W
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeBowler() }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val bBalls = bowler?.let { bw -> currentInningsBalls.filter { it.bowlerId == bw.id } } ?: emptyList()
                val bLegal = bBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                val bOvers = "${bLegal / 6}.${bLegal % 6}"
                val bRuns = bBalls.sumOf { it.runsScored + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.extraRuns else 0) }
                val bWkts = bBalls.count { it.isWicket && it.dismissalType != DismissalType.RUN_OUT }
                val bMaidens = bBalls.groupBy { it.overNumber }.count { (_, ob) ->
                    ob.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL } == 6 &&
                        ob.sumOf { it.runsScored + it.extraRuns } == 0
                }

                Text(
                    text = if (bowler != null) "🥎 ${bowler.name} (Tap to change)" else "⚠️ Tap to select Bowler",
                    color = if (bowler != null) Color.White else IplGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$bOvers-$bMaidens-$bRuns-$bWkts",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // This Over Circles Row
            val currentOverIndex = legalBallsTotal / 6
            val currentOverBalls = currentInningsBalls.filter { it.overNumber == currentOverIndex }
            val legalInThisOver = currentOverBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
            val emptyCirclesCount = (6 - legalInThisOver).coerceAtLeast(0)

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
            ) {
                items(currentOverBalls) { ball ->
                    val label = when {
                        ball.isWicket -> "W"
                        ball.extraType != ExtraType.NONE -> "${ball.runsScored}${ball.extraType.name.take(1)}"
                        else -> "${ball.runsScored}"
                    }
                    val circleColor = when {
                        ball.isWicket -> crichuntOutRed
                        ball.runsScored == 6 -> crichuntGreen6
                        ball.runsScored == 4 -> crichuntBlue4
                        else -> Color.White
                    }
                    val labelColor = if (ball.isWicket || ball.runsScored == 4 || ball.runsScored == 6) Color.White else Color.Black

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(circleColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = labelColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
                items(emptyCirclesCount) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }
        }

        // --- Innings Break Banner OR Crichunt Tactile Keypad ---
        if (match.status == MatchStatus.INNINGS_BREAK) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = tileBg),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, IplGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🏁 INNINGS 1 COMPLETE", color = IplGold, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                        Text(
                            text = "${battingTeam?.name ?: "Team A"} scored ${match.teamARuns}/${match.teamAWickets}",
                            color = tileText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${bowlingTeam?.name ?: "Team B"} needs ${match.teamARuns + 1} runs to win",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        if (isEditable) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onStartChase,
                                    colors = ButtonDefaults.buttonColors(containerColor = BidGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🏏 Start Chase", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                }
                                OutlinedButton(
                                    onClick = onUndoLastBall,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("↩ Undo Last Ball", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Crichunt 4-Row Scoring Pad (fixed height so it is NEVER squished on smaller screens)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(255.dp)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top 3 rows: Left 2 cols (0,1 / 2,3 / 4,6) + Right col (WD, NB, BYE, LB)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left 2 columns: Runs 0,1 | 2,3 | 4,6
                    Column(
                        modifier = Modifier
                            .weight(2.2f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CrichuntRunTile("0", tileBg, tileText, isEditable, Modifier.weight(1f).fillMaxHeight()) { onScoreRuns(0) }
                            CrichuntRunTile("1", tileBg, tileText, isEditable, Modifier.weight(1f).fillMaxHeight()) { onScoreRuns(1) }
                        }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CrichuntRunTile("2", tileBg, tileText, isEditable, Modifier.weight(1f).fillMaxHeight()) { onScoreRuns(2) }
                            CrichuntRunTile("3", tileBg, tileText, isEditable, Modifier.weight(1f).fillMaxHeight()) { onScoreRuns(3) }
                        }
                        Row(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CrichuntRunTile("4", crichuntBlue4, Color.White, isEditable, Modifier.weight(1f).fillMaxHeight()) { onScoreRuns(4) }
                            CrichuntRunTile("6", crichuntGreen6, Color.White, isEditable, Modifier.weight(1f).fillMaxHeight()) { onScoreRuns(6) }
                        }
                    }

                    // Right column: WD, NB, BYE, LB
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val extrasList = listOf(
                            Triple(ExtraType.WIDE, "WD", Color(0xFFB59F00)),
                            Triple(ExtraType.NO_BALL, "NB", Color(0xFFD81B60)),
                            Triple(ExtraType.BYE, "BYE", Color(0xFF2E7D32)),
                            Triple(ExtraType.LEG_BYE, "LB", Color(0xFF7B1FA2))
                        )
                        extrasList.forEach { (extra, label, accentColor) ->
                            val isSel = selectedExtraType == extra
                            Surface(
                                onClick = {
                                    if (isEditable) {
                                        activeExtraPickerType = extra
                                    }
                                },
                                color = if (isSel) accentColor else tileBg,
                                shape = RoundedCornerShape(10.dp),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (isSel) Color.White else accentColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Row: OUT | UNDO | 5,7 | ...
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.95f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = { if (isEditable) onOpenWicket() },
                        color = crichuntOutRed,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("OUT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    Surface(
                        onClick = { if (isEditable) onUndoLastBall() },
                        color = tileBg,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("UNDO", color = tileText, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }

                    Surface(
                        onClick = { if (isEditable) showFiveSevenDialog = true },
                        color = tileBg,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("5,7", color = tileText, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }

                    Surface(
                        onClick = { showMoreActionsDialog = true },
                        color = tileBg,
                        shape = RoundedCornerShape(10.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("•••", color = tileText, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }

    // Replace Batter Dialog (Striker or Non-Striker)
    if (showReplaceBatterDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceBatterDialog = false },
            containerColor = tileBg,
            titleContentColor = tileText,
            textContentColor = tileText,
            title = {
                Text(
                    text = if (replaceTargetIsStriker) "Select Striker" else "Select Non-Striker",
                    color = tileText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = replaceTargetIsStriker,
                            onClick = { replaceTargetIsStriker = true },
                            label = { Text("Striker") }
                        )
                        FilterChip(
                            selected = !replaceTargetIsStriker,
                            onClick = { replaceTargetIsStriker = false },
                            label = { Text("Non-Striker") }
                        )
                    }
                    battingPlayers.forEach { p ->
                        val isCurrent = (replaceTargetIsStriker && p.id == striker?.id) ||
                            (!replaceTargetIsStriker && p.id == nonStriker?.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onReplaceBatter(replaceTargetIsStriker, p.id)
                                    showReplaceBatterDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = p.name,
                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isCurrent) BidGreen else Color.Unspecified
                            )
                            Text(p.role.label, fontSize = 11.sp, color = Color.Gray)
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReplaceBatterDialog = false }) { Text("Close") }
            }
        )
    }

    // 1-Tap Extra Runs Picker Dialog (WD, NB, BYE, LB)
    if (activeExtraPickerType != null) {
        val exType = activeExtraPickerType!!
        val extraOptions: List<Pair<String, Int>> = when (exType) {
            ExtraType.WIDE -> listOf("WD (1)" to 0, "WD + 1 (2)" to 1, "WD + 2 (3)" to 2, "WD + 4 (5)" to 4)
            ExtraType.NO_BALL -> listOf("NB (1)" to 0, "NB + 1 (2)" to 1, "NB + 4 (5)" to 4, "NB + 6 (7)" to 6)
            ExtraType.BYE -> listOf("1 Bye" to 1, "2 Byes" to 2, "3 Byes" to 3, "4 Byes" to 4)
            ExtraType.LEG_BYE -> listOf("1 LB" to 1, "2 LB" to 2, "3 LB" to 3, "4 LB" to 4)
            else -> emptyList()
        }
        AlertDialog(
            onDismissRequest = { activeExtraPickerType = null },
            containerColor = tileBg,
            titleContentColor = tileText,
            textContentColor = tileText,
            title = { Text("Record ${exType.name.replace("_", " ")}", color = tileText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    extraOptions.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { (lbl, runsVal) ->
                                Button(
                                    onClick = {
                                        onRecordExtraBall(exType, runsVal)
                                        activeExtraPickerType = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = crichuntBlue4, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(46.dp)
                                ) {
                                    Text(lbl, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { activeExtraPickerType = null }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    // 5 or 7 Runs Picker Dialog
    if (showFiveSevenDialog) {
        AlertDialog(
            onDismissRequest = { showFiveSevenDialog = false },
            containerColor = tileBg,
            titleContentColor = tileText,
            textContentColor = tileText,
            title = { Text("Score 5 or 7 Runs", color = tileText, fontWeight = FontWeight.Bold) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            onScoreRuns(5)
                            showFiveSevenDialog = false
                        },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("5 Runs", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    Button(
                        onClick = {
                            onScoreRuns(7)
                            showFiveSevenDialog = false
                        },
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("7 Runs", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFiveSevenDialog = false }) { Text("Cancel") }
            }
        )
    }

    // More Actions Dialog (•••)
    if (showMoreActionsDialog) {
        AlertDialog(
            onDismissRequest = { showMoreActionsDialog = false },
            containerColor = tileBg,
            titleContentColor = tileText,
            textContentColor = tileText,
            title = { Text("Match Actions", color = tileText, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showMoreActionsDialog = false
                            onOpenYouTubeStream()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (match.liveStreamUrl.isNotBlank()) "📺 Edit / View YouTube Live Stream" else "📺 Host YouTube Live Stream",
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            showMoreActionsDialog = false
                            onEndInnings()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = crichuntOutRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (currentInnings == 1) "End Innings 1" else "End Innings 2", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showMoreActionsDialog = false
                            onDirectEndMatch()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Declare Result / End Match", fontWeight = FontWeight.Bold)
                    }
                    if (currentInnings == 2 || match.status == MatchStatus.COMPLETED) {
                        OutlinedButton(
                            onClick = {
                                showMoreActionsDialog = false
                                onUndoEndInnings()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Reopen / Undo End Innings", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoreActionsDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun CrichuntRunTile(
    label: String,
    bgColor: Color,
    textColor: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = { if (enabled) onClick() },
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun CricbuzzSpectatorView(
    match: Match,
    teams: List<Team>,
    players: List<Player>,
    matchBalls: List<BallRecord>,
    striker: Player?,
    nonStriker: Player?,
    bowler: Player?,
    isDarkTheme: Boolean,
    isPreviewByScorer: Boolean = false,
    onExitPreview: (() -> Unit)? = null
) {
    val bgColor = if (isDarkTheme) StadiumDark else Color(0xFFF1F5F9)
    val cardBg = if (isDarkTheme) StadiumCardDark else Color.White
    val surfaceBg = if (isDarkTheme) StadiumSurface else Color(0xFFF8FAFC)
    val textMain = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val textSub = if (isDarkTheme) Color.LightGray else Color(0xFF64748B)
    val cricbuzzGreen = Color(0xFF009270)

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Live", "Scorecard", "Overs")
    val context = LocalContext.current

    val streamVideoId = remember(match.liveStreamUrl) {
        val trimmed = match.liveStreamUrl.trim()
        when {
            trimmed.isBlank() -> null
            Regex("""^[a-zA-Z0-9_-]{11}$""").matches(trimmed) -> trimmed
            else -> Regex("""(?:youtu\.be/|youtube\.com/(?:watch\?v=|live/|embed/|shorts/))([a-zA-Z0-9_-]{11})""")
                .find(trimmed)?.groupValues?.getOrNull(1)
        }
    }
    var streamOverlayOnly by remember(match.id, match.liveStreamUrl) {
        mutableStateOf(match.liveStreamUrl.isNotBlank())
    }

    val teamA = teams.find { it.id == match.teamAId }
    val teamB = teams.find { it.id == match.teamBId }
    val currentInnings = match.currentInnings
    val currentInningsBalls = matchBalls.filter { it.innings == currentInnings }

    val inn1Legal = matchBalls.count { it.innings == 1 && it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
    val inn2Legal = matchBalls.count { it.innings == 2 && it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
    val inn1OversStr = "${inn1Legal / 6}.${inn1Legal % 6}"
    val inn2OversStr = "${inn2Legal / 6}.${inn2Legal % 6}"

    val activeLegal = if (currentInnings == 1) inn1Legal else inn2Legal
    val activeRuns = if (currentInnings == 1) match.teamARuns else match.teamBRuns
    val crr = if (activeLegal > 0) (activeRuns * 6.0) / activeLegal else 0.0
    val target = match.teamARuns + 1
    val need = (target - match.teamBRuns).coerceAtLeast(0)
    val ballsRem = (match.totalOvers * 6 - inn2Legal).coerceAtLeast(0)
    val rrr = if (ballsRem > 0) (need * 6.0) / ballsRem else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        if (isPreviewByScorer && onExitPreview != null) {
            Surface(
                color = NeonBlue.copy(alpha = 0.16f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SPECTATOR VIEW PREVIEW", color = textMain, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    TextButton(onClick = onExitPreview, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Back to Scorer", color = NeonBlue, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
            }
        }

        // Cricbuzz Green Header Strip + Match Scores Card
        Surface(
            color = cricbuzzGreen,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${teamA?.shortCode ?: "T1"} vs ${teamB?.shortCode ?: "T2"} • ${match.totalOvers} Ov",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Surface(
                        color = when (match.status) {
                            MatchStatus.COMPLETED -> Color.White.copy(alpha = 0.22f)
                            MatchStatus.INNINGS_BREAK -> IplGold
                            else -> Color(0xFFE53935)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (match.status) {
                                MatchStatus.COMPLETED -> "RESULT"
                                MatchStatus.INNINGS_BREAK -> "INNINGS BREAK"
                                else -> "● LIVE"
                            },
                            color = if (match.status == MatchStatus.INNINGS_BREAK) Color.Black else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // YouTube Live Stream + Score Overlay Only Mode (for Spectators when live stream is active)
        if (match.liveStreamUrl.isNotBlank()) {
            val activeTeam = if (currentInnings == 1) teamA else teamB
            val activeWkts = if (currentInnings == 1) match.teamAWickets else match.teamBWickets
            val activeOvStr = if (currentInnings == 1) inn1OversStr else inn2OversStr
            val overIdx = activeLegal / 6
            val thisOverBalls = currentInningsBalls.filter { it.overNumber == overIdx }

            Column(modifier = Modifier.fillMaxWidth().background(Color.Black)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            onClick = { streamOverlayOnly = true },
                            color = if (streamOverlayOnly) Color(0xFFFF0000) else Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📺 Stream + Score Overlay",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            onClick = { streamOverlayOnly = false },
                            color = if (!streamOverlayOnly) cricbuzzGreen else Color.White.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📊 Full Scorecard",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val ytUrl = if (streamVideoId != null) "https://www.youtube.com/watch?v=$streamVideoId" else match.liveStreamUrl
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)).apply { setPackage("com.google.android.youtube") })
                            } catch (_: Exception) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)))
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open in YouTube", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                // Embedded YouTube Player + Broadcast Score Overlay Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (streamOverlayOnly) 260.dp else 185.dp)
                        .background(Color.Black)
                ) {
                    if (streamVideoId != null) {
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
                                if (webView.url != embedUrl) webView.loadUrl(embedUrl)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("YouTube Live: ${match.liveStreamUrl}", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    // Compact Broadcast Score Overlay pinned at bottom of the video
                    BroadcastScoreOverlayBar(
                        battingTeamShort = activeTeam?.shortCode ?: "TEAM",
                        runs = activeRuns,
                        wickets = activeWkts,
                        oversStr = activeOvStr,
                        totalOvers = match.totalOvers,
                        currentInnings = currentInnings,
                        targetRuns = target,
                        needRuns = need,
                        ballsRemaining = ballsRem,
                        striker = striker,
                        nonStriker = nonStriker,
                        bowler = bowler,
                        currentInningsBalls = currentInningsBalls,
                        thisOverBalls = thisOverBalls,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(6.dp)
                    )
                }
            }
        }

        if (match.liveStreamUrl.isBlank() || !streamOverlayOnly) {
            // Cricbuzz Sub-Tabs
            TabRow(
            selectedTabIndex = selectedTab,
            containerColor = cardBg,
            contentColor = cricbuzzGreen
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == idx) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (selectedTab == idx) cricbuzzGreen else textSub,
                            fontSize = 13.sp
                        )
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Cricbuzz Summary Score Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(10.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Team A Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(26.dp).clip(CircleShape).background(cricbuzzGreen.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(teamA?.shortCode?.take(2) ?: "A", color = cricbuzzGreen, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(teamA?.name ?: "Team A", color = textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(
                                text = "${match.teamARuns}-${match.teamAWickets} ($inn1OversStr)",
                                color = textMain,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }

                        // Team B Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(26.dp).clip(CircleShape).background(NeonBlue.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(teamB?.shortCode?.take(2) ?: "B", color = NeonBlue, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(teamB?.name ?: "Team B", color = textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text(
                                text = if (currentInnings == 2 || match.status == MatchStatus.COMPLETED)
                                    "${match.teamBRuns}-${match.teamBWickets} ($inn2OversStr)"
                                else "Yet to bat",
                                color = if (currentInnings == 2) textMain else textSub,
                                fontWeight = if (currentInnings == 2) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = if (currentInnings == 2) 17.sp else 13.sp
                            )
                        }

                        HorizontalDivider(color = textSub.copy(alpha = 0.15f))

                        val equationText = when {
                            match.status == MatchStatus.COMPLETED && match.resultSummary.isNotBlank() -> match.resultSummary
                            match.status == MatchStatus.INNINGS_BREAK -> "${teamB?.shortCode ?: "Team B"} need $target runs to win"
                            currentInnings == 2 -> "${teamB?.shortCode ?: "Team B"} need $need runs in $ballsRem balls"
                            else -> {
                                val tossW = teams.find { it.id == match.tossWinnerTeamId }
                                if (tossW != null && !match.tossDecision.isNullOrBlank())
                                    "${tossW.shortCode} won the toss and opted to ${match.tossDecision.lowercase()}"
                                else "${teamA?.shortCode ?: "Team A"} batting"
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = equationText,
                                color = if (match.status == MatchStatus.COMPLETED) cricbuzzGreen else Color(0xFFD97706),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (currentInnings == 2 && match.status != MatchStatus.COMPLETED)
                                    "CRR: ${String.format("%.2f", crr)}  RRR: ${String.format("%.2f", rrr)}"
                                else "CRR: ${String.format("%.2f", crr)}",
                                color = textSub,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // LIVE TAB: Batters + Bowler + This Over
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth().background(surfaceBg).padding(vertical = 4.dp, horizontal = 6.dp)) {
                                Text("Batter", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.4f))
                                Text("R", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("B", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("4s", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("6s", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("SR", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                            listOfNotNull(striker?.let { it to true }, nonStriker?.let { it to false }).forEach { (b, isStr) ->
                                val bBalls = currentInningsBalls.filter { it.strikerId == b.id }
                                val r = bBalls.sumOf { it.runsScored }
                                val f = bBalls.count { it.extraType != ExtraType.WIDE }
                                val f4 = bBalls.count { it.runsScored == 4 }
                                val f6 = bBalls.count { it.runsScored == 6 }
                                val sr = if (f > 0) String.format("%.1f", r * 100.0 / f) else "0.0"
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Text(
                                        text = "${b.name}${if (isStr) " *" else ""}",
                                        color = if (isStr) cricbuzzGreen else textMain,
                                        fontWeight = if (isStr) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(2.4f)
                                    )
                                    Text("$r", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$f", color = textSub, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$f4", color = textSub, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("$f6", color = textSub, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text(sr, color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth().background(surfaceBg).padding(vertical = 4.dp, horizontal = 6.dp)) {
                                Text("Bowler", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.4f))
                                Text("O", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("M", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("R", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                Text("W", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                Text("ECO", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            }
                            if (bowler != null) {
                                val bwBalls = currentInningsBalls.filter { it.bowlerId == bowler.id }
                                val bwLegal = bwBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                val bwOv = "${bwLegal / 6}.${bwLegal % 6}"
                                val bwRuns = bwBalls.sumOf { it.runsScored + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.extraRuns else 0) }
                                val bwWkts = bwBalls.count { it.isWicket && it.dismissalType != DismissalType.RUN_OUT }
                                val bwEcon = if (bwLegal > 0) String.format("%.2f", bwRuns * 6.0 / bwLegal) else "0.00"
                                val bwMaidens = bwBalls.groupBy { it.overNumber }.count { (_, ob) ->
                                    ob.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL } == 6 &&
                                        ob.sumOf { it.runsScored + it.extraRuns } == 0
                                }
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 3.dp)) {
                                    Text(bowler.name, color = textMain, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.weight(2.4f))
                                    Text(bwOv, color = textMain, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$bwMaidens", color = textSub, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text("$bwRuns", color = textMain, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.7f))
                                    Text("$bwWkts", color = UnsoldRed, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text(bwEcon, color = textSub, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Recent Balls / This Over
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("THIS OVER", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            val overIdx = activeLegal / 6
                            val thisOverBalls = currentInningsBalls.filter { it.overNumber == overIdx }
                            if (thisOverBalls.isEmpty()) {
                                Text("Waiting for next ball...", color = textSub, fontSize = 12.sp)
                            } else {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(thisOverBalls) { ball ->
                                        val txt = if (ball.isWicket) "W" else if (ball.extraType != ExtraType.NONE) "${ball.runsScored}${ball.extraType.name.take(1)}" else "${ball.runsScored}"
                                        val col = when {
                                            ball.isWicket -> UnsoldRed
                                            ball.runsScored == 6 -> cricbuzzGreen
                                            ball.runsScored == 4 -> Color(0xFF1976D2)
                                            else -> surfaceBg
                                        }
                                        val txtCol = if (ball.isWicket || ball.runsScored == 4 || ball.runsScored == 6) Color.White else textMain
                                        Surface(color = col, shape = CircleShape, modifier = Modifier.size(30.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(txt, color = txtCol, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // SCORECARD TAB: Full Innings 1 & 2 Batting + Bowling
                listOf(1 to (teamA?.name ?: "Innings 1"), 2 to (teamB?.name ?: "Innings 2")).forEach { (innNum, innTitle) ->
                    val innBalls = matchBalls.filter { it.innings == innNum }
                    if (innBalls.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val totR = innBalls.sumOf { it.runsScored + it.extraRuns }
                                    val totW = innBalls.count { it.isWicket }
                                    val totL = innBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(innTitle, color = cricbuzzGreen, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                        Text("$totR-$totW (${totL / 6}.${totL % 6} Ov)", color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                    }
                                    HorizontalDivider(color = textSub.copy(alpha = 0.15f))
                                    val batterIds = innBalls.map { it.strikerId }.distinct()
                                    batterIds.forEach { bId ->
                                        val bPlayer = players.find { it.id == bId }
                                        val bBalls = innBalls.filter { it.strikerId == bId }
                                        val r = bBalls.sumOf { it.runsScored }
                                        val f = bBalls.count { it.extraType != ExtraType.WIDE }
                                        val outBall = innBalls.find { it.isWicket && (it.dismissedPlayerId == bId || (it.dismissedPlayerId == null && it.strikerId == bId)) }
                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(bPlayer?.name ?: "Batter", color = textMain, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text(
                                                    text = if (outBall != null) outBall.dismissalType?.label ?: "Out" else "not out",
                                                    color = if (outBall != null) textSub else cricbuzzGreen,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Text("$r ($f)", color = textMain, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // OVERS TAB: Over-by-over summary
                val groupedOvers = currentInningsBalls.groupBy { it.overNumber }.toSortedMap(compareByDescending { it })
                if (groupedOvers.isEmpty()) {
                    item {
                        Text("No overs bowled yet in Innings $currentInnings.", color = textSub, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    groupedOvers.forEach { (overIdx, oBalls) ->
                        item {
                            val overRuns = oBalls.sumOf { it.runsScored + it.extraRuns }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Over ${overIdx + 1}", color = textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("$overRuns runs", color = cricbuzzGreen, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(oBalls) { b ->
                                            val txt = if (b.isWicket) "W" else if (b.extraType != ExtraType.NONE) "${b.runsScored}${b.extraType.name.take(1)}" else "${b.runsScored}"
                                            Surface(color = surfaceBg, shape = CircleShape, modifier = Modifier.size(26.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(txt, color = textMain, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
        }
    }
}

@Composable
fun BroadcastScoreOverlayBar(
    battingTeamShort: String,
    runs: Int,
    wickets: Int,
    oversStr: String,
    totalOvers: Int,
    currentInnings: Int,
    targetRuns: Int,
    needRuns: Int,
    ballsRemaining: Int,
    striker: Player?,
    nonStriker: Player?,
    bowler: Player?,
    currentInningsBalls: List<BallRecord>,
    thisOverBalls: List<BallRecord>,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xE60A192F),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = Color(0xFFE53935), shape = RoundedCornerShape(4.dp)) {
                        Text("LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                    Text(
                        text = "$battingTeamShort $runs/$wickets",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "($oversStr/$totalOvers)",
                        color = IplGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = if (currentInnings == 2) "T:$targetRuns • Need $needRuns in $ballsRemaining b" else "1st Innings",
                    color = NeonBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sRuns = striker?.let { s -> currentInningsBalls.filter { it.strikerId == s.id }.sumOf { it.runsScored } } ?: 0
                val sBalls = striker?.let { s -> currentInningsBalls.filter { it.strikerId == s.id && it.extraType != ExtraType.WIDE }.size } ?: 0
                val nsRuns = nonStriker?.let { ns -> currentInningsBalls.filter { it.strikerId == ns.id }.sumOf { it.runsScored } } ?: 0
                val nsBalls = nonStriker?.let { ns -> currentInningsBalls.filter { it.strikerId == ns.id && it.extraType != ExtraType.WIDE }.size } ?: 0

                Text(
                    text = "${striker?.name?.take(10) ?: "-"}* $sRuns($sBalls)  |  ${nonStriker?.name?.take(10) ?: "-"} $nsRuns($nsBalls)",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    items(thisOverBalls.takeLast(6)) { b ->
                        val lbl = if (b.isWicket) "W" else if (b.extraType != ExtraType.NONE) "${b.runsScored}${b.extraType.name.take(1)}" else "${b.runsScored}"
                        val bg = when {
                            b.isWicket -> UnsoldRed
                            b.runsScored == 6 -> BidGreen
                            b.runsScored == 4 -> Color(0xFF1976D2)
                            else -> Color.White.copy(alpha = 0.22f)
                        }
                        Box(
                            modifier = Modifier.size(18.dp).clip(CircleShape).background(bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(lbl, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuctionBroadcastOverlayBar(
    tournamentName: String,
    activePlayer: Player?,
    highestBidderTeam: Team?,
    timerSeconds: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xE60A192F),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.8f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(color = Color(0xFFE53935), shape = RoundedCornerShape(4.dp)) {
                        Text("AUCTION LIVE", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                    Text(
                        text = tournamentName,
                        color = IplGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "⏱ ${timerSeconds}s",
                    color = if (timerSeconds <= 5) UnsoldRed else NeonBlue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
            if (activePlayer != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activePlayer.name} (${activePlayer.role.label})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${activePlayer.currentBid.formatIplCurrency()} • ${highestBidderTeam?.shortCode ?: "Base"}",
                        color = BidGreen,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
fun BackgroundCameraPreviewLayer(
    useFrontCamera: Boolean = false,
    modifier: Modifier = Modifier
) {
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraRef?.stopPreview()
                cameraRef?.release()
                cameraRef = null
            } catch (_: Exception) { }
        }
    }

    key(useFrontCamera) {
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            try {
                                cameraRef?.stopPreview()
                                cameraRef?.release()
                                val camCount = Camera.getNumberOfCameras()
                                var targetId = 0
                                val info = Camera.CameraInfo()
                                for (i in 0 until camCount) {
                                    Camera.getCameraInfo(i, info)
                                    if (useFrontCamera && info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                                        targetId = i
                                        break
                                    } else if (!useFrontCamera && info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                                        targetId = i
                                        break
                                    }
                                }
                                val cam = Camera.open(targetId)
                                cam.setDisplayOrientation(90)
                                cam.setPreviewTexture(surface)
                                cam.startPreview()
                                cameraRef = cam
                            } catch (_: Exception) { }
                        }
                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            try {
                                cameraRef?.stopPreview()
                                cameraRef?.release()
                                cameraRef = null
                            } catch (_: Exception) { }
                            return true
                        }
                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            modifier = modifier
        )
    }
}

@Suppress("DEPRECATION")
@Composable
fun PrismYouTubeLiveStreamerDialog(
    viewModel: AuctionViewModel,
    streamTitle: String,
    streamDescription: String,
    initialStreamUrl: String,
    overlayContent: @Composable () -> Unit,
    onSaveStreamUrl: (String) -> Unit,
    onEnableBackgroundCamera: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isYouTubeConnected by viewModel.isYouTubeConnected.collectAsState()
    val connectedEmail by viewModel.connectedGoogleEmail.collectAsState()

    var streamUrlInput by remember(initialStreamUrl) { mutableStateOf(initialStreamUrl) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var isCreatingBroadcast by remember { mutableStateOf(false) }
    var activeBroadcast by remember { mutableStateOf<YouTubeBroadcastResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showOverlayOnCamera by remember { mutableStateOf(true) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleYouTubeSignInResult(result.data) { ok, msg ->
            if (!ok) {
                errorMessage = msg
            } else {
                errorMessage = null
                Toast.makeText(context, "✅ $msg", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = Color.Black,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Live Camera Preview via TextureView (never occluded by Compose Dialog surface)
                BackgroundCameraPreviewLayer(
                    useFrontCamera = useFrontCamera,
                    modifier = Modifier.fillMaxSize()
                )

                // Top PRISM-Style Broadcast Bar
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (activeBroadcast?.success == true) Color(0xFFFF0000) else IplGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (activeBroadcast?.success == true) "ON AIR • YOUTUBE LIVE" else "DIRECT STREAM STUDIO",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { showOverlayOnCamera = !showOverlayOnCamera },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = if (showOverlayOnCamera) "Overlay ON" else "Overlay OFF",
                                color = IplGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { useFrontCamera = !useFrontCamera }) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close Streamer", tint = Color.White)
                        }
                    }
                }

                // Bottom Controls + Live Overlay + Direct Google Sign-In & YouTube Broadcast
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.72f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showOverlayOnCamera) {
                        overlayContent()
                    }

                    // Google Sign-In Status & Direct YouTube Live Controls (No Web Page Sign-In)
                    if (!isYouTubeConnected) {
                        Button(
                            onClick = {
                                googleSignInLauncher.launch(viewModel.getYouTubeSignInIntent(context))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1F2937)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text(
                                text = "G  Sign in with Google (Direct YouTube Live)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ YouTube: ${connectedEmail ?: "Connected"}",
                                color = BidGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = {
                                    viewModel.signOutGoogleAccount {
                                        googleSignInLauncher.launch(viewModel.getYouTubeSignInIntent(context))
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Switch", color = NeonBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            TextButton(
                                onClick = {
                                    viewModel.signOutGoogleAccount {
                                        Toast.makeText(context, "👋 Signed out of Google", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Logout", color = UnsoldRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!errorMessage.isNullOrBlank()) {
                        Surface(
                            color = UnsoldRed.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, UnsoldRed)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (activeBroadcast?.success == true) {
                        val b = activeBroadcast!!
                        Surface(
                            color = Color(0xFF142238),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BidGreen)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🔴 YouTube Broadcast Created & Linked!", color = BidGreen, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                Text("Watch URL: ${b.watchUrl}", color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (b.streamKey.isNotBlank()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "RTMP Key: ${b.streamKey.take(14)}...",
                                            color = IplGold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                cm.setPrimaryClip(ClipData.newPlainText("RTMP", "${b.ingestionAddress}/${b.streamKey}"))
                                                Toast.makeText(context, "📋 RTMP URL & Key copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy RTMP", tint = IplGold, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeBroadcast?.success == true || streamUrlInput.isNotBlank()) {
                            Button(
                                onClick = {
                                    if (activeBroadcast?.success == true) {
                                        viewModel.endDirectYouTubeBroadcast(activeBroadcast!!.broadcastId)
                                        activeBroadcast = null
                                    }
                                    streamUrlInput = ""
                                    onSaveStreamUrl("")
                                    Toast.makeText(context, "⏹ YouTube Broadcast ended", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("⏹ End YouTube Stream", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!isYouTubeConnected) {
                                        googleSignInLauncher.launch(viewModel.getYouTubeSignInIntent(context))
                                        return@Button
                                    }
                                    isCreatingBroadcast = true
                                    errorMessage = null
                                    viewModel.startDirectYouTubeBroadcast(streamTitle, streamDescription) { res ->
                                        isCreatingBroadcast = false
                                        if (res.success) {
                                            activeBroadcast = res
                                            streamUrlInput = res.watchUrl
                                            onSaveStreamUrl(res.watchUrl)
                                            Toast.makeText(context, "🔴 YouTube Live Broadcast Started!", Toast.LENGTH_LONG).show()
                                        } else {
                                            errorMessage = res.errorMessage
                                        }
                                    }
                                },
                                enabled = !isCreatingBroadcast,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCreatingBroadcast) "Starting Stream..." else "🔴 Start Direct YouTube Live",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (onEnableBackgroundCamera != null) {
                            Button(
                                onClick = {
                                    onSaveStreamUrl(streamUrlInput.trim())
                                    onEnableBackgroundCamera()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("📹 BG Score Mode", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = {
                                onSaveStreamUrl(streamUrlInput.trim())
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BidGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save & Close", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = streamUrlInput,
                        onValueChange = { streamUrlInput = it },
                        placeholder = { Text("YouTube Live URL (auto-filled or paste manual)...", color = Color.LightGray, fontSize = 11.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = IplGold,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun CameraLiveBroadcastOverlayDialog(
    viewModel: AuctionViewModel,
    match: Match,
    teams: List<Team>,
    currentInningsBalls: List<BallRecord>,
    striker: Player?,
    nonStriker: Player?,
    bowler: Player?,
    initialStreamUrl: String,
    onSaveStreamUrl: (String) -> Unit,
    onEnableBackgroundCamera: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val teamA = teams.find { it.id == match.teamAId }
    val teamB = teams.find { it.id == match.teamBId }
    val currentInnings = match.currentInnings
    val activeTeam = if (currentInnings == 1) teamA else teamB
    val activeRuns = if (currentInnings == 1) match.teamARuns else match.teamBRuns
    val activeWkts = if (currentInnings == 1) match.teamAWickets else match.teamBWickets
    val activeLegal = currentInningsBalls.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
    val activeOvStr = "${activeLegal / 6}.${activeLegal % 6}"
    val overIdx = activeLegal / 6
    val thisOverBalls = currentInningsBalls.filter { it.overNumber == overIdx }
    val target = match.teamARuns + 1
    val need = (target - match.teamBRuns).coerceAtLeast(0)
    val ballsRem = (match.totalOvers * 6 - activeLegal).coerceAtLeast(0)

    PrismYouTubeLiveStreamerDialog(
        viewModel = viewModel,
        streamTitle = "Live Cricket: ${teamA?.shortCode ?: "Team A"} vs ${teamB?.shortCode ?: "Team B"}",
        streamDescription = "Live cricket match broadcast with real-time score overlay from Cricket Hub.",
        initialStreamUrl = initialStreamUrl,
        overlayContent = {
            BroadcastScoreOverlayBar(
                battingTeamShort = activeTeam?.shortCode ?: "TEAM",
                runs = activeRuns,
                wickets = activeWkts,
                oversStr = activeOvStr,
                totalOvers = match.totalOvers,
                currentInnings = currentInnings,
                targetRuns = target,
                needRuns = need,
                ballsRemaining = ballsRem,
                striker = striker,
                nonStriker = nonStriker,
                bowler = bowler,
                currentInningsBalls = currentInningsBalls,
                thisOverBalls = thisOverBalls,
                modifier = Modifier.fillMaxWidth()
            )
        },
        onSaveStreamUrl = onSaveStreamUrl,
        onEnableBackgroundCamera = onEnableBackgroundCamera,
        onDismiss = onDismiss
    )
}
