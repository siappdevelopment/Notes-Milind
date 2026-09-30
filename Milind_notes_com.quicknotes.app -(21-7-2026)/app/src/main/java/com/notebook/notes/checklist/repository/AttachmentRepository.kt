package com.notebook.notes.checklist.repository

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.notebook.notes.checklist.attachment.AttachmentTypeDetector
import com.notebook.notes.checklist.data.AttachmentDao
import com.notebook.notes.checklist.model.Attachment
import com.notebook.notes.checklist.model.AttachmentCategory
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.storage.FileStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class ImportedAttachment(
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val category: AttachmentCategory,
    val durationMs: Long = 0,
    val thumbnailPath: String? = null
)

@Singleton
class AttachmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStorageManager: FileStorageManager,
    private val attachmentDao: AttachmentDao
) {

    suspend fun importFromUri(uri: Uri): Result<ImportedAttachment> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val rawMimeType = resolver.getType(uri)
            val displayName = resolveDisplayName(uri)
            // Detect before copy so audio lands in voice_notes alongside in-app recordings.
            val preliminaryCategory = AttachmentTypeDetector.detect(rawMimeType, displayName)
            val storageType = if (preliminaryCategory == AttachmentCategory.AUDIO) {
                BlockType.VOICE
            } else {
                BlockType.ATTACHMENT
            }
            val filePath = fileStorageManager.copyUriToStorage(uri, storageType, displayName)
            val file = File(filePath)
            if (!file.exists() || file.length() <= 0) {
                throw IllegalStateException("Unable to read selected file.")
            }
            // Prefer original display name for type detection; fall back to copied file name
            // so extension-based detection still works if the provider omitted DISPLAY_NAME.
            val nameForDetection = displayName
                .takeIf { it.contains('.') }
                ?: file.name
            val category = AttachmentTypeDetector.detect(rawMimeType, nameForDetection)
            val mimeType = AttachmentTypeDetector.resolveMimeType(rawMimeType, nameForDetection, category)
            val durationMs = if (category == AttachmentCategory.VIDEO || category == AttachmentCategory.AUDIO) {
                readMediaDuration(filePath)
            } else 0L
            val thumbnailPath = if (category == AttachmentCategory.VIDEO) {
                saveVideoThumbnail(filePath)
            } else null
            ImportedAttachment(
                filePath = filePath,
                fileName = displayName,
                mimeType = mimeType,
                fileSize = file.length(),
                category = category,
                durationMs = durationMs,
                thumbnailPath = thumbnailPath
            )
        }
    }

    fun toMetadata(imported: ImportedAttachment): AttachmentMetadata =
        AttachmentMetadata(
            fileName = imported.fileName,
            mimeType = imported.mimeType,
            fileSize = imported.fileSize,
            category = imported.category.name,
            durationMs = imported.durationMs,
            thumbnailPath = imported.thumbnailPath
        )

    suspend fun syncAttachmentsForNote(noteId: Long, blocks: List<NoteBlock>) {
        withContext(Dispatchers.IO) {
            attachmentDao.deleteAttachmentsForNote(noteId)
            val attachments = blocks.filter {
                it.type == BlockType.ATTACHMENT || it.type == BlockType.FILE
            }.mapNotNull { block ->
                val metadata = AttachmentMetadata.fromBlock(block) ?: return@mapNotNull null
                Attachment(
                    noteId = noteId,
                    fileName = metadata.displayLabel(),
                    fileUri = block.content,
                    mimeType = metadata.mimeType,
                    fileSize = metadata.fileSize,
                    createdAt = metadata.createdAt
                )
            }
            if (attachments.isNotEmpty()) {
                attachmentDao.insertAttachments(attachments)
            }
        }
    }

    fun deleteAttachmentFile(path: String) {
        fileStorageManager.deleteFile(path)
    }

    fun fileExists(path: String): Boolean = fileStorageManager.getFile(path) != null

    private fun resolveDisplayName(uri: Uri): String {
        queryDisplayName(uri)?.takeIf { it.isNotBlank() }?.let { return it }
        uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringBefore('?')
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return "attachment"
    }

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index >= 0) cursor.getString(index) else null
        }

    private fun readMediaDuration(filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    private fun saveVideoThumbnail(filePath: String): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val bitmap = retriever.getFrameAtTime(0) ?: return null
            val bytes = bitmap.toPngBytes() ?: return null
            fileStorageManager.saveBytes(bytes, "thumbnails", "png")
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun Bitmap.toPngBytes(): ByteArray? {
        val stream = java.io.ByteArrayOutputStream()
        return if (compress(Bitmap.CompressFormat.PNG, 90, stream)) stream.toByteArray() else null
    }
}
