package com.notebook.notes.checklist.util

import android.content.Context
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.utils.JsonUtils

/**
 * Resolves note titles for display/copy. Blank titles, legacy stored
 * "Untitled" placeholders, and "(Copy)" suffixes (any app locale) are
 * shown with the current language.
 */
object NoteTitleHelper {

    /** All known translations of [R.string.untitled] — used for existing DB values. */
    private val UNTITLED_PLACEHOLDERS = setOf(
        "Untitled",
        "Sonder titel",
        "Sem título",
        "Tanpa judul",
        "Başlıksız",
        "Naamloos",
        "Walang pamagat",
        "Χωρίς τίτλο",
        "ไม่มีชื่อ",
        "无标题",
        "Unbenannt",
        "Senza titolo",
        "제목 없음",
        "無題",
        "Sin título",
        "Sans titre",
        "Без названия",
        "शीर्षकहीन"
    )

    /** All known translations of [R.string.note_copy_suffix] — longest first for matching. */
    private val COPY_SUFFIX_PLACEHOLDERS = listOf(
        " (Αντίγραφο)",
        " (복사본)",
        " (Salinan)",
        " (コピー)",
        " (สำเนา)",
        " (Cópia)",
        " (копия)",
        " (कॉपी)",
        " (Kopie)",
        " (Kopya)",
        " (Copie)",
        " (Copia)",
        " (Copy)",
        " (副本)"
    ).sortedByDescending { it.length }

    fun plainTitle(titleJson: String): String =
        JsonUtils.blockContentPlainText(titleJson).trim()

    fun isUntitledPlaceholder(plain: String): Boolean {
        val trimmed = plain.trim()
        if (trimmed.isEmpty()) return true
        return UNTITLED_PLACEHOLDERS.any { it.equals(trimmed, ignoreCase = true) }
    }

    /** True if [plain] already ends with any known localized copy suffix. */
    fun hasCopySuffix(plain: String): Boolean =
        stripCopySuffix(plain.trim()).second

    /**
     * @return pair of (base title without copy suffix, whether a suffix was removed)
     */
    fun stripCopySuffix(plain: String): Pair<String, Boolean> {
        val trimmed = plain.trim()
        for (suffix in COPY_SUFFIX_PLACEHOLDERS) {
            if (trimmed.endsWith(suffix)) {
                return trimmed.removeSuffix(suffix).trimEnd() to true
            }
            // Case-insensitive fallback for Latin suffixes
            if (trimmed.length >= suffix.length &&
                trimmed.takeLast(suffix.length).equals(suffix, ignoreCase = true)
            ) {
                return trimmed.dropLast(suffix.length).trimEnd() to true
            }
        }
        return trimmed to false
    }

    fun displayTitle(context: Context, titleJson: String): String {
        val plain = plainTitle(titleJson)
        val localizedCopy = context.getString(R.string.note_copy_suffix)
        val (base, hadCopy) = stripCopySuffix(plain)

        val localizedBase = if (isUntitledPlaceholder(base)) {
            context.getString(R.string.untitled)
        } else {
            base
        }

        return if (hadCopy) localizedBase + localizedCopy else localizedBase
    }
}
