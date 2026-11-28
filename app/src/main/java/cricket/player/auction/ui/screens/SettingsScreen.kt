package cricket.player.auction.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.UserRole
import cricket.player.auction.ui.components.PhotoUploaderField
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AuctionViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentUser by viewModel.currentUser.collectAsState()
    val activeTournament by viewModel.activeTournament.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isYouTubeConnected by viewModel.isYouTubeConnected.collectAsState()
    val connectedGoogleEmail by viewModel.connectedGoogleEmail.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.handleGoogleYouTubeSignInResult(result.data) { ok, msg ->
            Toast.makeText(context, if (ok) "✅ $msg" else "⚠️ $msg", Toast.LENGTH_SHORT).show()
        }
    }

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    var selectedRole by remember { mutableStateOf(currentUser?.role ?: UserRole.ADMIN_AUCTIONEER) }
    var emailInput by remember { mutableStateOf(currentUser?.email ?: "admin@cricket.com") }
    var nameInput by remember { mutableStateOf(currentUser?.displayName ?: "Cricket Admin") }
    var avatarUrl by remember { mutableStateOf(currentUser?.photoUrl ?: "") }
    var showResetConfirmModal by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "APP SETTINGS & DATABASE",
            color = themeAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Manage local database backup/restore, storage & user roles",
            color = textSub,
            fontSize = 12.sp
        )

        // --- 0. Google Sign-In for Direct YouTube Live Streaming ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isYouTubeConnected) BidGreen else Color(0xFFFF0000).copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "GOOGLE & YOUTUBE LIVE ACCOUNT",
                    color = if (isYouTubeConnected) (if (isDarkTheme) BidGreen else Color(0xFF15803D)) else textMain,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (isYouTubeConnected)
                        "Connected: ${connectedGoogleEmail ?: "Google Account"} — Ready for direct YouTube Live streaming in Auction & Matches."
                    else
                        "Sign in with Google once to start direct YouTube Live broadcasts from Auction & Match Scorer without web sign-in.",
                    color = textSub,
                    fontSize = 11.sp
                )
                Button(
                    onClick = { googleSignInLauncher.launch(viewModel.getYouTubeSignInIntent(context)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isYouTubeConnected) surfaceBg else Color.White,
                        contentColor = if (isYouTubeConnected) textMain else Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isYouTubeConnected) "G  Switch Google Account" else "G  Sign in with Google",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // --- 1. Spectator Code Card (Prominently displayed at top) ---
        val spectatorCode = activeTournament?.spectatorInviteCode.orEmpty().ifBlank {
            "SPEC" + (activeTournament?.id ?: "").takeLast(4).uppercase()
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonBlue)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = NeonBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SPECTATOR CODE (READ-ONLY)",
                            color = NeonBlue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    StatChip(text = "LIVE VIEW ONLY", backgroundColor = NeonBlue.copy(alpha = 0.2f), textColor = NeonBlue)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Share this spectator code with audience, fans, or team viewers. Anyone joining with this code can watch the lucky player spin wheel, auction bids, match scorecards, and points table in real-time without modifying any data.",
                    color = textSub,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = surfaceBg,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SPECTATOR JOIN CODE", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = spectatorCode,
                                color = textMain,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }

                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(spectatorCode))
                                Toast.makeText(context, "👁️ Spectator Code '$spectatorCode' copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("COPY CODE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // --- 2. Admin Role & Code Management Card (Only for Tournament Admin) ---
        if (currentUser?.role == UserRole.ADMIN_AUCTIONEER && activeTournament != null) {
            val tourney = activeTournament!!
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = themeAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ROLE & ACCESS CODES (ADMIN)",
                            color = textMain,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Generate and share codes to add co-admins, scorers, or reset codes to revoke old users.",
                        color = textSub,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Admin Code Row
                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("ADMIN / AUCTIONEER CODE", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(tourney.adminCode, color = themeAccent, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Share to add another admin with full privileges", color = textSub, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(tourney.adminCode))
                                    Toast.makeText(context, "🔑 Admin Code copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = themeAccent, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    val newCode = viewModel.regenerateAdminCode(tourney.id)
                                    Toast.makeText(context, "🔄 New Admin Code generated: $newCode", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = NeonBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // 2. Scorer Code Row
                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("LIVE SCORER INVITE CODE", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(tourney.scorerInviteCode, color = NeonBlue, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Reset to remove previous scorer and invite new one", color = textSub, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(tourney.scorerInviteCode))
                                    Toast.makeText(context, "🏏 Scorer Code copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonBlue, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    val newCode = viewModel.regenerateScorerCode(tourney.id)
                                    Toast.makeText(context, "🔄 Scorer reset! New code: $newCode", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = themeAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // 3. Spectator Code Row
                    Surface(
                        color = surfaceBg,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SPECTATOR INVITE CODE", color = textSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text(spectatorCode, color = textMain, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Reset to cycle audience code", color = textSub, fontSize = 10.sp)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(spectatorCode))
                                    Toast.makeText(context, "👁️ Spectator Code copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = textMain, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = {
                                    val newCode = viewModel.regenerateSpectatorCode(tourney.id)
                                    Toast.makeText(context, "🔄 Spectator code reset! New code: $newCode", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Regenerate", tint = NeonBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Export & Import Backup Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = themeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "BACKUP & RESTORE", color = textMain, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Automatic one-click backup and restore directly to the 'Cricket' folder in your device's File Manager (Documents/Cricket/cricket_backup.json).",
                    color = textSub,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Backup Button
                    Button(
                        onClick = {
                            viewModel.backupToCricketFolder().fold(
                                onSuccess = { file ->
                                    Toast.makeText(context, "✅ Backed up to Cricket folder in File Manager!", Toast.LENGTH_LONG).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "❌ Backup failed: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BACKUP", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Restore Button
                    Button(
                        onClick = {
                            viewModel.restoreFromCricketFolder().fold(
                                onSuccess = {
                                    Toast.makeText(context, "✅ Restored successfully from Cricket folder!", Toast.LENGTH_LONG).show()
                                },
                                onFailure = { err ->
                                    Toast.makeText(context, "❌ Restore failed: ${err.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RESTORE", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // --- Local Database Status Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = themeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "LOCAL OFFLINE DATABASE ENGINE", color = textMain, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "100% offline-first architecture. All tournament rosters, bids, live scorecards, and MVP leaderboards are stored locally on your device.",
                    color = textSub,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(
                        text = "✓ Offline Engine Active",
                        backgroundColor = Color(0xFF2E7D32).copy(alpha = 0.3f),
                        textColor = Color(0xFF81C784)
                    )
                    StatChip(
                        text = activeTournament?.name ?: "Fresh App State",
                        backgroundColor = NeonBlue.copy(alpha = 0.2f),
                        textColor = NeonBlue
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showResetConfirmModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All App Data & Start Fresh", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- User Profile & Role Settings Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = themeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "PROFILE & PERMISSION ROLE", color = textMain, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Account Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                PhotoUploaderField(
                    imageUrl = avatarUrl,
                    onImageUrlChange = { avatarUrl = it },
                    label = "Profile Avatar Photo"
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(text = "Select Permission Role:", color = textSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    UserRole.values().forEach { role ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedRole == role,
                                onClick = {
                                    selectedRole = role
                                    viewModel.switchUserRole(role)
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = themeAccent)
                            )
                            Text(text = role.label, color = textMain, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // --- Reset Confirmation Modal ---
    if (showResetConfirmModal) {
        AlertDialog(
            onDismissRequest = { showResetConfirmModal = false },
            title = { Text("Reset All Application Data?", color = UnsoldRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This will permanently remove all tournaments, teams, players, bids, and match scorecards. The app will start completely fresh for new user entries.",
                    color = textSub,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        Toast.makeText(context, "App data cleared completely!", Toast.LENGTH_SHORT).show()
                        showResetConfirmModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UnsoldRed, contentColor = Color.White)
                ) {
                    Text("YES, CLEAR ALL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmModal = false }) {
                    Text("Cancel", color = textSub)
                }
            },
            containerColor = cardBg
        )
    }
}
