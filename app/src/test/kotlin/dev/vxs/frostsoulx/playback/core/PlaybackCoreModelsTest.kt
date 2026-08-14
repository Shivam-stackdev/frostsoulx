/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.playback.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCoreModelsTest {
    @Test
    fun `insert next keeps current item selected`() {
        val result =
            QueueMutationPlanner.insert(
                items = listOf("a", "b", "c"),
                currentIndex = 1,
                incoming = listOf("x", "y"),
                mode = QueueInsertionMode.Next,
            )

        assertEquals(listOf("a", "b", "x", "y", "c"), result.items)
        assertEquals(1, result.currentIndex)
        assertTrue(result.changed)
        assertEquals(QueueMutationReason.InsertNext, result.undoToken?.reason)
    }

    @Test
    fun `inserting into an empty queue selects the first new item`() {
        val result =
            QueueMutationPlanner.insert(
                items = emptyList<String>(),
                currentIndex = -1,
                incoming = listOf("first"),
                mode = QueueInsertionMode.Next,
            )

        assertEquals(listOf("first"), result.items)
        assertEquals(0, result.currentIndex)
    }

    @Test
    fun `multi delete produces an undo snapshot with original queue`() {
        val result =
            QueueMutationPlanner.remove(
                items = listOf("a", "b", "c", "d", "e"),
                currentIndex = 3,
                indices = listOf(1, 3),
            )

        assertEquals(listOf("a", "c", "e"), result.items)
        assertEquals(2, result.currentIndex)
        val restored = QueueMutationPlanner.undo(requireNotNull(result.undoToken))
        assertEquals(listOf("a", "b", "c", "d", "e"), restored.items)
        assertEquals(3, restored.currentIndex)
    }

    @Test
    fun `move adjusts current selection when another item crosses it`() {
        val result =
            QueueMutationPlanner.move(
                items = listOf("a", "b", "c", "d"),
                currentIndex = 2,
                fromIndex = 0,
                toIndex = 3,
            )

        assertEquals(listOf("b", "c", "d", "a"), result.items)
        assertEquals(1, result.currentIndex)
    }

    @Test
    fun `smart queue only appends unseen valid identifiers within limit`() {
        val additions =
            SmartQueuePlanner.appendDistinct(
                current = listOf("a", "b"),
                candidates = listOf("", "b", "c", "c", "d", "e"),
                maxAdditionalItems = 2,
                keyOf = { it },
            )

        assertEquals(listOf("c", "d"), additions)
    }

    @Test
    fun `shuffle order is deterministic and starts with current item`() {
        val first = PlaybackShuffleEngine.currentFirstOrder(itemCount = 6, currentIndex = 4, seed = 91L)
        val second = PlaybackShuffleEngine.currentFirstOrder(itemCount = 6, currentIndex = 4, seed = 91L)

        assertArrayEquals(first, second)
        assertEquals(4, first.first())
        assertEquals((0 until 6).toSet(), first.toSet())
    }

    @Test
    fun `replay gain clamps at peak safe volume`() {
        val multiplier = ReplayGainPolicy.gainMultiplier(trackGainDb = 12f, preampDb = 0f, peak = 2f)

        assertEquals(0.5f, multiplier, 0.0001f)
        assertFalse(multiplier > 0.5f)
    }
}
