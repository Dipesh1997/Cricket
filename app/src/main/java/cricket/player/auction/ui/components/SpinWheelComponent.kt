package cricket.player.auction.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.CaptainQuizSession
import cricket.player.auction.model.MathQuizQuestion
import cricket.player.auction.model.Player
import cricket.player.auction.model.PlayerRole
import cricket.player.auction.model.PlayerStatus
import cricket.player.auction.model.Team
import cricket.player.auction.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val WheelColors = listOf(
    Color(0xFFFFD700), // IPL Gold
    Color(0xFF00D2FF), // Neon Blue
    Color(0xFF9C27B0), // Purple
    Color(0xFFFF5722), // Orange
    Color(0xFF4CAF50), // Green
    Color(0xFFE91E63), // Pink
    Color(0xFF3F51B5), // Indigo
    Color(0xFFFFC107)  // Amber
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinWheelModal(
    players: List<Player>,
    teams: List<Team> = emptyList(),
    isDarkTheme: Boolean = true,
    autoAssignLastPlayerAtAvg: Boolean = false,
    onToggleAutoAssignRule: ((Boolean) -> Unit)? = null,
    currentSpinTeam: Team? = null,
    teamPickedCounts: Map<String, Int> = emptyMap(),
    quotaPerTeam: Int = 0,
    captainQuizSession: CaptainQuizSession? = null,
    onOpenQuizForTeam: ((teamId: String) -> Unit)? = null,
    onTogglePlayerFixedSet: ((playerId: String, isFixed: Boolean) -> Unit)? = null,
    onSelectNextBatch: ((count: Int) -> Unit)? = null,
    onClearAllFixed: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onPlayerSelected: (Player) -> Unit
) {
    var isSpinning by remember { mutableStateOf(false) }
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    var showFixedSetSelectorDialog by remember { mutableStateOf(false) }
    val rotationAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    // Determine Fixed Category vs Open Category Priority
    val upNextPlayers = remember(players) {
        players.filter { it.status == PlayerStatus.UP_NEXT }
    }

    val fixedSetPlayers = remember(upNextPlayers) {
        upNextPlayers.filter { it.isFixedSet }
    }

    val openCategoryPlayers = remember(upNextPlayers) {
        upNextPlayers.filter { !it.isFixedSet }
    }

    val isFixedSetStage = fixedSetPlayers.isNotEmpty()
    val totalTeams = teams.size
    val fixedCount = fixedSetPlayers.size

    // Multiple of Teams Rule for Fixed Set:
    // If there are teams, fixedSetPlayers must be >= totalTeams and fixedSetPlayers % totalTeams == 0
    val isFixedSetQuotaMet = remember(isFixedSetStage, totalTeams, fixedCount) {
        if (!isFixedSetStage) true
        else if (totalTeams <= 0) true
        else (fixedCount >= totalTeams && fixedCount % totalTeams == 0)
    }

    val nextMultipleTarget = remember(totalTeams, fixedCount) {
        if (totalTeams <= 0) fixedCount
        else if (fixedCount < totalTeams) totalTeams
        else ((fixedCount / totalTeams) + 1) * totalTeams
    }

    val playersDeficit = remember(isFixedSetStage, totalTeams, fixedCount, nextMultipleTarget) {
        if (!isFixedSetStage || totalTeams <= 0 || isFixedSetQuotaMet) 0
        else nextMultipleTarget - fixedCount
    }

    val availablePlayers = if (isFixedSetStage) fixedSetPlayers else openCategoryPlayers

    val isLight = !isDarkTheme
    val cardBg = if (isLight) LightPitchCard else StadiumCardDark
    val surfaceBg = if (isLight) LightPitchSurface else StadiumSurface
    val textMain = if (isLight) LightPitchText else Color.White
    val textSub = if (isLight) LightPitchSecondary else Color.LightGray
    val themeAccent = if (isLight) LightPitchPrimary else IplGold

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Casino, contentDescription = null, tint = themeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LUCKY PLAYER SPIN WHEEL",
                        color = textMain,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                StatChip(
                    text = if (isFixedSetStage) "FIXED CATEGORY SET (${fixedSetPlayers.size} Left)" else "OPEN CATEGORY (${openCategoryPlayers.size} Left)",
                    backgroundColor = surfaceBg,
                    textColor = textMain,
                    icon = if (isFixedSetStage) {
                        { Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = themeAccent, modifier = Modifier.size(12.dp)) }
                    } else null
                )
            }
        },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rule Checkmark Banner: Last Player Auto-Assign at Average Price
                Surface(
                    color = if (autoAssignLastPlayerAtAvg) BidGreen.copy(alpha = 0.12f) else surfaceBg,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (autoAssignLastPlayerAtAvg) BidGreen.copy(alpha = 0.6f) else textSub.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onToggleAutoAssignRule != null) {
                            onToggleAutoAssignRule?.invoke(!autoAssignLastPlayerAtAvg)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoAssignLastPlayerAtAvg,
                            onCheckedChange = { onToggleAutoAssignRule?.invoke(it) },
                            enabled = onToggleAutoAssignRule != null,
                            colors = CheckboxDefaults.colors(checkedColor = BidGreen, checkmarkColor = Color.White),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Rule: Last Player Auto-Assign at Avg Price",
                                color = textMain,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = if (autoAssignLastPlayerAtAvg)
                                    "ON • Equal set quota; last 2 captains play 5-Q math quiz for 2nd-last spin."
                                else
                                    "OFF • Tap to enable auto-assign of last player at set average price.",
                                color = textSub,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }

                // Current Spin Turn Banner
                if (currentSpinTeam != null && availablePlayers.isNotEmpty()) {
                    Surface(
                        color = themeAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🎡 Spin Turn: ${currentSpinTeam.name} (${currentSpinTeam.shortCode})",
                                color = themeAccent,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Equal Distribution Per-Team Quota Progress
                if (isFixedSetStage && quotaPerTeam > 0 && teams.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(teams) { team ->
                            val picked = teamPickedCounts[team.id] ?: 0
                            val done = picked >= quotaPerTeam
                            Surface(
                                color = if (done) BidGreen.copy(alpha = 0.18f) else surfaceBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (done) BidGreen else textSub.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "${team.shortCode}: $picked/$quotaPerTeam ${if (done) "✅" else ""}",
                                    color = if (done) BidGreen else textMain,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 2-Left Captain Math Quiz Gate
                if (isFixedSetStage && autoAssignLastPlayerAtAvg && fixedCount == 2 && captainQuizSession != null) {
                    val t1 = teams.find { it.id == captainQuizSession.team1Id }
                    val t2 = teams.find { it.id == captainQuizSession.team2Id }
                    val s1 = captainQuizSession.team1Submission
                    val s2 = captainQuizSession.team2Submission
                    val quizDone = captainQuizSession.winnerTeamId != null
                    val winnerTeam = teams.find { it.id == captainQuizSession.winnerTeamId }

                    Surface(
                        color = if (quizDone) BidGreen.copy(alpha = 0.14f) else NeonBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (quizDone) BidGreen else NeonBlue),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (quizDone) "🏆 Quiz Won by ${winnerTeam?.name ?: "Winner"}!" else "🧠 CAPTAINS 5-Q MATH QUIZ (+, -, ×, ÷)",
                                color = if (quizDone) BidGreen else NeonBlue,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (quizDone)
                                    "${winnerTeam?.shortCode ?: ""} gets to spin for the 2nd-last player! Last player will auto-assign at avg price."
                                else
                                    "2 players left! Both captains solve the same 5 questions (3 options). Higher score or faster time wins the 2nd-last spin.",
                                color = textMain,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 13.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (t1 != null) {
                                    val done1 = s1?.isCompleted == true
                                    OutlinedButton(
                                        onClick = { onOpenQuizForTeam?.invoke(t1.id) },
                                        enabled = !done1 && onOpenQuizForTeam != null,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (done1) "${t1.shortCode}: ${s1!!.correctCount}/5 (${String.format("%.1f", s1.timeTakenMs / 1000.0)}s)" else "Play ${t1.shortCode}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (t2 != null) {
                                    val done2 = s2?.isCompleted == true
                                    OutlinedButton(
                                        onClick = { onOpenQuizForTeam?.invoke(t2.id) },
                                        enabled = !done2 && onOpenQuizForTeam != null,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = if (done2) "${t2.shortCode}: ${s2!!.correctCount}/5 (${String.format("%.1f", s2.timeTakenMs / 1000.0)}s)" else "Play ${t2.shortCode}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                // Fixed Set Team Multiples Notice / Warning
                if (isFixedSetStage && !isFixedSetQuotaMet && totalTeams > 0) {
                    Surface(
                        color = Color(0xFFD32F2F).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FIXED SET INCOMPLETE",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Total Teams: $totalTeams. Fixed sets require players in exact multiples of $totalTeams (e.g. $totalTeams, ${totalTeams * 2}, ${totalTeams * 3}...). You currently have $fixedCount player${if (fixedCount == 1) "" else "s"}.\n\nPlease add $playersDeficit more player${if (playersDeficit == 1) "" else "s"} to complete the set of $nextMultipleTarget before spinning!",
                                color = textMain,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 15.sp
                            )
                            if (onTogglePlayerFixedSet != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showFixedSetSelectorDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252), contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("SELECT PLAYERS (CHECKBOXES) - NEED $playersDeficit MORE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (isFixedSetStage && isFixedSetQuotaMet && totalTeams > 0) {
                    Surface(
                        color = BidGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BidGreen.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✅ Fixed Set Quota Met: $fixedCount players (${fixedCount / totalTeams} round${if (fixedCount / totalTeams > 1) "s" else ""} for $totalTeams teams)",
                                color = BidGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            if (onTogglePlayerFixedSet != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap to edit list 📝",
                                    color = BidGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { showFixedSetSelectorDialog = true }
                                )
                            }
                        }
                    }
                }

                // Direct Manage Fixed Set Button (for open or fixed stage)
                if (onTogglePlayerFixedSet != null && (!isFixedSetStage || isFixedSetQuotaMet)) {
                    Surface(
                        onClick = { showFixedSetSelectorDialog = true },
                        color = surfaceBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null, tint = themeAccent, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isFixedSetStage) "Fixed Set: $fixedCount players selected" else "Setup Fixed Set (Checkboxes)",
                                    color = textMain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Manage List 📝",
                                color = themeAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (availablePlayers.isEmpty()) {
                    Text(
                        text = "No 'UP_NEXT' players available in roster to spin!",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // Circular Avatars Bar of Wheel Players
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availablePlayers) { player ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = if (player.isFixedSet) IplGold else NeonBlue,
                                            shape = CircleShape
                                        )
                                        .background(StadiumDark)
                                ) {
                                    DriveImage(
                                        url = player.imageUrl,
                                        contentDescription = player.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Text(
                                    text = player.name.take(6) + "…",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Main Spin Canvas
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasSize = size.minDimension
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = canvasSize / 2f
                            val sliceAngle = 360f / availablePlayers.size

                            rotate(rotationAnim.value, pivot = center) {
                                availablePlayers.forEachIndexed { index, player ->
                                    val startAngle = index * sliceAngle
                                    val color = WheelColors[index % WheelColors.size]

                                    // Draw Slice
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sliceAngle,
                                        useCenter = true,
                                        size = Size(canvasSize, canvasSize),
                                        topLeft = Offset((size.width - canvasSize) / 2f, (size.height - canvasSize) / 2f)
                                    )

                                    // Draw Slice Border
                                    drawArc(
                                        color = StadiumDark,
                                        startAngle = startAngle,
                                        sweepAngle = sliceAngle,
                                        useCenter = true,
                                        style = Stroke(width = 3f),
                                        size = Size(canvasSize, canvasSize),
                                        topLeft = Offset((size.width - canvasSize) / 2f, (size.height - canvasSize) / 2f)
                                    )

                                    // Draw Text on Slice
                                    val midAngleRad = (startAngle + sliceAngle / 2f) * (PI / 180f)
                                    val textDistance = radius * 0.6f
                                    val textX = center.x + textDistance * cos(midAngleRad).toFloat()
                                    val textY = center.y + textDistance * sin(midAngleRad).toFloat()

                                    val displayName = if (player.name.length > 9) player.name.take(8) + "…" else player.name
                                    val textLayoutResult = textMeasurer.measure(
                                        text = displayName,
                                        style = TextStyle(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )

                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = Offset(
                                            textX - textLayoutResult.size.width / 2f,
                                            textY - textLayoutResult.size.height / 2f
                                        )
                                    )
                                }
                            }

                            // Center Circle Badge
                            drawCircle(color = StadiumDark, radius = radius * 0.25f, center = center)
                            drawCircle(color = IplGold, radius = radius * 0.20f, center = center)

                            // Top Pointer Needle
                            val path = Path().apply {
                                moveTo(center.x, 0f)
                                lineTo(center.x - 14f, -20f)
                                lineTo(center.x + 14f, -20f)
                                close()
                            }
                            drawPath(path = path, color = IplGold)
                        }

                        // Wheel Center Icon
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Selected Winner Card with Circular Cutout Image
                    if (selectedPlayer != null) {
                        Surface(
                            color = surfaceBg,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, themeAccent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, themeAccent, CircleShape)
                                        .background(if (isLight) LightPitchCard else StadiumDark)
                                ) {
                                    DriveImage(
                                        url = selectedPlayer!!.imageUrl,
                                        contentDescription = selectedPlayer!!.name,
                                        modifier = Modifier.fillMaxSize(),
                                        fallbackIcon = getPlayerPlaceholderIcon(selectedPlayer!!.id, selectedPlayer!!.name)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedPlayer!!.name,
                                        color = textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${selectedPlayer!!.role.label} • Base: ${selectedPlayer!!.basePrice}Cr",
                                        color = textSub,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availablePlayers.isNotEmpty()) {
                val isQuizBlocking = isFixedSetStage && autoAssignLastPlayerAtAvg && fixedCount == 2 &&
                    captainQuizSession != null && captainQuizSession.winnerTeamId == null
                val canSpin = !isSpinning && (!isFixedSetStage || isFixedSetQuotaMet) && !isQuizBlocking
                Button(
                    onClick = {
                        if (canSpin) {
                            isSpinning = true
                            scope.launch {
                                val targetIndex = availablePlayers.indices.random()
                                val sliceAngle = 360f / availablePlayers.size
                                val targetAngle = 360f * 6 - (targetIndex * sliceAngle + sliceAngle / 2f) + 270f

                                rotationAnim.animateTo(
                                    targetValue = targetAngle,
                                    animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing)
                                )

                                val winner = availablePlayers[targetIndex]
                                selectedPlayer = winner
                                isSpinning = false
                                onPlayerSelected(winner)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canSpin) themeAccent else Color.Gray,
                        contentColor = if (isLight) Color.White else Color.Black
                    ),
                    enabled = canSpin
                ) {
                    Text(
                        if (isSpinning) "SPINNING..."
                        else if (isQuizBlocking) "COMPLETE CAPTAINS QUIZ FIRST"
                        else if (isFixedSetStage && !isFixedSetQuotaMet) "ADD $playersDeficit MORE TO SPIN ($fixedCount/$nextMultipleTarget)"
                        else "SPIN WHEEL!",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                enabled = !isSpinning
            ) {
                Text("Close", color = textSub)
            }
        },
        containerColor = cardBg
    )

    if (showFixedSetSelectorDialog && onTogglePlayerFixedSet != null) {
        FixedSetPlayerSelectorDialog(
            players = players,
            teams = teams,
            isDarkTheme = isDarkTheme,
            onTogglePlayerFixedSet = onTogglePlayerFixedSet,
            onSelectNextBatch = onSelectNextBatch,
            onClearAllFixed = onClearAllFixed,
            onDismiss = { showFixedSetSelectorDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedSetPlayerSelectorDialog(
    players: List<Player>,
    teams: List<Team>,
    isDarkTheme: Boolean = true,
    onTogglePlayerFixedSet: (playerId: String, isFixed: Boolean) -> Unit,
    onSelectNextBatch: ((count: Int) -> Unit)? = null,
    onClearAllFixed: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isLight = !isDarkTheme
    val cardBg = if (isLight) LightPitchCard else StadiumCardDark
    val surfaceBg = if (isLight) LightPitchSurface else StadiumSurface
    val textMain = if (isLight) LightPitchText else Color.White
    val textSub = if (isLight) LightPitchSecondary else Color.LightGray
    val themeAccent = if (isLight) LightPitchPrimary else IplGold

    val upNextPlayers = remember(players) {
        players.filter { it.status == PlayerStatus.UP_NEXT }
    }
    val fixedPlayers = remember(upNextPlayers) {
        upNextPlayers.filter { it.isFixedSet }
    }
    val fixedCount = fixedPlayers.size
    val totalTeams = teams.size

    val isQuotaMet = if (totalTeams <= 0) true else (fixedCount >= totalTeams && fixedCount % totalTeams == 0)
    val nextMultipleTarget = if (totalTeams <= 0) fixedCount else if (fixedCount < totalTeams) totalTeams else ((fixedCount / totalTeams) + 1) * totalTeams
    val playersDeficit = if (totalTeams <= 0 || isQuotaMet) 0 else nextMultipleTarget - fixedCount

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<PlayerRole?>(null) }

    val filteredPlayers = remember(upNextPlayers, searchQuery, selectedRoleFilter) {
        upNextPlayers.filter { player ->
            (searchQuery.isBlank() || player.name.contains(searchQuery, ignoreCase = true) || player.country.contains(searchQuery, ignoreCase = true)) &&
            (selectedRoleFilter == null || player.role == selectedRoleFilter)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = themeAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FIXED SET PLAYERS",
                            color = textMain,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        color = if (isQuotaMet && fixedCount > 0) BidGreen.copy(alpha = 0.2f) else themeAccent.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$fixedCount Selected",
                            color = if (isQuotaMet && fixedCount > 0) BidGreen else themeAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (totalTeams > 0) {
                        if (isQuotaMet && fixedCount > 0) "✅ Quota met: $fixedCount players (${fixedCount / totalTeams} round for $totalTeams teams)"
                        else if (fixedCount == 0) "ℹ️ Select at least $totalTeams players (or multiple: ${totalTeams * 2}, ${totalTeams * 3}...)"
                        else "⚠️ Need $playersDeficit more player(s) to reach multiple of $totalTeams ($nextMultipleTarget)"
                    } else "Select players to lock into the Spin Wheel fixed set",
                    color = if (isQuotaMet && fixedCount > 0) BidGreen else if (fixedCount > 0) UnsoldRed else textSub,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search player name...", fontSize = 12.sp, color = textSub) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = textSub, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain,
                        focusedBorderColor = themeAccent,
                        unfocusedBorderColor = textSub.copy(alpha = 0.5f),
                        focusedContainerColor = surfaceBg,
                        unfocusedContainerColor = surfaceBg
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    textStyle = TextStyle(fontSize = 12.sp, color = textMain)
                )

                // Quick Action Buttons (Select Top N, Clear All)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (totalTeams > 0 && onSelectNextBatch != null) {
                        val batchToSelect = if (playersDeficit > 0) playersDeficit else totalTeams
                        Button(
                            onClick = { onSelectNextBatch(batchToSelect) },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent.copy(alpha = 0.2f), contentColor = themeAccent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(30.dp)
                        ) {
                            Text("+ Auto Select $batchToSelect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (fixedCount > 0 && onClearAllFixed != null) {
                        OutlinedButton(
                            onClick = { onClearAllFixed() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = UnsoldRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, UnsoldRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Clear All", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Filter Chips by Role
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedRoleFilter == null,
                            onClick = { selectedRoleFilter = null },
                            label = { Text("All (${upNextPlayers.size})", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = surfaceBg,
                                labelColor = textSub,
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = if (isLight) Color.White else Color.Black
                            )
                        )
                    }
                    items(PlayerRole.values()) { role ->
                        val count = upNextPlayers.count { it.role == role }
                        FilterChip(
                            selected = selectedRoleFilter == role,
                            onClick = { selectedRoleFilter = if (selectedRoleFilter == role) null else role },
                            label = { Text("${role.label} ($count)", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = surfaceBg,
                                labelColor = textSub,
                                selectedContainerColor = themeAccent,
                                selectedLabelColor = if (isLight) Color.White else Color.Black
                            )
                        )
                    }
                }

                // Player List with Checkboxes
                if (filteredPlayers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No matching players found", color = textSub, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredPlayers, key = { it.id }) { player ->
                            val isChecked = player.isFixedSet
                            Surface(
                                color = if (isChecked) (if (isDarkTheme) IplGold.copy(alpha = 0.15f) else LightPitchPrimary.copy(alpha = 0.12f)) else surfaceBg,
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isChecked) 1.2.dp else 0.8.dp,
                                    if (isChecked) themeAccent else surfaceBg
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTogglePlayerFixedSet(player.id, !isChecked) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onTogglePlayerFixedSet(player.id, it) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = themeAccent,
                                            checkmarkColor = if (isLight) Color.White else Color.Black
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, if (isChecked) themeAccent else surfaceBg, CircleShape)
                                            .background(if (isLight) LightPitchCard else StadiumDark)
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = player.name,
                                                color = textMain,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isChecked) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "⭐ FIXED",
                                                    color = themeAccent,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${player.role.label} • ${player.country} • ₹${player.basePrice}Cr",
                                            color = textSub,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isLight) Color.White else Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isQuotaMet && fixedCount > 0) "APPLY FIXED SET ($fixedCount PLAYERS) ✅"
                    else if (fixedCount == 0) "DONE (OPEN CATEGORY)"
                    else "CLOSE & SAVE ($fixedCount/$nextMultipleTarget)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        },
        containerColor = cardBg
    )
}

@Composable
fun CaptainMathQuizDialog(
    team: Team,
    questions: List<MathQuizQuestion>,
    isDarkTheme: Boolean = true,
    onSubmit: (correctCount: Int, timeTakenMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val isLight = !isDarkTheme
    val cardBg = if (isLight) LightPitchCard else StadiumCardDark
    val surfaceBg = if (isLight) LightPitchSurface else StadiumSurface
    val textMain = if (isLight) LightPitchText else Color.White
    val textSub = if (isLight) LightPitchSecondary else Color.LightGray
    val themeAccent = if (isLight) LightPitchPrimary else IplGold

    var currentIndex by remember(team.id, questions) { mutableStateOf(0) }
    var correctCount by remember(team.id, questions) { mutableStateOf(0) }
    val startTimeMs = remember(team.id, questions) { System.currentTimeMillis() }
    var elapsedSeconds by remember(team.id, questions) { mutableStateOf(0.0) }

    LaunchedEffect(team.id, questions) {
        while (true) {
            delay(100L)
            elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000.0
        }
    }

    val currentQuestion = questions.getOrNull(currentIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🧠 ${team.name} Quiz",
                        color = themeAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "⏱ ${String.format("%.1f", elapsedSeconds)}s",
                            color = NeonBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size} • Score: $correctCount",
                    color = textSub,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            if (currentQuestion != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, themeAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentQuestion.prompt,
                            color = textMain,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 20.dp, horizontal = 12.dp)
                        )
                    }

                    Text(
                        text = "Select the correct answer (3 options):",
                        color = textSub,
                        fontSize = 11.sp
                    )

                    currentQuestion.options.forEach { option ->
                        Button(
                            onClick = {
                                val nextCorrect = if (option == currentQuestion.correctAnswer) correctCount + 1 else correctCount
                                if (currentIndex + 1 >= questions.size) {
                                    val totalTimeMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1L)
                                    onSubmit(nextCorrect, totalTimeMs)
                                } else {
                                    correctCount = nextCorrect
                                    currentIndex += 1
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeAccent,
                                contentColor = if (isLight) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text(
                                text = "$option",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textSub)
            }
        },
        containerColor = cardBg
    )
}
