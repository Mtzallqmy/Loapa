package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val TacticalDarkColorScheme = darkColorScheme(
    primary = DesertGold,
    onPrimary = ObsidianBlack,
    primaryContainer = DesertGoldContainer,
    onPrimaryContainer = DesertGoldBright,
    secondary = ArmorCyan,
    onSecondary = ObsidianBlack,
    secondaryContainer = TacticalSlateElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = TacticalEmerald,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = TacticalSlateDark,
    onSurface = TextPrimary,
    surfaceVariant = TacticalSlateSurface,
    onSurfaceVariant = TextSecondary,
    outline = TacticalBorder,
    error = SignalCrimson,
    onError = TextPrimary
)

private val TacticalShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp)
)

@Composable
fun CombatFrontsTheme(
    textScale: Float = 1f,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalDarkColorScheme,
        typography = buildCombatTypography(textScale),
        shapes = TacticalShapes,
        content = content
    )
}
