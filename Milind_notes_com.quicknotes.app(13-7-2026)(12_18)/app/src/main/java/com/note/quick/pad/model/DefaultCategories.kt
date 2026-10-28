package com.note.quick.pad.model

object DefaultCategories {
    const val ALL_NOTES = "All Notes"
    const val OFFICE = "Office"
    const val WORKPLACE = "Workplace"

    const val ICON_ALL_NOTES = "ic_category_all"
    const val ICON_OFFICE = "ic_category_office"
    const val ICON_WORKPLACE = "ic_category_workplace"
    const val ICON_DEFAULT = "ic_category_default"

    val PROTECTED_FROM_DELETE = setOf(ALL_NOTES, OFFICE, WORKPLACE)
}
