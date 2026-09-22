package com.cricket.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.PlayerAuctionStatus
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.PlayerRole
import com.cricket.auction.data.model.TournamentEntity
import com.cricket.auction.ui.components.EmptyStateView
import com.cricket.auction.ui.components.OverseasBadge
import com.cricket.auction.ui.components.RoleBadge
import com.cricket.auction.ui.components.StatusBadge
import com.cricket.auction.ui.components.TeamAvatar
import com.cricket.auction.ui.viewmodel.TournamentHubViewModel
import com.cricket.auction.util.AuctionUtils
import kotlinx.coroutines.launch

enum class PlayerFilter {
    ALL,
    BATSMAN,
    BOWLER,
    ALL_ROUNDER,
    WICKET_KEEPER,
    OVERSEAS,
    UPCOMING,
    SOLD,
    UNSOLD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    viewModel: TournamentHubViewModel,
    onNavigateToAuction: () -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    val allPlayers by viewModel.allPlayers.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(PlayerFilter.ALL) }

    var showAddSheet by remember { mutableStateOf(false) }
    var showBatchSheet by remember { mutableStateOf(false) }
    var playerToEdit by remember { mutableStateOf<PlayerEntity?>(null) }
    var playerToDelete by remember { mutableStateOf<PlayerEntity?>(null) }

    val filteredPlayers = remember(allPlayers, searchQuery, selectedFilter) {
        allPlayers.filter { player ->
            val matchesQuery = searchQuery.isBlank() ||
                    player.name.contains(searchQuery, ignoreCase = true) ||
                    player.country.contains(searchQuery, ignoreCase = true) ||
                    player.tier.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                PlayerFilter.ALL -> true
                PlayerFilter.BATSMAN -> player.role == PlayerRole.BATSMAN
                PlayerFilter.BOWLER -> player.role == PlayerRole.BOWLER
                PlayerFilter.ALL_ROUNDER -> player.role == PlayerRole.ALL_ROUNDER
                PlayerFilter.WICKET_KEEPER -> player.role == PlayerRole.WICKET_KEEPER
                PlayerFilter.OVERSEAS -> player.isOverseas
                PlayerFilter.UPCOMING -> player.status == PlayerAuctionStatus.UPCOMING
                PlayerFilter.SOLD -> player.status == PlayerAuctionStatus.SOLD
                PlayerFilter.UNSOLD -> player.status == PlayerAuctionStatus.UNSOLD
            }

            matchesQuery && matchesFilter
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FloatingActionButton(
                    onClick = { showBatchSheet = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Batch Import")
                }
                FloatingActionButton(
                    onClick = { showAddSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Player")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search and quick action header
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, role, country, tier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter chips scrollable row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerFilter.values().forEach { filter ->
                        val count = when (filter) {
                            PlayerFilter.ALL -> allPlayers.size
                            PlayerFilter.BATSMAN -> allPlayers.count { it.role == PlayerRole.BATSMAN }
                            PlayerFilter.BOWLER -> allPlayers.count { it.role == PlayerRole.BOWLER }
                            PlayerFilter.ALL_ROUNDER -> allPlayers.count { it.role == PlayerRole.ALL_ROUNDER }
                            PlayerFilter.WICKET_KEEPER -> allPlayers.count { it.role == PlayerRole.WICKET_KEEPER }
                            PlayerFilter.OVERSEAS -> allPlayers.count { it.isOverseas }
                            PlayerFilter.UPCOMING -> allPlayers.count { it.status == PlayerAuctionStatus.UPCOMING }
                            PlayerFilter.SOLD -> allPlayers.count { it.status == PlayerAuctionStatus.SOLD }
                            PlayerFilter.UNSOLD -> allPlayers.count { it.status == PlayerAuctionStatus.UNSOLD }
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text("${filter.name.replace("_", " ")} ($count)") }
                        )
                    }
                }
            }

            if (allPlayers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Person,
                    title = "No Players in Auction Pool",
                    message = "Add players one by one or paste a complete list using Batch Import (supports CSV / line-by-line format).",
                    buttonText = "Add First Player",
                    onButtonClick = { showAddSheet = true },
                    modifier = Modifier.weight(1f)
                )
            } else if (filteredPlayers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No players match the current filter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPlayers, key = { it.id }) { player ->
                        val soldToTeam = teams.find { it.id == player.soldToTeamId }
                        PlayerCard(
                            player = player,
                            soldToTeam = soldToTeam,
                            tournament = tournament,
                            onEdit = { playerToEdit = player },
                            onDelete = { playerToDelete = player },
                            onAuctionNow = {
                                viewModel.selectPlayerForAuction(player)
                                onNavigateToAuction()
                            }
                        )
                    }
                }
            }
        }
    }

    // Single Player Form Sheet
    if (showAddSheet || playerToEdit != null) {
        PlayerFormSheet(
            player = playerToEdit,
            tournament = tournament,
            onDismiss = {
                showAddSheet = false
                playerToEdit = null
            },
            onSave = { name, role, batStyle, bowlStyle, isOverseas, country, basePrice, tier, notes ->
                if (playerToEdit != null) {
                    viewModel.updatePlayer(
                        playerToEdit!!.copy(
                            name = name,
                            role = role,
                            battingStyle = batStyle,
                            bowlingStyle = bowlStyle,
                            isOverseas = isOverseas,
                            country = country,
                            basePrice = basePrice,
                            tier = tier,
                            notes = notes
                        )
                    ) {
                        playerToEdit = null
                        scope.launch { snackbarHostState.showSnackbar("Player updated") }
                    }
                } else {
                    viewModel.addPlayer(
                        name = name,
                        role = role,
                        battingStyle = batStyle,
                        bowlingStyle = bowlStyle,
                        isOverseas = isOverseas,
                        country = country,
                        basePrice = basePrice,
                        tier = tier,
                        notes = notes,
                        onSuccess = {
                            showAddSheet = false
                            scope.launch { snackbarHostState.showSnackbar("Player '$name' added to auction") }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                }
            }
        )
    }

    // Batch Import Sheet
    if (showBatchSheet) {
        BatchImportSheet(
            tournament = tournament,
            onDismiss = { showBatchSheet = false },
            onImport = { rawText ->
                viewModel.addBatchPlayers(
                    rawText = rawText,
                    onSuccess = { count ->
                        showBatchSheet = false
                        scope.launch { snackbarHostState.showSnackbar("Successfully imported $count players!") }
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar(err) }
                    }
                )
            }
        )
    }

    // Delete confirmation
    playerToDelete?.let { player ->
        AlertDialog(
            onDismissRequest = { playerToDelete = null },
            title = { Text("Delete Player?") },
            text = { Text("Are you sure you want to remove '${player.name}' from the auction pool?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayer(player.id)
                        playerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PlayerCard(
    player: PlayerEntity,
    soldToTeam: com.cricket.auction.data.model.TeamEntity?,
    tournament: TournamentEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAuctionNow: () -> Unit
) {
    val currency = tournament?.currencyFormat ?: CurrencyFormat.INR_CR_LAKH

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    RoleBadge(role = player.role)
                    if (player.isOverseas) {
                        OverseasBadge()
                    }
                }

                StatusBadge(status = player.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Batting & Bowling Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏏 ${player.battingStyle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (player.bowlingStyle != "None") {
                    Text(
                        text = "⚾ ${player.bowlingStyle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (player.tier.isNotBlank() && player.tier != "Standard") {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = player.tier,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Base Price",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = AuctionUtils.formatAmount(player.basePrice, currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (player.status == PlayerAuctionStatus.SOLD && soldToTeam != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TeamAvatar(shortCode = soldToTeam.shortCode, colorHex = soldToTeam.colorHex, size = 28.dp, textSize = 10)
                        Column {
                            Text(
                                text = "Sold: ${AuctionUtils.formatAmount(player.soldPrice ?: 0L, currency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = soldToTeam.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (player.status == PlayerAuctionStatus.UPCOMING || player.status == PlayerAuctionStatus.UNSOLD) {
                        OutlinedButton(
                            onClick = onAuctionNow,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auction", fontSize = 12.sp)
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerFormSheet(
    player: PlayerEntity?,
    tournament: TournamentEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        role: PlayerRole,
        batStyle: String,
        bowlStyle: String,
        isOverseas: Boolean,
        country: String,
        basePrice: Long,
        tier: String,
        notes: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(player?.name ?: "") }
    var selectedRole by remember { mutableStateOf(player?.role ?: PlayerRole.BATSMAN) }
    var battingStyle by remember { mutableStateOf(player?.battingStyle ?: "Right Hand") }
    var bowlingStyle by remember { mutableStateOf(player?.bowlingStyle ?: "Right-arm Fast") }
    var isOverseas by remember { mutableStateOf(player?.isOverseas ?: false) }
    var country by remember { mutableStateOf(player?.country ?: "Domestic") }

    val defaultPriceStr = remember(tournament) {
        if (tournament != null) {
            AuctionUtils.formatAmount(tournament.defaultBasePrice, tournament.currencyFormat)
        } else "20 L"
    }
    var basePriceInput by remember {
        mutableStateOf(
            if (player != null && tournament != null) AuctionUtils.formatAmount(player.basePrice, tournament.currencyFormat)
            else defaultPriceStr
        )
    }
    var tier by remember { mutableStateOf(player?.tier ?: "Standard") }
    var notes by remember { mutableStateOf(player?.notes ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (player == null) "Add Player to Auction" else "Edit Player",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Player Name *") },
                placeholder = { Text("e.g. Virat Kohli, Ben Stokes") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Role selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Role *", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PlayerRole.values().forEach { role ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedRole == role) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedRole = role }
                        ) {
                            Text(
                                text = role.shortName,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontWeight = if (selectedRole == role) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedRole == role) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Batting Style
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Batting Style", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Right Hand", "Left Hand").forEach { style ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (battingStyle == style) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { battingStyle = style }
                        ) {
                            Text(
                                text = style,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontWeight = if (battingStyle == style) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Bowling Style
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bowling Style", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Right-arm Fast", "Right-arm Medium", "Right-arm Spin", "Left-arm Fast", "Left-arm Spin", "None").forEach { style ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (bowlingStyle == style) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { bowlingStyle = style }
                        ) {
                            Text(
                                text = style,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontWeight = if (bowlingStyle == style) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // Overseas toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Overseas Player?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Counts towards tournament overseas quota", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = isOverseas,
                    onCheckedChange = {
                        isOverseas = it
                        if (it && country == "Domestic") country = "Overseas"
                        else if (!it && country == "Overseas") country = "Domestic"
                    }
                )
            }

            if (isOverseas) {
                OutlinedTextField(
                    value = country,
                    onValueChange = { country = it },
                    label = { Text("Country / Nationality") },
                    placeholder = { Text("e.g. Australia, England, South Africa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = basePriceInput,
                onValueChange = { basePriceInput = it },
                label = { Text("Base Price *") },
                placeholder = { Text("e.g. 20 L, 50 L, 1 Cr, 2 Cr") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = tier,
                onValueChange = { tier = it },
                label = { Text("Auction Set / Tier") },
                placeholder = { Text("e.g. Marquee, Set 1, Capped, Uncapped") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Stats (Optional)") },
                placeholder = { Text("e.g. Captain material, Death bowler") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = {
                    val basePrice = tournament?.let { AuctionUtils.parseAmount(basePriceInput, it.currencyFormat) } ?: 20_00_000L
                    onSave(name, selectedRole, battingStyle, bowlingStyle, isOverseas, country, basePrice, tier, notes)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (player == null) "Add Player" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchImportSheet(
    tournament: TournamentEntity?,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var batchText by remember { mutableStateOf("") }

    val sampleText = """
        Virat Kohli, Batsman, 2 Cr
        Jasprit Bumrah, Bowler, 2 Cr
        Glenn Maxwell, All-Rounder, 2 Cr, Australia
        KL Rahul, Wicket Keeper, 2 Cr
        Rashid Khan, Bowler, 2 Cr, Afghanistan
        Shreyas Iyer, Batsman, 1.5 Cr
        Rinku Singh, Batsman, 50 L
        Mitchell Starc, Bowler, 2 Cr, Australia
    """.trimIndent()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Batch Import Players",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Paste your list of players below (one per line). Format: Name, Role (optional), Base Price (optional), Country/Overseas (optional).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = batchText,
                onValueChange = { batchText = it },
                label = { Text("Player List (One per line)") },
                placeholder = { Text(sampleText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                maxLines = 15
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { batchText = sampleText }) {
                    Text("Load Sample Players")
                }
                TextButton(onClick = { batchText = "" }) {
                    Text("Clear")
                }
            }

            Button(
                onClick = { onImport(batchText) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = batchText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Import All Players", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
