package com.cricket.auction.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color.White,
    primaryContainer = GreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = GoldSecondary,
    onSecondaryContainer = Color.Black,
    background = DarkBackground,
    onBackground = Color(0xFFECEFF1),
    surface = DarkSurface,
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCFD8DC),
    outline = Color(0xFF455A64)
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E7DD),
    onPrimaryContainer = Color(0xFF0F5132),
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFF3CD),
    onSecondaryContainer = Color(0xFF664D03),
    background = LightBackground,
    onBackground = Color(0xFF1B2320),
    surface = LightSurface,
    onSurface = Color(0xFF1B2320),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF37474F),
    outline = Color(0xFFB0BEC5)
)

@Composable
fun CricketAuctionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
