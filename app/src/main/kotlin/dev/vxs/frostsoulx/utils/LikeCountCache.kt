package dev.vxs.frostsoulx.utils

import android.content.Context

object LikeCountCache {
    private const val PREFS = "like_count_cache"

    fun get(context: Context, songId: String): Int? {
        if (songId.isBlank()) return null
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(songId)) prefs.getInt(songId, 0) else null
    }

    fun put(context: Context, songId: String, count: Int) {
        if (songId.isBlank() || count < 0) return
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(songId, count)
            .apply()
    }
}
