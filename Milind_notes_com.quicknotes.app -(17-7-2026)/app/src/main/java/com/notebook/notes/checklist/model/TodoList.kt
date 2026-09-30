package com.notebook.notes.checklist.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todo_lists",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class TodoList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long? = null,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
