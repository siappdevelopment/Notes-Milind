package com.notebook.notes.checklist.repository

import android.graphics.Bitmap
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.storage.FileStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrawingRepository @Inject constructor(
    private val fileStorageManager: FileStorageManager
) {

    suspend fun saveDrawing(bitmap: Bitmap, existingPath: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val stream = ByteArrayOutputStream()
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IllegalStateException("Failed to compress drawing bitmap")
                }
                val bytes = stream.toByteArray()
                if (existingPath != null) {
                    val file = File(existingPath)
                    if (file.exists()) {
                        file.writeBytes(bytes)
                        return@runCatching existingPath
                    }
                }
                fileStorageManager.saveBytes(bytes, BlockType.DRAWING, "png")
            }
        }

    fun deleteDrawingFile(path: String) {
        fileStorageManager.deleteFile(path)
    }

    fun fileExists(path: String): Boolean =
        fileStorageManager.getFile(path) != null
}
