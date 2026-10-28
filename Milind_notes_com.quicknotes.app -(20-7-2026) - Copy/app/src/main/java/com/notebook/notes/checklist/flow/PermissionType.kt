package com.notebook.notes.checklist.flow

/**
 * Permission types that can appear in [permission_screen_permissions], in RC order.
 */
enum class PermissionType {
    NOTIFICATION,
    CALL_STATE,
    OVERLAY;

    companion object {
        fun fromRemoteValue(value: String): PermissionType? =
            entries.find { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
