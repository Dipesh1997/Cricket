package cricket.player.auction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

enum class ListSubTab(val title: String, val icon: ImageVector, val key: String) {
    TEAMS("Teams", Icons.Default.Shield, "teams"),
    PLAYERS("Players", Icons.Default.Person, "players"),
    POINTS("Points", Icons.Default.Assessment, "points"),
    MVP("MVP", Icons.Default.Star, "mvp");

    companion object {
        fun fromKey(key: String?): ListSubTab {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.title.equals(key, ignoreCase = true) } ?: TEAMS
        }
    }
}

@Composable
fun ListsScreen(
    viewModel: AuctionViewModel,
    initialTab: String? = null
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val accentColor = if (isDarkTheme) IplGold else LightPitchPrimary
    val surfaceColor = if (isDarkTheme) StadiumCardDark else LightPitchSurface
    val unselectedColor = if (isDarkTheme) Color.Gray else LightPitchSecondary

    var selectedTab by remember(initialTab) {
        mutableStateOf(ListSubTab.fromKey(initialTab))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) StadiumDark else LightPitchBackground)
    ) {
        // Sub-tabs Navigation Header
        PrimaryTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = surfaceColor,
            contentColor = accentColor,
            indicator = {
                TabRowDefaults.PrimaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(selectedTab.ordinal),
                    height = 3.dp,
                    color = accentColor
                )
            },
            divider = {
                HorizontalDivider(color = if (isDarkTheme) StadiumSurface else LightPitchContainer, thickness = 1.dp)
            }
        ) {
            ListSubTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) accentColor else unselectedColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            text = tab.title,
                            color = if (isSelected) accentColor else unselectedColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Sub-tab Active Screen Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                ListSubTab.TEAMS -> TeamsScreen(viewModel = viewModel)
                ListSubTab.PLAYERS -> PlayersScreen(viewModel = viewModel)
                ListSubTab.POINTS -> PointsTableScreen(viewModel = viewModel)
                ListSubTab.MVP -> MvpLeaderboardScreen(viewModel = viewModel)
            }
        }
    }
}
