package com.note.quick.pad.util

import com.note.quick.pad.R
import com.note.quick.pad.model.DefaultCategories

object CategoryIconResolver {

    private const val DEFAULT_IC_CT_ICON = "ic_ct_defult_lebel"

    private val legacyIconMap = mapOf(
        DefaultCategories.ICON_ALL_NOTES to R.drawable.ic_ct_all_notes,
        DefaultCategories.ICON_OFFICE to R.drawable.ic_ct_office,
        DefaultCategories.ICON_WORKPLACE to R.drawable.ic_ct_workspace,
        DefaultCategories.ICON_DEFAULT to R.drawable.ic_ct_defult_lebel
    )

    private val nameIconMap = mapOf(
        DefaultCategories.ALL_NOTES.lowercase() to R.drawable.ic_ct_all_notes,
        "personal" to R.drawable.ic_ct_personal,
        "shopping" to R.drawable.ic_ct_shopping,
        "task" to R.drawable.ic_ct_task,
        "study" to R.drawable.ic_ct_study,
        "school" to R.drawable.ic_ct_school,
        "math" to R.drawable.ic_ct_math,
        "education" to R.drawable.ic_ct_eduction,
        "eduction" to R.drawable.ic_ct_eduction,
        "receipt" to R.drawable.ic_ct_reciept,
        "reciept" to R.drawable.ic_ct_reciept,
        DefaultCategories.OFFICE.lowercase() to R.drawable.ic_ct_office,
        DefaultCategories.WORKPLACE.lowercase() to R.drawable.ic_ct_workspace
    )

    private val icCtDrawableMap = mapOf(
        "ic_ct_all_notes" to R.drawable.ic_ct_all_notes,
        "ic_ct_personal" to R.drawable.ic_ct_personal,
        "ic_ct_shopping" to R.drawable.ic_ct_shopping,
        "ic_ct_task" to R.drawable.ic_ct_task,
        "ic_ct_study" to R.drawable.ic_ct_study,
        "ic_ct_school" to R.drawable.ic_ct_school,
        "ic_ct_math" to R.drawable.ic_ct_math,
        "ic_ct_eduction" to R.drawable.ic_ct_eduction,
        "ic_ct_reciept" to R.drawable.ic_ct_reciept,
        "ic_ct_add_category" to R.drawable.ic_ct_add_category,
        DEFAULT_IC_CT_ICON to R.drawable.ic_ct_defult_lebel
    )

    private val icCtIconNames = setOf(
        "all_notes",
        "personal",
        "shopping",
        "task",
        "study",
        "school",
        "math",
        "eduction",
        "reciept"
    )

    fun resolve(iconName: String?, categoryName: String? = null): Int {
        iconName?.let { name ->
            icCtDrawableMap[name]?.let { return it }
            legacyIconMap[name]?.let { return it }
        }

        categoryName
            ?.trim()
            ?.lowercase()
            ?.let { normalizedName ->
                nameIconMap[normalizedName]?.let { return it }
            }

        return R.drawable.ic_ct_defult_lebel
    }

    fun iconNameForCategory(name: String): String {
        val normalized = name.trim().lowercase().replace(" ", "_")
        return if (icCtIconNames.contains(normalized)) {
            "ic_ct_$normalized"
        } else {
            DEFAULT_IC_CT_ICON
        }
    }
}
