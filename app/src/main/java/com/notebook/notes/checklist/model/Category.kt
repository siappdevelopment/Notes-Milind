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
    val isAllNotes: Boolean get() = name.equals(DefaultCategories.ALL_NOTES, ignoreCase = true)

    /** Office, Workplace, and all user labels are deletable; only All Notes is not. */
    val canDelete: Boolean get() = !isAllNotes

    val canRename: Boolean get() = !isAllNotes
}
