package com.notebook.notes.checklist.repository

import com.notebook.notes.checklist.data.CategoryDao
import com.notebook.notes.checklist.data.NoteDao
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.model.CategoryWithCount
import com.notebook.notes.checklist.model.DefaultCategories
import com.notebook.notes.checklist.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

sealed class CategoryError(val message: String) {
    data object EmptyName : CategoryError("Category name cannot be empty.")
    data object DuplicateName : CategoryError("Category already exists.")
    data object NotFound : CategoryError("Unable to load category.")
    data object CannotDelete : CategoryError("This category cannot be deleted.")
    data object CannotRename : CategoryError("This category cannot be renamed.")
}

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val noteDao: NoteDao,
    private val reminderScheduler: ReminderScheduler
) {
    private val initMutex = Mutex()
    private var defaultsEnsured = false

    private val _selectedFilterCategoryId = MutableStateFlow<Long?>(null)
    val selectedFilterCategoryId = _selectedFilterCategoryId.asStateFlow()

    private val _allNotesCategoryId = MutableStateFlow<Long?>(null)
    val allNotesCategoryId = _allNotesCategoryId.asStateFlow()

    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>> = categoryDao.getCategoriesWithCount()

    suspend fun ensureDefaultCategories() {
        initMutex.withLock {
            if (defaultsEnsured && categoryDao.countCategories() > 0) return
            if (categoryDao.countCategories() > 0) {
                defaultsEnsured = true
                _allNotesCategoryId.value = getAllNotesCategory()?.id
                if (_selectedFilterCategoryId.value == null) {
                    _selectedFilterCategoryId.value = _allNotesCategoryId.value
                }
                return
            }

            val now = System.currentTimeMillis()
            val defaults = listOf(
                Category(
                    name = DefaultCategories.ALL_NOTES,
                    icon = DefaultCategories.ICON_ALL_NOTES,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    name = DefaultCategories.OFFICE,
                    icon = DefaultCategories.ICON_OFFICE,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now
                ),
                Category(
                    name = DefaultCategories.WORKPLACE,
                    icon = DefaultCategories.ICON_WORKPLACE,
                    isDefault = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
            defaults.forEach { categoryDao.insertCategory(it) }
            defaultsEnsured = true
            _allNotesCategoryId.value = getAllNotesCategory()?.id
            _selectedFilterCategoryId.value = _allNotesCategoryId.value
        }
    }

    suspend fun getCategoryById(categoryId: Long): Category? =
        categoryDao.getCategoryById(categoryId)

    suspend fun getOfficeCategory(): Category? =
        categoryDao.getCategoryByName(DefaultCategories.OFFICE)

    suspend fun getAllNotesCategory(): Category? =
        categoryDao.getCategoryByName(DefaultCategories.ALL_NOTES)

    suspend fun getDefaultNoteCategoryId(): Long {
        getOfficeCategory()?.id?.let { return it }
        return categoryDao.getAllCategoriesOnce()
            .firstOrNull { !it.isAllNotes }
            ?.id
            ?: 0L
    }

    fun selectFilterCategory(categoryId: Long?) {
        _selectedFilterCategoryId.value = categoryId
    }

    suspend fun createCategory(name: String, icon: String? = DefaultCategories.ICON_DEFAULT): Result<Category> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException(CategoryError.EmptyName.message))
        if (categoryDao.getCategoryByName(trimmed) != null) {
            return Result.failure(IllegalArgumentException(CategoryError.DuplicateName.message))
        }

        val now = System.currentTimeMillis()
        val id = categoryDao.insertCategory(
            Category(
                name = trimmed,
                icon = icon,
                isDefault = false,
                createdAt = now,
                updatedAt = now
            )
        )
        val created = categoryDao.getCategoryById(id)
            ?: return Result.failure(IllegalStateException(CategoryError.NotFound.message))
        return Result.success(created)
    }

    suspend fun updateCategory(categoryId: Long, newName: String): Result<Category> {
        val category = categoryDao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalStateException(CategoryError.NotFound.message))
        if (!category.canRename) {
            return Result.failure(IllegalStateException(CategoryError.CannotRename.message))
        }

        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException(CategoryError.EmptyName.message))

        val existing = categoryDao.getCategoryByName(trimmed)
        if (existing != null && existing.id != categoryId) {
            return Result.failure(IllegalArgumentException(CategoryError.DuplicateName.message))
        }

        val updated = category.copy(name = trimmed, updatedAt = System.currentTimeMillis())
        categoryDao.updateCategory(updated)
        return Result.success(updated)
    }

    suspend fun deleteCategory(categoryId: Long): Result<Unit> {
        val category = categoryDao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalStateException(CategoryError.NotFound.message))
        // Only the system "All Notes" filter is blocked; Office / Workplace defaults are allowed.
        if (category.isAllNotes) {
            return Result.failure(IllegalStateException(CategoryError.CannotDelete.message))
        }

        return runCatching {
            // Delete all notes in this category (blocks/attachments/reminders cascade in DB).
            noteDao.getNotesForCategory(categoryId).forEach { note ->
                reminderScheduler.cancel(note)
            }
            noteDao.deleteNotesByCategory(categoryId)
            categoryDao.deleteCategory(category)

            if (_selectedFilterCategoryId.value == categoryId) {
                _selectedFilterCategoryId.value = _allNotesCategoryId.value
            }
        }
    }

    suspend fun lockCategoryNotes(categoryId: Long): Result<Int> {
        val category = categoryDao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalStateException(CategoryError.NotFound.message))
        if (category.isAllNotes) {
            return Result.failure(IllegalStateException(CategoryError.CannotDelete.message))
        }

        val lockedCount = noteDao.lockNotesByCategory(
            categoryId = categoryId,
            updatedAt = System.currentTimeMillis()
        )
        return Result.success(lockedCount)
    }

    suspend fun unlockCategoryNotes(categoryId: Long): Result<Int> {
        val category = categoryDao.getCategoryById(categoryId)
            ?: return Result.failure(IllegalStateException(CategoryError.NotFound.message))
        if (category.isAllNotes) {
            return Result.failure(IllegalStateException(CategoryError.CannotDelete.message))
        }

        val unlockedCount = noteDao.unlockNotesByCategory(
            categoryId = categoryId,
            updatedAt = System.currentTimeMillis()
        )
        return Result.success(unlockedCount)
    }

    suspend fun areCategoryNotesLocked(categoryId: Long): Boolean {
        val total = noteDao.countNotesForCategory(categoryId)
        if (total <= 0) return false
        return noteDao.countLockedNotesForCategory(categoryId) == total
    }

    fun isAllNotesFilter(categoryId: Long?): Boolean {
        if (categoryId == null) return true
        return categoryId == _allNotesCategoryId.value
    }

    suspend fun isAllNotesCategory(categoryId: Long): Boolean =
        categoryId == _allNotesCategoryId.value || categoryDao.getCategoryById(categoryId)?.isAllNotes == true
}
