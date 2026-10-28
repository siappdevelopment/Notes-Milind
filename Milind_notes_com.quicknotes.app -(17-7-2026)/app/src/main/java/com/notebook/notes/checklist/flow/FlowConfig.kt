package com.notebook.notes.checklist.flow

/**
 * Snapshot of Firebase Remote Config values that drive app navigation.
 */
data class FlowConfig(
    val screenFlow: List<FlowStep>,
    val splashPermissionMode: SplashPermissionMode,
    val permissionScreenPermissions: List<PermissionType>,
    val onboardingCount: Int = DEFAULT_ONBOARDING_COUNT,
    val showPaywallAfterOnboarding: Boolean = false,
    /** Sessions between session-based paywalls; 0 = disabled. */
    val paywallSessionInterval: Int = DEFAULT_PAYWALL_SESSION_INTERVAL
) {
    companion object {
        const val DEFAULT_ONBOARDING_COUNT = 3
        const val MIN_ONBOARDING_COUNT = 0
        const val MAX_ONBOARDING_COUNT = 3
        const val DEFAULT_PAYWALL_SESSION_INTERVAL = 0

        fun defaults(): FlowConfig = FlowConfig(
            screenFlow = listOf(
                FlowStep.LANGUAGE,
                FlowStep.ONBOARDING,
                FlowStep.PERMISSION,
                FlowStep.HOME
            ),
            // Keep in sync with res/xml/remote_config_defaults.xml when XML cannot be read.
            splashPermissionMode = SplashPermissionMode.BOTH,
            // Match previous Welcome → Call (phone + notification) → Overlay chain.
            permissionScreenPermissions = listOf(
                PermissionType.CALL_STATE,
                PermissionType.NOTIFICATION,
                PermissionType.OVERLAY
            ),
            onboardingCount = DEFAULT_ONBOARDING_COUNT,
            // Keep in sync with res/xml/remote_config_defaults.xml when XML cannot be read.
            showPaywallAfterOnboarding = true,
            paywallSessionInterval = DEFAULT_PAYWALL_SESSION_INTERVAL
        )
    }
}
