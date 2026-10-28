package com.notebook.notes.checklist.model

data class NoteWithPreview(
    val note: Note,
    val preview: String = ""
)
