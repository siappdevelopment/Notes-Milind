package com.notebook.notes.checklist.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val color: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0L,
    val isLocked: Boolean = false,
    val categoryId: Long = 0,
    val position: Long = 0,
    val reminderEnabled: Boolean = false,
    val reminderTimeMillis: Long = 0L,
    val repeatType: String = RepeatType.NEVER.name,
    val notificationId: Int = 0,
    /** Line spacing extra in px from Settings when the note was created; null = legacy / unset. */
    val listItemHeight: Int? = null
)
