package dev.vxs.frostsoulx.utils

import android.content.Context

object UserGreetingPreferences {
    private const val PREFS = "user_greeting"
    private const val NAME = "name"
    private const val PROMPTED = "prompted"

    fun getName(context: Context): String? =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(NAME, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun hasPrompted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(PROMPTED, false)

    fun save(context: Context, name: String) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(NAME, name.trim())
            .putBoolean(PROMPTED, true)
            .apply()
    }

    fun skip(context: Context) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PROMPTED, true)
            .apply()
    }
}
