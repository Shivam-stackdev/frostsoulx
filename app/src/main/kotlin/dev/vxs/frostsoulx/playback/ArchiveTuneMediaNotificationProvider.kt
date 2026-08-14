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
import android.widget.RemoteViews
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
import dev.vxs.frostsoulx.lyrics.core.LyricsSyncState

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
    private var lastLyricLine: CharSequence = ""
    private var lastLyricPrimary: CharSequence = ""
    private var lastLyricSecondary: CharSequence = ""
    private var lastActiveIndex = -1
    private var lastActiveWordCount = -1

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
                .setContentText(lastLyricPrimary)
                .setSubText(lastLyricSecondary)
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

    /**
     * Updates the expanded notification's current lyric line.
     *
     * When [highlightEnabled] and the entry has word-level timing, already-sung words are
     * painted dark cyan. RemoteViews can't animate a live sweep like the in-app player does,
     * so this is a stepped approximation: each word snaps to cyan once playback reaches it,
     * re-rendered on the same ~400ms tick that drives this call.
     */
    fun updateLyricsPosition(
        lyrics: List<LyricsEntry>?,
        positionMs: Long,
        highlightEnabled: Boolean,
    ): Boolean {
        val newIndex = findCurrentIndex(lyrics, positionMs)
        val entry = lyrics?.getOrNull(newIndex)
        val wordCount = if (highlightEnabled) countHighlightedWords(entry, positionMs) else -1
        if (newIndex == lastActiveIndex && wordCount == lastActiveWordCount) return false
        lastActiveIndex = newIndex
        lastActiveWordCount = wordCount
        setRenderedLyrics(
            current = buildLyricLine(entry, positionMs, highlightEnabled),
            next = lyrics?.getOrNull(newIndex + 1)?.text,
        )
        return true
    }

    fun updateLyricsState(
        state: LyricsSyncState,
        highlightEnabled: Boolean,
    ): Boolean {
        val line = state.currentLine
            ?: run {
                lastActiveIndex = -1
                lastActiveWordCount = -1
                return setRenderedLyrics(current = "", next = null)
            }
        val activeWords =
            if (highlightEnabled) {
                line.words.count { state.timestampMs >= it.startMs }
            } else {
                -1
            }
        if (state.currentLineIndex == lastActiveIndex && activeWords == lastActiveWordCount) return false
        lastActiveIndex = state.currentLineIndex
        lastActiveWordCount = activeWords
        val current =
            if (highlightEnabled && line.words.isNotEmpty()) {
                val builder = SpannableStringBuilder()
                line.words.forEach { word ->
                    val start = builder.length
                    builder.append(word.text)
                    if (state.timestampMs >= word.startMs) {
                        builder.setSpan(
                            ForegroundColorSpan(0xFF008B8B.toInt()),
                            start,
                            builder.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                        )
                    }
                }
                builder
            } else {
                line.text
            }
        setRenderedLyrics(current = current, next = state.nextLine?.text)
        return true
    }

    private fun setRenderedLyrics(
        current: CharSequence,
        next: CharSequence?,
    ): Boolean {
        val normalizedNext: CharSequence = next?.takeIf { it.isNotBlank() } ?: ""
        val combined =
            normalizedNext
                .takeIf { it.isNotEmpty() }
                ?.let { SpannableStringBuilder(current).append("\n").append(it) }
                ?: current
        if (current == lastLyricPrimary && normalizedNext == lastLyricSecondary && combined == lastLyricLine) {
            return false
        }
        lastLyricPrimary = current
        lastLyricSecondary = normalizedNext
        lastLyricLine = combined
        bigRemoteViews.setTextViewText(R.id.notification_lyrics, lastLyricLine)
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
