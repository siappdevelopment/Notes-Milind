package com.notebook.notes.checklist.model

import java.io.File

object MediaGalleryHelper {

    fun collectViewableItems(blocks: List<NoteBlock>): List<MediaViewerItem> {
        return blocks
            .filter { block ->
                MediaBlockHelper.isGalleryMedia(block) && !MediaBlockHelper.isVideo(block)
            }
            .sortedBy { it.position }
            .map(::toViewerItem)
    }

    fun indexOfPath(items: List<MediaViewerItem>, path: String): Int =
        items.indexOfFirst { it.path == path }.coerceAtLeast(0)

    private fun toViewerItem(block: NoteBlock): MediaViewerItem {
        val metadata = AttachmentMetadata.fromBlock(block)
        val file = File(block.content)
        val mimeType = when {
            metadata != null -> metadata.mimeType
            block.type == BlockType.DRAWING -> "image/png"
            else -> "image/*"
        }
        val title = when {
            metadata != null -> metadata.displayLabel()
            file.exists() -> file.name
            else -> ""
        }
        return MediaViewerItem(
            blockId = block.id,
            path = block.content,
            title = title,
            mimeType = mimeType
        )
    }
}
