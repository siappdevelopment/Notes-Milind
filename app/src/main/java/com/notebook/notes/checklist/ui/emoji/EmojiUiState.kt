package com.notebook.notes.checklist.ui.emoji

data class EmojiUiState(
    val categories: List<EmojiCategory> = EmojiCategory.displayCategories,
    val selectedCategoryId: String = EmojiCategory.SMILEYS.id,
    val emojis: List<EmojiItem> = emptyList()
)
