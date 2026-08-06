/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package moe.rukamori.archivetune.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.lyrics.LyricsEntry

/** Media3 notification provider with an expanded, time-synced lyrics line. */
@UnstableApi
class ArchiveTuneMediaNotificationProvider(
    private val context: Context,
    @DrawableRes smallIconResId: Int,
) : MediaNotification.Provider {
    private val delegate =
        DefaultMediaNotificationProvider(
            context,
            { MusicService.NOTIFICATION_ID },
            MusicService.CHANNEL_ID,
            R.string.music_player,
        ).apply {
            setSmallIcon(smallIconResId)
        }

    private val bigRemoteViews by lazy {
        RemoteViews(context.packageName, R.layout.notification_player_big)
    }
    private var lastLyricLine = ""
    private var lastActiveIndex = -1

    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val mediaNotification =
            delegate.createNotification(
                mediaSession,
                mediaButtonPreferences,
                actionFactory,
                onNotificationChangedCallback,
            )
        val original = mediaNotification.notification
        val originalDeleteIntent = original.deleteIntent
        val notification =
            NotificationCompat.Builder(context, original)
                .setCustomBigContentView(bigRemoteViews.apply {
                    setTextViewText(R.id.notification_lyrics, lastLyricLine)
                })
                .build()
        if (originalDeleteIntent != null) {
            notification.deleteIntent = PendingIntent.getService(
                context,
                mediaNotification.notificationId,
                Intent(context, MusicService::class.java).apply {
                    action = MusicService.ACTION_MEDIA_NOTIFICATION_DISMISSED
                    putExtra(MusicService.EXTRA_MEDIA_NOTIFICATION_DELETE_INTENT, originalDeleteIntent)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        return MediaNotification(mediaNotification.notificationId, notification)
    }

    /** Updates the expanded notification's current lyric line. */
    fun updateLyricsPosition(lyrics: List<LyricsEntry>?, positionMs: Long): Boolean {
        val newLine = findCurrentLine(lyrics, positionMs)
        val newIndex = findCurrentIndex(lyrics, positionMs)
        if (newLine == lastLyricLine && newIndex == lastActiveIndex) return false
        lastLyricLine = newLine
        lastActiveIndex = newIndex
        bigRemoteViews.setTextViewText(R.id.notification_lyrics, newLine)
        return true
    }

    private fun findCurrentLine(lyrics: List<LyricsEntry>?, positionMs: Long): String {
        if (lyrics.isNullOrEmpty()) return ""
        var current = ""
        for (entry in lyrics) {
            if (entry.time <= positionMs) current = entry.text else break
        }
        return current
    }

    private fun findCurrentIndex(lyrics: List<LyricsEntry>?, positionMs: Long): Int {
        if (lyrics.isNullOrEmpty()) return -1
        var index = -1
        for (i in lyrics.indices) {
            if (lyrics[i].time <= positionMs) index = i else break
        }
        return index
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle,
    ): Boolean = delegate.handleCustomCommand(session, action, extras)

    override fun getNotificationChannelInfo(): MediaNotification.Provider.NotificationChannelInfo =
        delegate.notificationChannelInfo
}
