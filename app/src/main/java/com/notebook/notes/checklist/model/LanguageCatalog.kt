package com.notebook.notes.checklist.model

import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.utils.Constant

object LanguageCatalog {

    val languages: List<Language> = listOf(
        Language("en", "English", "English", R.drawable.ic_ln_enlish, false, "#FFFBEB"),
        Language("en-GB", "UK", "English", R.drawable.ic_ln_english_uk, false, "#EEF4FA", "English (UK)"),
        Language("hi", "Hindi", "हिंदी", R.drawable.ic_ln_hindi, false, "#FEF2F2"),
        Language("ru", "Russian", "Русский", R.drawable.ic_ln_russian, false, "#EFFAF4"),
        Language("it", "Italian", "Italiano", R.drawable.ic_ln_italian, false, "#FFEEEE"),
        Language("fr", "French", "Français", R.drawable.ic_ln_franch, false, "#FEF8F2"),
        Language("es", "Spanish", "Español", R.drawable.ic_ln_spanish, false, "#FFF6F8"),
        Language("ja", "Japanese", "日本語", R.drawable.ic_ln_japanese, false, "#F1F1F8"),
        Language("ko", "Korean", "한국인", R.drawable.ic_ln_korean, false, "#FFECEC"),
        Language("de", "German", "Deutsch", R.drawable.ic_ln_german, false, "#FEF9F2"),
        Language("zh", "Chinese", "中国人", R.drawable.ic_ln_chinese, false, "#FFF6F0"),
        Language("th", "Thai", "แบบไทย", R.drawable.ic_ln_thai, false, "#FAF0EC"),
        Language("el", "Greek", "ελληνικά", R.drawable.ic_ln_greek, false, "#FAF9F3"),
        Language("pt", "Portuguese", "Português", R.drawable.ic_ln_portuguese, false, "#FFF8E7", "Portuguese (Portugal)"),
        Language("pt-BR", "Portuguese", "Português", R.drawable.ic_ln_portuguese_br, false, "#EFFAF4", "Portuguese (Brazil)"),
        Language("nl", "Dutch", "Nederlands", R.drawable.ic_ln_dutch, false, "#EEF4FA"),
        Language("fil", "Filipino", "Filipino", R.drawable.ic_ln_filipino, false, "#FFF6F8"),
        Language("tr", "Turkish", "Türkçe", R.drawable.ic_ln_turkish, false, "#FAF4F0"),
        Language("in", "Indonesian", "Indonesia", R.drawable.ic_ln_indonesia, false, "#FFF6F0"),
        Language("af", "Afrikaans", "Afrikaans", R.drawable.ic_ln_afrikaans, false, "#FAF9F3")
    )

    fun displayNameForCode(code: String?): String {
        if (code.isNullOrBlank()) return Constant.DEFAULT_LANGUAGE_NAME
        return languages.find { it.code.equals(code, ignoreCase = true) }?.nativeName
            ?: Constant.DEFAULT_LANGUAGE_NAME
    }

    fun findByCode(code: String?): Language? {
        if (code.isNullOrBlank()) return null
        return languages.find { it.code.equals(code, ignoreCase = true) }
    }
}
