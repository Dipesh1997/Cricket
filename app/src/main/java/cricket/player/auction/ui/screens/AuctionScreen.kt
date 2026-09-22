package cricket.player.auction.ui.screens

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
    val activePlayer by viewModel.activePlayer.collectAsState()
    val highestBidderTeam by viewModel.highestBidderTeam.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val bids by viewModel.bids.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val players by viewModel.players.collectAsState()

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
            .background(StadiumDark)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Status Bar & Spin Wheel Trigger ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.3f))
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
                            color = IplGold,
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
                            colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
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
            if (activePlayer != null) {
                val player = activePlayer!!

                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, IplGold)
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
                                StatChip(text = player.setName, backgroundColor = StadiumSurface, textColor = IplGold)

                                FilterChip(
                                    selected = player.isFixedSet,
                                    onClick = {
                                        if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) {
                                            viewModel.togglePlayerFixedSet(player.id, !player.isFixedSet)
                                        }
                                    },
                                    label = { Text(if (player.isFixedSet) "⭐ Fixed Set" else "+ Fix Set", fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IplGold,
                                        selectedLabelColor = Color.Black,
                                        containerColor = StadiumSurface,
                                        labelColor = Color.White
                                    )
                                )
                            }

                            if (player.isOverseas) {
                                StatChip(
                                    text = "Overseas (${player.country})",
                                    backgroundColor = NeonBlue.copy(alpha = 0.2f),
                                    textColor = NeonBlue,
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Public,
                                            contentDescription = null,
                                            tint = NeonBlue,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                )
                            } else {
                                StatChip(text = player.country, backgroundColor = StadiumSurface, textColor = Color.LightGray)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player Photo from Google Drive
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, IplGold, RoundedCornerShape(16.dp))
                        ) {
                            DriveImage(
                                url = player.imageUrl,
                                contentDescription = player.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Player Name & Role
                        Text(
                            text = player.name,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatChip(text = player.role.label, backgroundColor = IplGold.copy(alpha = 0.2f), textColor = IplGold)
                            StatChip(text = "Base: ${player.basePrice.formatIplCurrency()}", backgroundColor = StadiumSurface, textColor = Color.White)
                        }

                        if (player.stats.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = player.stats,
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        )

                        // --- Live Bid Dashboard ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (player.status == PlayerStatus.SOLD) "SOLD PRICE" else "CURRENT BID",
                                color = Color.Gray,
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
                                    color = if (player.status == PlayerStatus.SOLD) BidGreen else IplGold,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Highest Bidder Team Display
                            if (highestBidderTeam != null) {
                                TeamBadge(team = highestBidderTeam!!, showPurse = true)
                            } else {
                                Surface(
                                    color = StadiumSurface,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "No Bids Placed Yet",
                                        color = Color.LightGray,
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
                    colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = IplGold, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Auction Stage Empty", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Select a player from the roster to start bidding", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }

        // --- Bidding Controls Section ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
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
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Selector Button for Admin or Team Switcher
                        TextButton(onClick = { showTeamSelectorDialog = true }) {
                            Text(
                                text = currentBiddingTeam?.name ?: "Select Team",
                                color = IplGold,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = IplGold)
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
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val activeP = activePlayer
                    val isBidEnabled = activeP != null && activeP.status != PlayerStatus.SOLD && currentBiddingTeam != null
                    val availablePurse = currentBiddingTeam?.remainingPurse ?: 0.0

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

        // --- Admin Auctioneer Controls ---
        if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AUCTIONEER ADMIN CONTROLS",
                            color = NeonBlue,
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
                                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
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
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val activeBids = bids.filter { it.playerId == activePlayer?.id }
        if (activeBids.isEmpty()) {
            item {
                Text(
                    text = "No bids recorded for this player yet",
                    color = Color.DarkGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        } else {
            items(activeBids) { bid ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
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
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = bid.amount.formatIplCurrency(),
                            color = IplGold,
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
            title = { Text("Select Bidding Team", color = Color.White) },
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
                            Text(text = team.name, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(text = team.remainingPurse.formatIplCurrency(), color = IplGold)
                        }
                        Divider(color = Color.DarkGray)
                    }
                }
            },
            confirmButton = {},
            containerColor = StadiumCardDark
        )
    }

    // --- Interactive Spin Wheel Dialog ---
    if (showSpinWheelModal) {
        SpinWheelModal(
            players = players,
            onDismiss = { showSpinWheelModal = false },
            onPlayerSelected = { selectedPlayer ->
                viewModel.selectActivePlayer(selectedPlayer.id)
                showSpinWheelModal = false
            }
        )
    }
}
