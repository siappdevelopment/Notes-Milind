package com.notebook.notes.checklist.util

/**
 * Compat facade — prefer [DateTimeUtils] for new call sites.
 */
object NoteDateFormatter {

    fun formatHeaderDate(timestamp: Long): String = DateTimeUtils.formatHeaderDate(timestamp)
}
