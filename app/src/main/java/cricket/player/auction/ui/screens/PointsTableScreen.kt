package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.PointsTableEntry
import cricket.player.auction.ui.components.*
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointsTableScreen(viewModel: AuctionViewModel) {
    val pointsTable by viewModel.pointsTable.collectAsState()
    val activeTournament by viewModel.activeTournament.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    Scaffold(
        containerColor = screenBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = activeTournament?.name ?: "Cricket Tournament",
                color = textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Automated Standings & Net Run Rate (NRR)",
                color = textSub,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Table Header Bar
            Surface(
                color = cardBg,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", color = themeAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                    Text("TEAM", color = textSub, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("P", color = textSub, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("W", color = textSub, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("L", color = textSub, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("PTS", color = themeAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    Text("NRR", color = if (isDarkTheme) NeonBlue else LightPitchText, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(54.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pointsTable.isEmpty()) {
                EmptyStateGuideCard(
                    mainIcon = Icons.Default.Assessment,
                    badgeText = "STANDINGS & NRR",
                    title = "Points Table Awaiting Matches 📈",
                    description = "Standings, Points, Won/Lost stats, and Net Run Rate (NRR) calculate automatically as tournament matches are completed.",
                    steps = listOf(
                        GuideStep(
                            stepNumber = 1,
                            title = "Play Tournament Matches",
                            description = "Go to the Live Scorer tab to start matches between teams.",
                            icon = Icons.Default.SportsCricket
                        ),
                        GuideStep(
                            stepNumber = 2,
                            title = "Automatic NRR Calculation",
                            description = "Runs scored and overs batted vs conceded compute official Net Run Rate dynamically.",
                            icon = Icons.Default.Assessment
                        ),
                        GuideStep(
                            stepNumber = 3,
                            title = "Leaderboard Qualification",
                            description = "Top ranking franchises advance to the knockout stages and final trophy!",
                            icon = Icons.Default.EmojiEvents
                        )
                    ),
                    isDarkTheme = isDarkTheme,
                    accentColor = themeAccent
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(pointsTable) { index, entry ->
                        PointsTableRow(rank = index + 1, entry = entry, isDarkTheme = isDarkTheme)
                    }
                }
            }
        }
    }
}

@Composable
fun PointsTableRow(rank: Int, entry: PointsTableEntry, isDarkTheme: Boolean = true) {
    val teamColor = parseColorHex(entry.primaryColorHex)
    val nrrFormatted = if (entry.netRunRate >= 0) "+${String.format("%.3f", entry.netRunRate)}" else String.format("%.3f", entry.netRunRate)
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.LightGray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        border = if (rank <= 4) androidx.compose.foundation.BorderStroke(1.dp, teamColor) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Surface(
                shape = CircleShape,
                color = if (rank <= 4) themeAccent else surfaceBg,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$rank",
                        color = if (rank <= 4) (if (isDarkTheme) Color.Black else Color.White) else textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Team Badge & Short Code
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(teamColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entry.shortCode.take(3), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.teamName,
                    color = textMain,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text("${entry.matchesPlayed}", color = textSub, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Text("${entry.won}", color = if (isDarkTheme) BidGreen else LightModeGreenDark, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Text("${entry.lost}", color = if (isDarkTheme) UnsoldRed else LightModeRedDark, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Text("${entry.points}", color = themeAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.width(36.dp))
            Text(nrrFormatted, color = if (isDarkTheme) NeonBlue else LightPitchText, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.width(54.dp))
        }
    }
}
