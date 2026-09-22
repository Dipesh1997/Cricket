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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.PointsTableEntry
import cricket.player.auction.ui.components.parseColorHex
import cricket.player.auction.ui.theme.*
import cricket.player.auction.viewmodel.AuctionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PointsTableScreen(viewModel: AuctionViewModel) {
    val pointsTable by viewModel.pointsTable.collectAsState()
    val activeTournament by viewModel.activeTournament.collectAsState()

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
                text = activeTournament?.name ?: "IPL Tournament",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Automated Standings & Net Run Rate (NRR)",
                color = Color.Gray,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Table Header Bar
            Surface(
                color = StadiumCardDark,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", color = IplGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(24.dp))
                    Text("TEAM", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("P", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("W", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("L", color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(28.dp))
                    Text("PTS", color = IplGold, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    Text("NRR", color = NeonBlue, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.width(54.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (pointsTable.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No match records found", color = Color.LightGray, fontSize = 14.sp)
                        Text("Record live matches to update standings", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(pointsTable) { index, entry ->
                        PointsTableRow(rank = index + 1, entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
fun PointsTableRow(rank: Int, entry: PointsTableEntry) {
    val teamColor = parseColorHex(entry.primaryColorHex)
    val nrrFormatted = if (entry.netRunRate >= 0) "+${String.format("%.3f", entry.netRunRate)}" else String.format("%.3f", entry.netRunRate)

    Card(
        colors = CardDefaults.cardColors(containerColor = StadiumCardDark),
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
                color = if (rank <= 4) IplGold else StadiumSurface,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$rank",
                        color = if (rank <= 4) Color.Black else Color.White,
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Text("${entry.matchesPlayed}", color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Text("${entry.won}", color = BidGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Text("${entry.lost}", color = UnsoldRed, fontSize = 12.sp, modifier = Modifier.width(28.dp))
            Text("${entry.points}", color = IplGold, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.width(36.dp))
            Text(nrrFormatted, color = NeonBlue, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, modifier = Modifier.width(54.dp))
        }
    }
}
