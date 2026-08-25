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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.ui.frostsoul.FSButton
import dev.vxs.frostsoulx.ui.player.rememberDeviceMusicVolumeController
import dev.vxs.frostsoulx.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedHashMap

@Composable
internal fun FrostSoulPlayer(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    modifier: Modifier = Modifier,
) {
    val pages = remember { listOf(FrostSoulPage.Recommendations, FrostSoulPage.MainPlayer, FrostSoulPage.Details, FrostSoulPage.Lyrics) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var queueVisible by remember { mutableStateOf(false) }
    var optionsVisible by remember { mutableStateOf(false) }
    var downwardDragDistance by remember { mutableFloatStateOf(0f) }
    val settledDragOffset by animateFloatAsState(
        targetValue = downwardDragDistance,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "frostsoul-player-dismiss-drag",
    )
    val collapseFraction = (settledDragOffset / 280f).coerceIn(0f, 1f)

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
            palette = uiState.palette,
            pageOffset = pagerState.currentPageOffsetFraction,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = 18.dp),
        ) {
            FSTopBar(
                selectedPage = pagerState.currentPage,
                pageOffsetFraction = pagerState.currentPageOffsetFraction,
                pageCount = pages.size,
                onPageSelected = { targetPage ->
                    scope.launch { pagerState.animateScrollToPage(targetPage) }
                },
                onDismiss = actions.onDismiss,
                onOpenQueue = { queueVisible = true },
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
            )
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
                                val distance = kotlin.math.abs(pageDistance).coerceIn(0f, 1f)
                                alpha = (1f - distance * 0.34f).coerceIn(0.62f, 1f)
                                translationX = -pageDistance * 28f
                                scaleX = 1f - distance * 0.055f
                                scaleY = 1f - distance * 0.055f
                                rotationY = pageDistance * 2.8f
                                cameraDistance = 16f * density
                            },
                ) {
                    when (pages[pageIndex]) {
                        FrostSoulPage.Lyrics ->
                            FSLyrics(
                                rawLyrics = uiState.lyrics,
                                positionMs = uiState.positionMs,
                                durationMs = uiState.safeDurationMs,
                                onSeek = actions.onSeek,
                                onTogglePlayPause = actions.onTogglePlayPause,
                                onToggleLike = actions.onToggleLike,
                                onOpenAudioOutput = actions.onOpenAudioOutput,
                                onOpenOptions = { optionsVisible = true },
                            )

                        FrostSoulPage.MainPlayer ->
                            FrostSoulAlbumPage(
                                uiState = uiState,
                                actions = actions,
                                onOpenLyrics = {
                                    scope.launch { pagerState.animateScrollToPage(pages.indexOf(FrostSoulPage.Lyrics)) }
                                },
                                onOpenQueue = { queueVisible = true },
                                onOpenOptions = { optionsVisible = true },
                            )
                        FrostSoulPage.Recommendations -> FrostSoulRecommendationsPage(uiState = uiState, actions = actions)
                        FrostSoulPage.Details -> FrostSoulSongDetailsPage(uiState = uiState, actions = actions)
                        FrostSoulPage.Queue -> Unit
                    }
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
        AnimatedVisibility(
            visible = optionsVisible,
            enter = fadeIn(tween(160)) + slideInVertically(tween(280)) { it },
            exit = fadeOut(tween(140)) + slideOutVertically(tween(240)) { it },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            FrostSoulPlayerOptionsSheet(
                accent = uiState.palette.accent,
                onDismiss = { optionsVisible = false },
                onOpenAudioOutput = actions.onOpenAudioOutput,
            )
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
    isLiked: Boolean,
    palette: FrostSoulPalette,
    height: androidx.compose.ui.unit.Dp,
    artworkSize: androidx.compose.ui.unit.Dp,
    peeked: Boolean,
    shape: RoundedCornerShape,
    interactionSource: androidx.compose.foundation.interaction.MutableInteractionSource,
    pureBlack: Boolean,
    onCardClick: () -> Unit,
    onLongPress: () -> Unit,
    onTogglePlayPause: () -> Unit,
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
    val backgroundColor = if (pureBlack) Color.Black else FrostSoulSurfaceElevated

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
        if (!track.artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.14f; scaleY = 1.14f }.blur(26.dp).alpha(0.32f),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.76f), backgroundColor.copy(alpha = 0.94f)),
                        ),
                    ),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 9.dp, vertical = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(artworkSize).clip(RoundedCornerShape(16.dp)).background(FrostSoulSurface),
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
                        tint = FrostSoulOnSurfaceMuted,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f).padding(end = 6.dp),
            ) {
                Text(
                    text = track.title,
                    color = FrostSoulOnSurface,
                    fontSize = if (peeked) 15.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = track.artist,
                    color = FrostSoulOnSurfaceMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (peeked && track.album.isNotBlank()) {
                    Text(
                        text = track.album,
                        color = palette.accent.copy(alpha = 0.86f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            FSIconButton(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = onTogglePlayPause,
                active = isPlaying,
                compact = true,
            )
            if (peeked) {
                onQueueClick?.let { openQueue ->
                    FSIconButton(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Open queue",
                        onClick = openQueue,
                        compact = true,
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress)
                    .height(if (peeked) 3.dp else 2.dp)
                    .background(palette.accent),
        )
    }
}

@Composable
internal fun FSPlayerControls(
    state: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    modifier: Modifier = Modifier,
) {
    val remainingMs = (state.safeDurationMs - state.positionMs).coerceAtLeast(0L)
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
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
            Text(state.positionMs.asFrostSoulTime(), color = FrostSoulOnSurfaceMuted, fontSize = 11.sp)
            Text("−${remainingMs.asFrostSoulTime()}", color = FrostSoulOnSurfaceMuted, fontSize = 11.sp)
        }
        state.audioQualityBadge?.let { badge ->
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .padding(top = 7.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, state.palette.accent.copy(alpha = 0.72f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = badge,
                    color = FrostSoulCyanBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FSIconButton(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous track",
                onClick = actions.onSkipPrevious,
                enabled = state.canSkipPrevious,
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
                .size(68.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(FrostSoulCyanBright, accent, Color(0xFF00636E)),
                    ),
                ).clickable(onClick = onClick),
    ) {
        Icon(
            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.Black,
            modifier = Modifier.size(if (isBuffering) 24.dp else 30.dp).alpha(if (isBuffering) 0.54f else 1f),
        )
    }
}

@Composable
internal fun FrostSoulPagerDots(
    pageCount: Int,
    selectedPage: Int,
    selectedPageOffsetFraction: Float = 0f,
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
            val width = 7.dp + (22.dp - 7.dp) * selection
            val alpha = 0.36f + (1f - 0.36f) * selection
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
                        .background(if (selection > 0.5f) FrostSoulCyanBright else FrostSoulOnSurfaceMuted)
                        .clickable { onPageSelected(index) },
            )
        }
    }
}

@Composable
private fun FrostSoulAlbumPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit,
) {
    val volumeController = rememberDeviceMusicVolumeController()
    var lastAudibleVolume by remember { mutableFloatStateOf(0.55f) }
    var upwardDragDistance by remember { mutableFloatStateOf(0f) }
    val volumeFraction = volumeController.volumeFraction.coerceIn(0f, 1f)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            upwardDragDistance += dragAmount
                        },
                        onDragEnd = {
                            if (upwardDragDistance <= -72f) onOpenOptions()
                            upwardDragDistance = 0f
                        },
                        onDragCancel = { upwardDragDistance = 0f },
                    )
                },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            FSGlassCard(
                accent = uiState.palette.accent,
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier.fillMaxWidth(0.90f).aspectRatio(1f),
            ) {
                FSAlbumArt(
                    artworkUrl = uiState.track.artworkUrl,
                    title = uiState.track.title,
                    isPlaying = uiState.isPlaying,
                    palette = uiState.palette,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                Text(
                    text = uiState.track.title,
                    color = FrostSoulOnSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = uiState.track.artist,
                    color = FrostSoulOnSurface,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 7.dp)) {
                    FSChip(label = uiState.audioQualityBadge ?: "STANDARD", selected = false, onClick = {})
                    FSChip(label = "${uiState.queue.size} IN QUEUE", selected = false, onClick = {})
                }
                AnimatedContent(
                    targetState = uiState.currentLyricLine?.takeIf { it.isNotBlank() } ?: "Lyrics unavailable",
                    transitionSpec = {
                        (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 2 }) togetherWith
                            (fadeOut(tween(220)) + slideOutVertically(tween(220)) { -it / 2 })
                    },
                    label = "frostsoul-live-lyric-preview",
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenLyrics)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                ) { lyricLine ->
                    Text(
                        text = lyricLine,
                        color =
                            if (uiState.currentLyricLine.isNullOrBlank()) {
                                FrostSoulOnSurfaceMuted.copy(alpha = 0.60f)
                            } else {
                                FrostSoulOnSurface.copy(alpha = 0.88f)
                            },
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FSPlayerControls(
                state = uiState,
                actions = actions,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            ) {
                FSIconButton(
                    painter = painterResource(R.drawable.volume_off),
                    contentDescription = if (volumeFraction > 0.01f) "Mute" else "Restore volume",
                    onClick = {
                        if (volumeFraction > 0.01f) {
                            lastAudibleVolume = volumeFraction
                            volumeController.setVolumeFraction(0f)
                        } else {
                            volumeController.setVolumeFraction(lastAudibleVolume.coerceAtLeast(0.1f))
                        }
                    },
                    active = volumeFraction <= 0.01f,
                    compact = true,
                )
                FSSeekbar(
                    progress = volumeFraction,
                    durationMs = 1_000L,
                    onSeek = { target -> volumeController.setVolumeFraction(target / 1_000f) },
                    accent = uiState.palette.accent,
                    modifier = Modifier.weight(1f),
                )
                FSIconButton(
                    painter = painterResource(R.drawable.volume_up),
                    contentDescription = "Set full volume",
                    onClick = { volumeController.setVolumeFraction(1f) },
                    active = volumeFraction > 0.01f,
                    compact = true,
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            ) {
                FSIconButton(
                    painter = painterResource(R.drawable.queue_music),
                    contentDescription = "Open playback queue",
                    onClick = onOpenQueue,
                    compact = true,
                )
                FSIconButton(
                    painter = painterResource(R.drawable.lyrics),
                    contentDescription = "Open synchronized lyrics",
                    onClick = onOpenLyrics,
                    active = uiState.currentLyricLine != null,
                    compact = true,
                )
                FSIconButton(
                    painter = painterResource(R.drawable.bluetooth),
                    contentDescription = "Open Bluetooth audio output",
                    onClick = actions.onOpenAudioOutput,
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun FrostSoulPlayerOptionsSheet(
    accent: Color,
    onDismiss: () -> Unit,
    onOpenAudioOutput: () -> Unit,
) {
    val options =
        listOf(
            Triple(R.drawable.style, "Music Therapy", false),
            Triple(R.drawable.lyrics, "View Score", false),
            Triple(R.drawable.tune, "Driving Mode", false),
            Triple(R.drawable.info, "Production Team", false),
            Triple(R.drawable.graphic_eq, "Workout Mode", false),
            Triple(R.drawable.graphic_eq, "Game Mode", false),
            Triple(R.drawable.settings, "Theme Center", false),
            Triple(R.drawable.favorite, "Listen Together", false),
            Triple(R.drawable.share, "Lyrics Poster", false),
            Triple(R.drawable.mic, "Sing This Song", false),
            Triple(R.drawable.tune, "Dislike", false),
            Triple(R.drawable.info, "Report", false),
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
                        color = FrostSoulCyanBright,
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
                                        onOpenAudioOutput()
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp),
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = if (actionable) FrostSoulCyanBright else FrostSoulOnSurface,
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
    val recommendationQueue = uiState.queue.filterNot { it.isCurrent }.take(8)
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 22.dp),
    ) {
        Text(
            text = "RECOMMENDATIONS",
            color = FrostSoulCyanBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
        Text(
            text = "Continue with your listening queue",
            color = FrostSoulOnSurface,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "FrostSoul keeps this page grounded in songs already selected on this device.",
            color = FrostSoulOnSurfaceMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
        FSGlassCard(
            accent = uiState.palette.accent,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            FSQueue(
                title = "",
                queue = recommendationQueue,
                onSelect = { compactIndex ->
                    recommendationQueue.getOrNull(compactIndex)?.let { actions.onSelectQueueItem(it.index) }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun FrostSoulInfoRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label.uppercase(),
            color = FrostSoulOnSurfaceMuted,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = value,
            color = FrostSoulOnSurface,
            fontSize = 16.sp,
            maxLines = 2,
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
                    color = FrostSoulCyanBright,
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
        accent = if (item.isCurrent) FrostSoulCyanBright else FrostSoulCyan,
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
                        .background(if (item.isCurrent) FrostSoulCyan.copy(alpha = 0.26f) else Color.White.copy(alpha = 0.06f)),
            ) {
                Text(
                    text = if (item.isCurrent) "•" else (item.index + 1).toString(),
                    color = if (item.isCurrent) FrostSoulCyanBright else FrostSoulOnSurfaceMuted,
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
                        artworkPrimary = colors.firstOrNull() ?: FrostSoulCyan,
                        artworkSecondary = colors.getOrElse(1) { FrostSoulSurfaceElevated },
                        accent = FrostSoulCyan,
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
    palette: FrostSoulPalette,
    pageOffset: Float,
) {
    AnimatedContent(
        targetState = artworkUrl,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "fs-background-artwork",
        modifier = Modifier.fillMaxSize(),
    ) { art ->
        if (!art.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainterCompat(art),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = 1.18f
                            scaleY = 1.18f
                            translationX = pageOffset * 28f
                        }.blur(52.dp)
                        .alpha(0.10f),
            )
        }
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val primaryGlow =
                        Brush.radialGradient(
                            listOf(palette.artworkPrimary.copy(alpha = 0.36f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.16f, size.height * 0.20f),
                            radius = size.width * 0.92f,
                        )
                    val cyanGlow =
                        Brush.radialGradient(
                            listOf(FrostSoulCyan.copy(alpha = 0.23f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.68f),
                            radius = size.width * 0.80f,
                        )
                    onDrawBehind {
                        drawRect(Color(0xFF1E1E1E))
                        drawRect(primaryGlow)
                        drawRect(cyanGlow)
                        drawRect(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.68f to Color.Transparent,
                                    1f to palette.artworkSecondary.copy(alpha = 0.86f),
                                ),
                            ),
                        )
                    }
                },
    )
}

@Composable
private fun rememberAsyncImagePainterCompat(model: String): Painter {
    val context = LocalContext.current
    val request =
        remember(model, context) {
            ImageRequest
                .Builder(context)
                .data(model)
                .size(Size(768, 768))
                .build()
        }
    return coil3.compose.rememberAsyncImagePainter(model = request)
}


@Composable
private fun FrostSoulSongDetailsPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
) {
    val track = uiState.track
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = track.artworkUrl,
                    contentDescription = "Album artwork for ${track.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(104.dp).clip(RoundedCornerShape(22.dp)),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(track.title, color = FrostSoulOnSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, color = FrostSoulCyanBright, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                    Text(track.album.ifBlank { "Single release" }, color = FrostSoulOnSurfaceMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FSChip(label = uiState.audioQualityBadge ?: "HQ", selected = true, onClick = {})
                FSChip(label = track.durationMs.asFrostSoulTime(), selected = false, onClick = {})
                FSChip(label = "ON DEVICE", selected = false, onClick = {})
            }
        }
        item {
            FSGlassCard(accent = uiState.palette.accent, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TRACK CREDITS", color = FrostSoulCyanBright, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    FrostSoulDetailRow("Artist", track.artist)
                    FrostSoulDetailRow("Album", track.album.ifBlank { "Single release" })
                    FrostSoulDetailRow("Duration", track.durationMs.asFrostSoulTime())
                    FrostSoulDetailRow("Playback", if (uiState.isPlaying) "Now playing" else "Paused")
                }
            }
        }
        val related = uiState.queue.filterNot { it.isCurrent }.take(8)
        if (related.isNotEmpty()) {
            item {
                Text("PEOPLE WHO LIKE THIS ALSO LIKE", color = FrostSoulCyanBright, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(related, key = { "related_${it.id}" }) { item ->
                        FSGlassCard(
                            modifier = Modifier.width(142.dp).height(174.dp),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(10.dp),
                            onClick = { actions.onSelectQueueItem(item.index) },
                        ) {
                            Column {
                                AsyncImage(model = item.artworkUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)))
                                Text(item.title, color = FrostSoulOnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                                Text(item.artist, color = FrostSoulOnSurfaceMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            FSGlassCard(accent = uiState.palette.accent, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FEATURED VIDEOS", color = FrostSoulCyanBright, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("Video highlights will appear here when available for this track.", color = FrostSoulOnSurfaceMuted, fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
        }
    }
}

@Composable
private fun FrostSoulDetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = FrostSoulOnSurfaceMuted, fontSize = 12.sp)
        Text(value, color = FrostSoulOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 16.dp))
    }
}
