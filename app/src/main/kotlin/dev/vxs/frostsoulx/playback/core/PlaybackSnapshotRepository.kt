/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.playback.core

import android.content.Context
import android.util.AtomicFile
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import javax.inject.Inject
import javax.inject.Singleton

interface PlaybackSnapshotRepository {
    val snapshots: StateFlow<List<PlaybackSessionSnapshot>>

    suspend fun save(snapshot: PlaybackSessionSnapshot)

    suspend fun latest(): PlaybackSessionSnapshot?

    suspend fun clear()
}

@Singleton
class FilePlaybackSnapshotRepository @Inject constructor(
    context: Context,
) : PlaybackSnapshotRepository {
    private val file = AtomicFile(context.filesDir.resolve(SnapshotFileName))
    private val writeMutex = Mutex()
    private val _snapshots = MutableStateFlow(readSnapshots())

    override val snapshots: StateFlow<List<PlaybackSessionSnapshot>> = _snapshots.asStateFlow()

    override suspend fun save(snapshot: PlaybackSessionSnapshot) {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                val updated =
                    buildList(MaxSnapshots) {
                        add(snapshot)
                        addAll(_snapshots.value.filterNot { it.isEquivalentTo(snapshot) }.take(MaxSnapshots - 1))
                    }
                writeSnapshots(updated)
                _snapshots.value = updated
            }
        }
    }

    override suspend fun latest(): PlaybackSessionSnapshot? = _snapshots.value.firstOrNull()

    override suspend fun clear() {
        writeMutex.withLock {
            withContext(Dispatchers.IO) {
                file.delete()
                _snapshots.value = emptyList()
            }
        }
    }

    private fun readSnapshots(): List<PlaybackSessionSnapshot> =
        runCatching {
            if (!file.baseFile.exists()) return emptyList()
            file.openRead().use { input ->
                ObjectInputStream(BufferedInputStream(input)).use { stream ->
                    val payload = stream.readObject() as? SnapshotPayload ?: return emptyList()
                    payload.snapshots
                        .filter { it.items.isNotEmpty() }
                        .sortedByDescending { it.capturedAtEpochMs }
                        .take(MaxSnapshots)
                }
            }
        }.getOrElse {
            file.delete()
            emptyList()
        }

    private fun writeSnapshots(snapshots: List<PlaybackSessionSnapshot>) {
        var output = file.startWrite()
        try {
            val stream = ObjectOutputStream(BufferedOutputStream(output))
            stream.writeObject(SnapshotPayload(snapshots))
            stream.flush()
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    private fun PlaybackSessionSnapshot.isEquivalentTo(other: PlaybackSessionSnapshot): Boolean =
        currentIndex == other.currentIndex &&
            currentPositionMs == other.currentPositionMs &&
            items.map { it.mediaId } == other.items.map { it.mediaId }

    private data class SnapshotPayload(
        val snapshots: List<PlaybackSessionSnapshot>,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private companion object {
        const val SnapshotFileName = "playback-session-snapshots.bin"
        const val MaxSnapshots = 12
    }
}

@VisibleForTesting
internal class InMemoryPlaybackSnapshotRepository : PlaybackSnapshotRepository {
    private val _snapshots = MutableStateFlow<List<PlaybackSessionSnapshot>>(emptyList())
    override val snapshots: StateFlow<List<PlaybackSessionSnapshot>> = _snapshots.asStateFlow()

    override suspend fun save(snapshot: PlaybackSessionSnapshot) {
        _snapshots.value = listOf(snapshot) + _snapshots.value.filterNot { it.capturedAtEpochMs == snapshot.capturedAtEpochMs }
    }

    override suspend fun latest(): PlaybackSessionSnapshot? = _snapshots.value.firstOrNull()

    override suspend fun clear() {
        _snapshots.value = emptyList()
    }
}
