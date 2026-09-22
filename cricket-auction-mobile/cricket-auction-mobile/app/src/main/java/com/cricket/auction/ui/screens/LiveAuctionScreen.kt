package com.cricket.auction.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.TeamEntity
import com.cricket.auction.data.model.TeamWithPlayers
import com.cricket.auction.data.model.TournamentEntity
import com.cricket.auction.ui.components.EmptyStateView
import com.cricket.auction.ui.components.OverseasBadge
import com.cricket.auction.ui.components.RoleBadge
import com.cricket.auction.ui.components.TeamAvatar
import com.cricket.auction.ui.viewmodel.GavelStage
import com.cricket.auction.ui.viewmodel.TournamentHubViewModel
import com.cricket.auction.util.AuctionUtils
import kotlinx.coroutines.launch

@Composable
fun LiveAuctionScreen(
    viewModel: TournamentHubViewModel,
    onNavigateToPlayers: () -> Unit,
    onNavigateToTeams: () -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    val auctionState by viewModel.liveAuctionState.collectAsState()
    val teamsWithPlayers by viewModel.teamsWithPlayers.collectAsState()
    val allPlayers by viewModel.allPlayers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val currency = tournament?.currencyFormat ?: CurrencyFormat.INR_CR_LAKH
    val player = auctionState.currentPlayer

    var selectedIncrement by remember { mutableStateOf<Long?>(null) }
    var showCustomBidDialog by remember { mutableStateOf(false) }
    var customBidInput by remember { mutableStateOf("") }

    val suggestedIncrements = remember(auctionState.currentBid, currency) {
        AuctionUtils.getSuggestedIncrements(auctionState.currentBid, currency)
    }

    // Default next increment
    val currentIncrement = selectedIncrement ?: suggestedIncrements.firstOrNull() ?: 10_00_000L
    val nextBidAmount = if (auctionState.leadingTeam == null) {
        player?.basePrice ?: 0L
    } else {
        auctionState.currentBid + currentIncrement
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (tournament == null) {
                // Loading
            } else if (teamsWithPlayers.size < 2) {
                EmptyStateView(
                    icon = Icons.Default.Gavel,
                    title = "Teams Required for Auction",
                    message = "You need at least 2 teams before you can start the live auction. Add your participating teams now!",
                    buttonText = "Go to Teams",
                    onButtonClick = onNavigateToTeams,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (allPlayers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.SportsCricket,
                    title = "Auction Pool is Empty",
                    message = "Add players to your tournament pool to start bidding. You can add them one by one or import an entire squad at once!",
                    buttonText = "Add Players",
                    onButtonClick = onNavigateToPlayers,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (player == null) {
                // No active player (auction finished or all auctioned)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Auction Round Completed",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = auctionState.auctionMessage ?: "All upcoming players have been presented.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                viewModel.reAuctionUnsoldPlayers { count ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Accelerated Round: $count unsold players re-entered!")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Re-Auction Unsold")
                        }

                        OutlinedButton(onClick = { viewModel.loadRandomUpcomingPlayer() }) {
                            Icon(Icons.Default.Casino, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Random Pick")
                        }
                    }
                }
            } else {
                // Active Auction Stage
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Player Spotlight Card
                    PlayerSpotlightCard(
                        player = player,
                        currency = currency,
                        onNextRandom = { viewModel.loadRandomUpcomingPlayer() },
                        onNextSequential = { viewModel.loadNextUpcomingPlayer() }
                    )

                    // Current Bid & Leading Team Banner
                    BidHeroCard(
                        currentBid = auctionState.currentBid,
                        leadingTeam = auctionState.leadingTeam,
                        gavelStage = auctionState.gavelStage,
                        currency = currency
                    )

                    // Countdown Timer & Gavel Call Controls
                    AuctioneerGavelBar(
                        gavelStage = auctionState.gavelStage,
                        timerSeconds = auctionState.timerSeconds,
                        isTimerRunning = auctionState.isTimerRunning,
                        onAdvanceGavel = { viewModel.advanceGavel() },
                        onHammerSold = { viewModel.sellCurrentPlayer() },
                        onMarkUnsold = { viewModel.markCurrentPlayerUnsold() },
                        onPass = { viewModel.passCurrentPlayer() },
                        onToggleTimer = {
                            if (auctionState.isTimerRunning) viewModel.pauseTimer()
                            else viewModel.startTimer()
                        },
                        onAddSeconds = { viewModel.addTimerSeconds(5) },
                        onResetTimer = { viewModel.resetTimer(15) },
                        onUndoBid = { viewModel.undoLastBid() }
                    )

                    // Bid Increment Selector
                    if (auctionState.gavelStage != GavelStage.SOLD && auctionState.gavelStage != GavelStage.UNSOLD) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Bid Increment:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = { showCustomBidDialog = true },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Custom Bid Amount", fontSize = 12.sp)
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(suggestedIncrements) { inc ->
                                    val formatted = AuctionUtils.formatAmount(inc, currency)
                                    val isSelected = currentIncrement == inc
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                        modifier = Modifier.clickable { selectedIncrement = inc }
                                    ) {
                                        Text(
                                            text = "+$formatted",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Next Bid Amount Announcement
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (auctionState.leadingTeam == null) "Opening Bid:" else "Next Bid:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = AuctionUtils.formatAmount(nextBidAmount, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Team Bidding Paddles Header
                        Text(
                            text = "Tap a Team Paddle to Bid:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Team Paddle Grid
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            teamsWithPlayers.chunked(2).forEach { rowTeams ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowTeams.forEach { twp ->
                                        val isLeading = auctionState.leadingTeam?.id == twp.team.id
                                        val canAfford = twp.team.remainingPurse >= nextBidAmount
                                        val isSquadFull = tournament != null && twp.squadCount >= tournament!!.maxSquadSize
                                        val isOverseasFull = tournament != null && player.isOverseas && twp.overseasCount >= tournament!!.maxOverseasPlayers

                                        TeamPaddleCard(
                                            teamWithPlayers = twp,
                                            isLeading = isLeading,
                                            canAfford = canAfford,
                                            isSquadFull = isSquadFull,
                                            isOverseasFull = isOverseasFull,
                                            currency = currency,
                                            modifier = Modifier.weight(1f),
                                            onBid = {
                                                viewModel.placeBid(
                                                    team = twp.team,
                                                    newBidAmount = nextBidAmount,
                                                    onError = { err ->
                                                        scope.launch { snackbarHostState.showSnackbar(err) }
                                                    }
                                                )
                                            }
                                        )
                                    }
                                    if (rowTeams.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Bid History Log
                    if (auctionState.bidHistory.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bidding Log (${auctionState.bidHistory.size} bids)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            auctionState.bidHistory.take(5).forEachIndexed { index, bid ->
                                val bidTeam = teamsWithPlayers.find { it.team.id == bid.teamId }?.team
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            TeamAvatar(
                                                shortCode = bidTeam?.shortCode ?: "--",
                                                colorHex = bidTeam?.colorHex ?: "#000000",
                                                size = 24.dp,
                                                textSize = 9
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bidTeam?.name ?: "Unknown Team",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                        Text(
                                            text = AuctionUtils.formatAmount(bid.bidAmount, currency),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

    // Custom Bid Dialog
    if (showCustomBidDialog) {
        AlertDialog(
            onDismissRequest = { showCustomBidDialog = false },
            title = { Text("Enter Custom Bid") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter a custom bid amount for the current player:")
                    OutlinedTextField(
                        value = customBidInput,
                        onValueChange = { customBidInput = it },
                        label = { Text("Bid Amount") },
                        placeholder = { Text("e.g. 15.5 Cr, 80 L, 2500") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = AuctionUtils.parseAmount(customBidInput, currency)
                        if (parsed != null && parsed > auctionState.currentBid) {
                            selectedIncrement = parsed - auctionState.currentBid
                            showCustomBidDialog = false
                            customBidInput = ""
                        } else {
                            scope.launch { snackbarHostState.showSnackbar("Bid must be greater than current bid") }
                        }
                    }
                ) {
                    Text("Set Bid")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomBidDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PlayerSpotlightCard(
    player: PlayerEntity,
    currency: CurrencyFormat,
    onNextRandom: () -> Unit,
    onNextSequential: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoleBadge(role = player.role)
                        if (player.isOverseas) {
                            OverseasBadge()
                        }
                        if (player.tier.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = player.tier,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Skip / Next Controls
                Row {
                    IconButton(onClick = onNextRandom) {
                        Icon(Icons.Default.Casino, contentDescription = "Random Player", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onNextSequential) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next Player", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Player Specs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🏏 ${player.battingStyle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (player.bowlingStyle != "None") {
                    Text(
                        text = "⚾ ${player.bowlingStyle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (player.country.isNotBlank()) {
                    Text(
                        text = "🌍 ${player.country}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Base Price:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AuctionUtils.formatAmount(player.basePrice, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun BidHeroCard(
    currentBid: Long,
    leadingTeam: TeamEntity?,
    gavelStage: GavelStage,
    currency: CurrencyFormat
) {
    val bgColor by animateColorAsState(
        targetValue = when (gavelStage) {
            GavelStage.SOLD -> Color(0xFF1B5E20)
            GavelStage.UNSOLD -> Color(0xFFB71C1C)
            GavelStage.GOING_ONCE -> Color(0xFFE65100)
            GavelStage.GOING_TWICE -> Color(0xFFC62828)
            GavelStage.BIDDING -> MaterialTheme.colorScheme.primaryContainer
        },
        animationSpec = tween(400)
    )

    val textColor = when (gavelStage) {
        GavelStage.BIDDING -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> Color.White
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (gavelStage) {
                    GavelStage.SOLD -> "🎉 SOLD TO"
                    GavelStage.UNSOLD -> "❌ UNSOLD"
                    GavelStage.GOING_ONCE -> "GOING ONCE..."
                    GavelStage.GOING_TWICE -> "GOING TWICE... FINAL CALL!"
                    GavelStage.BIDDING -> if (leadingTeam == null) "BASE PRICE" else "CURRENT HIGHEST BID"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = textColor.copy(alpha = 0.85f),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = AuctionUtils.formatAmount(currentBid, currency),
                style = MaterialTheme.typography.headlineLarge,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (leadingTeam != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeamAvatar(
                        shortCode = leadingTeam.shortCode,
                        colorHex = leadingTeam.colorHex,
                        size = 32.dp,
                        textSize = 11
                    )
                    Text(
                        text = leadingTeam.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            } else if (gavelStage != GavelStage.UNSOLD) {
                Text(
                    text = "Awaiting first bid...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AuctioneerGavelBar(
    gavelStage: GavelStage,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    onAdvanceGavel: () -> Unit,
    onHammerSold: () -> Unit,
    onMarkUnsold: () -> Unit,
    onPass: () -> Unit,
    onToggleTimer: () -> Unit,
    onAddSeconds: () -> Unit,
    onResetTimer: () -> Unit,
    onUndoBid: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Timer Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = if (timerSeconds <= 5 && isTimerRunning) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${timerSeconds}s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (timerSeconds <= 5 && isTimerRunning) Color.Red else MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(onClick = onToggleTimer) {
                        Icon(
                            imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Timer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onAddSeconds) {
                        Text("+5s")
                    }
                    IconButton(onClick = onResetTimer) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Timer", tint = MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onUndoBid) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo Last Bid", tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { (timerSeconds.toFloat() / 15f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (timerSeconds <= 5) Color.Red else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            if (gavelStage == GavelStage.SOLD || gavelStage == GavelStage.UNSOLD) {
                Button(
                    onClick = onPass,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auction Next Player", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Advance Gavel Button
                    Button(
                        onClick = onAdvanceGavel,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (gavelStage) {
                                GavelStage.BIDDING -> MaterialTheme.colorScheme.secondary
                                GavelStage.GOING_ONCE -> Color(0xFFE65100)
                                GavelStage.GOING_TWICE -> Color(0xFFC62828)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (gavelStage) {
                                GavelStage.BIDDING -> "Going Once"
                                GavelStage.GOING_ONCE -> "Going Twice"
                                GavelStage.GOING_TWICE -> "Hammer Final"
                                else -> "Next"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Hammer Instant Sold Button
                    Button(
                        onClick = onHammerSold,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Text("SOLD!", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Mark Unsold Button
                    OutlinedButton(
                        onClick = onMarkUnsold,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Text("UNSOLD", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TeamPaddleCard(
    teamWithPlayers: TeamWithPlayers,
    isLeading: Boolean,
    canAfford: Boolean,
    isSquadFull: Boolean,
    isOverseasFull: Boolean,
    currency: CurrencyFormat,
    modifier: Modifier = Modifier,
    onBid: () -> Unit
) {
    val team = teamWithPlayers.team
    val isEligible = canAfford && !isSquadFull && !isOverseasFull

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = isEligible, onClick = onBid),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLeading) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isLeading) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else if (!isEligible) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isLeading) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TeamAvatar(
                        shortCode = team.shortCode,
                        colorHex = team.colorHex,
                        size = 32.dp,
                        textSize = 11
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = if (isEligible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                if (isLeading) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Purse: ${AuctionUtils.formatAmount(team.remainingPurse, currency)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (canAfford) MaterialTheme.colorScheme.primary else Color.Red
            )

            Text(
                text = "Squad: ${teamWithPlayers.squadCount} | OS: ${teamWithPlayers.overseasCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            if (!isEligible) {
                Spacer(modifier = Modifier.height(4.dp))
                val reason = when {
                    !canAfford -> "Low Purse"
                    isSquadFull -> "Squad Full"
                    isOverseasFull -> "OS Quota Full"
                    else -> "Ineligible"
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Red.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
