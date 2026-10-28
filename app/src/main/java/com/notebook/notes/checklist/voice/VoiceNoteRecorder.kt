package com.notebook.notes.checklist.voice

import android.media.MediaRecorder
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceNoteRecorder @Inject constructor() {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isActive: Boolean
        get() = mediaRecorder != null

    fun start(output: File) {
        release()
        outputFile = output
        val recorder = MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioEncodingBitRate(128_000)
        recorder.setAudioSamplingRate(44_100)
        recorder.setOutputFile(output.absolutePath)
        recorder.prepare()
        recorder.start()
        mediaRecorder = recorder
    }

    fun pause() {
        mediaRecorder?.pause()
    }

    fun resume() {
        mediaRecorder?.resume()
    }

    fun stop(): File? {
        return try {
            mediaRecorder?.stop()
            outputFile
        } catch (_: Exception) {
            outputFile?.delete()
            null
        } finally {
            release()
        }
    }

    fun getMaxAmplitude(): Int {
        return try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }

    fun release() {
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
            // ignore
        }
        mediaRecorder = null
    }
}
