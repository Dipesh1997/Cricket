package com.cricket.auction.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.TournamentEntity
import com.cricket.auction.ui.components.EmptyStateView
import com.cricket.auction.ui.viewmodel.TournamentViewModel
import com.cricket.auction.util.AuctionUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentListScreen(
    viewModel: TournamentViewModel,
    onTournamentClick: (Long) -> Unit
) {
    val tournaments by viewModel.tournaments.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateSheet by remember { mutableStateOf(false) }
    var tournamentToDelete by remember { mutableStateOf<TournamentEntity?>(null) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsCricket,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Cricket Auction",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Live Auction & Squad Builder",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Tournament")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (tournaments.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.EmojiEvents,
                    title = "No Tournaments Yet",
                    message = "Start with a clean slate! Create your tournament, customize the purse budget, add your teams, and build your player auction pool.",
                    buttonText = "Create New Tournament",
                    onButtonClick = { showCreateSheet = true },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Your Tournaments (${tournaments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    items(tournaments, key = { it.id }) { tournament ->
                        TournamentCard(
                            tournament = tournament,
                            onClick = { onTournamentClick(tournament.id) },
                            onDelete = { tournamentToDelete = tournament }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    tournamentToDelete?.let { tournament ->
        AlertDialog(
            onDismissRequest = { tournamentToDelete = null },
            title = { Text("Delete Tournament?") },
            text = {
                Text("Are you sure you want to delete '${tournament.name}'? All associated teams, players, and auction bids will also be permanently removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTournament(tournament.id)
                        tournamentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tournamentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Tournament Bottom Sheet
    if (showCreateSheet) {
        CreateTournamentSheet(
            onDismiss = { showCreateSheet = false },
            onCreate = { name, edition, budget, currency, minSquad, maxSquad, maxOverseas, defaultBase ->
                viewModel.createTournament(
                    name = name,
                    edition = edition,
                    budgetPerTeam = budget,
                    currencyFormat = currency,
                    minSquadSize = minSquad,
                    maxSquadSize = maxSquad,
                    maxOverseasPlayers = maxOverseas,
                    defaultBasePrice = defaultBase
                ) { newId ->
                    showCreateSheet = false
                    onTournamentClick(newId)
                }
            }
        )
    }
}

@Composable
fun TournamentCard(
    tournament: TournamentEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(tournament.createdAt) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(tournament.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (tournament.edition.isNotBlank()) {
                        Text(
                            text = tournament.edition,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Tournament",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InfoChip(
                    label = "Purse",
                    value = AuctionUtils.formatAmount(tournament.budgetPerTeam, tournament.currencyFormat)
                )
                InfoChip(
                    label = "Squad",
                    value = "${tournament.minSquadSize}-${tournament.maxSquadSize}"
                )
                InfoChip(
                    label = "Overseas Max",
                    value = "${tournament.maxOverseasPlayers}"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Created $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Enter Hub →",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTournamentSheet(
    onDismiss: () -> Unit,
    onCreate: (
        name: String,
        edition: String,
        budget: Long,
        currency: CurrencyFormat,
        minSquad: Int,
        maxSquad: Int,
        maxOverseas: Int,
        defaultBase: Long
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var edition by remember { mutableStateOf("2026") }
    var budgetInput by remember { mutableStateOf("100 Cr") }
    var currencyFormat by remember { mutableStateOf(CurrencyFormat.INR_CR_LAKH) }
    var minSquadSizeInput by remember { mutableStateOf("15") }
    var maxSquadSizeInput by remember { mutableStateOf("25") }
    var maxOverseasInput by remember { mutableStateOf("8") }
    var defaultBasePriceInput by remember { mutableStateOf("20 L") }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Create Auction Tournament",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Set up your tournament rules, purse budget, and squad limits. Everything starts completely empty so you can add your custom teams and players.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tournament Name *") },
                placeholder = { Text("e.g. Premier League 2026, Club Cup") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = edition,
                onValueChange = { edition = it },
                label = { Text("Edition / Season") },
                placeholder = { Text("e.g. 2026, Season 1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Budget presets & input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Team Purse Budget *") },
                    placeholder = { Text("e.g. 100 Cr, 50 Cr, 1000 Pts") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("50 Cr", "80 Cr", "100 Cr", "120 Cr").forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (budgetInput == preset) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { budgetInput = preset }
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp),
                                fontWeight = if (budgetInput == preset) FontWeight.Bold else FontWeight.Normal,
                                color = if (budgetInput == preset) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Currency format selector
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Currency Format",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        CurrencyFormat.INR_CR_LAKH to "₹ Lakhs / Crores",
                        CurrencyFormat.POINTS to "Points (Pts)",
                        CurrencyFormat.USD to "USD ($)"
                    ).forEach { (format, label) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (currencyFormat == format) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currencyFormat = format }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                                fontWeight = if (currencyFormat == format) FontWeight.Bold else FontWeight.Normal,
                                color = if (currencyFormat == format) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Squad sizes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minSquadSizeInput,
                    onValueChange = { minSquadSizeInput = it },
                    label = { Text("Min Squad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxSquadSizeInput,
                    onValueChange = { maxSquadSizeInput = it },
                    label = { Text("Max Squad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = maxOverseasInput,
                    onValueChange = { maxOverseasInput = it },
                    label = { Text("Overseas Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            OutlinedTextField(
                value = defaultBasePriceInput,
                onValueChange = { defaultBasePriceInput = it },
                label = { Text("Default Base Price") },
                placeholder = { Text("e.g. 20 L, 50 L, 1 Cr") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val budget = AuctionUtils.parseAmount(budgetInput, currencyFormat) ?: 100_00_00_000L
                    val defaultBase = AuctionUtils.parseAmount(defaultBasePriceInput, currencyFormat) ?: 20_00_000L
                    val minSquad = minSquadSizeInput.toIntOrNull() ?: 15
                    val maxSquad = maxSquadSizeInput.toIntOrNull() ?: 25
                    val maxOverseas = maxOverseasInput.toIntOrNull() ?: 8

                    onCreate(
                        name,
                        edition,
                        budget,
                        currencyFormat,
                        minSquad,
                        maxSquad,
                        maxOverseas,
                        defaultBase
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = name.isNotBlank()
            ) {
                Text(
                    "Create Tournament",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
