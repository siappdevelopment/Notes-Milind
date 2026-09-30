package com.notebook.notes.checklist.model

import com.google.gson.Gson
import com.notebook.notes.checklist.attachment.AttachmentTypeDetector
import com.notebook.notes.checklist.util.DateTimeUtils

data class AttachmentMetadata(
    val fileName: String,
    val mimeType: String,
    val fileSize: Long,
    val category: String,
    val displayName: String? = null,
    val durationMs: Long = 0,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun attachmentCategory(): AttachmentCategory {
        val stored = AttachmentCategory.fromString(category)
        // Re-detect when older imports stored UNKNOWN despite an audio/image extension.
        if (stored != AttachmentCategory.UNKNOWN) return stored
        return AttachmentTypeDetector.detect(mimeType, fileName)
    }

    fun displayLabel(): String =
        displayName?.takeIf { it.isNotBlank() } ?: fileName

    companion object {
        private val gson = Gson()

        fun fromJson(json: String?): AttachmentMetadata? {
            if (json.isNullOrBlank()) return null
            return try {
                gson.fromJson(json, AttachmentMetadata::class.java)
            } catch (_: Exception) {
                null
            }
        }

        fun toJson(metadata: AttachmentMetadata): String = gson.toJson(metadata)

        fun fromBlock(block: NoteBlock): AttachmentMetadata? {
            fromJson(block.extraData)?.let { return it }
            if (block.type != BlockType.FILE && block.type != BlockType.ATTACHMENT) return null
            val rawName = block.extraData?.takeIf { !it.trimStart().startsWith("{") }
            val fileName = rawName ?: java.io.File(block.content).name
            val file = java.io.File(block.content)
            val category = AttachmentTypeDetector.detect(null, fileName)
            return AttachmentMetadata(
                fileName = fileName,
                mimeType = "application/octet-stream",
                fileSize = file.length(),
                category = category.name,
                displayName = fileName
            )
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.lastIndex) {
        size /= 1024
        unitIndex++
    }
    return if (unitIndex == 0) "${bytes.toInt()} ${units[unitIndex]}"
    else "%.1f %s".format(size, units[unitIndex])
}

fun formatAttachmentMeta(createdAt: Long, fileSize: Long): String {
    val date = DateTimeUtils.formatDate(createdAt)
    return "$date • ${formatFileSize(fileSize)}"
}
