/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.lyrics.editor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vxs.frostsoulx.lyrics.core.LyricsDocument
import dev.vxs.frostsoulx.lyrics.core.LyricsSearchRequest
import dev.vxs.frostsoulx.lyrics.repository.LyricsRepository
import dev.vxs.frostsoulx.lyrics.repository.ManualLyricsCandidate
import dev.vxs.frostsoulx.lyrics.sync.LyricsSynchronizationEngine
import dev.vxs.frostsoulx.models.MediaMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable editor contract used by any lyrics settings or player-sheet implementation. */
@Immutable
data class LyricsEditorUiState(
    val metadata: MediaMetadata? = null,
    val offsetMs: Long = 0L,
    val originalText: String = "",
    val translationText: String = "",
    val romanizationText: String = "",
    val candidates: List<ManualLyricsCandidate> = emptyList(),
    val isSearching: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

/**
 * Keeps editing operations isolated from playback UI while synchronizing successful changes to all
 * state consumers in one update. It never parses or resolves timing locally; the repository and
 * synchronization engine stay authoritative.
 */
@HiltViewModel
class LyricsEditorViewModel
    @Inject
    constructor(
        private val lyricsRepository: LyricsRepository,
        private val synchronizationEngine: LyricsSynchronizationEngine,
    ) : ViewModel() {
        private val _state = MutableStateFlow(LyricsEditorUiState())
        val state: StateFlow<LyricsEditorUiState> = _state.asStateFlow()

        fun open(metadata: MediaMetadata) {
            val document = synchronizationEngine.documentState.value?.takeIf { it.songId == metadata.id }
            _state.value = document.toEditorState(metadata)
        }

        fun updateOriginalText(text: String) {
            _state.value = _state.value.copy(originalText = text, errorMessage = null)
        }

        fun updateTranslationText(text: String) {
            _state.value = _state.value.copy(translationText = text, errorMessage = null)
        }

        fun updateRomanizationText(text: String) {
            _state.value = _state.value.copy(romanizationText = text, errorMessage = null)
        }

        fun adjustOffset(deltaMs: Long) {
            setOffset(_state.value.offsetMs + deltaMs)
        }

        fun setOffset(offsetMs: Long) {
            val metadata = _state.value.metadata ?: return
            val safeOffset = offsetMs.coerceIn(MinimumOffsetMs, MaximumOffsetMs)
            _state.value = _state.value.copy(offsetMs = safeOffset, errorMessage = null)
            viewModelScope.launch {
                try {
                    lyricsRepository.updateOffset(metadata.id, safeOffset)
                    if (synchronizationEngine.documentState.value?.songId == metadata.id) {
                        synchronizationEngine.setOffset(safeOffset)
                    }
                } catch (error: Exception) {
                    _state.value = _state.value.copy(errorMessage = error.userMessage())
                }
            }
        }

        fun search() {
            val metadata = _state.value.metadata ?: return
            if (_state.value.isSearching) return
            _state.value = _state.value.copy(isSearching = true, candidates = emptyList(), errorMessage = null)
            viewModelScope.launch {
                try {
                    val results =
                        lyricsRepository.search(
                            LyricsSearchRequest(
                                songId = metadata.id,
                                title = metadata.title,
                                artist = metadata.artists.joinToString(separator = ", ") { it.name },
                                album = metadata.album?.title,
                                durationMs = metadata.duration.takeIf { it >= 0 }?.times(1_000L),
                            ),
                        )
                    _state.value = _state.value.copy(candidates = results, isSearching = false)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _state.value = _state.value.copy(isSearching = false, errorMessage = error.userMessage())
                }
            }
        }

        fun selectCandidate(candidate: ManualLyricsCandidate) {
            val current = _state.value
            val metadata = current.metadata ?: return
            save(
                metadata = metadata,
                lyrics = candidate.lyrics,
                provider = candidate.provider,
                translation = current.translationText.takeIf(String::isNotBlank),
                romanization = current.romanizationText.takeIf(String::isNotBlank),
            )
        }

        fun saveManualEntry() {
            val current = _state.value
            val metadata = current.metadata ?: return
            if (current.originalText.isBlank()) {
                _state.value = current.copy(errorMessage = "Enter lyrics before saving.")
                return
            }
            save(
                metadata = metadata,
                lyrics = current.originalText,
                provider = ManualProvider,
                translation = current.translationText.takeIf(String::isNotBlank),
                romanization = current.romanizationText.takeIf(String::isNotBlank),
            )
        }

        private fun save(
            metadata: MediaMetadata,
            lyrics: String,
            provider: String,
            translation: String?,
            romanization: String?,
        ) {
            if (_state.value.isSaving) return
            _state.value = _state.value.copy(isSaving = true, errorMessage = null)
            viewModelScope.launch {
                try {
                    val document =
                        lyricsRepository.saveManualSelection(
                            songId = metadata.id,
                            lyrics = lyrics,
                            provider = provider,
                            translation = translation,
                            romanization = romanization,
                            artworkKey = metadata.thumbnailUrl,
                        ).withOffset(_state.value.offsetMs)
                    if (document.offsetMs != 0L) lyricsRepository.updateOffset(metadata.id, document.offsetMs)
                    synchronizationEngine.setDocument(document)
                    _state.value = document.toEditorState(metadata).copy(candidates = _state.value.candidates)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    _state.value = _state.value.copy(isSaving = false, errorMessage = error.userMessage())
                }
            }
        }

        private fun LyricsDocument?.toEditorState(metadata: MediaMetadata): LyricsEditorUiState =
            LyricsEditorUiState(
                metadata = metadata,
                offsetMs = this?.offsetMs ?: 0L,
                originalText = this?.original?.lines?.toEditableLrc().orEmpty(),
                translationText = this?.translation?.lines?.toEditableLrc().orEmpty(),
                romanizationText = this?.romanization?.lines?.toEditableLrc().orEmpty(),
            )

        private fun List<dev.vxs.frostsoulx.lyrics.core.LyricsLine>.toEditableLrc(): String =
            joinToString(separator = "\n") { line ->
                val minute = line.startMs / 60_000L
                val second = (line.startMs % 60_000L) / 1_000L
                val millisecond = line.startMs % 1_000L
                "[%02d:%02d.%03d]%s".format(minute, second, millisecond, line.text)
            }

        private fun Throwable.userMessage(): String = localizedMessage?.takeIf(String::isNotBlank) ?: "Lyrics could not be updated."

        private companion object {
            const val ManualProvider = "FrostSoul manual entry"
            const val MinimumOffsetMs = -30_000L
            const val MaximumOffsetMs = 30_000L
        }
    }
