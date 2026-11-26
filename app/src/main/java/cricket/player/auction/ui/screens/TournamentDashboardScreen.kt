package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import cricket.player.auction.model.formatIplCurrency
import cricket.player.auction.ui.components.*
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
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val context = LocalContext.current
    var showDemoQuizTeamId by remember { mutableStateOf<String?>(null) }

    val screenBg = if (isDarkTheme) StadiumDark else LightPitchBackground
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textMain = if (isDarkTheme) Color.White else LightPitchText
    val textSub = if (isDarkTheme) Color.Gray else LightPitchSecondary
    val themeAccent = if (isDarkTheme) IplGold else LightPitchPrimary

    val subPages = listOf(
        DashboardSubPageItem("Auction Arena", "Live Bidding & Spin Wheel", "auction", Icons.Default.Gavel, IplGold),
        DashboardSubPageItem("Teams & Squads", "Purse & Create Players", "list?tab=teams", Icons.Default.Shield, NeonBlue),
        DashboardSubPageItem("Player Roster", "Photos & Pool Management", "list?tab=players", Icons.Default.Person, Color(0xFF9C27B0)),
        DashboardSubPageItem("Live Scorer", "Match Recorder & Solo Rule", "scorer", Icons.Default.SportsCricket, BidGreen),
        DashboardSubPageItem("Points Table", "Standings & NRR", "list?tab=points", Icons.Default.Assessment, Color(0xFFFF9800)),
        DashboardSubPageItem("MVP Ranks", "CricHeroes Points & Stats", "list?tab=mvp", Icons.Default.Star, Color(0xFFE91E63)),
        DashboardSubPageItem("Backup & Settings", "JSON Backup Export/Import", "settings", Icons.Default.Settings, Color(0xFF00BCD4))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(16.dp)
    ) {
        // --- Tournament Stats Summary Banner ---
        val tourney = activeTournament
        if (tourney != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, themeAccent.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = themeAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = tourney.name, color = textMain, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        StatChip(text = "ACTIVE WORKSPACE", backgroundColor = BidGreen.copy(alpha = 0.2f), textColor = BidGreen)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(text = "Budget: ${tourney.defaultPurse.formatIplCurrency()}", backgroundColor = surfaceBg, textColor = textSub)
                        StatChip(text = "Teams: ${teams.size}", backgroundColor = surfaceBg, textColor = NeonBlue)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(text = "Players: ${players.size}", backgroundColor = surfaceBg, textColor = themeAccent)
                        StatChip(text = "Matches: ${matches.size}", backgroundColor = surfaceBg, textColor = BidGreen)
                    }
                }
            }
        } else {
            EmptyStateGuideCard(
                mainIcon = Icons.Default.GridView,
                badgeText = "WORKSPACE OVERVIEW",
                title = "No Active Tournament Selected 📊",
                description = "Select or create a tournament from the home page to inspect franchise purse allocations, squad rosters, and workspace tools.",
                steps = listOf(
                    GuideStep(
                        stepNumber = 1,
                        title = "Choose Tournament Workspace",
                        description = "Return to the Home screen and choose or join a tournament workspace.",
                        icon = Icons.Default.Home
                    ),
                    GuideStep(
                        stepNumber = 2,
                        title = "Auction & Squad Analysis",
                        description = "Monitor remaining budgets, overseas slots, and player distributions across teams.",
                        icon = Icons.Default.Assessment
                    ),
                    GuideStep(
                        stepNumber = 3,
                        title = "Live Activities & Backup",
                        description = "Access Live Auction, Scorer, Points Table, and JSON backup exports directly.",
                        icon = Icons.Default.PlayArrow
                    )
                ),
                primaryButtonText = "SELECT TOURNAMENT FROM HOME 🏆",
                onPrimaryClick = onBackToHome,
                isDarkTheme = isDarkTheme,
                accentColor = themeAccent
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Demo Mode Feature Playground (Test Quiz, Spectator, Live Scorer & YouTube Hosting without online data) ---
        if (tourney != null && (tourney.isOfflineOnly || tourney.id.startsWith("demo_"))) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonBlue.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎮 DEMO MODE FEATURE PLAYGROUND",
                            color = NeonBlue,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "ALL UNLOCKED",
                            color = BidGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "Preview every live feature instantly using pre-loaded demo data:",
                        color = textSub,
                        fontSize = 10.sp
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Button(
                                onClick = {
                                    viewModel.ensureCaptainQuizIfNeeded(tourney.id)
                                    val quizTeamId = tourney.captainQuizSession?.team1Id ?: teams.firstOrNull()?.id
                                    if (quizTeamId != null) {
                                        showDemoQuizTeamId = quizTeamId
                                    } else {
                                        onNavigateToSubPage("auction")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonBlue, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🧠 5-Q Captain Quiz", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    viewModel.requestScorerLaunch(mode = "SPECTATOR", openLive = true)
                                    onNavigateToSubPage("scorer")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009270), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("👁️ Spectator (Cricbuzz)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    viewModel.requestScorerLaunch(mode = "CLASSIC", openLive = true)
                                    onNavigateToSubPage("scorer")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BidGreen, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("🏏 Live Scorer (Crichunt)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        item {
                            Button(
                                onClick = {
                                    viewModel.requestScorerLaunch(mode = "CLASSIC", openLive = true, openYouTubeDialog = true)
                                    onNavigateToSubPage("scorer")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("📺 YouTube Hosting", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "TOURNAMENT SUB-PAGES & WORKSPACE",
            color = textSub,
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
                    colors = CardDefaults.cardColors(containerColor = cardBg),
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

                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = textSub)
                        }

                        Column {
                            Text(text = page.title, color = textMain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = page.subtitle, color = textSub, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }

    val demoQuizSession = activeTournament?.captainQuizSession
    if (showDemoQuizTeamId != null && activeTournament != null && demoQuizSession != null && demoQuizSession.questions.isNotEmpty()) {
        val quizTeam = teams.find { it.id == showDemoQuizTeamId }
        if (quizTeam != null) {
            CaptainMathQuizDialog(
                team = quizTeam,
                questions = demoQuizSession.questions,
                isDarkTheme = isDarkTheme,
                onSubmit = { correctCount, timeTakenMs ->
                    viewModel.submitCaptainQuizResult(activeTournament!!.id, quizTeam.id, correctCount, timeTakenMs)
                    showDemoQuizTeamId = null
                    Toast.makeText(
                        context,
                        "🧠 ${quizTeam.shortCode} scored $correctCount/5 in ${String.format("%.1f", timeTakenMs / 1000.0)}s!",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onDismiss = { showDemoQuizTeamId = null }
            )
        }
    }
}
