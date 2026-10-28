package com.notebook.notes.checklist.iap

import android.util.Log
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.iap.model.PremiumState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for premium access, and the only writer of the premium preferences.
 *
 * Two flows on purpose:
 * - [isPremium] is what the UI, ads and feature gates consume. It starts from the persisted value so
 *   a returning subscriber is never briefly treated as a free user, and it only ever drops on an
 *   authoritative revocation.
 * - [state] is the authority ([PremiumState.Unknown] until RevenueCat answers), which is a different
 *   fact from "not entitled".
 *
 * [applyResolved] enforces a monotonic watermark on `CustomerInfo.requestDate`, so out-of-order
 * responses from the several concurrent producers (purchase result, customer-info listener, startup
 * sync, foreground refresh, manual restore) cannot undo each other.
 */
@Singleton
class PremiumStateStore @Inject constructor(
    private val preferenceHelper: PreferenceHelper,
) {

    private val _isPremium = MutableStateFlow(
        preferenceHelper.getBoolean(BillingConstants.PREF_IS_PREMIUM, false),
    )
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _state = MutableStateFlow<PremiumState>(PremiumState.Unknown)
    val state: StateFlow<PremiumState> = _state.asStateFlow()

    /**
     * In-memory watermark of the newest snapshot applied in this process. Deliberately not
     * persisted: races are process-scoped, and a persisted watermark would make a legitimate
     * post-restart snapshot look stale if server and device clocks disagree.
     */
    private var lastAppliedAtMillis: Long = Long.MIN_VALUE

    val currentPlan: PremiumPlan?
        get() = preferenceHelper.getString(BillingConstants.PREF_PREMIUM_PRODUCT_ID)
            ?.let { PremiumPlan.fromProductId(it) }

    /**
     * @return true when the snapshot was accepted, false when it was discarded as stale.
     */
    @Synchronized
    fun applyResolved(snapshot: PremiumState.Resolved, trigger: String): Boolean {
        // Applied to revocations only. RevenueCat stamps requestDate from a server clock at second
        // granularity, and a startup recovery fires several calls within the same second, which
        // routinely come back with equal or inverted timestamps. Discarding a *grant* on that basis
        // would throw away a purchase the store just confirmed. Keeping a late grant costs nothing,
        // because the only thing that clears premium is a revocation and those are confirmed
        // against Google Play first.
        if (!snapshot.entitled && snapshot.resolvedAtMillis < lastAppliedAtMillis) {
            Log.w(
                BillingConstants.TAG,
                "Discarding stale revocation from $trigger " +
                    "(requestDate=${snapshot.resolvedAtMillis} < applied=$lastAppliedAtMillis)",
            )
            return false
        }
        lastAppliedAtMillis = maxOf(lastAppliedAtMillis, snapshot.resolvedAtMillis)

        if (snapshot.entitled) persistGrant(snapshot) else persistRevocation()
        _state.value = snapshot
        _isPremium.value = snapshot.entitled

        Log.d(
            BillingConstants.TAG,
            "Premium ${if (snapshot.entitled) "granted" else "revoked"} by $trigger " +
                "(plan=${snapshot.productId}, willRenew=${snapshot.willRenew}, " +
                "expiresAt=${snapshot.expiresAtMillis})",
        )
        return true
    }

    fun isInitialSyncDone(): Boolean =
        preferenceHelper.getBoolean(BillingConstants.PREF_INITIAL_SYNC_DONE, false)

    fun markInitialSyncDone() {
        preferenceHelper.putBoolean(BillingConstants.PREF_INITIAL_SYNC_DONE, true)
    }

    private fun persistGrant(snapshot: PremiumState.Resolved) {
        preferenceHelper.putBoolean(BillingConstants.PREF_IS_PREMIUM, true)
        snapshot.productId?.let {
            preferenceHelper.putString(BillingConstants.PREF_PREMIUM_PRODUCT_ID, it)
        }
        preferenceHelper.putBoolean(
            BillingConstants.PREF_PREMIUM_IS_SUBSCRIPTION,
            snapshot.isSubscription,
        )
        preferenceHelper.putBoolean(BillingConstants.PREF_PREMIUM_WILL_RENEW, snapshot.willRenew)
        preferenceHelper.putLong(
            BillingConstants.PREF_PREMIUM_EXPIRES_AT,
            snapshot.expiresAtMillis ?: 0L,
        )
    }

    private fun persistRevocation() {
        preferenceHelper.putBoolean(BillingConstants.PREF_IS_PREMIUM, false)
        preferenceHelper.remove(BillingConstants.PREF_PREMIUM_PRODUCT_ID)
        preferenceHelper.putBoolean(BillingConstants.PREF_PREMIUM_IS_SUBSCRIPTION, false)
        preferenceHelper.putBoolean(BillingConstants.PREF_PREMIUM_WILL_RENEW, false)
        preferenceHelper.remove(BillingConstants.PREF_PREMIUM_EXPIRES_AT)
    }
}
