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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vxs.frostsoulx.lyrics.LyricsEntry
import dev.vxs.frostsoulx.lyrics.LyricsUtils
import kotlin.math.max

@Composable
internal fun FSLyrics(
    rawLyrics: String?,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries =
        remember(rawLyrics, durationMs) {
            rawLyrics?.let { lyrics ->
                val normalized = LyricsUtils.normalizeLyricsText(lyrics)
                when {
                    normalized.isBlank() || !LyricsUtils.hasMeaningfulLyricsContent(normalized) -> emptyList()
                    LyricsUtils.isTtml(normalized) -> LyricsUtils.parseTtml(normalized, (durationMs / 1_000L).toInt())
                    LyricsUtils.isLineSyncedLrc(normalized) -> LyricsUtils.parseLyrics(normalized)
                    else ->
                        LyricsUtils
                            .displayLyricsText(normalized)
                            .lineSequence()
                            .mapIndexed { index, line -> LyricsEntry(time = index.toLong() * 1_000L, text = line) }
                            .toList()
                }
            }.orEmpty()
        }
    val currentIndex =
        remember(entries, positionMs) {
            entries.indexOfLast { entry -> entry.time <= positionMs }.coerceAtLeast(0)
        }
    val listState = rememberLazyListState()

    LaunchedEffect(currentIndex, entries.size) {
        if (entries.isNotEmpty() && listState.isScrollInProgress.not()) {
            listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    if (entries.isEmpty()) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier.fillMaxSize().padding(horizontal = 28.dp),
        ) {
            Text(
                text = "Lyrics are unavailable for this track.",
                color = FrostSoulOnSurfaceMuted,
                fontSize = 19.sp,
                lineHeight = 28.sp,
            )
            Text(
                text = "When timed lyrics are available, FrostSoul follows every word in real time.",
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
            items = entries,
            key = { _, entry -> "${entry.time}-${entry.text}" },
        ) { index, entry ->
            FrostSoulKaraokeLine(
                entry = entry,
                positionMs = positionMs,
                isCurrent = index == currentIndex,
                isPast = index < currentIndex,
                onSeek = onSeek,
            )
        }
    }
}

@Composable
private fun FrostSoulKaraokeLine(
    entry: LyricsEntry,
    positionMs: Long,
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
        remember(entry, positionMs, isCurrent, lineColor, emphasis) {
            entry.asKaraokeAnnotatedText(
                positionMs = positionMs,
                activeLine = isCurrent,
                inactiveColor = lineColor,
                activeColor = FrostSoulCyanBright,
                glowStrength = emphasis,
            )
        }

    Text(
        text = annotatedText,
        color = lineColor,
        fontSize = if (isCurrent) 27.sp else 23.sp,
        lineHeight = if (isCurrent) 36.sp else 31.sp,
        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier =
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = 0.56f + (emphasis * 0.44f)
                }.clickable(
                    enabled = entry.time >= 0L,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onSeek(entry.time) },
                ),
    )
}

private fun LyricsEntry.asKaraokeAnnotatedText(
    positionMs: Long,
    activeLine: Boolean,
    inactiveColor: Color,
    activeColor: Color,
    glowStrength: Float,
) =
    buildAnnotatedString {
        val timedWords = words.orEmpty()
        if (!activeLine || timedWords.isEmpty()) {
            withStyle(
                SpanStyle(
                    color = inactiveColor,
                    shadow =
                        if (activeLine) {
                            Shadow(color = activeColor.copy(alpha = 0.42f * glowStrength), blurRadius = 15f)
                        } else {
                            null
                        },
                ),
            ) {
                append(text)
            }
            return@buildAnnotatedString
        }

        timedWords.forEachIndexed { index, word ->
            val wordStart = (word.startTime * 1_000.0).toLong()
            val wordEnd = max(wordStart + 1L, (word.endTime * 1_000.0).toLong())
            val wordProgress = ((positionMs - wordStart).toFloat() / (wordEnd - wordStart).toFloat()).coerceIn(0f, 1f)
            val color = lerpColor(inactiveColor, activeColor, wordProgress)
            withStyle(
                SpanStyle(
                    color = color,
                    shadow =
                        if (wordProgress > 0.02f) {
                            Shadow(
                                color = activeColor.copy(alpha = 0.55f * wordProgress * glowStrength),
                                blurRadius = 18f * wordProgress,
                            )
                        } else {
                            null
                        },
                ),
            ) {
                append(word.text)
                if (index < timedWords.lastIndex && word.text.lastOrNull()?.isWhitespace() != true) append(" ")
            }
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
