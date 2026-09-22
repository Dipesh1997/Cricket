package com.cricket.auction.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.TeamEntity
import com.cricket.auction.data.model.TeamWithPlayers
import com.cricket.auction.data.model.TournamentEntity
import com.cricket.auction.ui.components.EmptyStateView
import com.cricket.auction.ui.components.TeamAvatar
import com.cricket.auction.ui.viewmodel.TournamentHubViewModel
import com.cricket.auction.util.AuctionUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    viewModel: TournamentHubViewModel
) {
    val tournament by viewModel.tournament.collectAsState()
    val teamsWithPlayers by viewModel.teamsWithPlayers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var teamToEdit by remember { mutableStateOf<TeamEntity?>(null) }
    var teamToDelete by remember { mutableStateOf<TeamEntity?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Team")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (teamsWithPlayers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Groups,
                    title = "No Teams Added",
                    message = "Every tournament starts fresh! Add the participating teams with custom names, short codes, branding colors, and owners.",
                    buttonText = "Add Your First Team",
                    onButtonClick = { showAddSheet = true },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Participating Teams (${teamsWithPlayers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Budget: ${AuctionUtils.formatAmount(tournament?.budgetPerTeam ?: 0L, tournament?.currencyFormat ?: CurrencyFormat.INR_CR_LAKH)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    items(teamsWithPlayers, key = { it.team.id }) { twp ->
                        TeamCard(
                            teamWithPlayers = twp,
                            tournament = tournament,
                            onEdit = { teamToEdit = twp.team },
                            onDelete = { teamToDelete = twp.team }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Sheet
    if (showAddSheet || teamToEdit != null) {
        TeamFormSheet(
            team = teamToEdit,
            tournament = tournament,
            onDismiss = {
                showAddSheet = false
                teamToEdit = null
            },
            onSave = { name, shortCode, colorHex, owner, purse ->
                if (teamToEdit != null) {
                    viewModel.updateTeam(
                        teamToEdit!!.copy(
                            name = name,
                            shortCode = shortCode,
                            colorHex = colorHex,
                            ownerName = owner,
                            totalPurse = purse ?: teamToEdit!!.totalPurse
                        )
                    ) {
                        teamToEdit = null
                        scope.launch { snackbarHostState.showSnackbar("Team updated successfully") }
                    }
                } else {
                    viewModel.addTeam(
                        name = name,
                        shortCode = shortCode,
                        colorHex = colorHex,
                        ownerName = owner,
                        customPurse = purse,
                        onSuccess = {
                            showAddSheet = false
                            scope.launch { snackbarHostState.showSnackbar("Team '$name' added") }
                        },
                        onError = { err ->
                            scope.launch { snackbarHostState.showSnackbar(err) }
                        }
                    )
                }
            }
        )
    }

    // Delete confirmation
    teamToDelete?.let { team ->
        AlertDialog(
            onDismissRequest = { teamToDelete = null },
            title = { Text("Delete Team?") },
            text = { Text("Delete '${team.name}'? Any players assigned to this team will be released back to the unassigned auction pool.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTeam(team.id)
                        teamToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { teamToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TeamCard(
    teamWithPlayers: TeamWithPlayers,
    tournament: TournamentEntity?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val team = teamWithPlayers.team
    val currency = tournament?.currencyFormat ?: CurrencyFormat.INR_CR_LAKH
    val maxSquad = tournament?.maxSquadSize ?: 25
    val maxOverseas = tournament?.maxOverseasPlayers ?: 8

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamAvatar(
                    shortCode = team.shortCode,
                    colorHex = team.colorHex,
                    size = 46.dp,
                    textSize = 15
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = team.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (team.ownerName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = team.ownerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Team",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Team",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Purse & Squad Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Remaining Purse",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = AuctionUtils.formatAmount(team.remainingPurse, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Squad Count",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${teamWithPlayers.squadCount} / $maxSquad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Role summary pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallStatPill("BAT", "${teamWithPlayers.batsmenCount}")
                SmallStatPill("BOWL", "${teamWithPlayers.bowlersCount}")
                SmallStatPill("AR", "${teamWithPlayers.allRoundersCount}")
                SmallStatPill("WK", "${teamWithPlayers.wicketKeepersCount}")
                SmallStatPill("OS", "${teamWithPlayers.overseasCount}/$maxOverseas")
            }
        }
    }
}

@Composable
fun SmallStatPill(label: String, count: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$label: ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Text(
                text = count,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamFormSheet(
    team: TeamEntity?,
    tournament: TournamentEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, shortCode: String, colorHex: String, owner: String, purse: Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(team?.name ?: "") }
    var shortCode by remember { mutableStateOf(team?.shortCode ?: "") }
    var ownerName by remember { mutableStateOf(team?.ownerName ?: "") }
    var selectedColor by remember { mutableStateOf(team?.colorHex ?: AuctionUtils.DEFAULT_TEAM_COLORS[0]) }

    val defaultPurseStr = remember(tournament) {
        if (tournament != null) {
            AuctionUtils.formatAmount(tournament.budgetPerTeam, tournament.currencyFormat)
        } else "100 Cr"
    }
    var purseInput by remember {
        mutableStateOf(
            if (team != null && tournament != null) AuctionUtils.formatAmount(team.totalPurse, tournament.currencyFormat)
            else defaultPurseStr
        )
    }

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
                text = if (team == null) "Add New Team" else "Edit Team",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Preview Avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamAvatar(
                    shortCode = shortCode.ifBlank { "T" },
                    colorHex = selectedColor,
                    size = 54.dp,
                    textSize = 18
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = name.ifBlank { "Team Name Preview" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Short Code: ${shortCode.ifBlank { "--" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    // Auto-generate short code if empty
                    if (shortCode.isEmpty() && it.isNotBlank()) {
                        val words = it.trim().split("\\s+".toRegex())
                        shortCode = if (words.size > 1) {
                            words.take(3).mapNotNull { w -> w.firstOrNull()?.uppercase() }.joinToString("")
                        } else {
                            it.take(3).uppercase()
                        }
                    }
                },
                label = { Text("Team Name *") },
                placeholder = { Text("e.g. Royal Strikers, Mumbai Mavericks") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = shortCode,
                onValueChange = { shortCode = it.take(4).uppercase() },
                label = { Text("Short Code / Initials (2-4 chars) *") },
                placeholder = { Text("e.g. RS, MM, CSK") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = { ownerName = it },
                label = { Text("Owner / Coach / Manager") },
                placeholder = { Text("e.g. John Doe, Management") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = purseInput,
                onValueChange = { purseInput = it },
                label = { Text("Team Purse Budget") },
                placeholder = { Text("e.g. 100 Cr, 80 Cr") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Color Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Team Color Branding",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AuctionUtils.DEFAULT_TEAM_COLORS.take(6).forEach { colorHex ->
                        ColorCircle(
                            colorHex = colorHex,
                            isSelected = selectedColor == colorHex,
                            onClick = { selectedColor = colorHex }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AuctionUtils.DEFAULT_TEAM_COLORS.drop(6).take(6).forEach { colorHex ->
                        ColorCircle(
                            colorHex = colorHex,
                            isSelected = selectedColor == colorHex,
                            onClick = { selectedColor = colorHex }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val purse = tournament?.let { AuctionUtils.parseAmount(purseInput, it.currencyFormat) }
                    onSave(name, shortCode, selectedColor, ownerName, purse)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = name.isNotBlank() && shortCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    if (team == null) "Add Team" else "Save Changes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ColorCircle(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        Color.Gray
    }

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
