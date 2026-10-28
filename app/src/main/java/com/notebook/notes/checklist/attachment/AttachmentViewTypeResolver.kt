package com.notebook.notes.checklist.attachment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.notebook.notes.checklist.model.AttachmentCategory
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.NoteBlock
import java.io.File

object AttachmentIntentHelper {

    fun openFile(context: Context, filePath: String, mimeType: String): Result<Unit> = runCatching {
        val file = File(filePath)
        if (!file.exists()) throw IllegalStateException("missing_file")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType.ifBlank { "*/*" })
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            throw IllegalStateException("no_viewer")
        }
        context.startActivity(intent)
    }

    fun shareFile(context: Context, filePath: String, mimeType: String): Result<Unit> = runCatching {
        val file = File(filePath)
        if (!file.exists()) throw IllegalStateException("missing_file")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType.ifBlank { "*/*" }
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}

object AttachmentViewTypeResolver {

    const val VIEW_ATTACHMENT_IMAGE = 10
    const val VIEW_ATTACHMENT_VIDEO = 11
    const val VIEW_ATTACHMENT_AUDIO = 12
    const val VIEW_ATTACHMENT_DOCUMENT = 13
    const val VIEW_ATTACHMENT_GENERIC = 14

    fun viewTypeForBlock(block: NoteBlock): Int {
        val metadata = AttachmentMetadata.fromBlock(block) ?: return VIEW_ATTACHMENT_GENERIC
        return when (metadata.attachmentCategory()) {
            AttachmentCategory.IMAGE -> VIEW_ATTACHMENT_IMAGE
            AttachmentCategory.VIDEO -> VIEW_ATTACHMENT_VIDEO
            // Picked audio uses the same file-row UI as other attachments;
            // tap opens the system / default audio player.
            AttachmentCategory.AUDIO -> VIEW_ATTACHMENT_GENERIC
            AttachmentCategory.DOCUMENT -> VIEW_ATTACHMENT_DOCUMENT
            AttachmentCategory.UNKNOWN -> VIEW_ATTACHMENT_GENERIC
        }
    }

    fun isAttachmentBlock(block: NoteBlock): Boolean =
        block.type == BlockType.ATTACHMENT || block.type == BlockType.FILE
}
