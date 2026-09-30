package com.notebook.notes.checklist.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.utils.Constant

object LocaleHelper {

    fun getSavedLanguageCode(context: Context): String {
        return PreferenceHelper(context.applicationContext).getString(
            Constant.PREF_SELECTED_LANGUAGE_CODE,
            Constant.DEFAULT_LANGUAGE_CODE
        ) ?: Constant.DEFAULT_LANGUAGE_CODE
    }

    fun applySavedLanguage(context: Context) {
        setApplicationLanguage(getSavedLanguageCode(context))
    }

    fun applyLanguage(languageCode: String) {
        setApplicationLanguage(languageCode)
    }

    private fun setApplicationLanguage(languageCode: String) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageCode)
        )
    }
}
