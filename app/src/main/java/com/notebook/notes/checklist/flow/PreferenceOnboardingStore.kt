package com.notebook.notes.checklist.flow

import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.utils.Constant

/** [OnboardingStore] backed by the app's SharedPreferences. */
class PreferenceOnboardingStore(
    private val preferenceHelper: PreferenceHelper,
) : OnboardingStore {

    // Users who finished onboarding before this feature existed only have the legacy flags set;
    // count them as "seen" so they are not forced through onboarding again.
    override var hasSeenOnboarding: Boolean
        get() = preferenceHelper.getBoolean(KEY_HAS_SEEN_ONBOARDING, false) ||
            preferenceHelper.getBoolean(Constant.ONBOARDING_COMPLETED, false) ||
            preferenceHelper.getBoolean(Constant.IS_ONBOARDING_COMPLTED, false)
        set(value) = preferenceHelper.putBoolean(KEY_HAS_SEEN_ONBOARDING, value)

    override var launchesSinceOnboarding: Int
        get() = preferenceHelper.getInt(KEY_LAUNCHES_SINCE_ONBOARDING, 0)
        set(value) = preferenceHelper.putInt(KEY_LAUNCHES_SINCE_ONBOARDING, value)

    private companion object {
        const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"
        const val KEY_LAUNCHES_SINCE_ONBOARDING = "launches_since_onboarding"
    }
}
