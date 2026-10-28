package com.notebook.notes.checklist.flow

/**
 * Screens that can appear in the Remote Config–driven [screen_flow] sequence.
 * Splash is always first and is not part of this enum.
 */
enum class FlowStep {
    LANGUAGE,
    ONBOARDING,
    PERMISSION,
    HOME,
    UPDATE,
    RATE_US,
    PREMIUM;

    companion object {
        fun fromRemoteValue(value: String): FlowStep? =
            entries.find { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
