package com.notebook.notes.checklist.iap.model

/**
 * Authoritative premium snapshot.
 *
 * The [Unknown] case is the important one: "RevenueCat has not answered yet" is a different fact
 * from "this user is not entitled". Without that distinction a cold start, an offline launch or a
 * failed request all look identical to an expired subscription, and premium gets revoked.
 *
 * Only a [Resolved] snapshot may change entitlement. Failures produce no snapshot at all.
 */
sealed interface PremiumState {

    data object Unknown : PremiumState

    /**
     * A successful CustomerInfo read.
     *
     * @param resolvedAtMillis `CustomerInfo.requestDate` — the server timestamp of the snapshot.
     *   Used as a monotonic watermark so an older in-flight response can never overwrite a newer
     *   one (the race that revokes premium seconds after a purchase).
     */
    data class Resolved(
        val entitled: Boolean,
        val productId: String?,
        val isSubscription: Boolean,
        val willRenew: Boolean,
        val expiresAtMillis: Long?,
        val resolvedAtMillis: Long,
    ) : PremiumState
}

/** True only when RevenueCat has explicitly confirmed an active entitlement. */
val PremiumState.grantsPremium: Boolean
    get() = this is PremiumState.Resolved && entitled
