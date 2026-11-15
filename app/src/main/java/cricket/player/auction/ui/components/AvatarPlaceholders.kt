package cricket.player.auction.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs

/**
 * 20 Curated Lightweight Vector Icons for Teams (Zero APK bloat, built-in vector icons)
 */
val TeamPlaceholderIcons: List<ImageVector> = listOf(
    Icons.Default.Shield,
    Icons.Default.EmojiEvents,
    Icons.Default.SportsCricket,
    Icons.Default.SportsBaseball,
    Icons.Default.Whatshot,
    Icons.Default.FlashOn,
    Icons.Default.Bolt,
    Icons.Default.Star,
    Icons.Default.Diamond,
    Icons.Default.MilitaryTech,
    Icons.Default.Flag,
    Icons.Default.Groups,
    Icons.Default.WorkspacePremium,
    Icons.Default.Public,
    Icons.Default.Flare,
    Icons.Default.AutoAwesome,
    Icons.Default.Castle,
    Icons.Default.RocketLaunch,
    Icons.Default.LocalFireDepartment,
    Icons.Default.Security
)

/**
 * 50 Curated Lightweight Vector Icons for Players (Zero APK bloat, built-in vector icons)
 */
val PlayerPlaceholderIcons: List<ImageVector> = listOf(
    Icons.Default.Person,
    Icons.Default.SportsCricket,
    Icons.Default.DirectionsRun,
    Icons.Default.FitnessCenter,
    Icons.Default.SportsBaseball,
    Icons.Default.SportsHandball,
    Icons.Default.Star,
    Icons.Default.Bolt,
    Icons.Default.FlashOn,
    Icons.Default.Whatshot,
    Icons.Default.MilitaryTech,
    Icons.Default.WorkspacePremium,
    Icons.Default.EmojiEvents,
    Icons.Default.Face,
    Icons.Default.Psychology,
    Icons.Default.Speed,
    Icons.Default.Shield,
    Icons.Default.Timer,
    Icons.Default.AccessibilityNew,
    Icons.Default.SelfImprovement,
    Icons.Default.SportsScore,
    Icons.Default.TrackChanges,
    Icons.Default.Sports,
    Icons.Default.ThumbUp,
    Icons.Default.Grade,
    Icons.Default.AutoAwesome,
    Icons.Default.Flare,
    Icons.Default.Stars,
    Icons.Default.Diamond,
    Icons.Default.Handshake,
    Icons.Default.CenterFocusStrong,
    Icons.Default.Visibility,
    Icons.Default.SportsKabaddi,
    Icons.Default.SportsTennis,
    Icons.Default.EmojiEmotions,
    Icons.Default.PersonPin,
    Icons.Default.CoPresent,
    Icons.Default.DirectionsWalk,
    Icons.Default.Surfing,
    Icons.Default.Skateboarding,
    Icons.Default.Kayaking,
    Icons.Default.Rowing,
    Icons.Default.NordicWalking,
    Icons.Default.Paragliding,
    Icons.Default.Snowboarding,
    Icons.Default.Kitesurfing,
    Icons.Default.SportsGymnastics,
    Icons.Default.SportsMartialArts,
    Icons.Default.SportsMotorsports,
    Icons.Default.SportsGolf
)

/**
 * Deterministically picks one of the 20 team placeholder icons based on team id/name.
 */
fun getTeamPlaceholderIcon(teamId: String, name: String = ""): ImageVector {
    val seed = (teamId + name).ifBlank { "team_default" }
    val index = abs(seed.hashCode()) % TeamPlaceholderIcons.size
    return TeamPlaceholderIcons[index]
}

/**
 * Deterministically picks one of the 50 player placeholder icons based on player id/name.
 */
fun getPlayerPlaceholderIcon(playerId: String, name: String = ""): ImageVector {
    val seed = (playerId + name).ifBlank { "player_default" }
    val index = abs(seed.hashCode()) % PlayerPlaceholderIcons.size
    return PlayerPlaceholderIcons[index]
}
