package cricket.player.auction.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.Tournament
import cricket.player.auction.model.formatIplCurrency
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentScreen(
    viewModel: AuctionViewModel,
    onTournamentClick: (Tournament) -> Unit,
    exportTrigger: Int = 0,
    importTrigger: Int = 0
) {
    val context = LocalContext.current
    val tournaments by viewModel.tournaments.collectAsState()
    val activeTournamentId by viewModel.activeTournamentId.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val players by viewModel.players.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTournament by remember { mutableStateOf<Tournament?>(null) }
    var deletingTournament by remember { mutableStateOf<Tournament?>(null) }

    // Export Backup File Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.exportDatabaseJson()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }
                Toast.makeText(context, "Database exported & saved to file!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Import Backup File Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
                if (json != null && viewModel.importDatabaseJson(json)) {
                    Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Invalid JSON database backup file", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to import: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(exportTrigger) {
        if (exportTrigger > 0) {
            val fileName = "cricket_backup_${System.currentTimeMillis()}.json"
            exportLauncher.launch(fileName)
        }
    }

    LaunchedEffect(importTrigger) {
        if (importTrigger > 0) {
            importLauncher.launch("*/*")
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = IplGold,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "New Tournament")
            }
        },
        containerColor = StadiumDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary
            val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CRICKET TOURNAMENT HUB",
                        color = themeAccent,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Click a tournament card to enter its workspace dashboard",
                        color = if (isDarkTheme) Color.Gray else LightPitchText.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            // --- Home Screen Light Theme Toggle Card ---
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) StadiumCardDark else LightPitchSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (isDarkTheme) StadiumSurface else LightPitchContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = themeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (isDarkTheme) "Dark Stadium Mode" else "Light Green Grass Mode 🌿",
                                color = if (isDarkTheme) Color.White else LightPitchText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Use fresh light green pitch colors across app",
                                color = if (isDarkTheme) Color.Gray else LightPitchSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = !isDarkTheme,
                        onCheckedChange = { viewModel.toggleTheme() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LightPitchPrimary,
                            checkedTrackColor = LightPitchContainer,
                            uncheckedThumbColor = IplGold,
                            uncheckedTrackColor = StadiumSurface
                        )
                    )
                }
            }

            if (tournaments.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = IplGold, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "No Tournaments Created Yet", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Click '+ New Tournament' to create your first tournament", color = Color.Gray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black)
                        ) {
                            Text("CREATE FIRST TOURNAMENT", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            } else {
                Text(
                    text = "SELECT TOURNAMENT WORKSPACE",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(tournaments) { tourney ->
                        val isActive = tourney.id == activeTournamentId

                        Card(
                            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectTournament(tourney.id)
                                    onTournamentClick(tourney)
                                },
                            border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, IplGold) else null
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = IplGold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = tourney.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isActive) {
                                            StatChip(text = "ACTIVE", backgroundColor = BidGreen.copy(alpha = 0.2f), textColor = BidGreen)
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        // Edit Button
                                        IconButton(onClick = { editingTournament = tourney }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Tournament", tint = IplGold, modifier = Modifier.size(18.dp))
                                        }

                                        // Delete Button
                                        IconButton(onClick = { deletingTournament = tourney }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Tournament", tint = UnsoldRed, modifier = Modifier.size(18.dp))
                                        }

                                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    StatChip(text = "Purse: ${tourney.defaultPurse.formatIplCurrency()}", backgroundColor = StadiumSurface, textColor = Color.LightGray)
                                    StatChip(text = "Teams: ${teams.count { it.tournamentId == tourney.id }}", backgroundColor = StadiumSurface, textColor = NeonBlue)
                                    StatChip(text = "Players: ${players.count { it.tournamentId == tourney.id }}", backgroundColor = StadiumSurface, textColor = IplGold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create Tournament Dialog ---
    if (showCreateDialog) {
        var tourneyName by remember { mutableStateOf("") }
        var purseStr by remember { mutableStateOf("100.0") }
        var maxSlotsStr by remember { mutableStateOf("25") }
        var maxOverseasStr by remember { mutableStateOf("8") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Tournament", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tourneyName,
                        onValueChange = { tourneyName = it },
                        label = { Text("Tournament Name") },
                        placeholder = { Text("e.g. IPL 2026 Mega Auction") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = purseStr,
                        onValueChange = { purseStr = it },
                        label = { Text("Team Purse Budget (in ₹ Cr)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxSlotsStr,
                        onValueChange = { maxSlotsStr = it },
                        label = { Text("Max Squad Slots per Team") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = maxOverseasStr,
                        onValueChange = { maxOverseasStr = it },
                        label = { Text("Max Overseas Slots per Team") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val purse = purseStr.toDoubleOrNull() ?: 100.0
                        val slots = maxSlotsStr.toIntOrNull() ?: 25
                        val overseas = maxOverseasStr.toIntOrNull() ?: 8

                        val tourney = viewModel.createTournament(tourneyName, purse, slots, overseas)
                        showCreateDialog = false
                        onTournamentClick(tourney)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = tourneyName.isNotBlank()
                ) {
                    Text("CREATE & ENTER DASHBOARD", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
        )
    }

    // --- Edit Tournament Dialog ---
    if (editingTournament != null) {
        val target = editingTournament!!
        var editName by remember(target) { mutableStateOf(target.name) }
        var editPurseStr by remember(target) { mutableStateOf(target.defaultPurse.toString()) }
        var editSlotsStr by remember(target) { mutableStateOf(target.maxSlots.toString()) }
        var editOverseasStr by remember(target) { mutableStateOf(target.maxOverseas.toString()) }

        AlertDialog(
            onDismissRequest = { editingTournament = null },
            title = { Text("Edit Tournament", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Tournament Name") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editPurseStr,
                        onValueChange = { editPurseStr = it },
                        label = { Text("Team Purse Budget (in ₹ Cr)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editSlotsStr,
                        onValueChange = { editSlotsStr = it },
                        label = { Text("Max Squad Slots per Team") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editOverseasStr,
                        onValueChange = { editOverseasStr = it },
                        label = { Text("Max Overseas Slots per Team") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val purse = editPurseStr.toDoubleOrNull() ?: target.defaultPurse
                        val slots = editSlotsStr.toIntOrNull() ?: target.maxSlots
                        val overseas = editOverseasStr.toIntOrNull() ?: target.maxOverseas

                        val updated = target.copy(
                            name = editName,
                            defaultPurse = purse,
                            maxSlots = slots,
                            maxOverseas = overseas
                        )
                        viewModel.updateTournament(updated)
                        editingTournament = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = editName.isNotBlank()
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTournament = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
        )
    }

    // --- Delete Confirmation Dialog ---
    if (deletingTournament != null) {
        val target = deletingTournament!!
        AlertDialog(
            onDismissRequest = { deletingTournament = null },
            title = { Text("Delete Tournament?", color = UnsoldRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${target.name}' and all associated teams and players?", color = Color.LightGray)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTournament(target.id)
                        deletingTournament = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("DELETE TOURNAMENT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTournament = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
        )
    }
}
