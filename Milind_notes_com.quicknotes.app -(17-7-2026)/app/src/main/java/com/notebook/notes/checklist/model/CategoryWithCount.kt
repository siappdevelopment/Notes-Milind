package com.notebook.notes.checklist.model

data class CategoryWithCount(
    val id: Long,
    val name: String,
    val icon: String?,
    val isDefault: Boolean,
    val count: Int
)
