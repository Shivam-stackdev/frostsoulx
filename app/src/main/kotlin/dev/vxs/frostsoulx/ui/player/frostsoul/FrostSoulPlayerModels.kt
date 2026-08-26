/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.sp
import dev.vxs.frostsoulx.models.ActiveOutputDevice
import dev.vxs.frostsoulx.models.PlayerOutputDevice
import androidx.compose.ui.graphics.Color
import dev.vxs.frostsoulx.models.MediaMetadata

internal val FrostSoulSurface = Color(0xFF1E1E1E)
internal val FrostSoulSurfaceElevated = Color(0xFF242424)
internal val FrostSoulOnSurface = Color(0xFFFDFDFD)
internal val FrostSoulOnSurfaceMuted = Color(0xFFA5A5A5)

@Immutable
internal data class FrostSoulPalette(
    val artworkPrimary: Color = Color(0xFF8A8A8A),
    val artworkSecondary: Color = Color(0xFF30262B),
    val accent: Color = Color.White,
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
    val durationMs: Long = 0L,
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
    val currentLyricLine: String? = null,
    val audioQualityBadge: String? = null,
    val outputDevice: ActiveOutputDevice = ActiveOutputDevice(
        type = PlayerOutputDevice.Unknown,
        name = PlayerOutputDevice.Unknown.defaultName,
    ),
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
    val onToggleRepeat: () -> Unit = {},
    val onSeek: (Long) -> Unit,
    val onToggleLike: () -> Unit,
    val onOpenAudioOutput: () -> Unit = {},
    val onSelectQueueItem: (Int) -> Unit,
)

internal enum class FrostSoulPage {
    Lyrics,
    MainPlayer,
    Recommendations,
}

internal fun Long.asFrostSoulTime(): String {
    val totalSeconds = (coerceAtLeast(0L) / 1_000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
