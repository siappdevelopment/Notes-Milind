package com.notebook.notes.checklist.ui.activity

import androidx.annotation.StringRes
import com.notebook.notes.checklist.R

data class HomeTopBarConfiguration(
    @StringRes val titleRes: Int = R.string.home_title,
    val showProBadge: Boolean = true,
    val showSettings: Boolean = true,
    val showAutoSavedBadge: Boolean = true
)

interface HomeTopBarCallback {
    fun getTopBarConfiguration(): HomeTopBarConfiguration = HomeTopBarConfiguration()

    fun onTopBarSearchQuery(query: String) {}

    fun isNotesGridView(): Boolean = true

    fun toggleNotesViewMode() {}

    /** Push this tab's current notes count into the shared top bar. */
    fun syncTopBarNotesCount() {}
}

/** Selection bottom-bar host for Home / Reminder / Bookmark / Archive lists. */
interface NoteSelectionCallback {
    fun exitSelectionMode()
    fun exitSelectionModeIfActive(): Boolean
    fun selectAllNotes()
    /** Enter selection mode and select every currently listed note. */
    fun enterSelectionSelectingAll()
    fun isAllNotesSelected(): Boolean
    fun areAllSelectedNotesPinned(): Boolean
    fun areAllSelectedNotesLocked(): Boolean
    fun areAllSelectedNotesArchived(): Boolean
    /** Reminder tab hides Add Reminder on the selection bar. */
    fun showsSelectionReminderAction(): Boolean = true
    fun handleBulkLock()
    fun handleBulkPin()
    fun handleBulkReminder()
    fun handleBulkCopy()
    fun handleBulkDelete()
    fun handleBulkArchive()
}
