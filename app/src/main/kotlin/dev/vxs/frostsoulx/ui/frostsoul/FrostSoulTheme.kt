/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.frostsoul

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class FrostSoulColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceGlass: Color,
    val surfaceGlassStrong: Color,
    val accent: Color,
    val accentBright: Color,
    val accentMuted: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val outline: Color,
    val error: Color,
    val scrim: Color,
)

@Immutable
data class FrostSoulTypography(
    val display: TextStyle,
    val title: TextStyle,
    val sectionTitle: TextStyle,
    val body: TextStyle,
    val bodyMuted: TextStyle,
    val label: TextStyle,
    val overline: TextStyle,
)

@Immutable
data class FrostSoulShapes(
    val tiny: Shape,
    val small: Shape,
    val medium: Shape,
    val large: Shape,
    val extraLarge: Shape,
    val pill: Shape,
)

@Immutable
data class FrostSoulElevation(
    val none: Dp = 0.dp,
    val low: Dp = 2.dp,
    val medium: Dp = 8.dp,
    val high: Dp = 18.dp,
)

@Immutable
data class FrostSoulEffects(
    val glassBlurRadius: Dp = 24.dp,
    val backdropBlurRadius: Dp = 48.dp,
    val activeGlowAlpha: Float = 0.24f,
    val ambientGlowAlpha: Float = 0.10f,
)

@Immutable
data class FrostSoulSpacing(
    val micro: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val section: Dp = 24.dp,
    val page: Dp = 20.dp,
    val hero: Dp = 32.dp,
)

@Immutable
data class FrostSoulMotion(
    val quick: Int = 120,
    val standard: Int = 220,
    val expressive: Int = 420,
    val slow: Int = 650,
    val contentSpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.82f,
        stiffness = Spring.StiffnessMediumLow,
    ),
    val controlSpring: AnimationSpec<Float> = spring(
        dampingRatio = 0.74f,
        stiffness = Spring.StiffnessMedium,
    ),
) {
    fun <T> quickTween(): AnimationSpec<T> = tween(durationMillis = quick)

    fun <T> standardTween(): AnimationSpec<T> = tween(durationMillis = standard)

    fun <T> expressiveTween(): AnimationSpec<T> = tween(durationMillis = expressive)
}

@Immutable
data class FrostSoulDesignTokens(
    val colors: FrostSoulColors,
    val typography: FrostSoulTypography,
    val shapes: FrostSoulShapes,
    val elevation: FrostSoulElevation,
    val effects: FrostSoulEffects,
    val spacing: FrostSoulSpacing,
    val motion: FrostSoulMotion,
)

private val DefaultFrostSoulTokens = FrostSoulDesignTokens(
    colors = FrostSoulColors(
        background = Color(0xFF0C0C0C),
        surface = Color(0xFF1E1E1E),
        surfaceRaised = Color(0xFF282828),
        surfaceGlass = Color(0xFF1E1E1E),
        surfaceGlassStrong = Color(0xFF242424),
        accent = Color(0xFF00E676),
        accentBright = Color(0xFF00E676),
        accentMuted = Color(0xFF008542),
        onBackground = Color(0xFFFDFDFD),
        onSurface = Color(0xFFFDFDFD),
        onSurfaceMuted = Color(0xFFA5A5A5),
        outline = Color.Transparent,
        error = Color(0xFFFF7C8F),
        scrim = Color.Black.copy(alpha = 0.72f),
    ),
    typography = FrostSoulTypography(
        display = TextStyle(fontWeight = FontWeight.SemiBold),
        title = TextStyle(fontWeight = FontWeight.SemiBold),
        sectionTitle = TextStyle(fontWeight = FontWeight.SemiBold),
        body = TextStyle(fontWeight = FontWeight.Normal),
        bodyMuted = TextStyle(fontWeight = FontWeight.Normal),
        label = TextStyle(fontWeight = FontWeight.Medium),
        overline = TextStyle(fontWeight = FontWeight.SemiBold, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified),
    ),
    shapes = FrostSoulShapes(
        tiny = RoundedCornerShape(10.dp),
        small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
        pill = RoundedCornerShape(50),
    ),
    elevation = FrostSoulElevation(),
    effects = FrostSoulEffects(),
    spacing = FrostSoulSpacing(),
    motion = FrostSoulMotion(),
)

val LocalFrostSoulTokens: ProvidableCompositionLocal<FrostSoulDesignTokens> =
    compositionLocalOf { DefaultFrostSoulTokens }

object FrostSoulTheme {
    val colors: FrostSoulColors
        @Composable get() = LocalFrostSoulTokens.current.colors

    val typography: FrostSoulTypography
        @Composable get() = LocalFrostSoulTokens.current.typography

    val shapes: FrostSoulShapes
        @Composable get() = LocalFrostSoulTokens.current.shapes

    val elevation: FrostSoulElevation
        @Composable get() = LocalFrostSoulTokens.current.elevation

    val effects: FrostSoulEffects
        @Composable get() = LocalFrostSoulTokens.current.effects

    val spacing: FrostSoulSpacing
        @Composable get() = LocalFrostSoulTokens.current.spacing

    val motion: FrostSoulMotion
        @Composable get() = LocalFrostSoulTokens.current.motion
}

@Composable
fun FrostSoulDesignSystem(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tokens = remember(darkTheme) {
        if (darkTheme) {
            DefaultFrostSoulTokens
        } else {
            DefaultFrostSoulTokens.copy(
                colors = DefaultFrostSoulTokens.colors.copy(
                    background = Color(0xFFF3F5F8),
                    surface = Color.White,
                    surfaceRaised = Color(0xFFF8F9FB),
                    surfaceGlass = Color.White.copy(alpha = 0.96f),
                    surfaceGlassStrong = Color.White,
                    accent = Color(0xFF121417),
                    accentBright = Color(0xFF121417),
                    accentMuted = Color(0xFF5E636A),
                    onBackground = Color(0xFF101216),
                    onSurface = Color(0xFF101216),
                    onSurfaceMuted = Color(0xFF656B73),
                    outline = Color(0x1A101216),
                    scrim = Color.Black.copy(alpha = 0.48f),
                ),
            )
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(LocalFrostSoulTokens provides tokens, content = content)
}

@Composable
fun Modifier.frostSoulGlass(shape: Shape = FrostSoulTheme.shapes.large): Modifier {
    val colors = FrostSoulTheme.colors
    return this.background(colors.surfaceGlass, shape)
}

@Composable
fun Modifier.frostSoulGlow(
    color: Color = FrostSoulTheme.colors.accent,
    alpha: Float = FrostSoulTheme.effects.activeGlowAlpha,
): Modifier =
    drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = size.maxDimension * 0.72f,
            ),
            radius = size.maxDimension * 0.72f,
        )
    }

@Composable
fun Modifier.frostSoulScreenBackground(): Modifier {
    val colors = FrostSoulTheme.colors
    return background(colors.background)
        .drawBehind {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.09f),
                        Color.Transparent,
                    ),
                    endY = size.height * 0.42f,
                ),
            )
        }
}
