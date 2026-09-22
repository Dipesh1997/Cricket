package com.cricket.auction.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.PlayerEntity
import com.cricket.auction.data.model.TeamWithPlayers
import com.cricket.auction.data.model.TournamentEntity
import com.cricket.auction.ui.components.EmptyStateView
import com.cricket.auction.ui.components.OverseasBadge
import com.cricket.auction.ui.components.RoleBadge
import com.cricket.auction.ui.components.TeamAvatar
import com.cricket.auction.ui.viewmodel.TournamentHubViewModel
import com.cricket.auction.util.AuctionUtils

@Composable
fun SquadsScreen(
    viewModel: TournamentHubViewModel
) {
    val tournament by viewModel.tournament.collectAsState()
    val teamsWithPlayers by viewModel.teamsWithPlayers.collectAsState()
    val currency = tournament?.currencyFormat ?: CurrencyFormat.INR_CR_LAKH

    var selectedTabIndex by remember { mutableStateOf(0) }
    var playerToRelease by remember { mutableStateOf<PlayerEntity?>(null) }

    if (teamsWithPlayers.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Groups,
            title = "No Teams Registered",
            message = "Add teams in the Teams tab to track their rosters and squad compositions.",
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val safeIndex = selectedTabIndex.coerceIn(0, teamsWithPlayers.size - 1)
        val selectedTeamWithPlayers = teamsWithPlayers[safeIndex]
        val team = selectedTeamWithPlayers.team
        val maxSquad = tournament?.maxSquadSize ?: 25
        val minSquad = tournament?.minSquadSize ?: 15
        val maxOverseas = tournament?.maxOverseasPlayers ?: 8

        Column(modifier = Modifier.fillMaxSize()) {
            // Horizontal scrollable team tabs
            ScrollableTabRow(
                selectedTabIndex = safeIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                teamsWithPlayers.forEachIndexed { index, twp ->
                    Tab(
                        selected = index == safeIndex,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TeamAvatar(
                                    shortCode = twp.team.shortCode,
                                    colorHex = twp.team.colorHex,
                                    size = 22.dp,
                                    textSize = 8
                                )
                                Text(
                                    text = "${twp.team.shortCode} (${twp.squadCount})",
                                    fontWeight = if (index == safeIndex) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Team Summary Header Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TeamAvatar(
                                    shortCode = team.shortCode,
                                    colorHex = team.colorHex,
                                    size = 50.dp,
                                    textSize = 16
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = team.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (team.ownerName.isNotBlank()) {
                                        Text(
                                            text = "Owner: ${team.ownerName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Purse stats
                            val spentPercent = if (team.totalPurse > 0) {
                                (selectedTeamWithPlayers.totalSpent.toFloat() / team.totalPurse.toFloat()).coerceIn(0f, 1f)
                            } else 0f

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Remaining Purse", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = AuctionUtils.formatAmount(team.remainingPurse, currency),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Spent / Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${AuctionUtils.formatAmount(selectedTeamWithPlayers.totalSpent, currency)} / ${AuctionUtils.formatAmount(team.totalPurse, currency)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { spentPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Role Breakdown Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RoleStatCard(label = "Squad", count = "${selectedTeamWithPlayers.squadCount}/$maxSquad", subLabel = "Min: $minSquad", modifier = Modifier.weight(1f))
                                RoleStatCard(label = "Overseas", count = "${selectedTeamWithPlayers.overseasCount}/$maxOverseas", subLabel = "Quota", modifier = Modifier.weight(1f))
                                RoleStatCard(label = "Batsmen", count = "${selectedTeamWithPlayers.batsmenCount}", subLabel = "BAT", modifier = Modifier.weight(1f))
                                RoleStatCard(label = "Bowlers", count = "${selectedTeamWithPlayers.bowlersCount}", subLabel = "BOWL", modifier = Modifier.weight(1f))
                                RoleStatCard(label = "AR / WK", count = "${selectedTeamWithPlayers.allRoundersCount}/${selectedTeamWithPlayers.wicketKeepersCount}", subLabel = "AR/WK", modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Squad Roster (${selectedTeamWithPlayers.players.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (selectedTeamWithPlayers.players.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No players acquired yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Bid and win players for ${team.name} in the Live Auction room.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(selectedTeamWithPlayers.players, key = { it.id }) { player ->
                        RosterPlayerCard(
                            player = player,
                            currency = currency,
                            onRelease = { playerToRelease = player }
                        )
                    }
                }
            }
        }
    }

    // Release Confirmation Dialog
    playerToRelease?.let { p ->
        AlertDialog(
            onDismissRequest = { playerToRelease = null },
            title = { Text("Release Player?") },
            text = {
                Text("Release '${p.name}' from squad? The price paid (${AuctionUtils.formatAmount(p.soldPrice ?: 0L, currency)}) will be refunded to the team's remaining purse, and the player will return to the unassigned auction pool.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.undoSaleForPlayer(p.id)
                        playerToRelease = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Release & Refund")
                }
            },
            dismissButton = {
                TextButton(onClick = { playerToRelease = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RoleStatCard(label: String, count: String, subLabel: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = count, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subLabel, style = MaterialTheme.typography.labelSmall, fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun RosterPlayerCard(
    player: PlayerEntity,
    currency: CurrencyFormat,
    onRelease: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = player.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    RoleBadge(role = player.role)
                    if (player.isOverseas) {
                        OverseasBadge()
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = AuctionUtils.formatAmount(player.soldPrice ?: player.basePrice, currency),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onRelease, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = "Release Player",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
