package cricket.player.auction.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import cricket.player.auction.data.remote.GoogleDriveHelper
import cricket.player.auction.model.Team
import cricket.player.auction.model.formatIplCurrency
import cricket.player.auction.ui.theme.*

@Composable
fun AdminJoinButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Enter Joining Code 🔑"
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = IplGold,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(22.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DriveImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Person,
    fallbackTint: Color? = null
) {
    val isLight = MaterialTheme.colorScheme.background == LightPitchBackground
    val defaultTint = fallbackTint ?: if (isLight) LightPitchPrimary else IplGold
    val placeholderBg = if (isLight) LightPitchSurface else StadiumSurface

    if (url.isBlank()) {
        Box(
            modifier = modifier
                .background(placeholderBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = defaultTint,
                modifier = Modifier.size(36.dp)
            )
        }
        return
    }

    val directUrl = GoogleDriveHelper.toDirectImageUrl(url)

    SubcomposeAsyncImage(
        model = directUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(placeholderBg),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = defaultTint, modifier = Modifier.size(24.dp))
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(placeholderBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = contentDescription,
                    tint = defaultTint,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    )
}

@Composable
fun StatChip(
    text: String,
    backgroundColor: Color = StadiumSurface,
    textColor: Color = Color.White,
    icon: (@Composable () -> Unit)? = null
) {
    val isLight = MaterialTheme.colorScheme.background == LightPitchBackground
    val effectiveTextColor = if (isLight) textColor.toAdaptiveText(isDark = false) else textColor
    val effectiveBgColor = if (isLight && (backgroundColor == StadiumSurface || backgroundColor == StadiumCardDark)) LightPitchSurface else backgroundColor

    Surface(
        color = effectiveBgColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, effectiveTextColor.copy(alpha = if (isLight) 0.35f else 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = effectiveTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TeamBadge(
    team: Team,
    modifier: Modifier = Modifier,
    showPurse: Boolean = false
) {
    val isLight = MaterialTheme.colorScheme.background == LightPitchBackground
    val teamColor = parseColorHex(team.primaryColorHex)
    val teamIcon = getTeamPlaceholderIcon(team.id, team.name)

    Surface(
        modifier = modifier,
        color = teamColor.copy(alpha = if (isLight) 0.15f else 0.25f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, teamColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(teamColor),
                contentAlignment = Alignment.Center
            ) {
                if (team.logoUrl.isNotBlank()) {
                    DriveImage(
                        url = team.logoUrl,
                        contentDescription = team.name,
                        modifier = Modifier.fillMaxSize(),
                        fallbackIcon = teamIcon
                    )
                } else {
                    Icon(
                        imageVector = teamIcon,
                        contentDescription = team.name,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = team.name,
                    color = if (isLight) LightPitchText else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (showPurse) {
                    Text(
                        text = "Purse: ${team.remainingPurse.formatIplCurrency()}",
                        color = if (isLight) LightPitchText else IplGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun BidIncrementButton(
    label: String,
    amountCr: Double,
    availablePurse: Double,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val canAfford = availablePurse >= amountCr
    val buttonEnabled = isEnabled && canAfford

    Button(
        onClick = onClick,
        enabled = buttonEnabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = BidGreen,
            contentColor = Color.Black,
            disabledContainerColor = Color.DarkGray,
            disabledContentColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(44.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun PurseProgressBar(
    spent: Double,
    total: Double,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == LightPitchBackground
    val remaining = (total - spent).coerceAtLeast(0.0)
    val fraction = if (total > 0) (spent / total).toFloat().coerceIn(0f, 1f) else 0f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spent: ${spent.formatIplCurrency()}",
                color = if (isLight) LightPitchSecondary else Color.LightGray,
                fontSize = 12.sp
            )
            Text(
                text = "Remaining: ${remaining.formatIplCurrency()}",
                color = if (isLight) LightPitchText else IplGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (fraction > 0.85f) UnsoldRed else if (isLight) LightPitchPrimary else NeonBlue,
            trackColor = if (isLight) LightPitchContainer else StadiumSurface
        )
    }
}

fun parseColorHex(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000 or colorInt)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        IplGold
    }
}

@Composable
fun PhotoUploaderField(
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
    label: String = "Photo / Logo Image",
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Person,
    modifier: Modifier = Modifier
) {
    val isLight = MaterialTheme.colorScheme.background == LightPitchBackground
    val accent = if (isLight) LightPitchPrimary else IplGold
    val btnContentColor = if (isLight) Color.White else Color.Black
    val galleryLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onImageUrlChange(uri.toString())
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = label, color = if (isLight) LightPitchText else Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

        // Photo Preview Box & Upload Trigger Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Thumbnail Preview
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLight) LightPitchSurface else StadiumSurface)
                    .border(1.5.dp, accent, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    DriveImage(
                        url = imageUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize(),
                        fallbackIcon = placeholderIcon
                    )
                } else {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = null,
                        tint = if (isLight) LightPitchPrimary else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = btnContentColor),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose Photo from Device", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = onImageUrlChange,
                    placeholder = { Text("Or paste Google Drive URL / ID", color = Color.Gray, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = if (isLight) LightPitchContainer else Color.Gray.copy(alpha = 0.5f),
                        focusedTextColor = if (isLight) LightPitchText else Color.White,
                        unfocusedTextColor = if (isLight) LightPitchText else Color.White
                    )
                )
            }
        }
    }
}
