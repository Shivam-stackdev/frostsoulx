/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.ui.frostsoul.FSIcon
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme

@Composable
internal fun FSGlassCard(
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier =
            modifier
                .clip(shape)
                .background(FrostSoulSurface.copy(alpha = 0.98f))
                .border(
                    width = 1.dp,
                    color = accent.copy(alpha = 0.16f),
                    shape = shape,
                ),
        content = content,
    )
}

@Composable
internal fun FSIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    val baseTint = if (isLightTheme) FrostSoulTheme.colors.onSurface else Color.White
    val iconTint = if (enabled) baseTint else baseTint.copy(alpha = 0.28f)
    val background =
        if (active) {
            baseTint.copy(alpha = if (isLightTheme) 0.10f else 0.14f)
        } else {
            if (isLightTheme) FrostSoulTheme.colors.surfaceRaised else Color.White.copy(alpha = 0.08f)
        }
    val buttonSize = if (compact) 42.dp else 50.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(buttonSize)
                .clip(CircleShape)
                .background(background)
                .border(1.dp, iconTint.copy(alpha = if (active) 0.52f else 0.15f), CircleShape)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ).semantics { this.contentDescription = contentDescription },
    ) {
        FSIcon(
            painter = painter,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(if (compact) 20.dp else 23.dp),
        )
    }
}

@Composable
internal fun FSAlbumArt(
    artworkUrl: String?,
    title: String,
    isPlaying: Boolean,
    palette: FrostSoulPalette,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val rotationTransition = rememberInfiniteTransition(label = "fs-album-rotation")
    val rotation by rotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 26_000, easing = LinearEasing)),
        label = "fs-album-rotation-value",
    )
    var pausedRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) pausedRotation = rotation
    }
    val displayedRotation = if (isPlaying) rotation else pausedRotation
    val tonearmAngle by animateFloatAsState(
        targetValue = if (isPlaying) -5f else -25f,
        animationSpec = tween(durationMillis = 420),
        label = "fs-tonearm-angle",
    )
    val artShape = if (compact) RoundedCornerShape(16.dp) else CircleShape

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .aspectRatio(1f)
                .shadow(
                    elevation = if (compact) 0.dp else 16.dp,
                    shape = CircleShape,
                    clip = false,
                )
                .drawBehind {
                    val radius = size.minDimension * 0.74f
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(palette.accent.copy(alpha = 0.38f), Color.Transparent),
                                center = center,
                                radius = radius,
                            ),
                        radius = radius,
                        center = center,
                    )
                }.padding(if (compact) 2.dp else 0.dp),
    ) {
        if (!compact) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val vinylRadius = size.minDimension * 0.47f
                drawCircle(color = Color(0xFF020506), radius = vinylRadius, center = center)
                for (ringIndex in 1..7) {
                    val ringRadius = vinylRadius * (0.22f + ringIndex * 0.095f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.035f + ringIndex * 0.006f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = (1f + ringIndex * 0.18f).dp.toPx()),
                    )
                }
                drawCircle(
                    color = palette.accent.copy(alpha = 0.22f),
                    radius = vinylRadius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx()),
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            Color.White.copy(alpha = 0.18f),
                            Color.Transparent,
                            Color.Transparent,
                        ),
                        center = center,
                    ),
                    startAngle = -56f,
                    sweepAngle = 72f,
                    useCenter = false,
                    topLeft = Offset(center.x - vinylRadius, center.y - vinylRadius),
                    size = Size(vinylRadius * 2f, vinylRadius * 2f),
                    style = Stroke(width = size.minDimension * 0.06f),
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(if (compact) 0.dp else 22.dp)
                    .graphicsLayer { rotationZ = displayedRotation }
                    .clip(artShape)
                    .background(FrostSoulSurfaceElevated)
                    .border(1.dp, palette.accent.copy(alpha = 0.45f), artShape),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = "Album artwork for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (artworkUrl.isNullOrBlank()) {
                FSIcon(
                    painter = painterResource(R.drawable.music_note),
                    contentDescription = null,
                    tint = FrostSoulOnSurfaceMuted,
                    modifier = Modifier.size(if (compact) 26.dp else 72.dp),
                )
            }
            if (!compact) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val discRadius = size.minDimension * 0.14f
                    drawCircle(color = Color.Black.copy(alpha = 0.72f), radius = discRadius, center = center)
                    drawCircle(color = Color.White.copy(alpha = 0.14f),
 radius = discRadius * 0.34f, center = center)
                    drawCircle(color = Color.Black.copy(alpha = 0.9f), radius = discRadius * 0.11f, center = center)
                }
            }
        }

        if (!compact) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val pivot = Offset(size.width * 0.77f, size.height * 0.15f)
                val armLength = size.minDimension * 0.38f
                val armEnd = Offset(pivot.x - armLength * 0.74f, pivot.y + armLength * 0.68f)
                withTransform({ rotate(tonearmAngle, pivot) }) {
                    drawCircle(color = Color(0xFF1B2A2E), radius = size.minDimension * 0.065f, center = pivot)
                    drawLine(
                        color = Color(0xFFB9D1D5),
                        start = pivot,
                        end = armEnd,
                        strokeWidth = size.minDimension * 0.022f,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.28f),
                        start = pivot,
                        end = armEnd,
                        strokeWidth = size.minDimension * 0.007f,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(color = Color(0xFFD5F2F5), radius = size.minDimension * 0.024f, center = armEnd)
                }
            }
        }
    }
}

@Composable
internal fun FSSeekbar(
    progress: Float,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.White,
    isEnabled: Boolean = durationMs > 0L,
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }
    val visibleProgress = (if (isDragging) dragProgress else progress).coerceIn(0f, 1f)
    val targetDuration = durationMs.coerceAtLeast(1L)

    fun seekAt(x: Float) {
        if (!isEnabled || containerSize.width == 0) return
        val fraction = (x / containerSize.width.toFloat()).coerceIn(0f, 1f)
        dragProgress = fraction
        onSeek((targetDuration * fraction).toLong())
    }

    Box(
        modifier =
            modifier
                .height(30.dp)
                .fillMaxWidth()
                .onSizeChanged { containerSize = it }
                .pointerInput(isEnabled, targetDuration) {
                    detectTapGestures { offset -> seekAt(offset.x) }
                }.pointerInput(isEnabled, targetDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            seekAt(offset.x)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            seekAt(change.position.x)
                        },
                    )
                }.semantics { contentDescription = "Playback progress" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeight = 5.dp.toPx()
            val y = this.size.height / 2f
            val trackStart = Offset(0f, y)
            val trackEnd = Offset(this.size.width, y)
            val activeEnd = Offset(this.size.width * visibleProgress, y)
            drawLine(
                color = Color.White.copy(alpha = 0.16f),
                start = trackStart,
                end = trackEnd,
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = accent,
                start = trackStart,
                end = activeEnd,
                strokeWidth = trackHeight,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = Color.White,
                radius = if (isDragging) 7.dp.toPx() else 5.dp.toPx(),
                center = activeEnd,
            )
        }
    }
}

@Composable
internal fun FSTopBar(
    selectedPage: Int,
    pageOffsetFraction: Float,
    pageCount: Int,
    onPageSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    onOpenOptions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        FSIconButton(
            painter = painterResource(R.drawable.expand_less),
            contentDescription = "Collapse player",
            onClick = onDismiss,
            compact = true,
        )
        Spacer(Modifier.width(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f),
        ) {
            FrostSoulPagerDots(
                pageCount = pageCount,
                selectedPage = selectedPage,
                selectedPageOffsetFraction = pageOffsetFraction,
                onPageSelected = onPageSelected,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FSIconButton(
                painter = painterResource(R.drawable.style),
                contentDescription = "Player appearance",
                onClick = onOpenOptions,
                compact = true,
            )
            FSIconButton(
                painter = painterResource(R.drawable.share),
                contentDescription = "Share track",
                onClick = onOpenOptions,
                compact = true,
            )
        }
    }
}
