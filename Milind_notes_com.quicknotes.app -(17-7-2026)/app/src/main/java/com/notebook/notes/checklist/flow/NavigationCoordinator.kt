package com.notebook.notes.checklist.flow

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.ui.activity.CallPermissionActivity
import com.notebook.notes.checklist.ui.activity.HomeActivity
import com.notebook.notes.checklist.ui.activity.LanguageActivity
import com.notebook.notes.checklist.ui.activity.Onboarding1Activity
import com.notebook.notes.checklist.ui.activity.OverLayPermissionActivity
import com.notebook.notes.checklist.ui.activity.PermissionFlowActivity
import com.notebook.notes.checklist.iap.PaywallActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lifecycle-safe activity launches for [FlowStep] and permission UIs.
 * Screens must not hardcode destination activities; they call [FlowManager.next] instead.
 */
@Singleton
class NavigationCoordinator @Inject constructor() {

    fun openStep(from: Activity, step: FlowStep) {
        val target = activityClassFor(step) ?: return
        from.startActivity(Intent(from, target))
        // Hold both windows so consecutive flow steps don't flash the launcher.
        from.overridePendingTransition(R.anim.flow_fade_in, R.anim.flow_fade_out)
    }

    fun openCallPermission(from: Activity) {
        from.startActivity(Intent(from, CallPermissionActivity::class.java))
    }

    fun openOverlayPermission(from: Activity) {
        from.startActivity(Intent(from, OverLayPermissionActivity::class.java))
    }

    /**
     * Returns false for future steps that do not yet have a screen implementation.
     */
    fun canNavigate(step: FlowStep): Boolean = activityClassFor(step) != null

    private fun activityClassFor(step: FlowStep): Class<out Activity>? = when (step) {
        FlowStep.LANGUAGE -> LanguageActivity::class.java
        FlowStep.ONBOARDING -> Onboarding1Activity::class.java
        FlowStep.PERMISSION -> PermissionFlowActivity::class.java
        FlowStep.HOME -> HomeActivity::class.java
        FlowStep.PREMIUM -> PaywallActivity::class.java
        FlowStep.UPDATE,
        FlowStep.RATE_US -> null
    }.also {
    }
}
