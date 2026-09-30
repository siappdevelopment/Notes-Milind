package com.notebook.notes.checklist.helper

import android.content.Context
import android.content.SharedPreferences

class PreferenceHelper(
    context: Context
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun putString(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    /** Synchronous write — use before an activity recreate (e.g. locale change). */
    fun putStringSync(key: String, value: String?) {
        sharedPreferences.edit().putString(key, value).commit()
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    fun putInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        return sharedPreferences.getInt(key, defaultValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long {
        return sharedPreferences.getLong(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float {
        return sharedPreferences.getFloat(key, defaultValue)
    }

    fun putStringSet(key: String, values: Set<String>?) {
        sharedPreferences.edit().putStringSet(key, values).apply()
    }

    fun getStringSet(key: String, defaultValues: Set<String>? = null): Set<String>? {
        return sharedPreferences.getStringSet(key, defaultValues)
    }

    fun contains(key: String): Boolean = sharedPreferences.contains(key)

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}
