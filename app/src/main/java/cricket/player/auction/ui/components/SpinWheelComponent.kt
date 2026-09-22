package cricket.player.auction.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cricket.player.auction.model.Player
import cricket.player.auction.model.PlayerStatus
import cricket.player.auction.ui.theme.IplGold
import cricket.player.auction.ui.theme.NeonBlue
import cricket.player.auction.ui.theme.StadiumCardDark
import cricket.player.auction.ui.theme.StadiumDark
import cricket.player.auction.ui.theme.StadiumSurface
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val WheelColors = listOf(
    Color(0xFFFFD700), // IPL Gold
    Color(0xFF00D2FF), // Neon Blue
    Color(0xFF9C27B0), // Purple
    Color(0xFFFF5722), // Orange
    Color(0xFF4CAF50), // Green
    Color(0xFFE91E63), // Pink
    Color(0xFF3F51B5), // Indigo
    Color(0xFFFFC107)  // Amber
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinWheelModal(
    players: List<Player>,
    onDismiss: () -> Unit,
    onPlayerSelected: (Player) -> Unit
) {
    var isSpinning by remember { mutableStateOf(false) }
    var selectedPlayer by remember { mutableStateOf<Player?>(null) }
    val rotationAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()

    // Determine Fixed Category vs Open Category Priority
    val upNextPlayers = remember(players) {
        players.filter { it.status == PlayerStatus.UP_NEXT }
    }

    val fixedSetPlayers = remember(upNextPlayers) {
        upNextPlayers.filter { it.isFixedSet }
    }

    val openCategoryPlayers = remember(upNextPlayers) {
        upNextPlayers.filter { !it.isFixedSet }
    }

    val isFixedSetStage = fixedSetPlayers.isNotEmpty()
    val availablePlayers = if (isFixedSetStage) fixedSetPlayers else openCategoryPlayers

    AlertDialog(
        onDismissRequest = { if (!isSpinning) onDismiss() },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Casino, contentDescription = null, tint = IplGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LUCKY PLAYER SPIN WHEEL",
                        color = IplGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                StatChip(
                    text = if (isFixedSetStage) "FIXED CATEGORY SET (${fixedSetPlayers.size} Left)" else "OPEN CATEGORY (${openCategoryPlayers.size} Left)",
                    backgroundColor = if (isFixedSetStage) IplGold.copy(alpha = 0.2f) else NeonBlue.copy(alpha = 0.2f),
                    textColor = if (isFixedSetStage) IplGold else NeonBlue,
                    icon = if (isFixedSetStage) {
                        { Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = IplGold, modifier = Modifier.size(12.dp)) }
                    } else null
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (availablePlayers.isEmpty()) {
                    Text(
                        text = "No 'UP_NEXT' players available in roster to spin!",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // Circular Avatars Bar of Wheel Players
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availablePlayers) { player ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = 2.dp,
                                            color = if (player.isFixedSet) IplGold else NeonBlue,
                                            shape = CircleShape
                                        )
                                        .background(StadiumDark)
                                ) {
                                    DriveImage(
                                        url = player.imageUrl,
                                        contentDescription = player.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Text(
                                    text = player.name.take(6) + "…",
                                    color = Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Main Spin Canvas
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val canvasSize = size.minDimension
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = canvasSize / 2f
                            val sliceAngle = 360f / availablePlayers.size

                            rotate(rotationAnim.value, pivot = center) {
                                availablePlayers.forEachIndexed { index, player ->
                                    val startAngle = index * sliceAngle
                                    val color = WheelColors[index % WheelColors.size]

                                    // Draw Slice
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sliceAngle,
                                        useCenter = true,
                                        size = Size(canvasSize, canvasSize),
                                        topLeft = Offset((size.width - canvasSize) / 2f, (size.height - canvasSize) / 2f)
                                    )

                                    // Draw Slice Border
                                    drawArc(
                                        color = StadiumDark,
                                        startAngle = startAngle,
                                        sweepAngle = sliceAngle,
                                        useCenter = true,
                                        style = Stroke(width = 3f),
                                        size = Size(canvasSize, canvasSize),
                                        topLeft = Offset((size.width - canvasSize) / 2f, (size.height - canvasSize) / 2f)
                                    )

                                    // Draw Text on Slice
                                    val midAngleRad = (startAngle + sliceAngle / 2f) * (PI / 180f)
                                    val textDistance = radius * 0.6f
                                    val textX = center.x + textDistance * cos(midAngleRad).toFloat()
                                    val textY = center.y + textDistance * sin(midAngleRad).toFloat()

                                    val displayName = if (player.name.length > 9) player.name.take(8) + "…" else player.name
                                    val textLayoutResult = textMeasurer.measure(
                                        text = displayName,
                                        style = TextStyle(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )

                                    drawText(
                                        textLayoutResult = textLayoutResult,
                                        topLeft = Offset(
                                            textX - textLayoutResult.size.width / 2f,
                                            textY - textLayoutResult.size.height / 2f
                                        )
                                    )
                                }
                            }

                            // Center Circle Badge
                            drawCircle(color = StadiumDark, radius = radius * 0.25f, center = center)
                            drawCircle(color = IplGold, radius = radius * 0.20f, center = center)

                            // Top Pointer Needle
                            val path = Path().apply {
                                moveTo(center.x, 0f)
                                lineTo(center.x - 14f, -20f)
                                lineTo(center.x + 14f, -20f)
                                close()
                            }
                            drawPath(path = path, color = IplGold)
                        }

                        // Wheel Center Icon
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Selected Winner Card with Circular Cutout Image
                    if (selectedPlayer != null) {
                        Surface(
                            color = StadiumSurface,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, IplGold)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, IplGold, CircleShape)
                                        .background(StadiumDark)
                                ) {
                                    DriveImage(
                                        url = selectedPlayer!!.imageUrl,
                                        contentDescription = selectedPlayer!!.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedPlayer!!.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "${selectedPlayer!!.role.label} • Base: ${selectedPlayer!!.basePrice}Cr",
                                        color = IplGold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (availablePlayers.isNotEmpty()) {
                Button(
                    onClick = {
                        if (!isSpinning) {
                            isSpinning = true
                            scope.launch {
                                val targetIndex = availablePlayers.indices.random()
                                val sliceAngle = 360f / availablePlayers.size
                                val targetAngle = 360f * 6 - (targetIndex * sliceAngle + sliceAngle / 2f) + 270f

                                rotationAnim.animateTo(
                                    targetValue = targetAngle,
                                    animationSpec = tween(durationMillis = 3500, easing = FastOutSlowInEasing)
                                )

                                val winner = availablePlayers[targetIndex]
                                selectedPlayer = winner
                                isSpinning = false
                                onPlayerSelected(winner)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IplGold, contentColor = Color.Black),
                    enabled = !isSpinning
                ) {
                    Text(if (isSpinning) "SPINNING..." else "SPIN WHEEL!", fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss() },
                enabled = !isSpinning
            ) {
                Text("Close", color = Color.Gray)
            }
        },
        containerColor = StadiumCardDark
    )
}
