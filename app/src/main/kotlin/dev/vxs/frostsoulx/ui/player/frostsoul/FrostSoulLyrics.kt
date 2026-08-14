/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.EntryPointAccessors
import dev.vxs.frostsoulx.di.LyricsHelperEntryPoint
import dev.vxs.frostsoulx.lyrics.core.LyricsLine
import dev.vxs.frostsoulx.lyrics.core.LyricsSyncState

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

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(vertical = 88.dp, horizontal = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        itemsIndexed(
            items = lines,
            key = { _, line -> "${line.startMs}-${line.endMs}-${line.text}" },
        ) { index, line ->
            FrostSoulKaraokeLine(
                line = line,
                syncState = syncState,
                isCurrent = index == currentIndex,
                isPast = currentIndex >= 0 && index < currentIndex,
                onSeek = onSeek,
            )
        }
    }
}

@Composable
private fun FrostSoulKaraokeLine(
    line: LyricsLine,
    syncState: LyricsSyncState,
    isCurrent: Boolean,
    isPast: Boolean,
    onSeek: (Long) -> Unit,
) {
    val emphasis by animateFloatAsState(
        targetValue = if (isCurrent) 1f else if (isPast) 0.60f else 0f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 500f),
        label = "fs-karaoke-line-emphasis",
    )
    val scale by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
        label = "fs-karaoke-line-scale",
    )
    val lineColor =
        when {
            isCurrent -> FrostSoulOnSurface
            isPast -> FrostSoulOnSurfaceMuted.copy(alpha = 0.72f)
            else -> FrostSoulOnSurfaceMuted.copy(alpha = 0.46f)
        }
    val annotatedText =
        remember(line, syncState.currentWordIndex, syncState.wordProgress, syncState.lineProgress, isCurrent, lineColor, emphasis) {
            line.asKaraokeAnnotatedText(
                activeLine = isCurrent,
                currentWordIndex = syncState.currentWordIndex,
                wordProgress = syncState.wordProgress,
                lineProgress = syncState.lineProgress,
                inactiveColor = lineColor,
                activeColor = FrostSoulCyan,
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
                    alpha = 0.56f + (emphasis * 0.44f)
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
            fontSize = if (isCurrent) 27.sp else 23.sp,
            lineHeight = if (isCurrent) 36.sp else 31.sp,
            fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        line.translation?.takeIf { it.isNotBlank() }?.let { translation ->
            Text(
                text = translation,
                color = FrostSoulOnSurfaceMuted.copy(alpha = if (isCurrent) 0.84f else 0.48f),
                fontSize = if (isCurrent) 16.sp else 14.sp,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        line.romanization?.takeIf { it.isNotBlank() }?.let { romanization ->
            Text(
                text = romanization,
                color = FrostSoulOnSurfaceMuted.copy(alpha = if (isCurrent) 0.70f else 0.40f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
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
