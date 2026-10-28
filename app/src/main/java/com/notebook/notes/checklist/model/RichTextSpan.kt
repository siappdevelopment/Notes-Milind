package com.notebook.notes.checklist.model

data class RichTextSpan(
    val text: String = "",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val fontSize: Int = 16,
    val fontFamily: String = "default",
    val textColor: String = "#000000",
    val highlightColor: String? = null,
    val linkUrl: String? = null,
    val isCheckbox: Boolean = false,
    val checked: Boolean = false,
    val isBullet: Boolean = false,
    val isNumbered: Boolean = false,
    val numberIndex: Int = 0
)

data class RichTextDocument(
    val spans: List<RichTextSpan> = listOf(RichTextSpan()),
    val alignment: String = ALIGN_LEFT
) {
    fun plainText(): String = spans.joinToString("") { it.text }

    companion object {
        const val ALIGN_LEFT = "left"
        const val ALIGN_CENTER = "center"
        const val ALIGN_RIGHT = "right"
        const val ALIGN_JUSTIFY = "justify"
    }
}
