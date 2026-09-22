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
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Sign in with Google"
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF3C4043)
        ),
        shape = RoundedCornerShape(22.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4285F4)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = Color(0xFF3C4043),
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
    contentScale: ContentScale = ContentScale.Crop
) {
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
                    .background(StadiumSurface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = IplGold, modifier = Modifier.size(24.dp))
            }
        },
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(StadiumSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = IplGold,
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
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
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
                color = textColor,
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
    val teamColor = parseColorHex(team.primaryColorHex)

    Surface(
        modifier = modifier,
        color = teamColor.copy(alpha = 0.25f),
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
                Text(
                    text = team.shortCode,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = team.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (showPurse) {
                    Text(
                        text = "Purse: ${team.remainingPurse.formatIplCurrency()}",
                        color = IplGold,
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
    val remaining = (total - spent).coerceAtLeast(0.0)
    val fraction = if (total > 0) (spent / total).toFloat().coerceIn(0f, 1f) else 0f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Spent: ${spent.formatIplCurrency()}",
                color = Color.LightGray,
                fontSize = 12.sp
            )
            Text(
                text = "Remaining: ${remaining.formatIplCurrency()}",
                color = IplGold,
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
            color = if (fraction > 0.85f) UnsoldRed else NeonBlue,
            trackColor = StadiumSurface
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
    modifier: Modifier = Modifier
) {
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
        Text(text = label, color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

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
                    .background(StadiumSurface)
                    .border(1.5.dp, IplGold, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotBlank()) {
                    DriveImage(
                        url = imageUrl,
                        contentDescription = "Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Gray,
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
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
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
                        focusedBorderColor = IplGold,
                        unfocusedBorderColor = Color.DarkGray,
                        focusedContainerColor = StadiumSurface,
                        unfocusedContainerColor = StadiumSurface,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        }
    }
}
