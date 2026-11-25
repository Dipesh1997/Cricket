package cricket.player.auction.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.ui.theme.*

@Composable
fun HelpGuideDialog(
    isDarkTheme: Boolean,
    isMasterAdmin: Boolean = false,
    onToggleMasterAdmin: (() -> Unit)? = null,
    onStartDemoMode: (() -> Unit)? = null,
    hideDemoData: Boolean = false,
    onToggleHideDemoData: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDarkTheme) StadiumCardDark else LightPitchCard
    val surfaceBg = if (isDarkTheme) StadiumSurface else LightPitchSurface
    val textColor = if (isDarkTheme) Color.White else LightPitchText
    val subTextColor = if (isDarkTheme) Color.LightGray else LightPitchSecondary
    val accentColor = if (isDarkTheme) IplGold else LightPitchPrimary
    val context = LocalContext.current
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTimestamp by remember { mutableStateOf(0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CRICKET HUB GUIDE",
                    color = accentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 0. Demo Mode Card
                Surface(
                    color = if (isDarkTheme) StadiumSurface else LightPitchSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, accentColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🎮 Try Demo Tournament (Offline)",
                                color = textColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "One-tap spin up a full offline test tournament with 4 teams & 20 marquee players to test live auction bidding, spin wheel, and live scoring without saving to Firebase!",
                            color = subTextColor,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Checkbox to Hide / Disable Demo Data
                        if (onToggleHideDemoData != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onToggleHideDemoData(!hideDemoData) }
                                    .background(surfaceBg.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Checkbox(
                                    checked = hideDemoData,
                                    onCheckedChange = { onToggleHideDemoData(it) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = accentColor,
                                        checkmarkColor = if (isDarkTheme) Color.Black else Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Hide Demo Data / Tournaments",
                                        color = textColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = if (hideDemoData) "Demo tournament hidden from list" else "Unchecked: Demo tournament visible in list",
                                        color = subTextColor,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                onStartDemoMode?.invoke()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = if (isDarkTheme) Color.Black else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("START DEMO TOURNAMENT (4 TEAMS / 20 PLAYERS)", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Text(
                    text = "Complete guide to every feature and workflow in the app:",
                    color = subTextColor,
                    fontSize = 12.sp
                )

                // 1. Tournaments
                HelpGuideSection(
                    icon = Icons.Default.EmojiEvents,
                    iconColor = if (isDarkTheme) IplGold else LightPitchPrimary,
                    title = "1. Creating & Managing Tournaments",
                    description = "• Tap '+ CREATE NEW' on the Home screen.\n• Enter Tournament Name, Total Purse (e.g. ₹100 Cr), Max Slots (e.g. 25), and Max Overseas Limit.\n• The app automatically generates unique Admin and Scorer Codes for live multi-device sharing.",
                    surfaceBg = surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // 2. Joining & Role Codes
                HelpGuideSection(
                    icon = Icons.Default.VpnKey,
                    iconColor = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                    title = "2. Multi-Device Joining & Role Codes",
                    description = "• Other devices tap 'ENTER CODE 🔑' on Home to join.\n• ADMIN CODE: Grants full tournament admin, staging, and bidding controls.\n• SPECTATOR CODE: Allows anyone to view live auction, scorecards, and standings in read-only mode.\n• TEAM CAPTAIN CODE: Binds the device to their team with dedicated bidding.\n• SCORER CODE: Unlocks live scoring and ball-by-ball updates.\n• Admins can remove captains, reset scorer codes, or cycle admin codes anytime in Settings.",
                    surfaceBg = surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // 3. Teams & Squads
                HelpGuideSection(
                    icon = Icons.Default.Shield,
                    iconColor = if (isDarkTheme) IplGold else LightPitchPrimary,
                    title = "3. Teams & Squads Dashboard",
                    description = "• In 'Teams & Squads', tap '+' to create custom teams with team colors and initial budget.\n• Copy and share the Invite Code with the team captain.\n• Admins can remove captains or reset invite codes directly from each team card.\n• Tap any team card to inspect acquired players, slot counts, and remaining purse.",
                    surfaceBg = surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // 4. Player Roster & Spin Wheel
                HelpGuideSection(
                    icon = Icons.Default.Casino,
                    iconColor = if (isDarkTheme) BidGreen else LightPitchPrimary,
                    title = "4. Player Roster & Spin Wheel",
                    description = "• Add players with their role, country, overseas status, and base price.\n• ⭐ FIXED SETS: Fixed set players must be in exact multiples of the total teams (e.g. 4, 8, 12 players for 4 teams) to spin for equal team opportunities.\n• In Auction Arena, use the 'SPIN WHEEL' to pick players with exciting live animations!",
                    surfaceBg = surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // 5. Live Auction & Captain Ready Check
                HelpGuideSection(
                    icon = Icons.Default.Gavel,
                    iconColor = if (isDarkTheme) IplGold else LightPitchPrimary,
                    title = "5. Live Auction Arena & Captain Readiness",
                    description = "• Captains must tap 'I AM READY 👍' before bidding starts.\n• Admin monitors captain readiness in real-time and taps 'START BIDDING 🚀'.\n• (If playing solo with only Admin, bidding is instantly enabled without waiting!).\n• Bid increments: +₹20L, +₹50L, +₹1.00Cr, +₹2.00Cr.\n• Admin options: 'SELL BID', 'SELL BASE PRICE', or 'MARK UNSOLD'.",
                    surfaceBg = surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // 6. Live Scorer & Matches
                HelpGuideSection(
                    icon = Icons.Default.SportsCricket,
                    iconColor = if (isDarkTheme) NeonBlue else LightPitchPrimary,
                    title = "6. Live Scorer & Scorecard",
                    description = "• Create matches, enter total overs, and score ball-by-ball (0, 1, 2, 3, 4, 6, Wides, No Balls, Byes, Leg Byes).\n• Tap 'WICKET!' to record batsman dismissals.\n• When all out, innings automatically ends.\n• Tap 'END INNINGS' with full 'UNDO INNINGS' protection if pressed accidentally!",
                    surfaceBg = surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // 7. Points Table & MVP Leaderboard
                HelpGuideSection(
                    icon = Icons.Default.Assessment,
                    iconColor = if (isDarkTheme) IplGold else LightPitchPrimary,
                    title = "7. Points Table & MVP Leaderboard",
                    description = "• Automatic Net Run Rate (NRR) calculation after every match.\n• MVP points awarded for runs, boundaries, strike rates, wickets, and economy.\n• Track top batsmen, top bowlers, and boundary leaders.",
                    surfaceBg = surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor
                )

                // 8. Auto Backup & Restore (Tapping 5x unlocks/disables Master Admin Mode)
                HelpGuideSection(
                    icon = if (isMasterAdmin) Icons.Default.Security else Icons.Default.Storage,
                    iconColor = if (isMasterAdmin) (if (isDarkTheme) IplGold else LightPitchPrimary) else (if (isDarkTheme) BidGreen else LightPitchPrimary),
                    title = if (isMasterAdmin) "8. Automatic Backup & Restore 👑 (MASTER ADMIN ACTIVE)" else "8. Automatic Backup & Restore",
                    description = if (isMasterAdmin) {
                        "• Tap 'Backup' in the top menu to save your full database directly to the 'Cricket' folder in File Manager.\n• Tap 'Restore' to reload and restore from 'Cricket/cricket_backup.json' anytime.\n\n👑 MASTER ADMIN IS ACTIVE: Full database control unlocked across all tournaments (Android & Web). View, update, edit, and delete any tournament.\n(Tap this card 5 times to disable Master Admin and restore device view)."
                    } else {
                        "• Tap 'Backup' in the top menu to save your full database directly to the 'Cricket' folder in File Manager.\n• Tap 'Restore' to automatically reload and restore from 'Cricket/cricket_backup.json' anytime without manual file selection!"
                    },
                    surfaceBg = if (isMasterAdmin) (if (isDarkTheme) IplGold.copy(alpha = 0.12f) else LightPitchContainer) else surfaceBg,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTimestamp > 2500L) {
                            tapCount = 1
                        } else {
                            tapCount++
                        }
                        lastTapTimestamp = now

                        if (tapCount >= 5) {
                            tapCount = 0
                            onToggleMasterAdmin?.invoke()
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = if (isDarkTheme) Color.Black else Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("GOT IT 👍", fontWeight = FontWeight.ExtraBold)
            }
        },
        containerColor = cardBg,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun HelpGuideSection(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    surfaceBg: Color,
    textColor: Color,
    subTextColor: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = surfaceBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                color = subTextColor,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

