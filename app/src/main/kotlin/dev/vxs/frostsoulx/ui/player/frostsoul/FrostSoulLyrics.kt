/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vxs.frostsoulx.ui.frostsoul.FSIcon as Icon
import dev.vxs.frostsoulx.ui.frostsoul.FSText as Text
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.EntryPointAccessors
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.constants.LyricsTemplateTooltipDismissedKey
import dev.vxs.frostsoulx.di.LyricsHelperEntryPoint
import dev.vxs.frostsoulx.lyrics.core.LyricsLine
import dev.vxs.frostsoulx.lyrics.core.LyricsSyncState
import dev.vxs.frostsoulx.utils.rememberPreference

/**
 * Renders karaoke directly from the singleton lyrics synchronization engine.
 *
 * [rawLyrics], [positionMs], and [durationMs] remain part of the player-surface contract while
 * the service owns parsing, caching, offset correction, and timestamp resolution. This prevents
 * a second parser or clock from drifting away from notification and overlay lyric consumers.
 */
@Composable
internal fun FSLyrics(
    rawLyrics: String?,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onOpenAudioOutput: () -> Unit = {},
    onOpenOptions: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val applicationContext = LocalContext.current.applicationContext
    val synchronizationEngine =
        remember(applicationContext) {
            EntryPointAccessors
                .fromApplication(applicationContext, LyricsHelperEntryPoint::class.java)
                .lyricsSynchronizationEngine()
        }
    val document by synchronizationEngine.documentState.collectAsState()
    val syncState by synchronizationEngine.state.collectAsState()
    val lines = document?.original?.lines.orEmpty()
    val currentIndex = syncState.currentLineIndex.takeIf { it in lines.indices } ?: -1
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex, lines.size) {
        if (currentIndex >= 0 && listState.isScrollInProgress.not()) {
            listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    if (lines.isEmpty()) {
        val lookupMessage =
            if (rawLyrics.isNullOrBlank()) {
                "Lyrics are unavailable for this track."
            } else {
                "Preparing synchronized lyrics for this track."
            }
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
        ) {
            Text(
                text = lookupMessage,
                color = FrostSoulOnSurfaceMuted,
                fontSize = 19.sp,
                lineHeight = 28.sp,
            )
            Text(
                text = "FrostSoul keeps lyrics, notification updates, and overlays on one shared timeline.",
                color = FrostSoulOnSurfaceMuted.copy(alpha = 0.62f),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        return
    }

    val tooltipState = rememberPreference(LyricsTemplateTooltipDismissedKey, false)
    val tooltipDismissed by tooltipState

    PremiumLyricsBackgroundContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 24.dp, top = 88.dp, end = 24.dp, bottom = 156.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = lines,
                key = { _, line -> "${line.startMs}-${line.endMs}-${line.text}" },
            ) { index, line ->
                val isCurrent = index == currentIndex
                FrostSoulKaraokeLine(
                    line = line,
                    isCurrent = isCurrent,
                    isPast = currentIndex >= 0 && index < currentIndex,
                    currentWordIndex = if (isCurrent) syncState.currentWordIndex else -1,
                    wordProgress = if (isCurrent) syncState.wordProgress else 0f,
                    lineProgress = if (isCurrent) syncState.lineProgress else 0f,
                    onSeek = onSeek,
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
                    .background(Color.Black.copy(alpha = 0.76f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            FSIconButton(painterResource(R.drawable.timer), "Sleep timer", onOpenOptions, compact = true)
            FSIconButton(painterResource(R.drawable.bluetooth), "Audio output", onOpenAudioOutput, compact = true)
            FSIconButton(painterResource(R.drawable.settings), "Lyrics settings", onOpenOptions, compact = true)
            FSIconButton(painterResource(R.drawable.share), "Lyrics poster", onOpenOptions, compact = true)
            FSIconButton(painterResource(R.drawable.favorite_border), "Like this song", onToggleLike, compact = true)
            FSIconButton(
                painterResource(R.drawable.play),
                "Play or pause",
                onTogglePlayPause,
                active = true,
                compact = true,
            )
        }

        if (!tooltipDismissed) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 86.dp)
                        .background(Color.Black.copy(alpha = 0.86f), RoundedCornerShape(18.dp))
                        .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text("Lyrics template updated", color = FrostSoulOnSurface, fontSize = 12.sp)
                FSIconButton(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Dismiss lyrics update",
                    onClick = { tooltipState.value = true },
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun PremiumLyricsBackgroundContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    Box(
        modifier = modifier.fillMaxSize().background(if (isLightTheme) Color(0xFFF3F5F8) else Color.Black),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (isLightTheme) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE6F1EE).copy(alpha = 0.80f), Color(0xFFF3F5F8)),
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A1F1D).copy(alpha = 0.60f), Color.Black),
                    )
                },
            ),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (isLightTheme) Color.White.copy(alpha = 0.32f) else Color.Black.copy(alpha = 0.50f),
            ),
        )
        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), content = content)
    }
}

@Composable
private fun FrostSoulKaraokeLine(
    line: LyricsLine,
    isCurrent: Boolean,
    isPast: Boolean,
    currentWordIndex: Int,
    wordProgress: Float,
    lineProgress: Float,
    onSeek: (Long) -> Unit,
) {
    val emphasis by animateFloatAsState(
        targetValue = if (isCurrent) 1f else if (isPast) 0.60f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 500f),
        label = "fs-karaoke-line-emphasis",
    )
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
        label = "fs-karaoke-line-scale",
    )
    val lineColor = if (isCurrent) FrostSoulOnSurface else FrostSoulOnSurfaceMuted
    val annotatedText =
        remember(line, currentWordIndex, wordProgress, lineProgress, isCurrent, lineColor, emphasis) {
            line.asKaraokeAnnotatedText(
                activeLine = isCurrent,
                currentWordIndex = currentWordIndex,
                wordProgress = wordProgress,
                lineProgress = lineProgress,
                inactiveColor = lineColor,
                activeColor = Color.White,
                glowStrength = emphasis,
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f
                }.clickable(
                    enabled = line.startMs >= 0L,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onSeek(line.startMs) },
                ),
    ) {
        Text(
            text = annotatedText,
            color = lineColor,
            fontSize = 27.sp,
            lineHeight = 36.sp,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        line.translation?.takeIf { it.isNotBlank() }?.let { translation ->
            Text(
                text = translation,
                color = if (isCurrent) FrostSoulOnSurface else FrostSoulOnSurfaceMuted,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        line.romanization?.takeIf { it.isNotBlank() }?.let { romanization ->
            Text(
                text = romanization,
                color = if (isCurrent) FrostSoulOnSurface else FrostSoulOnSurfaceMuted,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

private fun LyricsLine.asKaraokeAnnotatedText(
    activeLine: Boolean,
    currentWordIndex: Int,
    wordProgress: Float,
    lineProgress: Float,
    inactiveColor: Color,
    activeColor: Color,
    glowStrength: Float,
) =
    buildAnnotatedString {
        if (!activeLine) {
            withStyle(SpanStyle(color = inactiveColor)) { append(text) }
            return@buildAnnotatedString
        }

        if (words.isEmpty()) {
            val progress = lineProgress.coerceIn(0f, 1f)
            withStyle(
                SpanStyle(
                    color = lerpColor(inactiveColor, activeColor, progress),
                    shadow =
                        Shadow(
                            color = activeColor.copy(alpha = 0.42f * progress * glowStrength),
                            blurRadius = 16f * progress,
                        ),
                ),
            ) {
                append(text)
            }
            return@buildAnnotatedString
        }

        words.forEachIndexed { index, word ->
            val progress =
                when {
                    index < currentWordIndex -> 1f
                    index == currentWordIndex -> wordProgress.coerceIn(0f, 1f)
                    else -> 0f
                }
            withStyle(
                wordKaraokeStyle(
                    progress = progress,
                    inactiveColor = inactiveColor,
                    activeColor = activeColor,
                    glowStrength = glowStrength,
                ),
            ) {
                append(word.text)
                if (index < words.lastIndex && word.text.lastOrNull()?.isWhitespace() != true) append(" ")
            }
        }
    }

private fun wordKaraokeStyle(
    progress: Float,
    inactiveColor: Color,
    activeColor: Color,
    glowStrength: Float,
): SpanStyle {
    val fill = progress.coerceIn(0f, 1f)
    val brush =
        if (fill > 0.02f && fill < 0.98f) {
            Brush.horizontalGradient(
                colorStops =
                    arrayOf(
                        0f to activeColor,
                        fill to activeColor,
                        (fill + 0.018f).coerceAtMost(1f) to inactiveColor,
                        1f to inactiveColor,
                    ),
            )
        } else {
            null
        }
    val color =
        when {
            fill <= 0.02f -> inactiveColor
            fill >= 0.98f -> activeColor
            else -> Color.Unspecified
        }
    val shadow =
        if (fill > 0.02f) {
            Shadow(
                color = activeColor.copy(alpha = 0.52f * fill * glowStrength),
                blurRadius = 18f * fill,
            )
        } else {
            null
        }
    return if (brush != null) {
        SpanStyle(brush = brush, shadow = shadow)
    } else {
        SpanStyle(color = color, shadow = shadow)
    }
}

private fun lerpColor(
    start: Color,
    stop: Color,
    fraction: Float,
): Color =
    Color(
        red = start.red + (stop.red - start.red) * fraction,
        green = start.green + (stop.green - start.green) * fraction,
        blue = start.blue + (stop.blue - start.blue) * fraction,
        alpha = start.alpha + (stop.alpha - start.alpha) * fraction,
    )
