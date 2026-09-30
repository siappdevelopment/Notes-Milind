package com.notebook.notes.checklist.ui.model

data class TodoPreviewItem(
    val text: String,
    val isCompleted: Boolean
)

data class TodoPreviewContent(
    val items: List<TodoPreviewItem>,
    val remainingCount: Int = 0,
    val completedCount: Int = 0,
    val totalCount: Int = 0
)

data class NumberListPreviewItem(
    val number: Int,
    val text: String
)

data class NumberListPreviewContent(
    val items: List<NumberListPreviewItem>,
    val remainingCount: Int = 0
)

data class BulletListPreviewItem(
    val text: String
)

data class BulletListPreviewContent(
    val items: List<BulletListPreviewItem>,
    val remainingCount: Int = 0
)

data class ImagePreviewContent(
    val imageUri: String,
    val placeholderRes: Int? = null
)

data class TextPreviewContent(
    val text: String
)

data class MixedPreviewContent(
    val todoItems: List<TodoPreviewItem>,
    val remainingTodoCount: Int = 0,
    val todoCompletedCount: Int = 0,
    val todoTotalCount: Int = 0,
    val numberListItems: List<NumberListPreviewItem> = emptyList(),
    val remainingNumberListCount: Int = 0,
    val bulletListItems: List<BulletListPreviewItem> = emptyList(),
    val remainingBulletListCount: Int = 0,
    val hasImage: Boolean = false,
    val hasVoice: Boolean = false,
    val hasDocument: Boolean = false,
    val textSnippet: String? = null
)

enum class CardBackgroundType {
    SOLID,
    GRADIENT,
    PATTERN
}
