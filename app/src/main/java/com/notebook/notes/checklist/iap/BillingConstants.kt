package com.notebook.notes.checklist.iap

/**
 * Play Console / RevenueCat product IDs for lifetime (INAPP) + weekly / monthly / yearly (SUBS).
 *
 * Create these in Play Console → Monetize, then import them into RevenueCat:
 * - In-app products → [PRODUCT_PREMIUM_LIFETIME]
 * - Subscriptions → [PRODUCT_PREMIUM_WEEKLY], [PRODUCT_PREMIUM_MONTHLY], [PRODUCT_PREMIUM_YEARLY]
 *
 * RevenueCat Dashboard: create one Entitlement named [ENTITLEMENT_PREMIUM] and attach all four products.
 */
object BillingConstants {
    const val TAG = "NotesBilling"

    const val PRODUCT_PREMIUM_LIFETIME = "premium_lifetime"
    const val PRODUCT_PREMIUM_WEEKLY = "premium_weekly"
    const val PRODUCT_PREMIUM_MONTHLY = "premium_monthly"
    const val PRODUCT_PREMIUM_YEARLY = "premium_yearly"

    val INAPP_PRODUCT_IDS: List<String> = listOf(PRODUCT_PREMIUM_LIFETIME)

    val SUBSCRIPTION_PRODUCT_IDS: List<String> = listOf(
        PRODUCT_PREMIUM_WEEKLY,
        PRODUCT_PREMIUM_MONTHLY,
        PRODUCT_PREMIUM_YEARLY,
    )

    val ALL_PRODUCT_IDS: List<String> = INAPP_PRODUCT_IDS + SUBSCRIPTION_PRODUCT_IDS

    /** SharedPreferences keys. Written only by [PremiumStateStore]. */
    const val PREF_IS_PREMIUM = "pref_is_premium"
    const val PREF_PREMIUM_PRODUCT_ID = "pref_premium_product_id"
    const val PREF_PREMIUM_IS_SUBSCRIPTION = "pref_premium_is_subscription"
    const val PREF_PREMIUM_WILL_RENEW = "pref_premium_will_renew"
    const val PREF_PREMIUM_EXPIRES_AT = "pref_premium_expires_at"

    /**
     * False after a fresh install / reinstall until Play purchases have been pushed to this
     * install's RevenueCat app user id at least once. Drives the automatic entitlement recovery
     * so the user never has to tap "Restore Purchases" after reinstalling.
     */
    const val PREF_INITIAL_SYNC_DONE = "pref_initial_sync_done"

    /**
     * How long startup may wait for the first authoritative entitlement answer before continuing
     * with the cached value. Keeps a dead network from ever blocking the splash screen.
     *
     * Only safe for an install that has already synced at least once, because only then is there a
     * trustworthy cached value to fall back to.
     */
    const val STARTUP_SYNC_TIMEOUT_MS: Long = 4_000L

    /**
     * Budget for the first launch of an install, where falling back to the cache means answering
     * "not premium" to a paying user — there is nothing cached yet to fall back to.
     *
     * The reinstall recovery has to complete a customer-info fetch, bind Google Play Billing, query
     * the account's purchases and post them to a brand-new app user id, sometimes followed by a
     * restore. On a cold start that competes with Remote Config, consent and the app-open ad, which
     * routinely puts it past [STARTUP_SYNC_TIMEOUT_MS].
     */
    const val FIRST_LAUNCH_SYNC_TIMEOUT_MS: Long = 15_000L

    const val ENABLE_DEBUG_LOGGING = true

    /** Product type identifiers (kept for [ProductInfo.productType] compatibility). */
    const val PRODUCT_TYPE_SUBS: String = "subs"
    const val PRODUCT_TYPE_INAPP: String = "inapp"

    /**
     * RevenueCat Google Play public SDK key (Dashboard → Project settings → API keys).
     * Safe to ship in the app binary; never put a secret API key here.
     */
    const val REVENUECAT_API_KEY: String = "goog_CKDGlGBtISqqddfsmAulrBpXRms"

    /**
     * Must match the Entitlement Identifier in RevenueCat Dashboard.
     * Attach weekly / monthly / yearly / lifetime products to this entitlement.
     */
    const val ENTITLEMENT_PREMIUM: String = "Note App Pro"

    /** Optional Offering identifier. Leave as "default" unless you use a named offering. */
    const val OFFERING_DEFAULT: String = "default"
}

enum class PremiumPlan(
    val productId: String,
    val productType: String,
) {
    WEEKLY(
        productId = BillingConstants.PRODUCT_PREMIUM_WEEKLY,
        productType = BillingConstants.PRODUCT_TYPE_SUBS,
    ),
    MONTHLY(
        productId = BillingConstants.PRODUCT_PREMIUM_MONTHLY,
        productType = BillingConstants.PRODUCT_TYPE_SUBS,
    ),
    YEARLY(
        productId = BillingConstants.PRODUCT_PREMIUM_YEARLY,
        productType = BillingConstants.PRODUCT_TYPE_SUBS,
    ),
    LIFETIME(
        productId = BillingConstants.PRODUCT_PREMIUM_LIFETIME,
        productType = BillingConstants.PRODUCT_TYPE_INAPP,
    );

    val isSubscription: Boolean
        get() = productType == BillingConstants.PRODUCT_TYPE_SUBS

    companion object {
        fun fromProductId(productId: String): PremiumPlan? =
            entries.find { it.productId == productId }

        fun displayOrder(): List<PremiumPlan> = listOf(WEEKLY, MONTHLY, YEARLY, LIFETIME)
    }
}
