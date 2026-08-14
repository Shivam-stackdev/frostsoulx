/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.parser

import dev.vxs.frostsoulx.lyrics.core.LyricsFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsParserTest {
    @Test
    fun `lrc preserves millisecond line timestamps`() {
        val document =
            LyricsParser.parse(
                songId = "song",
                originalText = "[00:01.250]First line\n[00:03.005]Second line",
            )

        assertEquals(LyricsFormat.Lrc, document.format)
        assertEquals(1_250L, document.original.lines[0].startMs)
        assertEquals(3_005L, document.original.lines[1].startMs)
        assertEquals(3_005L, document.original.lines[0].endMs)
    }

    @Test
    fun `enhanced lrc exposes word-by-word timing`() {
        val document =
            LyricsParser.parse(
                songId = "song",
                originalText = "[00:01.000]<00:01.000>Hello <00:01.500>world",
            )
        val line = document.original.lines.single()

        assertEquals(LyricsFormat.EnhancedLrc, document.format)
        assertEquals("Hello world", line.text)
        assertEquals(2, line.words.size)
        assertEquals(1_000L, line.words[0].startMs)
        assertEquals(1_500L, line.words[1].startMs)
        assertTrue(line.words[0].endMs >= line.words[0].startMs)
    }

    @Test
    fun `yrc parses line duration and per-word timestamps`() {
        val document =
            LyricsParser.parse(
                songId = "song",
                originalText = "[1000,1500](1000,500,0)Hello(1500,500,0)world",
            )
        val line = document.original.lines.single()

        assertEquals(LyricsFormat.Yrc, document.format)
        assertEquals(1_000L, line.startMs)
        assertEquals(2_500L, line.endMs)
        assertEquals("Helloworld", line.text)
        assertEquals(1_000L, line.words[0].startMs)
        assertEquals(1_500L, line.words[0].endMs)
    }

    @Test
    fun `ttml parses paragraph and word timings`() {
        val document =
            LyricsParser.parse(
                songId = "song",
                originalText =
                    """
                    <tt><body><div>
                    <p begin="1.2s" end="3.4s"><span begin="1.2s" end="2.0s">Blue</span><span begin="2.0s" end="3.4s"> sky</span></p>
                    </div></body></tt>
                    """.trimIndent(),
            )
        val line = document.original.lines.single()

        assertEquals(LyricsFormat.Ttml, document.format)
        assertEquals(1_200L, line.startMs)
        assertEquals(3_400L, line.endMs)
        assertEquals("Blue sky", line.text)
        assertEquals(2, line.words.size)
    }

    @Test
    fun `lrc offset remains document metadata and does not mutate parsed timestamps`() {
        val document =
            LyricsParser.parse(
                songId = "song",
                originalText = "[offset:+250]\n[00:01.000]Offset line",
            )

        assertEquals(250L, document.offsetMs)
        assertEquals(1_000L, document.original.lines.single().startMs)
    }

    @Test
    fun `translation and romanization align with original timed lines`() {
        val document =
            LyricsParser.parse(
                songId = "song",
                originalText = "[00:01.000]こんにちは\n[00:03.000]世界",
                translationText = "[00:01.100]Hello\n[00:03.100]World",
                romanizationText = "[00:01.050]Konnichiwa\n[00:03.050]Sekai",
            )

        assertEquals("Hello", document.original.lines[0].translation)
        assertEquals("Konnichiwa", document.original.lines[0].romanization)
        assertEquals("World", document.original.lines[1].translation)
        assertFalse(document.hasWordTiming)
    }
}
