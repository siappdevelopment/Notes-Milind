package com.notebook.notes.checklist.repository

import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.model.DefaultCategories
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRepositoryTest {

    @Test
    fun `trimmed category name validation rejects empty`() {
        assertFalse(isValidCategoryName("   "))
        assertFalse(isValidCategoryName(""))
    }

    @Test
    fun `trimmed category name validation accepts non-empty`() {
        assertTrue(isValidCategoryName("  Personal  "))
        assertEquals("Personal", normalizeCategoryName("  Personal  "))
    }

    @Test
    fun `duplicate detection is case insensitive`() {
        val existing = listOf(
            sampleCategory(1, DefaultCategories.OFFICE),
            sampleCategory(2, "Shopping")
        )
        assertTrue(isDuplicateName(existing, "shopping"))
        assertFalse(isDuplicateName(existing, "Travel"))
    }

    @Test
    fun `protected categories cannot be deleted`() {
        DefaultCategories.PROTECTED_FROM_DELETE.forEach { name ->
            val category = sampleCategory(1, name, isDefault = true)
            assertFalse(category.canDelete)
        }
    }

    @Test
    fun `user categories can be deleted`() {
        val category = sampleCategory(4, "Ideas", isDefault = false)
        assertTrue(category.canDelete)
    }

    @Test
    fun `all notes category cannot be renamed`() {
        val category = sampleCategory(1, DefaultCategories.ALL_NOTES, isDefault = true)
        assertFalse(category.canRename)
    }

    private fun isValidCategoryName(name: String): Boolean =
        normalizeCategoryName(name).isNotEmpty()

    private fun normalizeCategoryName(name: String): String = name.trim()

    private fun isDuplicateName(categories: List<Category>, name: String): Boolean {
        val normalized = normalizeCategoryName(name).lowercase()
        return categories.any { it.name.trim().lowercase() == normalized }
    }

    private fun sampleCategory(
        id: Long,
        name: String,
        isDefault: Boolean = false
    ) = Category(
        id = id,
        name = name,
        icon = DefaultCategories.ICON_DEFAULT,
        isDefault = isDefault,
        createdAt = 1L,
        updatedAt = 1L
    )
}
