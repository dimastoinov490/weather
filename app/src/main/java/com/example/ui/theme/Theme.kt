package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AtmosphericDarkColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = ObsidianBg,
    primaryContainer = Color(0x2E60A5FA),
    onPrimaryContainer = IceWhite,
    secondary = SkyAzure,
    onSecondary = ObsidianBg,
    secondaryContainer = Color(0x2638BDF8),
    onSecondaryContainer = IceWhite,
    tertiary = AccentBright,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = IceWhite,
    surface = ObsidianSurface,
    onSurface = IceWhite,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = MutedText,
    outline = ObsidianBorder,
    outlineVariant = ObsidianDivider,
    error = StatusUnhealthy,
    onError = ObsidianBg
)

val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun NeboWeatherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AtmosphericDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
