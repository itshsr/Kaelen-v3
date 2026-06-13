package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush

enum class AppThemeVariant {
    ARCTIC_WOLF, // Dark Mode (Vibrant Blue Slate & Soft Icy Blue)
    SOLAR_WOLF   // Light Mode (Vibrant Coral Orange & Soft Indigo)
}

data class AppColorPalette(
    val bg: Color,
    val card: Color,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val shadowColor: Color,
    val primaryGradient: List<Color>,
    val headerGradient: List<Color>,
    val name: String,
    val isLight: Boolean
)

val ArcticWolfPalette = AppColorPalette(
    bg = Color(0xFF24293E),          // Deep sleek dark blue slate
    card = Color(0xFF2A2F45),        // Slightly lighter dark slate container for contrast
    primary = Color(0xFF8EBBFF),     // Vibrant light electric sky blue
    secondary = Color(0xFFCCCCCC),   // Cool steel silver
    tertiary = Color(0xFF6C63FF),    // Electric indigo
    text = Color(0xFFF4F5FC),        // Off-white/icy white high readability text
    muted = Color(0xFF94A3B8),       // Slate gray for subtexts
    border = Color(0xFF3F4462),      // Deep border outline to match cards
    shadowColor = Color(0x33000000), // Dark shadow
    primaryGradient = listOf(Color(0xFF8EBBFF), Color(0xFF6C63FF)), // Sky blue to electric indigo
    headerGradient = listOf(Color(0xFF1E2235), Color(0xFF24293E)),
    name = "Arctic Wolf",
    isLight = false
)

val SolarWolfPalette = AppColorPalette(
    bg = Color(0xFFE7F1F9),          // Soft pale blue-gray light background
    card = Color(0xFFFFFFFF),        // Pure clean white with soft shadow
    primary = Color(0xFFFE805D),     // Vibrant warm coral orange
    secondary = Color(0xFF6C63FF),   // Warm royal indigo
    tertiary = Color(0xFFFF6B35),    // Bright tangerine orange
    text = Color(0xFF26344F),        // Deep rich navy slate for reading text
    muted = Color(0xFF7B8387),       // Medium steel-gray
    border = Color(0xFFD0DFEE),      // Soft border outline to match white cards
    shadowColor = Color(0x1126344F), // Light cool navy shadow tint
    primaryGradient = listOf(Color(0xFFFE805D), Color(0xFFFFB09C)), // Coral to soft coral pink gradient
    headerGradient = listOf(Color(0xFFD7E5F0), Color(0xFFE7F1F9)),
    name = "Solar Wolf",
    isLight = true
)

object ThemeManager {
    var activeVariant = mutableStateOf(AppThemeVariant.ARCTIC_WOLF)
    var isSideBySide = mutableStateOf(false)
}

val LocalAppColors = staticCompositionLocalOf { ArcticWolfPalette }

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
    get() = if (LocalAppColors.current.isLight) Color.White else Color(0xFF1E2235)
