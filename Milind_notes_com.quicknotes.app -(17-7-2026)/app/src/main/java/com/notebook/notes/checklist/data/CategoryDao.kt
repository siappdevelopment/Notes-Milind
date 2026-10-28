package com.notebook.notes.checklist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.model.CategoryWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name COLLATE NOCASE ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("""
        SELECT c.id, c.name, c.icon, c.isDefault, 
        (CASE WHEN c.name = 'All Notes' 
              THEN (SELECT COUNT(*) FROM notes WHERE isArchived = 0 AND isDeleted = 0)
              ELSE (SELECT COUNT(*) FROM notes n WHERE n.categoryId = c.id AND n.isArchived = 0 AND n.isDeleted = 0)
         END) as count 
        FROM categories c 
        ORDER BY c.isDefault DESC, c.name COLLATE NOCASE ASC
    """)
    fun getCategoriesWithCount(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name COLLATE NOCASE ASC")
    suspend fun getAllCategoriesOnce(): List<Category>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): Category?

    @Query("SELECT * FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)
}
