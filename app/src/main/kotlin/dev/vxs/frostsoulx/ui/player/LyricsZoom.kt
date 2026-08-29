/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package dev.vxs.frostsoulx.ui.player

import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

private const val LyricsMinZoom = 0.75f
private const val LyricsMaxZoom = 1.80f

/**
 * Hosts an expanded lyric page with diagonal pinch zoom. One-finger scrolling remains owned by
 * the lyric list; zoom is applied only when the gesture changes scale with two or more pointers.
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
                    detectTransformGestures { _, _, zoomChange, _ ->
                        // Ignore translation here so the existing LazyColumn keeps its normal
                        // vertical scrolling behavior. A diagonal pinch changes zoomChange.
                        if (abs(zoomChange - 1f) > 0.001f) {
                            zoom = (zoom * zoomChange).coerceIn(LyricsMinZoom, LyricsMaxZoom)
                        }
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

