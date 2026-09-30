package com.notebook.notes.checklist.storage

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val imagesDir: File by lazy { ensureDir("images") }
    private val filesDir: File by lazy { ensureDir("files") }
    private val drawingsDir: File by lazy { ensureDir("drawings") }
    private val voiceNotesDir: File by lazy { ensureDir("voice_notes") }

    private val attachmentsDir: File by lazy { ensureDir("attachments") }
    private val thumbnailsDir: File by lazy { ensureDir("thumbnails") }

    fun copyUriToStorage(uri: Uri, type: String, displayName: String? = null): String {
        val targetDir = when (type) {
            "IMAGE" -> imagesDir
            "FILE" -> filesDir
            "DRAWING" -> drawingsDir
            "VOICE" -> voiceNotesDir
            "ATTACHMENT" -> attachmentsDir
            else -> filesDir
        }
        val extension = displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
            ?: context.contentResolver.getType(uri)?.substringAfterLast('/')
            ?: "dat"
        val fileName = "${UUID.randomUUID()}.$extension"
        val destFile = File(targetDir, fileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile.absolutePath
    }

    fun saveBytes(bytes: ByteArray, type: String, extension: String): String {
        val targetDir = when (type) {
            "DRAWING" -> drawingsDir
            "VOICE" -> voiceNotesDir
            "IMAGE" -> imagesDir
            "ATTACHMENT" -> attachmentsDir
            "thumbnails" -> thumbnailsDir
            else -> filesDir
        }
        val fileName = "${UUID.randomUUID()}.$extension"
        val destFile = File(targetDir, fileName)
        destFile.writeBytes(bytes)
        return destFile.absolutePath
    }

    fun getFile(path: String): File? {
        val file = File(path)
        return file.takeIf { it.exists() }
    }

    fun createVoiceRecordingFile(): File {
        val fileName = "voice_${UUID.randomUUID()}.m4a"
        return File(voiceNotesDir, fileName)
    }

    fun deleteFile(path: String): Boolean {
        val file = File(path)
        return if (file.exists()) file.delete() else true
    }

    private fun ensureDir(name: String): File {
        return File(context.filesDir, name).apply { mkdirs() }
    }
}
