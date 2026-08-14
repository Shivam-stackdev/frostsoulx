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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import dev.vxs.frostsoulx.playback.PlayerConnection
import dev.vxs.frostsoulx.playback.queues.ListQueue
import dev.vxs.frostsoulx.ui.component.MenuState
import dev.vxs.frostsoulx.ui.frostsoul.FSAlbumCard
import dev.vxs.frostsoulx.ui.frostsoul.FSArtistCard
import dev.vxs.frostsoulx.ui.frostsoul.FSButton
import dev.vxs.frostsoulx.ui.frostsoul.FSChip
import dev.vxs.frostsoulx.ui.frostsoul.FSEmptyState
import dev.vxs.frostsoulx.ui.frostsoul.FSGlassCard
import dev.vxs.frostsoulx.ui.frostsoul.FSTextField
import dev.vxs.frostsoulx.ui.frostsoul.FSListItem
import dev.vxs.frostsoulx.ui.frostsoul.FSLoading
import dev.vxs.frostsoulx.ui.frostsoul.FSSectionHeader
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme
import dev.vxs.frostsoulx.ui.frostsoul.frostSoulScreenBackground
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
                top = 16.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + MiniPlayerHeight + 108.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.section),
        modifier = modifier.fillMaxSize().frostSoulScreenBackground(),
    ) {
        item(key = "frostsoul_home_greeting") {
            FrostSoulGreetingHeader(accountName = uiState.accountName)
        }

        item(key = "frostsoul_home_hero") {
            FrostSoulHomeHero(
                track = mediaMetadata,
                isPlaying = isPlaying,
                onQuickSearch = { navController.navigate(Screens.Search.route) },
                onPlayPause = { playerConnection.player.togglePlayPause() },
            )
        }

        item(key = "frostsoul_quick_search") {
            FrostSoulQuickSearch(onOpenSearch = { navController.navigate(Screens.Search.route) })
        }

        if (uiState.showCategoryChips && uiState.homePage?.chips?.isNotEmpty() == true) {
            item(key = "frostsoul_home_tabs") {
                FrostSoulHomeTabs(
                    chips = uiState.homePage?.chips.orEmpty(),
                    selectedChip = uiState.selectedChip,
                    onChipSelected = { chip ->
                        onAction(HomeAction.SelectChip(if (chip == uiState.selectedChip) null else chip))
                    },
                )
            }
        }

        if (uiState.quickPicks.isNotEmpty()) {
            item(key = "frostsoul_spotlight_header") {
                FSSectionHeader(title = "Made for you", eyebrow = "SPOTLIGHT")
            }
            item(key = "frostsoul_spotlight") {
                FrostSoulSongShelf(
                    songs = uiState.quickPicks,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    badge = "FOR YOU",
                    spotlight = true,
                )
            }
        }

        if (uiState.keepListening.isNotEmpty()) {
            item(key = "frostsoul_continue_listening_header") {
                FSSectionHeader(
                    title = "Finally, you are here",
                    eyebrow = "RESUME LISTENING",
                    actionLabel = if (isPlaying) "PAUSE" else "RESUME",
                    onAction = {
                        val first = uiState.keepListening.firstOrNull()
                        if (first is Song && first.id != mediaMetadata?.id) {
                            first.openFromFrostSoul(playerConnection, navController)
                        } else {
                            playerConnection.player.togglePlayPause()
                        }
                    },
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

        if (uiState.speedDialItems.isNotEmpty()) {
            item(key = "frostsoul_daily_mix_header") {
                FSSectionHeader(title = "Daily Mix", eyebrow = "MADE FOR THIS MOMENT")
            }
            item(key = "frostsoul_daily_mix") {
                FrostSoulLocalShelf(
                    items = uiState.speedDialItems,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    navController = navController,
                    badge = "MIX",
                )
            }
        }

        uiState.offlineMixes.forEach { mix ->
            item(key = "frostsoul_offline_mix_header_${mix.id}") {
                FSSectionHeader(title = mix.title, eyebrow = "ON DEVICE")
            }
            item(key = "frostsoul_offline_mix_${mix.id}") {
                FSGlassCard(
                    modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
                    contentPadding = PaddingValues(vertical = FrostSoulTheme.spacing.small),
                ) {
                    mix.tracks.take(6).forEach { track ->
                        FSListItem(
                            title = track.title,
                            subtitle = track.artists.joinToString { it.name }.ifBlank { mix.description },
                            artworkUrl = track.thumbnailUrl,
                            isActive = track.id == mediaMetadata?.id && isPlaying,
                            onClick = {
                                playerConnection.playQueue(
                                    ListQueue(
                                        title = mix.title,
                                        items = mix.tracks.map { it.toMediaItem() },
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }

        if (uiState.forgottenFavorites.isNotEmpty()) {
            item(key = "frostsoul_forgotten_header") {
                FSSectionHeader(title = "Forgotten Gems", eyebrow = "REVISIT")
            }
            item(key = "frostsoul_forgotten") {
                FrostSoulSongShelf(
                    songs = uiState.forgottenFavorites,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    badge = "GEM",
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
private fun FrostSoulQuickSearch(onOpenSearch: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.small)) {
        FSSectionHeader(title = "Quick Search", eyebrow = "FIND YOUR NEXT PLAY")
        FSTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = "Artist, album, song, or mood",
            leading = {
                androidx.compose.material3.Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = null,
                    tint = FrostSoulTheme.colors.accentBright,
                )
            },
            trailing = {
                FSButton(
                    label = "Search",
                    onClick = onOpenSearch,
                    emphasized = query.isNotBlank(),
                )
            },
            modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
        )
    }
}

@Composable
private fun FrostSoulGreetingHeader(accountName: String) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeOfDay =
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Good night"
        }
    val firstName = accountName.trim().substringBefore(' ').takeIf { it.isNotBlank() }

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page, vertical = 4.dp),
    ) {
        androidx.compose.material3.Text(
            text = if (firstName == null) timeOfDay else "$timeOfDay, $firstName",
            color = FrostSoulTheme.colors.onSurface,
            style = FrostSoulTheme.typography.display,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        androidx.compose.material3.Text(
            text = "A listening space shaped by your library.",
            color = FrostSoulTheme.colors.onSurfaceMuted,
            style = FrostSoulTheme.typography.body,
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
                androidx.compose.material3.Text(
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
    onQuickSearch: () -> Unit,
    onPlayPause: () -> Unit,
) {
    FSGlassCard(
        modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
        shape = FrostSoulTheme.shapes.extraLarge,
    ) {
        androidx.compose.material3.Text(
            text = "FROSTSOUL",
            color = FrostSoulTheme.colors.accent,
            style = FrostSoulTheme.typography.overline,
        )
        Spacer(Modifier.height(FrostSoulTheme.spacing.small))
        androidx.compose.material3.Text(
            text = track?.title ?: "Your sound, uninterrupted.",
            color = FrostSoulTheme.colors.onSurface,
            style = FrostSoulTheme.typography.display,
            maxLines = 2,
        )
        Spacer(Modifier.height(FrostSoulTheme.spacing.small))
        androidx.compose.material3.Text(
            text = track?.artists?.joinToString(separator = " • ") { it.name } ?: "A focused library for every listening moment.",
            color = FrostSoulTheme.colors.onSurfaceMuted,
            style = FrostSoulTheme.typography.body,
            maxLines = 2,
        )
        Spacer(Modifier.height(FrostSoulTheme.spacing.large))
        Row(horizontalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.small)) {
            FSButton(
                label = if (track != null) if (isPlaying) "Pause" else "Play" else "Quick Search",
                onClick = if (track != null) onPlayPause else onQuickSearch,
            )
            FSButton(
                label = if (track != null) "Quick Search" else "Explore",
                onClick = onQuickSearch,
                emphasized = false,
            )
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
                showPlayOverlay = spotlight,
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
