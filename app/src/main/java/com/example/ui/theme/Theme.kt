package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    themeVariant: AppThemeVariant = ThemeManager.activeVariant.value,
    content: @Composable () -> Unit
) {
    val palette = if (themeVariant == AppThemeVariant.ARCTIC_WOLF) ArcticWolfPalette else SolarWolfPalette
    
    val colorScheme = if (palette.isLight) {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.bg,
            secondary = palette.secondary,
            onSecondary = palette.text,
            tertiary = palette.tertiary,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.card,
            onSurface = palette.text,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.muted,
            outline = palette.border,
            error = Color(0xFFD32F2F),
            onError = Color.White
        )
    } else {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.bg,
            secondary = palette.secondary,
            onSecondary = palette.text,
            tertiary = palette.tertiary,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.card,
            onSurface = palette.text,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.muted,
            outline = palette.border,
            error = Color(0xFFE57373),
            onError = Color.Black
        )
    }

    CompositionLocalProvider(
        LocalAppColors provides palette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
