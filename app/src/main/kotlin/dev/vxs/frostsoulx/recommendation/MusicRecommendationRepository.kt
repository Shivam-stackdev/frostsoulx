/*
 * FrostSoulX (2026)
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.vxs.frostsoulx.recommendation

import dev.vxs.frostsoulx.db.MusicDatabase
import dev.vxs.frostsoulx.db.entities.EventWithSong
import dev.vxs.frostsoulx.db.entities.Song
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Event tracker adapter for the existing `event` Room table. MusicService already
 * persists qualified playback events there, so this adapter avoids a second
 * listening-history database and caps every read before recommendation work starts.
 */
class RecommendationEventTracker {
    fun fromRoom(events: List<EventWithSong>): List<RecommendationEvent> =
        events.map { eventWithSong ->
            RecommendationEvent(
                songId = eventWithSong.event.songId,
                occurredAtEpochMs =
                    eventWithSong.event.timestamp
                        .atZone(ZoneOffset.UTC)
                        .toInstant()
                        .toEpochMilli(),
                listenedMs = eventWithSong.event.playTime.coerceAtLeast(0L),
                type = RecommendationEventType.PLAYBACK_COMPLETED,
            )
        }
}

/**
 * Public on-device Recommendation API. It reads a bounded Room snapshot,
 * delegates to the deterministic core, and does not call a remote model or
 * build an embedding cache.
 */
class MusicRecommendationRepository(
    private val database: MusicDatabase,
    private val limits: RecommendationLimits = RecommendationLimits(),
    private val engine: RecommendationEngine = RecommendationEngine(limits),
    private val eventTracker: RecommendationEventTracker = RecommendationEventTracker(),
) : RecommendationApi {
    override suspend fun recommendations(request: RecommendationRequest): RecommendationResponse =
            withSnapshot { songs, events ->
                engine.recommend(
                    songs = songs,
                    eventsNewestFirst = events,
                    request = request.copy(limit = request.limit.coerceIn(1, limits.rankedResultLimit)),
                )
            }

        override suspend fun smartQueue(
            seedSongId: String?,
            currentQueueSongIds: Set<String>,
            limit: Int,
        ): RecommendationResponse =
            recommendations(
                RecommendationRequest(
                    surface = RecommendationSurface.SMART_QUEUE,
                    limit = limit.coerceIn(1, limits.rankedResultLimit),
                    seedSongId = seedSongId,
                    currentQueueSongIds = currentQueueSongIds,
                ),
            )

        override suspend fun dailyMix(
            dayEpochMs: Long,
            limit: Int,
        ): DailyMix =
            withSnapshot { songs, events ->
                engine.dailyMix(
                    songs = songs,
                    eventsNewestFirst = events,
                    dayEpochMs = dayEpochMs,
                    limit = limit.coerceIn(1, limits.rankedResultLimit),
                )
            }

        override suspend fun discovery(limit: Int): RecommendationResponse =
            recommendations(
                RecommendationRequest(
                    surface = RecommendationSurface.DISCOVERY,
                    limit = limit.coerceIn(1, limits.rankedResultLimit),
                ),
            )

        private suspend fun <T> withSnapshot(
            block: (songs: List<RecommendationSong>, events: List<RecommendationEvent>) -> T,
        ): T =
            withContext(Dispatchers.IO) {
                val songs =
                    database
                        .recommendationSongs(limits.libraryCandidateLimit)
                        .first()
                        .map { song -> song.toRecommendationSong() }
                val events =
                    eventTracker.fromRoom(
                        database.events(
                            limit = limits.historyLimit,
                            offset = 0,
                        ),
                    )
                block(songs, events)
            }

        private fun Song.toRecommendationSong(): RecommendationSong =
            RecommendationSong(
                id = id,
                title = title,
                artistIds = artists.map { it.id },
                albumId = album?.id ?: song.albumId,
                year = song.year,
                durationMs = song.duration.coerceAtLeast(0).toLong() * 1_000L,
                liked = song.liked,
                isInLibrary = song.inLibrary != null,
                isMusicVideo = song.isMusicVideo,
            )
    }
