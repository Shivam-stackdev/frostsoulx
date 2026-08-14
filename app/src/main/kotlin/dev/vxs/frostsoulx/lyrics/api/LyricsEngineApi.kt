/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.api

import androidx.compose.runtime.Immutable
import dev.vxs.frostsoulx.lyrics.core.LyricsDocument
import dev.vxs.frostsoulx.lyrics.core.LyricsTrack

/** Stable extension point for offline, remote, or future AI translation providers. */
interface LyricsTranslationProvider {
    val providerId: String
    val capabilities: LyricsProviderCapabilities

    suspend fun translate(request: LyricsTranslationRequest): LyricsTranslationOutcome
}

/** Stable extension point for timestamp repair and AI-assisted word-level synchronization. */
interface LyricsSynchronizationProvider {
    val providerId: String
    val capabilities: LyricsProviderCapabilities

    suspend fun synchronize(request: LyricsSynchronizationRequest): LyricsSynchronizationOutcome
}

@Immutable
data class LyricsProviderCapabilities(
    val supportsOffline: Boolean,
    val supportsTranslation: Boolean,
    val supportsRomanization: Boolean,
    val supportsLineTiming: Boolean,
    val supportsWordTiming: Boolean,
    val supportedSourceLanguages: Set<String> = emptySet(),
    val supportedTargetLanguages: Set<String> = emptySet(),
)

@Immutable
data class LyricsTranslationRequest(
    val songId: String,
    val source: LyricsDocument,
    val targetLanguageTag: String,
    val preserveTimestamps: Boolean = true,
    val includeRomanization: Boolean = false,
)

@Immutable
data class LyricsSynchronizationRequest(
    val songId: String,
    val unsynchronizedLyrics: LyricsDocument,
    val audioReference: LyricsAudioReference,
    val preferredWordTiming: Boolean = true,
    val maximumOffsetCorrectionMs: Long = 30_000L,
)

/**
 * A provider receives only a reference to an audio source, allowing implementations to use a local
 * URI, a content URI, or a privacy-preserving acoustic fingerprint without changing this API.
 */
@Immutable
data class LyricsAudioReference(
    val uri: String? = null,
    val acousticFingerprint: ByteArray? = null,
    val durationMs: Long? = null,
)

sealed interface LyricsTranslationOutcome {
    @Immutable
    data class Success(
        val translation: LyricsTrack,
        val romanization: LyricsTrack? = null,
        val confidence: Float,
        val providerId: String,
    ) : LyricsTranslationOutcome

    @Immutable
    data class UnsupportedLanguage(
        val languageTag: String,
        val providerId: String,
    ) : LyricsTranslationOutcome

    @Immutable
    data class Failure(
        val providerId: String,
        val retryable: Boolean,
        val message: String,
    ) : LyricsTranslationOutcome
}

sealed interface LyricsSynchronizationOutcome {
    @Immutable
    data class Success(
        val synchronizedDocument: LyricsDocument,
        val quality: LyricsSynchronizationQuality,
        val providerId: String,
    ) : LyricsSynchronizationOutcome

    @Immutable
    data class Failure(
        val providerId: String,
        val retryable: Boolean,
        val message: String,
    ) : LyricsSynchronizationOutcome
}

@Immutable
data class LyricsSynchronizationQuality(
    val lineConfidence: Float,
    val wordConfidence: Float? = null,
    val estimatedOffsetMs: Long = 0L,
    val requiresUserReview: Boolean = false,
) {
    init {
        require(lineConfidence in 0f..1f)
        require(wordConfidence == null || wordConfidence in 0f..1f)
    }
}

/** Provider-neutral policy for selecting an installed extension without coupling core playback to AI. */
interface LyricsProviderSelector {
    fun translationProvider(request: LyricsTranslationRequest): LyricsTranslationProvider?

    fun synchronizationProvider(request: LyricsSynchronizationRequest): LyricsSynchronizationProvider?
}
