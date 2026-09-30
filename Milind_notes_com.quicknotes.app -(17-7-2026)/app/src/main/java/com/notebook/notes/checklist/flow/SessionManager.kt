package com.notebook.notes.checklist.flow

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.iap.PaywallActivity
import com.notebook.notes.checklist.iap.PremiumManager
import com.notebook.notes.checklist.utils.Constant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks app-open sessions and decides when to show the session-based paywall.
 * Independent of the one-time onboarding paywall.
 *
 * Persistence: [PreferenceHelper] (`total_session_count`, `last_paywall_session`).
 *
 * A session increments when the app comes to the foreground from background
 * (including cold start). In-app activity changes and rotation do not increment
 * ([ProcessLifecycleOwner] only fires when the whole app stops/starts).
 */
@Singleton
class SessionManager @Inject constructor(
    private val preferenceHelper: PreferenceHelper,
    private val remoteConfigManager: RemoteConfigManager
) {

    /** True while the app is considered backgrounded (also initial state before first ON_START). */
    @Volatile
    private var isInBackground: Boolean = true

    /**
     * When true, Splash/FlowManager already handled paywall for this foreground period
     * so Home must not present a second one.
     */
    @Volatile
    var startupHandledPaywallThisForeground: Boolean = false
        private set

    fun getTotalSessionCount(): Int =
        preferenceHelper.getInt(Constant.TOTAL_SESSION_COUNT, 0)

    fun getLastPaywallSession(): Int =
        preferenceHelper.getInt(Constant.LAST_PAYWALL_SESSION, 0)

    /** Called from [com.notebook.notes.checklist.di.MyApplication] on app foreground. */
    fun onAppForegrounded() {
        if (!isInBackground) return
        isInBackground = false
        startupHandledPaywallThisForeground = false
        val next = getTotalSessionCount() + 1
        preferenceHelper.putInt(Constant.TOTAL_SESSION_COUNT, next)
        Log.d(TAG, "Session incremented (foreground) → $next")
        try {
            // Context-free analytics not available here; Splash/Home may also log.
        } catch (_: Exception) {
        }
    }

    /** Called from [com.notebook.notes.checklist.di.MyApplication] on app background. */
    fun onAppBackgrounded() {
        isInBackground = true
        startupHandledPaywallThisForeground = false
    }

    /** Splash/Flow path already evaluated paywall for this open — Home should skip. */
    fun markStartupHandledPaywall() {
        startupHandledPaywallThisForeground = true
    }

    /**
     * Whether a session-based paywall should be shown.
     * Uses (currentSession - lastPaywall) >= interval — not modulo.
     */
    fun shouldShowSessionPaywall(interval: Int = remoteConfigManager.getPaywallSessionInterval()): Boolean {
        if (interval <= 0) return false
        if (PremiumManager.isPremiumUser()) return false
        val current = getTotalSessionCount()
        val last = getLastPaywallSession()
        val due = (current - last) >= interval
        Log.d(
            TAG,
            "shouldShowSessionPaywall interval=$interval current=$current last=$last due=$due"
        )
        return due
    }

    /** Commit interval immediately when session paywall is presented. */
    fun markSessionPaywallShown() {
        val current = getTotalSessionCount()
        preferenceHelper.putInt(Constant.LAST_PAYWALL_SESSION, current)
        Log.d(TAG, "Session paywall marked at session $current")
    }

    /**
     * For warm starts that land on Home (Splash skipped): present PaywallActivity when due.
     * No-ops if startup flow already handled paywall this foreground period.
     */
    fun maybePresentSessionPaywallFromHome(activity: Activity) {
        if (startupHandledPaywallThisForeground) return
        if (activity.isFinishing) return
        if (PremiumManager.isPremiumUser()) return
        if (!shouldShowSessionPaywall()) return

        markSessionPaywallShown()
        markStartupHandledPaywall()
        try {
            ADSUtilitis.trackScreen(activity, "Session_Paywall_Shown")
        } catch (_: Exception) {
        }
        activity.startActivity(Intent(activity, PaywallActivity::class.java))
        Log.d(TAG, "Presented session paywall from Home (session=${getTotalSessionCount()})")
    }

    companion object {
        private const val TAG = "SessionManager"
    }
}
