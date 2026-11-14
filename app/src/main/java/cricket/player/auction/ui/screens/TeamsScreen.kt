package cricket.player.auction.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.Player
import cricket.player.auction.model.PlayerRole
import cricket.player.auction.model.PlayerStatus
import cricket.player.auction.model.Team
import cricket.player.auction.model.formatIplCurrency
import cricket.player.auction.ui.components.DriveImage
import cricket.player.auction.ui.components.PhotoUploaderField
import cricket.player.auction.ui.components.PurseProgressBar
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.components.parseColorHex
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(viewModel: AuctionViewModel) {
    val teams by viewModel.teams.collectAsState()
    val players by viewModel.players.collectAsState()
    val activeTournament by viewModel.activeTournament.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground

    var selectedTeamForRoster by remember { mutableStateOf<Team?>(null) }
    var showCreateTeamDialog by remember { mutableStateOf(false) }
    var editingTeam by remember { mutableStateOf<Team?>(null) }
    var deletingTeam by remember { mutableStateOf<Team?>(null) }
    var showAddPlayerForTeamDialog by remember { mutableStateOf<Team?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateTeamDialog = true },
                containerColor = IplGold,
                contentColor = Color.Black
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Team")
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
            // Top Header: Single row with title and team count chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "TEAMS & SQUADS",
                            color = themeAccent,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        StatChip(
                            text = "${teams.size} TEAMS",
                            backgroundColor = surfaceBg,
                            textColor = themeAccent
                        )
                    }
                    Text(
                        text = "Click a team card to view squad, edit or add players",
                        color = textSub,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            if (teams.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "No Teams Registered Yet", color = textMain, fontWeight = FontWeight.Bold)
                        Text(text = "Tap the + button to create a custom team", color = textSub, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(teams) { team ->
                        val squad = players.filter { it.soldToTeamId == team.id }
                        val squadCount = squad.size
                        val overseasCount = squad.count { it.isOverseas }

                        TeamPurseCard(
                            team = team,
                            squadCount = squadCount,
                            overseasCount = overseasCount,
                            isDarkTheme = isDarkTheme,
                            onClick = { selectedTeamForRoster = team },
                            onEditClick = { editingTeam = team },
                            onDeleteClick = { deletingTeam = team }
                        )
                    }
                }
            }
        }
    }

    // --- Team Squad Roster & Direct Player Creator Modal ---
    if (selectedTeamForRoster != null) {
        val team = selectedTeamForRoster!!
        val squadPlayers = players.filter { it.soldToTeamId == team.id }

        AlertDialog(
            onDismissRequest = { selectedTeamForRoster = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = team.name, color = Color.White, fontWeight = FontWeight.Bold)
                    StatChip(text = team.remainingPurse.formatIplCurrency(), backgroundColor = IplGold.copy(alpha = 0.2f), textColor = IplGold)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Squad Roster (${squadPlayers.size}/${team.maxSlots} Players)",
                            color = IplGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = {
                                showAddPlayerForTeamDialog = team
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Player", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (squadPlayers.isEmpty()) {
                        Text(
                            text = "No players acquired yet. Click 'Add Player' to add team members directly!",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(squadPlayers) { player ->
                                Surface(
                                    color = StadiumSurface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .border(1.5.dp, IplGold, androidx.compose.foundation.shape.CircleShape)
                                                    .background(StadiumDark)
                                            ) {
                                                DriveImage(url = player.imageUrl, contentDescription = player.name, modifier = Modifier.fillMaxSize())
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = player.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = player.role.label + if (player.isOverseas) " (OS)" else "",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                        Text(
                                            text = (player.soldPrice ?: player.currentBid).formatIplCurrency(),
                                            color = BidGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTeamForRoster = null }) {
                    Text("Close", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Direct Add Player for Specific Team Dialog ---
    if (showAddPlayerForTeamDialog != null) {
        val targetTeam = showAddPlayerForTeamDialog!!
        var name by remember { mutableStateOf("") }
        var role by remember { mutableStateOf(PlayerRole.BATSMAN) }
        var country by remember { mutableStateOf("India") }
        var isOverseas by remember { mutableStateOf(false) }
        var priceStr by remember { mutableStateOf("1.0") }
        var photoUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPlayerForTeamDialog = null },
            title = { Text("Add New Player to ${targetTeam.shortCode}", color = textMain, fontWeight = FontWeight.Bold) },
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
                        Text("Overseas Player", color = textMain, fontSize = 13.sp)
                    }

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Auction Price (in ₹ Crores)") },
                        singleLine = true
                    )

                    PhotoUploaderField(
                        imageUrl = photoUrl,
                        onImageUrlChange = { photoUrl = it },
                        label = "Player Photo"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = priceStr.toDoubleOrNull() ?: 1.0
                        viewModel.localRepository.addPlayer(
                            Player(
                                id = "p_${System.currentTimeMillis()}",
                                tournamentId = targetTeam.tournamentId,
                                name = name,
                                role = role,
                                country = country,
                                isOverseas = isOverseas,
                                basePrice = price,
                                currentBid = price,
                                soldPrice = price,
                                soldToTeamId = targetTeam.id,
                                status = PlayerStatus.SOLD,
                                imageUrl = photoUrl
                            )
                        )
                        showAddPlayerForTeamDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = name.isNotBlank()
                ) {
                    Text("ADD TO SQUAD", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPlayerForTeamDialog = null }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Create Custom Team Dialog ---
    if (showCreateTeamDialog) {
        var teamName by remember { mutableStateOf("") }
        var shortCode by remember { mutableStateOf("") }
        var colorHex by remember { mutableStateOf("#FFD700") }
        var purseStr by remember { mutableStateOf(activeTournament?.defaultPurse?.toString() ?: "100.0") }
        var captainEmail by remember { mutableStateOf("") }
        var logoUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateTeamDialog = false },
            title = { Text("Create Custom Team", color = textMain) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = { Text("Team Name") },
                        placeholder = { Text("e.g. Royal Strikers") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = shortCode,
                        onValueChange = { shortCode = it },
                        label = { Text("Short Code (2-4 letters)") },
                        placeholder = { Text("e.g. STR") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = colorHex,
                        onValueChange = { colorHex = it },
                        label = { Text("Primary Team Hex Color") },
                        placeholder = { Text("#FFD700, #00D2FF, #EC1C24") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = purseStr,
                        onValueChange = { purseStr = it },
                        label = { Text("Total Purse Budget (in ₹ Cr)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = captainEmail,
                        onValueChange = { captainEmail = it },
                        label = { Text("Captain Email (Optional)") },
                        singleLine = true
                    )

                    PhotoUploaderField(
                        imageUrl = logoUrl,
                        onImageUrlChange = { logoUrl = it },
                        label = "Team Logo"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val purse = purseStr.toDoubleOrNull() ?: 100.0
                        viewModel.createTeam(
                            name = teamName,
                            shortCode = shortCode,
                            primaryColorHex = colorHex,
                            totalPurse = purse,
                            captainEmail = captainEmail.ifBlank { null },
                            logoUrl = logoUrl
                        )
                        showCreateTeamDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = teamName.isNotBlank() && shortCode.isNotBlank()
                ) {
                    Text("Create Team", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTeamDialog = false }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Edit Team Dialog ---
    if (editingTeam != null) {
        val target = editingTeam!!
        var editName by remember(target) { mutableStateOf(target.name) }
        var editCode by remember(target) { mutableStateOf(target.shortCode) }
        var editColorHex by remember(target) { mutableStateOf(target.primaryColorHex) }
        var editPurseStr by remember(target) { mutableStateOf(target.totalPurse.toString()) }
        var editCaptainEmail by remember(target) { mutableStateOf(target.captainEmail ?: "") }
        var editLogoUrl by remember(target) { mutableStateOf(target.logoUrl) }

        AlertDialog(
            onDismissRequest = { editingTeam = null },
            title = { Text("Edit Team ${target.shortCode}", color = textMain, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Team Name") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editCode,
                        onValueChange = { editCode = it },
                        label = { Text("Short Code") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editColorHex,
                        onValueChange = { editColorHex = it },
                        label = { Text("Primary Color Hex") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editPurseStr,
                        onValueChange = { editPurseStr = it },
                        label = { Text("Total Purse Budget (in ₹ Cr)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editCaptainEmail,
                        onValueChange = { editCaptainEmail = it },
                        label = { Text("Captain Email") },
                        singleLine = true
                    )

                    PhotoUploaderField(
                        imageUrl = editLogoUrl,
                        onImageUrlChange = { editLogoUrl = it },
                        label = "Team Logo"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val purse = editPurseStr.toDoubleOrNull() ?: target.totalPurse
                        val updated = target.copy(
                            name = editName,
                            shortCode = editCode,
                            primaryColorHex = editColorHex,
                            totalPurse = purse,
                            captainEmail = editCaptainEmail.ifBlank { null },
                            logoUrl = editLogoUrl
                        )
                        viewModel.updateTeam(updated)
                        editingTeam = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = editName.isNotBlank() && editCode.isNotBlank()
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTeam = null }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }

    // --- Delete Team Dialog ---
    if (deletingTeam != null) {
        val target = deletingTeam!!
        AlertDialog(
            onDismissRequest = { deletingTeam = null },
            title = { Text("Delete Team?", color = UnsoldRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${target.name}' (${target.shortCode})?", color = textSub)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTeam(target.id)
                        deletingTeam = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("DELETE TEAM", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTeam = null }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }
}

@Composable
fun TeamPurseCard(
    team: Team,
    squadCount: Int,
    overseasCount: Int,
    isDarkTheme: Boolean = true,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val teamColor = parseColorHex(team.primaryColorHex)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, teamColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(teamColor, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = team.shortCode,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = team.name,
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                        if (team.inviteCode.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                color = IplGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.4f)),
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString(team.inviteCode))
                                    Toast.makeText(context, "🔑 Code ${team.inviteCode} copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = IplGold, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "INVITE: ${team.inviteCode}",
                                        color = IplGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = IplGold, modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatChip(
                        text = team.remainingPurse.formatIplCurrency(),
                        backgroundColor = IplGold.copy(alpha = 0.2f),
                        textColor = IplGold
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    IconButton(onClick = onEditClick) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Team", tint = IplGold, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Team", tint = UnsoldRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PurseProgressBar(spent = team.spentPurse, total = team.totalPurse)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = textSub, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Squad: $squadCount/${team.maxSlots}",
                        color = textMain,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = NeonBlue, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Overseas: $overseasCount/${team.maxOverseas}",
                        color = NeonBlue,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
