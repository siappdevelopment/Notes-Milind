package com.notebook.notes.checklist.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_blocks",
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
data class NoteBlock(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val noteId: Long = 0,
    val type: String = BlockType.TEXT,
    val content: String = "",
    val checked: Boolean = false,
    val extraData: String? = null,
    val position: Int = 0
)
