package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.repository.VoiceNoteRepository
import com.notebook.notes.checklist.ui.audio.VoiceWaveformHelper
import com.notebook.notes.checklist.voice.VoiceNotePlayer
import com.notebook.notes.checklist.voice.VoiceNoteRecorder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class VoiceNoteUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val isPlaying: Boolean = false,
    val isPlaybackPaused: Boolean = false,
    val currentDuration: Long = 0,
    val recordingAmplitude: Int = 0,
    val currentPosition: Long = 0,
    val playbackDuration: Long = 0,
    val activeBlockId: Long? = null,
    val error: String? = null,
    val completedRecording: CompletedRecording? = null
)

data class CompletedRecording(
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long,
    val waveform: List<Float> = emptyList()
)

@HiltViewModel
class VoiceNoteViewModel @Inject constructor(
    private val voiceNoteRepository: VoiceNoteRepository,
    private val recorder: VoiceNoteRecorder,
    private val player: VoiceNotePlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceNoteUiState())
    val uiState: StateFlow<VoiceNoteUiState> = _uiState.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var playbackTimerJob: Job? = null
    /** Prevents an old queued start from reclaiming the shared audio player. */
    private var playbackStartJob: Job? = null
    private var recordingStartMs: Long = 0
    private var accumulatedRecordingMs: Long = 0
    private var recordingOutputFile: File? = null
    private var activePlaybackPath: String? = null
    private val recordingSamples = mutableListOf<Float>()

    fun startRecording() {
        if (_uiState.value.isRecording) return
        viewModelScope.launch {
            try {
                val file = voiceNoteRepository.createRecordingFile()
                recordingOutputFile = file
                recorder.start(file)
                accumulatedRecordingMs = 0
                recordingSamples.clear()
                recordingStartMs = System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        isRecording = true,
                        isPaused = false,
                        currentDuration = 0,
                        error = null,
                        completedRecording = null
                    )
                }
                startRecordingTimer()
            } catch (e: Exception) {
                recorder.release()
                recordingOutputFile = null
                _uiState.update {
                    it.copy(error = e.message ?: "Recording failed")
                }
            }
        }
    }

    fun pauseRecording() {
        if (!_uiState.value.isRecording || _uiState.value.isPaused) return
        try {
            recorder.pause()
            accumulatedRecordingMs += System.currentTimeMillis() - recordingStartMs
            recordingTimerJob?.cancel()
            _uiState.update { it.copy(isPaused = true) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to pause recording") }
        }
    }

    fun resumeRecording() {
        if (!_uiState.value.isRecording || !_uiState.value.isPaused) return
        try {
            recorder.resume()
            recordingStartMs = System.currentTimeMillis()
            _uiState.update { it.copy(isPaused = false) }
            startRecordingTimer()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Failed to resume recording") }
        }
    }

    fun cancelRecording() {
        if (!_uiState.value.isRecording) return
        recordingTimerJob?.cancel()
        try {
            recorder.stop()
        } catch (_: Exception) {
            // ignore
        }
        recordingOutputFile?.delete()
        recordingOutputFile = null
        accumulatedRecordingMs = 0
        recordingSamples.clear()
        _uiState.update {
            it.copy(
                isRecording = false,
                isPaused = false,
                currentDuration = 0,
                recordingAmplitude = 0,
                completedRecording = null
            )
        }
    }

    fun stopRecording() {
        if (!_uiState.value.isRecording) return
        recordingTimerJob?.cancel()
        val duration = currentRecordingDuration()
        val file = try {
            recorder.stop()
        } catch (e: Exception) {
            recordingOutputFile?.delete()
            null
        }
        recordingOutputFile = null
        accumulatedRecordingMs = 0

        if (file == null || !file.exists() || file.length() == 0L) {
            file?.delete()
            _uiState.update {
                it.copy(
                    isRecording = false,
                    isPaused = false,
                    currentDuration = 0,
                    error = "Recording failed"
                )
            }
            return
        }

        val metadata = voiceNoteRepository.buildMetadata(file, duration)
        val waveform = recordingSamples.toList()
        recordingSamples.clear()
        _uiState.update {
            it.copy(
                isRecording = false,
                isPaused = false,
                currentDuration = 0,
                recordingAmplitude = 0,
                completedRecording = CompletedRecording(
                    filePath = file.absolutePath,
                    durationMs = metadata.durationMs,
                    fileSize = metadata.fileSize,
                    waveform = waveform
                )
            )
        }
    }

    fun clearCompletedRecording() {
        _uiState.update { it.copy(completedRecording = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun togglePlayback(blockId: Long, filePath: String, durationMs: Long) {
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
                    // Finished / idle on same item → restart from beginning without opening note again
                    replayFromStart(blockId, filePath, durationMs)
                    return
                }
            }
        }
        startPlayback(blockId, filePath, durationMs, 0)
    }

    /** Restart finished audio from 00:00, reusing session when possible. */
    private fun replayFromStart(blockId: Long, filePath: String, durationMs: Long) {
        if (player.hasActiveSession && activePlaybackPath == filePath) {
            try {
                player.seekTo(0)
                if (!tryResumePlayer()) return
                val totalDuration = player.duration.takeIf { it > 0 }?.toLong() ?: durationMs
                _uiState.update {
                    it.copy(
                        activeBlockId = blockId,
                        isPlaying = true,
                        isPlaybackPaused = false,
                        currentPosition = 0,
                        playbackDuration = totalDuration,
                        error = null
                    )
                }
                startPlaybackTimer(totalDuration)
                return
            } catch (_: Exception) {
                // Fall through to full start
            }
        }
        startPlayback(blockId, filePath, durationMs, 0)
    }

    fun startPlayback(blockId: Long, filePath: String, durationMs: Long, positionMs: Long = 0) {
        stopPlaybackInternal(resetState = false)
        playbackStartJob = viewModelScope.launch {
            try {
                if (!voiceNoteRepository.fileExists(filePath)) {
                    _uiState.update { it.copy(error = "Audio file not found") }
                    return@launch
                }
                activePlaybackPath = filePath
                player.play(filePath, positionMs.toInt()) {
                    onPlaybackCompleted()
                }
                val totalDuration = player.duration.takeIf { it > 0 }?.toLong() ?: durationMs
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
            } catch (e: Exception) {
                // This coroutine is the current start request; clean up directly
                // rather than cancelling the coroutine from inside its own catch block.
                player.stop()
                activePlaybackPath = null
                _uiState.update {
                    it.copy(
                        isPlaying = false,
                        isPlaybackPaused = false,
                        currentPosition = 0,
                        playbackDuration = 0,
                        activeBlockId = null
                    )
                }
                _uiState.update { it.copy(error = e.message ?: "Playback failed") }
            }
        }
    }

    fun pausePlayback() {
        if (!_uiState.value.isPlaying) return
        player.pause()
        playbackTimerJob?.cancel()
        _uiState.update {
            it.copy(
                isPlaying = false,
                isPlaybackPaused = true,
                currentPosition = player.currentPosition.toLong()
            )
        }
    }

    fun resumePlayback() {
        val state = _uiState.value
        if (state.activeBlockId == null) return
        if (!state.isPlaybackPaused && !player.isCompleted) return
        val path = activePlaybackPath
        if (!player.hasActiveSession) {
            if (path.isNullOrBlank()) return
            startPlayback(state.activeBlockId!!, path, state.playbackDuration, state.currentPosition)
            return
        }
        try {
            if (!tryResumePlayer()) return
            _uiState.update { it.copy(isPlaying = true, isPlaybackPaused = false) }
            startPlaybackTimer(state.playbackDuration)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Playback failed") }
        }
    }

    /**
     * Seek audio to [positionMs].
     * Playing → jump and continue. Paused → keep paused at new position.
     * Idle/completed → jump and start from that position when [startIfIdle] is true.
     */
    fun seekPlayback(
        positionMs: Long,
        resumeIfPaused: Boolean = false,
        startIfIdle: Boolean = true
    ) {
        val state = _uiState.value
        val duration = state.playbackDuration.takeIf { it > 0 }
            ?: player.duration.toLong().takeIf { it > 0 }
            ?: Long.MAX_VALUE
        val clamped = positionMs.coerceIn(0L, if (duration == Long.MAX_VALUE) positionMs.coerceAtLeast(0L) else duration)

        if (state.activeBlockId == null || !player.hasActiveSession) {
            val path = activePlaybackPath
            val blockId = state.activeBlockId
            if (blockId != null && !path.isNullOrBlank()) {
                startPlayback(blockId, path, state.playbackDuration, clamped)
            }
            return
        }

        player.seekTo(clamped.toInt())
        _uiState.update { it.copy(currentPosition = clamped) }

        when {
            state.isPlaying || resumeIfPaused -> {
                if (!player.isPlaying && !tryResumePlayer()) return
                _uiState.update { it.copy(isPlaying = true, isPlaybackPaused = false) }
                startPlaybackTimer(state.playbackDuration)
            }
            state.isPlaybackPaused -> {
                // Stay paused at new position
            }
            startIfIdle -> {
                // Completed / stopped → start from scrubbed position
                if (!tryResumePlayer()) return
                _uiState.update { it.copy(isPlaying = true, isPlaybackPaused = false) }
                startPlaybackTimer(state.playbackDuration)
            }
        }
    }

    fun stopPlayback() {
        stopPlaybackInternal(resetState = true)
    }

    private fun onPlaybackCompleted() {
        playbackTimerJob?.cancel()
        player.resetToStart()
        _uiState.update {
            it.copy(
                isPlaying = false,
                isPlaybackPaused = false,
                currentPosition = 0
            )
        }
    }

    /** Converts MediaPlayer state failures into visible UI state instead of a crash or fake play state. */
    private fun tryResumePlayer(): Boolean = try {
        player.resume().also { started ->
            if (!started) _uiState.update { it.copy(error = "Playback failed") }
        }
    } catch (e: Exception) {
        _uiState.update { it.copy(error = e.message ?: "Playback failed") }
        false
    }

    private fun stopPlaybackInternal(resetState: Boolean) {
        playbackStartJob?.cancel()
        playbackStartJob = null
        playbackTimerJob?.cancel()
        player.stop()
        if (resetState) {
            activePlaybackPath = null
            _uiState.update {
                it.copy(
                    isPlaying = false,
                    isPlaybackPaused = false,
                    currentPosition = 0,
                    playbackDuration = 0,
                    activeBlockId = null
                )
            }
        }
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(100)
                if (!_uiState.value.isPaused) {
                    val amplitude = recorder.getMaxAmplitude()
                    recordingSamples.add(VoiceWaveformHelper.normalizeAmplitude(amplitude))
                    _uiState.update {
                        it.copy(
                            currentDuration = currentRecordingDuration(),
                            recordingAmplitude = amplitude
                        )
                    }
                }
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
                if (!player.isPlaying) {
                    if (player.isCompleted || !player.hasActiveSession) break
                    continue
                }
                _uiState.update {
                    it.copy(
                        currentPosition = player.currentPosition.toLong(),
                        playbackDuration = player.duration.takeIf { d -> d > 0 }?.toLong()
                            ?: totalDuration
                    )
                }
            }
        }
    }

    private fun currentRecordingDuration(): Long {
        val elapsed = if (_uiState.value.isPaused) {
            0L
        } else {
            System.currentTimeMillis() - recordingStartMs
        }
        return accumulatedRecordingMs + elapsed
    }

    override fun onCleared() {
        recordingTimerJob?.cancel()
        playbackTimerJob?.cancel()
        playbackStartJob?.cancel()
        if (_uiState.value.isRecording) {
            recorder.stop()
        }
        recorder.release()
        player.release()
        super.onCleared()
    }
}
