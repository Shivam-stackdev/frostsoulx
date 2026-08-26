/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import android.provider.Settings
import dagger.hilt.android.EntryPointAccessors
import dev.vxs.frostsoulx.db.entities.containerLabel
import dev.vxs.frostsoulx.di.LyricsHelperEntryPoint
import androidx.media3.common.Timeline
import dev.vxs.frostsoulx.extensions.mediaItems
import dev.vxs.frostsoulx.extensions.metadata
import dev.vxs.frostsoulx.extensions.togglePlayPause
import dev.vxs.frostsoulx.extensions.toggleRepeatMode
import dev.vxs.frostsoulx.models.MediaMetadata
import dev.vxs.frostsoulx.playback.PlayerConnection
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulPlayer
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulPlayerActions
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulPlayerUiState
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulQueueItem
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulTrack
import dev.vxs.frostsoulx.ui.player.frostsoul.rememberFrostSoulPalette

@Composable
internal fun FrostSoulPlayerAdapter(
    mediaMetadata: MediaMetadata,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isLiked: Boolean,
    queueTitle: String?,
    queueWindows: List<Timeline.Window>,
    currentQueueIndex: Int,
    lyrics: String?,
    playerConnection: PlayerConnection,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberFrostSoulPalette(mediaMetadata.thumbnailUrl)
    val applicationContext = LocalContext.current.applicationContext
    val lyricsSynchronizationEngine =
        remember(applicationContext) {
            EntryPointAccessors
                .fromApplication(applicationContext, LyricsHelperEntryPoint::class.java)
                .lyricsSynchronizationEngine()
        }
    val currentLyricLine by lyricsSynchronizationEngine.currentLine.collectAsState()
    val currentLyricText = currentLyricLine?.text
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    val audioQualityBadge =
        remember(currentFormat) {
            currentFormat?.containerLabel()?.uppercase()?.takeIf { it.isNotBlank() }
        }
    val queue =
        remember(queueWindows, currentQueueIndex) {
            queueWindows.mapIndexedNotNull { index, window ->
                val item = window.mediaItem.metadata ?: return@mapIndexedNotNull null
                FrostSoulQueueItem(
                    index = index,
                    id = item.id,
                    title = item.title,
                    artist = item.artists.joinToString(separator = " • ") { it.name }.ifBlank { "Unknown artist" },
                    artworkUrl = item.thumbnailUrl,
                    durationMs = item.duration.coerceAtLeast(0).toLong() * 1_000L,
                    isCurrent = index == currentQueueIndex,
                )
            }
        }
    val uiState =
        remember(
            mediaMetadata,
            positionMs,
            durationMs,
            isPlaying,
            isLoading,
            canSkipPrevious,
            canSkipNext,
            isLiked,
            queueTitle,
            queue,
            lyrics,
            currentLyricText,
            audioQualityBadge,
            palette,
        ) {
            FrostSoulPlayerUiState(
                track = FrostSoulTrack.from(mediaMetadata, isLiked),
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                isBuffering = isLoading,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                queueTitle = queueTitle,
                queue = queue,
                lyrics = lyrics,
                currentLyricLine = currentLyricText,
                audioQualityBadge = audioQualityBadge,
                palette = palette,
            )
        }
    val actions =
        remember(playerConnection, queueWindows, onCollapse, applicationContext) {
            FrostSoulPlayerActions(
                onDismiss = onCollapse,
                onTogglePlayPause = { playerConnection.player.togglePlayPause() },
                onSkipPrevious = playerConnection::seekToPrevious,
                onSkipNext = playerConnection::seekToNext,
                onToggleRepeat = { playerConnection.player.toggleRepeatMode() },
                onSeek = { targetPosition -> playerConnection.player.seekTo(targetPosition) },
                onToggleLike = playerConnection::toggleLike,
                onOpenAudioOutput = {
                    applicationContext.startActivity(
                        Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
                onSelectQueueItem = { queueIndex ->
                    val targetWindow = queueWindows.getOrNull(queueIndex)
                    if (targetWindow != null) {
                        val targetMediaId = targetWindow.mediaItem.mediaId
                        val mediaIndex = playerConnection.player.mediaItems.indexOfFirst { it.mediaId == targetMediaId }
                        if (mediaIndex >= 0) {
                            playerConnection.player.seekToDefaultPosition(mediaIndex)
                            playerConnection.player.prepare()
                            playerConnection.player.play()
                        }
                    }
                },
            )
        }

    FrostSoulPlayer(
        uiState = uiState,
        actions = actions,
        modifier = modifier,
    )
}
