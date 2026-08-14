/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.sync

import dev.vxs.frostsoulx.lyrics.core.LyricsDocument
import dev.vxs.frostsoulx.lyrics.core.LyricsFormat
import dev.vxs.frostsoulx.lyrics.core.LyricsLine
import dev.vxs.frostsoulx.lyrics.core.LyricsSyncStatus
import dev.vxs.frostsoulx.lyrics.core.LyricsTrack
import dev.vxs.frostsoulx.lyrics.core.LyricsWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.system.measureNanoTime

class LyricsSynchronizationEngineTest {
    @Test
    fun `binary lookup resolves current previous and next lines`() {
        val engine = LyricsSynchronizationEngine()
        engine.setDocument(document(lines = listOf(line(0L, 1_000L, "one"), line(1_000L, 2_000L, "two"), line(2_000L, 3_000L, "three"))))

        engine.update(playbackPositionMs = 1_500L, durationMs = 3_000L, isPlaying = true)
        val state = engine.state.value

        assertEquals(LyricsSyncStatus.Playing, state.status)
        assertEquals(1, state.currentLineIndex)
        assertEquals(0, state.previousLineIndex)
        assertEquals(2, state.nextLineIndex)
        assertEquals("two", state.currentLine?.text)
        assertEquals(0.5f, state.lineProgress, 0.001f)
    }

    @Test
    fun `word interpolation publishes active word and fractional progress`() {
        val engine = LyricsSynchronizationEngine()
        val line =
            LyricsLine(
                startMs = 1_000L,
                endMs = 3_000L,
                text = "dark cyan",
                words = listOf(LyricsWord("dark", 1_000L, 1_500L), LyricsWord("cyan", 1_500L, 2_500L)),
            )
        engine.setDocument(document(lines = listOf(line)))

        engine.update(playbackPositionMs = 2_000L, durationMs = 3_000L, isPlaying = true)
        val state = engine.state.value

        assertEquals(0, state.currentLineIndex)
        assertEquals(1, state.currentWordIndex)
        assertEquals(0.5f, state.wordProgress, 0.001f)
        assertEquals(0.5f, state.lineProgress, 0.001f)
    }

    @Test
    fun `line-only flow follows current line boundaries`() {
        val engine = LyricsSynchronizationEngine()
        engine.setDocument(document(lines = listOf(line(0L, 1_000L, "one"), line(1_000L, 2_000L, "two"))))

        engine.update(playbackPositionMs = 250L, durationMs = 2_000L, isPlaying = true)
        assertEquals("one", engine.currentLine.value?.text)

        engine.update(playbackPositionMs = 1_250L, durationMs = 2_000L, isPlaying = true)
        assertEquals("two", engine.currentLine.value?.text)
    }

    @Test
    fun `document offset is applied exactly once to synchronization timestamp`() {
        val engine = LyricsSynchronizationEngine()
        engine.setDocument(document(offsetMs = 250L, lines = listOf(line(1_000L, 2_000L, "offset"))))

        engine.update(playbackPositionMs = 750L, durationMs = 2_000L, isPlaying = true)
        val state = engine.state.value

        assertEquals(1_000L, state.timestampMs)
        assertEquals(0, state.currentLineIndex)
        assertEquals("offset", state.currentLine?.text)
    }

    @Test
    fun `paused update preserves resolved line while exposing ready status`() {
        val engine = LyricsSynchronizationEngine()
        engine.setDocument(document(lines = listOf(line(0L, 2_000L, "held"))))

        engine.update(playbackPositionMs = 1_000L, durationMs = 2_000L, isPlaying = false)

        assertEquals(LyricsSyncStatus.Ready, engine.state.value.status)
        assertEquals("held", engine.state.value.currentLine?.text)
    }

    @Test
    fun `binary synchronization stays below frame-safe budget for fifty thousand lines`() {
        val engine = LyricsSynchronizationEngine()
        val lines = List(50_000) { index -> line(index * 1_000L, (index + 1L) * 1_000L, "line $index") }
        engine.setDocument(document(lines = lines))

        val elapsedNanos =
            measureNanoTime {
                repeat(1_000) { index ->
                    engine.update(
                        playbackPositionMs = (index * 47L % 50_000L) * 1_000L + 500L,
                        durationMs = 50_000_000L,
                        isPlaying = true,
                    )
                }
            }

        assertTrue("1000 updates must remain comfortably below one second", elapsedNanos < 1_000_000_000L)
        assertEquals((999L * 47L % 50_000L).toInt(), engine.state.value.currentLineIndex)
    }

    private fun document(
        offsetMs: Long = 0L,
        lines: List<LyricsLine>,
    ): LyricsDocument =
        LyricsDocument(
            songId = "song",
            format = LyricsFormat.EnhancedLrc,
            original = LyricsTrack(lines = lines),
            offsetMs = offsetMs,
        )

    private fun line(
        startMs: Long,
        endMs: Long,
        text: String,
    ) = LyricsLine(startMs = startMs, endMs = endMs, text = text)
}
