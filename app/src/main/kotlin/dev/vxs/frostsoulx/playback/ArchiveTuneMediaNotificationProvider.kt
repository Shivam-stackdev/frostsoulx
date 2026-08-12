/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package dev.vxs.frostsoulx.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.lyrics.LyricsEntry

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

    private var mediaSession: MediaSession? = null
    private var mediaButtonPreferences: ImmutableList<CommandButton> = ImmutableList.of()
    private var actionFactory: MediaNotification.ActionFactory? = null
    private var onNotificationChangedCallback: MediaNotification.Provider.Callback? = null
    private var lastLyricLine: CharSequence = ""
    private var lastLyricsHash: Int? = null
    private var lastActiveIndex = -1
    private var lastActiveWordCount = -1
    private var lastLyricsEnabled = true

    override fun createNotification(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        this.mediaSession = mediaSession
        this.mediaButtonPreferences = mediaButtonPreferences
        this.actionFactory = actionFactory
        this.onNotificationChangedCallback = onNotificationChangedCallback
        return createNotificationWithCurrentLyric(
            mediaSession = mediaSession,
            mediaButtonPreferences = mediaButtonPreferences,
            actionFactory = actionFactory,
            callback = onNotificationChangedCallback,
        )
    }

    /**
     * Media3 owns posting a provider-created notification. Android 16 can ignore a retained
     * custom RemoteViews surface in the compact media player, so lyrics are placed in the
     * delegated notification's standard content text and republished through Media3's callback.
     */
    private fun createNotificationWithCurrentLyric(
        mediaSession: MediaSession,
        mediaButtonPreferences: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        callback: MediaNotification.Provider.Callback,
    ): MediaNotification {
        val mediaNotification =
            delegate.createNotification(
                mediaSession,
                mediaButtonPreferences,
                actionFactory,
                callback,
            )
        val original = mediaNotification.notification
        val originalDeleteIntent = original.deleteIntent
        val notification =
            NotificationCompat.Builder(context, original)
                .setContentText(lastLyricLine.takeIf { it.isNotEmpty() })
                .setOnlyAlertOnce(true)
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

    /**
     * Updates the notification lyric line using the same playback position as the player.
     * RemoteViews cannot render the in-app sweep animation, so it displays completed words
     * in Dark Cyan and is rebuilt only when the line or completed-word boundary changes.
     */
    fun updateLyricsPosition(
        lyrics: List<LyricsEntry>?,
        positionMs: Long,
        highlightEnabled: Boolean,
        lyricsEnabled: Boolean = true,
    ): Boolean {
        val lyricsHash = lyrics?.hashCode()
        val newIndex = if (lyricsEnabled) findCurrentIndex(lyrics, positionMs) else -1
        val entry = if (lyricsEnabled) lyrics?.getOrNull(newIndex) else null
        val wordCount = if (highlightEnabled && lyricsEnabled) countHighlightedWords(entry, positionMs) else -1
        if (
            lyricsHash == lastLyricsHash &&
            newIndex == lastActiveIndex &&
            wordCount == lastActiveWordCount &&
            lyricsEnabled == lastLyricsEnabled
        ) {
            return false
        }
        lastLyricsHash = lyricsHash
        lastActiveIndex = newIndex
        lastActiveWordCount = wordCount
        lastLyricsEnabled = lyricsEnabled
        lastLyricLine = buildLyricLine(entry, positionMs, highlightEnabled)

        val session = mediaSession
        val factory = actionFactory
        val callback = onNotificationChangedCallback
        if (session != null && factory != null && callback != null) {
            callback.onNotificationChanged(
                createNotificationWithCurrentLyric(
                    mediaSession = session,
                    mediaButtonPreferences = mediaButtonPreferences,
                    actionFactory = factory,
                    callback = callback,
                ),
            )
        }
        return true
    }

    private fun countHighlightedWords(entry: LyricsEntry?, positionMs: Long): Int {
        val words = entry?.words ?: return -1
        return words.count { positionMs >= (it.startTime * 1000).toLong() }
    }

    private fun buildLyricLine(
        entry: LyricsEntry?,
        positionMs: Long,
        highlightEnabled: Boolean,
    ): CharSequence {
        if (entry == null) return ""
        val words = entry.words
        if (!highlightEnabled || words.isNullOrEmpty()) return entry.text

        val builder = SpannableStringBuilder()
        for (word in words) {
            val start = builder.length
            builder.append(word.text)
            val wordStartMs = (word.startTime * 1000).toLong()
            if (positionMs >= wordStartMs) {
                builder.setSpan(
                    ForegroundColorSpan(0xFF008B8B.toInt()),
                    start,
                    builder.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        return builder
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
