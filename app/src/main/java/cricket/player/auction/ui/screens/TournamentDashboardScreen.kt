package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.formatIplCurrency
import cricket.player.auction.ui.components.StatChip
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

data class DashboardSubPageItem(
    val title: String,
    val subtitle: String,
    val route: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun TournamentDashboardScreen(
    viewModel: AuctionViewModel,
    onNavigateToSubPage: (String) -> Unit,
    onBackToHome: () -> Unit
) {
    val activeTournament by viewModel.activeTournament.collectAsState()
    val teams by viewModel.teams.collectAsState()
    val players by viewModel.players.collectAsState()
    val matches by viewModel.matches.collectAsState()

    val subPages = listOf(
        DashboardSubPageItem("Auction Arena", "Live Bidding & Spin Wheel", "auction", Icons.Default.Gavel, IplGold),
        DashboardSubPageItem("Teams & Squads", "Purse & Create Players", "teams", Icons.Default.Shield, NeonBlue),
        DashboardSubPageItem("Player Roster", "Photos & Pool Management", "players", Icons.Default.Person, Color(0xFF9C27B0)),
        DashboardSubPageItem("Live Scorer", "Match Recorder & Solo Rule", "scorer", Icons.Default.SportsCricket, BidGreen),
        DashboardSubPageItem("Points Table", "Standings & NRR", "points", Icons.Default.Assessment, Color(0xFFFF9800)),
        DashboardSubPageItem("MVP Ranks", "CricHeroes Points & Stats", "mvp", Icons.Default.Star, Color(0xFFE91E63)),
        DashboardSubPageItem("Backup & Settings", "JSON Backup Export/Import", "settings", Icons.Default.Settings, Color(0xFF00BCD4))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StadiumDark)
            .padding(16.dp)
    ) {
        // --- Tournament Stats Summary Banner ---
        val tourney = activeTournament
        if (tourney != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, IplGold.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = IplGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = tourney.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        StatChip(text = "ACTIVE WORKSPACE", backgroundColor = BidGreen.copy(alpha = 0.2f), textColor = BidGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip(text = "Budget: ${tourney.defaultPurse.formatIplCurrency()}", backgroundColor = StadiumSurface, textColor = Color.LightGray)
                        StatChip(text = "Teams: ${teams.size}", backgroundColor = StadiumSurface, textColor = NeonBlue)
                        StatChip(text = "Players: ${players.size}", backgroundColor = StadiumSurface, textColor = IplGold)
                        StatChip(text = "Matches: ${matches.size}", backgroundColor = StadiumSurface, textColor = BidGreen)
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "No Active Tournament Selected", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onBackToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black)
                    ) {
                        Text("Select Tournament From Home", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "TOURNAMENT SUB-PAGES & WORKSPACE",
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Sub-pages Grid Tiles
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(subPages.size) { index ->
                val page = subPages[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .clickable { onNavigateToSubPage(page.route) },
                    border = androidx.compose.foundation.BorderStroke(1.dp, page.accentColor.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = page.accentColor.copy(alpha = 0.2f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = page.icon, contentDescription = null, tint = page.accentColor, modifier = Modifier.size(20.dp))
                                }
                            }

                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                        }

                        Column {
                            Text(text = page.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = page.subtitle, color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
