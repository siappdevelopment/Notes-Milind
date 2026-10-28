package com.notebook.notes.checklist.flow

import android.app.Activity
import android.util.Log
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
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

    /** True when the PREMIUM step in [steps] was inserted for a session paywall. */
    private var pendingSessionPaywall: Boolean = false

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
        pendingSessionPaywall = false
        val screenFlow = resolveScreenFlow(config)
        val filtered = screenFlow.filter { shouldShow(it) }
        steps = if (filtered.none { it == FlowStep.HOME }) {
            filtered + FlowStep.HOME
        } else {
            filtered
        }
        currentIndex = -1
        isInitialized = true
        sessionManager.markStartupHandledPaywall()
        Log.d(
            TAG,
            "Built flow: $steps (paywallRc=${config.showPaywallAfterOnboarding}, " +
                "sessionInterval=${config.paywallSessionInterval}, sessionPaywall=$pendingSessionPaywall, " +
                "session=${sessionManager.getTotalSessionCount()})"
        )
    }

    /**
     * Two independent paywall flows (never both on the same launch):
     *
     * **Flow A – Onboarding paywall** (Firebase only, no SessionManager):
     *   Language → Onboarding → Permission → [PaywallActivity] → Home
     *   when [FlowConfig.showPaywallAfterOnboarding] is true (one-time).
     *
     * **Flow B – Session paywall** (separate; [SessionManager] + interval RC only):
     *   … → [PaywallActivity] → Home every N cold launches.
     *   Evaluated only if Flow A did not insert PREMIUM this launch.
     */
    private fun resolveScreenFlow(config: FlowConfig): List<FlowStep> {
        val flow = config.screenFlow.toMutableList()

        // Flow A — Firebase show_paywall_after_onboarding (does NOT use SessionManager)
        if (insertOnboardingPaywallIfNeeded(flow, config)) {
            pendingSessionPaywall = false
            return flow
        }

        // Flow B — session interval (independent; skipped when Flow A ran)
        if (insertSessionPaywallIfNeeded(flow, config)) {
            pendingSessionPaywall = true
        }
        return flow
    }

    /**
     * Onboarding / post-permission paywall. Controlled only by
     * [FlowConfig.showPaywallAfterOnboarding] — never by session count.
     */
    private fun insertOnboardingPaywallIfNeeded(
        flow: MutableList<FlowStep>,
        config: FlowConfig
    ): Boolean {
        if (!config.showPaywallAfterOnboarding) return false
        if (isPostOnboardingPaywallShown()) return false
        if (FlowStep.PREMIUM in flow) return false

        insertPremiumBeforeHome(flow)
        Log.d(TAG, "Inserted PREMIUM before HOME (RC show_paywall_after_onboarding=true)")
        return true
    }

    /**
     * Session-based paywall. Independent of onboarding RC / POST_ONBOARDING flag.
     * Marks [last_paywall_session] immediately when inserted so 5 → 10 → 15 stays reliable.
     */
    private fun insertSessionPaywallIfNeeded(
        flow: MutableList<FlowStep>,
        config: FlowConfig
    ): Boolean {
        if (FlowStep.PREMIUM in flow) return false
        if (!sessionManager.shouldShowSessionPaywall(config.paywallSessionInterval)) return false

        insertPremiumBeforeHome(flow)
        sessionManager.markSessionPaywallShown()
        Log.d(
            TAG,
            "Inserted PREMIUM before HOME (session paywall, " +
                "interval=${config.paywallSessionInterval}, session=${sessionManager.getTotalSessionCount()})"
        )
        return true
    }

    private fun insertPremiumBeforeHome(flow: MutableList<FlowStep>) {
        val homeIndex = flow.indexOf(FlowStep.HOME)
        if (homeIndex >= 0) {
            flow.add(homeIndex, FlowStep.PREMIUM)
        } else {
            flow.add(FlowStep.PREMIUM)
        }
    }

    fun markPostOnboardingPaywallShown() {
        preferenceHelper.putBoolean(Constant.POST_ONBOARDING_PAYWALL_SHOWN, true)
    }

    fun isPostOnboardingPaywallShown(): Boolean =
        preferenceHelper.getBoolean(Constant.POST_ONBOARDING_PAYWALL_SHOWN, false)

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
        if (currentStep == FlowStep.PREMIUM) {
            if (pendingSessionPaywall) {
                // last_paywall already committed when inserted; keep dismiss analytics only
                pendingSessionPaywall = false
                try {
                    ADSUtilitis.trackScreen(from, "Session_Paywall_Dismissed")
                } catch (_: Exception) {
                    // analytics must never block navigation
                }
            } else {
                markPostOnboardingPaywallShown()
            }
        }
        var index = currentIndex + 1
        while (index < steps.size) {
            val step = steps[index]
            if (!shouldShow(step) || !navigationCoordinator.canNavigate(step)) {
                index++
                continue
            }
            currentIndex = index
            if (step == FlowStep.PREMIUM && pendingSessionPaywall) {
                try {
                    ADSUtilitis.trackScreen(from, "Session_Paywall_Shown")
                } catch (_: Exception) {
                    // analytics must never block navigation
                }
            }
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
        FlowStep.PREMIUM -> !premiumManager.isPremium()
    }

    companion object {
        private const val TAG = "FlowManager"
    }
}
