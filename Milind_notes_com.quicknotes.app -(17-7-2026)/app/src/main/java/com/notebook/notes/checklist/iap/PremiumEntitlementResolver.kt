package com.notebook.notes.checklist.iap

import android.util.Log
import com.notebook.notes.checklist.iap.model.PremiumState
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.VerificationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one and only place that decides whether a [CustomerInfo] means "premium".
 *
 * Keeping the rule here — rather than at each call site that reads entitlements — makes it
 * auditable and testable, and stops two readers reaching different conclusions from one payload.
 */
@Singleton
class PremiumEntitlementResolver @Inject constructor() {

    /**
     * @param storeProductIds product ids the RevenueCat offering advertises for this app. Consulted
     *   in addition to [BillingConstants], so a Play Console id that does not match the hardcoded
     *   constants cannot make an owned purchase look unowned and revoke a paying user.
     */
    fun resolve(
        customerInfo: CustomerInfo,
        storeProductIds: Set<String> = emptySet(),
    ): PremiumState.Resolved {
        val resolvedAt = customerInfo.requestDate.time

        entitlementGrant(customerInfo, resolvedAt)?.let { return it }
        // A missing or misnamed dashboard entitlement must never lock out a paying user, so raw
        // store ownership is consulted before concluding "not entitled".
        ownershipGrant(customerInfo, resolvedAt, storeProductIds)?.let { return it }

        return PremiumState.Resolved(
            entitled = false,
            productId = null,
            isSubscription = false,
            willRenew = false,
            expiresAtMillis = null,
            resolvedAtMillis = resolvedAt,
        )
    }

    private fun entitlementGrant(customerInfo: CustomerInfo, resolvedAt: Long): PremiumState.Resolved? {
        val entitlement = customerInfo.entitlements[BillingConstants.ENTITLEMENT_PREMIUM] ?: return null
        if (!entitlement.isActive) return null
        if (entitlement.verification == VerificationResult.FAILED) {
            Log.w(BillingConstants.TAG, "Entitlement signature verification FAILED — ignoring snapshot")
            return null
        }

        val productId = normalize(entitlement.productIdentifier)
        return PremiumState.Resolved(
            entitled = true,
            productId = productId,
            isSubscription = entitlement.expirationDate != null ||
                productId in BillingConstants.SUBSCRIPTION_PRODUCT_IDS,
            willRenew = entitlement.willRenew,
            expiresAtMillis = entitlement.expirationDate?.time,
            resolvedAtMillis = resolvedAt,
        )
    }

    private fun ownershipGrant(
        customerInfo: CustomerInfo,
        resolvedAt: Long,
        storeProductIds: Set<String>,
    ): PremiumState.Resolved? {
        val lifetimeIds = BillingConstants.INAPP_PRODUCT_IDS + storeProductIds
        val lifetime = customerInfo.nonSubscriptionTransactions
            .map { normalize(it.productIdentifier) }
            .firstOrNull { it in lifetimeIds }
        if (lifetime != null) {
            warnMisconfigured(lifetime)
            return PremiumState.Resolved(
                entitled = true,
                productId = lifetime,
                isSubscription = false,
                willRenew = false,
                expiresAtMillis = null,
                resolvedAtMillis = resolvedAt,
            )
        }

        val subscriptionIds = BillingConstants.SUBSCRIPTION_PRODUCT_IDS + storeProductIds
        val activeSubRawId = customerInfo.activeSubscriptions
            .firstOrNull { normalize(it) in subscriptionIds }
            ?: return null
        val productId = normalize(activeSubRawId)
        warnMisconfigured(productId)
        return PremiumState.Resolved(
            entitled = true,
            productId = productId,
            isSubscription = true,
            willRenew = true,
            expiresAtMillis = expirationOf(customerInfo, activeSubRawId, productId),
            resolvedAtMillis = resolvedAt,
        )
    }

    private fun expirationOf(customerInfo: CustomerInfo, rawId: String, productId: String): Long? =
        (customerInfo.getExpirationDateForProductId(rawId)
            ?: customerInfo.getExpirationDateForProductId(productId))?.time

    private fun warnMisconfigured(productId: String) {
        Log.w(
            BillingConstants.TAG,
            "Product '$productId' is owned but entitlement '${BillingConstants.ENTITLEMENT_PREMIUM}' " +
                "is not active. Granting premium from store ownership — attach the product to the " +
                "entitlement in the RevenueCat Dashboard.",
        )
    }

    /** Google Play subscription identifiers arrive as "productId:basePlanId". */
    private fun normalize(productId: String?): String =
        productId.orEmpty().substringBefore(':')
}
