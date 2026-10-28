package com.notebook.notes.checklist.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String?,
    val isDefault: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isAllNotes: Boolean get() = name == DefaultCategories.ALL_NOTES

    val canDelete: Boolean get() = name !in DefaultCategories.PROTECTED_FROM_DELETE

    val canRename: Boolean get() = !isAllNotes
}
