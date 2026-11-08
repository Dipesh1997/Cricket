package cricket.player.auction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cricket.player.auction.ui.screens.*
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Tournament : Screen("tournament", "Home", Icons.Default.Home)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.GridView)
    object Auction : Screen("auction", "Auction", Icons.Default.Gavel)
    object List : Screen("list", "List", Icons.AutoMirrored.Filled.List)
    object Teams : Screen("teams", "Teams", Icons.Default.Shield)
    object Players : Screen("players", "Players", Icons.Default.Person)
    object Scorer : Screen("scorer", "Scorer", Icons.Default.SportsCricket)
    object Points : Screen("points", "Points", Icons.Default.Assessment)
    object MVP : Screen("mvp", "MVP", Icons.Default.Star)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: AuctionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            CricketTheme(darkTheme = isDarkTheme) {
                MainAppStructure(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppStructure(viewModel: AuctionViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Tournament.route

    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isHomeScreen = currentRoute == Screen.Tournament.route
    var exportTrigger by remember { mutableStateOf(0) }
    var importTrigger by remember { mutableStateOf(0) }

    val subPageScreens = listOf(
        Screen.Dashboard,
        Screen.Auction,
        Screen.List,
        Screen.Scorer,
        Screen.Settings
    )

    val accentColor = if (isDarkTheme) IplGold else LightPitchPrimary
    val topBarContainer = if (isDarkTheme) StadiumCardDark else LightPitchSurface
    val mainBackground = if (isDarkTheme) StadiumDark else LightPitchBackground

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isHomeScreen) {
                            IconButton(onClick = {
                                if (currentRoute == Screen.Dashboard.route) {
                                    navController.navigate(Screen.Tournament.route)
                                } else {
                                    navController.popBackStack()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = accentColor
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.SportsCricket,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = when {
                                currentRoute == Screen.Tournament.route -> "CRICKET HUB"
                                currentRoute == Screen.Dashboard.route -> "TOURNAMENT WORKSPACE"
                                currentRoute == Screen.Auction.route -> "AUCTION ARENA"
                                currentRoute.startsWith("list") || currentRoute == Screen.List.route -> "TOURNAMENT LISTS"
                                currentRoute == Screen.Teams.route -> "TEAMS & SQUADS"
                                currentRoute == Screen.Players.route -> "PLAYER ROSTER"
                                currentRoute == Screen.Scorer.route -> "LIVE SCORER"
                                currentRoute == Screen.Points.route -> "POINTS TABLE"
                                currentRoute == Screen.MVP.route -> "MVP LEADERBOARD"
                                currentRoute == Screen.Settings.route -> "SETTINGS & BACKUP"
                                else -> "CRICKET HUB"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = accentColor
                        )
                    }
                },
                actions = {
                    // Theme Toggle Button for Quick Switching!
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light/Dark Theme",
                            tint = accentColor
                        )
                    }

                    if (isHomeScreen) {
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.ImportExport,
                                    contentDescription = "Backup Options",
                                    tint = accentColor
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(if (isDarkTheme) StadiumCardDark else LightPitchCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export Backup JSON", color = if (isDarkTheme) Color.White else LightPitchText, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = accentColor) },
                                    onClick = {
                                        menuExpanded = false
                                        exportTrigger++
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Import Backup JSON", color = if (isDarkTheme) Color.White else LightPitchText, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                    leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null, tint = NeonBlue) },
                                    onClick = {
                                        menuExpanded = false
                                        importTrigger++
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = topBarContainer,
                    titleContentColor = accentColor
                )
            )
        },
        bottomBar = {
            // Clean up: Bottom Navigation ONLY appears inside sub-pages/tournament workspace!
            if (!isHomeScreen) {
                NavigationBar(
                    containerColor = topBarContainer,
                    contentColor = if (isDarkTheme) Color.White else LightPitchText
                ) {
                    subPageScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route ||
                            (screen == Screen.List && (
                                currentRoute.startsWith("list") ||
                                currentRoute == Screen.Teams.route ||
                                currentRoute == Screen.Players.route ||
                                currentRoute == Screen.Points.route ||
                                currentRoute == Screen.MVP.route
                            ))
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (isSelected) accentColor else Color.Gray
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    color = if (isSelected) accentColor else Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = if (isDarkTheme) StadiumSurface else LightPitchSurface
                            )
                        )
                    }
                }
            }
        },
        containerColor = mainBackground
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tournament.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Tournament.route) {
                TournamentScreen(
                    viewModel = viewModel,
                    onTournamentClick = {
                        navController.navigate(Screen.Dashboard.route)
                    },
                    onNavigateToAuction = {
                        navController.navigate(Screen.Auction.route)
                    },
                    onNavigateToScorer = {
                        navController.navigate(Screen.Scorer.route)
                    },
                    exportTrigger = exportTrigger,
                    importTrigger = importTrigger
                )
            }
            composable(Screen.Dashboard.route) {
                TournamentDashboardScreen(
                    viewModel = viewModel,
                    onNavigateToSubPage = { subRoute ->
                        navController.navigate(subRoute)
                    },
                    onBackToHome = {
                        navController.navigate(Screen.Tournament.route)
                    }
                )
            }
            composable(Screen.Auction.route) {
                AuctionScreen(viewModel = viewModel)
            }
            composable(
                route = "list?tab={tab}",
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = "teams"
                        nullable = true
                    }
                )
            ) { backStackEntry ->
                val tab = backStackEntry.arguments?.getString("tab") ?: "teams"
                ListsScreen(viewModel = viewModel, initialTab = tab)
            }
            composable(Screen.Teams.route) {
                ListsScreen(viewModel = viewModel, initialTab = "teams")
            }
            composable(Screen.Players.route) {
                ListsScreen(viewModel = viewModel, initialTab = "players")
            }
            composable(Screen.Points.route) {
                ListsScreen(viewModel = viewModel, initialTab = "points")
            }
            composable(Screen.MVP.route) {
                ListsScreen(viewModel = viewModel, initialTab = "mvp")
            }
            composable(Screen.Scorer.route) {
                ScoreRecorderScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}