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
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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
    // While the seekbar is being dragged, the pager's own horizontal-swipe gesture must not
    // compete with it — otherwise a horizontal drag on the seekbar can get interpreted as a
    // page-change swipe instead of a seek. Disabling userScrollEnabled for the duration of the
    // drag is the reliable fix (plain pointerInput consumption on the seekbar alone doesn't
    // reliably win against the pager's own scrollable gesture detection).
    var isSeekbarDragging by remember { mutableStateOf(false) }
    var downwardDragDistance by remember { mutableFloatStateOf(0f) }
    val settledDragOffset by animateFloatAsState(
        targetValue = downwardDragDistance,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "frostsoul-player-dismiss-drag",
    )
    val collapseFraction = (settledDragOffset / 280f).coerceIn(0f, 1f)
    // On the ARTWORK_BLUR ("Immersive") style, the main player page wants its artwork to
    // reach the true top of the screen (behind the already-hidden status bar), with the
    // collapse chevron + pager dots floating over the artwork instead of sitting in their
    // own reserved row above it. Other pages/styles keep the reserved row untouched.
    val isImmersiveArtworkMainPage =
        playerDesignStyle == dev.vxs.frostsoulx.constants.PlayerDesignStyle.ARTWORK_BLUR &&
            pages.getOrNull(pagerState.currentPage) == FrostSoulPage.MainPlayer
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
            // Status bar is fully hidden (immersive) for this style while the player is
            // expanded — see MainActivity.shouldHideStatusBars, which now also covers
            // FROSTSOUL/ARTWORK_BLUR alongside V7. With the bar actually hidden (not just
            // drawn behind), WindowInsets.systemBars collapses to ~0 here, so this Column and
            // the artwork header below it already reach the true top edge of the screen with
            // no extra offset/overlay tricks needed.
            //
            // On the Immersive main player page this row drops to 0dp height so the pager
            // below reclaims the space (letting the artwork header start at the true y=0),
            // while zIndex keeps the chevron/dots painted above the artwork instead of
            // being drawn underneath it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isImmersiveArtworkMainPage) 0.dp else 42.dp)
                    .zIndex(if (isImmersiveArtworkMainPage) 12f else 0f)
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
                userScrollEnabled = !isSeekbarDragging,
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
                                    onSeekDraggingChanged = { isSeekbarDragging = it },
                                )
                            } else {
                                FrostSoulAlbumPage(
                                    uiState = uiState,
                                    actions = actions,
                                    onOpenQueue = { queueVisible = true },
                                    onOpenOptions = actions.onOpenOptions,
                                    onSearchTrack = onSearchTrack,
                                    onShowArtists = { showArtistDialog = true },
                                    onSeekDraggingChanged = { isSeekbarDragging = it },
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(artworkSize + 10.dp),
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val strokeWidth = 2.5.dp.toPx()
                    val inset = strokeWidth / 2f
                    val left = inset
                    val top = inset
                    val right = size.width - inset
                    val bottom = size.height - inset
                    val cornerRadius = 10.dp.toPx().coerceAtMost((minOf(size.width, size.height) / 2f) - inset)
                    val topMidX = (left + right) / 2f
                    val timelineColor = if (isLightTheme) Color.Black else Color.White
                    // Built by hand (instead of Path.addRoundRect, whose start point sits near a
                    // corner and which Compose defaults to counter-clockwise) so distance=0 on
                    // this path is exactly the middle of the top edge and the path winds
                    // clockwise from there — matching the requested start point/direction for
                    // the progress sweep below.
                    val perimeterPath = Path().apply {
                        moveTo(topMidX, top)
                        lineTo(right - cornerRadius, top)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(right - 2 * cornerRadius, top, right, top + 2 * cornerRadius),
                            startAngleDegrees = -90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(right, bottom - cornerRadius)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(right - 2 * cornerRadius, bottom - 2 * cornerRadius, right, bottom),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(left + cornerRadius, bottom)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(left, bottom - 2 * cornerRadius, left + 2 * cornerRadius, bottom),
                            startAngleDegrees = 90f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(left, top + cornerRadius)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(left, top, left + 2 * cornerRadius, top + 2 * cornerRadius),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 90f,
                            forceMoveTo = false,
                        )
                        lineTo(topMidX, top)
                        close()
                    }
                    val perimeterMeasure = PathMeasure()
                    perimeterMeasure.setPath(perimeterPath, forceClosed = true)
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    )
                    drawPath(
                        path = perimeterPath,
                        color = timelineColor.copy(alpha = 0.22f),
                        style = stroke,
                    )
                    if (progress > 0f) {
                        val progressPath = Path()
                        perimeterMeasure.getSegment(
                            startDistance = 0f,
                            stopDistance = perimeterMeasure.length * progress,
                            destination = progressPath,
                            startWithMoveTo = true,
                        )
                        drawPath(
                            path = progressPath,
                            color = timelineColor,
                            style = stroke,
                        )
                    }
                }
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
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.weight(1f).basicMarquee(iterations = Int.MAX_VALUE),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(42.dp).zIndex(1f).clickable(onClick = onToggleLike),
            ) {
                Icon(
                    painter = painterResource(if (track.isLiked) R.drawable.favorite else R.drawable.favorite_border),
                    contentDescription = if (track.isLiked) "Remove from favorites" else "Add to favorites",
                    tint = if (track.isLiked) Color(0xFFFF3B4D) else primaryTextColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            FSIconButton(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = onTogglePlayPause,
                active = false,
                buttonSize = 42.dp,
                iconSize = 24.dp,
                showContainer = false,
                dimBackdrop = false,
                tintOverride = if (isLightTheme) Color.Black else Color.White,
                modifier = Modifier.zIndex(1f),
            )
            onQueueClick?.let { openQueue ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .size(48.dp)
                            .zIndex(2f)
                            .clickable(onClick = openQueue),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = "Open queue",
                        tint = if (isLightTheme) Color.Black else Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun FSPlayerControls(
    state: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    immersive: Boolean = false,
    onSeekDraggingChanged: (Boolean) -> Unit = {},
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
                immersive = immersive,
                immersiveColor = state.palette.artworkPrimary.copy(alpha = 0.56f),
            )
            FSTwoDotButton(onClick = actions.onOpenOptions, immersive = immersive)
        }
        FSSeekbar(
            progress = state.progress,
            durationMs = state.safeDurationMs,
            onSeek = actions.onSeek,
            accent = state.palette.accent,
            onDraggingChanged = onSeekDraggingChanged,
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
            // Repeat and queue are toggle-style controls, so they keep a soft container to make
            // their active/inactive state readable at a glance (also bumped up in size for a
            // sturdier touch target, matching the reference design).
            FSIconButton(
                painter = painterResource(
                    if (state.repeatMode == androidx.media3.common.Player.REPEAT_MODE_ONE) {
                        R.drawable.repeat_one
                    } else {
                        R.drawable.repeat
                    },
                ),
                contentDescription = "Toggle repeat mode",
                onClick = actions.onToggleRepeat,
                active = state.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF,
                buttonSize = 36.dp,
                iconSize = 19.dp,
                showContainer = false,
                forceWhite = true,
            )
            FSIconButton(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = "Previous track",
                onClick = actions.onSkipPrevious,
                enabled = state.canSkipPrevious,
                buttonSize = 44.dp,
                iconSize = 34.dp,
                showContainer = false,
                forceWhite = true,
            )
            FSPlayButton(
                isPlaying = state.isPlaying,
                isBuffering = state.isBuffering,
                onClick = actions.onTogglePlayPause,
            )
            FSIconButton(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = "Next track",
                onClick = actions.onSkipNext,
                enabled = state.canSkipNext,
                buttonSize = 44.dp,
                iconSize = 34.dp,
                showContainer = false,
                forceWhite = true,
            )
            FSIconButton(
                painter = painterResource(R.drawable.queue_music),
                contentDescription = "Open playback queue",
                onClick = onOpenQueue,
                buttonSize = 36.dp,
                iconSize = 19.dp,
                showContainer = false,
                forceWhite = true,
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
private fun FSTwoDotButton(
    onClick: () -> Unit,
    immersive: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(if (immersive) 40.dp else 36.dp).clickable(onClick = onClick),
    ) {
        repeat(2) {
            Box(
                modifier = Modifier
                    .size(if (immersive) 7.dp else 5.dp)
                    .background(
                        if (immersive) Color.White else FrostSoulTheme.colors.onSurface,
                        androidx.compose.foundation.shape.CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun FSPlayButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
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
            modifier = Modifier.size(if (isBuffering) 36.dp else 44.dp).alpha(if (isBuffering) 0.54f else 1f),
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
                                Icon(
                                    painter = painterResource(R.drawable.artist),
                                    contentDescription = null,
                                    tint = FrostSoulOnSurface.copy(alpha = 0.72f),
                                    modifier = Modifier.size(24.dp),
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
    // The artwork-blur player wants exactly the current 2-line block, bigger and more
    // prominent, with no extra trailing preview lines below it (reference: a clean 2-line
    // block only). The vinyl page still uses onlyCurrentLine = true (single line) and is
    // unaffected by this flag.
    showExtraPreviewLines: Boolean = !onlyCurrentLine,
    maxLinesPerLyric: Int = 2,
    horizontalPadding: Dp = PlayerLayoutTokens.MasterHorizontalPadding,
    modifier: Modifier = Modifier,
) {
    val currentLine = uiState.currentLyricModel
    if (currentLine == null && uiState.lyricPreviewLines.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
    ) {
        currentLine?.let { line ->
            Text(
                text = line.asMainPlayerKaraokeText(
                    currentWordIndex = uiState.currentWordIndex,
                    wordProgress = uiState.currentWordProgress,
                    lineProgress = uiState.currentLineProgress,
                ),
                color = FrostSoulOnSurface.copy(alpha = 0.96f),
                fontSize = if (onlyCurrentLine) 17.sp else 21.sp,
                lineHeight = if (onlyCurrentLine) 23.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (onlyCurrentLine) 1 else maxLinesPerLyric,
                overflow = TextOverflow.Ellipsis,
            )
        } ?: uiState.currentLyricLine?.takeIf { it.isNotBlank() }?.let { line ->
            Text(
                text = line,
                color = FrostSoulOnSurface.copy(alpha = 0.96f),
                fontSize = if (onlyCurrentLine) 17.sp else 21.sp,
                lineHeight = if (onlyCurrentLine) 23.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = if (onlyCurrentLine) 1 else maxLinesPerLyric,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showExtraPreviewLines) {
            uiState.lyricPreviewLines.drop(1).take(1).forEach { line ->
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
    onSeekDraggingChanged: (Boolean) -> Unit = {},
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
                FrostSoulFullPlayerLikeButton(
                    videoId = uiState.track.id,
                    isLiked = uiState.track.isLiked,
                    onClick = actions.onToggleLike,
                )
            }
            Column(
                horizontalAlignment = Alignment.Start,
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
                horizontalPadding = 0.dp,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 28.dp),
            )
            FSPlayerControls(
                    state = uiState,
                    actions = actions,
                    onOpenQueue = onOpenQueue,
                    modifier = Modifier.padding(top = 2.dp),
                    immersive = true,
                    onSeekDraggingChanged = onSeekDraggingChanged,
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
    onSeekDraggingChanged: (Boolean) -> Unit = {},
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
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Full-bleed artwork header: the image spans the whole width with no card
       // inset, and fades edge-to-edge into the page background so the thumbnail
            // reads as one seamless surface (QQ Music "immersive cover" behaviour).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlayerLayoutTokens.ArtworkBlurHeaderHeight)
                    .clipToBounds(),
            ) {
                if (!uiState.track.artworkUrl.isNullOrBlank()) {
                    // The blurred artwork is already rendered full-screen underneath this header.
                    // Mask the sharp cover at its lower edge instead of painting a black fade over
                    // it; this lets the two layers actually dissolve into one another like the
                    // original ArchiveTune Immersive Extended player.
                    AsyncImage(
                        model = uiState.track.artworkUrl,
                        contentDescription = "Album artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            // FS-BUG-IMMERSIVE-BORDER: BlendMode.DstIn only combines correctly
                            // with what's *already inside this composable's own layer*. Without
                            // an explicit offscreen layer here, this image shares the pager
                            // page's layer, and DstIn ends up cutting into whatever else is
                            // already drawn there instead of just fading this image's own alpha
                            // to transparent — which is exactly why the header shows a hard
                            // rectangular edge (the "border line square") while settled on the
                            // current page. It only looked fixed mid-drag because the pager's
                            // own alpha-fade on adjacent pages happened to force an offscreen
                            // layer at that moment. Forcing it here directly makes the fade
                            // isolated and consistent regardless of pager/drag state.
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.00f to Color.White,
                                        0.48f to Color.White,
                                        0.68f to Color.White.copy(alpha = 0.96f),
                                        0.82f to Color.White.copy(alpha = 0.72f),
                                        0.93f to Color.White.copy(alpha = 0.28f),
                                        1.00f to Color.Transparent,
                                    ),
                                    blendMode = BlendMode.DstIn,
                                )
                            },
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(
                            Brush.verticalGradient(
                                colors = listOf(uiState.palette.artworkPrimary, uiState.palette.artworkSecondary),
                            ),
                        ),
                    )
                }
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
                FrostSoulFullPlayerLikeButton(
                    videoId = uiState.track.id,
                    isLiked = uiState.track.isLiked,
                    onClick = actions.onToggleLike,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            FrostSoulMainLyricPreview(
                uiState = uiState,
                showExtraPreviewLines = true,
                maxLinesPerLyric = 1,
                modifier = Modifier.heightIn(min = 60.dp),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        FSPlayerControls(
            state = uiState,
            actions = actions,
            onOpenQueue = onOpenQueue,
            modifier = Modifier
                .padding(horizontal = PlayerLayoutTokens.MasterHorizontalPadding)
                .padding(top = 18.dp),
            immersive = true,
            onSeekDraggingChanged = onSeekDraggingChanged,
        )
    }
}

private fun formatLikeCount(count: Int): String = when {
    count >= 1_000_000 -> "${(count / 1_000_000f).toString().trimEnd('0').trimEnd('.')}M"
    count >= 1_000 -> "${(count / 1_000f).toString().trimEnd('0').trimEnd('.')}K"
    else -> count.toString()
}

@Composable
private fun FrostSoulFullPlayerLikeButton(
    videoId: String,
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var likeCount by remember(videoId) { mutableStateOf<Int?>(null) }
    LaunchedEffect(videoId) {
        if (videoId.isNotBlank()) likeCount = YouTube.getMediaInfo(videoId).getOrNull()?.like
    }
    val tint = if (isLiked) Color(0xFFFF3B4D) else {
        if (FrostSoulTheme.colors.background.luminance() > 0.5f) Color.Black else Color.White
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(42.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Icon(
            painter = painterResource(if (isLiked) R.drawable.favorite else R.drawable.favorite_border),
            contentDescription = if (isLiked) "Unlike track" else "Like track",
            tint = tint,
            modifier = Modifier.size(25.dp),
        )
        Text(
            text = formatLikeCount(likeCount ?: 0),
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp).widthIn(min = 24.dp),
        )
    }
}

@Composable
private fun FrostSoulOutputDeviceButton(
    device: dev.vxs.frostsoulx.models.ActiveOutputDevice,
    onClick: () -> Unit,
    immersive: Boolean = false,
    immersiveColor: Color = FrostSoulTheme.colors.surfaceGlass,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                if (immersive) immersiveColor else FrostSoulTheme.colors.surface.copy(alpha = 0.58f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        androidx.compose.material3.Icon(
            imageVector = device.type.imageVector,
            contentDescription = "Audio output device",
            tint = FrostSoulTheme.colors.onSurface,
            modifier = Modifier.size(if (immersive) 22.dp else 20.dp),
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
    // This page renders directly over FrostSoulDynamicBackground's ambient blurred artwork,
    // which stays dark in both app themes — so text/chip colors stay white-based regardless of
    // the app's light/dark theme setting (fixes FS-BUG-LIGHTMODE: text was flipping to
    // near-black here and disappearing against the still-dark backdrop in light theme).
    val primaryText = FrostSoulOnSurface
    val mutedText = FrostSoulOnSurfaceMuted
    val chipText = Color.White
    val chipSurface = Color.White.copy(alpha = 0.08f)
    val chipOutline = Color.White.copy(alpha = 0.18f)
    val cardSurface = Color.White.copy(alpha = 0.07f)

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

/**
 * Fraction of the screen height the ambient glow is allowed to occupy, anchored to the bottom.
 *
 * The glow used to be a full-screen radial gradient that drifted across the entire backdrop,
 * which lit up the vinyl deck, washed out the artwork and — because every frame re-blended a
 * screen-sized translucent layer — was the single most expensive thing on the GPU. It is now
 * confined to the bottom band around the seek bar and transport controls, fading out just above
 * them, so the turntable area above stays a dark, moody canvas.
 */
private const val GlowHeightFraction = 0.46f

/** Slow "breathing" cycle for the glow, in milliseconds. Ambient, not attention-grabbing. */
private const val GlowBreathDurationMs = 7_000

/**
 * Pixel size the ambient backdrop artwork is decoded at. The image is blurred into a soft wash,
 * so full-resolution detail is thrown away anyway — decoding a small bitmap and letting it scale
 * up costs a fraction of the memory and bandwidth, and lets the blur radius drop sharply.
 */
private const val AmbientArtworkSampleSize = 192

/**
 * Background styles that paint a palette-tinted gradient over the blurred artwork.
 * Hoisted to file scope so the set is allocated once rather than on every recomposition.
 */
private val GradientBackgroundStyles: Set<PlayerBackgroundStyle> =
    java.util.EnumSet.of(
        PlayerBackgroundStyle.GRADIENT,
        PlayerBackgroundStyle.COLORING,
        PlayerBackgroundStyle.BLUR_GRADIENT,
        PlayerBackgroundStyle.GLOW,
        PlayerBackgroundStyle.GLOW_ANIMATED,
    )

/**
 * Single consolidated scrim for the glow styles.
 *
 * This one brush replaces what used to be three stacked full-screen layers (a radial vignette, a
 * neutral ambient tone and a final readability scrim). Keeping the top ~60% deliberately dark is
 * what makes the vinyl deck read as "slightly dark" like the reference player, while the lower
 * stops stay lighter so the glow underneath can show through around the controls.
 */
private val GlowModeScrim =
    Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.62f),
            Color.Black.copy(alpha = 0.55f),
            Color.Black.copy(alpha = 0.40f),
            Color.Black.copy(alpha = 0.30f),
            Color.Black.copy(alpha = 0.46f),
        ),
    )

/** Equivalent consolidated scrim for the non-glow styles. */
private val PlainModeScrim =
    Brush.verticalGradient(
        colors = listOf(
            Color.Black.copy(alpha = 0.34f),
            Color.Black.copy(alpha = 0.16f),
            Color.Black.copy(alpha = 0.44f),
        ),
    )

@Composable
private fun FrostSoulDynamicBackground(
    artworkUrl: String?,
    playerDesignStyle: PlayerDesignStyle,
    playerBackgroundStyle: PlayerBackgroundStyle,
    blurRadius: Float,
    palette: FrostSoulPalette,
    moodSeed: String,
) {
    val isVinyl = playerDesignStyle == PlayerDesignStyle.FROSTSOUL
    val isAnimatedGlow = isVinyl && playerBackgroundStyle == PlayerBackgroundStyle.GLOW_ANIMATED
    val isStaticGlow = isVinyl && playerBackgroundStyle == PlayerBackgroundStyle.GLOW
    val isGlowMode = isAnimatedGlow || isStaticGlow
    // Keep the selected artwork present behind every player mode. Vinyl's Gradient/Glow
    // variants tint this same blurred image instead of replacing it with a flat color.
    val shouldBlurArtwork = !artworkUrl.isNullOrBlank()
    val shouldUseGradient = isVinyl && playerBackgroundStyle in GradientBackgroundStyles
    val moodAccent = remember(moodSeed, palette) { resolveVinylMoodAccent(moodSeed, palette) }

    // The breathing phase is kept as a State and only read inside graphicsLayer, i.e. during the
    // draw phase. Previously `.value` was read straight into composition, so the infinite glow
    // animation recomposed this whole background — including the full-screen blurred AsyncImage —
    // on every single frame. That recomposition storm was the main source of the stutter.
    val glowBreath: State<Float>? =
        if (isAnimatedGlow) {
            rememberInfiniteTransition(label = "vinyl-glow-transition").animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(GlowBreathDurationMs), RepeatMode.Reverse),
                label = "vinyl-glow-phase",
            )
        } else {
            null
        }

    // Because the ambient artwork is decoded small and scaled up, it is already very soft — so a
    // far smaller blur radius reproduces the old look. Blur cost scales with radius, and the old
    // 36..120dp range over a full-screen layer was extremely expensive on mid-range GPUs.
    val ambientBlurRadius = (blurRadius * 0.34f).coerceIn(10f, 26f)

    val context = LocalContext.current
    val ambientArtworkRequest = remember(artworkUrl, context) {
        artworkUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .size(Size(AmbientArtworkSampleSize, AmbientArtworkSampleSize))
                .build()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black),
    ) {
        if (shouldBlurArtwork && ambientArtworkRequest != null) {
            // The previous implementation also applied ColorFilter.colorMatrix with
            // setToSaturation(1.0f) — an identity matrix. It changed nothing visually while
            // forcing an extra full-screen color-filter pass every frame, so it is gone.
            AsyncImage(
                model = ambientArtworkRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
                    .blur(
                        ambientBlurRadius.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded,
                    ),
            )
        }

        if (shouldUseGradient) {
            // Brush depends only on the palette, so it is cached instead of being rebuilt (with
            // its Color list) on every frame.
            val gradientBrush = remember(palette) {
                Brush.verticalGradient(
                    colors = listOf(
                        palette.artworkPrimary.copy(alpha = 0.42f),
                        palette.artworkSecondary.copy(alpha = 0.26f),
                        Color.Black.copy(alpha = 0.92f),
                    ),
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(gradientBrush))
        }

        // One consolidated scrim instead of the old three stacked full-screen layers. This is
        // what keeps the turntable area above the glow "halka dark" like the reference player.
        Box(modifier = Modifier.fillMaxSize().background(if (isGlowMode) GlowModeScrim else PlainModeScrim))

        if (isGlowMode) {
            // Bottom-anchored glow band. Only ~46% of the screen is blended here rather than the
            // whole canvas, and the layer is bottom-aligned so it sits around the seek bar and
            // transport controls, easing out to fully transparent just above them.
            val glowBrush = remember(moodAccent, palette, isAnimatedGlow) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        moodAccent.copy(alpha = if (isAnimatedGlow) 0.14f else 0.11f),
                        moodAccent.copy(alpha = if (isAnimatedGlow) 0.30f else 0.24f),
                        palette.artworkPrimary.copy(alpha = if (isAnimatedGlow) 0.34f else 0.27f),
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(GlowHeightFraction)
                    // Animation is consumed in the draw phase only: alpha "breathes" and the
                    // focal point drifts laterally via translation. Using graphicsLayer instead
                    // of Modifier.offset also means no layout pass is triggered per frame.
                    .graphicsLayer {
                        val breath = glowBreath?.value ?: 0.5f
                        alpha = 0.72f + breath * 0.28f
                        translationX = (breath - 0.5f) * 46f * density
                    }
                    .background(glowBrush),
            )
        }
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
