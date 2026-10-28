package com.notebook.notes.checklist.util

import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.DefaultCategories

object CategoryIconResolver {

    private const val DEFAULT_IC_CT_ICON = "ic_ct_defult_lebel"

    private val legacyIconMap = mapOf(
        DefaultCategories.ICON_ALL_NOTES to R.drawable.ic_all_notes,
        DefaultCategories.ICON_OFFICE to R.drawable.ic_ct_office,
        DefaultCategories.ICON_WORKPLACE to R.drawable.ic_ct_workspace,
        DefaultCategories.ICON_DEFAULT to R.drawable.ic_ct_defult_lebel
    )

    /** Suggested + known category names → drawables. */
    private val nameIconMap = mapOf(
        DefaultCategories.ALL_NOTES.lowercase() to R.drawable.ic_all_notes,
        "personal" to R.drawable.ic_ct_personal,
        "shopping" to R.drawable.ic_ct_shopping,
        "task" to R.drawable.ic_ct_task,
        "tasks" to R.drawable.ic_ct_task,
        // Suggested labels
        "diary" to R.drawable.ic_ct_study,
        "study" to R.drawable.ic_ct_study,
        "recipes" to R.drawable.ic_ct_reciept,
        "recipe" to R.drawable.ic_ct_reciept,
        "school" to R.drawable.ic_ct_school,
        "math" to R.drawable.ic_ct_math,
        "songs" to R.drawable.ic_em_music,
        "song" to R.drawable.ic_em_music,
        "music" to R.drawable.ic_em_music,
        "education" to R.drawable.ic_ct_eduction,
        "eduction" to R.drawable.ic_ct_eduction,
        "receipt" to R.drawable.ic_ct_reciept,
        "reciept" to R.drawable.ic_ct_reciept,
        DefaultCategories.OFFICE.lowercase() to R.drawable.ic_ct_office,
        DefaultCategories.WORKPLACE.lowercase() to R.drawable.ic_ct_workspace
    )

    private val icCtDrawableMap = mapOf(
        "ic_ct_all_notes" to R.drawable.ic_all_notes,
        "ic_all_notes" to R.drawable.ic_all_notes,
        "ic_ct_personal" to R.drawable.ic_ct_personal,
        "ic_ct_shopping" to R.drawable.ic_ct_shopping,
        "ic_ct_task" to R.drawable.ic_ct_task,
        "ic_ct_study" to R.drawable.ic_ct_study,
        "ic_ct_diary" to R.drawable.ic_ct_study,
        "ic_ct_school" to R.drawable.ic_ct_school,
        "ic_ct_math" to R.drawable.ic_ct_math,
        "ic_ct_eduction" to R.drawable.ic_ct_eduction,
        "ic_ct_reciept" to R.drawable.ic_ct_reciept,
        "ic_ct_recipes" to R.drawable.ic_ct_reciept,
        "ic_ct_songs" to R.drawable.ic_em_music,
        "ic_ct_music" to R.drawable.ic_em_music,
        "ic_ct_add_category" to R.drawable.ic_ct_add_category,
        DEFAULT_IC_CT_ICON to R.drawable.ic_ct_defult_lebel
    )

    /** Normalized category name → stored icon key used when creating a category. */
    private val categoryIconNameMap = mapOf(
        "all_notes" to "ic_ct_all_notes",
        "personal" to "ic_ct_personal",
        "shopping" to "ic_ct_shopping",
        "task" to "ic_ct_task",
        "tasks" to "ic_ct_task",
        "diary" to "ic_ct_diary",
        "study" to "ic_ct_study",
        "recipes" to "ic_ct_recipes",
        "recipe" to "ic_ct_recipes",
        "school" to "ic_ct_school",
        "math" to "ic_ct_math",
        "songs" to "ic_ct_songs",
        "song" to "ic_ct_songs",
        "music" to "ic_ct_songs",
        "education" to "ic_ct_eduction",
        "eduction" to "ic_ct_eduction",
        "receipt" to "ic_ct_reciept",
        "reciept" to "ic_ct_reciept",
        "office" to DefaultCategories.ICON_OFFICE,
        "workplace" to DefaultCategories.ICON_WORKPLACE
    )

    fun resolve(iconName: String?, categoryName: String? = null): Int {
        // Prefer name so suggested labels (Diary, Songs, …) win even if DB still has default icon.
        categoryName
            ?.trim()
            ?.lowercase()
            ?.let { normalizedName ->
                nameIconMap[normalizedName]?.let { return it }
            }

        iconName?.let { name ->
            if (name != DEFAULT_IC_CT_ICON) {
                icCtDrawableMap[name]?.let { return it }
                legacyIconMap[name]?.let { return it }
            }
        }

        return R.drawable.ic_ct_defult_lebel
    }

    fun iconNameForCategory(name: String): String {
        val normalized = name.trim().lowercase().replace(" ", "_")
        return categoryIconNameMap[normalized] ?: DEFAULT_IC_CT_ICON
    }
}
