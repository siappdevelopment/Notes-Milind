package com.notebook.notes.checklist.model

import com.google.gson.Gson

data class VoiceNoteMetadata(
    val displayName: String? = null,
    val durationMs: Long = 0,
    val fileSize: Long = 0,
    val waveform: List<Float> = emptyList()
) {
    fun displayLabel(fallback: String): String =
        displayName?.takeIf { it.isNotBlank() } ?: fallback

    companion object {
        private val gson = Gson()

        fun fromJson(json: String?): VoiceNoteMetadata {
            if (json.isNullOrBlank()) return VoiceNoteMetadata()
            return try {
                gson.fromJson(json, VoiceNoteMetadata::class.java) ?: VoiceNoteMetadata()
            } catch (_: Exception) {
                VoiceNoteMetadata()
            }
        }

        fun toJson(metadata: VoiceNoteMetadata): String = gson.toJson(metadata)
    }
}

fun formatVoiceDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
