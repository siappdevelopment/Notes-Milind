package com.notebook.notes.checklist.ui.model

import com.notebook.notes.checklist.model.Note

data class NoteCardUiModel(
    val note: Note,
    val noteId: Long,
    val position: Long,
    val title: String,
    val categoryName: String,
    val categoryIconRes: Int,
    val backgroundType: CardBackgroundType,
    val backgroundKey: String,
    val previewType: PreviewType,
    val previewContent: Any?,
    val isPinned: Boolean,
    val isLocked: Boolean,
    val isFavorite: Boolean,
    val updatedAt: Long
)
