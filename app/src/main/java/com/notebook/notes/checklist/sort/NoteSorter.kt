package com.notebook.notes.checklist.sort

import com.notebook.notes.checklist.model.Note

/**
 * Single source of truth for ordering notes according to the selected [NoteSortOrder].
 *
 * Pinned notes always stay on top to preserve existing pinned behavior; the chosen
 * sort is applied as the secondary comparator within pinned and unpinned groups.
 */
object NoteSorter {

    fun sort(notes: List<Note>, order: NoteSortOrder): List<Note> {
        val secondary: Comparator<Note> = when (order) {
            NoteSortOrder.COLOR ->
                compareBy<Note, String>(String.CASE_INSENSITIVE_ORDER) { it.color.orEmpty() }
                    .thenBy { it.position }

            NoteSortOrder.MODIFIED_TIME ->
                compareByDescending<Note> { it.updatedAt }

            NoteSortOrder.CREATED_TIME ->
                compareByDescending<Note> { it.createdAt }

            NoteSortOrder.REMINDER_TIME ->
                // Notes with an active reminder first (closest time), notes without last.
                compareBy<Note> { if (it.reminderEnabled && it.reminderTimeMillis > 0) 0 else 1 }
                    .thenBy { if (it.reminderTimeMillis > 0) it.reminderTimeMillis else Long.MAX_VALUE }

            NoteSortOrder.ALPHABETICAL ->
                compareBy<Note, String>(String.CASE_INSENSITIVE_ORDER) { it.title.trim() }

            NoteSortOrder.CUSTOM_ORDER ->
                // Manual drag-and-drop order persisted in Note.position.
                compareBy<Note> { it.position }
        }

        return notes.sortedWith(compareByDescending<Note> { it.isPinned }.then(secondary))
    }
}
