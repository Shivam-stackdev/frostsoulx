/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package dev.vxs.frostsoulx.lyrics

import android.content.Context
import dev.vxs.frostsoulx.constants.EnableGeniusLyricsKey
import dev.vxs.frostsoulx.utils.dataStore
import dev.vxs.frostsoulx.utils.get
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeniusLyricsProvider : LyricsProvider {
    override val name = "Genius"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableGeniusLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val query = URLEncoder.encode("$title $artist", StandardCharsets.UTF_8.name())
            val searchJson = request("https://genius.com/api/search/multi?q=$query")
            val candidateUrls = Regex("\\\"url\\\":\\\"(https://genius\\.com/[^\\\"]+)\\\"")
                .findAll(searchJson)
                .map { it.groupValues[1].replace("\\\\/", "/") }
                .distinct()
                .toList()
            val page = candidateUrls.firstOrNull { url ->
                val normalizedUrl = url.lowercase()
                normalizedUrl.contains(title.lowercase().replace(" ", "-")) ||
                    normalizedUrl.contains(artist.lowercase().replace(" ", "-"))
            } ?: candidateUrls.firstOrNull() ?: error("Genius track not found")
            val html = request(page)
            val lyrics = Regex("data-lyrics-container=\\\"true\\\"[^>]*>(.*?)</div>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .findAll(html)
                .map { decodeHtml(it.groupValues[1]) }
                .joinToString("\n")
                .trim()
            lyrics.takeIf { it.isNotBlank() } ?: error("Genius lyrics unavailable")
        }
    }

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getLyrics(id, title, artist, album, duration).onSuccess(callback)
    }

    private fun request(url: String): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", "FrostSoulX")
        connection.setRequestProperty("Accept", "application/json, text/html")
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    private fun decodeHtml(value: String): String = value
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#x27;", "'")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
}
