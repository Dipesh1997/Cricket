package cricket.player.auction.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
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
import cricket.player.auction.model.*
import cricket.player.auction.ui.components.*
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuctionScreen(viewModel: AuctionViewModel) {
    val context = LocalContext.current
    val activeTournament by viewModel.activeTournament.collectAsState()
    val activePlayer by viewModel.activePlayer.collectAsState()
    val highestBidderTeam by viewModel.highestBidderTeam.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val bids by viewModel.bids.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val players by viewModel.players.collectAsState()
    val joinedCaptainTeams by viewModel.joinedCaptainTeams.collectAsState()
    val captainReadyMap by viewModel.captainReadyMap.collectAsState()
    val areAllCaptainsReady by viewModel.areAllCaptainsReady.collectAsState()
    val isBiddingStarted by viewModel.isBiddingStarted.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val lastAutoAssignedMessage by viewModel.lastAutoAssignedMessage.collectAsState()
    val isAdmin = currentUser?.role == UserRole.ADMIN_AUCTIONEER

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    var selectedBiddingTeamId by remember { mutableStateOf<String?>(null) }
    var showTeamSelectorDialog by remember { mutableStateOf(false) }
    var showSpinWheelModal by remember { mutableStateOf(false) }
    var showFixedSetSelectorModal by remember { mutableStateOf(false) }
    var showEditSoldAmountDialog by remember { mutableStateOf(false) }
    var playerToEditSoldAmount by remember { mutableStateOf<Player?>(null) }
    var showRemovePlayerDialog by remember { mutableStateOf(false) }
    var isRecentlySoldExpanded by remember { mutableStateOf(true) }
    var quizTargetTeamId by remember { mutableStateOf<String?>(null) }

    val tourneyId = activeTournament?.id ?: ""
    val tourneyTeams = remember(teams, tourneyId) {
        teams.filter { it.tournamentId == tourneyId }
    }
    val setQuotaPerTeam = remember(players, tourneyTeams, tourneyId) {
        if (tourneyId.isNotBlank()) viewModel.getSetQuotaPerTeam(tourneyId) else 0
    }
    val teamPickedCounts = remember(players, tourneyTeams, tourneyId) {
        tourneyTeams.associate { t ->
            t.id to (if (tourneyId.isNotBlank()) viewModel.getTeamSetPickedCount(tourneyId, t.id) else 0)
        }
    }
    val currentSpinTeam = remember(tourneyTeams, activeTournament?.spinTurnIndex, teamPickedCounts, setQuotaPerTeam) {
        if (tourneyTeams.isEmpty()) null
        else {
            val idx = (activeTournament?.spinTurnIndex ?: 0).coerceAtLeast(0) % tourneyTeams.size
            val candidate = tourneyTeams[idx]
            if (setQuotaPerTeam > 0 && (teamPickedCounts[candidate.id] ?: 0) >= setQuotaPerTeam) {
                tourneyTeams.firstOrNull { (teamPickedCounts[it.id] ?: 0) < setQuotaPerTeam } ?: candidate
            } else {
                candidate
            }
        }
    }

    // Auto-select user assigned team if team captain or default to the first tournament team
    LaunchedEffect(currentUser, teams) {
        if (currentUser?.role == UserRole.TEAM_CAPTAIN && currentUser?.assignedTeamId != null) {
            selectedBiddingTeamId = currentUser?.assignedTeamId
        } else if (teams.isNotEmpty() && (selectedBiddingTeamId == null || teams.none { it.id == selectedBiddingTeamId })) {
            selectedBiddingTeamId = teams.first().id
        }
    }

    val effectiveBiddingTeam = (if (currentUser?.role == UserRole.TEAM_CAPTAIN && currentUser?.assignedTeamId != null) {
        teams.find { it.id == currentUser?.assignedTeamId }
    } else {
        teams.find { it.id == selectedBiddingTeamId }
    }) ?: teams.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Status Bar & Spin Wheel Trigger ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(BidGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE IPL AUCTION ARENA",
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Spin Wheel Trigger Button
                        Button(
                            onClick = { showSpinWheelModal = true },
                            colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SPIN WHEEL", fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }

                        // Fixed Set Multi-Select Checkboxes Trigger Button
                        if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) {
                            val fixedCount = players.count { it.status == PlayerStatus.UP_NEXT && it.isFixedSet }
                            Surface(
                                onClick = { showFixedSetSelectorModal = true },
                                color = surfaceBg,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null, tint = themeAccent, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("SET: $fixedCount 📝", color = textMain, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }

                        // Live Timer Badge
                        Surface(
                            color = if (timerSeconds <= 5) UnsoldRed else StadiumSurface,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${timerSeconds}s",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Set Rule, Equal Distribution & Captain Math Quiz Card ---
        val upNextFixedCount = players.count { it.tournamentId == tourneyId && it.isFixedSet && it.status == PlayerStatus.UP_NEXT }
        val quizSessionNow = activeTournament?.captainQuizSession
        if (tourneyTeams.isNotEmpty() && (setQuotaPerTeam > 0 || isAdmin || quizSessionNow != null)) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (quizSessionNow != null && quizSessionNow.winnerTeamId == null) NeonBlue else surfaceBg
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = activeTournament?.autoAssignLastPlayerAtAvg == true,
                                    onCheckedChange = { checked ->
                                        if (isAdmin && tourneyId.isNotBlank()) {
                                            viewModel.setTournamentAutoAssignRule(tourneyId, checked)
                                        }
                                    },
                                    enabled = isAdmin,
                                    colors = CheckboxDefaults.colors(checkedColor = BidGreen, checkmarkColor = Color.White),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Last Player Auto-Assign at Avg Price",
                                        color = textMain,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = if (currentSpinTeam != null) "🎡 Spin Turn: ${currentSpinTeam.shortCode}" else "Equal set quota active",
                                        color = themeAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (setQuotaPerTeam > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                tourneyTeams.forEach { t ->
                                    val picked = teamPickedCounts[t.id] ?: 0
                                    val done = picked >= setQuotaPerTeam
                                    Surface(
                                        color = if (done) BidGreen.copy(alpha = 0.16f) else surfaceBg,
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (done) BidGreen else textSub.copy(alpha = 0.25f))
                                    ) {
                                        Text(
                                            text = "${t.shortCode}: $picked/$setQuotaPerTeam${if (done) "✅" else ""}",
                                            color = if (done) BidGreen else textMain,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (upNextFixedCount == 2 && activeTournament?.autoAssignLastPlayerAtAvg == true && quizSessionNow != null) {
                            val t1 = tourneyTeams.find { it.id == quizSessionNow.team1Id }
                            val t2 = tourneyTeams.find { it.id == quizSessionNow.team2Id }
                            val s1 = quizSessionNow.team1Submission
                            val s2 = quizSessionNow.team2Submission
                            val winnerTeam = tourneyTeams.find { it.id == quizSessionNow.winnerTeamId }

                            Surface(
                                color = if (winnerTeam != null) BidGreen.copy(alpha = 0.14f) else NeonBlue.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (winnerTeam != null) BidGreen else NeonBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (winnerTeam != null)
                                            "🏆 Quiz Winner: ${winnerTeam.name} spins for 2nd-last player!"
                                        else
                                            "🧠 2 Players Left! Captains 5-Q Math Quiz (+, -, ×, ÷)",
                                        color = if (winnerTeam != null) BidGreen else NeonBlue,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (t1 != null) {
                                            val canPlay1 = (s1?.isCompleted != true) && (isAdmin || currentUser?.assignedTeamId == t1.id)
                                            Button(
                                                onClick = { quizTargetTeamId = t1.id },
                                                enabled = canPlay1,
                                                colors = ButtonDefaults.buttonColors(containerColor = if (s1?.isCompleted == true) BidGreen else NeonBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = if (s1?.isCompleted == true) "${t1.shortCode}: ${s1.correctCount}/5 (${String.format("%.1f", s1.timeTakenMs / 1000.0)}s)" else "🧠 Quiz: ${t1.shortCode}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        if (t2 != null) {
                                            val canPlay2 = (s2?.isCompleted != true) && (isAdmin || currentUser?.assignedTeamId == t2.id)
                                            Button(
                                                onClick = { quizTargetTeamId = t2.id },
                                                enabled = canPlay2,
                                                colors = ButtonDefaults.buttonColors(containerColor = if (s2?.isCompleted == true) BidGreen else NeonBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = if (s2?.isCompleted == true) "${t2.shortCode}: ${s2.correctCount}/5 (${String.format("%.1f", s2.timeTakenMs / 1000.0)}s)" else "🧠 Quiz: ${t2.shortCode}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Black,
                                                    maxLines = 1
                                                )
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

        // --- Main Player Stage Card ---
        item {
            val player = activePlayer
            if (player != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, themeAccent)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Player Set Tag & Overseas Badge & Fixed Set Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatChip(text = player.setName, backgroundColor = surfaceBg, textColor = themeAccent)

                                FilterChip(
                                    selected = player.isFixedSet,
                                    onClick = {
                                        if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) {
                                            val newFixed = !player.isFixedSet
                                            viewModel.togglePlayerFixedSet(player.id, newFixed)
                                            val tourneyTeams = teams.filter { it.tournamentId == activeTournament?.id }
                                            val currentFixed = players.count {
                                                it.tournamentId == activeTournament?.id && it.status == PlayerStatus.UP_NEXT && (if (it.id == player.id) newFixed else it.isFixedSet)
                                            }
                                            val tCount = tourneyTeams.size
                                            if (tCount > 0) {
                                                if (currentFixed >= tCount && currentFixed % tCount == 0) {
                                                    Toast.makeText(context, "⭐ Fixed Set complete: $currentFixed players (${currentFixed / tCount} round for $tCount teams)!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val target = if (currentFixed < tCount) tCount else ((currentFixed / tCount) + 1) * tCount
                                                    val needed = target - currentFixed
                                                    Toast.makeText(context, "⭐ Fixed Set: $currentFixed players. Add $needed more for $tCount teams multiple ($target)!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    label = { Text(if (player.isFixedSet) "⭐ Fixed Set" else "+ Fix Set", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = themeAccent,
                                        selectedLabelColor = Color.Black,
                                        containerColor = surfaceBg,
                                        labelColor = textMain
                                    )
                                )
                            }

                            if (player.isOverseas) {
                                StatChip(
                                    text = "Overseas (${player.country})",
                                    backgroundColor = if (isDarkTheme) NeonBlue.copy(alpha = 0.2f) else LightPitchSurface,
                                    textColor = textMain,
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                )
                            } else {
                                StatChip(text = player.country, backgroundColor = surfaceBg, textColor = textSub)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player Photo from Google Drive
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, themeAccent, RoundedCornerShape(16.dp))
                        ) {
                            DriveImage(
                                url = player.imageUrl,
                                contentDescription = player.name,
                                modifier = Modifier.fillMaxSize(),
                                fallbackIcon = getPlayerPlaceholderIcon(player.id, player.name)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player Name & Role
                        Text(
                            text = player.name,
                            color = textMain,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatChip(text = player.role.label, backgroundColor = themeAccent.copy(alpha = 0.2f), textColor = themeAccent)
                            StatChip(text = "Base: ${player.basePrice.formatIplCurrency()}", backgroundColor = surfaceBg, textColor = textMain)
                        }

                        if (player.stats.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = player.stats,
                                color = textSub,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = textSub.copy(alpha = 0.2f)
                        )

                        // --- Live Bid Dashboard ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (player.status == PlayerStatus.SOLD) "SOLD PRICE" else "CURRENT BID",
                                color = textSub,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            AnimatedContent(
                                targetState = player.currentBid,
                                label = "bid_anim"
                            ) { bidVal ->
                                Text(
                                    text = bidVal.formatIplCurrency(),
                                    color = if (player.status == PlayerStatus.SOLD) BidGreen else themeAccent,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            if (player.status == PlayerStatus.SOLD && currentUser?.role == UserRole.ADMIN_AUCTIONEER) {
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        playerToEditSoldAmount = player
                                        showEditSoldAmountDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = themeAccent, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Sold Amount ✏️", color = themeAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Highest Bidder Team Display
                            val bidderTeam = highestBidderTeam
                            if (bidderTeam != null) {
                                TeamBadge(team = bidderTeam, showPurse = true)
                            } else {
                                Surface(
                                    color = surfaceBg,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "No Bids Placed Yet",
                                        color = textSub,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Interactive Guide with icons and dummy illustration when no active player
                EmptyStateGuideCard(
                    mainIcon = Icons.Default.Gavel,
                    badgeText = "AUCTION ARENA READY",
                    title = "Auction Stage is Empty 🏏",
                    description = "Draft players onto the auction stage using the Spin Wheel or Fixed Sets to start real-time bidding.",
                    steps = listOf(
                        GuideStep(
                            stepNumber = 1,
                            title = "Spin Wheel or Fix Sets",
                            description = "Tap 'SPIN WHEEL' or 'SET' at the top to draft upcoming players onto the stage.",
                            icon = Icons.Default.Casino
                        ),
                        GuideStep(
                            stepNumber = 2,
                            title = "Captains Ready & Live Bidding",
                            description = "Captains tap ready, and bidding starts with live quick increments (+₹20L, +₹50L, +₹1Cr).",
                            icon = Icons.Default.Bolt
                        ),
                        GuideStep(
                            stepNumber = 3,
                            title = "Admin Auction Controls",
                            description = "Sell to highest bidder or mark unsold. Admin can edit sold amounts, undo bids, or undo sold players anytime.",
                            icon = Icons.Default.Gavel
                        )
                    ),
                    primaryButtonText = "SPIN WHEEL 🎡",
                    onPrimaryClick = { showSpinWheelModal = true },
                    secondaryButtonText = if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) "SELECT SET 📝" else null,
                    onSecondaryClick = if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) { { showFixedSetSelectorModal = true } } else null,
                    isDarkTheme = isDarkTheme,
                    accentColor = themeAccent
                )
            }
        }

        // --- Captain Readiness Check Banner ---
        item {
            if (currentUser?.role == UserRole.TEAM_CAPTAIN) {
                val myTeamId = currentUser?.assignedTeamId
                val isMyCaptainReady = myTeamId != null && (captainReadyMap[myTeamId] == true)
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isMyCaptainReady) BidGreen.copy(alpha = 0.15f) else surfaceBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isMyCaptainReady) BidGreen else themeAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isMyCaptainReady) Icons.Default.CheckCircle else Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isMyCaptainReady) BidGreen else themeAccent
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isMyCaptainReady) "YOU ARE READY FOR AUCTION ✅" else "CAPTAIN READINESS CHECK",
                                    color = if (isMyCaptainReady) BidGreen else textMain,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (isMyCaptainReady) "Waiting for other captains & auctioneer to begin..." else "Click ready so auctioneer can begin bidding!",
                                    color = textSub,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.setMyCaptainReady(!isMyCaptainReady) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMyCaptainReady) Color.DarkGray else BidGreen,
                                contentColor = if (isMyCaptainReady) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isMyCaptainReady) "UNREADY" else "I AM READY 👍", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            } else if (currentUser?.role == UserRole.ADMIN_AUCTIONEER && joinedCaptainTeams.isNotEmpty()) {
                val readyCount = joinedCaptainTeams.count { captainReadyMap[it.id] == true }
                val allReady = readyCount == joinedCaptainTeams.size
                Card(
                    colors = CardDefaults.cardColors(containerColor = surfaceBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (allReady || isBiddingStarted) BidGreen else themeAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (allReady || isBiddingStarted) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (allReady || isBiddingStarted) BidGreen else themeAccent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (allReady || isBiddingStarted) "ALL CAPTAINS READY ($readyCount/${joinedCaptainTeams.size}) 🚀" else "WAITING FOR CAPTAINS ($readyCount/${joinedCaptainTeams.size} READY)",
                                    color = if (allReady || isBiddingStarted) BidGreen else themeAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            if (allReady && !isBiddingStarted) {
                                Button(
                                    onClick = { viewModel.startBidding() },
                                    colors = ButtonDefaults.buttonColors(containerColor = BidGreen, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("START BIDDING 🚀", fontWeight = FontWeight.Black, fontSize = 11.sp)
                                }
                            } else if (!isBiddingStarted) {
                                TextButton(onClick = { viewModel.startBidding() }) {
                                    Text("Force Start (Override)", color = textSub, fontSize = 10.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            joinedCaptainTeams.forEach { team ->
                                val isReady = captainReadyMap[team.id] == true
                                Surface(
                                    color = if (isReady) BidGreen.copy(alpha = 0.15f) else textMain.copy(alpha = 0.05f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isReady) BidGreen else textSub.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${team.shortCode}: ${if (isReady) "READY ✅" else "WAITING ⏳"}",
                                        color = if (isReady) BidGreen else textSub,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Bidding Controls Section or Spectator View Banner ---
        item {
            if (currentUser?.role == UserRole.SPECTATOR) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SPECTATOR LIVE AUCTION VIEW",
                                color = textMain,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You are watching this auction in real-time spectator mode. Bids and auction outcomes update live on your screen.",
                            color = textSub,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BIDDING TEAM",
                                color = textSub,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            val isTeamLocked = currentUser?.assignedTeamId != null
                            TextButton(
                                onClick = {
                                    if (!isTeamLocked) {
                                        showTeamSelectorDialog = true
                                    }
                                },
                                enabled = !isTeamLocked
                            ) {
                                Text(
                                    text = if (isTeamLocked) "🔒 ${effectiveBiddingTeam?.name ?: "My Team"}" else (effectiveBiddingTeam?.name ?: "Select Team"),
                                    color = themeAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isTeamLocked) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = themeAccent)
                                }
                            }
                        }

                        if (effectiveBiddingTeam != null) {
                            PurseProgressBar(
                                spent = effectiveBiddingTeam.spentPurse,
                                total = effectiveBiddingTeam.totalPurse,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "QUICK BID INCREMENTS",
                            color = textSub,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val activeP = activePlayer
                        val isAuctionReady = joinedCaptainTeams.isEmpty() || isBiddingStarted || areAllCaptainsReady
                        val isTeamSetQuotaReached = activeP?.isFixedSet == true && setQuotaPerTeam > 0 &&
                            (teamPickedCounts[effectiveBiddingTeam?.id] ?: 0) >= setQuotaPerTeam
                        val isBidEnabled = activeP != null && activeP.status != PlayerStatus.SOLD &&
                            effectiveBiddingTeam != null && isAuctionReady && !isTeamSetQuotaReached
                        val availablePurse = effectiveBiddingTeam?.remainingPurse ?: 0.0

                        if (isTeamSetQuotaReached && effectiveBiddingTeam != null) {
                            Surface(
                                color = BidGreen.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "✅ ${effectiveBiddingTeam.name} completed its set quota ($setQuotaPerTeam/$setQuotaPerTeam)! Other teams bid for remaining players.",
                                    color = BidGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (!isAuctionReady) {
                            Surface(
                                color = themeAccent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            ) {
                                Text(
                                    text = "⏳ Bidding is locked until all joined captains click READY",
                                    color = themeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BidIncrementButton(
                                    label = "+₹20L",
                                    amountCr = 0.20,
                                    availablePurse = availablePurse,
                                    isEnabled = isBidEnabled,
                                    onClick = {
                                        if (effectiveBiddingTeam != null) {
                                            viewModel.placeBid(effectiveBiddingTeam.id, 0.20)
                                        }
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BidIncrementButton(
                                    label = "+₹50L",
                                    amountCr = 0.50,
                                    availablePurse = availablePurse,
                                    isEnabled = isBidEnabled,
                                    onClick = {
                                        if (effectiveBiddingTeam != null) {
                                            viewModel.placeBid(effectiveBiddingTeam.id, 0.50)
                                        }
                                    }
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BidIncrementButton(
                                    label = "+₹1.00Cr",
                                    amountCr = 1.00,
                                    availablePurse = availablePurse,
                                    isEnabled = isBidEnabled,
                                    onClick = {
                                        if (effectiveBiddingTeam != null) {
                                            viewModel.placeBid(effectiveBiddingTeam.id, 1.00)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Admin Auctioneer Controls ---
        if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = surfaceBg),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AUCTIONEER ADMIN CONTROLS",
                            color = textMain,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val tourneySoldPlayers = remember(players, activeTournament) {
                            players.filter { it.tournamentId == activeTournament?.id && it.status == PlayerStatus.SOLD }
                        }
                        val canUndoBid = activePlayer != null && activePlayer?.status != PlayerStatus.SOLD && bids.any { it.playerId == activePlayer?.id }
                        val canUndoSold = tourneySoldPlayers.isNotEmpty()
                        val canEditSold = (activePlayer?.status == PlayerStatus.SOLD) || tourneySoldPlayers.isNotEmpty()

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1: Sell Bid & Sell Base Price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val buyerName = highestBidderTeam?.name ?: effectiveBiddingTeam?.name ?: "Highest Bidder"
                                        val pName = activePlayer?.name ?: "Player"
                                        if (viewModel.sellActivePlayer()) {
                                            Toast.makeText(context, "🎉 $pName SOLD to $buyerName!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BidGreen, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f),
                                    enabled = activePlayer != null && highestBidderTeam != null && activePlayer?.status != PlayerStatus.SOLD
                                ) {
                                    Icon(imageVector = Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SELL BID", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (effectiveBiddingTeam != null) {
                                            val buyerName = effectiveBiddingTeam.name
                                            val pName = activePlayer?.name ?: "Player"
                                            if (viewModel.sellActivePlayerAtBasePrice(effectiveBiddingTeam.id)) {
                                                Toast.makeText(context, "🎉 $pName SOLD to $buyerName at Base Price!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f),
                                    enabled = activePlayer != null && effectiveBiddingTeam != null && activePlayer?.status != PlayerStatus.SOLD &&
                                        !(activePlayer?.isFixedSet == true && setQuotaPerTeam > 0 && (teamPickedCounts[effectiveBiddingTeam?.id] ?: 0) >= setQuotaPerTeam)
                                ) {
                                    Icon(imageVector = Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SELL BASE PRICE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Row 2: Mark Unsold & Undo Last Bid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.markActivePlayerUnsold() },
                                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed),
                                    modifier = Modifier.weight(1f),
                                    enabled = activePlayer != null && activePlayer?.status != PlayerStatus.SOLD
                                ) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MARK UNSOLD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (viewModel.undoLastBid()) {
                                            Toast.makeText(context, "↩️ Last bid undone!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canUndoBid) (if (isDarkTheme) Color(0xFFFF9800) else Color(0xFFE65100)) else Color.Gray.copy(alpha = 0.5f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f),
                                    enabled = canUndoBid
                                ) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("UNDO BID", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Row 3: Edit Sold Amount & Undo Last Player Sold
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val target = if (activePlayer?.status == PlayerStatus.SOLD) activePlayer else tourneySoldPlayers.lastOrNull()
                                        if (target != null) {
                                            playerToEditSoldAmount = target
                                            showEditSoldAmountDialog = true
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canEditSold) (if (isDarkTheme) NeonBlue else LightPitchPrimary) else Color.Gray.copy(alpha = 0.5f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f),
                                    enabled = canEditSold
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("EDIT SOLD ₹", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        val restored = viewModel.undoLastSoldPlayer()
                                        if (restored != null) {
                                            Toast.makeText(context, "🔄 Undone sold player ${restored.name}! Restored to auction stage.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "No sold player found to undo", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (canUndoSold) (if (isDarkTheme) Color(0xFF9C27B0) else Color(0xFF6A1B9A)) else Color.Gray.copy(alpha = 0.5f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f),
                                    enabled = canUndoSold
                                ) {
                                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("UNDO SOLD", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            // Row 4: Remove Player from Pool
                            if (activePlayer != null) {
                                OutlinedButton(
                                    onClick = { showRemovePlayerDialog = true },
                                    border = androidx.compose.foundation.BorderStroke(1.dp, UnsoldRed),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = UnsoldRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("REMOVE CURRENT PLAYER FROM POOL 🗑️", color = UnsoldRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Live Bidding History Log ---
        item {
            Text(
                text = "LIVE BIDDING LOG",
                color = textSub,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val activeBids = bids.filter { it.playerId == activePlayer?.id }
        if (activeBids.isEmpty()) {
            item {
                Text(
                    text = "No bids recorded for this player yet",
                    color = textSub,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(activeBids) { bid ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
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
                            text = bid.teamName,
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = bid.amount.formatIplCurrency(),
                            color = themeAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // --- Recently Sold Players (Expandable - Last 3) ---
        item {
            val last3SoldPlayers = remember(players, activeTournament, bids) {
                players.filter { it.tournamentId == activeTournament?.id && it.status == PlayerStatus.SOLD }
                    .sortedByDescending { p ->
                        bids.filter { it.playerId == p.id }.maxOfOrNull { it.timestamp } ?: 0L
                    }
                    .take(3)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRecentlySoldExpanded = !isRecentlySoldExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RECENTLY SOLD PLAYERS (LAST 3)",
                                color = textMain,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = themeAccent.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "${last3SoldPlayers.size}",
                                    color = themeAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { isRecentlySoldExpanded = !isRecentlySoldExpanded },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecentlySoldExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isRecentlySoldExpanded) "Collapse" else "Expand",
                                tint = textSub
                            )
                        }
                    }

                    if (isRecentlySoldExpanded) {
                        Spacer(modifier = Modifier.height(10.dp))
                        if (last3SoldPlayers.isEmpty()) {
                            Text(
                                text = "No players sold yet in this tournament",
                                color = textSub,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                last3SoldPlayers.forEach { soldP ->
                                    val buyerTeam = teams.find { it.id == soldP.soldToTeamId }
                                    Surface(
                                        color = if (isDarkTheme) StadiumSurface else LightPitchSurface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .border(1.dp, themeAccent, CircleShape)
                                                        .background(if (isDarkTheme) StadiumDark else LightPitchCard)
                                                ) {
                                                    DriveImage(
                                                        url = soldP.imageUrl,
                                                        contentDescription = soldP.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        fallbackIcon = getPlayerPlaceholderIcon(soldP.id, soldP.name)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = soldP.name,
                                                        color = textMain,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = soldP.role.label + if (soldP.isOverseas) " ✈️" else "",
                                                            color = textSub,
                                                            fontSize = 11.sp
                                                        )
                                                        if (buyerTeam != null) {
                                                            Text(
                                                                text = " • ${buyerTeam.shortCode}",
                                                                color = themeAccent,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = (soldP.soldPrice ?: soldP.currentBid).formatIplCurrency(),
                                                    color = BidGreen,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp
                                                )
                                                if (isAdmin) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Button(
                                                        onClick = {
                                                            viewModel.removePlayerFromTeamForReAuction(soldP.id)
                                                            Toast.makeText(context, "Undid sale of ${soldP.name}. Restored to auction stage!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFFEF5350).copy(alpha = 0.2f),
                                                            contentColor = Color(0xFFEF5350)
                                                        ),
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Text(
                                                            text = "UNDO",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
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

    // --- Team Switcher Dialog ---
    if (showTeamSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showTeamSelectorDialog = false },
            title = { Text("Select Bidding Team", color = textMain, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    teams.forEach { team ->
                        val isSelected = team.id == effectiveBiddingTeam?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBiddingTeamId = team.id
                                    showTeamSelectorDialog = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = if (setQuotaPerTeam > 0) "${team.name} (${teamPickedCounts[team.id] ?: 0}/$setQuotaPerTeam)" else team.name,
                                    color = if (isSelected) themeAccent else textMain,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            }
                            Text(text = team.remainingPurse.formatIplCurrency(), color = themeAccent, fontSize = 12.sp)
                        }
                        HorizontalDivider(color = textSub.copy(alpha = 0.2f))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTeamSelectorDialog = false }) {
                    Text("Close", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Interactive Spin Wheel Dialog ---
    if (showSpinWheelModal) {
        SpinWheelModal(
            players = players.filter { it.tournamentId == tourneyId },
            teams = tourneyTeams,
            isDarkTheme = isDarkTheme,
            autoAssignLastPlayerAtAvg = activeTournament?.autoAssignLastPlayerAtAvg == true,
            onToggleAutoAssignRule = if (isAdmin && tourneyId.isNotBlank()) {
                { enabled -> viewModel.setTournamentAutoAssignRule(tourneyId, enabled) }
            } else null,
            currentSpinTeam = currentSpinTeam,
            teamPickedCounts = teamPickedCounts,
            quotaPerTeam = setQuotaPerTeam,
            captainQuizSession = activeTournament?.captainQuizSession,
            onOpenQuizForTeam = { tId ->
                quizTargetTeamId = tId
            },
            onTogglePlayerFixedSet = { playerId, isFixed ->
                viewModel.togglePlayerFixedSet(playerId, isFixed)
            },
            onSelectNextBatch = { count ->
                val unselected = players.filter { it.tournamentId == activeTournament?.id && it.status == PlayerStatus.UP_NEXT && !it.isFixedSet }.take(count)
                viewModel.setMultiplePlayersFixedSet(unselected.map { it.id }.toSet(), true)
            },
            onClearAllFixed = {
                activeTournament?.id?.let { viewModel.clearAllFixedSets(it) }
            },
            onDismiss = { showSpinWheelModal = false },
            onPlayerSelected = { selectedPlayer ->
                viewModel.selectActivePlayer(selectedPlayer.id)
                if (tourneyId.isNotBlank()) {
                    viewModel.advanceSpinTurn(tourneyId)
                }
                showSpinWheelModal = false
            }
        )
    }

    // --- Captain 5-Question Math Quiz Dialog (+, -, ×, ÷) ---
    val activeQuizSession = activeTournament?.captainQuizSession
    if (quizTargetTeamId != null && activeQuizSession != null && activeQuizSession.questions.isNotEmpty()) {
        val quizTeam = tourneyTeams.find { it.id == quizTargetTeamId }
        if (quizTeam != null) {
            cricket.player.auction.ui.components.CaptainMathQuizDialog(
                team = quizTeam,
                questions = activeQuizSession.questions,
                isDarkTheme = isDarkTheme,
                onSubmit = { correctCount, timeTakenMs ->
                    viewModel.submitCaptainQuizResult(tourneyId, quizTeam.id, correctCount, timeTakenMs)
                    quizTargetTeamId = null
                    Toast.makeText(
                        context,
                        "🧠 ${quizTeam.shortCode} scored $correctCount/5 in ${String.format("%.1f", timeTakenMs / 1000.0)}s!",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = { quizTargetTeamId = null }
            )
        }
    }

    // --- Auto-Assign Last Player Celebration Dialog ---
    if (lastAutoAssignedMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAutoAssignedMessage() },
            title = {
                Text("⚡ Last Player Auto-Assigned!", color = BidGreen, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            },
            text = {
                Text(
                    text = lastAutoAssignedMessage!!,
                    color = textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearAutoAssignedMessage() },
                    colors = ButtonDefaults.buttonColors(containerColor = BidGreen, contentColor = Color.White)
                ) {
                    Text("AWESOME!", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Fixed Set Multi-Player Checkbox Selector Dialog ---
    if (showFixedSetSelectorModal) {
        val tourneyTeams = remember(teams, activeTournament) {
            teams.filter { it.tournamentId == activeTournament?.id }
        }
        FixedSetPlayerSelectorDialog(
            players = players,
            teams = tourneyTeams,
            isDarkTheme = isDarkTheme,
            onTogglePlayerFixedSet = { playerId, isFixed ->
                viewModel.togglePlayerFixedSet(playerId, isFixed)
            },
            onSelectNextBatch = { count ->
                val unselected = players.filter { it.tournamentId == activeTournament?.id && it.status == PlayerStatus.UP_NEXT && !it.isFixedSet }.take(count)
                viewModel.setMultiplePlayersFixedSet(unselected.map { it.id }.toSet(), true)
            },
            onClearAllFixed = {
                activeTournament?.id?.let { viewModel.clearAllFixedSets(it) }
            },
            onDismiss = { showFixedSetSelectorModal = false }
        )
    }

    // --- Admin Edit Sold Price Dialog ---
    if (showEditSoldAmountDialog && playerToEditSoldAmount != null) {
        val targetPlayer = playerToEditSoldAmount!!
        val buyerTeam = teams.find { it.id == targetPlayer.soldToTeamId }
        var inputAmountStr by remember(targetPlayer) {
            mutableStateOf(String.format(java.util.Locale.US, "%.2f", targetPlayer.soldPrice ?: targetPlayer.currentBid))
        }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                showEditSoldAmountDialog = false
                playerToEditSoldAmount = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = themeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Sold Amount", color = textMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Player: ${targetPlayer.name} (${targetPlayer.role.label})",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Sold to: ${buyerTeam?.name ?: "Unknown Team"}",
                        color = themeAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Enter the corrected final sold amount in Crores (₹ Cr). Team purse will adjust automatically.",
                        color = textSub,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = inputAmountStr,
                        onValueChange = {
                            inputAmountStr = it
                            errorMsg = null
                        },
                        label = { Text("Sold Amount (₹ Cr)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeAccent,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(text = errorMsg!!, color = UnsoldRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmount = inputAmountStr.toDoubleOrNull()
                        if (newAmount == null || newAmount <= 0) {
                            errorMsg = "Please enter a valid positive number"
                            return@Button
                        }
                        val success = viewModel.editPlayerSoldPrice(targetPlayer.id, newAmount)
                        if (success) {
                            Toast.makeText(context, "✅ Sold price for ${targetPlayer.name} updated to ${newAmount.formatIplCurrency()}!", Toast.LENGTH_SHORT).show()
                            showEditSoldAmountDialog = false
                            playerToEditSoldAmount = null
                        } else {
                            errorMsg = "Failed to update: Team may not have sufficient purse budget"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White)
                ) {
                    Text("SAVE AMOUNT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditSoldAmountDialog = false
                        playerToEditSoldAmount = null
                    }
                ) {
                    Text("CANCEL", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Admin Remove Player Confirmation Dialog ---
    if (showRemovePlayerDialog && activePlayer != null) {
        val playerToRemove = activePlayer!!
        AlertDialog(
            onDismissRequest = { showRemovePlayerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = UnsoldRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Player?", color = textMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${playerToRemove.name}' from the tournament roster? If the player was sold, the purchase amount will be refunded to the buying team.",
                    color = textSub,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayer(playerToRemove.id)
                        Toast.makeText(context, "🗑️ Player ${playerToRemove.name} removed from pool", Toast.LENGTH_SHORT).show()
                        showRemovePlayerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("REMOVE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemovePlayerDialog = false }) {
                    Text("CANCEL", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }
}
