package com.notebook.notes.checklist.flow

import android.app.Activity
import android.util.Log
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.iap.PremiumManager
import com.notebook.notes.checklist.ui.activity.HomeActivity
import com.notebook.notes.checklist.utils.Constant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the dynamic screen sequence from Remote Config, tracks position,
 * and advances via [next] / [previous]. Returning-user prefs skip completed steps.
 */
@Singleton
class FlowManager @Inject constructor(
    private val preferenceHelper: PreferenceHelper,
    private val navigationCoordinator: NavigationCoordinator,
    private val remoteConfigManager: RemoteConfigManager,
    private val sessionManager: SessionManager,
    private val premiumManager: PremiumManager,
    private val onboardingManager: OnboardingManager
) {

    /**
     * Whether onboarding is due on THIS cold launch, decided once by SplashActivity before the
     * flow is built. Fixed for the launch so recording the launch (which bumps the counter)
     * cannot flip the answer mid-flow. Null (e.g. after process death) falls back to a live read.
     */
    var onboardingDueThisLaunch: Boolean? = null

    private var steps: List<FlowStep> = emptyList()
    private var currentIndex: Int = -1
    private var onboardingCount: Int = FlowConfig.DEFAULT_ONBOARDING_COUNT

    @Volatile
    var isInitialized: Boolean = false
        private set

    val currentStep: FlowStep?
        get() = steps.getOrNull(currentIndex)

    /**
     * One-time migration for users who already finished onboarding under the old flow
     * so they are not forced back through the permission step.
     */
    fun migrateLegacyCompletionFlags() {
        if (isOnboardingCompleted() && !preferenceHelper.contains(Constant.IS_PERMISSION_COMPLETED)) {
            preferenceHelper.putBoolean(Constant.IS_PERMISSION_COMPLETED, true)
        }
        if (isLanguageCompleted() && !preferenceHelper.contains(Constant.LANGUAGE_COMPLETED)) {
            preferenceHelper.putBoolean(Constant.LANGUAGE_COMPLETED, true)
        }
        if (isOnboardingCompleted() && !preferenceHelper.contains(Constant.ONBOARDING_COMPLETED)) {
            preferenceHelper.putBoolean(Constant.ONBOARDING_COMPLETED, true)
        }
    }

    fun buildFlow(config: FlowConfig = remoteConfigManager.getFlowConfig()) {
        migrateLegacyCompletionFlags()
        onboardingCount = config.onboardingCount
        val filtered = config.screenFlow.filter { it != FlowStep.PREMIUM && shouldShow(it) }
        steps = if (filtered.none { it == FlowStep.HOME }) {
            filtered + FlowStep.HOME
        } else {
            filtered
        }
        currentIndex = -1
        isInitialized = true
        Log.d(TAG, "Built flow: $steps")
    }

    fun start(from: Activity) {
        if (!isInitialized) {
            buildFlow(remoteConfigManager.getFlowConfig())
        }
        currentIndex = -1
        next(from)
    }

    /**
     * Advance to the next unfinished, navigable step and finish [from].
     */
    fun next(from: Activity) {
        if (!isInitialized) {
            buildFlow(remoteConfigManager.getFlowConfig())
        }
        var index = currentIndex + 1
        while (index < steps.size) {
            val step = steps[index]
            if (!shouldShow(step) || !navigationCoordinator.canNavigate(step)) {
                index++
                continue
            }
            currentIndex = index
            navigationCoordinator.openStep(from, step)
            if (from !is HomeActivity) {
                finishFlowActivity(from)
            }
            return
        }
        currentIndex = steps.indexOf(FlowStep.HOME).takeIf { it >= 0 } ?: steps.lastIndex
        navigationCoordinator.openStep(from, FlowStep.HOME)
        if (from !is HomeActivity) {
            finishFlowActivity(from)
        }
    }

    fun previous(from: Activity) {
        if (!isInitialized || currentIndex <= 0) return
        var index = currentIndex - 1
        while (index >= 0) {
            val step = steps[index]
            if (navigationCoordinator.canNavigate(step)) {
                currentIndex = index
                navigationCoordinator.openStep(from, step)
                finishFlowActivity(from)
                return
            }
            index--
        }
    }

    /** Finish without the default close animation (avoids brief launcher flash). */
    private fun finishFlowActivity(from: Activity) {
        from.finish()
        from.overridePendingTransition(R.anim.flow_fade_in, R.anim.flow_fade_out)
    }

    fun markLanguageCompleted() {
        preferenceHelper.putBoolean(Constant.IS_LANGUAGE_SELECTED, true)
        preferenceHelper.putBoolean(Constant.LANGUAGE_COMPLETED, true)
    }

    /** Called when the user finishes or skips onboarding. */
    fun markOnboardingCompleted() {
        preferenceHelper.putBoolean(Constant.IS_ONBOARDING_COMPLTED, true)
        preferenceHelper.putBoolean(Constant.ONBOARDING_COMPLETED, true)
        onboardingManager.onOnboardingCompleted()
    }

    fun markPermissionCompleted() {
        preferenceHelper.putBoolean(Constant.IS_PERMISSION_COMPLETED, true)
    }

    fun isLanguageCompleted(): Boolean =
        preferenceHelper.getBoolean(Constant.LANGUAGE_COMPLETED, false) ||
            preferenceHelper.getBoolean(Constant.IS_LANGUAGE_SELECTED, false)

    fun isOnboardingCompleted(): Boolean =
        preferenceHelper.getBoolean(Constant.ONBOARDING_COMPLETED, false) ||
            preferenceHelper.getBoolean(Constant.IS_ONBOARDING_COMPLTED, false)

    fun isPermissionCompleted(): Boolean =
        preferenceHelper.getBoolean(Constant.IS_PERMISSION_COMPLETED, false)

    private fun shouldShow(step: FlowStep): Boolean = when (step) {
        FlowStep.LANGUAGE -> !isLanguageCompleted()
        FlowStep.ONBOARDING ->
            (onboardingDueThisLaunch ?: onboardingManager.shouldShowOnboarding()) && onboardingCount > 0
        FlowStep.PERMISSION -> !isPermissionCompleted()
        FlowStep.HOME -> true
        FlowStep.UPDATE,
        FlowStep.RATE_US -> true
        // Reads the premium state itself rather than the raw preference, so there is one reader of
        // one truth. SplashActivity waits for the entitlement sync before building the flow.
        FlowStep.PREMIUM -> false
    }

    companion object {
        private const val TAG = "FlowManager"
    }
}
