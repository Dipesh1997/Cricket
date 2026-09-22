package com.cricket.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.auction.ui.viewmodel.TournamentHubViewModel
import com.cricket.auction.util.AuctionUtils

sealed class HubTab(val title: String, val icon: ImageVector) {
    object LiveAuction : HubTab("Auction", Icons.Default.Gavel)
    object Teams : HubTab("Teams", Icons.Default.Groups)
    object Players : HubTab("Players", Icons.Default.Person)
    object Squads : HubTab("Squads", Icons.Default.Shield)
    object Stats : HubTab("Stats", Icons.Default.Analytics)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TournamentHubScreen(
    viewModel: TournamentHubViewModel,
    onBack: () -> Unit
) {
    val tournament by viewModel.tournament.collectAsState()
    var selectedTab by remember { mutableStateOf<HubTab>(HubTab.LiveAuction) }

    val tabs = listOf(
        HubTab.LiveAuction,
        HubTab.Teams,
        HubTab.Players,
        HubTab.Squads,
        HubTab.Stats
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = tournament?.name ?: "Tournament",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (tournament != null) {
                            Text(
                                text = "Purse: ${AuctionUtils.formatAmount(tournament!!.budgetPerTeam, tournament!!.currencyFormat)} | Squad: ${tournament!!.minSquadSize}-${tournament!!.maxSquadSize}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                fontSize = 11.sp
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
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                HubTab.LiveAuction -> LiveAuctionScreen(
                    viewModel = viewModel,
                    onNavigateToPlayers = { selectedTab = HubTab.Players },
                    onNavigateToTeams = { selectedTab = HubTab.Teams }
                )
                HubTab.Teams -> TeamsScreen(viewModel = viewModel)
                HubTab.Players -> PlayersScreen(
                    viewModel = viewModel,
                    onNavigateToAuction = { selectedTab = HubTab.LiveAuction }
                )
                HubTab.Squads -> SquadsScreen(viewModel = viewModel)
                HubTab.Stats -> StatsAndExportScreen(viewModel = viewModel)
            }
        }
    }
}
