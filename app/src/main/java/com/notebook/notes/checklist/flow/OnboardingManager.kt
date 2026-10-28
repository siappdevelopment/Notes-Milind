package com.notebook.notes.checklist.flow

/** Persistence for [OnboardingManager]; a plain interface so the logic can be unit-tested on the JVM. */
interface OnboardingStore {
    /** True once onboarding has been finished (or skipped) at least once. */
    var hasSeenOnboarding: Boolean

    /** Normal (non-onboarding) cold launches since onboarding was last completed. */
    var launchesSinceOnboarding: Int
}

/**
 * Decides whether a cold launch should show onboarding.
 *
 * - The first launch always shows it.
 * - With a repeat count of N > 0 it is shown again after N normal launches, forever.
 * - With a repeat count of 0 (or negative) it is shown only once.
 *
 * Onboarding that was started but never completed leaves the state untouched, so it is shown again.
 */
class OnboardingManager(
    private val store: OnboardingStore,
    private val repeatCount: () -> Int,
) {

    /** Pure read; safe to call any number of times within one launch. */
    fun shouldShowOnboarding(): Boolean {
        if (!store.hasSeenOnboarding) return true
        val repeat = repeatCount().coerceAtLeast(0)
        // `>=` (not `==`): a counter already past a lowered repeat count still triggers onboarding.
        return repeat > 0 && store.launchesSinceOnboarding >= repeat
    }

    /** Call when the user finishes or skips onboarding. Restarts the cycle. */
    fun onOnboardingCompleted() {
        store.hasSeenOnboarding = true
        store.launchesSinceOnboarding = 0
    }

    /** Call once per cold launch that did NOT show onboarding. */
    fun recordNormalLaunch() {
        store.launchesSinceOnboarding += 1
    }
}
