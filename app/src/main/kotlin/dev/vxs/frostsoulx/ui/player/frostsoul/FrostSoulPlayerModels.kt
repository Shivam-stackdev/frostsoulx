/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import dev.vxs.frostsoulx.models.MediaMetadata

internal val FrostSoulCyan = Color(0xFF00B7C7)
internal val FrostSoulCyanBright = Color(0xFF72F3FF)
internal val FrostSoulSurface = Color(0xFF071013)
internal val FrostSoulSurfaceElevated = Color(0xFF0B191D)
internal val FrostSoulOnSurface = Color(0xFFF4FCFD)
internal val FrostSoulOnSurfaceMuted = Color(0xFFA7BEC3)

@Immutable
internal data class FrostSoulPalette(
    val artworkPrimary: Color = FrostSoulCyan,
    val artworkSecondary: Color = Color(0xFF12494F),
    val accent: Color = FrostSoulCyan,
) {
    companion object {
        val Default = FrostSoulPalette()
    }
}

@Immutable
internal data class FrostSoulTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val isLiked: Boolean,
) {
    companion object {
        fun from(metadata: MediaMetadata, isLiked: Boolean = metadata.liked): FrostSoulTrack =
            FrostSoulTrack(
                id = metadata.id,
                title = metadata.title.ifBlank { "Unknown track" },
                artist = metadata.artists.joinToString(separator = " • ") { it.name }.ifBlank { "Unknown artist" },
                album = metadata.album?.title.orEmpty(),
                artworkUrl = metadata.thumbnailUrl,
                durationMs = metadata.duration.coerceAtLeast(0).toLong() * 1_000L,
                isLiked = isLiked,
            )
    }
}

@Immutable
internal data class FrostSoulQueueItem(
    val index: Int,
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isCurrent: Boolean,
)

@Immutable
internal data class FrostSoulPlayerUiState(
    val track: FrostSoulTrack,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val canSkipPrevious: Boolean,
    val canSkipNext: Boolean,
    val queueTitle: String?,
    val queue: List<FrostSoulQueueItem>,
    val lyrics: String?,
    val palette: FrostSoulPalette = FrostSoulPalette.Default,
) {
    val safeDurationMs: Long
        get() = durationMs.takeIf { it > 0L } ?: track.durationMs

    val progress: Float
        get() =
            if (safeDurationMs <= 0L) {
                0f
            } else {
                (positionMs.toFloat() / safeDurationMs.toFloat()).coerceIn(0f, 1f)
            }
}

@Immutable
internal data class FrostSoulPlayerActions(
    val onDismiss: () -> Unit,
    val onTogglePlayPause: () -> Unit,
    val onSkipPrevious: () -> Unit,
    val onSkipNext: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onToggleLike: () -> Unit,
    val onSelectQueueItem: (Int) -> Unit,
)

internal enum class FrostSoulPage {
    Album,
    Lyrics,
    Info,
    Queue,
}

internal fun Long.asFrostSoulTime(): String {
    val totalSeconds = (coerceAtLeast(0L) / 1_000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
