/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.sync

import dev.vxs.frostsoulx.lyrics.core.LyricsDocument
import dev.vxs.frostsoulx.lyrics.core.LyricsLine
import dev.vxs.frostsoulx.lyrics.core.LyricsSyncState
import dev.vxs.frostsoulx.lyrics.core.LyricsSyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsSynchronizationEngine @Inject constructor() {
    private val _state = MutableStateFlow(LyricsSyncState())
    private val _document = MutableStateFlow<LyricsDocument?>(null)
    private var document: LyricsDocument? = null

    val state: StateFlow<LyricsSyncState> = _state.asStateFlow()
    val documentState: StateFlow<LyricsDocument?> = _document.asStateFlow()

    fun setDocument(document: LyricsDocument?) {
        this.document = document
        _document.value = document
        _state.value = if (document == null || document.original.lines.isEmpty()) LyricsSyncState() else resolve(0L, 0L, false, false)
    }

    fun setOffset(offsetMs: Long) {
        val current = document ?: return
        document = current.withOffset(offsetMs)
        _document.value = document
        val state = _state.value
        _state.value = resolve(state.timestampMs, durationMs = 0L, isPlaying = state.status == LyricsSyncStatus.Playing, isSeeking = false)
    }

    fun update(
        playbackPositionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        isSeeking: Boolean = false,
    ) {
        if (document == null) {
            _state.value = LyricsSyncState(timestampMs = playbackPositionMs.coerceAtLeast(0L))
            return
        }
        _state.value = resolve(playbackPositionMs, durationMs, isPlaying, isSeeking)
    }

    private fun resolve(
        playbackPositionMs: Long,
        durationMs: Long,
        isPlaying: Boolean,
        isSeeking: Boolean,
    ): LyricsSyncState {
        val source = document ?: return LyricsSyncState()
        val lines = source.original.lines
        if (lines.isEmpty()) return LyricsSyncState(timestampMs = playbackPositionMs.coerceAtLeast(0L))
        val timestamp = (playbackPositionMs + source.offsetMs).coerceAtLeast(0L)
        val lineIndex = lines.floorIndex(timestamp)
        val current = lines.getOrNull(lineIndex)
        val previous = lines.getOrNull(lineIndex - 1)
        val next = lines.getOrNull(lineIndex + 1)
        val lineEnd = current?.endMs?.takeIf { it > current.startMs } ?: next?.startMs ?: (current?.startMs ?: timestamp) + DefaultLineDurationMs
        val lineProgress = current?.let { ((timestamp - it.startMs).toFloat() / (lineEnd - it.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f) } ?: 0f
        val wordIndex = current?.words?.floorIndex(timestamp) ?: -1
        val word = current?.words?.getOrNull(wordIndex)
        val wordProgress =
            word?.let {
                ((timestamp - it.startMs).toFloat() / (it.endMs - it.startMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
            } ?: lineProgress
        return LyricsSyncState(
            status =
                when {
                    isSeeking -> LyricsSyncStatus.Seeking
                    !isPlaying -> LyricsSyncStatus.Ready
                    lineIndex >= lines.lastIndex && timestamp >= lineEnd -> LyricsSyncStatus.Ended
                    else -> LyricsSyncStatus.Playing
                },
            timestampMs = timestamp,
            playbackProgress = if (durationMs > 0L) (playbackPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
            currentLineIndex = lineIndex,
            previousLineIndex = (lineIndex - 1).takeIf { it in lines.indices } ?: -1,
            nextLineIndex = (lineIndex + 1).takeIf { it in lines.indices } ?: -1,
            currentWordIndex = wordIndex,
            lineProgress = lineProgress,
            wordProgress = wordProgress,
            currentLine = current,
            previousLine = previous,
            nextLine = next,
        )
    }

    private fun List<LyricsLine>.floorIndex(timestampMs: Long): Int {
        var low = 0
        var high = lastIndex
        var result = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (this[middle].startMs <= timestampMs) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return result
    }

    private fun List<dev.vxs.frostsoulx.lyrics.core.LyricsWord>.floorIndex(timestampMs: Long): Int {
        var low = 0
        var high = lastIndex
        var result = -1
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (this[middle].startMs <= timestampMs) {
                result = middle
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return result
    }

    private companion object {
        const val DefaultLineDurationMs = 3_500L
    }
}
