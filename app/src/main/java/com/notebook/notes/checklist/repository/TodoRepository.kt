package com.notebook.notes.checklist.repository

import com.notebook.notes.checklist.data.TodoItemDao
import com.notebook.notes.checklist.data.TodoListDao
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.TodoItem
import com.notebook.notes.checklist.model.TodoList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TodoRepository @Inject constructor(
    private val todoListDao: TodoListDao,
    private val todoItemDao: TodoItemDao
) {

    fun observeItems(listId: Long): Flow<List<TodoItem>> =
        todoItemDao.getItemsForList(listId)

    fun searchItems(listId: Long, query: String): Flow<List<TodoItem>> =
        if (query.isBlank()) {
            todoItemDao.getItemsForList(listId)
        } else {
            todoItemDao.searchItems(listId, query)
        }

    suspend fun getListByNoteId(noteId: Long): TodoList? =
        todoListDao.getListByNoteId(noteId)

    suspend fun getItemsOnce(listId: Long): List<TodoItem> =
        todoItemDao.getItemsForListOnce(listId)

    suspend fun createList(title: String, noteId: Long? = null): Long {
        val now = System.currentTimeMillis()
        return todoListDao.insertList(
            TodoList(
                noteId = noteId,
                title = title.ifBlank { "Untitled" },
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateListTitle(listId: Long, title: String) {
        val now = System.currentTimeMillis()
        todoListDao.updateTitle(listId, title.ifBlank { "Untitled" }, now)
    }

    suspend fun linkListToNote(listId: Long, noteId: Long) {
        todoListDao.linkToNote(listId, noteId, System.currentTimeMillis())
    }

    /**
     * Creates a new todo list on [targetNoteId] with the same items as the list
     * attached to [sourceNoteId]. No-op when the source note has no list.
     */
    suspend fun duplicateListForNote(sourceNoteId: Long, targetNoteId: Long) {
        val sourceList = todoListDao.getListByNoteId(sourceNoteId) ?: return
        val items = todoItemDao.getItemsForListOnce(sourceList.id)
        val now = System.currentTimeMillis()
        val newListId = todoListDao.insertList(
            sourceList.copy(
                id = 0,
                noteId = targetNoteId,
                createdAt = now,
                updatedAt = now
            )
        )
        items.forEach { item ->
            todoItemDao.insertItem(
                item.copy(
                    id = 0,
                    listId = newListId,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun migrateCheckboxBlocks(noteId: Long, title: String, blocks: List<NoteBlock>): Long? {
        val existing = todoListDao.getListByNoteId(noteId)
        if (existing != null) return existing.id

        val checkboxBlocks = blocks.filter { it.type == BlockType.CHECKBOX }
        if (checkboxBlocks.isEmpty()) return null

        val now = System.currentTimeMillis()
        val listId = todoListDao.insertList(
            TodoList(
                noteId = noteId,
                title = title.ifBlank { "Untitled" },
                createdAt = now,
                updatedAt = now
            )
        )
        checkboxBlocks.forEach { block ->
            todoItemDao.insertItem(
                TodoItem(
                    listId = listId,
                    text = block.content,
                    isCompleted = block.checked,
                    position = block.position,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        return listId
    }

    suspend fun addItem(listId: Long, text: String = "", position: Int? = null): Long {
        val now = System.currentTimeMillis()
        val itemPosition = position ?: todoItemDao.getItemCount(listId)
        return todoItemDao.insertItem(
            TodoItem(
                listId = listId,
                text = text,
                isCompleted = false,
                position = itemPosition,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateItemText(itemId: Long, text: String) {
        val item = todoItemDao.getItemById(itemId) ?: return
        todoItemDao.updateItem(
            item.copy(text = text, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun toggleItem(itemId: Long) {
        val item = todoItemDao.getItemById(itemId) ?: return
        todoItemDao.updateItem(
            item.copy(
                isCompleted = !item.isCompleted,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteItem(itemId: Long) {
        val item = todoItemDao.getItemById(itemId) ?: return
        val deletedPosition = item.position
        todoItemDao.deleteItem(itemId)
        val remaining = todoItemDao.getItemsForListOnce(item.listId)
        val now = System.currentTimeMillis()
        remaining.filter { it.position > deletedPosition }.forEach { remainingItem ->
            todoItemDao.updateItem(
                remainingItem.copy(position = remainingItem.position - 1, updatedAt = now)
            )
        }
    }

    suspend fun reorderItems(listId: Long, orderedItemIds: List<Long>) {
        val now = System.currentTimeMillis()
        orderedItemIds.forEachIndexed { index, itemId ->
            val item = todoItemDao.getItemById(itemId) ?: return@forEachIndexed
            if (item.position != index) {
                todoItemDao.updateItem(item.copy(position = index, updatedAt = now))
            }
        }
    }

    suspend fun shiftPositionsFrom(listId: Long, fromPosition: Int, delta: Int) {
        if (delta == 0) return
        todoItemDao.shiftPositionsFrom(listId, fromPosition, delta)
    }

    suspend fun insertItemAt(listId: Long, text: String, position: Int): Long {
        shiftPositionsFrom(listId, position, 1)
        return addItem(listId, text, position)
    }

    suspend fun updateItemPosition(itemId: Long, position: Int) {
        val item = todoItemDao.getItemById(itemId) ?: return
        if (item.position == position) return
        todoItemDao.updateItem(
            item.copy(position = position, updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun applyItemPositions(positions: Map<Long, Int>) {
        val now = System.currentTimeMillis()
        positions.forEach { (itemId, position) ->
            val item = todoItemDao.getItemById(itemId) ?: return@forEach
            if (item.position != position) {
                todoItemDao.updateItem(item.copy(position = position, updatedAt = now))
            }
        }
    }

    suspend fun getStats(listId: Long): TodoStats {
        val total = todoItemDao.getItemCount(listId)
        val completed = todoItemDao.getCompletedCount(listId)
        val remaining = total - completed
        val percentage = if (total > 0) (completed * 100) / total else 0
        return TodoStats(total, completed, remaining, percentage)
    }
}

data class TodoStats(
    val total: Int,
    val completed: Int,
    val remaining: Int,
    val completionPercentage: Int
)

enum class TodoFilter {
    ALL, ACTIVE, COMPLETED
}
