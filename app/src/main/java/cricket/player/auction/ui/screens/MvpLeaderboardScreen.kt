package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.PlayerCareerStats
import cricket.player.auction.ui.components.*
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MvpLeaderboardScreen(viewModel: AuctionViewModel) {
    val mvpLeaderboard by viewModel.mvpLeaderboard.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    var selectedTab by remember { mutableStateOf(0) } // 0: Overall MVP, 1: Orange Cap (Runs), 2: Purple Cap (Wkts)

    val displayedList = remember(mvpLeaderboard, selectedTab) {
        when (selectedTab) {
            1 -> mvpLeaderboard.sortedByDescending { it.runsScored }
            2 -> mvpLeaderboard.sortedByDescending { it.wicketsTaken }
            else -> mvpLeaderboard.sortedByDescending { it.mvpPoints }
        }
    }

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        val emptyScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(
                    if (displayedList.isEmpty()) {
                        Modifier
                            .verticalScroll(emptyScrollState)
                            .padding(16.dp)
                            .padding(bottom = 88.dp)
                    } else {
                        Modifier.padding(16.dp)
                    }
                )
        ) {
            Text(
                text = "Most Valuable Player Points & Career History",
                color = textMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Points calculated for Runs, 4s, 6s, Wickets, Dots, Maidens & Catches",
                color = textSub,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = cardBg,
                contentColor = themeAccent
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("⭐ Overall MVP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedTab == 0) themeAccent else textSub) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🏏 Orange Cap", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedTab == 1) themeAccent else textSub) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("⚡ Purple Cap", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (selectedTab == 2) themeAccent else textSub) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (displayedList.isEmpty()) {
                EmptyStateGuideCard(
                    mainIcon = Icons.Default.Star,
                    badgeText = "CRICHEROES MVP AWARDS",
                    title = "MVP Leaderboard Awaiting Matches ⭐",
                    description = "Player career MVP points, Orange Cap (most runs), and Purple Cap (most wickets) calculate dynamically as matches are played.",
                    steps = listOf(
                        GuideStep(
                            stepNumber = 1,
                            title = "Batting & Bowling Impact",
                            description = "Runs, boundaries, strike-rates, dot balls, and wickets contribute to CricHeroes points.",
                            icon = Icons.Default.Star
                        ),
                        GuideStep(
                            stepNumber = 2,
                            title = "Orange & Purple Cap Race",
                            description = "Dedicated tabs highlight top run-scorers and leading wicket-takers in real time.",
                            icon = Icons.Default.EmojiEvents
                        ),
                        GuideStep(
                            stepNumber = 3,
                            title = "Tournament MVP Crown",
                            description = "The highest ranked overall match-winner claims the prestigious Tournament MVP trophy!",
                            icon = Icons.Default.MilitaryTech
                        )
                    ),
                    isDarkTheme = isDarkTheme,
                    accentColor = themeAccent
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(displayedList) { index, playerStats ->
                        MvpPlayerCard(rank = index + 1, stats = playerStats, isDarkTheme = isDarkTheme)
                    }
                }
            }
        }
    }
}

@Composable
fun MvpPlayerCard(rank: Int, stats: PlayerCareerStats, isDarkTheme: Boolean = true) {
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = if (rank == 1) androidx.compose.foundation.BorderStroke(2.dp, themeAccent) else null,
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
                    color = if (rank == 1) themeAccent else if (rank <= 3) (if (isDarkTheme) NeonBlue else LightPitchSecondary) else surfaceBg,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            color = if (rank <= 3) (if (isDarkTheme) Color.Black else Color.White) else textMain,
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
                        .border(1.dp, themeAccent, CircleShape)
                ) {
                    DriveImage(
                        url = stats.photoUrl,
                        contentDescription = stats.playerName,
                        fallbackIcon = getPlayerPlaceholderIcon(stats.playerId, stats.playerName),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stats.playerName,
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stats.playerRole.label} • ${stats.matches} Matches",
                        color = textSub,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // CricHeroes MVP Points Badge
                Surface(
                    color = themeAccent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = themeAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${stats.mvpPoints.toInt()} pts",
                            color = themeAccent,
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
                StatChip(text = "Runs: ${stats.runsScored} (SR: ${String.format("%.1f", stats.strikeRate)})", backgroundColor = surfaceBg, textColor = textMain)
                StatChip(text = "Wkts: ${stats.wicketsTaken} (Econ: ${String.format("%.1f", stats.economyRate)})", backgroundColor = surfaceBg, textColor = if (isDarkTheme) BidGreen else LightModeGreenDark)
                StatChip(text = "Catches: ${stats.catches}", backgroundColor = surfaceBg, textColor = if (isDarkTheme) NeonBlue else LightPitchText)
            }
        }
    }
}
