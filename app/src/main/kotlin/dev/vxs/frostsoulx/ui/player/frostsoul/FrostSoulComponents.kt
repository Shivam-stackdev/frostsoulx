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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.ui.frostsoul.FSIcon
import dev.vxs.frostsoulx.ui.frostsoul.FSText
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
    buttonSize: Dp = if (compact) 42.dp else 50.dp,
    iconSize: Dp = if (compact) 20.dp else 23.dp,
    showContainer: Boolean = true,
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
    val buttonModifier = modifier.size(buttonSize)
    val styledModifier =
        if (showContainer) {
            buttonModifier
                .clip(CircleShape)
                .background(background)
                .border(1.dp, iconTint.copy(alpha = if (active) 0.52f else 0.15f), CircleShape)
        } else {
            buttonModifier
        }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            styledModifier
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
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
internal fun FSAlbumArt(
    artworkUrl: String?,
    title: String,
    isPlaying: Boolean,
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

    if (compact) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(FrostSoulSurfaceElevated),
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
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        return
    }

    val cardShape = RoundedCornerShape(24.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .shadow(elevation = 18.dp, shape = cardShape, clip = false)
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF3A3A3E), Color(0xFF141416)),
                ),
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(PlayerLayoutTokens.TurntablePlatterSize)
                .graphicsLayer { rotationZ = displayedRotation },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val platterRadius = size.minDimension / 2f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC7C7CC), Color(0xFF8A8A90)),
                        center = center,
                        radius = platterRadius,
                    ),
                    radius = platterRadius,
                    center = center,
                )
                for (ringIndex in 1..20) {
                    val ringRadius = platterRadius * (0.14f + ringIndex * 0.041f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
                drawCircle(
                    color = Color.Black.copy(alpha = 0.14f),
                    radius = platterRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            Box(
                modifier = Modifier
                    .size(PlayerLayoutTokens.TurntablePolaroidOuterSize)
                    .graphicsLayer { rotationZ = 45f }
                    .shadow(4.dp, RoundedCornerShape(4.dp), clip = false)
                    .background(Color(0xFFF5F1E8), RoundedCornerShape(4.dp))
                    .padding(14.dp),
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "Album artwork for $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(1.dp)),
                )
                if (artworkUrl.isNullOrBlank()) {
                    FSIcon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = Color(0xFF2B2B2B),
                        modifier = Modifier.align(Alignment.Center).size(32.dp),
                    )
                }
                FSText(
                    text = title.take(18),
                    color = Color(0xFF2B2B2B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterStart).rotate(-90f),
                )
                FSText(
                    text = title.take(18),
                    color = Color(0xFF2B2B2B),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    modifier = Modifier.align(Alignment.CenterEnd).rotate(90f),
                )
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pivot = Offset(size.width * 0.77f, size.height * 0.15f)
            val armLength = size.minDimension * 0.38f
            val elbow = Offset(pivot.x - armLength * 0.74f, pivot.y + armLength * 0.68f)
            val needle = Offset(elbow.x - armLength * 0.16f, elbow.y + armLength * 0.15f)
            val metalBrush = Brush.linearGradient(
                colors = listOf(Color(0xFF4A4A4E), Color(0xFF1A1A1C)),
                start = pivot,
                end = elbow,
            )
            withTransform({ rotate(tonearmAngle, pivot) }) {
                drawCircle(
                    brush = metalBrush,
                    radius = PlayerLayoutTokens.TurntableTonearmMountSize.toPx() / 2f,
                    center = pivot,
                )
                drawLine(
                    brush = metalBrush,
                    start = pivot,
                    end = elbow,
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    brush = metalBrush,
                    radius = PlayerLayoutTokens.TurntableTonearmElbowSize.toPx() / 2f,
                    center = elbow,
                )
                drawLine(
                    brush = metalBrush,
                    start = elbow,
                    end = needle,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = Color(0xFFD5F2F5),
                    radius = 8.dp.toPx(),
                    center = needle,
                )
            }

            val pegCenter = Offset(
                center.x + size.minDimension * 0.29f,
                center.y + size.minDimension * 0.31f,
            )
            drawCircle(
                brush = Brush.linearGradient(colors = listOf(Color(0xFF4A4A4E), Color(0xFF1A1A1C))),
                radius = PlayerLayoutTokens.TurntableRestPegSize.toPx() / 2f,
                center = pegCenter,
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF101012)),
        ) {
            FSIcon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = "Audio source",
                tint = FrostSoulTheme.colors.accentBright,
                modifier = Modifier.size(20.dp),
            )
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
    }
}
