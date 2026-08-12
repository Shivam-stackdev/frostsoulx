/*
 * FrostSoulX (2026)
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package dev.vxs.frostsoulx.recommendation

import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.exp
import kotlin.math.min

/**
 * Stateless content-based recommendation pipeline. It deliberately keeps only
 * bounded maps and lists in memory; all long-term listening data stays in Room.
 */
class RecommendationEngine(
    private val limits: RecommendationLimits = RecommendationLimits(),
) {
    fun extract(song: RecommendationSong): SongFeatures =
        SongFeatures(
            songId = song.id,
            artistIds = song.artistIds.filter(String::isNotBlank).toSet(),
            albumId = song.albumId?.takeIf(String::isNotBlank),
            titleTokens = tokenize(song.title),
            decade = song.year?.takeIf { it in 1900..2100 }?.let { it / 10 * 10 },
            durationBucket = (song.durationMs.coerceAtLeast(0L) / DurationBucketMs).toInt(),
        )

    fun buildProfile(
        eventsNewestFirst: List<RecommendationEvent>,
        songsById: Map<String, RecommendationSong>,
        nowEpochMs: Long,
    ): UserProfile {
        val artistScores = mutableMapOf<String, Double>()
        val albumScores = mutableMapOf<String, Double>()
        val tokenScores = mutableMapOf<String, Double>()
        val recentSongIds = linkedSetOf<String>()
        val skippedSongIds = linkedSetOf<String>()
        var positiveHistorySize = 0

        eventsNewestFirst.take(limits.historyLimit).forEachIndexed { position, event ->
            val song = songsById[event.songId] ?: return@forEachIndexed
            val feature = extract(song)
            val recency = recencyWeight(event.occurredAtEpochMs, nowEpochMs, position)
            val signal = eventSignal(event)
            if (signal > 0.0) positiveHistorySize++
            if (event.type == RecommendationEventType.PLAYBACK_SKIPPED) {
                skippedSongIds += event.songId
            }
            if (recentSongIds.size < limits.recentExclusionLimit) {
                recentSongIds += event.songId
            }

            val weightedSignal = signal * recency
            feature.artistIds.forEach { artistId -> artistScores.add(artistId, weightedSignal) }
            feature.albumId?.let { albumId -> albumScores.add(albumId, weightedSignal) }
            feature.titleTokens.forEach { token -> tokenScores.add(token, weightedSignal * TokenWeight) }
        }

        return UserProfile(
            artistAffinities = artistScores.topPositive(limits.topAffinityLimit),
            albumAffinities = albumScores.topPositive(limits.topAffinityLimit),
            tokenAffinities = tokenScores.topPositive(limits.topAffinityLimit),
            recentSongIds = recentSongIds,
            skippedSongIds = skippedSongIds,
            positiveHistorySize = positiveHistorySize,
        )
    }

    fun recommend(
        songs: List<RecommendationSong>,
        eventsNewestFirst: List<RecommendationEvent>,
        request: RecommendationRequest,
    ): RecommendationResponse {
        val boundedSongs = songs
            .asSequence()
            .filter { it.isInLibrary && !it.isMusicVideo }
            .take(limits.libraryCandidateLimit)
            .toList()
        val songsById = boundedSongs.associateBy { it.id }
        val profile = buildProfile(eventsNewestFirst, songsById, request.nowEpochMs)
        val seedFeature = request.seedSongId?.let(songsById::get)?.let(::extract)
        val candidates = generateCandidates(boundedSongs, profile, request, seedFeature)
        val ranked = rank(candidates, profile, request).take(request.limit)
        return RecommendationResponse(
            surface = request.surface,
            profileHistorySize = profile.positiveHistorySize,
            candidatesConsidered = candidates.size,
            recommendations = ranked,
        )
    }

    fun dailyMix(
        songs: List<RecommendationSong>,
        eventsNewestFirst: List<RecommendationEvent>,
        dayEpochMs: Long,
        limit: Int,
    ): DailyMix {
        val day = Instant.ofEpochMilli(dayEpochMs).atZone(ZoneOffset.UTC).toLocalDate()
        val response =
            recommend(
                songs = songs,
                eventsNewestFirst = eventsNewestFirst,
                request =
                    RecommendationRequest(
                        surface = RecommendationSurface.DAILY_MIX,
                        limit = limit,
                        nowEpochMs = dayEpochMs,
                    ),
            )
        val daySalt = day.toEpochDay().toInt()
        val rotatedTracks = response.recommendations.rotateDeterministically(daySalt)
        return DailyMix(
            id = "daily-${day}",
            title = "Daily Mix",
            description = "A fresh on-device mix based on your recent listening.",
            tracks = rotatedTracks,
        )
    }

    private fun generateCandidates(
        songs: List<RecommendationSong>,
        profile: UserProfile,
        request: RecommendationRequest,
        seedFeature: SongFeatures?,
    ): List<RecommendationCandidate> {
        val excluded = buildSet {
            addAll(request.excludedSongIds)
            addAll(request.currentQueueSongIds)
            addAll(profile.skippedSongIds)
            if (request.surface != RecommendationSurface.DAILY_MIX) addAll(profile.recentSongIds)
        }
        val candidates = ArrayList<RecommendationCandidate>(limits.generatedCandidateLimit)

        for (song in songs) {
            if (song.id in excluded) continue
            val feature = extract(song)
            val sources = linkedSetOf<CandidateSource>()
            var score = if (song.liked) LikedSongBoost else 0.0

            val artistAffinity = feature.artistIds.sumOf { profile.artistAffinities[it] ?: 0.0 }
            if (artistAffinity > 0.0) {
                sources += CandidateSource.ARTIST_AFFINITY
                score += artistAffinity * ArtistAffinityWeight
            }

            val albumAffinity = feature.albumId?.let { profile.albumAffinities[it] ?: 0.0 } ?: 0.0
            if (albumAffinity > 0.0) {
                sources += CandidateSource.ALBUM_AFFINITY
                score += albumAffinity * AlbumAffinityWeight
            }

            val tokenAffinity = feature.titleTokens.sumOf { profile.tokenAffinities[it] ?: 0.0 }
            if (tokenAffinity > 0.0) {
                sources += CandidateSource.TITLE_SIMILARITY
                score += tokenAffinity * TitleAffinityWeight
            }

            if (seedFeature != null) {
                val seedSimilarity = featureSimilarity(feature, seedFeature)
                if (seedSimilarity > 0.0) {
                    sources += CandidateSource.TITLE_SIMILARITY
                    score += seedSimilarity * SeedSimilarityWeight
                }
            }

            if (sources.isEmpty()) {
                sources += CandidateSource.LIBRARY_FALLBACK
                score += if (song.liked) 0.20 else 0.05
            }
            candidates += RecommendationCandidate(song, feature, sources, score)
        }

        return candidates
            .sortedWith(compareByDescending<RecommendationCandidate> { it.baseScore }.thenBy { it.song.id })
            .take(limits.generatedCandidateLimit)
    }

    private fun rank(
        candidates: List<RecommendationCandidate>,
        profile: UserProfile,
        request: RecommendationRequest,
    ): List<RankedRecommendation> {
        val artistOccurrences = mutableMapOf<String, Int>()
        return candidates
            .asSequence()
            .map { candidate ->
                val repetition = candidate.features.artistIds.sumOf { artistOccurrences[it] ?: 0 }
                val diversityPenalty = min(MaxDiversityPenalty, repetition * DiversityPenaltyPerArtist)
                val noveltyBoost = if (candidate.song.id !in profile.recentSongIds) NoveltyBoost else 0.0
                val discoveryBoost =
                    if (request.surface == RecommendationSurface.DISCOVERY &&
                        candidate.sources == setOf(CandidateSource.LIBRARY_FALLBACK)
                    ) {
                        DiscoveryFallbackBoost
                    } else {
                        0.0
                    }
                candidate to (candidate.baseScore + noveltyBoost + discoveryBoost - diversityPenalty)
            }.sortedWith(compareByDescending<Pair<RecommendationCandidate, Double>> { it.second }.thenBy { it.first.song.id })
            .take(limits.rankedResultLimit)
            .map { (candidate, score) ->
                candidate.features.artistIds.forEach { artistId -> artistOccurrences.add(artistId, 1) }
                RankedRecommendation(
                    song = candidate.song,
                    score = score.coerceAtLeast(0.0),
                    reasons = reasonsFor(candidate, profile, request),
                )
            }.toList()
    }

    private fun reasonsFor(
        candidate: RecommendationCandidate,
        profile: UserProfile,
        request: RecommendationRequest,
    ): List<String> =
        buildList(3) {
            if (CandidateSource.ARTIST_AFFINITY in candidate.sources) add("Matches artists you play often")
            if (CandidateSource.ALBUM_AFFINITY in candidate.sources) add("Connects with albums in your history")
            if (CandidateSource.TITLE_SIMILARITY in candidate.sources) add("Similar to your listening patterns")
            if (request.surface == RecommendationSurface.DISCOVERY &&
                candidate.song.id !in profile.recentSongIds
            ) {
                add("Fresh library discovery")
            }
            if (isEmpty()) add("From your music library")
        }

    private fun featureSimilarity(left: SongFeatures, right: SongFeatures): Double {
        var score = 0.0
        if (left.artistIds.intersect(right.artistIds).isNotEmpty()) score += 1.0
        if (left.albumId != null && left.albumId == right.albumId) score += 0.75
        if (left.titleTokens.intersect(right.titleTokens).isNotEmpty()) score += 0.35
        if (left.decade != null && left.decade == right.decade) score += 0.20
        if (left.durationBucket == right.durationBucket) score += 0.10
        return score
    }

    private fun eventSignal(event: RecommendationEvent): Double =
        when (event.type) {
            RecommendationEventType.PLAYBACK_COMPLETED -> 0.55 + min(0.45, event.listenedMs / FullListenMs.toDouble())
            RecommendationEventType.KARAOKE_COMPLETED -> 1.20
            RecommendationEventType.SONG_LIKED -> 1.35
            RecommendationEventType.SONG_QUEUED -> 0.15
            RecommendationEventType.PLAYBACK_SKIPPED -> -1.00
        }

    private fun recencyWeight(
        occurredAtEpochMs: Long,
        nowEpochMs: Long,
        fallbackPosition: Int,
    ): Double {
        val ageDays = ((nowEpochMs - occurredAtEpochMs).coerceAtLeast(0L) / MillisPerDay).toDouble()
        val timeDecay = exp(-ageDays / RecencyHalfLifeDays)
        val positionDecay = 1.0 / (1.0 + fallbackPosition * 0.04)
        return timeDecay * positionDecay
    }

    private fun tokenize(value: String): Set<String> =
        value
            .lowercase()
            .split(TokenSeparator)
            .asSequence()
            .map(String::trim)
            .filter { it.length >= 2 }
            .take(MaxTokensPerTitle)
            .toSet()

    private fun MutableMap<String, Double>.add(key: String, value: Double) {
        this[key] = (this[key] ?: 0.0) + value
    }

    private fun Map<String, Double>.topPositive(limit: Int): Map<String, Double> =
        entries
            .asSequence()
            .filter { it.value > 0.0 }
            .sortedByDescending { it.value }
            .take(limit)
            .associate { it.key to it.value }

    private fun List<RankedRecommendation>.rotateDeterministically(salt: Int): List<RankedRecommendation> {
        if (size < 2) return this
        val shift = (salt and Int.MAX_VALUE) % size
        return drop(shift) + take(shift)
    }

    private companion object {
        const val DurationBucketMs = 60_000L
        const val FullListenMs = 240_000L
        const val MillisPerDay = 86_400_000L
        const val RecencyHalfLifeDays = 21.0
        const val TokenWeight = 0.20
        const val ArtistAffinityWeight = 1.00
        const val AlbumAffinityWeight = 0.65
        const val TitleAffinityWeight = 0.35
        const val SeedSimilarityWeight = 0.80
        const val LikedSongBoost = 0.25
        const val NoveltyBoost = 0.08
        const val DiscoveryFallbackBoost = 0.16
        const val DiversityPenaltyPerArtist = 0.16
        const val MaxDiversityPenalty = 0.48
        const val MaxTokensPerTitle = 12
        val TokenSeparator = Regex("[^\\p{L}\\p{Nd}]+")
    }
}
