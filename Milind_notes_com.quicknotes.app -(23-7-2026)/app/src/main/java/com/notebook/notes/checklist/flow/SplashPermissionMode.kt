package com.notebook.notes.checklist.flow

/**
 * Controls which runtime permissions Splash requests before entering [screen_flow].
 */
enum class SplashPermissionMode {
    NONE,
    NOTIFICATION,
    CALL_STATE,
    BOTH;

    companion object {
        fun fromRemoteValue(value: String): SplashPermissionMode =
            entries.find { it.name.equals(value.trim(), ignoreCase = true) } ?: NONE
    }
}
