package com.notebook.notes.checklist.model

import androidx.annotation.DrawableRes

data class Language(
    val code: String,
    val name: String,
    val nativeName: String,
    @DrawableRes val iconRes: Int,
    var isSelected: Boolean = false,
    val backgroundColor: String = "#F8F8F8", // Default static color
    val subtitle: String? = null
) {
    fun displayName(): String = subtitle ?: name
}
