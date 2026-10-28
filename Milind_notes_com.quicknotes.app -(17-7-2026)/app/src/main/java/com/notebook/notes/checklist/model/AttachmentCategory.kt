package com.notebook.notes.checklist.model

enum class AttachmentCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): AttachmentCategory =
            entries.find { it.name == value } ?: UNKNOWN
    }
}
