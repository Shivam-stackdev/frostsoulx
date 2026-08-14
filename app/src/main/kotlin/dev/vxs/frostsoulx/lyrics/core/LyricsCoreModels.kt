/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.core

import androidx.compose.runtime.Immutable

@Immutable
data class LyricsWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val isBackground: Boolean = false,
) {
    init {
        require(startMs >= 0L)
        require(endMs >= startMs)
    }
}

@Immutable
data class LyricsLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<LyricsWord> = emptyList(),
    val isInstrumental: Boolean = false,
    val translation: String? = null,
    val romanization: String? = null,
) {
    init {
        require(startMs >= 0L)
        require(endMs >= startMs)
    }
}

enum class LyricsFormat {
    Plain,
    Lrc,
    EnhancedLrc,
    Yrc,
    Ttml,
}

@Immutable
data class LyricsTrack(
    val language: String? = null,
    val lines: List<LyricsLine>,
)

@Immutable
data class LyricsDocument(
    val songId: String,
    val format: LyricsFormat,
    val original: LyricsTrack,
    val translation: LyricsTrack? = null,
    val romanization: LyricsTrack? = null,
    val offsetMs: Long = 0L,
    val source: String? = null,
    val artworkKey: String? = null,
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    val hasTimedLines: Boolean
        get() = original.lines.any { it.startMs > 0L || it.endMs > 0L }

    val hasWordTiming: Boolean
        get() = original.lines.any { it.words.isNotEmpty() }

    fun withOffset(offsetMs: Long): LyricsDocument = copy(offsetMs = offsetMs.coerceIn(-30_000L, 30_000L))
}

enum class LyricsSyncStatus {
    Empty,
    Ready,
    Seeking,
    Playing,
    Ended,
}

@Immutable
data class LyricsSyncState(
    val status: LyricsSyncStatus = LyricsSyncStatus.Empty,
    val timestampMs: Long = 0L,
    val playbackProgress: Float = 0f,
    val currentLineIndex: Int = -1,
    val previousLineIndex: Int = -1,
    val nextLineIndex: Int = -1,
    val currentWordIndex: Int = -1,
    val lineProgress: Float = 0f,
    val wordProgress: Float = 0f,
    val currentLine: LyricsLine? = null,
    val previousLine: LyricsLine? = null,
    val nextLine: LyricsLine? = null,
)

@Immutable
data class LyricsSearchRequest(
    val songId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
)

enum class LyricsDownloadResult {
    Downloaded,
    AlreadyCached,
    NotFound,
    RetryScheduled,
}
