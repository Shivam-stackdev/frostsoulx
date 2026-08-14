/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import dev.vxs.frostsoulx.ui.theme.PlayerColorExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FrostSoulPlayer(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
    modifier: Modifier = Modifier,
) {
    val pages = remember { listOf(FrostSoulPage.Album, FrostSoulPage.Lyrics, FrostSoulPage.Info) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val page = pages.getOrElse(pagerState.currentPage) { FrostSoulPage.Album }
    var queueVisible by remember { mutableStateOf(false) }
    var downwardDragDistance by remember { mutableFloatStateOf(0f) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(actions.onDismiss) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (dragAmount > 0f) downwardDragDistance += dragAmount
                        },
                        onDragEnd = {
                            if (downwardDragDistance >= 96f) actions.onDismiss()
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
                currentPage = page,
                onDismiss = actions.onDismiss,
                onOpenQueue = { queueVisible = true },
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
            )
            FrostSoulPagerDots(
                pageCount = pages.size,
                selectedPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp),
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
                                alpha = (1f - kotlin.math.abs(pageDistance) * 0.26f).coerceIn(0.70f, 1f)
                                translationX = -pageDistance * 18f
                            },
                ) {
                    when (pages[pageIndex]) {
                        FrostSoulPage.Album -> FrostSoulAlbumPage(uiState = uiState, actions = actions)
                        FrostSoulPage.Lyrics ->
                            FSLyrics(
                                rawLyrics = uiState.lyrics,
                                positionMs = uiState.positionMs,
                                durationMs = uiState.safeDurationMs,
                                onSeek = actions.onSeek,
                            )

                        FrostSoulPage.Info -> FrostSoulInfoPage(uiState = uiState, actions = actions)
                        FrostSoulPage.Queue -> Unit
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = queueVisible,
            enter = fadeIn() + slideInVertically { height -> height / 3 },
            exit = fadeOut() + slideOutVertically { height -> height / 3 },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            FSGlassCard(
                accent = uiState.palette.accent,
                modifier = Modifier.fillMaxWidth().height(420.dp),
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

@Composable
internal fun FSMiniPlayer(
    track: FrostSoulTrack,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress =
        if (durationMs > 0L) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(68.dp)
                .background(Color.Black.copy(alpha = 0.86f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        ) {
            FSAlbumArt(
                artworkUrl = track.artworkUrl,
                title = track.title,
                isPlaying = isPlaying,
                palette = FrostSoulPalette.Default,
                compact = true,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = FrostSoulOnSurface,
                    fontSize = 14.sp,
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
                )
            }
            FSIconButton(
                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = if (isPlaying) "Pause" else "Play",
                onClick = onTogglePlayPause,
                compact = true,
            )
        }
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(progress)
                    .height(2.dp)
                    .background(FrostSoulCyanBright),
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
            FSIconButton(
                painter = painterResource(if (state.track.isLiked) R.drawable.favorite else R.drawable.favorite_border),
                contentDescription = if (state.track.isLiked) "Unlike track" else "Like track",
                onClick = actions.onToggleLike,
                active = state.track.isLiked,
                compact = true,
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
private fun FrostSoulPagerDots(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier =
                    Modifier
                        .height(3.dp)
                        .width(if (index == selectedPage) 22.dp else 7.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(
                            if (index == selectedPage) FrostSoulCyanBright else FrostSoulOnSurfaceMuted.copy(alpha = 0.36f),
                        ),
            )
        }
    }
}

@Composable
private fun FrostSoulAlbumPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize().padding(bottom = 14.dp),
    ) {
        Spacer(Modifier.height(6.dp))
        FSAlbumArt(
            artworkUrl = uiState.track.artworkUrl,
            title = uiState.track.title,
            isPlaying = uiState.isPlaying,
            palette = uiState.palette,
            modifier = Modifier.fillMaxWidth(0.84f),
        )
        FSGlassCard(
            accent = uiState.palette.accent,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 68.dp, top = 16.dp, bottom = 16.dp),
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
                    color = FrostSoulCyanBright,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
                if (uiState.track.album.isNotBlank()) {
                    Text(
                        text = uiState.track.album,
                        color = FrostSoulOnSurfaceMuted,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            FSIconButton(
                painter = painterResource(if (uiState.track.isLiked) R.drawable.favorite else R.drawable.favorite_border),
                contentDescription = if (uiState.track.isLiked) "Unlike track" else "Like track",
                onClick = actions.onToggleLike,
                active = uiState.track.isLiked,
                compact = true,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
            )
        }
        FSPlayerControls(
            state = uiState,
            actions = actions,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun FrostSoulInfoPage(
    uiState: FrostSoulPlayerUiState,
    actions: FrostSoulPlayerActions,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 22.dp),
    ) {
        Text(
            text = "TRACK INFORMATION",
            color = FrostSoulCyanBright,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        )
        FSGlassCard(
            accent = uiState.palette.accent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.padding(20.dp),
            ) {
                FrostSoulInfoRow(label = "Title", value = uiState.track.title)
                FrostSoulInfoRow(label = "Artist", value = uiState.track.artist)
                FrostSoulInfoRow(label = "Album", value = uiState.track.album.ifBlank { "Single" })
                FrostSoulInfoRow(label = "Duration", value = uiState.safeDurationMs.asFrostSoulTime())
                FrostSoulInfoRow(label = "Library", value = if (uiState.track.isLiked) "Liked" else "Available offline" )
            }
        }
        FSButton(
            label = if (uiState.track.isLiked) "Remove from liked songs" else "Add to liked songs",
            onClick = actions.onToggleLike,
            emphasized = uiState.track.isLiked.not(),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Swipe left or right to return to artwork and synchronized lyrics.",
            color = FrostSoulOnSurfaceMuted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
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
    val paletteCache = remember { mutableMapOf<String, FrostSoulPalette>() }
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
                        .alpha(0.20f),
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
                        drawRect(Color.Black)
                        drawRect(primaryGlow)
                        drawRect(cyanGlow)
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))))
                    }
                },
    )
}

@Composable
private fun rememberAsyncImagePainterCompat(model: String) = coil3.compose.rememberAsyncImagePainter(model = model)
