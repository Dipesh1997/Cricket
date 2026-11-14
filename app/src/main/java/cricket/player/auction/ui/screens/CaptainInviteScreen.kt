package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.UserRole
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptainInviteScreen(viewModel: AuctionViewModel) {
    val teams by viewModel.teams.collectAsState()
    val invites by viewModel.captainInvites.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    var inviteCodeInput by remember { mutableStateOf("") }
    var selectedTeamToInvite by remember { mutableStateOf<String?>(null) }
    var claimMessage by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp)
    ) {
        Text(
            text = "CAPTAIN INVITATION SYSTEM",
            color = themeAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Invite team captains & grant bidding permissions",
            color = textSub,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Current User Status Card ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "YOUR ACTIVE ACCOUNT", color = textSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = currentUser?.displayName ?: "Spectator", color = textMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = currentUser?.email ?: "", color = textSub, fontSize = 12.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(text = currentUser?.role?.label ?: "Spectator", backgroundColor = themeAccent.copy(alpha = 0.2f), textColor = themeAccent)
                    if (currentUser?.assignedTeamId != null) {
                        val assignedTeam = teams.find { it.id == currentUser?.assignedTeamId }
                        if (assignedTeam != null) {
                            StatChip(text = "Assigned: ${assignedTeam.name}", backgroundColor = NeonBlue.copy(alpha = 0.2f), textColor = NeonBlue)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Claim Team with Invite Code Section ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = themeAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "CLAIM TEAM AS CAPTAIN", color = textMain, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inviteCodeInput,
                    onValueChange = { inviteCodeInput = it },
                    placeholder = { Text("Enter Invite Code (e.g., CSK2026)", color = textSub) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themeAccent,
                        unfocusedBorderColor = if (isDarkTheme) Color.DarkGray else LightPitchContainer,
                        focusedContainerColor = surfaceBg,
                        unfocusedContainerColor = surfaceBg,
                        focusedTextColor = textMain,
                        unfocusedTextColor = textMain
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val claimedTeam = viewModel.claimCaptainInvite(inviteCodeInput.trim())
                        if (claimedTeam != null) {
                            claimMessage = "Successfully claimed ${claimedTeam.name}! You now have captain bidding access."
                        } else {
                            val teamByDefaultCode = teams.find { it.inviteCode.equals(inviteCodeInput.trim(), ignoreCase = true) }
                            if (teamByDefaultCode != null) {
                                viewModel.switchUserRole(UserRole.TEAM_CAPTAIN, teamByDefaultCode.id)
                                claimMessage = "Claimed ${teamByDefaultCode.name}! Assigned as Captain."
                            } else {
                                claimMessage = "Invalid or expired invite code."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeAccent, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = inviteCodeInput.isNotBlank()
                ) {
                    Text("CLAIM TEAM", fontWeight = FontWeight.Bold)
                }

                if (claimMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = claimMessage!!,
                        color = if (claimMessage!!.startsWith("Success") || claimMessage!!.startsWith("Claimed")) BidGreen else UnsoldRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Generate Invite Code (Admin Section) ---
        if (currentUser?.role == UserRole.ADMIN_AUCTIONEER) {
            Card(
                colors = CardDefaults.cardColors(containerColor = surfaceBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonBlue.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "GENERATE CAPTAIN INVITE CODE", color = NeonBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Select Team to Invite Captain:", color = textSub, fontSize = 12.sp)

                    LazyColumn(
                        modifier = Modifier.height(150.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(teams) { team ->
                            Surface(
                                color = cardBg,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.generateCaptainInvite(team.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = team.name, color = textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(text = "Default Code: ${team.inviteCode}", color = themeAccent, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
