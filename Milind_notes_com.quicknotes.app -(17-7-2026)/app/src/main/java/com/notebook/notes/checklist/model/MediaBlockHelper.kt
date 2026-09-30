package com.notebook.notes.checklist.model

object MediaBlockHelper {

    fun isGalleryMedia(block: NoteBlock): Boolean {
        return when (block.type) {
            BlockType.IMAGE, BlockType.DRAWING -> true
            BlockType.ATTACHMENT, BlockType.FILE -> {
                when (AttachmentMetadata.fromBlock(block)?.attachmentCategory()) {
                    AttachmentCategory.IMAGE, AttachmentCategory.VIDEO -> true
                    else -> false
                }
            }
            else -> false
        }
    }

    fun isVideo(block: NoteBlock): Boolean {
        return AttachmentMetadata.fromBlock(block)?.attachmentCategory() == AttachmentCategory.VIDEO
    }

    fun groupIntoRows(items: List<NoteContentItem>): List<NoteContentItem> {
        val result = mutableListOf<NoteContentItem>()
        val mediaBuffer = mutableListOf<NoteBlock>()

        fun flushMedia() {
            if (mediaBuffer.isEmpty()) return
            mediaBuffer.chunked(2).forEach { chunk ->
                result.add(NoteContentItem.MediaRow(chunk))
            }
            mediaBuffer.clear()
        }

        items.forEach { item ->
            when (item) {
                is NoteContentItem.BlockItem -> {
                    if (isGalleryMedia(item.block)) {
                        mediaBuffer.add(item.block)
                    } else {
                        flushMedia()
                        result.add(item)
                    }
                }
                else -> {
                    flushMedia()
                    result.add(item)
                }
            }
        }
        flushMedia()
        return result
    }
}
