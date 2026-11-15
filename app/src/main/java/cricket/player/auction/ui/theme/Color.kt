package cricket.player.auction.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// IPL Signature Colors (Dark Stadium Theme)
val IplGold = Color(0xFFFFD700)
val IplGoldVariant = Color(0xFFFFC107)
val StadiumDark = Color(0xFF0B0E17)
val StadiumCardDark = Color(0xFF161C2E)
val StadiumSurface = Color(0xFF1E2640)
val NeonBlue = Color(0xFF00D2FF)
val BidGreen = Color(0xFF00E676)
val UnsoldRed = Color(0xFFFF5252)

// Proper Clean Light Theme Palette (Clean White & Slate, No Grass Tint)
val LightPitchBackground = Color(0xFFF8FAFC) // Crisp Off-White / Soft Slate 50
val LightPitchSurface = Color(0xFFF1F5F9)    // Soft Neutral Slate 100 Surface
val LightPitchCard = Color(0xFFFFFFFF)       // Crisp Pure White Card
val LightPitchPrimary = Color(0xFF1E3A8A)    // Stadium Royal Navy Blue
val LightPitchSecondary = Color(0xFF475569)  // Slate 600 Secondary Text & Icons
val LightPitchText = Color(0xFF0F172A)       // Rich Slate 900 High-Contrast Text
val LightPitchContainer = Color(0xFFE2E8F0)  // Clean Border & Divider (Slate 200)

// High-contrast Dark Colors for Light Mode (Clean & Highly Readable, replacing washed out yellow/blue)
val LightModeText = Color(0xFF0F172A)        // Deep Rich Slate 900
val LightModeMuted = Color(0xFF334155)       // Slate 700 (High-contrast secondary)
val LightModeNavy = Color(0xFF1E3A8A)        // Deep Royal Navy Blue (instead of neon cyan)
val LightModeGoldDark = Color(0xFF78350F)    // Deep Amber Brown / Dark Gold (high-contrast instead of yellow)
val LightModeGreenDark = Color(0xFF14532D)   // Deep Emerald Green (instead of bright green)
val LightModeRedDark = Color(0xFF991B1B)     // Deep Crimson Red (instead of light red)

/**
 * Returns a high-contrast dark color when in light mode so that text is never
 * rendered in washed-out yellow or bright neon blue against light surfaces.
 */
fun Color.toAdaptiveText(isDark: Boolean): Color {
    if (isDark) return this
    return when (this) {
        IplGold, IplGoldVariant -> Color(0xFF0F172A)
        NeonBlue -> Color(0xFF1E3A8A)
        BidGreen -> Color(0xFF14532D)
        UnsoldRed -> Color(0xFF991B1B)
        Color.White, Color.LightGray -> Color(0xFF0F172A)
        Color.Yellow, Color.Cyan -> Color(0xFF0F172A)
        else -> {
            if (this.luminance() > 0.45f) Color(0xFF0F172A) else this
        }
    }
}

// IPL Team Colors
val TeamCSK = Color(0xFFF9CD05)
val TeamMI = Color(0xFF004BA0)
val TeamRCB = Color(0xFFEC1C24)
val TeamKKR = Color(0xFF3A225D)
val TeamGT = Color(0xFF1B2133)
val TeamRR = Color(0xFFEA1A85)
val TeamSRH = Color(0xFFF26522)
val TeamDC = Color(0xFF0078BC)
val TeamLSG = Color(0xFFA7D5ED)
val TeamPBKS = Color(0xFFDD1F2D)

// Standard Dark Palette
val PrimaryDark = IplGold
val SecondaryDark = NeonBlue
val BackgroundDark = StadiumDark
val SurfaceDark = StadiumCardDark
val OnPrimaryDark = Color.Black
val OnBackgroundDark = Color.White
val OnSurfaceDark = Color.White
