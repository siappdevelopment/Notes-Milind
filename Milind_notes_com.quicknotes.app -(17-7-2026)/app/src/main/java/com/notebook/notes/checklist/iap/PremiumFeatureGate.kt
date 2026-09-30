package com.notebook.notes.checklist.iap

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import com.notebook.notes.checklist.R

/**
 * Central premium gate for feature access.
 *
 * Usage:
 * ```
 * PremiumFeatureGate.requirePremium(this, PremiumFeature.LOCK_NOTE) {
 *     // existing lock flow
 * }
 * ```
 *
 * Free users see [PurchaseDialog]; Continue opens [PaywallActivity].
 * [onGranted] is the last parameter so trailing lambdas bind correctly.
 */
object PremiumFeatureGate {

    fun requirePremium(
        activity: Activity,
        feature: PremiumFeature,
        onDenied: (() -> Unit)? = null,
        onGranted: () -> Unit,
    ) {
        requirePremium(
            context = activity,
            layoutInflater = activity.layoutInflater,
            feature = feature,
            onDenied = onDenied,
            onGranted = onGranted,
        )
    }

    fun requirePremium(
        fragment: Fragment,
        feature: PremiumFeature,
        onDenied: (() -> Unit)? = null,
        onGranted: () -> Unit,
    ) {
        val activity = fragment.activity ?: return
        if (!fragment.isAdded) return
        requirePremium(
            context = activity,
            layoutInflater = fragment.layoutInflater,
            feature = feature,
            onDenied = onDenied,
            onGranted = onGranted,
        )
    }

    fun requirePremium(
        context: Context,
        layoutInflater: LayoutInflater,
        feature: PremiumFeature,
        onDenied: (() -> Unit)? = null,
        onGranted: () -> Unit,
    ) {
        if (isPremiumSafe()) {
            onGranted()
            return
        }
        showPurchaseDialog(
            context = context,
            layoutInflater = layoutInflater,
            feature = feature,
            onDenied = onDenied,
        )
    }

    fun isPremiumSafe(): Boolean = PremiumManager.isPremiumUser()

    private fun showPurchaseDialog(
        context: Context,
        layoutInflater: LayoutInflater,
        feature: PremiumFeature,
        onDenied: (() -> Unit)?,
    ) {
        try {
            PurchaseDialog.show(
                context = context,
                layoutInflater = layoutInflater,
                onContinue = { openPaywall(context) },
                onCancel = onDenied,
                titleRes = feature.titleRes,
                messageRes = feature.messageRes,
                continueRes = R.string.iap_upgrade,

            )
        } catch (_: Exception) {
            onDenied?.invoke()
            openPaywall(context)
        }
    }

    private fun openPaywall(context: Context) {
        try {
            val intent = Intent(context, PaywallActivity::class.java)
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Never crash navigation
        }
    }
}
