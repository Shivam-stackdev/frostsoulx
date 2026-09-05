/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.vxs.frostsoulx.lyrics.LyricsUtils
import dev.vxs.frostsoulx.db.entities.LyricsEntity
import dev.vxs.frostsoulx.ui.menu.SongMenu
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlin.math.abs
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
import dev.vxs.frostsoulx.ui.frostsoul.FSIconButton
import dev.vxs.frostsoulx.ui.frostsoul.FSGlassCard
import dev.vxs.frostsoulx.ui.frostsoul.FSTextField
import dev.vxs.frostsoulx.ui.frostsoul.FSLoading
import dev.vxs.frostsoulx.ui.frostsoul.FSSectionHeader
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme
import dev.vxs.frostsoulx.ui.premium.PremiumCard
import dev.vxs.frostsoulx.ui.premium.PremiumHeroBanner
import dev.vxs.frostsoulx.ui.premium.PremiumListRow
import dev.vxs.frostsoulx.ui.premium.PremiumSearchBar
import dev.vxs.frostsoulx.ui.premium.PremiumSegmentedTabs
import dev.vxs.frostsoulx.ui.premium.PremiumTopBar
import dev.vxs.frostsoulx.ui.frostsoul.frostSoulScreenBackground
import dev.vxs.frostsoulx.ui.player.frostsoul.asFrostSoulTime
import dev.vxs.frostsoulx.utils.UserGreetingPreferences
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope
import java.util.Calendar

private val FrostSoulShelfItemPadding = PaddingValues(horizontal = 16.dp)
private val FrostSoulShelfSpacing = 12.dp

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
    val context = LocalContext.current
    var greetingName by remember(context) { mutableStateOf(UserGreetingPreferences.getName(context)) }
    var showGreetingNameDialog by remember(context) {
        mutableStateOf(!UserGreetingPreferences.hasPrompted(context))
    }
    var greetingNameDraft by rememberSaveable { mutableStateOf("") }
    val albums = remember(uiState.speedDialItems) { uiState.speedDialItems.filterIsInstance<Album>() }
    val artists = remember(uiState.speedDialItems) { uiState.speedDialItems.filterIsInstance<Artist>() }
    val recentItems = remember(uiState.keepListening) { uiState.keepListening.take(6) }
    val openSearchPortal: () -> Unit = {
        navController.currentBackStackEntry?.savedStateHandle?.set("openSearch", true)
    }
    val pageSections = uiState.homePage?.sections.orEmpty()
    var category by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        state = lazyListState,
        contentPadding =
            PaddingValues(
                top = 4.dp,
                bottom = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding() + MiniPlayerHeight + 108.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color(0xFF131723), 0.16f to Color(0xFF211A25),
            0.30f to Color(0xFF07090D), 1f to Color(0xFF07090D),
        )).statusBarsPadding(),
    ) {
        item(key = "frostsoul_home_header") {
            FrostSoulHomeHeader(
                userName = greetingName,
                currentSong = mediaMetadata,
                onOpenRecent = { navController.navigate(Screens.Settings.route) },
                onSearch = openSearchPortal,
            )
        }

        item(key = "frostsoul_quick_search") {
            FrostSoulQuickSearch(onOpenSearch = { openSearchPortal() })
        }

        item(key = "frostsoul_home_tabs") {
            // These are real destinations, not arbitrary server chips relabelled by index.
            LazyRow(contentPadding = FrostSoulShelfItemPadding, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("For You", "Quick Picks", "Albums", "Artists", "Genres")) { label ->
                    val selected = label == (category ?: "For You")
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.heightIn(min = 48.dp)
                        .clickable {
                            when (label) {
                                "For You" -> { category = null; onAction(HomeAction.SelectChip(null)) }
                                "Genres" -> navController.navigate(Screens.MoodAndGenres.route)
                                else -> category = label
                            }
                        }) {
                        Text(label, color = if (selected) Color.Black else Color(0xFFB8B5C6), fontSize = 11.sp,
                            modifier = Modifier.clip(CircleShape)
                                .background(if (selected) Color.White else Color.White.copy(alpha = 0.04f))
                                .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape)
                                .padding(horizontal = 15.dp, vertical = 7.dp))
                    }
                }
            }
        }

        item(key = "frostsoul_home_hero") {
            FrostSoulHomeHero(
                track = mediaMetadata,
                isPlaying = isPlaying,
                onQuickSearch = { openSearchPortal() },
                playerConnection = playerConnection,
                navController = navController,
                menuState = menuState,
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
            item(key = "frostsoul_home_banner_carousel") {
                FrostSoulBannerCarousel(
                    songs = uiState.quickPicks.take(5),
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                )
            }
            item(key = "frostsoul_made_for_you_header") {
                FSSectionHeader(title = "Made for you", actionLabel = "See All", onAction = { openSearchPortal() })
            }
            item(key = "frostsoul_made_for_you") {
                FrostSoulSongShelf(
                    songs = uiState.quickPicks,
                    mediaMetadata = mediaMetadata,
                    playerConnection = playerConnection,
                    badge = null,
                    spotlight = false,
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
                FSSectionHeader(
                    title = "Recently Played",
                    eyebrow = "YOUR HISTORY",
                    actionLabel = "See All",
                    onAction = { navController.navigate("history") },
                )
            }
            item(key = "frostsoul_recently_played") {
                PremiumCard(
                    modifier = Modifier.padding(horizontal = FrostSoulTheme.spacing.page),
                    contentPadding = PaddingValues(vertical = FrostSoulTheme.spacing.small),
                ) {
                    recentItems.forEach { item ->
                        PremiumListRow(
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
                PremiumCard(
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
                PremiumCard(
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
                    onAction = { openSearchPortal() },
                )
            }
        }
    }

    category?.let { selected ->
        val categoryItems: List<LocalItem> = when (selected) {
            "Quick Picks" -> uiState.quickPicks
            "Albums" -> albums
            else -> artists
        }
        AlertDialog(
            onDismissRequest = { category = null },
            title = { Text(selected, color = Color.White, fontSize = 22.sp) },
            containerColor = Color(0xFF161920),
            text = {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    if (categoryItems.isEmpty()) item {
                        Text("No ${selected.lowercase()} yet. Explore music to build your collection.", color = Color.LightGray)
                    }
                    items(categoryItems, key = { "${it::class.simpleName}_${it.id}" }) { item ->
                        PremiumListRow(title = item.title, subtitle = item.frostSoulSubtitle(),
                            artworkUrl = item.frostSoulArtwork(), onClick = {
                                category = null
                                item.openFromFrostSoul(playerConnection, navController)
                            })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { category = null }) { Text("Close", color = Color.White) } },
        )
    }

    if (showGreetingNameDialog) {
        Dialog(
            onDismissRequest = {
                UserGreetingPreferences.skip(context)
                showGreetingNameDialog = false
            },
        ) {
            FSGlassCard(
                modifier = Modifier.widthIn(max = 420.dp),
                shape = FrostSoulTheme.shapes.extraLarge,
            ) {
                FSText(
                    text = "Make FrostSoul yours",
                    style = FrostSoulTheme.typography.title,
                    color = FrostSoulTheme.colors.onSurface,
                )
                Spacer(Modifier.height(FrostSoulTheme.spacing.small))
                FSText(
                    text = "What should we call you on the home screen?",
                    style = FrostSoulTheme.typography.body,
                    color = FrostSoulTheme.colors.onSurfaceMuted,
                )
                Spacer(Modifier.height(FrostSoulTheme.spacing.medium))
                FSTextField(
                    value = greetingNameDraft,
                    onValueChange = { greetingNameDraft = it.take(40) },
                    placeholder = "Your name",
                )
                Spacer(Modifier.height(FrostSoulTheme.spacing.medium))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.small),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    FSButton(
                        label = "Not now",
                        onClick = {
                            UserGreetingPreferences.skip(context)
                            showGreetingNameDialog = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                    FSButton(
                        label = "Save",
                        onClick = {
                            val name = greetingNameDraft.trim()
                            if (name.isNotBlank()) {
                                UserGreetingPreferences.save(context, name)
                                greetingName = name
                                showGreetingNameDialog = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        emphasized = true,
                    )
                }
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - 32.dp).coerceAtLeast(240.dp)
        val cardHeight = 120.dp
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FSSectionHeader(title = "Featured for you", actionLabel = "Play all", onAction = {
                playerConnection.playQueue(ListQueue(items = songs.map { it.toMediaItem() }))
            })
            Text("Because you love soulful melodies", color = Color(0xFFAAA7B5), fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 16.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(cardHeight),
            ) {
                items(songs, key = { "banner_${it.id}" }) { song ->
                    PremiumCard(
                        modifier = Modifier.width(cardWidth).fillMaxHeight(),
                        shape = RoundedCornerShape(12.dp),
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
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.55f to Color.Transparent,
                                        1f to Color.Black.copy(alpha = 0.86f),
                                    ),
                                ),
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.72f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    ),
                            )
                            Column(
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxSize().padding(14.dp),
                            ) {
                                Text("FEATURED", color = Color.White.copy(alpha = 0.8f), fontSize = 7.sp)
                                Text(
                                    text = song.title,
                                    style = FrostSoulTheme.typography.display.copy(fontFamily = FontFamily.Serif),
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = song.artists.joinToString(" • ") { it.name },
                                    color = Color.White.copy(alpha = 0.82f),
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                Text("Play now", color = Color.White, fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 6.dp).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 4.dp))
                            }
                            FSIconButton(
                                onClick = {
                                    if (song.id == mediaMetadata?.id) playerConnection.player.togglePlayPause()
                                    else playerConnection.playQueue(ListQueue(items = listOf(song.toMediaItem())))
                                },
                                highlighted = true,
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                            ) {
                                FSIcon(
                                    painterResource(if (song.id == mediaMetadata?.id && playerConnection.player.isPlaying) R.drawable.pause else R.drawable.play),
                                    contentDescription = "Play ${song.title}",
                                    tint = FrostSoulTheme.colors.accentBright,
                                )
                            }
                        }
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
            PremiumListRow(
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
    PremiumCard(
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
    val carouselState = rememberLazyListState()

    LazyRow(
        state = carouselState,
        contentPadding = FrostSoulShelfItemPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(songs, key = { "quick_card_${it.id}" }) { song ->
            val isCurrent = song.id == mediaMetadata?.id
            PremiumCard(
                modifier = Modifier
                    .width(148.dp)
                    .height(184.dp)
                    .graphicsLayer {
                        // Scroll information is read in the layer so transforms update without
                        // recomposing the complete recommendation section on every scroll tick.
                        val item = carouselState.layoutInfo.visibleItemsInfo
                            .firstOrNull { it.key == "quick_card_${song.id}" }
                        if (item != null) {
                            val viewportCenter = (
                                carouselState.layoutInfo.viewportStartOffset +
                                    carouselState.layoutInfo.viewportEndOffset
                            ) / 2f
                            val itemCenter = item.offset + item.size / 2f
                            val pageDistance = ((itemCenter - viewportCenter) / item.size)
                                .coerceIn(-1.25f, 1.25f)
                            val distance = abs(pageDistance).coerceIn(0f, 1f)
                            val focus = 1f - distance

                            // Center card stays dominant; neighbors fan back with perspective.
                            scaleX = 0.86f + 0.14f * focus
                            scaleY = 0.86f + 0.14f * focus
                            rotationY = -18f * pageDistance
                            alpha = 0.64f + 0.36f * focus
                            translationX = -pageDistance * 8.dp.toPx()
                            cameraDistance = 12f * density
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                            shape = RoundedCornerShape(16.dp)
                            clip = true
                        }
                    },
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
                    modifier = Modifier.padding(start = 2.dp, top = 3.dp),
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(48.dp)
            .clip(CircleShape).background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.13f), CircleShape)
            .clickable(onClickLabel = "Search music", onClick = onOpenSearch).padding(horizontal = 16.dp)) {
        FSIcon(painterResource(R.drawable.search), null, tint = Color(0xFFBAB6CC), modifier = Modifier.size(18.dp))
        Text("Search songs, albums, artists...", color = Color(0xFFBAB6CC), fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        FSIcon(painterResource(R.drawable.equalizer), null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun FrostSoulHomeHeader(
    userName: String?,
    currentSong: MediaMetadata?,
    onOpenRecent: () -> Unit,
    onSearch: () -> Unit,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var hour by remember { mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); delay(60_000) }
        }
    }
    val greeting = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..21 -> "Good Evening"
        else -> "Good Night"
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("F R O S T S O U L", color = Color.White, fontSize = 9.sp, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(greeting, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Canvas(Modifier.size(22.dp)) {
                    drawCircle(Color(0xFFFBE4B4))
                    drawCircle(Color(0xFF171925), radius = size.width * 0.46f,
                        center = Offset(size.width * 0.32f, size.height * 0.28f))
                }
            }
            Text(userName?.let { "$it, music feels better with you here." } ?: "Music feels better with you here.",
                color = Color(0xFFA8A4B5), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FSIconButton(onClick = onSearch, contentDescription = "Search", modifier = Modifier.size(48.dp)) {
            FSIcon(painterResource(R.drawable.search), null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp).clickable(onClickLabel = "Settings", onClick = onOpenRecent)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF1E334F))
                .border(1.dp, Color(0xFF516487), CircleShape), contentAlignment = Alignment.Center) {
                if (currentSong?.thumbnailUrl != null) AsyncImage(currentSong.thumbnailUrl, null,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                else FSIcon(painterResource(R.drawable.person), null, tint = Color(0xFFB9CDEC), modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun FrostSoulHomeTabs(
    chips: List<HomePage.Chip>,
    selectedChip: HomePage.Chip?,
    onChipSelected: (HomePage.Chip) -> Unit,
) {
    if (chips.isEmpty()) return

    val selectedEndpoint = selectedChip?.endpoint
    val selectedIndex = chips.indexOfFirst { it.endpoint == selectedEndpoint }.let { if (it < 0) 0 else it }
    PremiumSegmentedTabs(
        labels = chips.map { it.title },
        selectedIndex = selectedIndex,
        onSelected = { index -> onChipSelected(chips[index]) },
        modifier = Modifier.heightIn(min = 56.dp),
    )
}

@Composable
private fun FrostSoulHomeHero(
    track: MediaMetadata?,
    isPlaying: Boolean,
    onQuickSearch: () -> Unit,
    playerConnection: PlayerConnection,
    navController: NavController,
    menuState: MenuState,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var positionMs by remember(track?.id) { mutableLongStateOf(0L) }
    var durationMs by remember(track?.id) { mutableLongStateOf(0L) }
    val song by playerConnection.currentSong.collectAsStateWithLifecycle(initialValue = null)
    val lyrics by playerConnection.currentLyrics.collectAsStateWithLifecycle(initialValue = null)
    val shuffle by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
    val previous by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val next by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    var showLyrics by rememberSaveable(track?.id) { mutableStateOf(false) }
    LaunchedEffect(playerConnection, track?.id, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                positionMs = playerConnection.player.currentPosition.coerceAtLeast(0L)
                durationMs = playerConnection.player.duration.coerceAtLeast(0L)
                delay(500)
            }
        }
    }
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    PremiumHeroBanner(
        artworkUrl = track?.thumbnailUrl,
        title = track?.title ?: "Nothing playing",
        subtitle = track?.artists?.joinToString(" • ") { it.name } ?: "Choose a song to begin",
        modifier = Modifier.padding(horizontal = 16.dp),
        isPlaying = isPlaying, progress = progress,
        positionLabel = positionMs.asFrostSoulTime(), durationLabel = durationMs.asFrostSoulTime(),
        onPlayPause = { if (track == null) onQuickSearch() else playerConnection.player.togglePlayPause() },
        isLiked = song?.song?.liked == true, shuffleEnabled = shuffle,
        canSkipPrevious = track != null && previous, canSkipNext = track != null && next,
        onPrevious = playerConnection::seekToPrevious, onNext = playerConnection::seekToNext,
        onShuffle = { playerConnection.player.shuffleModeEnabled = !shuffle },
        onLike = { if (track != null) playerConnection.toggleLike() else onQuickSearch() },
        onLyrics = if (track != null) ({ showLyrics = true }) else null,
        onMore = song?.let { current -> { menuState.show { SongMenu(originalSong = current,
            navController = navController, onDismiss = menuState::dismiss) } } },
    )
    if (showLyrics) {
        val lines = remember(lyrics) {
            lyrics?.lyrics?.takeUnless { it == LyricsEntity.LYRICS_NOT_FOUND }?.let {
                runCatching { LyricsUtils.parseLyrics(it) }.getOrDefault(emptyList())
            }.orEmpty()
        }
        val activeIndex = LyricsUtils.findCurrentLineIndex(lines, positionMs)
        AlertDialog(onDismissRequest = { showLyrics = false }, containerColor = Color(0xFF161920),
            title = { Text(track?.title.orEmpty(), color = Color.White, fontSize = 20.sp) },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (lines.isEmpty()) item { Text("Lyrics aren't available for this track yet.", color = Color.LightGray) }
                    items(lines.size) { index -> Text(lines[index].text,
                        color = if (index == activeIndex) Color(0xFFF7DEAF) else Color.LightGray, fontSize = 16.sp) }
                }
            },
            confirmButton = { TextButton(onClick = { showLyrics = false }) { Text("Close", color = Color.White) } },
        )
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
                width = if (spotlight) 256.dp else 92.dp,
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
            PremiumCard(
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
                subtitle = null,
                artworkUrl = item.frostSoulArtwork(),
                width = 88.dp,
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
