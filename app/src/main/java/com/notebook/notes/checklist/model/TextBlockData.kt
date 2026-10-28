package com.notebook.notes.checklist.model

data class TextBlockData(
    val text: String = "",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val fontSize: Int = 16,
    val fontFamily: String = "default",
    val textColor: String = "#000000"
)
