package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

@Immutable
data class AstraExtendedColors(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val electricBlue: Color,
    val emerald: Color,
    val crimson: Color,
    val amber: Color,
    val violet: Color,
    val emeraldTint: Color,
    val crimsonTint: Color,
    val amberTint: Color,
    val blueTint: Color,
    val violetTint: Color
)

private val DarkExtendedColors = AstraExtendedColors(
    isDark = true,
    background = CosmicBackground,
    card = CosmicSurface,
    cardElevated = CosmicSurfaceElevated,
    border = CosmicBorder,
    textPrimary = CosmicOnBackground,
    textSecondary = CosmicOnSurfaceVariant,
    electricBlue = ElectricBlue,
    emerald = EmeraldSuccess,
    crimson = CrimsonTrap,
    amber = AmberInsight,
    violet = CosmicViolet,
    emeraldTint = Color(0x2222C55E),
    crimsonTint = Color(0x22EF4444),
    amberTint = Color(0x22F59E0B),
    blueTint = Color(0x223B82F6),
    violetTint = Color(0x228B5CF6)
)

private val WhitePaperExtendedColors = AstraExtendedColors(
    isDark = false,
    background = PaperBackground,
    card = PaperSurface,
    cardElevated = PaperSurfaceElevated,
    border = PaperBorder,
    textPrimary = PaperOnBackground,
    textSecondary = PaperOnSurfaceVariant,
    electricBlue = ElectricBlueDark,
    emerald = EmeraldDark,
    crimson = CrimsonDark,
    amber = AmberDark,
    violet = CosmicVioletDark,
    emeraldTint = Color(0x1816A34A),
    crimsonTint = Color(0x18DC2626),
    amberTint = Color(0x18D97706),
    blueTint = Color(0x182563EB),
    violetTint = Color(0x187C3AED)
)

val LocalAstraColors = staticCompositionLocalOf { DarkExtendedColors }

private val AstraDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1D3564),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = CosmicViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2E1E59),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = EmeraldSuccess,
    onTertiary = Color(0xFF052E16),
    background = CosmicBackground,
    onBackground = CosmicOnBackground,
    surface = CosmicSurface,
    onSurface = CosmicOnBackground,
    surfaceVariant = CosmicSurfaceElevated,
    onSurfaceVariant = CosmicOnSurfaceVariant,
    outline = CosmicBorder,
    error = CrimsonTrap,
    onError = Color.White
)

private val WhitePaperLightColorScheme = lightColorScheme(
    primary = ElectricBlueDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = CosmicVioletDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = EmeraldDark,
    onTertiary = Color.White,
    background = PaperBackground,
    onBackground = PaperOnBackground,
    surface = PaperSurface,
    onSurface = PaperOnBackground,
    surfaceVariant = PaperSurfaceElevated,
    onSurfaceVariant = PaperOnSurfaceVariant,
    outline = PaperBorder,
    error = CrimsonDark,
    onError = Color.White
)

val AstraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AstraFlowTheme(
    isWhitePaperExam: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isWhitePaperExam) WhitePaperLightColorScheme else AstraDarkColorScheme
    val extendedColors = if (isWhitePaperExam) WhitePaperExtendedColors else DarkExtendedColors

    CompositionLocalProvider(LocalAstraColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AstraTypography,
            shapes = AstraShapes,
            content = content
        )
    }
}
