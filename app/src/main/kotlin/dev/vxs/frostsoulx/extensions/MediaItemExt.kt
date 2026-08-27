/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.extensions

import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import dev.vxs.frostsoulx.db.entities.Song
import dev.vxs.frostsoulx.innertube.models.SongItem
import dev.vxs.frostsoulx.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_OMV
import dev.vxs.frostsoulx.innertube.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_UGC
import dev.vxs.frostsoulx.models.MediaMetadata
import dev.vxs.frostsoulx.models.toMediaMetadata
import dev.vxs.frostsoulx.ui.utils.YTThumbQuality
import dev.vxs.frostsoulx.ui.utils.YtimgResizePolicy
import dev.vxs.frostsoulx.ui.utils.buildYTThumbnailUrl
import dev.vxs.frostsoulx.ui.utils.resize
import dev.vxs.frostsoulx.utils.NotificationArtworkSizePx
import dev.vxs.frostsoulx.utils.isLocalMediaId

const val ExtraIsMusicVideo = "dev.vxs.frostsoulx.extra.IS_MUSIC_VIDEO"

val MediaItem.metadata: MediaMetadata?
    get() = localConfiguration?.tag as? MediaMetadata

private fun String?.toNotificationArtworkUri() =
    this
        ?.takeIf { it.isNotBlank() }
        ?.resize(
            width = NotificationArtworkSizePx,
            height = NotificationArtworkSizePx,
            ytimgResizePolicy = YtimgResizePolicy.PreserveOriginal,
        )?.toUri()

private fun notificationArtworkUri(
    mediaId: String,
    thumbnailUrl: String?,
    isMusicVideo: Boolean,
) =
    if (isMusicVideo && mediaId.length == 11 && !mediaId.isLocalMediaId()) {
        buildYTThumbnailUrl(mediaId, YTThumbQuality.HQ).toUri()
    } else {
        thumbnailUrl.toNotificationArtworkUri()
    }

private fun MediaItem.Builder.setCacheKeyIfRemote(mediaId: String): MediaItem.Builder {
    if (!mediaId.isLocalMediaId()) {
        setCustomCacheKey(mediaId)
    }
    return this
}

fun Song.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(song.id)
        .setUri(song.id)
        .setCacheKeyIfRemote(song.id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(song.title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    notificationArtworkUri(
                        mediaId = song.id,
                        thumbnailUrl = song.thumbnailUrl,
                        isMusicVideo = song.isMusicVideo,
                    ),
                )
                .setAlbumTitle(song.albumName)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, song.isMusicVideo) })
                .build(),
        ).build()

fun SongItem.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCacheKeyIfRemote(id)
        .setTag(toMediaMetadata())
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    notificationArtworkUri(
                        mediaId = id,
                        thumbnailUrl = thumbnail,
                        isMusicVideo = isMusicVideo(),
                    ),
                ).setAlbumTitle(album?.name)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, isMusicVideo()) })
                .build(),
        ).build()

fun MediaMetadata.toMediaItem() =
    MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(id)
        .setCacheKeyIfRemote(id)
        .setTag(this)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata
                .Builder()
                .setTitle(title)
                .setSubtitle(artists.joinToString { it.name })
                .setArtist(artists.joinToString { it.name })
                .setArtworkUri(
                    notificationArtworkUri(
                        mediaId = id,
                        thumbnailUrl = thumbnailUrl,
                        isMusicVideo = isMusicVideo,
                    ),
                ).setAlbumTitle(album?.title)
                .setIsPlayable(true)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setExtras(Bundle().apply { putBoolean(ExtraIsMusicVideo, isMusicVideo) })
                .build(),
        ).build()

private fun SongItem.isMusicVideo(): Boolean {
    val musicVideoType = endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
    return musicVideoType == MUSIC_VIDEO_TYPE_OMV || musicVideoType == MUSIC_VIDEO_TYPE_UGC
}
