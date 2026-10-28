package com.notebook.notes.checklist.voice

import android.media.MediaPlayer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single shared MediaPlayer for exclusive voice/attachment audio playback.
 * Timestamps/positions are retained across pause/resume; completion resets to 0
 * and leaves the player ready for replay without requiring a full release.
 */
@Singleton
class VoiceNotePlayer @Inject constructor() {

    private var mediaPlayer: MediaPlayer? = null
    private var activePath: String? = null
    private var completed: Boolean = false
    private var onCompletionCallback: (() -> Unit)? = null

    val isPlaying: Boolean
        get() = mediaPlayer?.isPlaying == true

    val hasActiveSession: Boolean
        get() = mediaPlayer != null && activePath != null

    val isCompleted: Boolean
        get() = completed

    val currentPosition: Int
        get() = mediaPlayer?.currentPosition?.coerceAtLeast(0) ?: 0

    val duration: Int
        get() = mediaPlayer?.duration?.takeIf { it > 0 } ?: 0

    fun play(
        filePath: String,
        startPositionMs: Int = 0,
        onCompletion: () -> Unit
    ) {
        onCompletionCallback = onCompletion
        if (activePath == filePath && mediaPlayer != null) {
            completed = false
            seekInternal(startPositionMs)
            startInternal()
            return
        }
        releaseInternal(keepCallback = true)
        val file = File(filePath)
        if (!file.exists()) throw IllegalStateException("Audio file not found")
        activePath = filePath
        completed = false
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            setOnCompletionListener {
                completed = true
                try {
                    seekTo(0)
                } catch (_: Exception) {
                    // ignore
                }
                onCompletionCallback?.invoke()
            }
            prepare()
            seekInternal(startPositionMs, this)
            start()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        }
    }

    /** Returns false if the player cannot actually enter the playing state. */
    fun resume(): Boolean {
        val player = mediaPlayer ?: return false
        if (completed) {
            completed = false
            seekInternal(0, player)
        }
        startInternal(player)
        return player.isPlaying
    }

    fun seekTo(positionMs: Int) {
        val player = mediaPlayer ?: return
        completed = false
        seekInternal(positionMs, player)
    }

    /** Soft stop: pause and seek to 0, keep session for replay. */
    fun resetToStart() {
        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) player.pause()
            seekInternal(0, player)
        } catch (_: Exception) {
            // ignore
        }
        completed = true
    }

    fun stop() {
        releaseInternal(keepCallback = false)
    }

    fun release() {
        releaseInternal(keepCallback = false)
    }

    private fun startInternal(player: MediaPlayer? = mediaPlayer) {
        val target = player ?: throw IllegalStateException("Audio player is not ready")
        target.start()
    }

    private fun seekInternal(positionMs: Int, player: MediaPlayer? = mediaPlayer) {
        val target = player ?: return
        val pos = positionMs.coerceAtLeast(0)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                target.seekTo(pos.toLong(), MediaPlayer.SEEK_CLOSEST)
            } else {
                target.seekTo(pos)
            }
        } catch (_: IllegalStateException) {
            // ignore invalid seek states
        }
    }

    private fun releaseInternal(keepCallback: Boolean) {
        try {
            mediaPlayer?.setOnCompletionListener(null)
            mediaPlayer?.release()
        } catch (_: Exception) {
            // ignore
        }
        mediaPlayer = null
        activePath = null
        completed = false
        if (!keepCallback) {
            onCompletionCallback = null
        }
    }
}
