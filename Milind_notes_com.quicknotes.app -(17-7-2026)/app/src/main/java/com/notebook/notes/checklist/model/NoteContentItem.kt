package com.notebook.notes.checklist.model

sealed class NoteContentItem {
    abstract val sortPosition: Int
    abstract val stableId: Long

    data class BlockItem(val block: NoteBlock) : NoteContentItem() {
        override val sortPosition: Int get() = block.position
        override val stableId: Long get() = block.id
    }

    data class TodoEntry(val item: TodoItem) : NoteContentItem() {
        override val sortPosition: Int get() = item.position
        override val stableId: Long get() = -1_000_000_000L - item.id
    }

    data object AddTodoFooter : NoteContentItem() {
        override val sortPosition: Int get() = Int.MAX_VALUE
        override val stableId: Long get() = -2_000_000_000L
    }

    data class MediaRow(val blocks: List<NoteBlock>) : NoteContentItem() {
        init {
            require(blocks.isNotEmpty() && blocks.size <= 2) {
                "MediaRow must contain 1 or 2 media blocks"
            }
        }

        override val sortPosition: Int get() = blocks.minOf { it.position }
        override val stableId: Long get() = -3_000_000_000L + blocks.first().id
    }

    companion object {
        fun merge(blocks: List<NoteBlock>, todos: List<TodoItem>): List<NoteContentItem> {
            val blockItems = blocks
                .filter { it.type != BlockType.CHECKBOX }
                .map { BlockItem(it) }
            val todoItems = todos.map { TodoEntry(it) }
            val merged = (blockItems + todoItems).sortedWith(
                compareBy<NoteContentItem> { it.sortPosition }.thenBy { it.stableId }
            )
            val grouped = MediaBlockHelper.groupIntoRows(merged)
            return withAddTodoFooter(grouped, showFooter = todos.isNotEmpty())
        }

        fun withAddTodoFooter(
            items: List<NoteContentItem>,
            showFooter: Boolean
        ): List<NoteContentItem> {
            if (!showFooter) return items.filterNot { it is AddTodoFooter }
            val withoutFooter = items.filterNot { it is AddTodoFooter }
            val lastTodoIndex = withoutFooter.indexOfLast { it is TodoEntry }
            if (lastTodoIndex < 0) return withoutFooter
            return withoutFooter.toMutableList().apply {
                add(lastTodoIndex + 1, AddTodoFooter)
            }
        }
    }
}
