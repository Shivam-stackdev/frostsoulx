/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package dev.vxs.frostsoulx.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

private const val LyricsMinZoom = 0.75f
private const val LyricsMaxZoom = 1.80f

/**
 * Hosts an expanded lyric page with pinch-to-zoom that only ever engages once two or more
 * pointers are down. One-finger gestures — vertical scroll on the lyric list, the parent
 * HorizontalPager's horizontal swipe, tap-to-seek on a line — are never touched here, so they
 * keep working exactly as if this container weren't present.
 *
 * FS-BUG-LYRICS-ZOOM-SWIPE: the earlier version used detectTransformGestures directly, which
 * reports (and can consume) pan/zoom/rotation off the *same* single-pointer stream used for
 * scrolling and paging, so a plain one-finger swipe on the Lyrics page sometimes got eaten
 * here instead of reaching the pager. detectPinchZoomOnly below reads pointer count on every
 * pass and only starts consuming once a second pointer actually touches down.
 */
@Composable
internal fun LyricsZoomContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }

    Box(
        modifier =
            modifier
                .clipToBounds()
                .pointerInput(Unit) {
                    detectPinchZoomOnly { zoomChange ->
                        zoom = (zoom * zoomChange).coerceIn(LyricsMinZoom, LyricsMaxZoom)
                    }
                },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                    },
        ) {
            content()
        }
    }
}

/**
 * Like detectTransformGestures, but only ever reads/consumes pointer changes once at least two
 * pointers are simultaneously pressed. With 0 or 1 pointers down this does nothing and consumes
 * nothing, so a single finger is completely invisible to it — it can freely scroll the lyric
 * list, drive the pager's horizontal swipe, or tap a line to seek. As soon as a second pointer
 * lands mid-gesture, that specific gesture cycle starts tracking pinch scale; dropping back to
 * one pointer ends tracking for that cycle and hands the remaining finger back untouched.
 */
private suspend fun PointerInputScope.detectPinchZoomOnly(onZoomChange: (Float) -> Unit) {
    awaitEachGesture {
        var pinching = false
        while (true) {
            val event = awaitPointerEvent()
            val pressedCount = event.changes.count { it.pressed }

            when {
                pressedCount >= 2 -> {
                    pinching = true
                    val zoomChange = event.calculateZoom()
                    if (zoomChange != 1f) {
                        onZoomChange(zoomChange)
                    }
                    // Only claim pointers while we're actively pinching — never on the first
                    // (single-pointer) frame of a gesture, and not once a pointer lifts below 2.
                    event.changes.forEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                }
                pinching -> {
                    // Was pinching, dropped below 2 pointers: end tracking for this gesture and
                    // hand the remaining pointer(s), if any, back to everyone else unconsumed.
                    return@awaitEachGesture
                }
                else -> {
                    // Fewer than 2 pointers and we were never pinching — not our gesture, leave
                    // every change exactly as we found it.
                }
            }

            if (event.changes.none { it.pressed }) return@awaitEachGesture
        }
    }
}
