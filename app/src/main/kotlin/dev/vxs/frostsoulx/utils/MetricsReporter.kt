package dev.vxs.frostsoulx.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Sends only anonymous install and daily-active events to the public metrics endpoint.
 * All failures are ignored so analytics can never affect app startup or playback.
 */
object MetricsReporter : DefaultLifecycleObserver {
    private const val ENDPOINT = "https://script.google.com/macros/s/AKfycbw0c2FPkJVrnKf0u5axydRhTp5nr1t3LFea0qcVLA7ZxuL3KcL_bCC6_026xu3S2UOPYg/exec"
    private const val PREFS = "anonymous_metrics"
    private const val INSTALL_ID = "install_id"
    private const val INSTALL_SENT = "install_sent"
    private const val LAST_ACTIVE_DATE = "last_active_date"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 10_000

    @Volatile
    private var initialized = false
    private lateinit var appContext: Context
    private lateinit var scope: CoroutineScope

    fun initialize(context: Context, applicationScope: CoroutineScope) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            appContext = context.applicationContext
            scope = applicationScope
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            reportInstallIfNeeded()
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        reportActiveIfNeeded()
    }

    private fun reportInstallIfNeeded() {
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(INSTALL_SENT, false)) return
        val installId = getOrCreateInstallId(prefs)
        scope.launch(Dispatchers.IO) {
            if (sendEvent(installId, "install")) {
                prefs.edit().putBoolean(INSTALL_SENT, true).apply()
            }
        }
    }

    private fun reportActiveIfNeeded() {
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (prefs.getString(LAST_ACTIVE_DATE, null) == today) return
        val installId = getOrCreateInstallId(prefs)
        scope.launch(Dispatchers.IO) {
            if (sendEvent(installId, "active")) {
                prefs.edit().putString(LAST_ACTIVE_DATE, today).apply()
            }
        }
    }

    private fun getOrCreateInstallId(prefs: SharedPreferences): String {
        return prefs.getString(INSTALL_ID, null) ?: UUID.randomUUID().toString().also { id ->
            prefs.edit().putString(INSTALL_ID, id).apply()
        }
    }

    private fun sendEvent(installId: String, event: String): Boolean {
        return runCatching {
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                useCaches = false
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            try {
                OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                    writer.write("{\"event\":\"$event\",\"installId\":\"$installId\"}")
                }
                connection.responseCode in 200..299
            } finally {
                connection.disconnect()
            }
        }.onFailure { Timber.d(it, "Anonymous metrics request failed") }.getOrDefault(false)
    }
}
