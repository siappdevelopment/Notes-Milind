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

/**
 * Normalized purchase payload used after Play Billing callbacks.
 */
data class PurchaseInfo(
    val productId: String,
    val purchaseToken: String,
    val orderId: String?,
    val purchaseState: Int,
    val isAcknowledged: Boolean,
    val purchaseTime: Long,
    val originalJson: String,
    val signature: String,
    val isRestore: Boolean = false,
    val isAutoRenewing: Boolean = false,
    val isSubscription: Boolean = PremiumPlan.fromProductId(productId)?.isSubscription == true,
)

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

sealed class PurchaseEvent {
    data class Success(val purchase: PurchaseInfo) : PurchaseEvent()
    data class Restored(val purchase: PurchaseInfo) : PurchaseEvent()
    data class Failed(val error: BillingError) : PurchaseEvent()
    data object NoneFound : PurchaseEvent()
}
