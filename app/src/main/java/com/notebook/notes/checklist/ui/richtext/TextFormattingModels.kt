package com.notebook.notes.checklist.ui.richtext

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.notebook.notes.checklist.R

enum class TextStylePreset(
    @StringRes val labelRes: Int,
    val fontSizeSp: Int,
    val previewTextSizeSp: Float
) {
    NORMAL(R.string.text_preset_normal, 16, 14f),
    HEADLINE(R.string.text_preset_headline, 32, 20f),
    SUBTITLE(R.string.text_preset_subtitle, 14, 12f),
    TITLE1(R.string.text_preset_title1, 24, 16f);

    companion object {
        fun fromFontSize(fontSize: Int): TextStylePreset =
            entries.minByOrNull { kotlin.math.abs(it.fontSizeSp - fontSize) } ?: NORMAL
    }
}

enum class FormattingToolId {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    HIGHLIGHT,
    TEXT_COLOR,
    ALIGN_LEFT,
    ALIGN_CENTER,
    ALIGN_RIGHT,
    ALIGN_JUSTIFY,
    BULLET_LIST,
    NUMBER_LIST
}

data class FormattingToolItem(
    val id: FormattingToolId,
    val label: String? = null,
    @DrawableRes val iconRes: Int? = null,
    val isSelected: Boolean = false,
    @StringRes val contentDescriptionRes: Int = 0,
    val color: Int? = null
)
