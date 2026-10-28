package com.notebook.notes.checklist.Advertisement

import android.util.Log
import com.notebook.notes.checklist.iap.BillingConstants
import com.notebook.notes.checklist.iap.PremiumManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only bridge between premium and ads, and it points one way.
 *
 * This lives in the ads layer on purpose: the ads SDK observes premium, premium knows nothing about
 * ads. Previously `SplashActivity` wrote `ADSMainClass.setAds_Free(true)` once at launch, which made
 * the ads SDK a second, silently diverging copy of the entitlement — it was never updated after a
 * mid-session purchase or an expiry, and Remote Config overwrote it on every cold start.
 */
@Singleton
class PremiumAdsSynchronizer @Inject constructor(
    private val premiumManager: PremiumManager,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var started = false

    @Synchronized
    fun start() {
        if (started) return
        started = true

        scope.launch {
            premiumManager.isPremiumFlow.collect { isPremium ->
                ADSMainClass.applyAdsFree()
                Log.d(
                    BillingConstants.TAG,
                    "Ads follow premium: premium=$isPremium → adsFree=${ADSMainClass.getAds_Free()}",
                )
            }
        }
    }
}
