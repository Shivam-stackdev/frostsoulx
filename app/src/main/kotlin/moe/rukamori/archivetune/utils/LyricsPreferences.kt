package moe.rukamori.archivetune.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferences for lyrics display options.
 */
object LyricsPreferences {

    private const val DARK_CYAN_HIGHLIGHT = "lyrics_dark_cyan_highlight"
    private const val NOTIFICATION_LYRICS_ENABLED = "notification_lyrics_enabled"

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences("lyrics_preferences", Context.MODE_PRIVATE)
    }

    /**
     * Whether the dark cyan overlay highlight on active lyric lines is enabled.
     */
    fun isDarkCyanHighlightEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(DARK_CYAN_HIGHLIGHT, false)
    }

    /**
     * Whether notification bar lyrics are enabled.
     */
    fun isNotificationLyricsEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(NOTIFICATION_LYRICS_ENABLED, true)
    }

    fun setDarkCyanHighlight(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(DARK_CYAN_HIGHLIGHT, enabled).apply()
    }

    fun setNotificationLyrics(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(NOTIFICATION_LYRICS_ENABLED, enabled).apply()
    }
}
