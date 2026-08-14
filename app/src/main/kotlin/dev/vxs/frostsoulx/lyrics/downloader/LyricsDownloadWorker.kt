/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.downloader

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.vxs.frostsoulx.lyrics.repository.LyricsRepository
import dev.vxs.frostsoulx.models.MediaMetadata
import java.util.concurrent.TimeUnit

/**
 * Downloads and persists lyrics without coupling a background request to a running playback
 * service. The repository remains the only writer for raw and normalized lyrics records.
 */
class LyricsDownloadWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val metadata = inputData.toMetadata() ?: return Result.failure(resultData(ResultCode.InvalidRequest))
        val repository =
            EntryPointAccessors
                .fromApplication(applicationContext, LyricsDownloadWorkerEntryPoint::class.java)
                .lyricsRepository()

        return try {
            val document = repository.resolve(metadata, forceRefresh = inputData.getBoolean(KeyForceRefresh, false))
            if (document == null) {
                Result.success(resultData(ResultCode.NotFound))
            } else {
                Result.success(resultData(ResultCode.Downloaded))
            }
        } catch (error: Exception) {
            if (runAttemptCount < MaximumAttempts - 1) {
                Result.retry()
            } else {
                Result.failure(resultData(ResultCode.Failed))
            }
        }
    }

    private fun Data.toMetadata(): MediaMetadata? {
        val id = getString(KeySongId)?.trim().orEmpty()
        val title = getString(KeyTitle)?.trim().orEmpty()
        if (id.isBlank() || title.isBlank()) return null
        val albumTitle = getString(KeyAlbumTitle)?.trim().orEmpty()
        val artists =
            getString(KeyArtists)
                .orEmpty()
                .split(ArtistDelimiter)
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { artist -> MediaMetadata.Artist(id = null, name = artist) }
        return MediaMetadata(
            id = id,
            title = title,
            artists = artists,
            duration = getInt(KeyDurationSeconds, UnknownDurationSeconds),
            thumbnailUrl = getString(KeyArtworkUrl),
            album = albumTitle.takeIf(String::isNotBlank)?.let { titleValue ->
                MediaMetadata.Album(id = getString(KeyAlbumId).orEmpty(), title = titleValue)
            },
        )
    }

    private fun resultData(code: ResultCode): Data = workDataOf(KeyResult to code.name)

    private enum class ResultCode {
        Downloaded,
        NotFound,
        InvalidRequest,
        Failed,
    }

    private companion object {
        const val KeySongId = "song_id"
        const val KeyTitle = "title"
        const val KeyArtists = "artists"
        const val KeyAlbumId = "album_id"
        const val KeyAlbumTitle = "album_title"
        const val KeyDurationSeconds = "duration_seconds"
        const val KeyArtworkUrl = "artwork_url"
        const val KeyForceRefresh = "force_refresh"
        const val KeyResult = "lyrics_download_result"
        const val ArtistDelimiter = "\u001F"
        const val UnknownDurationSeconds = -1
        const val MaximumAttempts = 4
    }
}

/** Schedules bounded, network-aware background lyrics retrieval for automatic and manual flows. */
object LyricsDownloadScheduler {
    fun enqueueAutomatic(
        context: Context,
        metadata: MediaMetadata,
    ) = enqueue(
        context = context,
        metadata = metadata,
        forceRefresh = false,
        policy = ExistingWorkPolicy.KEEP,
        workName = "$AutomaticWorkPrefix${metadata.id}",
    )

    fun enqueueManualRefresh(
        context: Context,
        metadata: MediaMetadata,
    ) = enqueue(
        context = context,
        metadata = metadata,
        forceRefresh = true,
        policy = ExistingWorkPolicy.REPLACE,
        workName = "$ManualWorkPrefix${metadata.id}",
    )

    private fun enqueue(
        context: Context,
        metadata: MediaMetadata,
        forceRefresh: Boolean,
        policy: ExistingWorkPolicy,
        workName: String,
    ) {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
        val request =
            OneTimeWorkRequestBuilder<LyricsDownloadWorker>()
                .setInputData(metadata.toInputData(forceRefresh))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(WorkTag)
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName, policy, request)
    }

    private fun MediaMetadata.toInputData(forceRefresh: Boolean): Data =
        workDataOf(
            "song_id" to id,
            "title" to title,
            "artists" to artists.joinToString(separator = "\u001F") { it.name },
            "album_id" to album?.id,
            "album_title" to album?.title,
            "duration_seconds" to duration,
            "artwork_url" to thumbnailUrl,
            "force_refresh" to forceRefresh,
        )

    private const val WorkTag = "lyrics-download"
    private const val AutomaticWorkPrefix = "lyrics-auto-"
    private const val ManualWorkPrefix = "lyrics-manual-"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LyricsDownloadWorkerEntryPoint {
    fun lyricsRepository(): LyricsRepository
}
