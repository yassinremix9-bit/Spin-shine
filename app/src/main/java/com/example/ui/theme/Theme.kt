package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Immutable
data class StatusColorPalette(
    val available: Color,
    val availableContainer: Color,
    val inUse: Color,
    val inUseContainer: Color,
    val endingSoon: Color,
    val endingSoonContainer: Color,
    val maintenance: Color,
    val maintenanceContainer: Color,
    val paused: Color,
    val pausedContainer: Color,
    val headerStart: Color,
    val headerMiddle: Color,
    val headerEnd: Color,
    val cardBorder: Color
)

private val LightStatusColors = StatusColorPalette(
    available = Emerald600,
    availableContainer = Emerald50,
    inUse = Brand600,
    inUseContainer = Brand50,
    endingSoon = Amber700,
    endingSoonContainer = Amber50,
    maintenance = Rose600,
    maintenanceContainer = Rose50,
    paused = Purple500,
    pausedContainer = Color(0xFFF5F3FF),
    headerStart = Brand700,
    headerMiddle = Brand600,
    headerEnd = Indigo700,
    cardBorder = Slate200
)

private val DarkStatusColors = StatusColorPalette(
    available = Emerald500,
    availableContainer = Emerald900.copy(alpha = 0.45f),
    inUse = Brand400,
    inUseContainer = Brand900.copy(alpha = 0.55f),
    endingSoon = Amber500,
    endingSoonContainer = Amber900.copy(alpha = 0.45f),
    maintenance = Rose500,
    maintenanceContainer = Rose900.copy(alpha = 0.45f),
    paused = Color(0xFFA78BFA),
    pausedContainer = Color(0xFF3B0764).copy(alpha = 0.45f),
    headerStart = Brand900,
    headerMiddle = Brand700,
    headerEnd = Indigo900,
    cardBorder = Slate700
)

val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val LightColorScheme = lightColorScheme(
    primary = Brand600,
    onPrimary = Color.White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand900,
    secondary = Indigo700,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Indigo900,
    tertiary = Emerald600,
    onTertiary = Color.White,
    tertiaryContainer = Emerald50,
    onTertiaryContainer = Emerald900,
    error = Rose600,
    onError = Color.White,
    errorContainer = Rose50,
    onErrorContainer = Rose900,
    background = Slate50,
    onBackground = Slate800,
    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate200,
    outlineVariant = Slate100
)

private val DarkColorScheme = darkColorScheme(
    primary = Brand400,
    onPrimary = Slate900,
    primaryContainer = Brand900,
    onPrimaryContainer = Brand100,
    secondary = Color(0xFF818CF8),
    onSecondary = Slate900,
    secondaryContainer = Indigo900,
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Emerald500,
    onTertiary = Slate900,
    tertiaryContainer = Emerald900,
    onTertiaryContainer = Emerald100,
    error = Rose500,
    onError = Color.White,
    errorContainer = Rose900,
    onErrorContainer = Rose100,
    background = DarkBackground,
    onBackground = Slate100,
    surface = DarkSurface,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800
)

val SpinShineShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun SpinShineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(
        LocalStatusColors provides statusColors,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SpinShineTypography,
            shapes = SpinShineShapes,
            content = content
        )
    }
}
