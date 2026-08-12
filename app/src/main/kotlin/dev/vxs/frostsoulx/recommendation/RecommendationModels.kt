/*
 * FrostSoulX (2026)
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.vxs.frostsoulx.recommendation

/**
 * The recommendation system uses compact, explainable signals rather than an
 * in-memory embedding index. This keeps the feature usable on low-memory devices.
 */
data class RecommendationLimits(
    val historyLimit: Int = 500,
    val libraryCandidateLimit: Int = 1_200,
    val generatedCandidateLimit: Int = 320,
    val rankedResultLimit: Int = 100,
    val topAffinityLimit: Int = 24,
    val recentExclusionLimit: Int = 40,
) {
    init {
        require(historyLimit > 0)
        require(libraryCandidateLimit > 0)
        require(generatedCandidateLimit > 0)
        require(rankedResultLimit > 0)
        require(topAffinityLimit > 0)
        require(recentExclusionLimit > 0)
    }
}

enum class RecommendationEventType {
    PLAYBACK_COMPLETED,
    PLAYBACK_SKIPPED,
    SONG_LIKED,
    SONG_QUEUED,
    KARAOKE_COMPLETED,
}

data class RecommendationEvent(
    val songId: String,
    val occurredAtEpochMs: Long,
    val listenedMs: Long,
    val type: RecommendationEventType = RecommendationEventType.PLAYBACK_COMPLETED,
)

/** A compact song representation used by the on-device engine. */
data class RecommendationSong(
    val id: String,
    val title: String,
    val artistIds: List<String>,
    val albumId: String?,
    val year: Int?,
    val durationMs: Long,
    val liked: Boolean,
    val isInLibrary: Boolean,
    val isMusicVideo: Boolean,
)

data class SongFeatures(
    val songId: String,
    val artistIds: Set<String>,
    val albumId: String?,
    val titleTokens: Set<String>,
    val decade: Int?,
    val durationBucket: Int,
)

data class UserProfile(
    val artistAffinities: Map<String, Double>,
    val albumAffinities: Map<String, Double>,
    val tokenAffinities: Map<String, Double>,
    val recentSongIds: Set<String>,
    val skippedSongIds: Set<String>,
    val positiveHistorySize: Int,
)

enum class CandidateSource {
    ARTIST_AFFINITY,
    ALBUM_AFFINITY,
    TITLE_SIMILARITY,
    LIBRARY_FALLBACK,
}

data class RecommendationCandidate(
    val song: RecommendationSong,
    val features: SongFeatures,
    val sources: Set<CandidateSource>,
    val baseScore: Double,
)

data class RankedRecommendation(
    val song: RecommendationSong,
    val score: Double,
    val reasons: List<String>,
)

enum class RecommendationSurface {
    HOME,
    SMART_QUEUE,
    DAILY_MIX,
    DISCOVERY,
}

data class RecommendationRequest(
    val surface: RecommendationSurface = RecommendationSurface.HOME,
    val limit: Int = 30,
    val seedSongId: String? = null,
    val excludedSongIds: Set<String> = emptySet(),
    val currentQueueSongIds: Set<String> = emptySet(),
    val nowEpochMs: Long = System.currentTimeMillis(),
) {
    init {
        require(limit in 1..100)
    }
}

data class RecommendationResponse(
    val surface: RecommendationSurface,
    val profileHistorySize: Int,
    val candidatesConsidered: Int,
    val recommendations: List<RankedRecommendation>,
)

data class DailyMix(
    val id: String,
    val title: String,
    val description: String,
    val tracks: List<RankedRecommendation>,
)

interface RecommendationApi {
    suspend fun recommendations(request: RecommendationRequest = RecommendationRequest()): RecommendationResponse

    suspend fun smartQueue(
        seedSongId: String?,
        currentQueueSongIds: Set<String>,
        limit: Int = 20,
    ): RecommendationResponse

    suspend fun dailyMix(
        dayEpochMs: Long = System.currentTimeMillis(),
        limit: Int = 30,
    ): DailyMix

    suspend fun discovery(limit: Int = 30): RecommendationResponse
}
