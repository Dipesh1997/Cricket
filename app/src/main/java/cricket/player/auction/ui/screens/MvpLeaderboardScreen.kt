package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.PlayerCareerStats
import cricket.player.auction.ui.components.DriveImage
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MvpLeaderboardScreen(viewModel: AuctionViewModel) {
    val mvpLeaderboard by viewModel.mvpLeaderboard.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Overall MVP, 1: Orange Cap (Runs), 2: Purple Cap (Wkts)

    val displayedList = remember(mvpLeaderboard, selectedTab) {
        when (selectedTab) {
            1 -> mvpLeaderboard.sortedByDescending { it.runsScored }
            2 -> mvpLeaderboard.sortedByDescending { it.wicketsTaken }
            else -> mvpLeaderboard.sortedByDescending { it.mvpPoints }
        }
    }

    Scaffold(
        containerColor = StadiumDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Most Valuable Player Points & Career History",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Points calculated for Runs, 4s, 6s, Wickets, Dots, Maidens & Catches",
                color = Color.Gray,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = StadiumCardDark,
                contentColor = IplGold
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("⭐ Overall MVP", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🏏 Orange Cap", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("⚡ Purple Cap", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (displayedList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No player history available yet", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(displayedList) { index, playerStats ->
                        MvpPlayerCard(rank = index + 1, stats = playerStats)
                    }
                }
            }
        }
    }
}

@Composable
fun MvpPlayerCard(rank: Int, stats: PlayerCareerStats) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
        shape = RoundedCornerShape(16.dp),
        border = if (rank == 1) androidx.compose.foundation.BorderStroke(2.dp, IplGold) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Number / Badge
                Surface(
                    shape = CircleShape,
                    color = if (rank == 1) IplGold else if (rank <= 3) NeonBlue else StadiumSurface,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            color = if (rank <= 3) Color.Black else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Player Photo
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.dp, IplGold, CircleShape)
                ) {
                    DriveImage(url = stats.photoUrl, contentDescription = stats.playerName, modifier = Modifier.fillMaxSize())
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stats.playerName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${stats.playerRole.label} • ${stats.matches} Matches",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                // CricHeroes MVP Points Badge
                Surface(
                    color = IplGold.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = IplGold, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.mvpPoints.toInt()} pts",
                            color = IplGold,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Multi-Tournament Career Stats Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatChip(text = "Runs: ${stats.runsScored} (SR: ${String.format("%.1f", stats.strikeRate)})", backgroundColor = StadiumSurface, textColor = Color.White)
                StatChip(text = "Wkts: ${stats.wicketsTaken} (Econ: ${String.format("%.1f", stats.economyRate)})", backgroundColor = StadiumSurface, textColor = BidGreen)
                StatChip(text = "Catches: ${stats.catches}", backgroundColor = StadiumSurface, textColor = NeonBlue)
            }
        }
    }
}
