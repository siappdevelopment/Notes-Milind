package com.notebook.notes.checklist.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.attachment.AttachmentIntentHelper
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.repository.AttachmentRepository
import com.notebook.notes.checklist.repository.ImportedAttachment
import com.notebook.notes.checklist.voice.VoiceNotePlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AttachmentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeBlockId: Long? = null,
    val isPlaying: Boolean = false,
    val isPlaybackPaused: Boolean = false,
    val currentPosition: Long = 0,
    val playbackDuration: Long = 0
)

@HiltViewModel
class AttachmentViewModel @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val mediaPlayer: VoiceNotePlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttachmentUiState())
    val uiState: StateFlow<AttachmentUiState> = _uiState.asStateFlow()

    private var playbackTimerJob: Job? = null
    private var activeFilePath: String? = null

    fun importAttachment(
        uri: Uri,
        onImported: (ImportedAttachment, AttachmentMetadata) -> Unit
    ) {
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            attachmentRepository.importFromUri(uri).fold(
                onSuccess = { imported ->
                    _uiState.update { it.copy(isLoading = false) }
                    onImported(imported, attachmentRepository.toMetadata(imported))
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = error.message ?: "import_failed")
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun toggleAudioPlayback(blockId: Long, filePath: String, durationMs: Long) {
        val state = _uiState.value
        if (state.activeBlockId == blockId) {
            when {
                state.isPlaying -> {
                    pausePlayback()
                    return
                }
                state.isPlaybackPaused -> {
                    resumePlayback()
                    return
                }
                else -> {
                    // Finished → restart from beginning
                    if (mediaPlayer.hasActiveSession && activeFilePath == filePath) {
                        try {
                            mediaPlayer.seekTo(0)
                            mediaPlayer.resume()
                            val total = mediaPlayer.duration.takeIf { it > 0 }?.toLong() ?: durationMs
                            _uiState.update {
                                it.copy(
                                    isPlaying = true,
                                    isPlaybackPaused = false,
                                    currentPosition = 0,
                                    playbackDuration = total,
                                    error = null
                                )
                            }
                            startPlaybackTimer(total)
                            return
                        } catch (_: Exception) {
                            // fall through
                        }
                    }
                    startPlayback(blockId, filePath, durationMs, 0L)
                    return
                }
            }
        }
        startPlayback(blockId, filePath, durationMs, 0L)
    }

    fun seekAudio(blockId: Long, filePath: String, durationMs: Long, positionMs: Long) {
        val state = _uiState.value
        val total = (if (state.activeBlockId == blockId && state.playbackDuration > 0) {
            state.playbackDuration
        } else {
            durationMs
        }).coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, total)

        if (state.activeBlockId != blockId || !mediaPlayer.hasActiveSession) {
            startPlayback(blockId, filePath, durationMs, clamped)
            return
        }

        mediaPlayer.seekTo(clamped.toInt())
        _uiState.update { it.copy(currentPosition = clamped) }

        when {
            state.isPlaying -> {
                if (!mediaPlayer.isPlaying) mediaPlayer.resume()
                startPlaybackTimer(total)
            }
            state.isPlaybackPaused -> {
                // Keep paused at new scrubbed position
            }
            else -> {
                // Idle/completed → start from scrubbed position
                mediaPlayer.resume()
                _uiState.update { it.copy(isPlaying = true, isPlaybackPaused = false) }
                startPlaybackTimer(total)
            }
        }
    }

    fun pausePlayback() {
        val state = _uiState.value
        if (!state.isPlaying) return
        mediaPlayer.pause()
        playbackTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isPlaying = false,
                isPlaybackPaused = true,
                currentPosition = mediaPlayer.currentPosition.toLong()
            )
        }
    }

    fun resumePlayback() {
        val state = _uiState.value
        if (state.activeBlockId == null) return
        if (!state.isPlaybackPaused && !mediaPlayer.isCompleted) return
        try {
            if (!mediaPlayer.hasActiveSession) {
                val path = activeFilePath ?: return
                startPlayback(state.activeBlockId!!, path, state.playbackDuration, state.currentPosition)
                return
            }
            mediaPlayer.resume()
            _uiState.update {
                it.copy(isPlaying = true, isPlaybackPaused = false, error = null)
            }
            startPlaybackTimer(state.playbackDuration)
        } catch (_: Exception) {
            _uiState.update { it.copy(error = "playback_failed") }
        }
    }

    fun stopPlayback() {
        stopPlaybackInternal(resetState = true)
    }

    fun refreshPlaybackPosition() {
        val state = _uiState.value
        if (state.activeBlockId == null) return
        _uiState.update {
            it.copy(
                currentPosition = mediaPlayer.currentPosition.toLong(),
                playbackDuration = mediaPlayer.duration.toLong().takeIf { d -> d > 0 }
                    ?: it.playbackDuration,
                isPlaying = mediaPlayer.isPlaying
            )
        }
    }

    fun openWithSystemApp(context: android.content.Context, filePath: String, mimeType: String) {
        AttachmentIntentHelper.openFile(context, filePath, mimeType).onFailure { error ->
            _uiState.update { it.copy(error = error.message ?: "open_failed") }
        }
    }

    private fun startPlayback(
        blockId: Long,
        filePath: String,
        durationMs: Long,
        positionMs: Long
    ) {
        stopPlaybackInternal(resetState = false)
        try {
            if (!attachmentRepository.fileExists(filePath)) {
                _uiState.update { it.copy(error = "missing_file") }
                return
            }
            activeFilePath = filePath
            mediaPlayer.play(filePath, positionMs.toInt()) {
                onPlaybackCompleted()
            }
            val totalDuration = mediaPlayer.duration.takeIf { it > 0 }?.toLong() ?: durationMs
            _uiState.update {
                it.copy(
                    activeBlockId = blockId,
                    isPlaying = true,
                    isPlaybackPaused = false,
                    currentPosition = positionMs,
                    playbackDuration = totalDuration,
                    error = null
                )
            }
            startPlaybackTimer(totalDuration)
        } catch (_: Exception) {
            stopPlaybackInternal(resetState = true)
            _uiState.update { it.copy(error = "playback_failed") }
        }
    }

    private fun onPlaybackCompleted() {
        playbackTimerJob?.cancel()
        mediaPlayer.resetToStart()
        _uiState.update {
            it.copy(
                isPlaying = false,
                isPlaybackPaused = false,
                currentPosition = 0
            )
        }
    }

    private fun stopPlaybackInternal(resetState: Boolean) {
        playbackTimerJob?.cancel()
        mediaPlayer.stop()
        activeFilePath = null
        if (resetState) {
            _uiState.update {
                it.copy(
                    activeBlockId = null,
                    isPlaying = false,
                    isPlaybackPaused = false,
                    currentPosition = 0,
                    playbackDuration = 0
                )
            }
        }
    }

    private fun startPlaybackTimer(totalDuration: Long) {
        playbackTimerJob?.cancel()
        playbackTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                val state = _uiState.value
                if (state.isPlaybackPaused) continue
                if (!mediaPlayer.isPlaying) {
                    // Completion callback owns terminal reset; keep looping briefly.
                    if (mediaPlayer.isCompleted || !mediaPlayer.hasActiveSession) break
                    continue
                }
                _uiState.update {
                    it.copy(
                        currentPosition = mediaPlayer.currentPosition.toLong(),
                        playbackDuration = mediaPlayer.duration.toLong().takeIf { d -> d > 0 }
                            ?: totalDuration,
                        isPlaying = true
                    )
                }
            }
        }
    }

    override fun onCleared() {
        playbackTimerJob?.cancel()
        mediaPlayer.stop()
        super.onCleared()
    }
}
