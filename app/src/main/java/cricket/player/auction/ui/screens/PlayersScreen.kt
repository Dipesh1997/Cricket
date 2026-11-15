package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import cricket.player.auction.model.*
import cricket.player.auction.ui.components.DriveImage
import cricket.player.auction.ui.components.PhotoUploaderField
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(viewModel: AuctionViewModel) {
    val context = LocalContext.current
    val players by viewModel.players.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val activeTournament by viewModel.activeTournament.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activePlayerId by viewModel.activePlayerId.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val isAdmin = currentUser?.role == UserRole.ADMIN_AUCTIONEER
    val isSpectator = currentUser?.role == UserRole.SPECTATOR

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterRole by remember { mutableStateOf<PlayerRole?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<PlayerStatus?>(null) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var deletingPlayer by remember { mutableStateOf<Player?>(null) }
    var editingPhotoPlayer by remember { mutableStateOf<Player?>(null) }

    val filteredPlayers = players.filter { player ->
        val matchesSearch = player.name.contains(searchQuery, ignoreCase = true) ||
                player.country.contains(searchQuery, ignoreCase = true)
        val matchesRole = selectedFilterRole == null || player.role == selectedFilterRole
        val matchesStatus = selectedStatusFilter == null || player.status == selectedStatusFilter
        matchesSearch && matchesRole && matchesStatus
    }

    Scaffold(
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showAddPlayerDialog = true },
                    containerColor = themeAccent,
                    contentColor = Color.Black
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Player")
                }
            }
        },
        containerColor = screenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "PLAYER ROSTER",
                color = themeAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Manage auction set list, player details & photos",
                color = textSub,
                fontSize = 12.sp
            )

            if (isSpectator) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = NeonBlue.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Spectator Mode: Player roster is in read-only view.",
                            color = NeonBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or country...", color = textSub) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = themeAccent) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themeAccent,
                    unfocusedBorderColor = if (isDarkTheme) Color.DarkGray else LightPitchContainer,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg,
                    focusedTextColor = textMain,
                    unfocusedTextColor = textMain
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Role Filters Ticker
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilterRole == null && selectedStatusFilter == null,
                        onClick = {
                            selectedFilterRole = null
                            selectedStatusFilter = null
                        },
                        label = { Text("All Players") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = themeAccent,
                            selectedLabelColor = Color.Black,
                            containerColor = surfaceBg,
                            labelColor = textMain
                        )
                    )
                }
                items(PlayerRole.values()) { role ->
                    FilterChip(
                        selected = selectedFilterRole == role,
                        onClick = {
                            selectedFilterRole = if (selectedFilterRole == role) null else role
                        },
                        label = { Text(role.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IplGold,
                            selectedLabelColor = Color.Black,
                            containerColor = StadiumSurface,
                            labelColor = Color.White
                        )
                    )
                }
                items(PlayerStatus.values()) { status ->
                    FilterChip(
                        selected = selectedStatusFilter == status,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == status) null else status
                        },
                        label = { Text(status.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonBlue,
                            selectedLabelColor = Color.Black,
                            containerColor = StadiumSurface,
                            labelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Player List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredPlayers) { player ->
                    val isActive = player.id == activePlayerId
                    val soldTeam = teams.find { it.id == player.soldToTeamId }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, themeAccent) else null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { if (!isSpectator) editingPhotoPlayer = player }
                            ) {
                                DriveImage(
                                    url = player.imageUrl,
                                    contentDescription = player.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = player.name,
                                    color = textMain,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${player.role.label} • ${player.country}",
                                    color = textSub,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StatChip(
                                        text = "Base: ${player.basePrice.formatIplCurrency()}",
                                        backgroundColor = surfaceBg,
                                        textColor = textSub
                                    )
                                    if (player.status == PlayerStatus.SOLD && soldTeam != null) {
                                        StatChip(
                                            text = "SOLD to ${soldTeam.shortCode}",
                                            backgroundColor = BidGreen.copy(alpha = 0.2f),
                                            textColor = BidGreen
                                        )
                                    } else if (player.status == PlayerStatus.UNSOLD) {
                                        StatChip(
                                            text = "UNSOLD",
                                            backgroundColor = UnsoldRed.copy(alpha = 0.2f),
                                            textColor = UnsoldRed
                                        )
                                    }

                                    if (isAdmin && player.status == PlayerStatus.UP_NEXT) {
                                        FilterChip(
                                            selected = player.isFixedSet,
                                            onClick = {
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
                                }
                            }

                            // Edit Player Details Icon Button
                            if (isAdmin) {
                                IconButton(onClick = { editingPlayer = player }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Player",
                                        tint = themeAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Delete Player Icon Button
                            if (isAdmin) {
                                IconButton(onClick = { deletingPlayer = player }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Player",
                                        tint = UnsoldRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Action Button: Set for Live Bidding
                            if (isAdmin && player.status != PlayerStatus.SOLD) {
                                IconButton(onClick = { viewModel.selectActivePlayer(player.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = "Bring to Stage",
                                        tint = if (isActive) themeAccent else textSub,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add New Player Dialog ---
    if (showAddPlayerDialog) {
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(PlayerRole.BATSMAN) }
        var country by remember { mutableStateOf("India") }
        var isOverseas by remember { mutableStateOf(false) }
        var basePriceStr by remember { mutableStateOf("2.0") }
        var driveUrl by remember { mutableStateOf("") }
        var stats by remember { mutableStateOf("") }
        var setName by remember { mutableStateOf("Set 1") }

        AlertDialog(
            onDismissRequest = { showAddPlayerDialog = false },
            title = { Text("Add New Player to Auction", color = Color.White) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Player Name") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isOverseas,
                            onCheckedChange = { isOverseas = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                        )
                        Text("Overseas Player", color = Color.White, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = basePriceStr,
                        onValueChange = { basePriceStr = it },
                        label = { Text("Base Price (in ₹ Crores e.g. 1.50)") },
                        singleLine = true
                    )

                    PhotoUploaderField(
                        imageUrl = driveUrl,
                        onImageUrlChange = { driveUrl = it },
                        label = "Player Photo"
                    )

                    OutlinedTextField(
                        value = stats,
                        onValueChange = { stats = it },
                        label = { Text("Career Stats / Details") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseP = basePriceStr.toDoubleOrNull() ?: 2.0
                        viewModel.addNewPlayer(
                            name = name,
                            role = role,
                            country = country,
                            isOverseas = isOverseas,
                            basePrice = baseP,
                            driveImageUrl = driveUrl,
                            stats = stats,
                            setName = setName
                        )
                        showAddPlayerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                    enabled = name.isNotBlank()
                ) {
                    Text("Add Player", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayerDialog = false }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Full Edit Player Dialog ---
    if (editingPlayer != null) {
        val target = editingPlayer!!
        var editName by remember(target) { mutableStateOf(target.name) }
        var editRole by remember(target) { mutableStateOf(target.role) }
        var editCountry by remember(target) { mutableStateOf(target.country) }
        var editIsOverseas by remember(target) { mutableStateOf(target.isOverseas) }
        var editBasePriceStr by remember(target) { mutableStateOf(target.basePrice.toString()) }
        var editSoldPriceStr by remember(target) { mutableStateOf((target.soldPrice ?: target.currentBid).toString()) }
        var editStatus by remember(target) { mutableStateOf(target.status) }
        var editPhotoUrl by remember(target) { mutableStateOf(target.imageUrl) }
        var editStats by remember(target) { mutableStateOf(target.stats) }

        AlertDialog(
            onDismissRequest = { editingPlayer = null },
            title = { Text("Edit Player Details", color = textMain, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Player Name") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editCountry,
                        onValueChange = { editCountry = it },
                        label = { Text("Country") },
                        singleLine = true
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = editIsOverseas,
                            onCheckedChange = { editIsOverseas = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonBlue)
                        )
                        Text("Overseas Player", color = textMain, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = editBasePriceStr,
                        onValueChange = { editBasePriceStr = it },
                        label = { Text("Base Price (in ₹ Cr)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editSoldPriceStr,
                        onValueChange = { editSoldPriceStr = it },
                        label = { Text("Current/Sold Price (in ₹ Cr)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editStats,
                        onValueChange = { editStats = it },
                        label = { Text("Stats & Career History") },
                        singleLine = true
                    )

                    PhotoUploaderField(
                        imageUrl = editPhotoUrl,
                        onImageUrlChange = { editPhotoUrl = it },
                        label = "Player Photo"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val baseP = editBasePriceStr.toDoubleOrNull() ?: target.basePrice
                        val soldP = editSoldPriceStr.toDoubleOrNull() ?: target.currentBid
                        val updated = target.copy(
                            name = editName,
                            role = editRole,
                            country = editCountry,
                            isOverseas = editIsOverseas,
                            basePrice = baseP,
                            currentBid = soldP,
                            soldPrice = if (editStatus == PlayerStatus.SOLD) soldP else target.soldPrice,
                            status = editStatus,
                            imageUrl = editPhotoUrl,
                            stats = editStats
                        )
                        viewModel.updatePlayer(updated)
                        editingPlayer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                    enabled = editName.isNotBlank()
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPlayer = null }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Delete Player Confirmation Dialog ---
    if (deletingPlayer != null) {
        val target = deletingPlayer!!
        AlertDialog(
            onDismissRequest = { deletingPlayer = null },
            title = { Text("Delete Player?", color = UnsoldRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${target.name}' from the tournament roster?", color = textSub)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlayer(target.id)
                        deletingPlayer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("DELETE PLAYER", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlayer = null }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Quick Photo Edit Modal ---
    if (editingPhotoPlayer != null) {
        var newPhotoUrl by remember(editingPhotoPlayer) { mutableStateOf(editingPhotoPlayer?.imageUrl ?: "") }

        AlertDialog(
            onDismissRequest = { editingPhotoPlayer = null },
            title = { Text("Update Photo for ${editingPhotoPlayer?.name}", color = textMain, fontWeight = FontWeight.Bold) },
            text = {
                PhotoUploaderField(
                    imageUrl = newPhotoUrl,
                    onImageUrlChange = { newPhotoUrl = it },
                    label = "Choose photo from gallery or URL"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingPhotoPlayer?.let { player ->
                            viewModel.updatePlayerPhoto(player.id, newPhotoUrl)
                        }
                        editingPhotoPlayer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black)
                ) {
                    Text("SAVE PHOTO", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPhotoPlayer = null }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }
}
