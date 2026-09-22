package com.cricket.auction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cricket.auction.data.model.CurrencyFormat
import com.cricket.auction.data.model.PlayerAuctionStatus
import com.cricket.auction.data.model.PlayerRole
import com.cricket.auction.ui.theme.RoleAllRounderColor
import com.cricket.auction.ui.theme.RoleBatsmanColor
import com.cricket.auction.ui.theme.RoleBowlerColor
import com.cricket.auction.ui.theme.RoleWicketKeeperColor
import com.cricket.auction.ui.theme.StatusSoldColor
import com.cricket.auction.ui.theme.StatusUnsoldColor
import com.cricket.auction.ui.theme.StatusUpcomingColor
import com.cricket.auction.util.AuctionUtils

@Composable
fun RoleBadge(
    role: PlayerRole,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (role) {
        PlayerRole.BATSMAN -> RoleBatsmanColor to Color.White
        PlayerRole.BOWLER -> RoleBowlerColor to Color.White
        PlayerRole.ALL_ROUNDER -> RoleAllRounderColor to Color.White
        PlayerRole.WICKET_KEEPER -> RoleWicketKeeperColor to Color.White
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, bgColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(bgColor)
            )
            Text(
                text = role.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = bgColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: PlayerAuctionStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, text) = when (status) {
        PlayerAuctionStatus.SOLD -> StatusSoldColor to "SOLD"
        PlayerAuctionStatus.UNSOLD -> StatusUnsoldColor to "UNSOLD"
        PlayerAuctionStatus.UPCOMING -> StatusUpcomingColor to "UPCOMING"
        PlayerAuctionStatus.IN_AUCTION -> MaterialTheme.colorScheme.secondary to "LIVE"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (status == PlayerAuctionStatus.IN_AUCTION) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun OverseasBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "Overseas",
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                text = "Overseas",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun TeamAvatar(
    shortCode: String,
    colorHex: String,
    size: Dp = 40.dp,
    textSize: Int = 14
) {
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(parsedColor)
            .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = shortCode.take(3).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = textSize.sp
        )
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Default.SportsCricket,
    title: String,
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(buttonText)
            }
        }
    }
}
