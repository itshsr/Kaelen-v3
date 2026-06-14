package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf

enum class AppThemeVariant {
    INFERNO, SOVEREIGN, NEXUS, APEX
}

data class AppColorPalette(
    val bg: Color,
    val card: Color,
    val primary: Color, // Accent highlight
    val secondary: Color, // Darker accent
    val tertiary: Color, // Gold or Accent 2
    val text: Color,
    val muted: Color,
    val border: Color,
    val shadowColor: Color,
    val primaryGradient: List<Color>,
    val headerGradient: List<Color>,
    val name: String,
    val isLight: Boolean
)

val InfernoPalette = AppColorPalette(
    bg = Color(0xFF0A0A0A),
    card = Color(0xFF1C1C1C),
    primary = Color(0xFFFF6B00), 
    secondary = Color(0xFF8B0000), 
    tertiary = Color(0xFFFFD700), 
    text = Color(0xFFF5F5F5),
    muted = Color(0xFF9E9E9E),
    border = Color(0xFF333333),
    shadowColor = Color(0x99000000),
    primaryGradient = listOf(Color(0xFF8B0000), Color(0xFFFF6B00)),
    headerGradient = listOf(Color(0xFF0A0A0A), Color(0xFF1C1C1C)),
    name = "INFERNO",
    isLight = false
)

val SovereignPalette = AppColorPalette(
    bg = Color(0xFF0D0B14),
    card = Color(0xFF1A1525),
    primary = Color(0xFFFF006E),
    secondary = Color(0xFF4A0080),
    tertiary = Color(0xFFFFC300),
    text = Color(0xFFE6E6FA),
    muted = Color(0xFF8B8A9F),
    border = Color(0xFF2B223D),
    shadowColor = Color(0x99000000),
    primaryGradient = listOf(Color(0xFF4A0080), Color(0xFFFF006E)),
    headerGradient = listOf(Color(0xFF0D0B14), Color(0xFF1A1525)),
    name = "SOVEREIGN",
    isLight = false
)

val NexusPalette = AppColorPalette(
    bg = Color(0xFF080C0C),
    card = Color(0xFF121A1A),
    primary = Color(0xFFFF4500),
    secondary = Color(0xFF004D4D),
    tertiary = Color(0xFFF5F0E8),
    text = Color(0xFFE0ECEC),
    muted = Color(0xFF6B8080),
    border = Color(0xFF1A2626),
    shadowColor = Color(0x99000000),
    primaryGradient = listOf(Color(0xFF004D4D), Color(0xFFFF4500)),
    headerGradient = listOf(Color(0xFF080C0C), Color(0xFF121A1A)),
    name = "NEXUS",
    isLight = false
)

val ApexPalette = AppColorPalette(
    bg = Color(0xFF050505),
    card = Color(0xFF111111),
    primary = Color(0xFFFF6000),
    secondary = Color(0xFFFF0080),
    tertiary = Color(0xFFFFFFFF),
    text = Color(0xFFF0F0F0),
    muted = Color(0xFF858585),
    border = Color(0xFF222222),
    shadowColor = Color(0x99000000),
    primaryGradient = listOf(Color(0xFFFF0080), Color(0xFFFF6000)),
    headerGradient = listOf(Color(0xFF050505), Color(0xFF111111)),
    name = "APEX",
    isLight = false
)

object ThemeManager {
    var activeVariant = mutableStateOf(AppThemeVariant.INFERNO)
    var isSideBySide = mutableStateOf(false)
}

val LocalAppColors = staticCompositionLocalOf { InfernoPalette }

val CosmicNavy: Color
    @Composable
    get() = LocalAppColors.current.bg

val CosmicCard: Color
    @Composable
    get() = LocalAppColors.current.card

val ElectricCyan: Color
    @Composable
    get() = LocalAppColors.current.primary

val DeepViolet: Color
    @Composable
    get() = LocalAppColors.current.secondary

val HotPink: Color
    @Composable
    get() = LocalAppColors.current.tertiary

val CoolWhite: Color
    @Composable
    get() = LocalAppColors.current.text

val TextMuted: Color
    @Composable
    get() = LocalAppColors.current.muted

val BorderColor: Color
    @Composable
    get() = LocalAppColors.current.border

// Material 3 mappings
val Purple80: Color
    @Composable
    get() = ElectricCyan

val PurpleGrey80: Color
    @Composable
    get() = DeepViolet

val Pink80: Color
    @Composable
    get() = HotPink

val Purple40: Color
    @Composable
    get() = ElectricCyan

val PurpleGrey40: Color
    @Composable
    get() = DeepViolet

val Pink40: Color
    @Composable
    get() = HotPink

val OnPrimaryColor: Color
    @Composable
    get() = Color.White
