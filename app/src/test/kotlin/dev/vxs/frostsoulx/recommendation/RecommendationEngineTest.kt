package dev.vxs.frostsoulx.recommendation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private val now = 1_730_000_000_000L

    @Test
    fun `profile keeps only bounded strongest affinities`() {
        val limits = RecommendationLimits(topAffinityLimit = 2)
        val engine = RecommendationEngine(limits)
        val songs = listOf(song("one", "a"), song("two", "b"), song("three", "c"))
        val profile =
            engine.buildProfile(
                eventsNewestFirst =
                    listOf(
                        event("one", now, RecommendationEventType.SONG_LIKED),
                        event("two", now - 1_000L, RecommendationEventType.PLAYBACK_COMPLETED),
                        event("three", now - 2_000L, RecommendationEventType.PLAYBACK_COMPLETED),
                    ),
                songsById = songs.associateBy { it.id },
                nowEpochMs = now,
            )

        assertEquals(2, profile.artistAffinities.size)
        assertTrue("a" in profile.artistAffinities)
    }

    @Test
    fun `smart queue excludes queued recent and skipped songs`() {
        val engine = RecommendationEngine()
        val songs =
            listOf(
                song("seed", "favorite"),
                song("queued", "favorite"),
                song("skipped", "other"),
                song("available", "new"),
            )
        val response =
            engine.recommend(
                songs = songs,
                eventsNewestFirst =
                    listOf(
                        event("seed", now, RecommendationEventType.PLAYBACK_COMPLETED),
                        event("skipped", now - 1_000L, RecommendationEventType.PLAYBACK_SKIPPED),
                    ),
                request =
                    RecommendationRequest(
                        surface = RecommendationSurface.SMART_QUEUE,
                        limit = 10,
                        seedSongId = "seed",
                        currentQueueSongIds = setOf("queued"),
                        nowEpochMs = now,
                    ),
            )

        val resultIds = response.recommendations.map { it.song.id }
        assertFalse("seed" in resultIds)
        assertFalse("queued" in resultIds)
        assertFalse("skipped" in resultIds)
        assertTrue("available" in resultIds)
    }

    @Test
    fun `daily mix is stable for the same day`() {
        val engine = RecommendationEngine()
        val songs = listOf(song("one", "a"), song("two", "b"), song("three", "c"))
        val events = listOf(event("one", now, RecommendationEventType.PLAYBACK_COMPLETED))

        val first = engine.dailyMix(songs, events, now, limit = 10)
        val second = engine.dailyMix(songs, events, now, limit = 10)

        assertEquals(first.id, second.id)
        assertEquals(first.tracks.map { it.song.id }, second.tracks.map { it.song.id })
    }

    private fun song(
        id: String,
        artistId: String,
        liked: Boolean = false,
    ) =
        RecommendationSong(
            id = id,
            title = "Song $id",
            artistIds = listOf(artistId),
            albumId = null,
            year = 2024,
            durationMs = 180_000L,
            liked = liked,
            isInLibrary = true,
            isMusicVideo = false,
        )

    private fun event(
        songId: String,
        occurredAt: Long,
        type: RecommendationEventType,
    ) =
        RecommendationEvent(
            songId = songId,
            occurredAtEpochMs = occurredAt,
            listenedMs = 180_000L,
            type = type,
        )
}
