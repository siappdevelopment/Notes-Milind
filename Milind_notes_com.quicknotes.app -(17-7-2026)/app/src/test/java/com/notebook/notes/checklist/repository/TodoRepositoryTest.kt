package com.notebook.notes.checklist.repository

import com.notebook.notes.checklist.model.TodoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TodoRepositoryTest {

    @Test
    fun `filterItems returns all items`() {
        val items = sampleItems()
        val result = filterItems(items, TodoFilter.ALL)
        assertEquals(3, result.size)
    }

    @Test
    fun `filterItems returns active items only`() {
        val items = sampleItems()
        val active = filterItems(items, TodoFilter.ACTIVE)
        assertEquals(2, active.size)
        assertFalse(active.any { it.isCompleted })
    }

    @Test
    fun `filterItems returns completed items only`() {
        val items = sampleItems()
        val completed = filterItems(items, TodoFilter.COMPLETED)
        assertEquals(1, completed.size)
        assertTrue(completed.all { it.isCompleted })
    }

    @Test
    fun `todo stats percentage calculation`() {
        val total = 10
        val completed = 7
        val remaining = total - completed
        val percentage = if (total > 0) (completed * 100) / total else 0

        assertEquals(3, remaining)
        assertEquals(70, percentage)
    }

    private fun filterItems(items: List<TodoItem>, filter: TodoFilter): List<TodoItem> =
        when (filter) {
            TodoFilter.ALL -> items
            TodoFilter.ACTIVE -> items.filter { !it.isCompleted }
            TodoFilter.COMPLETED -> items.filter { it.isCompleted }
        }

    private fun sampleItems() = listOf(
        TodoItem(1L, 10L, "Milk", false, 0, 1L, 1L),
        TodoItem(2L, 10L, "Bread", true, 1, 1L, 1L),
        TodoItem(3L, 10L, "Eggs", false, 2, 1L, 1L)
    )
}
