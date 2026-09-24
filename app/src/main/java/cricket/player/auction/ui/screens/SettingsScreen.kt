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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
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
import cricket.player.auction.model.UserRole
import cricket.player.auction.ui.components.PhotoUploaderField
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AuctionViewModel) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val activeTournament by viewModel.activeTournament.collectAsState()

    var selectedRole by remember { mutableStateOf(currentUser?.role ?: UserRole.ADMIN_AUCTIONEER) }
    var emailInput by remember { mutableStateOf(currentUser?.email ?: "admin@cricket.com") }
    var nameInput by remember { mutableStateOf(currentUser?.displayName ?: "Cricket Admin") }
    var avatarUrl by remember { mutableStateOf(currentUser?.photoUrl ?: "") }
    var showResetConfirmModal by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "APP SETTINGS & DATABASE",
            color = IplGold,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Manage local database backup export/import, storage & user roles",
            color = Color.Gray,
            fontSize = 12.sp
        )

        // --- Export & Import Backup Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = IplGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "DATABASE EXPORT & IMPORT BACKUP", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Save your entire database (tournaments, teams, players, scorecards) to a local JSON file or restore from a previous backup.",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export Button
                    Button(
                        onClick = {
                            val fileName = "cricket_backup_${System.currentTimeMillis()}.json"
                            exportLauncher.launch(fileName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT BACKUP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Import Button
                    Button(
                        onClick = { importLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IMPORT BACKUP", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- Local Database Status Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = IplGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "LOCAL OFFLINE DATABASE ENGINE", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "100% offline-first architecture. All tournament rosters, bids, live scorecards, and MVP leaderboards are stored locally on your device.",
                    color = Color.LightGray,
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
            colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = IplGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "PROFILE & PERMISSION ROLE", color = Color.White, fontWeight = FontWeight.Bold)
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

                Text(text = "Select Permission Role:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

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
                                colors = RadioButtonDefaults.colors(selectedColor = IplGold)
                            )
                            Text(text = role.label, color = Color.White, fontSize = 13.sp)
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
                    color = Color.LightGray,
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
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = StadiumCardDark
        )
    }
}
