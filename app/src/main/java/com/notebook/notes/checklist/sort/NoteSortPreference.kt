package com.notebook.notes.checklist.sort

import com.notebook.notes.checklist.helper.PreferenceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the globally selected [NoteSortOrder] and exposes it as an observable flow so
 * every note-listing screen reacts to sort changes immediately and after app restart.
 */
@Singleton
class NoteSortPreference @Inject constructor(
    private val preferenceHelper: PreferenceHelper
) {
    private val _sortOrder = MutableStateFlow(load())
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    val current: NoteSortOrder get() = _sortOrder.value

    fun setSortOrder(order: NoteSortOrder) {
        preferenceHelper.putString(KEY_SORT_ORDER, order.name)
        _sortOrder.value = order
    }

    private fun load(): NoteSortOrder {
        val stored = preferenceHelper.getString(KEY_SORT_ORDER, DEFAULT.name)
        return runCatching { NoteSortOrder.valueOf(stored ?: DEFAULT.name) }.getOrDefault(DEFAULT)
    }

    companion object {
        private const val KEY_SORT_ORDER = "note_sort_order"
        private val DEFAULT = NoteSortOrder.CUSTOM_ORDER
    }
}
