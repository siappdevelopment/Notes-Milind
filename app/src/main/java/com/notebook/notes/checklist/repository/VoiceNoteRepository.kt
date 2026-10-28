package com.notebook.notes.checklist.repository

import com.notebook.notes.checklist.model.VoiceNoteMetadata
import com.notebook.notes.checklist.storage.FileStorageManager
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceNoteRepository @Inject constructor(
    private val fileStorageManager: FileStorageManager
) {

    fun createRecordingFile(): File = fileStorageManager.createVoiceRecordingFile()

    fun buildMetadata(file: File, durationMs: Long): VoiceNoteMetadata =
        VoiceNoteMetadata(
            durationMs = durationMs,
            fileSize = file.length()
        )

    fun deleteVoiceFile(path: String) {
        fileStorageManager.deleteFile(path)
    }

    fun fileExists(path: String): Boolean =
        fileStorageManager.getFile(path) != null
}
