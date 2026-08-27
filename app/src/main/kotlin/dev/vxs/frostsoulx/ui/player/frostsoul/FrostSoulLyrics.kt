/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import android.widget.Toast
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.bush.translator.Language
import me.bush.translator.Translator
import dagger.hilt.android.EntryPointAccessors
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.utils.TranslatorLang
import dev.vxs.frostsoulx.utils.TranslatorLanguages
import dev.vxs.frostsoulx.di.LyricsHelperEntryPoint
import dev.vxs.frostsoulx.lyrics.core.LyricsLine
import dev.vxs.frostsoulx.lyrics.core.LyricsSyncState
import dev.vxs.frostsoulx.utils.rememberPreference
import coil3.compose.AsyncImage

private val LyricsHeaderPadding = 24.dp
private val LyricsActiveFontSize = 32.sp
private val LyricsInactiveFontSize = 27.sp
private val LyricsControlLabelSize = 10.sp

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
    artworkUrl: String?,
    title: String,
    artist: String,
    isPlaying: Boolean,
    isLiked: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onTogglePlayPause: () -> Unit = {},
    onToggleLike: () -> Unit = {},
    onOpenAudioOutput: () -> Unit = {},
    onRefetchLyrics: () -> Unit = {},
    isRefetchingLyrics: Boolean = false,
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
    val coroutineScope = rememberCoroutineScope()
    val translatorLanguages = remember(applicationContext) { TranslatorLanguages.load(applicationContext) }
    var languageMenuExpanded by remember(rawLyrics) { mutableStateOf(false) }
    var showTranslation by remember(rawLyrics) { mutableStateOf(false) }
    var selectedLanguageCode by remember(rawLyrics) { mutableStateOf("ENGLISH") }
    var translatedLines by remember(rawLyrics) { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isTranslating by remember(rawLyrics) { mutableStateOf(false) }

    fun translateCurrentLyrics(languageCode: String) {
        if (lines.isEmpty() || isTranslating) return
        isTranslating = true
        coroutineScope.launch {
            try {
                val language = Language(languageCode)
                val translated =
                    withContext(Dispatchers.IO) {
                        val translator = Translator()
                        lines.mapIndexed { index, line ->
                            index to translator.translateBlocking(line.text, language).translatedText
                        }.toMap()
                    }
                translatedLines = translated
                showTranslation = true
            } catch (error: Exception) {
                Toast.makeText(
                    applicationContext,
                    "Translation failed: ${error.localizedMessage ?: "try again"}",
                    Toast.LENGTH_SHORT,
                ).show()
            } finally {
                isTranslating = false
            }
        }
    }

    LaunchedEffect(currentIndex, lines.size) {
        if (currentIndex >= 0 && listState.isScrollInProgress.not()) {
            listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

        PremiumLyricsBackgroundContainer(artworkUrl = artworkUrl, modifier = modifier) {
        if (lines.isEmpty()) {
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            ) {
                Text(
                    text = if (rawLyrics.isNullOrBlank()) "Lyrics are unavailable for this track." else "Preparing synchronized lyrics for this track.",
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
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(
                        start = LyricsHeaderPadding,
                        end = LyricsHeaderPadding,
                        top = 12.dp,
                        bottom = 8.dp,
                    ),
                ) {
                    Text(
                        text = title,
                        color = FrostSoulOnSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artist,
                        color = FrostSoulOnSurfaceMuted,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 156.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    itemsIndexed(
                        items = lines,
                        key = { _, line -> "${line.startMs}-${line.endMs}-${line.text}" },
                    ) { index, line ->
                        val displayLine = if (showTranslation) {
                            line.copy(text = translatedLines[index] ?: line.text, words = emptyList(), translation = null)
                        } else {
                            line
                        }
                        val isCurrent = index == currentIndex
                        FrostSoulKaraokeLine(
                            line = displayLine,
                            isCurrent = isCurrent,
                            isPast = currentIndex >= 0 && index < currentIndex,
                            currentWordIndex = if (isCurrent) syncState.currentWordIndex else -1,
                            wordProgress = if (isCurrent) syncState.wordProgress else 0f,
                            lineProgress = if (isCurrent) syncState.lineProgress else 0f,
                            onSeek = onSeek,
                        )
                    }
                }
            }
        }

        FrostSoulLyricsBottomControls(
            onToggleLike = onToggleLike,
            onTogglePlayPause = onTogglePlayPause,
            onRefetchLyrics = onRefetchLyrics,
            isRefetchingLyrics = isRefetchingLyrics,
            isPlaying = isPlaying,
            isLiked = isLiked,
            showTranslation = showTranslation,
            isTranslating = isTranslating,
            onToggleTranslation = {
                if (showTranslation) {
                    showTranslation = false
                } else if (translatedLines.isNotEmpty()) {
                    showTranslation = true
                } else {
                    translateCurrentLyrics(selectedLanguageCode)
                }
            },
            onChooseTranslationLanguage = { code ->
                selectedLanguageCode = code
                languageMenuExpanded = false
                translateCurrentLyrics(code)
            },
            languageMenuExpanded = languageMenuExpanded,
            onLanguageMenuExpandedChange = { languageMenuExpanded = it },
            languages = translatorLanguages,
        )

        if (isRefetchingLyrics || isTranslating) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)),
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(42.dp),
                )
            }
        }

    }
}

@Composable
private fun BoxScope.FrostSoulLyricsBottomControls(
    onToggleLike: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onRefetchLyrics: () -> Unit,
    isRefetchingLyrics: Boolean,
    isPlaying: Boolean,
    isLiked: Boolean,
    showTranslation: Boolean,
    isTranslating: Boolean,
    onToggleTranslation: () -> Unit,
    onChooseTranslationLanguage: (String) -> Unit,
    languageMenuExpanded: Boolean,
    onLanguageMenuExpandedChange: (Boolean) -> Unit,
    languages: List<TranslatorLang>,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .background(Color.Black.copy(alpha = 0.78f), RoundedCornerShape(28.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        LyricsActionButton(
            painter = painterResource(R.drawable.sync),
            label = "Refresh",
            contentDescription = "Refetch lyrics",
            onClick = onRefetchLyrics,
            enabled = !isRefetchingLyrics && !isTranslating,
            active = isRefetchingLyrics,
        )
        Box {
            LyricsActionButton(
                painter = painterResource(R.drawable.translate),
                label = "Translate",
                contentDescription = "Translate lyrics",
                onClick = {
                    if (!isTranslating) {
                        if (showTranslation) onToggleTranslation() else onLanguageMenuExpandedChange(true)
                    }
                },
                enabled = !isRefetchingLyrics,
                active = showTranslation || isTranslating,
            )
            androidx.compose.material3.DropdownMenu(
                expanded = languageMenuExpanded,
                onDismissRequest = { onLanguageMenuExpandedChange(false) },
            ) {
                languages.forEach { language ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(language.name, color = Color.White, fontSize = 14.sp) },
                        onClick = { onChooseTranslationLanguage(language.code) },
                    )
                }
            }
        }
        LyricsActionButton(
            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            label = "Like",
            contentDescription = "Like this song",
            onClick = onToggleLike,
            enabled = !isRefetchingLyrics && !isTranslating,
            active = isLiked,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .background(FrostSoulTheme.colors.accentBright, androidx.compose.foundation.shape.CircleShape)
                .clickable(
                    enabled = !isRefetchingLyrics && !isTranslating,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTogglePlayPause,
                ),
        ) {
            Icon(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = "Play or pause",
                tint = Color.Black,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun LyricsActionButton(
    painter: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    active: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FSIconButton(
            painter = painter,
            contentDescription = contentDescription,
            onClick = onClick,
            enabled = enabled,
            active = active,
            compact = true,
            buttonSize = 30.dp,
            iconSize = 22.dp,
            showContainer = false,
        )
        Text(
            text = label,
            color = if (active) FrostSoulTheme.colors.accentBright else FrostSoulOnSurfaceMuted,
            fontSize = LyricsControlLabelSize,
            maxLines = 1,
        )
    }
}

@Composable
private fun PremiumLyricsBackgroundContainer(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    Box(
        modifier = modifier.fillMaxSize().background(
            if (artworkUrl.isNullOrBlank()) FrostSoulTheme.colors.background else Color.Transparent,
        ),
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(80.dp).alpha(0.82f),
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = if (isLightTheme) {
                        listOf(Color.White.copy(alpha = 0.42f), FrostSoulTheme.colors.background.copy(alpha = 0.82f))
                    } else {
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.42f))
                    },
                ),
            ),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                if (isLightTheme) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.18f),
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
        targetValue = if (isCurrent) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f),
        label = "fs-karaoke-line-scale",
    )
    val lineColor =
        when {
            isCurrent -> FrostSoulOnSurface
            isPast -> FrostSoulOnSurfaceMuted.copy(alpha = 0.70f)
            else -> FrostSoulOnSurfaceMuted.copy(alpha = 0.45f)
        }
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
            fontSize = if (isCurrent) LyricsActiveFontSize else LyricsInactiveFontSize,
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
