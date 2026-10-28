package com.notebook.notes.checklist.attachment

import android.webkit.MimeTypeMap
import com.notebook.notes.checklist.model.AttachmentCategory
import java.util.Locale

object AttachmentTypeDetector {

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "wav", "aac", "ogg", "flac", "3gp", "amr", "opus", "wma"
    )

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif"
    )

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "wmv", "m4v"
    )

    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
    )

    fun detect(mimeType: String?, fileName: String): AttachmentCategory {
        val extension = fileName.extension().lowercase(Locale.US)
        val normalizedMime = mimeType?.trim()?.lowercase(Locale.US).orEmpty()

        // Priority 1: usable ContentResolver MIME type
        categoryFromMime(normalizedMime)?.let { return it }

        // Priority 2: extension detection (also when MIME is missing/generic)
        categoryFromExtension(extension)?.let { return it }

        // Last resort: MimeTypeMap-derived MIME from extension
        val mappedMime = mimeFromExtension(extension).orEmpty()
        categoryFromMime(mappedMime)?.let { return it }

        if (isDocument(normalizedMime.ifBlank { mappedMime }, fileName)) {
            return AttachmentCategory.DOCUMENT
        }
        return AttachmentCategory.UNKNOWN
    }

    /**
     * Resolves a concrete MIME for storage/open intents.
     * Prefer ContentResolver MIME when specific; otherwise derive from extension/category.
     */
    fun resolveMimeType(mimeType: String?, fileName: String, category: AttachmentCategory): String {
        val normalized = mimeType?.trim().orEmpty()
        val lower = normalized.lowercase(Locale.US)
        if (normalized.isNotBlank() && !isGenericMime(normalized)) {
            // Keep provider MIME when it matches the resolved category (e.g. audio/*, application/ogg).
            val mimeMatchesCategory = when (category) {
                AttachmentCategory.AUDIO -> lower.startsWith("audio/") || isAudioMime(lower)
                AttachmentCategory.IMAGE -> lower.startsWith("image/")
                AttachmentCategory.VIDEO -> lower.startsWith("video/")
                AttachmentCategory.DOCUMENT, AttachmentCategory.UNKNOWN -> true
            }
            if (mimeMatchesCategory) return normalized
        }
        val extension = fileName.extension().lowercase(Locale.US)
        mimeFromExtension(extension)?.let { return it }
        return when (category) {
            AttachmentCategory.AUDIO -> "audio/*"
            AttachmentCategory.IMAGE -> "image/*"
            AttachmentCategory.VIDEO -> "video/*"
            AttachmentCategory.DOCUMENT -> "application/octet-stream"
            AttachmentCategory.UNKNOWN -> normalized.ifBlank { "application/octet-stream" }
        }
    }

    private fun categoryFromMime(mime: String): AttachmentCategory? {
        if (mime.isBlank() || isGenericMime(mime)) return null
        return when {
            mime.startsWith("image/") -> AttachmentCategory.IMAGE
            mime.startsWith("video/") -> AttachmentCategory.VIDEO
            mime.startsWith("audio/") || isAudioMime(mime) -> AttachmentCategory.AUDIO
            else -> null
        }
    }

    private fun categoryFromExtension(extension: String): AttachmentCategory? {
        if (extension.isBlank()) return null
        return when (extension) {
            in AUDIO_EXTENSIONS -> AttachmentCategory.AUDIO
            in IMAGE_EXTENSIONS -> AttachmentCategory.IMAGE
            in VIDEO_EXTENSIONS -> AttachmentCategory.VIDEO
            in DOCUMENT_EXTENSIONS -> AttachmentCategory.DOCUMENT
            else -> null
        }
    }

    private fun isGenericMime(mime: String): Boolean {
        val normalized = mime.lowercase(Locale.US)
        return normalized == "application/octet-stream" ||
            normalized == "application/download" ||
            normalized == "binary/octet-stream" ||
            normalized == "*/*"
    }

    /** MIME types that represent audio but do not use the audio/ prefix. */
    private fun isAudioMime(mime: String): Boolean =
        mime == "application/ogg" ||
            mime == "application/x-ogg" ||
            mime == "application/x-flac"

    private fun isDocument(mimeType: String, fileName: String): Boolean {
        if (mimeType == "application/pdf") return true
        if (mimeType.startsWith("text/")) return true
        val ext = fileName.extension().lowercase(Locale.US)
        return ext in DOCUMENT_EXTENSIONS ||
            mimeType.contains("word") ||
            mimeType.contains("excel") ||
            mimeType.contains("powerpoint") ||
            mimeType.contains("msword") ||
            mimeType.contains("spreadsheet") ||
            mimeType.contains("presentation")
    }

    private fun mimeFromExtension(extension: String): String? {
        if (extension.isBlank()) return null
        // Prefer explicit audio mappings — MimeTypeMap is incomplete on many devices
        // (e.g. m4a/aac/flac) and maps ogg to application/ogg.
        when (extension) {
            "mp3" -> return "audio/mpeg"
            "m4a" -> return "audio/mp4"
            "wav" -> return "audio/x-wav"
            "aac" -> return "audio/aac"
            "ogg", "opus" -> return "audio/ogg"
            "flac" -> return "audio/flac"
            "3gp" -> return "audio/3gpp"
            "amr" -> return "audio/amr"
            "wma" -> return "audio/x-ms-wma"
        }
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun String.extension(): String = substringAfterLast('.', "")
}
