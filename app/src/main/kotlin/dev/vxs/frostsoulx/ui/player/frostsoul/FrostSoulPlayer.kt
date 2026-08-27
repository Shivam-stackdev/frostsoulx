/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vxs.frostsoulx.ui.frostsoul.FSIcon as Icon
import dev.vxs.frostsoulx.ui.frostsoul.FSText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.constants.PlayerBackgroundStyle
import dev.vxs.frostsoulx.constants.PlayerDesignStyle
import dev.vxs.frostsoulx.lyrics.core.LyricsLine
import dev.vxs.frostsoulx.innertube.YouTube
import dev.vxs.frostsoulx.ui.frostsoul.FSButton
import dev.vxs.frostsoulx.ui.frostsoul.MinimalistMetadataChip
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme
import dev.vxs.frostsoulx.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

@Composable
internal fun FrostSoulPlayer(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    playerDesignStyle: dev.vxs.frostsoulx.constants.PlayerDesignStyle = dev.vxs.frostsoulx.constants.PlayerDesignStyle.FROSTSOUL,
    onSearchTrack: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // QQ-style pager: Recommendations stay on the left, Main Player in the center, Lyrics on the right.
    val pages = remember { listOf(FrostSoulPage.Recommendations, FrostSoulPage.MainPlayer, FrostSoulPage.Lyrics) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var queueVisible by remember { mutableStateOf(false) }
    var showArtistDialog by remember(uiState.track.id) { mutableStateOf(false) }
    var showPagerDots by remember { mutableStateOf(true) }
    var downwardDragDistance by remember { mutableFloatStateOf(0f) }
    val settledDragOffset by animateFloatAsState(
        targetValue = downwardDragDistance,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "frostsoul-player-dismiss-drag",
    )
    val collapseFraction = (settledDragOffset / 280f).coerceIn(0f, 1f)
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        showPagerDots = true
        if (!pagerState.isScrollInProgress) {
            delay(1_000L)
            if (!pagerState.isScrollInProgress) showPagerDots = false
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .graphicsLayer {
                    translationY = settledDragOffset
                    scaleX = 1f - collapseFraction * 0.035f
                    scaleY = 1f - collapseFraction * 0.035f
                    alpha = 1f - collapseFraction * 0.18f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .pointerInput(actions.onDismiss, queueVisible) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (!queueVisible) {
                                downwardDragDistance = (downwardDragDistance + dragAmount).coerceAtLeast(0f)
                            }
                        },
                        onDragEnd = {
                            if (downwardDragDistance >= 112f) actions.onDismiss()
                            downwardDragDistance = 0f
                        },
                        onDragCancel = { downwardDragDistance = 0f },
                    )
                },
        ) {
            FrostSoulDynamicBackground(
                artworkUrl = uiState.track.artworkUrl,
                playerDesignStyle = playerDesignStyle,
                playerBackgroundStyle = uiState.playerBackgroundStyle,
                blurRadius = uiState.blurRadius,
                palette = uiState.palette,
                moodSeed = "${uiState.track.title} ${uiState.track.artist} ${uiState.track.album}",
            )
            Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(
                        start = PlayerLayoutTokens.MasterHorizontalPadding,
                        end = PlayerLayoutTokens.MasterHorizontalPadding,
                        top = 4.dp,
                        bottom = 6.dp,
                    ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.expand_more),
                    contentDescription = "Collapse player",
                    tint = FrostSoulTheme.colors.onSurface,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(28.dp)
                        .clickable(onClick = actions.onDismiss),
                )
                if (showPagerDots) {
                    FrostSoulPagerDots(
                        pageCount = pages.size,
                        selectedPage = pagerState.currentPage,
                        selectedPageOffsetFraction = pagerState.currentPageOffsetFraction,
                        emphasizeSelected = pagerState.isScrollInProgress,
                        onPageSelected = { targetPage ->
                            scope.launch { pagerState.animateScrollToPage(targetPage) }
                        },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                key = { index -> pages[index].name },
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                val pageDistance = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Keep pages flat and full-bleed: only a light cross-fade so
                                // adjacent pages never look scaled-in or pushed off-centre.
                                val distance = kotlin.math.abs(pageDistance).coerceIn(0f, 1f)
                                alpha = (1f - distance * 0.28f).coerceIn(0.70f, 1f)
                            },
                ) {
                    when (pages[pageIndex]) {
                        FrostSoulPage.Lyrics ->
                            FSLyrics(
                                rawLyrics = uiState.lyrics,
                                title = uiState.track.title,
                                artist = uiState.track.artist,
                                isPlaying = uiState.isPlaying,
                                isLiked = uiState.track.isLiked,
                                positionMs = uiState.positionMs,
                                durationMs = uiState.safeDurationMs,
                                onSeek = actions.onSeek,
                                onTogglePlayPause = actions.onTogglePlayPause,
                                onToggleLike = actions.onToggleLike,
                                onOpenAudioOutput = actions.onOpenAudioOutput,
                                onRefetchLyrics = actions.onRefetchLyrics,
                                isRefetchingLyrics = actions.isRefetchingLyrics,
                            )

                        FrostSoulPage.MainPlayer ->
                            if (playerDesignStyle == dev.vxs.frostsoulx.constants.PlayerDesignStyle.ARTWORK_BLUR) {
                                FrostSoulArtworkBlurAlbumPage(
                                    uiState = uiState,
                                    actions = actions,
                                    onOpenQueue = { queueVisible = true },
                                    onOpenOptions = actions.onOpenOptions,
                                    onSearchTrack = onSearchTrack,
                                    onShowArtists = { showArtistDialog = true },
                                )
                            } else {
                                FrostSoulAlbumPage(
                                    uiState = uiState,
                                    actions = actions,
                                    onOpenQueue = { queueVisible = true },
                                    onOpenOptions = actions.onOpenOptions,
                                    onSearchTrack = onSearchTrack,
                                    onShowArtists = { showArtistDialog = true },
                                )
                            }
                        FrostSoulPage.Recommendations -> FrostSoulRecommendationsPage(uiState = uiState, actions = actions)
                    }
                }
            }
        }
        if (showArtistDialog) {
            Dialog(
                onDismissRequest = { showArtistDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    contentAlignment = Alignment.BottomCenter,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                ) {
                    FrostSoulArtistDialog(
                        artists = uiState.track.artists.ifEmpty {
                            uiState.track.artist
                                .split(" • ")
                                .map { name -> FrostSoulArtist(name = name.trim()) }
                                .filter { it.name.isNotBlank() }
                        },
                        onDismiss = { showArtistDialog = false },
                        onOpenArtist = { artistId ->
                            showArtistDialog = false
                            onOpenArtist(artistId)
                        },
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = queueVisible,
            enter =
                fadeIn(animationSpec = tween(160)) +
                    slideInVertically(
                        animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
                    ) { height -> height / 2 } +
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = spring(dampingRatio = 0.84f, stiffness = 500f),
                    ),
            exit =
                fadeOut(animationSpec = tween(120)) +
                    slideOutVertically(animationSpec = tween(180)) { height -> height / 3 } +
                    scaleOut(targetScale = 0.96f, animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            FSGlassCard(
                accent = uiState.palette.accent,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .graphicsLayer {
                            shadowElevation = 28.dp.toPx()
                            shape = RoundedCornerShape(30.dp)
                            clip = false
                        },
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp),
                    ) {
                        Text(
                            text = uiState.queueTitle ?: "Up next",
                            color = FrostSoulOnSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        FSIconButton(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Close playback queue",
                            onClick = { queueVisible = false },
                            compact = true,
                        )
                    }
                    FSQueue(
                        title = "",
                        queue = uiState.queue,
                        onSelect = { index ->
                            actions.onSelectQueueItem(index)
                            queueVisible = false
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun FSMiniPlayer(
    track: FrostSoulTrack,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    palette: FrostSoulPalette,
    height: androidx.compose.ui.unit.Dp,
    artworkSize: androidx.compose.ui.unit.Dp,
    peeked: Boolean,
    shape: RoundedCornerShape,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    onCardClick: () -> Unit,
    onLongPress: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLike: () -> Unit,
    onQueueClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val rawProgress =
        if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(220),
        label = "frostsoul-mini-player-progress",
    )
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    val backgroundColor = FrostSoulTheme.colors.surface
    val primaryTextColor = if (isLightTheme) FrostSoulTheme.colors.onSurface else FrostSoulOnSurface
    val mutedTextColor = if (isLightTheme) FrostSoulTheme.colors.onSurfaceMuted else FrostSoulOnSurfaceMuted
    val progressColor = if (isLightTheme) FrostSoulTheme.colors.accentBright else palette.accent

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .graphicsLayer {
                    shadowElevation = if (isPlaying) 18.dp.toPx() else 8.dp.toPx()
                    this.shape = shape
                    clip = false
                }
                .clip(shape)
                .background(backgroundColor.copy(alpha = 0.94f))
                .border(1.dp, palette.accent.copy(alpha = if (isPlaying) 0.48f else 0.20f), shape)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onCardClick,
                    onLongClick = onLongPress,
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(8.dp)).background(FrostSoulSurface),
            ) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = "Album artwork for ${track.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (track.artworkUrl.isNullOrBlank()) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = mutedTextColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = primaryTextColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    ) {
                        append(track.title)
                    }
                    if (track.artist.isNotBlank()) {
                        withStyle(
                            SpanStyle(
                                color = mutedTextColor,
                                fontSize = 13.sp,
                            ),
                        ) {
                            append("  -  ${track.artist}")
                        }
                    }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 18.dp),
            )
            FSIconButton(
                painter = painterResource(if (track.isLiked) R.drawable.favorite else R.drawable.favorite_border),
                contentDescription = if (track.isLiked) "Remove from favorites" else "Add to favorites",
                onClick = onToggleLike,
                active = track.isLiked,
                buttonSize = 24.dp,
                iconSize = 24.dp,
                showContainer = false,
                modifier = Modifier.padding(end = 18.dp),
            )
            FSIconButton(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = onTogglePlayPause,
                active = isPlaying,
                buttonSize = 28.dp,
                iconSize = 28.dp,
                showContainer = false,
                modifier = Modifier.padding(end = 18.dp),
            )
            onQueueClick?.let { openQueue ->
                FSIconButton(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Open queue",
                    onClick = openQueue,
                    buttonSize = 24.dp,
                    iconSize = 24.dp,
                    showContainer = false,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress)
                    .height(if (peeked) 3.dp else 2.dp)
                    .background(progressColor),
        )
    }
}

@Composable
internal fun FSPlayerControls(
    state: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(modifier = Modifier.weight(1f))
            FSDownloadButton(
                progress = state.downloadProgress,
                onClick = actions.onDownload,
            )
            FSIconButton(
                painter = painterResource(R.drawable.bedtime),
                contentDescription = if (state.sleepTimerActive) "Clear sleep timer" else "Set sleep timer",
                onClick = actions.onOpenSleepTimer,
                active = state.sleepTimerActive,
                buttonSize = 32.dp,
                iconSize = 21.dp,
                showContainer = false,
            )
            FrostSoulOutputDeviceButton(
                device = state.outputDevice,
                onClick = actions.onOpenAudioOutput,
            )
            FSTwoDotButton(onClick = actions.onOpenOptions)
        }
        FSSeekbar(
            progress = state.progress,
            durationMs = state.safeDurationMs,
            onSeek = actions.onSeek,
            accent = state.palette.accent,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                state.positionMs.asFrostSoulTime(),
                style = PlayerLayoutTokens.TimelineTimeStyle.copy(color = FrostSoulOnSurfaceMuted),
            )
            Text(
                state.safeDurationMs.asFrostSoulTime(),
                style = PlayerLayoutTokens.TimelineTimeStyle.copy(color = FrostSoulOnSurfaceMuted),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FSIconButton(
                painter = painterResource(R.drawable.repeat),
                contentDescription = "Toggle repeat mode",
                onClick = actions.onToggleRepeat,
                buttonSize = 32.dp,
                iconSize = 24.dp,
                showContainer = false,
            )
            FSIconButton(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous track",
                onClick = actions.onSkipPrevious,
                enabled = state.canSkipPrevious,
                buttonSize = 32.dp,
                iconSize = 32.dp,
                showContainer = false,
            )
            FSPlayButton(
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                onClick = actions.onTogglePlayPause,
                accent = state.palette.accent,
            )
            FSIconButton(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = "Next track",
                onClick = actions.onSkipNext,
                enabled = state.canSkipNext,
                buttonSize = 32.dp,
                iconSize = 32.dp,
                showContainer = false,
            )
            FSIconButton(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = "Open playback queue",
                onClick = onOpenQueue,
                buttonSize = 32.dp,
                iconSize = 24.dp,
                showContainer = false,
            )
        }
    }
}

@Composable
private fun FSDownloadButton(
    progress: Float?,
    onClick: () -> Unit,
) {
    val normalizedProgress = progress?.coerceIn(0f, 1f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp).clickable(onClick = onClick),
    ) {
        normalizedProgress?.let { value ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.White.copy(alpha = 0.22f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()),
                )
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = 360f * value,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Icon(
            painter = painterResource(if (normalizedProgress == 1f) R.drawable.check else R.drawable.ic_download),
            contentDescription = if (normalizedProgress == null) "Download song" else "Download progress ${((normalizedProgress * 100f).toInt())}%",
            tint = Color.White,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun FSTwoDotButton(onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(36.dp).clickable(onClick = onClick),
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(FrostSoulTheme.colors.onSurface, androidx.compose.foundation.shape.CircleShape),
            )
        }
    }
}

@Composable
private fun FSPlayButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    accent: Color,
) {
    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.94f,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 540f),
        label = "fs-play-button-scale",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(if (isBuffering) 24.dp else 30.dp).alpha(if (isBuffering) 0.54f else 1f),
        )
    }
}

@Composable
internal fun FrostSoulPagerDots(
    pageCount: Int,
    selectedPage: Int,
    selectedPageOffsetFraction: Float = 0f,
    emphasizeSelected: Boolean = true,
    onPageSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pagerPosition =
        (selectedPage + selectedPageOffsetFraction)
            .coerceIn(0f, (pageCount - 1).coerceAtLeast(0).toFloat())
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        repeat(pageCount) { index ->
            val selection = (1f - kotlin.math.abs(pagerPosition - index.toFloat())).coerceIn(0f, 1f)
            val width = if (emphasizeSelected) 7.dp + (22.dp - 7.dp) * selection else 7.dp
            val alpha = if (emphasizeSelected) 0.36f + (1f - 0.36f) * selection else if (index == selectedPage) 0.95f else 0.34f
            Box(
                modifier =
                    Modifier
                        .height(4.dp)
                        .width(width)
                        .graphicsLayer {
                            this.alpha = alpha
                            shadowElevation = 10.dp.toPx() * selection
                        }
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (selection > 0.5f) Color.White else Color.White.copy(alpha = 0.34f))
                        .clickable { onPageSelected(index) },
            )
        }
    }
}

@Composable
private fun FrostSoulArtistDialog(
    artists: List<FrostSoulArtist>,
    onDismiss: () -> Unit,
    onOpenArtist: (String) -> Unit,
) {
    FSGlassCard(
        accent = Color.White,
        modifier = Modifier.fillMaxWidth().height(300.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Artists involved",
                    color = FrostSoulOnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                FSIconButton(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Close artists dialog",
                    onClick = onDismiss,
                    compact = true,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 12.dp),
            ) {
                artists.forEach { artist ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),

                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(FrostSoulSurfaceElevated)
                                .clickable(enabled = !artist.id.isNullOrBlank()) {
                                    artist.id?.let(onOpenArtist)
                                },
                        ) {
                            if (artist.artworkUrl.isNullOrBlank()) {
                                Text(
                                    text = artist.name.take(1).uppercase(),
                                    color = FrostSoulOnSurface,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            } else {
                                AsyncImage(
                                    model = artist.artworkUrl,
                                    contentDescription = "${artist.name} artist image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                        Text(
                            text = artist.name,
                            color = FrostSoulOnSurface,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostSoulMainLyricPreview(
    uiState: FrostSoulPlayerUiState,
    onlyCurrentLine: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val currentLine = uiState.currentLyricModel
    if (currentLine == null && uiState.lyricPreviewLines.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = PlayerLayoutTokens.MasterHorizontalPadding),
    ) {
        currentLine?.let { line ->
            Text(
                text = line.asMainPlayerKaraokeText(
                    currentWordIndex = uiState.currentWordIndex,
                    wordProgress = uiState.currentWordProgress,
                    lineProgress = uiState.currentLineProgress,
                ),
                color = FrostSoulOnSurface.copy(alpha = 0.92f),
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (onlyCurrentLine) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        } ?: uiState.currentLyricLine?.takeIf { it.isNotBlank() }?.let { line ->
            Text(
                text = line,
                color = FrostSoulOnSurface.copy(alpha = 0.92f),
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (onlyCurrentLine) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!onlyCurrentLine) {
            uiState.lyricPreviewLines.drop(1).take(3).forEach { line ->
                Text(
                    text = line,
                    color = FrostSoulOnSurfaceMuted.copy(alpha = 0.58f),
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun LyricsLine.asMainPlayerKaraokeText(
    currentWordIndex: Int,
    wordProgress: Float,
    lineProgress: Float,
): androidx.compose.ui.text.AnnotatedString =
    buildAnnotatedString {
        if (words.isEmpty()) {
            val fill = lineProgress.coerceIn(0f, 1f)
            withStyle(
                SpanStyle(
                    color = Color.White.copy(alpha = 0.68f + (0.32f * fill)),
                    shadow = Shadow(
                        color = Color.White.copy(alpha = 0.32f * fill),
                        blurRadius = 14f * fill,
                    ),
                ),
            ) {
                append(text)
            }
            return@buildAnnotatedString
        }

        words.forEachIndexed { index, word ->
            val fill = when {
                index < currentWordIndex -> 1f
                index == currentWordIndex -> wordProgress.coerceIn(0f, 1f)
                else -> 0f
            }
            val wordColor = when {
                fill <= 0.02f -> FrostSoulOnSurfaceMuted.copy(alpha = 0.72f)
                fill >= 0.98f -> Color.White
                else -> Color.Unspecified
            }
            val brush = if (fill > 0.02f && fill < 0.98f) {
                Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0f to Color.White,
                        fill to Color.White,
                        (fill + 0.02f).coerceAtMost(1f) to FrostSoulOnSurfaceMuted.copy(alpha = 0.72f),
                        1f to FrostSoulOnSurfaceMuted.copy(alpha = 0.72f),
                    ),
                )
            } else {
                null
            }
            withStyle(
                if (brush != null) {
                    SpanStyle(
                        brush = brush,
                        shadow = if (fill > 0.02f) Shadow(color = Color.White.copy(alpha = 0.44f * fill), blurRadius = 14f * fill) else null,
                    )
                } else {
                    SpanStyle(
                        color = wordColor,
                        shadow = if (fill > 0.02f) Shadow(color = Color.White.copy(alpha = 0.44f * fill), blurRadius = 14f * fill) else null,
                    )
                },
            ) {
                append(word.text)
                if (index < words.lastIndex && word.text.lastOrNull()?.isWhitespace() != true) append(" ")
            }
        }
    }

@Composable
private fun FrostSoulAlbumPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit,
    onSearchTrack: () -> Unit,
    onShowArtists: () -> Unit,
) {
    val titleScrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize().padding(horizontal = PlayerLayoutTokens.MasterHorizontalPadding)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            FSAlbumArt(
                artworkUrl = uiState.track.artworkUrl,
                title = uiState.track.title,
                isPlaying = uiState.isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(maxWidth = PlayerLayoutTokens.TurntableCardSize)
                    .aspectRatio(1f),
            )
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 4.dp),
            ) {
                FSIconButton(
                    painter = painterResource(if (uiState.track.isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = if (uiState.track.isLiked) "Unlike track" else "Like track",
                    onClick = actions.onToggleLike,
                    active = uiState.track.isLiked,
                    compact = true,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(titleScrollState)
                            .clickable(onClick = onSearchTrack),
                    ) {
                        Text(
                            text = uiState.track.title,
                            style = PlayerLayoutTokens.TrackTitleStyle.copy(color = FrostSoulOnSurface),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Text(
                        text = uiState.track.artist,
                        style = PlayerLayoutTokens.ArtistSubtitleStyle.copy(color = FrostSoulOnSurfaceMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp).clickable(onClick = onShowArtists),
                    )
            }
            Spacer(modifier = Modifier.weight(1f))
            FrostSoulMainLyricPreview(
                uiState = uiState,
                onlyCurrentLine = true,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp),
            )
            FSPlayerControls(
                    state = uiState,
                    actions = actions,
                    onOpenQueue = onOpenQueue,
                    modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun FrostSoulArtworkBlurAlbumPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit,
    onSearchTrack: () -> Unit,
    onShowArtists: () -> Unit,
) {
    val titleScrollState = rememberScrollState()
    val artworkHeaderBlur =
        if (uiState.blurRadius > 0f) {
            (uiState.blurRadius + 18f).coerceIn(18f, 120f)
        } else {
            0f
        }
    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Full-bleed artwork header: the image spans the whole width with no card
            // inset, and fades edge-to-edge into the page background so the thumbnail
            // reads as one seamless surface (QQ Music "immersive cover" behaviour).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerLayoutTokens.ArtworkBlurHeaderHeight),
            ) {
                if (!uiState.track.artworkUrl.isNullOrBlank()) {
                    // Artwork Blur only: use an enlarged, low-opacity duplicate as an ambient
                    // canvas so the sharp cover never reads as a floating rectangle.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        uiState.palette.artworkPrimary.copy(alpha = 0.52f),
                                        uiState.palette.artworkSecondary.copy(alpha = 0.34f),
                                        Color.Black.copy(alpha = 0.82f),
                                    ),
                                    radius = 920f,
                                ),
                            ),
                    ) {
                        AsyncImage(
                            model = uiState.track.artworkUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 1.18f
                                    scaleY = 1.18f
                                    alpha = 0.62f
                                }
                                .blur(
                                    artworkHeaderBlur.dp,
                                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                                ),
                        )
                        // Low-opacity matte keeps the bright duplicate from overpowering text.
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.22f)),
                        )
                        AsyncImage(
                            model = uiState.track.artworkUrl,
                            contentDescription = "Album artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Bottom fade blends the cover into the page with no visible seam.
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    0.00f to Color.Black.copy(alpha = 0.42f),
                                    0.14f to Color.Transparent,
                                    0.54f to Color.Transparent,
                                    0.78f to Color.Black.copy(alpha = 0.60f),
                                    1.00f to Color.Black.copy(alpha = 0.98f),
                                ),
                            ),
                        )
                        // Stronger horizontal edge fade removes visible rectangular side cuts.
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.horizontalGradient(
                                    0.00f to Color.Black.copy(alpha = 0.62f),
                                    0.12f to Color.Black.copy(alpha = 0.18f),
                                    0.25f to Color.Transparent,
                                    0.75f to Color.Transparent,
                                    0.88f to Color.Black.copy(alpha = 0.18f),
                                    1.00f to Color.Black.copy(alpha = 0.62f),
                                ),
                            ),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                colors = listOf(uiState.palette.artworkPrimary, uiState.palette.artworkSecondary),
                            ),
                        ),
                    )
                }
                Text(
                    text = uiState.track.album.ifBlank { "Now playing" },
                    color = Color.White.copy(alpha = 0.86f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.3.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = PlayerLayoutTokens.MasterHorizontalPadding,
                            end = PlayerLayoutTokens.MasterHorizontalPadding,
                            bottom = 14.dp,
                        ),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PlayerLayoutTokens.MasterHorizontalPadding,
                        end = PlayerLayoutTokens.MasterHorizontalPadding,
                        top = 14.dp,
                        bottom = 12.dp,
                    ),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(titleScrollState)
                            .clickable(onClick = onSearchTrack),
                    ) {
                        Text(
                            text = uiState.track.title,
                            color = FrostSoulOnSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Text(
                        text = uiState.track.artist,
                        color = FrostSoulOnSurfaceMuted,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp).clickable(onClick = onShowArtists),
                    )
                }
                FSIconButton(
                    painter = painterResource(if (uiState.track.isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = if (uiState.track.isLiked) "Unlike track" else "Like track",
                    onClick = actions.onToggleLike,
                    active = uiState.track.isLiked,
                    compact = true,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            FrostSoulMainLyricPreview(uiState = uiState)
        }

        FSPlayerControls(
            state = uiState,
            actions = actions,
            onOpenQueue = onOpenQueue,
            modifier = Modifier
                .padding(horizontal = PlayerLayoutTokens.MasterHorizontalPadding)
                .padding(top = 18.dp),
        )
    }
}

@Composable
private fun FrostSoulOutputDeviceButton(
    device: dev.vxs.frostsoulx.models.ActiveOutputDevice,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(FrostSoulTheme.colors.surfaceGlass)
            .border(1.dp, FrostSoulTheme.colors.outline.copy(alpha = 0.82f), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = device.type.imageVector,
            contentDescription = "Audio output device",
            tint = FrostSoulTheme.colors.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = device.name,
            color = FrostSoulTheme.colors.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 126.dp),
        )
    }
}

@Composable
private fun FrostSoulPlayerOptionsSheet(
    accent: Color,
    onDismiss: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onShareSong: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onToggleLike: () -> Unit,
) {
    val options =
        listOf(
            Triple(R.drawable.playlist_play, "Queue", true),
            Triple(R.drawable.lyrics, "Lyrics", true),
            Triple(R.drawable.bluetooth, "Audio output", true),
            Triple(R.drawable.share, "Share Song", true),
            Triple(R.drawable.favorite_border, "Like track", true),
        )
    FSGlassCard(
        accent = accent,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .height(610.dp)
                .graphicsLayer {
                    shadowElevation = 28.dp.toPx()
                    shape = RoundedCornerShape(30.dp)
                    clip = false
                },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FrostSoulOnSurfaceMuted.copy(alpha = 0.35f)),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PLAYER OPTIONS",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.7.sp,
                    )
                    Text(
                        text = "QQ-style listening tools",
                        color = FrostSoulOnSurfaceMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                FSIconButton(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Close player options",
                    onClick = onDismiss,
                    compact = true,
                )
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            ) {
                options.forEach { (icon, label, actionable) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(43.dp)
                                .clickable(enabled = actionable) {
                                    if (actionable) {
                                        when (label) {
                                            "Queue" -> onOpenQueue()
                                            "Lyrics" -> onOpenLyrics()
                                            "Audio output" -> onOpenAudioOutput()
                                            "Share Song" -> onShareSong()
                                            "Like track" -> onToggleLike()
                                        }
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp),
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = if (actionable) Color.White else FrostSoulOnSurface,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = label,
                                color = FrostSoulOnSurface,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostSoulRecommendationsPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
) {
    val recommendationQueue = uiState.queue.filterNot { it.isCurrent }.take(12)
    val albumSongs =
        uiState.queue
            .filter { item -> uiState.track.albumId != null && item.albumId == uiState.track.albumId }
            .distinctBy { it.id }
            .take(5)
            .ifEmpty { recommendationQueue.take(5) }
    val recommendationKey = remember(recommendationQueue) {
        recommendationQueue.joinToString(separator = "|") { it.id }
    }
    val viewCounts by produceState<Map<String, Int?>>(emptyMap(), recommendationKey) {
        val requestLimiter = Semaphore(permits = 3)
        value = coroutineScope {
            recommendationQueue
                .map { item ->
                    async(Dispatchers.IO) {
                        requestLimiter.withPermit {
                            item.id to YouTube.getMediaInfo(item.id).getOrNull()?.viewCount
                        }
                    }
                }.awaitAll()
                .toMap()
        }
    }
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    val primaryText = if (isLightTheme) FrostSoulTheme.colors.onSurface else FrostSoulOnSurface
    val mutedText = if (isLightTheme) FrostSoulTheme.colors.onSurfaceMuted else FrostSoulOnSurfaceMuted
    val chipText = if (isLightTheme) Color.Black else Color.White
    val chipSurface = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    val chipOutline = if (isLightTheme) Color.Black.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.18f)
    val cardSurface = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.07f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 10.dp, bottom = 6.dp),
    ) {
        // Header block keeps a single shared gutter so nothing hangs off-screen.
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PlayerLayoutTokens.MasterHorizontalPadding),
        ) {
            Text(
                text = "RECOMMENDATIONS",
                color = primaryText.copy(alpha = 0.72f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
                maxLines = 1,
            )
            Text(
                text = uiState.track.title,
                color = primaryText,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = uiState.track.artist,
                color = mutedText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                AsyncImage(
                    model = uiState.track.artworkUrl,
                    contentDescription = "Album artwork for ${uiState.track.album}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape),
                )
                Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(
                        text = uiState.track.album.ifBlank { "Unknown album" },
                        color = primaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Album",
                        color = mutedText,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 10.dp),
            ) {
                AsyncImage(
                    model = uiState.track.artworkUrl,
                    contentDescription = "Artwork for ${uiState.track.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)),
                )
                Text(
                    text = uiState.track.title,
                    color = primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp).weight(1f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                FrostSoulRecommendationChip(
                    label = uiState.audioTechnicalInfo ?: uiState.audioQualityBadge ?: "AUDIO INFO",
                    textColor = chipText,
                    surfaceColor = chipSurface,
                    outlineColor = chipOutline,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable(enabled = uiState.track.albumId != null, onClick = actions.onOpenAlbum),
            ) {
                Text(
                    text = uiState.track.album.ifBlank { "Unknown album" },
                    color = primaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(R.drawable.arrow_forward),
                    contentDescription = "Open album",
                    tint = mutedText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = PlayerLayoutTokens.MasterHorizontalPadding,
                end = PlayerLayoutTokens.MasterHorizontalPadding,
                top = 12.dp,
                bottom = 2.dp,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                FrostSoulRecommendationChip(
                    label = "UP NEXT",
                    textColor = chipText,
                    surfaceColor = chipSurface,
                    outlineColor = chipOutline,
                    emphasized = true,
                )
            }
            item {
                FrostSoulRecommendationChip(
                    label = "${recommendationQueue.size} TRACKS",
                    textColor = chipText,
                    surfaceColor = chipSurface,
                    outlineColor = chipOutline,
                )
            }
            uiState.audioQualityBadge?.takeIf { it.isNotBlank() }?.let { quality ->
                item {
                    FrostSoulRecommendationChip(
                        label = quality,
                        textColor = chipText,
                        surfaceColor = chipSurface,
                        outlineColor = chipOutline,
                    )
                }
            }
            uiState.queueTitle?.takeIf { it.isNotBlank() }?.let { title ->
                item {
                    FrostSoulRecommendationChip(
                        label = title,
                        textColor = chipText,
                        surfaceColor = chipSurface,
                        outlineColor = chipOutline,
                    )
                }
            }
        }
        if (albumSongs.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PlayerLayoutTokens.MasterHorizontalPadding,
                        end = PlayerLayoutTokens.MasterHorizontalPadding,
                        top = 12.dp,
                    ),
            ) {
                Text(
                    text = "Songs from this album",
                    color = primaryText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                albumSongs.forEach { item ->
                    FrostSoulAlbumSongRow(
                        item = item,
                        textColor = primaryText,
                        mutedTextColor = mutedText,
                        onClick = { actions.onSelectQueueItem(item.index) },
                    )
                }
            }
        }
        if (recommendationQueue.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp),
            ) {
                Text(
                    text = "No more songs in this queue.",
                    color = mutedText,
                    fontSize = 14.sp,
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = PlayerLayoutTokens.MasterHorizontalPadding,
                        end = PlayerLayoutTokens.MasterHorizontalPadding,
                        top = 14.dp,
                        bottom = 16.dp,
                    ),
            ) {
                recommendationQueue.chunked(3).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowItems.forEach { item ->
                            // Tile = artwork card + text below it, matching the QQ recommendation grid.
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { actions.onSelectQueueItem(item.index) },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(cardSurface),
                                ) {
                                    AsyncImage(
                                        model = item.artworkUrl,
                                        contentDescription = "Artwork for ${item.title}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    viewCounts[item.id]?.takeIf { it >= 0 }?.let { count ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(5.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color.Black.copy(alpha = 0.74f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(if (item.isCurrent) R.drawable.pause else R.drawable.play),
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp),
                                            )
                                            Text(
                                                text = formatRecommendationViewCount(count),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                modifier = Modifier.padding(start = 3.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = item.title,
                                    color = primaryText,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                Text(
                                    text = item.artist,
                                    color = mutedText,
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostSoulAlbumSongRow(
    item: FrostSoulQueueItem,
    textColor: Color,
    mutedTextColor: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        AsyncImage(
            model = item.artworkUrl,
            contentDescription = "Artwork for ${item.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)),
        )
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(
                text = item.title,
                color = textColor,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist,
                color = mutedTextColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            painter = painterResource(if (item.isCurrent) R.drawable.pause else R.drawable.play),
            contentDescription = if (item.isCurrent) "Playing" else "Play ${item.title}",
            tint = textColor.copy(alpha = 0.74f),
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun formatRecommendationViewCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000f)}M"
        count >= 1_000 -> "${"%.1f".format(count / 1_000f)}K"
        else -> count.toString()
    }
}

@Composable
private fun FrostSoulRecommendationChip(
    label: String,
    textColor: Color,
    surfaceColor: Color,
    outlineColor: Color,
    emphasized: Boolean = false,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (emphasized) textColor.copy(alpha = 0.14f) else surfaceColor)
            .border(1.dp, if (emphasized) textColor.copy(alpha = 0.32f) else outlineColor, RoundedCornerShape(7.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label.uppercase(),
            color = textColor.copy(alpha = if (emphasized) 0.96f else 0.78f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun FSQueue(
    title: String,
    queue: List<FrostSoulQueueItem>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        if (title.isNotBlank()) {
            item {
                Text(
                    text = title.uppercase(),
                    color = Color.White,
                    fontSize = 12.sp,
                    letterSpacing = 1.7.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                )
            }
        }
        if (queue.isEmpty()) {
            item {
                Text(
                    text = "Your queue is empty.",
                    color = FrostSoulOnSurfaceMuted,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 22.dp),
                )
            }
        }
        items(queue, key = { item -> "${item.index}-${item.id}" }) { item ->
            FrostSoulQueueRow(item = item, onClick = { onSelect(item.index) })
        }
    }
}

@Composable
private fun FrostSoulQueueRow(
    item: FrostSoulQueueItem,
    onClick: () -> Unit,
) {
    FSGlassCard(
        accent = if (item.isCurrent) Color.White else Color.White,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(10.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (item.isCurrent) Color.White.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.06f)),
            ) {
                Text(
                    text = if (item.isCurrent) "•" else (item.index + 1).toString(),
                    color = if (item.isCurrent) Color.White else FrostSoulOnSurfaceMuted,
                    fontSize = if (item.isCurrent) 24.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.width(10.dp))
            AsyncImage(
                model = item.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = if (item.isCurrent) FrostSoulOnSurface else FrostSoulOnSurfaceMuted,
                    fontSize = 15.sp,
                    fontWeight = if (item.isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artist,
                    color = FrostSoulOnSurfaceMuted.copy(alpha = 0.76f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun rememberFrostSoulPalette(artworkUrl: String?): FrostSoulPalette {
    val context = LocalContext.current
    val paletteCache =
        remember {
            object : LinkedHashMap<String, FrostSoulPalette>(PaletteCacheCapacity, 0.75f, true) {
                protected override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FrostSoulPalette>?): Boolean =
                    size > PaletteCacheCapacity
            }
        }
    var palette by remember(artworkUrl) { mutableStateOf(FrostSoulPalette.Default) }

    LaunchedEffect(artworkUrl) {
        if (artworkUrl.isNullOrBlank()) {
            palette = FrostSoulPalette.Default
            return@LaunchedEffect
        }
        paletteCache[artworkUrl]?.let {
            palette = it
            return@LaunchedEffect
        }
        val extracted =
            try {
                val request =
                    ImageRequest
                        .Builder(context)
                        .data(artworkUrl)
                        .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
                        .allowHardware(false)
                        .build()
                val bitmap =
                    withContext(Dispatchers.IO) {
                        context.imageLoader.execute(request).image?.toBitmap()
                    }
                if (bitmap == null) {
                    null
                } else {
                    val colors =
                        withContext(Dispatchers.Default) {
                            val nativePalette =
                                Palette
                                    .from(bitmap)
                                    .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                                    .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                                    .generate()
                            PlayerColorExtractor.extractGradientColors(nativePalette, Color.Black.toArgb())
                        }
                    FrostSoulPalette(
                        artworkPrimary = colors.firstOrNull() ?: Color.White,
                        artworkSecondary = colors.getOrElse(1) { FrostSoulSurfaceElevated },
                        accent = Color.White,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                null
            }
        palette = extracted ?: FrostSoulPalette.Default
        paletteCache[artworkUrl] = palette
    }
    return palette
}

private const val PaletteCacheCapacity = 24

@Composable
private fun FrostSoulDynamicBackground(
    artworkUrl: String?,
    playerDesignStyle: PlayerDesignStyle,
    playerBackgroundStyle: PlayerBackgroundStyle,
    blurRadius: Float,
    palette: FrostSoulPalette,
    moodSeed: String,
) {
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    val isVinyl = playerDesignStyle == PlayerDesignStyle.FROSTSOUL
    val isAnimatedGlow = isVinyl && playerBackgroundStyle == PlayerBackgroundStyle.GLOW_ANIMATED
    val isStaticGlow = isVinyl && playerBackgroundStyle == PlayerBackgroundStyle.GLOW
    // Keep the selected artwork present behind every player mode. Vinyl's Gradient/Glow
    // variants tint this same blurred image instead of replacing it with a flat color.
    val shouldBlurArtwork = !artworkUrl.isNullOrBlank()
    val shouldUseGradient = isVinyl && playerBackgroundStyle in setOf(
        PlayerBackgroundStyle.GRADIENT,
        PlayerBackgroundStyle.COLORING,
        PlayerBackgroundStyle.BLUR_GRADIENT,
        PlayerBackgroundStyle.GLOW,
        PlayerBackgroundStyle.GLOW_ANIMATED,
    )
    val moodAccent = remember(moodSeed, palette) { resolveVinylMoodAccent(moodSeed, palette) }
    val animatedGlowPhase =
        if (isAnimatedGlow) {
            rememberInfiniteTransition(label = "vinyl-glow-transition").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(8_000), RepeatMode.Reverse),
                label = "vinyl-glow-phase",
            ).value
        } else {
            0.5f
        }
    val glowPhase = if (isAnimatedGlow) animatedGlowPhase else 0.5f
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (isLightTheme) FrostSoulTheme.colors.background else Color.Black),
    ) {
        if (shouldBlurArtwork && !artworkUrl.isNullOrBlank()) {
            val saturationMatrix = ColorMatrix().apply { setToSaturation(1.0f) }
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(saturationMatrix),
                modifier = Modifier
                    .fillMaxSize()
                    .blur(
                        blurRadius.coerceIn(0f, 120f).dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
            )
        }
        if (shouldUseGradient) {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = listOf(
                            palette.artworkPrimary.copy(alpha = 0.48f),
                            palette.artworkSecondary.copy(alpha = 0.30f),
                            Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
                ),
            )
        }
        if (isAnimatedGlow || isStaticGlow) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(
                        x = ((glowPhase - 0.5f) * 180f).dp,
                        y = ((0.5f - glowPhase) * 120f).dp,
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                moodAccent.copy(alpha = if (isAnimatedGlow) 0.54f else 0.38f),
                                palette.artworkPrimary.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                            radius = 900f,
                        ),
                    ),
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors =                                 listOf(Color.Transparent, Color.Black.copy(alpha = 0.54f)),

                        radius = 1_250f,
                    ),
                ),
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        colors = if (isLightTheme && !isVinyl) {
                            listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.34f))
                        } else {
                            listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent, Color.Black.copy(alpha = 0.30f))
                        },
                    ),
                ),
            )
        }
        // Keep lyric text readable when artwork contains bright whites or skin tones. This is
        // deliberately the final backdrop layer so animated glow cannot wash out lyric text.
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.24f),
                        Color.Black.copy(alpha = 0.14f),
                        Color.Black.copy(alpha = 0.42f),
                    ),
                ),
            ),
        )
    }
}

private fun resolveVinylMoodAccent(seed: String, palette: FrostSoulPalette): Color {
    val mood = seed.lowercase()
    return when {
        listOf("sad", "alone", "cry", "night", "broken", "dard", "udaas").any { keyword -> mood.contains(keyword) } -> Color(0xFF6D8FD6)
        listOf("love", "romance", "heart", "ishq", "pyaar", "romantic").any { keyword -> mood.contains(keyword) } -> Color(0xFFE27B93)
        listOf("party", "dance", "energy", "rock", "remix", "beat").any { keyword -> mood.contains(keyword) } -> Color(0xFFF09A58)
        else -> palette.artworkPrimary
    }
}
