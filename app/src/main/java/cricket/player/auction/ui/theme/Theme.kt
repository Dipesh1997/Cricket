package cricket.player.auction.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = IplGoldVariant,
    onPrimaryContainer = Color.Black,
    secondary = SecondaryDark,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = StadiumSurface,
    onSurfaceVariant = Color.LightGray,
    error = UnsoldRed
)

private val LightColorScheme = lightColorScheme(
    primary = LightPitchPrimary,
    onPrimary = Color.White,
    primaryContainer = LightPitchContainer,
    onPrimaryContainer = LightPitchSecondary,
    secondary = LightPitchSecondary,
    onSecondary = Color.White,
    background = LightPitchBackground,
    onBackground = LightPitchText,
    surface = LightPitchCard,
    onSurface = LightPitchText,
    surfaceVariant = LightPitchSurface,
    onSurfaceVariant = LightPitchText,
    error = UnsoldRed
)

@Composable
fun CricketTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}