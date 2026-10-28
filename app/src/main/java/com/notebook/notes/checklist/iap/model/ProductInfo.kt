package com.notebook.notes.checklist.iap.model

import com.notebook.notes.checklist.iap.PremiumPlan

/**
 * UI-facing product / plan details loaded from Google Play.
 */
data class ProductInfo(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val priceAmountMicros: Long,
    val priceCurrencyCode: String,
    /** INAPP or SUBS */
    val productType: String,
    /** Required for subscription billing flow. */
    val offerToken: String? = null,
    val billingPeriod: String? = null,
    val plan: PremiumPlan? = PremiumPlan.fromProductId(productId),
) {
    val isSubscription: Boolean
        get() = plan?.isSubscription == true
}

sealed class BillingError {
    data object BillingUnavailable : BillingError()
    data object NetworkFailure : BillingError()
    data object ProductNotFound : BillingError()
    data object UserCancelled : BillingError()
    data object PurchaseFailed : BillingError()
    data object PlayStoreMissing : BillingError()
    data object NotReady : BillingError()
    data object AlreadyOwned : BillingError()
    data class Unknown(val responseCode: Int, val debugMessage: String = "") : BillingError()
}

/** Outcome of a user-initiated purchase or restore. */
sealed class PurchaseEvent {
    data object Success : PurchaseEvent()
    data object Restored : PurchaseEvent()
    data class Failed(val error: BillingError) : PurchaseEvent()
    data object NoneFound : PurchaseEvent()
}
