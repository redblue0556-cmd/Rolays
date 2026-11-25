package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class LiquidGlassPalette(
    val background: Color,
    val glassFill: Color,
    val glassFillElevated: Color,
    val glassFillDeep: Color,
    val borderBright: Color,
    val borderSoft: Color,
    val cyanAccent: Color,
    val blueAccent: Color,
    val violetAccent: Color,
    val mintAccent: Color,
    val amberAccent: Color,
    val roseAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val LocalLiquidGlassPalette = staticCompositionLocalOf {
    LiquidGlassPalette(
        background = ObsidianNight,
        glassFill = GlassFillBase,
        glassFillElevated = GlassFillElevated,
        glassFillDeep = GlassFillDeep,
        borderBright = GlassBorderBright,
        borderSoft = GlassBorderSoft,
        cyanAccent = LiquidCyan,
        blueAccent = GlacialBlue,
        violetAccent = PrismViolet,
        mintAccent = AuroraMint,
        amberAccent = SolarAmber,
        roseAccent = CoralRose,
        textPrimary = FrostWhite,
        textSecondary = MistSilver,
        textMuted = SlateGlass
    )
}

private val RolaysColorScheme = darkColorScheme(
    primary = LiquidCyan,
    onPrimary = ObsidianNight,
    primaryContainer = GlassCyanGlow,
    onPrimaryContainer = FrostWhite,
    secondary = GlacialBlue,
    onSecondary = FrostWhite,
    secondaryContainer = GlassFillElevated,
    onSecondaryContainer = FrostWhite,
    tertiary = PrismViolet,
    onTertiary = FrostWhite,
    tertiaryContainer = GlassVioletGlow,
    onTertiaryContainer = FrostWhite,
    background = ObsidianNight,
    onBackground = FrostWhite,
    surface = DeepSapphire,
    onSurface = FrostWhite,
    surfaceVariant = GlassMidnight,
    onSurfaceVariant = MistSilver,
    outline = GlassBorderBright,
    outlineVariant = GlassBorderSoft,
    error = CoralRose,
    onError = ObsidianNight
)

val RolaysShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun RolaysTheme(
    content: @Composable () -> Unit
) {
    val palette = LocalLiquidGlassPalette.current
    CompositionLocalProvider(LocalLiquidGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = RolaysColorScheme,
            typography = RolaysTypography,
            shapes = RolaysShapes,
            content = content
        )
    }
}
