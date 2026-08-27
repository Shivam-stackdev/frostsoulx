/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.repository

import dev.vxs.frostsoulx.db.MusicDatabase
import dev.vxs.frostsoulx.db.entities.LyricsDocumentEntity
import dev.vxs.frostsoulx.db.entities.LyricsEntity
import dev.vxs.frostsoulx.lyrics.LyricsHelper
import dev.vxs.frostsoulx.lyrics.core.LyricsDocument
import dev.vxs.frostsoulx.lyrics.core.LyricsDownloadResult
import dev.vxs.frostsoulx.lyrics.core.LyricsSearchRequest
import dev.vxs.frostsoulx.lyrics.parser.LyricsParser
import dev.vxs.frostsoulx.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsRepository @Inject constructor(
    private val database: MusicDatabase,
    private val lyricsHelper: LyricsHelper,
) {
    private val memoryLock = Mutex()
    private val parsedDocuments = object : LinkedHashMap<String, LyricsDocument>(MemoryCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, LyricsDocument>?): Boolean = size > MemoryCacheSize
    }

    suspend fun resolve(
        metadata: MediaMetadata,
        forceRefresh: Boolean = false,
    ): LyricsDocument? =
        withContext(Dispatchers.IO) {
            if (!forceRefresh) memoryLock.withLock { parsedDocuments[metadata.id] }?.let { return@withContext it }
            if (!forceRefresh) database.lyricsDocument(metadata.id)?.toDocument()?.let { document ->
                cache(document)
                return@withContext document
            }

            val existingRaw = database.getLyricsById(metadata.id)
            val raw =
                when {
                    !forceRefresh &&
                        existingRaw != null &&
                        existingRaw.lyrics != LyricsEntity.LYRICS_NOT_FOUND &&
                        (
                            existingRaw.source == LyricsEntity.Source.USER_SELECTION.value ||
                                lyricsHelper.isLikelyForTrack(existingRaw.lyrics, metadata)
                        ) -> existingRaw.lyrics
                    else -> lyricsHelper.getLyrics(metadata, forceRefresh = forceRefresh)
                }
            if (raw == LyricsEntity.LYRICS_NOT_FOUND || raw.isBlank()) return@withContext null

            val document = LyricsParser.parse(songId = metadata.id, originalText = raw, source = existingRaw?.source)
            persist(document)
            database.replaceLyricsIfAbsentOrNotFound(metadata.id, raw)
            cache(document)
            document
        }

    suspend fun search(request: LyricsSearchRequest): List<ManualLyricsCandidate> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<ManualLyricsCandidate>()
            lyricsHelper.getAllLyrics(
                mediaId = request.songId,
                songTitle = request.title,
                songArtists = request.artist,
                songAlbum = request.album,
                duration = ((request.durationMs ?: 0L) / 1_000L).toInt(),
            ) { result ->
                results += ManualLyricsCandidate(provider = result.providerName, lyrics = result.lyrics)
            }
            results.distinctBy { "${it.provider}:${it.lyrics}" }
        }

    suspend fun saveManualSelection(
        songId: String,
        lyrics: String,
        provider: String,
        translation: String? = null,
        romanization: String? = null,
        artworkKey: String? = null,
    ): LyricsDocument {
        val document =
            LyricsParser.parse(
                songId = songId,
                originalText = lyrics,
                translationText = translation,
                romanizationText = romanization,
                source = provider,
                artworkKey = artworkKey,
            )
        withContext(Dispatchers.IO) {
            persist(document)
            database.insert(LyricsEntity(id = songId, lyrics = lyrics, source = LyricsEntity.Source.USER_SELECTION.value))
            cache(document)
        }
        return document
    }

    suspend fun updateOffset(
        songId: String,
        offsetMs: Long,
    ) {
        val safeOffset = offsetMs.coerceIn(MinimumOffsetMs, MaximumOffsetMs)
        withContext(Dispatchers.IO) {
            database.updateLyricsDocumentOffset(songId, safeOffset, System.currentTimeMillis())
            memoryLock.withLock {
                parsedDocuments[songId] = parsedDocuments[songId]?.withOffset(safeOffset) ?: return@withLock
            }
        }
    }

    suspend fun prune(retainAfterMs: Long): Int = withContext(Dispatchers.IO) { database.pruneLyricsDocuments(retainAfterMs) }

    private suspend fun persist(document: LyricsDocument) {
        database.upsertLyricsDocument(
            LyricsDocumentEntity(
                songId = document.songId,
                original = document.original.lines.toLrc(),
                translation = document.translation?.lines?.toLrc(),
                romanization = document.romanization?.lines?.toLrc(),
                format = document.format.name,
                source = document.source.orEmpty(),
                offsetMs = document.offsetMs,
                artworkKey = document.artworkKey,
                updatedAtMs = document.updatedAtMs,
            ),
        )
    }

    private suspend fun cache(document: LyricsDocument) {
        memoryLock.withLock { parsedDocuments[document.songId] = document }
    }

    private fun LyricsDocumentEntity.toDocument(): LyricsDocument =
        LyricsParser.parse(
            songId = songId,
            originalText = original,
            translationText = translation,
            romanizationText = romanization,
            source = source.ifBlank { null },
            artworkKey = artworkKey,
        ).withOffset(offsetMs)

    private fun List<dev.vxs.frostsoulx.lyrics.core.LyricsLine>.toLrc(): String =
        joinToString(separator = "\n") { line ->
            val minute = line.startMs / 60_000L
            val second = (line.startMs % 60_000L) / 1_000L
            val milli = line.startMs % 1_000L
            val words =
                line.words.takeIf { it.isNotEmpty() }?.joinToString(separator = "") { word ->
                    val wordMinute = word.startMs / 60_000L
                    val wordSecond = (word.startMs % 60_000L) / 1_000L
                    val wordMilli = word.startMs % 1_000L
                    "<%02d:%02d.%03d>%s".format(wordMinute, wordSecond, wordMilli, word.text)
                }
            "[%02d:%02d.%03d]%s".format(minute, second, milli, words ?: line.text)
        }

    private companion object {
        const val MemoryCacheSize = 24
        const val MinimumOffsetMs = -30_000L
        const val MaximumOffsetMs = 30_000L
    }
}

data class ManualLyricsCandidate(
    val provider: String,
    val lyrics: String,
)
