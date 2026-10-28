package com.notebook.notes.checklist.util

import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.NumberListHelper
import com.notebook.notes.checklist.model.TodoItem
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.repository.TodoRepository
import com.notebook.notes.checklist.utils.JsonUtils

/**
 * Builds plain-text share/export content: title, body text, numbered lists,
 * bullets, and todos. Image, drawing, voice, and file blocks are omitted.
 */
object NoteShareTextFormatter {

    private data class Line(
        val position: Int,
        val stableId: Long,
        val text: String
    )

    fun build(
        title: String,
        blocks: List<NoteBlock>,
        todos: List<TodoItem> = emptyList(),
        untitledFallback: String = "Untitled"
    ): String {
        val titlePlain = JsonUtils.blockContentPlainText(title).ifBlank { untitledFallback }
        val numberOrders = NumberListHelper.computeDisplayOrders(blocks)
        val includeLegacyCheckbox = todos.isEmpty()

        val lines = ArrayList<Line>(blocks.size + todos.size)

        for (block in blocks) {
            formatBlock(block, numberOrders, includeLegacyCheckbox)?.let { text ->
                lines += Line(block.position, block.id, text)
            }
        }

        for (todo in todos) {
            val plain = JsonUtils.blockContentPlainText(todo.text)
            if (plain.isBlank()) continue
            val prefix = if (todo.isCompleted) "[x] " else "[ ] "
            lines += Line(todo.position, -1_000_000_000L - todo.id, prefix + plain)
        }

        lines.sortWith(compareBy<Line> { it.position }.thenBy { it.stableId })

        val body = lines.joinToString("\n") { it.text }.trimEnd()
        return if (body.isEmpty()) titlePlain else "$titlePlain\n\n$body"
    }

    suspend fun buildForNote(
        noteId: Long,
        title: String,
        noteRepository: NoteRepository,
        todoRepository: TodoRepository,
        untitledFallback: String = "Untitled"
    ): String {
        val blocks = noteRepository.getBlocksOnce(noteId)
        val todos = todoRepository.getListByNoteId(noteId)
            ?.let { todoRepository.getItemsOnce(it.id) }
            .orEmpty()
        return build(title, blocks, todos, untitledFallback)
    }

    private fun formatBlock(
        block: NoteBlock,
        numberOrders: Map<Long, Int>,
        includeLegacyCheckbox: Boolean
    ): String? {
        val text = JsonUtils.blockContentPlainText(block.content)
        return when (block.type) {
            BlockType.TEXT, BlockType.LINK -> text.takeIf { it.isNotBlank() }
            BlockType.CHECKBOX -> {
                if (!includeLegacyCheckbox) return null
                if (text.isBlank()) return null
                val prefix = if (block.checked) "[x] " else "[ ] "
                prefix + text
            }
            BlockType.NUMBER_LIST -> {
                if (text.isBlank()) return null
                val order = numberOrders[block.id] ?: 1
                "$order. $text"
            }
            BlockType.BULLET_LIST -> {
                if (text.isBlank()) return null
                "• $text"
            }
            BlockType.DIVIDER -> "--------------------"
            BlockType.IMAGE,
            BlockType.DRAWING,
            BlockType.VOICE,
            BlockType.FILE,
            BlockType.ATTACHMENT -> null
            else -> text.takeIf { it.isNotBlank() }
        }
    }
}
