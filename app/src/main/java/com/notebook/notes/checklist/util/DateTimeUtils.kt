package com.notebook.notes.checklist.util

import android.content.Context
import com.google.android.material.timepicker.TimeFormat
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.utils.Constant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Single source of truth for displaying dates/times according to the user's
 * Settings → Date Format and Time Format preferences. Timestamps stay stored
 * as millis; formatting happens only at display time.
 */
object DateTimeUtils {

    private val timeFormatKey = AtomicReference(Constant.TIME_FORMAT_12)
    private val dateFormatKey = AtomicReference(Constant.DEFAULT_DATE_FORMAT)

    fun init(context: Context) {
        refresh(context)
    }

    fun refresh(context: Context) {
        val prefs = PreferenceHelper(context.applicationContext)
        val storedTime = prefs.getString(Constant.PREF_TIME_FORMAT, Constant.TIME_FORMAT_12)
            ?: Constant.TIME_FORMAT_12
        timeFormatKey.set(
            if (storedTime == Constant.TIME_FORMAT_24) Constant.TIME_FORMAT_24
            else Constant.TIME_FORMAT_12
        )

        val storedDate = prefs.getString(Constant.PREF_DATE_FORMAT, Constant.DEFAULT_DATE_FORMAT)
            ?: Constant.DEFAULT_DATE_FORMAT
        dateFormatKey.set(normalizeDatePattern(storedDate))
    }

    fun setFormat(format: String) {
        timeFormatKey.set(
            if (format == Constant.TIME_FORMAT_24) Constant.TIME_FORMAT_24
            else Constant.TIME_FORMAT_12
        )
    }

    fun setDateFormat(format: String) {
        dateFormatKey.set(normalizeDatePattern(format))
    }

    fun currentFormat(): String = timeFormatKey.get()
    fun currentDateLayout(): String = dateFormatKey.get()

    fun is24Hour(): Boolean = currentFormat() == Constant.TIME_FORMAT_24

    fun materialClockFormat(): Int =
        if (is24Hour()) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

    /** Date only using Settings → Date Format (e.g. `2026-07-15`). */
    fun formatDate(timestamp: Long): String =
        formatWith(timestamp, currentDateLayout())

    /** Time only: `09:30 AM` or `09:30` / `14:45`. */
    fun formatTime(timestamp: Long): String =
        formatWith(timestamp, if (is24Hour()) PATTERN_TIME_24 else PATTERN_TIME_12)

    /**
     * Create-note header: full Settings → Date Format + Time Format,
     * e.g. `15/07/2026, 02:30 PM` or `2026-07-15, 14:30`.
     */
    fun formatHeaderDate(timestamp: Long): String {
        val timePart = if (is24Hour()) PATTERN_TIME_24 else PATTERN_TIME_12
        return formatWith(timestamp, "${currentDateLayout()}, $timePart")
    }

    /** Full date + time using selected date layout: `15/07/2026 02:30 PM`. */
    fun formatDateTime(timestamp: Long): String {
        val timePart = if (is24Hour()) PATTERN_TIME_24 else PATTERN_TIME_12
        return formatWith(timestamp, "${currentDateLayout()} $timePart")
    }

    /**
     * Note card footer on Home / lists — date only (Settings → Date Format).
     * Example: `• 2026-07-15` (no time).
     */
    fun formatCardDate(timestamp: Long): String =
        formatWith(timestamp, "'•' ${currentDateLayout()}")

    /** List / legacy note row — date only using Settings → Date Format. */
    fun formatListDate(timestamp: Long): String = formatDate(timestamp)

    /**
     * Reminder date row: weekday + selected date layout,
     * e.g. `Wed, 2026-07-15`.
     */
    fun formatReminderDate(timestamp: Long): String =
        formatWith(timestamp, "EEE, ${currentDateLayout()}")

    private fun normalizeDatePattern(pattern: String): String {
        val trimmed = pattern.trim()
        return if (trimmed.isBlank()) Constant.DEFAULT_DATE_FORMAT else trimmed
    }

    private fun formatWith(timestamp: Long, pattern: String): String {
        // Locale.US keeps 12h markers as "AM"/"PM". Locale.getDefault() can emit
        // "a. m." / "p. m." (and similar), which looks wrong in the note header.
        val formatter = SimpleDateFormat(pattern, Locale.US)
        val formatted = formatter.format(Date(timestamp))
        if (is24Hour()) return formatted
        return normalizeAmPmMarker(formatted)
    }

    /** Collapses locale variants like "a.m.", "a. m.", "am" into "AM" / "PM". */
    private fun normalizeAmPmMarker(formatted: String): String =
        formatted
            .replace(Regex("""\ba\.?\s*m\.?\b""", RegexOption.IGNORE_CASE), "AM")
            .replace(Regex("""\bp\.?\s*m\.?\b""", RegexOption.IGNORE_CASE), "PM")

    private const val PATTERN_TIME_12 = "hh:mm a"
    private const val PATTERN_TIME_24 = "HH:mm"
}
