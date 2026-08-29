/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import dev.vxs.frostsoulx.constants.LyricsProviderOrderKey
import dev.vxs.frostsoulx.constants.PreferredLyricsProvider
import dev.vxs.frostsoulx.constants.deserializeLyricsProviderOrder
import dev.vxs.frostsoulx.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import dev.vxs.frostsoulx.models.MediaMetadata
import dev.vxs.frostsoulx.utils.GlobalLog
import dev.vxs.frostsoulx.utils.NetworkConnectivityObserver
import dev.vxs.frostsoulx.utils.dataStore
import dev.vxs.frostsoulx.utils.reportException
import javax.inject.Inject

class LyricsHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val networkConnectivity: NetworkConnectivityObserver,
    ) {
        private val baseProviders =
            listOf(
                BetterLyricsProvider,
                YouLyPlusLyricsProvider,
                LrcLibLyricsProvider,
                KuGouLyricsProvider,
                MegalobizLyricsProvider,
                SimpMusicLyricsProvider,
                UnisonLyricsProvider,
                PaxsenixAppleMusicLyricsProvider,
                PaxsenixNeteaseLyricsProvider,
                PaxsenixSpotifyLyricsProvider,
                PaxsenixMusixmatchLyricsProvider,
                PaxsenixYouTubeLyricsProvider,
                YouTubeSubtitleLyricsProvider,
                YouTubeLyricsProvider,
            )

        private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
        private val singleLyricsCache = LruCache<String, String>(MAX_CACHE_SIZE)

        suspend fun getLyrics(
            mediaMetadata: MediaMetadata,
            preferredProviderOnly: Boolean = false,
            forceRefresh: Boolean = false,
        ): String {
            val cacheKey = mediaMetadata.lyricsCacheKey
            if (forceRefresh) {
                invalidateCache(cacheKey)
            } else {
                singleLyricsCache.get(cacheKey)?.let { lyrics ->
                    GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
                    return lyrics
                }

                val cached = cache.get(cacheKey)?.firstOrNull()
                if (cached != null) {
                    GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
                    return cached.lyrics
                }
            }

            GlobalLog.append(
                Log.DEBUG,
                "LyricsHelper",
                "Fetching lyrics for ${mediaMetadata.title} (Artist: ${mediaMetadata.artists.joinToString {
                    it.name
                }}, Album: ${mediaMetadata.album?.title})",
            )

            val isNetworkAvailable =
                try {
                    networkConnectivity.isCurrentlyConnected()
                } catch (e: Exception) {
                    true
                }

            if (!isNetworkAvailable) {
                GlobalLog.append(Log.WARN, "LyricsHelper", "Network unavailable, aborting lyrics fetch")
                return LYRICS_NOT_FOUND
            }

            val ordered = orderedProviders().filter { it.isEnabled(context) }
            val providers = if (preferredProviderOnly) ordered.take(1) else ordered
            val lyrics = fetchPriorityLyrics(providers, mediaMetadata)
            if (isMeaningfulLyrics(lyrics)) {
                singleLyricsCache.put(cacheKey, lyrics)
            }

            return lyrics
        }

        suspend fun getAllLyrics(
            mediaId: String,
            songTitle: String,
            songArtists: String,
            songAlbum: String?,
            duration: Int,
            forceRefresh: Boolean = false,
            callback: (LyricsResult) -> Unit,
        ) {
            val cacheKey =
                lyricsCacheKey(
                    mediaId = mediaId,
                    title = songTitle,
                    artists = songArtists,
                    album = songAlbum,
                    duration = duration,
                )
            if (forceRefresh) {
                invalidateCache(cacheKey)
            } else {
                cache.get(cacheKey)?.let { results ->
                    results.forEach(callback)
                    return
                }
            }

            val isNetworkAvailable =
                try {
                    networkConnectivity.isCurrentlyConnected()
                } catch (e: Exception) {
                    true
                }

            if (!isNetworkAvailable) {
                return
            }

            val allResult = mutableListOf<LyricsResult>()
            val providers = orderedProviders()
            withContext(Dispatchers.IO) {
                providers.forEach { provider ->
                    if (!provider.isEnabled(context)) return@forEach

                    try {
                        provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) lyricsCallback@{ lyrics ->
                            val normalizedLyrics = LyricsUtils.lyricsOrNotFound(lyrics)
                            if (
                                normalizedLyrics == LYRICS_NOT_FOUND ||
                                    hasConflictingLrcMetadata(
                                        normalizedLyrics,
                                        title = songTitle,
                                        artists = songArtists,
                                    )
                            ) return@lyricsCallback
                            val result = LyricsResult(provider.name, normalizedLyrics)
                            allResult += result
                            callback(result)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        reportException(e)
                    }
                }
            }
            cache.put(cacheKey, allResult.toList())
        }

        private suspend fun fetchPriorityLyrics(
            providers: List<LyricsProvider>,
            mediaMetadata: MediaMetadata,
        ): String {
            if (providers.isEmpty()) return LYRICS_NOT_FOUND

            val artist = mediaMetadata.artists.joinToString { it.name }
            // ID-aware providers are less likely to return a same-title remix or live version.
            // Keep the user's ordering for the remaining providers.
            val exactIdProviders =
                listOf(
                    SimpMusicLyricsProvider,
                    UnisonLyricsProvider,
                    YouTubeSubtitleLyricsProvider,
                )
            val prioritizedProviders =
                if (mediaMetadata.id.isNotBlank()) {
                    providers.filter { it in exactIdProviders } + providers.filterNot { it in exactIdProviders }
                } else {
                    providers
                }
            val results =
                supervisorScope {
                    prioritizedProviders
                        .map { provider ->
                            async(Dispatchers.IO) {
                                fetchProviderLyrics(provider, mediaMetadata, artist)
                            }
                        }.mapNotNull { it.await() }
                }

            if (results.isEmpty()) return LYRICS_NOT_FOUND

            val syncedResults = results.filter { LyricsUtils.isLineSyncedLrc(it) }

if (syncedResults.isNotEmpty()) {
    return syncedResults.maxByOrNull {
        scoreSyncedLyrics(it, mediaMetadata.duration)
    } ?: syncedResults.first()
}

return results.first() 
        }
        private suspend fun fetchProviderLyrics(
            provider: LyricsProvider,
            mediaMetadata: MediaMetadata,
            artist: String,
        ): String? =
            try {
                provider
                    .getLyrics(
                        mediaMetadata.id,
                        mediaMetadata.title,
                        artist,
                        mediaMetadata.album?.title,
                        mediaMetadata.duration,
                    ).fold(
                        onSuccess = { lyrics ->
                            LyricsUtils
                                .lyricsOrNotFound(lyrics)
                                .takeIf {
                                    it != LYRICS_NOT_FOUND &&
                                        !hasConflictingLrcMetadata(
                                            lyrics = it,
                                            title = mediaMetadata.title,
                                            artists = artist,
                                        )
                                }
                        },
                        onFailure = {
                            reportException(it)
                            null
                        },
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                reportException(e)
                null
            }
        l

private suspend fun orderedProviders(): List<LyricsProvider> {
        private suspend fun orderedProviders(): List<LyricsProvider> {
            val orderStr = context.dataStore.data.first()[LyricsProviderOrderKey]
            val orderedEnums = deserializeLyricsProviderOrder(orderStr)
            val providerMap: Map<PreferredLyricsProvider, LyricsProvider> =
                mapOf(
                    PreferredLyricsProvider.LRCLIB to LrcLibLyricsProvider,
                    PreferredLyricsProvider.KUGOU to KuGouLyricsProvider,
                    PreferredLyricsProvider.MEGALOBIZ to MegalobizLyricsProvider,
                    PreferredLyricsProvider.BETTER_LYRICS to BetterLyricsProvider,
                    PreferredLyricsProvider.YOULY_PLUS to YouLyPlusLyricsProvider,
                    PreferredLyricsProvider.SIMPMUSIC to SimpMusicLyricsProvider,
                    PreferredLyricsProvider.PAXSENIX_APPLE_MUSIC to PaxsenixAppleMusicLyricsProvider,
                    PreferredLyricsProvider.PAXSENIX_NETEASE to PaxsenixNeteaseLyricsProvider,
                    PreferredLyricsProvider.PAXSENIX_SPOTIFY to PaxsenixSpotifyLyricsProvider,
                    PreferredLyricsProvider.PAXSENIX_MUSIXMATCH to PaxsenixMusixmatchLyricsProvider,
                    PreferredLyricsProvider.PAXSENIX_YOUTUBE to PaxsenixYouTubeLyricsProvider,
                    PreferredLyricsProvider.UNISON to UnisonLyricsProvider,
                )
            val userOrdered = orderedEnums.mapNotNull { providerMap[it] }
            val rest = baseProviders.filterNot { it in userOrdered }
            return userOrdered + rest
        }

        private fun isMeaningfulLyrics(lyrics: String): Boolean = LyricsUtils.hasMeaningfulLyricsContent(lyrics)

        fun clearCache() {
            cache.evictAll()
            singleLyricsCache.evictAll()
        }

        private fun invalidateCache(cacheKey: String) {
            cache.remove(cacheKey)
            singleLyricsCache.remove(cacheKey)
        }

        private val MediaMetadata.lyricsCacheKey: String
            get() =
                lyricsCacheKey(
                    mediaId = id,
                    title = title,
                    artists = artists.joinToString { it.name },
                    album = album?.title,
                    duration = duration,
                )

        private fun lyricsCacheKey(
            mediaId: String,
            title: String,
            artists: String,
            album: String?,
            duration: Int,
        ): String =
            listOf(mediaId, title, artists, album.orEmpty(), duration.toString())
                .joinToString("|") { normalizeIdentity(it) }

        private fun normalizeIdentity(value: String): String =
            value
                .lowercase()
                .replace("＆", "&")
                .replace(Regex("[^a-z0-9]+"), " ")
                .trim()
                .replace(Regex("\\s+"), " ")

        /** Reject only explicit provider metadata that contradicts the current track. */
        fun isLikelyForTrack(lyrics: String, metadata: MediaMetadata): Boolean =
            !hasConflictingLrcMetadata(
                lyrics,
                title = metadata.title,
                artists = metadata.artists.joinToString { it.name },
            )

        private fun hasConflictingLrcMetadata(
            lyrics: String,
            title: String,
            artists: String,
        ): Boolean {
            val titleTag = Regex("(?im)^\\s*\\[ti\\s*:\\s*([^]]+)]").find(lyrics)?.groupValues?.getOrNull(1)
            val artistTag = Regex("(?im)^\\s*\\[ar\\s*:\\s*([^]]+)]").find(lyrics)?.groupValues?.getOrNull(1)
            val normalizedTitle = normalizeIdentity(title)
            val normalizedArtists = artists.split(Regex("\\s*[,•;|/]\\s*"), limit = 0).map { normalizeIdentity(it) }
            val normalizedTaggedTitle = titleTag?.let(::normalizeIdentity)
            val normalizedTaggedArtist = artistTag?.let(::normalizeIdentity)
            fun matchesTitle(candidate: String): Boolean =
                normalizedTitle.isNotBlank() &&
                    candidate.isNotBlank() &&
                    (candidate == normalizedTitle ||
                        candidate.contains(normalizedTitle) ||
                        normalizedTitle.contains(candidate))
            fun matchesArtist(candidate: String): Boolean =
                normalizedArtists.any { expected ->
                    expected.isNotBlank() &&
                        candidate.isNotBlank() &&
                        (candidate == expected || candidate.contains(expected) || expected.contains(candidate))
                }

            val titleConflict =
                normalizedTaggedTitle != null &&
                    normalizedTaggedTitle.isNotBlank() &&
                    !matchesTitle(normalizedTaggedTitle)
            val artistConflict =
                normalizedTaggedArtist != null &&
                    normalizedTaggedArtist.isNotBlank() &&
                    normalizedArtists.isNotEmpty() &&
                    !matchesArtist(normalizedTaggedArtist)

            // Some providers return a plain-text header instead of [ti]/[ar] tags. For example:
            // "Janam Janam - Pritam/Arijit Singh/Antara Mitra". Treat that first-line title as
            // authoritative when its artist matches the current track; otherwise a valid result
            // for another song can be accepted for the currently playing track.
            val firstContentLine =
                lyrics.lineSequence()
                    .map { line ->
                        line.replace(
                            Regex("^\\s*(?:\\[\\d{1,2}:\\d{2}(?:[.:]\\d{1,3})?\\]\\s*)+"),
                            "",
                        ).trim()
                    }.firstOrNull { line -> line.isNotBlank() && !line.startsWith("[") }
            val plainHeader =
                firstContentLine?.let {
                    Regex("^(.{2,100}?)\\s+[-–—:]\\s+(.{2,160})$").matchEntire(it)
                }
            val plainHeaderTitle = plainHeader?.groupValues?.getOrNull(1)?.let(::normalizeIdentity)
            val plainHeaderArtist = plainHeader?.groupValues?.getOrNull(2)?.let(::normalizeIdentity)
            val plainHeaderConflict =
                plainHeaderTitle != null &&
                    plainHeaderArtist != null &&
                    !matchesTitle(plainHeaderTitle) &&
                    matchesArtist(plainHeaderArtist)

            return titleConflict || artistConflict || plainHeaderConflict
        }

        companion object {
            private const val MAX_CACHE_SIZE = 16
        }
    }

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
