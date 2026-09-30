package com.note.quick.pad.model

enum class RepeatType {
    NEVER,
    DAILY,
    WEEKLY,
    MONTHLY;

    companion object {
        fun fromStorage(value: String?): RepeatType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: NEVER

        fun fromDisplayLabel(label: String?): RepeatType = when {
            label.equals("Daily", ignoreCase = true) -> DAILY
            label.equals("Weekly", ignoreCase = true) -> WEEKLY
            label.equals("Monthly", ignoreCase = true) -> MONTHLY
            else -> NEVER
        }
    }

    fun toDisplayLabel(): String = when (this) {
        NEVER -> "Never"
        DAILY -> "Daily"
        WEEKLY -> "Weekly"
        MONTHLY -> "Monthly"
    }
}
