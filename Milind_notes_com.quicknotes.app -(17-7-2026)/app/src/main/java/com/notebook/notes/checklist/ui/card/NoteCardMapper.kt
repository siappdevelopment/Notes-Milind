package com.notebook.notes.checklist.ui.card

import android.content.Context
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.attachment.AttachmentTypeDetector
import com.notebook.notes.checklist.data.AttachmentDao
import com.notebook.notes.checklist.model.Attachment
import com.notebook.notes.checklist.model.AttachmentCategory
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.Note
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.NumberListHelper
import com.notebook.notes.checklist.model.NumberListMetadata
import com.notebook.notes.checklist.model.TodoItem
import com.notebook.notes.checklist.repository.CategoryRepository
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.repository.TodoRepository
import com.notebook.notes.checklist.ui.model.CardBackgroundType
import com.notebook.notes.checklist.ui.model.ImagePreviewContent
import com.notebook.notes.checklist.ui.model.MixedPreviewContent
import com.notebook.notes.checklist.ui.model.NoteCardUiModel
import com.notebook.notes.checklist.ui.model.BulletListPreviewContent
import com.notebook.notes.checklist.ui.model.BulletListPreviewItem
import com.notebook.notes.checklist.ui.model.NumberListPreviewContent
import com.notebook.notes.checklist.ui.model.NumberListPreviewItem
import com.notebook.notes.checklist.ui.model.PreviewType
import com.notebook.notes.checklist.ui.model.TextPreviewContent
import com.notebook.notes.checklist.ui.model.TodoPreviewContent
import com.notebook.notes.checklist.ui.model.TodoPreviewItem
import com.notebook.notes.checklist.util.CategoryIconResolver
import com.notebook.notes.checklist.util.NoteTitleHelper
import com.notebook.notes.checklist.utils.JsonUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteCardMapper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val noteRepository: NoteRepository,
    private val todoRepository: TodoRepository,
    private val categoryRepository: CategoryRepository,
    private val attachmentDao: AttachmentDao
) {

    suspend fun map(note: Note): NoteCardUiModel {
        val category = categoryRepository.getCategoryById(note.categoryId)

        // Locked cards must not bind title/body/todos/media — only theme + lock UI.
        if (note.isLocked) {
            val (backgroundType, backgroundKey) = resolveBackground(note, PreviewType.LOCKED)
            return NoteCardUiModel(
                note = note,
                noteId = note.id,
                position = note.position,
                title = "",
                categoryName = "",
                categoryIconRes = CategoryIconResolver.resolve(category?.icon, category?.name),
                backgroundType = backgroundType,
                backgroundKey = backgroundKey,
                previewType = PreviewType.LOCKED,
                previewContent = null,
                isPinned = note.isPinned,
                isLocked = true,
                isFavorite = note.isFavorite,
                updatedAt = note.updatedAt
            )
        }

        val blocks = noteRepository.getBlocksOnce(note.id)
        val todoItems = loadTodoItems(note.id, blocks)
        val numberListItems = loadNumberListItems(blocks)
        val bulletListItems = loadBulletListItems(blocks)
        val attachments = attachmentDao.getAttachmentsForNote(note.id)

        val hasTodos = todoItems.isNotEmpty()
        val hasNumberList = numberListItems.isNotEmpty()
        val hasBulletList = bulletListItems.isNotEmpty()
        val imageUri = findFirstImageUri(blocks, attachments)
        val hasImage = imageUri != null
        val hasAttachedAudio = blocks.any { block ->
            if (block.type != BlockType.ATTACHMENT && block.type != BlockType.FILE) return@any false
            AttachmentMetadata.fromBlock(block)?.attachmentCategory() == AttachmentCategory.AUDIO
        } || attachments.any {
            AttachmentTypeDetector.detect(it.mimeType, it.fileName) == AttachmentCategory.AUDIO
        }
        // Attached audio files are not documents — do not show the PDF badge for them.
        val hasVoice = blocks.any { it.type == BlockType.VOICE } || hasAttachedAudio
        val hasDocument = blocks.any { block ->
            if (block.type != BlockType.ATTACHMENT && block.type != BlockType.FILE) return@any false
            AttachmentMetadata.fromBlock(block)?.attachmentCategory() == AttachmentCategory.DOCUMENT
        } || attachments.any {
            AttachmentTypeDetector.detect(it.mimeType, it.fileName) == AttachmentCategory.DOCUMENT
        }
        val textSnippet = extractTextPreview(blocks)

        val previewType = resolvePreviewType(
            hasTodos = hasTodos,
            hasNumberList = hasNumberList,
            hasBulletList = hasBulletList,
            hasImage = hasImage,
            hasVoice = hasVoice,
            hasDocument = hasDocument,
            hasText = textSnippet.isNotBlank()
        )

        val previewContent = buildPreviewContent(
            previewType = previewType,
            todoItems = todoItems,
            numberListItems = numberListItems,
            bulletListItems = bulletListItems,
            imageUri = imageUri,
            textSnippet = textSnippet,
            hasImage = hasImage,
            hasVoice = hasVoice,
            hasDocument = hasDocument
        )

        val (backgroundType, backgroundKey) = resolveBackground(note, previewType)

        return NoteCardUiModel(
            note = note,
            noteId = note.id,
            position = note.position,
            title = NoteTitleHelper.displayTitle(context, note.title),
            categoryName = category?.name.orEmpty(),
            categoryIconRes = CategoryIconResolver.resolve(category?.icon, category?.name),
            backgroundType = backgroundType,
            backgroundKey = backgroundKey,
            previewType = previewType,
            previewContent = previewContent,
            isPinned = note.isPinned,
            isLocked = false,
            isFavorite = note.isFavorite,
            updatedAt = note.updatedAt
        )
    }

    suspend fun mapAll(notes: List<Note>): List<NoteCardUiModel> = notes.map { map(it) }

    private suspend fun loadTodoItems(noteId: Long, blocks: List<NoteBlock>): List<TodoItem> {
        val list = todoRepository.getListByNoteId(noteId)
        if (list != null) {
            return todoRepository.getItemsOnce(list.id).sortedBy { it.position }
        }
        return blocks
            .filter { it.type == BlockType.CHECKBOX }
            .sortedBy { it.position }
            .mapIndexed { index, block ->
                TodoItem(
                    id = block.id,
                    listId = 0,
                    text = block.content,
                    isCompleted = block.checked,
                    position = index,
                    createdAt = 0,
                    updatedAt = 0
                )
            }
    }

    private fun loadNumberListItems(blocks: List<NoteBlock>): List<NumberListPreviewItem> {
        val orders = NumberListHelper.computeDisplayOrders(blocks)
        return blocks
            .filter { it.type == BlockType.NUMBER_LIST }
            .sortedBy { it.position }
            .mapNotNull { block ->
                val text = JsonUtils.blockContentPlainText(block.content).trim()
                if (text.isEmpty()) return@mapNotNull null
                NumberListPreviewItem(
                    number = orders[block.id] ?: NumberListMetadata.fromBlock(block).order,
                    text = text
                )
            }
    }

    private fun loadBulletListItems(blocks: List<NoteBlock>): List<BulletListPreviewItem> {
        return blocks
            .filter { it.type == BlockType.BULLET_LIST }
            .sortedBy { it.position }
            .mapNotNull { block ->
                val text = JsonUtils.blockContentPlainText(block.content).trim()
                if (text.isEmpty()) return@mapNotNull null
                BulletListPreviewItem(text = text)
            }
    }

    private fun findFirstImageUri(blocks: List<NoteBlock>, attachments: List<Attachment>): String? {
        blocks.firstOrNull { it.type == BlockType.IMAGE && it.content.isNotBlank() }?.content?.let { return it }
        blocks.firstOrNull { it.type == BlockType.DRAWING && it.content.isNotBlank() }?.content?.let { return it }
        blocks.firstOrNull { block ->
            (block.type == BlockType.ATTACHMENT || block.type == BlockType.FILE) &&
                AttachmentMetadata.fromBlock(block)?.attachmentCategory() == AttachmentCategory.IMAGE
        }?.content?.let { return it }
        return attachments.firstOrNull { attachment ->
            AttachmentTypeDetector.detect(attachment.mimeType, attachment.fileName) == AttachmentCategory.IMAGE
        }?.fileUri
    }

    private fun extractTextPreview(blocks: List<NoteBlock>): String {
        return blocks
            .filter { it.type == BlockType.TEXT && it.content.isNotBlank() }
            .sortedBy { it.position }
            .joinToString("\n") { block ->
                val raw = block.content
                if (raw.trimStart().startsWith("[") || raw.trimStart().startsWith("{")) {
                    JsonUtils.jsonToRichText(raw).plainText()
                } else {
                    raw
                }
            }
            .trim()
            .lines()
            .take(5)
            .joinToString("\n")
    }

    private fun resolvePreviewType(
        hasTodos: Boolean,
        hasNumberList: Boolean,
        hasBulletList: Boolean,
        hasImage: Boolean,
        hasVoice: Boolean,
        hasDocument: Boolean,
        hasText: Boolean
    ): PreviewType {
        // Todo + plain text should still use the todo card (with progress), matching the home UI.
        val hasOtherStructured = hasNumberList || hasBulletList || hasImage || hasVoice || hasDocument
        if (hasTodos && !hasOtherStructured) return PreviewType.TODO

        val contentTypes = listOfNotNull(
            if (hasTodos) "todo" else null,
            if (hasNumberList) "number" else null,
            if (hasBulletList) "bullet" else null,
            if (hasImage) "image" else null,
            if (hasVoice) "voice" else null,
            if (hasDocument) "document" else null,
            if (hasText) "text" else null
        )

        if (contentTypes.size >= 2) return PreviewType.MIXED
        if (hasTodos) return PreviewType.TODO
        if (hasNumberList) return PreviewType.NUMBER_LIST
        if (hasBulletList) return PreviewType.BULLET_LIST
        if (hasImage) return PreviewType.IMAGE
        if (hasVoice || hasDocument) return PreviewType.MIXED
        if (hasText) return PreviewType.TEXT
        return PreviewType.TEXT
    }

    private fun buildPreviewContent(
        previewType: PreviewType,
        todoItems: List<TodoItem>,
        numberListItems: List<NumberListPreviewItem>,
        bulletListItems: List<BulletListPreviewItem>,
        imageUri: String?,
        textSnippet: String,
        hasImage: Boolean,
        hasVoice: Boolean,
        hasDocument: Boolean
    ): Any? = when (previewType) {
        PreviewType.LOCKED -> null
        PreviewType.TODO -> buildTodoPreview(todoItems)
        PreviewType.NUMBER_LIST -> buildNumberListPreview(numberListItems)
        PreviewType.BULLET_LIST -> buildBulletListPreview(bulletListItems)
        PreviewType.IMAGE -> ImagePreviewContent(
            imageUri = imageUri.orEmpty(),

        )
        PreviewType.MIXED -> MixedPreviewContent(
            todoItems = todoItems.take(MAX_PREVIEW_ITEMS).map { it.toPreviewItem() },
            remainingTodoCount = remainingCount(todoItems.size),
            todoCompletedCount = todoItems.count { it.isCompleted },
            todoTotalCount = todoItems.size,
            numberListItems = numberListItems.take(MAX_PREVIEW_ITEMS),
            remainingNumberListCount = remainingCount(numberListItems.size),
            bulletListItems = bulletListItems.take(MAX_PREVIEW_ITEMS),
            remainingBulletListCount = remainingCount(bulletListItems.size),
            hasImage = hasImage,
            hasVoice = hasVoice,
            hasDocument = hasDocument,
            textSnippet = textSnippet.takeIf { it.isNotBlank() }
        )
        PreviewType.TEXT -> TextPreviewContent(
            text = textSnippet.ifBlank { context.getString(R.string.no_content) }
        )
    }

    private fun buildTodoPreview(todoItems: List<TodoItem>): TodoPreviewContent {
        return TodoPreviewContent(
            items = todoItems.take(MAX_PREVIEW_ITEMS).map { it.toPreviewItem() },
            remainingCount = remainingCount(todoItems.size),
            completedCount = todoItems.count { it.isCompleted },
            totalCount = todoItems.size
        )
    }

    private fun buildNumberListPreview(items: List<NumberListPreviewItem>): NumberListPreviewContent {
        return NumberListPreviewContent(
            items = items.take(MAX_PREVIEW_ITEMS),
            remainingCount = remainingCount(items.size)
        )
    }

    private fun buildBulletListPreview(items: List<BulletListPreviewItem>): BulletListPreviewContent {
        return BulletListPreviewContent(
            items = items.take(MAX_PREVIEW_ITEMS),
            remainingCount = remainingCount(items.size)
        )
    }

    private fun remainingCount(total: Int) = (total - MAX_PREVIEW_ITEMS).coerceAtLeast(0)

    private fun TodoItem.toPreviewItem() = TodoPreviewItem(
        text = JsonUtils.jsonToRichText(text).plainText().ifBlank { "Task" },
        isCompleted = isCompleted
    )

    private fun resolveBackground(note: Note, @Suppress("UNUSED_PARAMETER") previewType: PreviewType): Pair<CardBackgroundType, String> {
        val color = note.color?.trim()
        if (!color.isNullOrBlank()) {
            return CardBackgroundType.SOLID to color
        }
        // Match CreateNotesActivity default when no background is selected.
        return CardBackgroundType.SOLID to DEFAULT_BACKGROUND_KEY
    }

    companion object {
        const val MAX_PREVIEW_ITEMS = 3
        const val DEFAULT_BACKGROUND_KEY = "#FFDAC7"
        const val BACKGROUND_TEAL_PINK = "teal_pink"
        const val BACKGROUND_PINK_YELLOW = "pink_yellow"
        const val BACKGROUND_PEACH = "peach"
        const val BACKGROUND_PURPLE_BLUE = "purple_blue"
        const val BACKGROUND_GRID = "grid"
    }
}
