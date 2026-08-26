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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.runtime.remember
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.ui.frostsoul.FSButton
import dev.vxs.frostsoulx.ui.frostsoul.MinimalistMetadataChip
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme
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
    // QQ-style pager: Recommendations stay on the left, Main Player in the center, Lyrics on the right.
    val pages = remember { listOf(FrostSoulPage.Recommendations, FrostSoulPage.MainPlayer, FrostSoulPage.Lyrics) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var queueVisible by remember { mutableStateOf(false) }
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
            FrostSoulDynamicBackground(artworkUrl = uiState.track.artworkUrl)
            Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = PlayerLayoutTokens.MasterHorizontalPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(FrostSoulTheme.colors.onSurfaceMuted.copy(alpha = 0.42f)),
                )
            }
            FSTopBar(
                selectedPage = pagerState.currentPage,
                pageOffsetFraction = pagerState.currentPageOffsetFraction,
                pageCount = pages.size,
                onPageSelected = { targetPage ->
                    scope.launch { pagerState.animateScrollToPage(targetPage) }
                },
                onDismiss = actions.onDismiss,
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
                                onRefetchLyrics = actions.onRefetchLyrics,
                                isRefetchingLyrics = actions.isRefetchingLyrics,
                            )

                        FrostSoulPage.MainPlayer ->
                            FrostSoulAlbumPage(
                                uiState = uiState,
                                actions = actions,
                                onOpenQueue = { queueVisible = true },
                                onOpenOptions = actions.onOpenOptions,
                            )
                        FrostSoulPage.Recommendations -> FrostSoulRecommendationsPage(uiState = uiState, actions = actions)
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
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
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
                }.clip(androidx.compose.foundation.shape.CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFE2E2E2), Color(0xFF777777)),
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
                        .background(if (selection > 0.5f) Color.White else Color.White.copy(alpha = 0.34f))
                        .clickable { onPageSelected(index) },
            )
        }
    }
}

@Composable
private fun FrostSoulAlbumPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            FSAlbumArt(
                artworkUrl = uiState.track.artworkUrl,
                title = uiState.track.title,
                isPlaying = uiState.isPlaying,
                modifier = Modifier.size(PlayerLayoutTokens.VinylDiscSize),
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            ) {
                    Text(
                        text = uiState.track.title,
                        style = PlayerLayoutTokens.TrackTitleStyle.copy(color = FrostSoulOnSurface),
                        maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                    Text(
                        text = uiState.track.artist,
                        style = PlayerLayoutTokens.ArtistSubtitleStyle.copy(color = FrostSoulOnSurfaceMuted),
                        maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
                    MinimalistMetadataChip(text = uiState.audioQualityBadge ?: "STANDARD")
                    MinimalistMetadataChip(text = "${uiState.queue.size} IN QUEUE")
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                FSIconButton(
                    painter = painterResource(if (uiState.track.isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = if (uiState.track.isLiked) "Remove like" else "Like track",
                    onClick = actions.onToggleLike,
                    active = uiState.track.isLiked,
                    compact = true,
                )
                FrostSoulOutputDeviceButton(
                    device = uiState.outputDevice,
                    onClick = actions.onOpenAudioOutput,
                )
                FSIconButton(
                    painter = painterResource(R.drawable.more_vert),
                    contentDescription = "Open player options",
                    onClick = onOpenOptions,
                    compact = true,
                )
            }
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
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    val primaryText = if (isLightTheme) FrostSoulTheme.colors.onSurface else FrostSoulOnSurface
    val mutedText = if (isLightTheme) FrostSoulTheme.colors.onSurfaceMuted else FrostSoulOnSurfaceMuted
    val chipText = if (isLightTheme) Color.Black else Color.White
    val chipSurface = if (isLightTheme) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.08f)
    val chipOutline = if (isLightTheme) Color.Black.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.18f)
    val cardSurface = if (isLightTheme) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.07f)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().padding(vertical = 22.dp),
    ) {
        Text(
            text = "RECOMMENDATIONS",
            color = primaryText.copy(alpha = 0.78f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
        Text(
            text = "Continue with your listening queue",
            color = primaryText,
            fontSize = 23.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "Songs already selected on this device, ready to play next.",
            color = mutedText,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
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
        if (recommendationQueue.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                Text(
                    text = "No more songs in this queue.",
                    color = mutedText,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                gridItems(recommendationQueue, key = { "recommendation_${it.index}_${it.id}" }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardSurface)
                            .clickable { actions.onSelectQueueItem(item.index) }
                            .padding(bottom = 8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp)),
                        ) {
                            AsyncImage(
                                model = item.artworkUrl,
                                contentDescription = "Artwork for ${item.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (item.durationMs > 0L) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(5.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(Color.Black.copy(alpha = 0.72f))
                                        .padding(horizontal = 5.dp, vertical = 3.dp),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.play),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Text(
                                        text = item.durationMs.asFrostSoulTime(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(start = 3.dp),
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.title,
                            color = primaryText,
                            fontSize = 12.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                        Text(
                            text = item.artist,
                            color = mutedText,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                }
            }
        }
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
private fun FrostSoulDynamicBackground(artworkUrl: String?) {
    val isLightTheme = FrostSoulTheme.colors.background.luminance() > 0.5f
    val saturationMatrix = ColorMatrix().apply { setToSaturation(0.1f) }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (isLightTheme) FrostSoulTheme.colors.background else Color.Black),
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(saturationMatrix),
                modifier = Modifier.fillMaxSize().blur(90.dp),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        if (isLightTheme) {
                            Color.White.copy(alpha = 0.70f)
                        } else {
                            Color.Black.copy(alpha = 0.68f)
                        },
                    ),
        )
    }
}
