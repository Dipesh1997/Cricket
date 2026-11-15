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

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    var selectedBiddingTeamId by remember { mutableStateOf<String?>(null) }
    var showTeamSelectorDialog by remember { mutableStateOf(false) }
    var showSpinWheelModal by remember { mutableStateOf(false) }

    // Auto-select user assigned team if team captain
    LaunchedEffect(currentUser, teams) {
        if (currentUser?.role == UserRole.TEAM_CAPTAIN && currentUser?.assignedTeamId != null) {
            selectedBiddingTeamId = currentUser?.assignedTeamId
        } else if (selectedBiddingTeamId == null && teams.isNotEmpty()) {
            selectedBiddingTeamId = teams.first().id
        }
    }

    val currentBiddingTeam = teams.find { it.id == selectedBiddingTeamId }

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

                        Divider(
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

                // No active player state
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = themeAccent, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Auction Stage Empty", color = textMain, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Select a player from the roster to start bidding", color = textSub, fontSize = 13.sp)
                    }
                }
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
                                    text = if (isTeamLocked) "🔒 ${currentBiddingTeam?.name ?: "My Team"}" else (currentBiddingTeam?.name ?: "Select Team"),
                                    color = themeAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!isTeamLocked) {
                                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = themeAccent)
                                }
                            }
                        }

                        if (currentBiddingTeam != null) {
                            PurseProgressBar(
                                spent = currentBiddingTeam.spentPurse,
                                total = currentBiddingTeam.totalPurse,
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
                        val isBidEnabled = activeP != null && activeP.status != PlayerStatus.SOLD && currentBiddingTeam != null && isAuctionReady
                        val availablePurse = currentBiddingTeam?.remainingPurse ?: 0.0

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
                                        if (currentBiddingTeam != null) {
                                            viewModel.placeBid(currentBiddingTeam.id, 0.20)
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
                                        if (currentBiddingTeam != null) {
                                            viewModel.placeBid(currentBiddingTeam.id, 0.50)
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
                                        if (currentBiddingTeam != null) {
                                            viewModel.placeBid(currentBiddingTeam.id, 1.00)
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

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.sellActivePlayer() },
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
                                        if (currentBiddingTeam != null) {
                                            viewModel.sellActivePlayerAtBasePrice(currentBiddingTeam.id)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                                    modifier = Modifier.weight(1f),
                                    enabled = activePlayer != null && currentBiddingTeam != null && activePlayer?.status != PlayerStatus.SOLD
                                ) {
                                    Icon(imageVector = Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("SELL BASE PRICE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Button(
                                onClick = { viewModel.markActivePlayerUnsold() },
                                colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = activePlayer != null && activePlayer?.status != PlayerStatus.SOLD
                            ) {
                                Text("MARK UNSOLD", fontWeight = FontWeight.Bold)
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
    }

    // --- Team Switcher Dialog ---
    if (showTeamSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showTeamSelectorDialog = false },
            title = { Text("Select Bidding Team", color = textMain) },
            text = {
                Column {
                    teams.forEach { team ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBiddingTeamId = team.id
                                    showTeamSelectorDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = team.name, color = textMain, fontWeight = FontWeight.Bold)
                            Text(text = team.remainingPurse.formatIplCurrency(), color = themeAccent)
                        }
                        Divider(color = textSub.copy(alpha = 0.3f))
                    }
                }
            },
            confirmButton = {},
            containerColor = cardBg
        )
    }

    // --- Interactive Spin Wheel Dialog ---
    if (showSpinWheelModal) {
        val tourneyTeams = remember(teams, activeTournament) {
            teams.filter { it.tournamentId == activeTournament?.id }
        }
        SpinWheelModal(
            players = players,
            teams = tourneyTeams,
            isDarkTheme = isDarkTheme,
            onDismiss = { showSpinWheelModal = false },
            onPlayerSelected = { selectedPlayer ->
                viewModel.selectActivePlayer(selectedPlayer.id)
                showSpinWheelModal = false
            }
        )
    }
}
