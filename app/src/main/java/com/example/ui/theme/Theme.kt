package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow

val activeVariantFlow = MutableStateFlow(AppThemeVariant.INFERNO)

class MutableStateFlowWrapper<T>(val flow: MutableStateFlow<T>) : MutableState<T> {
    override var value: T
        get() = flow.value
        set(v) { flow.value = v }
    
    override fun component1(): T = flow.value
    override fun component2(): (T) -> Unit = { flow.value = it }
}

private val initThemeWrapper = run {
    val currentVal = ThemeManager.activeVariant.value
    activeVariantFlow.value = currentVal
    ThemeManager.activeVariant = MutableStateFlowWrapper(activeVariantFlow)
    true
}

@Composable
fun MyApplicationTheme(
    themeVariant: AppThemeVariant = ThemeManager.activeVariant.value,
    content: @Composable () -> Unit
) {
    val initialized = initThemeWrapper
    
    val currentVariant by activeVariantFlow.collectAsState()
    
    androidx.compose.runtime.LaunchedEffect(themeVariant) {
        activeVariantFlow.value = themeVariant
    }
    
    val palette = when (currentVariant) {
        AppThemeVariant.INFERNO -> InfernoPalette
        AppThemeVariant.NEXUS -> NexusPalette
        AppThemeVariant.ARCTIC_FOX -> ArcticFoxPalette
        AppThemeVariant.CRIMSON_WOLF -> CrimsonWolfPalette
    }
    
    val colorScheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = Color.White,
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
