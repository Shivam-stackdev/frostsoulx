/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.vxs.frostsoulx.LocalPlayerAwareWindowInsets
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.constants.MiniPlayerHeight
import dev.vxs.frostsoulx.db.entities.Album
import dev.vxs.frostsoulx.db.entities.Artist
import dev.vxs.frostsoulx.db.entities.LocalItem
import dev.vxs.frostsoulx.db.entities.Playlist
import dev.vxs.frostsoulx.db.entities.Song
import dev.vxs.frostsoulx.extensions.toMediaItem
import dev.vxs.frostsoulx.extensions.togglePlayPause
import dev.vxs.frostsoulx.home.HomeAction
import dev.vxs.frostsoulx.innertube.pages.HomePage
import dev.vxs.frostsoulx.home.HomeUiState
import dev.vxs.frostsoulx.models.MediaMetadata
import dev.vxs.frostsoulx.library.LibraryTopMix
import dev.vxs.frostsoulx.playback.PlayerConnection
import dev.vxs.frostsoulx.playback.queues.ListQueue
import dev.vxs.frostsoulx.ui.component.MenuState
import dev.vxs.frostsoulx.ui.frostsoul.FSAlbumCard
import dev.vxs.frostsoulx.ui.frostsoul.FSIcon
import dev.vxs.frostsoulx.ui.frostsoul.FSText
import dev.vxs.frostsoulx.ui.frostsoul.FSText as Text
import dev.vxs.frostsoulx.ui.frostsoul.FSArtistCard
import dev.vxs.frostsoulx.ui.frostsoul.FSButton
import dev.vxs.frostsoulx.ui.frostsoul.FSChip
import dev.vxs.frostsoulx.ui.frostsoul.FSEmptyState
import dev.vxs.frostsoulx.ui.frostsoul.FSGlassCard
import dev.vxs.frostsoulx.ui.frostsoul.FSIconButton
import dev.vxs.frostsoulx.ui.frostsoul.FSTextField
import dev.vxs.frostsoulx.ui.frostsoul.FSListItem
import dev.vxs.frostsoulx.ui.frostsoul.FSLoading
import dev.vxs.frostsoulx.ui.frostsoul.FSSectionHeader
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme
import dev.vxs.frostsoulx.ui.frostsoul.frostSoulScreenBackground
import dev.vxs.frostsoulx.ui.player.frostsoul.asFrostSoulTime
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import java.util.Calendar

private val FrostSoulShelfItemPadding = PaddingValues(horizontal = 20.dp)
private val FrostSoulShelfSpacing = 14.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FrostSoulHomeFeed(
    uiState: HomeUiState,
    mediaMetadata: MediaMetadata?,
    isPlaying: Boolean,
    navController: NavController,
    playerConnection: PlayerConnection,
    menuState: MenuState,
    haptic: HapticFeedback,
    scope: CoroutineScope,
    lazyListState: LazyListState,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums = remember(uiState.speedDialItems) { uiState.speedDialItems.filterIsInstance<Album>() }
    val artists = remember(uiState.speedDialItems) { uiState.speedDialItems.filterIsInstance<Artist>() }
    val recentItems = remember(uiState.keepListening) { uiState.keepListening.take(6) }
    val pageSections = uiState.homePage?.sections.orEmpty()

    LazyColumn(
        state = lazyListState,
        contentPadding =
            PaddingValues(
                top = 8.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + MiniPlayerHeight + 108.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier.fillMaxSize().frostSoulScreenBackground(),
    ) {
        item(key = "frostsoul_home_header") {
            FrostSoulHomeHeader(
                accountName = uiState.accountName,
                onOpenNotifications = { navController.navigate("news") },
            )
        }

        item(key = "frostsoul_quick_search") {
            FrostSoulQuickSearch(onOpenSearch = { navController.navigate(Screens.Search.route) })
        }

        uiState.homePage?.chips.orEmpty().takeIf { it.isNotEmpty() }?.let { sourceChips ->
            val categoryLabels = listOf("For You", "Quick Picks", "Albums", "Artists", "Discover")
            val displayChips = sourceChips.take(categoryLabels.size).mapIndexed { index, chip ->
                chip.copy(title = categoryLabels[index])
            }
            item(key = "frostsoul_home_tabs") {
                FrostSoulHomeTabs(
                    chips = displayChips,
                    selectedChip = displayChips.firstOrNull { display ->
                        display.endpoint == uiState.selectedChip?.endpoint
                    },
                    onChipSelected = { displayChip ->
                        val sourceChip = sourceChips.firstOrNull { it.endpoint == displayChip.endpoint }
                        onAction(HomeAction.SelectChip(sourceChip))
                    },
                )
            }
        }

        if (uiState.quickPicks.isNotEmpty()) {
            item(key = "frostsoul_home_banner_carousel") {
                FrostSoulBannerCarousel(
                    songs = uiState.quickPicks.take(5),
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                )
            }
            item(key = "frostsoul_everyone_listening") {
                FrostSoulEveryoneListening(
                    songs = uiState.quickPicks.take(3),
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                )
            }
            item(key = "frostsoul_preference_prompt") {
                FrostSoulPreferencePrompt(onClick = { navController.navigate("settings") })
            }
        }

        item(key = "frostsoul_home_hero") {
            FrostSoulHomeHero(
                track = mediaMetadata,
                isPlaying = isPlaying,
                onQuickSearch = { navController.navigate(Screens.Search.route) },
                onPlayPause = { playerConnection.player.togglePlayPause() },
                positionMs = playerConnection.player.currentPosition,
                durationMs = playerConnection.player.duration,
            )
        }

        if (uiState.keepListening.isNotEmpty()) {
            item(key = "frostsoul_continue_listening_header") {
                FSSectionHeader(
                    title = "Continue Listening",
                    actionLabel = "See All",
                    onAction = { navController.navigate(Screens.Library.route) },
                )
            }
            item(key = "frostsoul_continue_listening") {
                FrostSoulLocalShelf(
                    items = uiState.keepListening,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    navController = navController,
                )
            }
        }

        if (uiState.quickPicks.isNotEmpty()) {
            item(key = "frostsoul_for_this_moment_header") {
                FSSectionHeader(title = "For This Moment", actionLabel = "See All", onAction = { navController.navigate(Screens.Search.route) })
            }
            item(key = "frostsoul_for_this_moment") {
                FrostSoulSongShelf(
                    songs = uiState.quickPicks,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    badge = "PLAY",
                    spotlight = false,
                )
            }
            item(key = "frostsoul_recommendation_list") {
                FrostSoulRecommendationList(
                    songs = uiState.quickPicks.take(5),
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                )
            }
        }

        if (uiState.offlineMixes.isNotEmpty()) {
            item(key = "frostsoul_daily_mix_header") {
                FSSectionHeader(title = "Daily Mix", actionLabel = "See All", onAction = { navController.navigate(Screens.Library.route) })
            }
            item(key = "frostsoul_daily_mix") {
                FrostSoulOfflineMixShelf(
                    mixes = uiState.offlineMixes,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                )
            }
        }

        if (uiState.forgottenFavorites.isNotEmpty()) {
            item(key = "frostsoul_recently_added_header") {
                FSSectionHeader(title = "Recently Added", actionLabel = "See All", onAction = { navController.navigate(Screens.Library.route) })
            }
            item(key = "frostsoul_recently_added") {
                FrostSoulSongShelf(
                    songs = uiState.forgottenFavorites,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    badge = "NEW",
                    spotlight = false,
                )
            }
        }

        if (recentItems.isNotEmpty()) {
            item(key = "frostsoul_recently_played_header") {
                FSSectionHeader(title = "Recently Played", eyebrow = "YOUR HISTORY")
            }
            item(key = "frostsoul_recently_played") {
                FSGlassCard(
                    modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
                    contentPadding = PaddingValues(vertical = FrostSoulTheme.spacing.small),
                ) {
                    recentItems.forEach { item ->
                        FSListItem(
                            title = item.title,
                            subtitle = item.frostSoulSubtitle(),
                            artworkUrl = item.frostSoulArtwork(),
                            isActive = item is Song && item.id == mediaMetadata?.id && isPlaying,
                            onClick = { item.openFromFrostSoul(playerConnection, navController) },
                        )
                    }
                }
            }
        }

        if (albums.isNotEmpty()) {
            item(key = "frostsoul_albums_header") {
                FSSectionHeader(title = "Albums", eyebrow = "COLLECTION")
            }
            item(key = "frostsoul_albums") {
                FrostSoulLocalShelf(
                    items = albums,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    navController = navController,
                )
            }
        }

        if (artists.isNotEmpty()) {
            item(key = "frostsoul_artists_header") {
                FSSectionHeader(title = "Artists", eyebrow = "FOLLOW THE VOICE")
            }
            item(key = "frostsoul_artists") {
                LazyRow(
                    contentPadding = FrostSoulShelfItemPadding,
                    horizontalArrangement = Arrangement.spacedBy(FrostSoulShelfSpacing),
                ) {
                    items(artists, key = { it.id }) { artist ->
                        FSArtistCard(
                            name = artist.title,
                            artworkUrl = artist.artist.thumbnailUrl,
                            subtitle = "Artist",
                            onClick = { navController.navigate("artist/${artist.id}") },
                        )
                    }
                }
            }
        }

        uiState.similarRecommendations.forEachIndexed { index, recommendation ->
            item(key = "frostsoul_recommendation_header_${recommendation.title.id}") {
                FSSectionHeader(
                    title = if (index == 0) "Recommended For You" else recommendation.title.title,
                    eyebrow = if (index == 0) "DISCOVER" else "BASED ON ${recommendation.title.title}",
                )
            }
            item(key = "frostsoul_recommendation_${recommendation.title.id}") {
                FSGlassCard(
                    modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
                    contentPadding = PaddingValues(vertical = FrostSoulTheme.spacing.small),
                ) {
                    SimilarRecommendationsSection(
                        recommendation = recommendation,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        scope = scope,
                    )
                }
            }
        }

        pageSections.forEachIndexed { index, section ->
            val sectionKey = "${section.endpoint?.browseId ?: section.title}_$index"
            item(key = "frostsoul_remote_header_$sectionKey") {
                FSSectionHeader(title = section.title, eyebrow = "EXPLORE")
            }
            item(key = "frostsoul_remote_$sectionKey") {
                FSGlassCard(
                    modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
                    contentPadding = PaddingValues(vertical = FrostSoulTheme.spacing.small),
                ) {
                    HomePageSectionContent(
                        section = section,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        scope = scope,
                    )
                }
            }
        }

        if (uiState.isLoadingMore) {
            item(key = "frostsoul_loading_more") {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(FrostSoulTheme.spacing.hero)) {
                    FSLoading()
                }
            }
        }

        if (
            uiState.keepListening.isEmpty() &&
                uiState.quickPicks.isEmpty() &&
                uiState.speedDialItems.isEmpty() &&
                pageSections.isEmpty()
        ) {
            item(key = "frostsoul_home_empty") {
                FSEmptyState(
                    title = "Your music will appear here",
                    message = "Start a search or play something to build a listening home tailored to you.",
                    modifier = Modifier.height(360.dp),
                    actionLabel = "Quick Search",
                    onAction = { navController.navigate(Screens.Search.route) },
                )
            }
        }
    }
}

@Composable
private fun FrostSoulBannerCarousel(
    songs: List<Song>,
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
) {
    LazyRow(
        contentPadding = FrostSoulShelfItemPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(songs, key = { "banner_${it.id}" }) { song ->
            FSGlassCard(
                modifier = Modifier.width(185.dp).height(123.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp),
                onClick = {
                    if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                    else playerConnection.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = song.song.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                listOf(Color.Black.copy(alpha = 0.92f), Color.Transparent),
                            ),
                        ),
                    )
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        Text("FEATURED FOR YOU", color = FrostSoulTheme.colors.accentBright, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                        Spacer(Modifier.weight(1f))
                        Text(song.title, color = FrostSoulTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artists.joinToString(" • ") { it.name }, color = FrostSoulTheme.colors.onSurfaceMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 3.dp))
                    }
                    FSIconButton(
                        onClick = {
                            if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                            else playerConnection.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                        },
                        highlighted = true,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                    ) {
                        FSIcon(painterResource(if (song.id == mediaMetadata?.id && playerConnection.player.isPlaying) R.drawable.pause else R.drawable.play), contentDescription = "Play ${song.title}", tint = FrostSoulTheme.colors.accentBright)
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostSoulEveryoneListening(
    songs: List<Song>,
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
    ) {
        FSSectionHeader(title = "Everyone is listening", actionLabel = "Play all", onAction = {
            playerConnection.playQueue(ListQueue(items = songs.map { it.toMediaItem() }))
        })
        songs.forEach { song ->
            FSListItem(
                title = song.title,
                subtitle = song.artists.joinToString(" • ") { it.name },
                artworkUrl = song.song.thumbnailUrl,
                isActive = song.id == mediaMetadata?.id && playerConnection.player.isPlaying,
                onClick = {
                    if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                    else playerConnection.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                },
            )
        }
    }
}

@Composable
private fun FrostSoulPreferencePrompt(onClick: () -> Unit) {
    FSGlassCard(
        modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page).fillMaxWidth(),
        shape = FrostSoulTheme.shapes.large,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        onClick = onClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FSText("Tell us your music taste", color = FrostSoulTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            FSText("Get recommendations tuned to your listening.", color = FrostSoulTheme.colors.onSurfaceMuted, fontSize = 13.sp)
            FSButton(label = "Set preferences", onClick = onClick, modifier = Modifier.padding(top = 8.dp), emphasized = true)
        }
    }
}

@Composable
private fun FrostSoulRecommendationList(
    songs: List<Song>,
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
) {
    LazyRow(
        contentPadding = FrostSoulShelfItemPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(songs, key = { "quick_card_${it.id}" }) { song ->
            val isCurrent = song.id == mediaMetadata?.id
            FSGlassCard(
                modifier = Modifier.width(148.dp).height(184.dp),
                shape = FrostSoulTheme.shapes.medium,
                contentPadding = PaddingValues(10.dp),
                onClick = {
                    if (isCurrent) playerConnection.player.togglePlayPause()
                    else playerConnection.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                },
            ) {
                AsyncImage(
                    model = song.song.thumbnailUrl,
                    contentDescription = "Artwork for ${song.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(108.dp).clip(FrostSoulTheme.shapes.small),
                )
                Text(
                    text = song.title,
                    color = FrostSoulTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = song.artists.firstOrNull()?.name.orEmpty(),
                    color = FrostSoulTheme.colors.onSurfaceMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                FSIcon(
                    painter = painterResource(if (isCurrent && playerConnection.player.isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isCurrent) "Pause ${song.title}" else "Play ${song.title}",
                    tint = FrostSoulTheme.colors.onSurface,
                    modifier = Modifier.size(18.dp).padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun FrostSoulQuickSearch(onOpenSearch: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).height(32.dp).clip(FrostSoulTheme.shapes.pill).background(Color(0xFF1E1E1E)).clickable(onClick = onOpenSearch).padding(horizontal = 12.dp),
        ) {
            FSIcon(
                painter = painterResource(R.drawable.search),
                contentDescription = "Search",
                tint = FrostSoulTheme.colors.onSurfaceMuted,
                modifier = Modifier.size(16.dp),
            )
            FSText(
                text = "Search songs, albums, artists...",
                color = FrostSoulTheme.colors.onSurfaceMuted,
                style = FrostSoulTheme.typography.body,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF1E1E1E)).clickable(onClick = onOpenSearch),
        ) {
            FSIcon(
                painter = painterResource(R.drawable.mic),
                contentDescription = "Voice search",
                tint = FrostSoulTheme.colors.accentBright,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun FrostSoulHomeHeader(
    accountName: String,
    onOpenNotifications: () -> Unit,
) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeOfDay =
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    val firstName = accountName.trim().substringBefore(' ').takeIf { it.isNotBlank() }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                FSIcon(
                    painter = painterResource(R.drawable.about_appbar),
                    contentDescription = "FrostSoul",
                    tint = FrostSoulTheme.colors.accentBright,
                    modifier = Modifier.size(30.dp),
                )
                FSText(
                    text = "F R O S T S O U L",
                    color = FrostSoulTheme.colors.accentBright,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.4.sp,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Box {
                FSIconButton(
                    onClick = onOpenNotifications,
                    contentDescription = "Notifications",
                    modifier = Modifier.size(44.dp),
                ) {
                    FSIcon(
                        painter = painterResource(R.drawable.newspaper),
                        contentDescription = null,
                        tint = FrostSoulTheme.colors.onSurface,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .background(FrostSoulTheme.colors.accentBright, FrostSoulTheme.shapes.pill),
                )
            }
        }
        FSText(
            text = if (firstName == null) timeOfDay else "$timeOfDay, $firstName",
            color = FrostSoulTheme.colors.onSurface,
            style = FrostSoulTheme.typography.display,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FrostSoulHomeTabs(
    chips: List<HomePage.Chip>,
    selectedChip: HomePage.Chip?,
    onChipSelected: (HomePage.Chip) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = FrostSoulTheme.spacing.page),
    ) {
        chips.forEach { chip ->
            val selected = chip == selectedChip
            val indicatorWidth by animateDpAsState(
                targetValue = if (selected) 28.dp else 0.dp,
                animationSpec = tween(durationMillis = 220),
                label = "frostsoul-home-tab-indicator",
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier =
                    Modifier
                        .clickable { onChipSelected(chip) }
                        .padding(vertical = 8.dp),
            ) {
                FSText(
                    text = chip.title,
                    color = if (selected) FrostSoulTheme.colors.onSurface else FrostSoulTheme.colors.onSurfaceMuted,
                    style = FrostSoulTheme.typography.label,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier =
                        Modifier
                            .padding(top = 8.dp)
                            .width(indicatorWidth)
                            .height(2.dp)
                            .background(FrostSoulTheme.colors.accentBright),
                )
            }
        }
    }
}

@Composable
private fun FrostSoulHomeHero(
    track: MediaMetadata?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onQuickSearch: () -> Unit,
    onPlayPause: () -> Unit,
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    FSGlassCard(
        modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page).fillMaxWidth(),
        shape = FrostSoulTheme.shapes.extraLarge,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(208.dp)) {
            track?.thumbnailUrl?.let { artworkUrl ->
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(Color.Black.copy(alpha = 0.98f), Color.Black.copy(alpha = 0.68f), Color.Transparent),
                    ),
                ),
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))),
                ),
            )
            Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                FSText(
                    text = "NOW PLAYING",
                    color = FrostSoulTheme.colors.accentBright,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(10.dp))
                FSText(
                    text = track?.title ?: "Nothing playing",
                    color = FrostSoulTheme.colors.onSurface,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                FSText(
                    text = track?.artists?.joinToString(separator = " • ") { it.name } ?: "Choose a song to begin",
                    color = FrostSoulTheme.colors.onSurfaceMuted,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                if (track != null) {
                    FSText(
                        text = "FLAC",
                        color = FrostSoulTheme.colors.accentBright,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(3.dp).background(FrostSoulTheme.colors.onSurfaceMuted.copy(alpha = 0.34f), FrostSoulTheme.shapes.pill),
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(progress).height(3.dp).background(FrostSoulTheme.colors.accentBright, FrostSoulTheme.shapes.pill),
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            FSText(positionMs.asFrostSoulTime(), color = FrostSoulTheme.colors.onSurfaceMuted, fontSize = 11.sp)
                            FSText(durationMs.asFrostSoulTime(), color = FrostSoulTheme.colors.onSurfaceMuted, fontSize = 11.sp)
                        }
                    }
                    FSIconButton(
                        onClick = if (track != null) onPlayPause else onQuickSearch,
                        highlighted = true,
                        modifier = Modifier.padding(start = 16.dp),
                    ) {
                        FSIcon(
                            painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = FrostSoulTheme.colors.accentBright,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostSoulSongShelf(
    songs: List<Song>,
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
    badge: String? = null,
    spotlight: Boolean = false,
) {
    LazyRow(
        contentPadding = FrostSoulShelfItemPadding,
        horizontalArrangement = Arrangement.spacedBy(FrostSoulShelfSpacing),
    ) {
        items(songs, key = { it.id }) { song ->
            FSAlbumCard(
                title = song.title,
                subtitle = song.artists.joinToString(separator = " • ") { it.name },
                artworkUrl = song.song.thumbnailUrl,
                badge = badge,
                width = if (spotlight) 256.dp else 154.dp,
                artworkAspectRatio = if (spotlight) 1.28f else 1f,
                showPlayOverlay = true,
                onClick = {
                    if (song.id == mediaMetadata?.id) {
                        playerConnection.player.togglePlayPause()
                    } else {
                        playerConnection.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                    }
                },
            )
        }
    }
}

@Composable
private fun FrostSoulOfflineMixShelf(
    mixes: List<LibraryTopMix>,
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
) {
    LazyRow(
        contentPadding = FrostSoulShelfItemPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(mixes, key = { it.id }) { mix ->
            FSGlassCard(
                modifier = Modifier.width(196.dp).height(86.dp),
                shape = FrostSoulTheme.shapes.large,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(title = mix.title, items = mix.tracks.map { it.toMediaItem() }),
                    )
                },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        FSText(
                            text = mix.title,
                            color = FrostSoulTheme.colors.onSurface,
                            style = FrostSoulTheme.typography.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        FSText(
                            text = mix.description.ifBlank { "For today" },
                            color = FrostSoulTheme.colors.onSurfaceMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(0.72f).padding(top = 10.dp).height(2.dp)
                                .background(FrostSoulTheme.colors.accentBright, FrostSoulTheme.shapes.pill),
                        )
                    }
                    FSIconButton(
                        onClick = {
                            playerConnection.playQueue(
                                ListQueue(title = mix.title, items = mix.tracks.map { it.toMediaItem() }),
                            )
                        },
                        highlighted = false,
                        modifier = Modifier.size(38.dp),
                    ) {
                        FSIcon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = "Play ${mix.title}",
                            tint = FrostSoulTheme.colors.accentBright,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostSoulLocalShelf(
    items: List<LocalItem>,
    mediaMetadata: MediaMetadata?,
    playerConnection: PlayerConnection,
    navController: NavController,
    badge: String? = null,
) {
    LazyRow(
        contentPadding = FrostSoulShelfItemPadding,
        horizontalArrangement = Arrangement.spacedBy(FrostSoulShelfSpacing),
    ) {
        items(items, key = { item -> "${item::class.simpleName}_${item.id}" }) { item ->
            FSAlbumCard(
                title = item.title,
                subtitle = item.frostSoulSubtitle(),
                artworkUrl = item.frostSoulArtwork(),
                badge = badge,
                showPlayOverlay = true,
                onClick = {
                    if (item is Song && item.id == mediaMetadata?.id) {
                        playerConnection.player.togglePlayPause()
                    } else {
                        item.openFromFrostSoul(playerConnection, navController)
                    }
                },
            )
        }
    }
}

private fun LocalItem.openFromFrostSoul(
    playerConnection: PlayerConnection,
    navController: NavController,
) {
    when (this) {
        is Song -> playerConnection.playQueue(ListQueue(items = listOf(toMediaItem())))
        is Album -> navController.navigate("album/$id")
        is Artist -> navController.navigate("artist/$id")
        is Playlist -> navController.navigate("local_playlist/$id")
    }
}

private fun LocalItem.frostSoulArtwork(): String? =
    when (this) {
        is Playlist -> thumbnails.firstOrNull()
        else -> thumbnailUrl
    }

private fun LocalItem.frostSoulSubtitle(): String =
    when (this) {
        is Song -> artists.joinToString(separator = " • ") { it.name }
        is Album -> artists.joinToString(separator = " • ") { it.name }.ifBlank { "Album" }
        is Artist -> "Artist"
        is Playlist -> "$songCount tracks"
    }
