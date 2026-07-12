package com.artiface.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Warm coral / ink / citrus palette — energetic without defaulting to purple-on-white.
val ArtifaceCoral = Color(0xFFFF5A3C)
val ArtifaceCoralDark = Color(0xFFE03D22)
val ArtifaceInk = Color(0xFF1A1210)
val ArtifaceInkSoft = Color(0xFF2C211E)
val ArtifaceCream = Color(0xFFFFF6EE)
val ArtifaceSand = Color(0xFFF2E2D4)
val ArtifaceCitrus = Color(0xFFFFC857)
val ArtifaceTeal = Color(0xFF1FA6A0)
val ArtifacePlum = Color(0xFF5C2A4D)
val ArtifaceMist = Color(0xFFFFE8DC)
val ArtifaceNight = Color(0xFF120E0D)
val ArtifaceNightSurface = Color(0xFF241B18)
val ArtifaceNightElevated = Color(0xFF342722)

private val LightColorScheme = lightColorScheme(
    primary = ArtifaceCoral,
    onPrimary = Color.White,
    primaryContainer = ArtifaceMist,
    onPrimaryContainer = ArtifaceInk,
    secondary = ArtifaceTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD4F3F1),
    onSecondaryContainer = ArtifaceInk,
    tertiary = ArtifaceCitrus,
    onTertiary = ArtifaceInk,
    tertiaryContainer = Color(0xFFFFF0C8),
    onTertiaryContainer = ArtifaceInk,
    background = ArtifaceCream,
    onBackground = ArtifaceInk,
    surface = ArtifaceCream,
    onSurface = ArtifaceInk,
    surfaceVariant = ArtifaceSand,
    onSurfaceVariant = ArtifaceInkSoft,
    outline = Color(0xFFC9B0A0),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = ArtifaceCoral,
    onPrimary = Color.White,
    primaryContainer = ArtifacePlum,
    onPrimaryContainer = ArtifaceMist,
    secondary = ArtifaceTeal,
    onSecondary = ArtifaceNight,
    secondaryContainer = Color(0xFF0F4F4C),
    onSecondaryContainer = Color(0xFFB8EDEA),
    tertiary = ArtifaceCitrus,
    onTertiary = ArtifaceNight,
    tertiaryContainer = Color(0xFF5A4208),
    onTertiaryContainer = ArtifaceCitrus,
    background = ArtifaceNight,
    onBackground = ArtifaceCream,
    surface = ArtifaceNightSurface,
    onSurface = ArtifaceCream,
    surfaceVariant = ArtifaceNightElevated,
    onSurfaceVariant = ArtifaceSand,
    outline = Color(0xFF6E564C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Immutable
data class ArtifaceGradients(
    val hero: Brush,
    val playful: Brush,
    val processing: Brush,
)

val LocalArtifaceGradients = staticCompositionLocalOf {
    ArtifaceGradients(
        hero = Brush.verticalGradient(listOf(ArtifaceMist, ArtifaceCream)),
        playful = Brush.linearGradient(listOf(ArtifaceCoral, ArtifaceCitrus)),
        processing = Brush.verticalGradient(listOf(ArtifacePlum, ArtifaceInk)),
    )
}

private fun lightGradients() = ArtifaceGradients(
    hero = Brush.verticalGradient(listOf(Color(0xFFFFE4D6), ArtifaceCream, Color(0xFFFFF9F4))),
    playful = Brush.linearGradient(listOf(ArtifaceCoral, ArtifaceCitrus, ArtifaceTeal)),
    processing = Brush.verticalGradient(listOf(ArtifacePlum, ArtifaceCoralDark, ArtifaceInk)),
)

private fun darkGradients() = ArtifaceGradients(
    hero = Brush.verticalGradient(listOf(ArtifaceNightElevated, ArtifaceNight)),
    playful = Brush.linearGradient(listOf(ArtifaceCoral, ArtifacePlum, ArtifaceTeal)),
    processing = Brush.verticalGradient(listOf(ArtifaceNight, ArtifacePlum, ArtifaceCoralDark)),
)

@Composable
fun ArtifaceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val gradients = if (darkTheme) darkGradients() else lightGradients()

    CompositionLocalProvider(LocalArtifaceGradients provides gradients) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ArtifaceTypography,
            shapes = ArtifaceShapes,
            content = content,
        )
    }
}

object ArtifaceThemeExtras {
    val gradients: ArtifaceGradients
        @Composable
        get() = LocalArtifaceGradients.current
}
