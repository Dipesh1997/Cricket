package com.cricket.auction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cricket.auction.ui.screens.TournamentHubScreen
import com.cricket.auction.ui.screens.TournamentListScreen
import com.cricket.auction.ui.theme.CricketAuctionTheme
import com.cricket.auction.ui.viewmodel.TournamentHubViewModel
import com.cricket.auction.ui.viewmodel.TournamentViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CricketAuctionApp
        val repository = app.repository

        setContent {
            CricketAuctionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CricketAuctionNavigation(repository = repository)
                }
            }
        }
    }
}

@Composable
fun CricketAuctionNavigation(repository: com.cricket.auction.data.repository.CricketAuctionRepository) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "tournaments"
    ) {
        composable("tournaments") {
            val tournamentViewModel: TournamentViewModel = viewModel(
                factory = TournamentViewModel.provideFactory(repository)
            )
            TournamentListScreen(
                viewModel = tournamentViewModel,
                onTournamentClick = { tournamentId ->
                    navController.navigate("hub/$tournamentId")
                }
            )
        }

        composable(
            route = "hub/{tournamentId}",
            arguments = listOf(
                navArgument("tournamentId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val tournamentId = backStackEntry.arguments?.getLong("tournamentId") ?: return@composable
            val hubViewModel: TournamentHubViewModel = viewModel(
                key = "hub_$tournamentId",
                factory = TournamentHubViewModel.provideFactory(tournamentId, repository)
            )
            TournamentHubScreen(
                viewModel = hubViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
