package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.automirrored.filled.PlaylistAddCheck
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import cricket.player.auction.model.*
import cricket.player.auction.ui.components.*
import cricket.player.auction.ui.components.FixedSetPlayerSelectorDialog
import cricket.player.auction.ui.components.PhotoUploaderField
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.components.getPlayerPlaceholderIcon
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
    var showNeedTeamsDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }
    var deletingPlayer by remember { mutableStateOf<Player?>(null) }
    var editingPhotoPlayer by remember { mutableStateOf<Player?>(null) }
    var showFixedSetSelectorModal by remember { mutableStateOf(false) }

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
                    onClick = {
                        if (teams.size < 2) {
                            showNeedTeamsDialog = true
                        } else {
                            showAddPlayerDialog = true
                        }
                    },
                    containerColor = themeAccent,
                    contentColor = Color.Black
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Player")
                }
            }
        },
        containerColor = screenBg
    ) { innerPadding ->
        val emptyScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (filteredPlayers.isEmpty()) {
                        Modifier
                            .verticalScroll(emptyScrollState)
                            .padding(16.dp)
                            .padding(bottom = 120.dp)
                    } else {
                        Modifier.padding(16.dp)
                    }
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                }
                if (isAdmin) {
                    val fixedCount = players.count { it.status == PlayerStatus.UP_NEXT && it.isFixedSet }
                    Surface(
                        onClick = { showFixedSetSelectorModal = true },
                        color = surfaceBg,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.PlaylistAddCheck, contentDescription = null, tint = themeAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fixed Set ($fixedCount) 📝", color = textMain, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            if (isSpectator) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = if (isDarkTheme) NeonBlue.copy(alpha = 0.15f) else LightPitchSurface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, (if (isDarkTheme) NeonBlue else LightPitchPrimary).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = if (isDarkTheme) NeonBlue else LightPitchPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Spectator Mode: Player roster is in read-only view.",
                            color = textMain,
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
                            selectedLabelColor = if (isDarkTheme) Color.Black else Color.White,
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
                            selectedContainerColor = themeAccent,
                            selectedLabelColor = if (isDarkTheme) Color.Black else Color.White,
                            containerColor = surfaceBg,
                            labelColor = textMain
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
                            selectedContainerColor = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                            selectedLabelColor = if (isDarkTheme) Color.Black else Color.White,
                            containerColor = surfaceBg,
                            labelColor = textMain
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredPlayers.isEmpty()) {
                if (players.isEmpty()) {
                    val needsTeamsFirst = teams.size < 2
                    EmptyStateGuideCard(
                        mainIcon = if (needsTeamsFirst) Icons.Default.Shield else Icons.Default.Person,
                        badgeText = if (needsTeamsFirst) "STEP 1: CREATE 2 TEAMS FIRST" else "PLAYER ROSTER POOL",
                        title = if (needsTeamsFirst) "Create 2 Teams First 🛡️" else "Player Pool is Empty 🏏",
                        description = if (needsTeamsFirst) {
                            "You must create at least 2 franchise teams in the Teams tab before you can add players to the tournament roster pool."
                        } else {
                            "Add players with roles (Batsman, Bowler, All-Rounder, Wicket Keeper), photos, and base prices to draft them in auction."
                        },
                        steps = if (needsTeamsFirst) {
                            listOf(
                                GuideStep(
                                    stepNumber = 1,
                                    title = "Create Team Franchises",
                                    description = "Head over to the Teams tab and create at least 2 teams (e.g. MI, CSK).",
                                    icon = Icons.Default.Shield
                                ),
                                GuideStep(
                                    stepNumber = 2,
                                    title = "Return to Add Players",
                                    description = "Once 2 teams are registered, you can add players and assign them to sets.",
                                    icon = Icons.Default.PersonAdd
                                ),
                                GuideStep(
                                    stepNumber = 3,
                                    title = "Conduct Live Auction",
                                    description = "Start bidding or spin the wheel to build your dream franchise squads!",
                                    icon = Icons.Default.Gavel
                                )
                            )
                        } else {
                            listOf(
                                GuideStep(
                                    stepNumber = 1,
                                    title = "Add Players or Import Roster",
                                    description = "Tap '+ Add Player' to input name, role, country, and starting base price.",
                                    icon = Icons.Default.PersonAdd
                                ),
                                GuideStep(
                                    stepNumber = 2,
                                    title = "Organize Sets & Fixed Sets",
                                    description = "Group into Set 1 (Marquee), Set 2, or mark as Fixed Sets for the spin wheel.",
                                    icon = Icons.AutoMirrored.Filled.PlaylistAddCheck
                                ),
                                GuideStep(
                                    stepNumber = 3,
                                    title = "Draft into Auction Stage",
                                    description = "Select any player from the roster or spin the wheel in Auction Arena to start live bidding.",
                                    icon = Icons.Default.Gavel
                                )
                            )
                        },
                        primaryButtonText = if (needsTeamsFirst) {
                            "CREATE 2 TEAMS FIRST (${teams.size}/2) 🛡️"
                        } else if (!isSpectator) {
                            "+ ADD FIRST PLAYER 👤"
                        } else null,
                        onPrimaryClick = if (needsTeamsFirst) {
                            { showNeedTeamsDialog = true }
                        } else if (!isSpectator) {
                            { showAddPlayerDialog = true }
                        } else null,
                        isDarkTheme = isDarkTheme,
                        accentColor = themeAccent
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = textSub, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "No players match '$searchQuery'", color = textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Try clearing your search query or role filter", color = textSub, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedStatusFilter = null
                                    selectedFilterRole = null
                                },
                                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent)
                            ) {
                                Text("CLEAR FILTERS", color = themeAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
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
                                    modifier = Modifier.fillMaxSize(),
                                    fallbackIcon = getPlayerPlaceholderIcon(player.id, player.name)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = player.name,
                                    color = textMain,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${player.role.label} • ${player.country}",
                                    color = textSub,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
}

    // --- Need At Least 2 Teams Dialog ---
    if (showNeedTeamsDialog) {
        AlertDialog(
            onDismissRequest = { showNeedTeamsDialog = false },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = themeAccent, modifier = Modifier.size(36.dp)) },
            title = { Text("Create Teams First 🛡️", color = textMain, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "A tournament requires at least 2 teams before adding players to the roster pool.\n\nPlease go to the Teams tab to create 2 teams first, then return here to add players.",
                    color = textSub,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showNeedTeamsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White)
                ) {
                    Text("OK, GOT IT", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = cardBg
        )
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

        var roleDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddPlayerDialog = false },
            title = { Text("Add New Player to Auction", color = textMain, fontWeight = FontWeight.Bold) },
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

                    // Role Dropdown (Batsman, Bowler, All-Rounder, Wicket Keeper)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = role.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Player Role / Discipline") },
                            trailingIcon = {
                                IconButton(onClick = { roleDropdownExpanded = !roleDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (roleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Role",
                                        tint = themeAccent
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { roleDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = roleDropdownExpanded,
                            onDismissRequest = { roleDropdownExpanded = false },
                            modifier = Modifier
                                .background(cardBg)
                                .border(1.dp, themeAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            listOf(
                                PlayerRole.BATSMAN to "🏏 Batsman",
                                PlayerRole.BOWLER to "🎯 Bowler",
                                PlayerRole.ALL_ROUNDER to "⚡ All-Rounder",
                                PlayerRole.WICKET_KEEPER to "🧤 Wicket Keeper"
                            ).forEach { (r, labelWithIcon) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = labelWithIcon,
                                            color = if (role == r) themeAccent else textMain,
                                            fontWeight = if (role == r) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        role = r
                                        roleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

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
                            colors = CheckboxDefaults.colors(checkedColor = if (isDarkTheme) NeonBlue else LightPitchPrimary)
                        )
                        Text("Overseas Player", color = textMain, fontSize = 13.sp)
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
                        label = "Player Photo",
                        placeholderIcon = getPlayerPlaceholderIcon(name)
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
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White),
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
        var editRoleDropdownExpanded by remember { mutableStateOf(false) }
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

                    // Role Dropdown for editing
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = editRole.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Player Role / Discipline") },
                            trailingIcon = {
                                IconButton(onClick = { editRoleDropdownExpanded = !editRoleDropdownExpanded }) {
                                    Icon(
                                        imageVector = if (editRoleDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Role",
                                        tint = themeAccent
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editRoleDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = editRoleDropdownExpanded,
                            onDismissRequest = { editRoleDropdownExpanded = false },
                            modifier = Modifier
                                .background(cardBg)
                                .border(1.dp, themeAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        ) {
                            listOf(
                                PlayerRole.BATSMAN to "🏏 Batsman",
                                PlayerRole.BOWLER to "🎯 Bowler",
                                PlayerRole.ALL_ROUNDER to "⚡ All-Rounder",
                                PlayerRole.WICKET_KEEPER to "🧤 Wicket Keeper"
                            ).forEach { (r, labelWithIcon) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = labelWithIcon,
                                            color = if (editRole == r) themeAccent else textMain,
                                            fontWeight = if (editRole == r) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        editRole = r
                                        editRoleDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

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
                            colors = CheckboxDefaults.colors(checkedColor = if (isDarkTheme) NeonBlue else LightPitchPrimary)
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
                        label = "Player Photo",
                        placeholderIcon = getPlayerPlaceholderIcon(target.id, editName)
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
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = if (isDarkTheme) Color.Black else Color.White),
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
}
