package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf

enum class AppThemeVariant {
    INFERNO, NEXUS, ARCTIC_FOX, CRIMSON_WOLF
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
    muted = Color(0xFFC0C0C0),
    border = Color(0xFF333333),
    shadowColor = Color(0x99000000),
    primaryGradient = listOf(Color(0xFF8B0000), Color(0xFFFF6B00)),
    headerGradient = listOf(Color(0xFF0A0A0A), Color(0xFF1C1C1C)),
    name = "INFERNO",
    isLight = false
)

val NexusPalette = AppColorPalette(
    bg = Color(0xFF080C0C),
    card = Color(0xFF121A1A),
    primary = Color(0xFFFF4500),
    secondary = Color(0xFF004D4D),
    tertiary = Color(0xFFF5F0E8),
    text = Color(0xFFE0ECEC),
    muted = Color(0xFFB0C4C4),
    border = Color(0xFF1A2626),
    shadowColor = Color(0x99000000),
    primaryGradient = listOf(Color(0xFF004D4D), Color(0xFFFF4500)),
    headerGradient = listOf(Color(0xFF080C0C), Color(0xFF121A1A)),
    name = "NEXUS",
    isLight = false
)

val ArcticFoxPalette = AppColorPalette(
    bg = Color(0xFFFFFFFF),
    card = Color(0xFFF5F5F5),
    primary = Color(0xFFFF6B9D),
    secondary = Color(0xFFC44DFF),
    tertiary = Color(0xFFFF6B9D),
    text = Color(0xFF1A1A1A),
    muted = Color(0xFF555555),
    border = Color(0xFFE2E8F0),
    shadowColor = Color(0x1A000000),
    primaryGradient = listOf(Color(0xFFFF6B9D), Color(0xFFC44DFF)),
    headerGradient = listOf(Color(0xFFFFFFFF), Color(0xFFF5F5F5)),
    name = "ARCTIC FOX",
    isLight = true
)

val CrimsonWolfPalette = AppColorPalette(
    bg = Color(0xFF0A0000),
    card = Color(0xFF1A0505),
    primary = Color(0xFFFF0000),
    secondary = Color(0xFFFF6B00),
    tertiary = Color(0xFFFF3D00),
    text = Color(0xFFFFFFFF),
    muted = Color(0xFFFFAAAA),
    border = Color(0xFF3D0C0C),
    shadowColor = Color(0x99000000),
    primaryGradient = listOf(Color(0xFFFF0000), Color(0xFFFF6B00)),
    headerGradient = listOf(Color(0xFF0A0000), Color(0xFF1A0505)),
    name = "CRIMSON WOLF",
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
