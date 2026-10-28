package com.notebook.notes.checklist.iap

import android.app.Activity
import android.util.Log
import com.notebook.notes.checklist.BuildConfig
import com.notebook.notes.checklist.iap.model.BillingError
import com.notebook.notes.checklist.iap.model.ProductInfo
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.awaitSyncPurchases
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.googleProduct
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, stateless-by-design wrapper over the RevenueCat SDK.
 *
 * It reports what the store says and nothing more: it never decides what "premium" means, never
 * touches preferences and never mutates app state. Every entitlement decision belongs to
 * [PremiumEntitlementResolver], and every state transition to [PremiumStateStore]. That separation
 * is what makes it impossible for a failed or stale request to revoke access.
 *
 * All suspend functions surface failures as [Result.failure] wrapping a [BillingException], so
 * callers get a typed [BillingError] instead of parsing exception messages.
 */
@Singleton
class RevenueCatRepository @Inject constructor() {

    /** productId → RevenueCat Package, needed to launch a purchase. */
    private val packageCache = mutableMapOf<String, Package>()

    private val listenerRegistered = AtomicBoolean(false)

    private val _products = MutableStateFlow<List<ProductInfo>>(emptyList())
    val products: StateFlow<List<ProductInfo>> = _products.asStateFlow()

    /**
     * Every product id the offering advertises, including ones missing from [BillingConstants].
     *
     * These are this app's paywall products as RevenueCat itself reports them, so they are a safe
     * second opinion when checking ownership: it stops a mismatch between the Play Console ids and
     * the hardcoded constants from making a genuinely owned purchase look unowned.
     */
    @Volatile
    var offeringProductIds: Set<String> = emptySet()
        private set

    /**
     * Every CustomerInfo the SDK produces: purchases, renewals, expirations, cancellations and
     * background refreshes. `replay = 1` so a late collector still sees the newest snapshot,
     * `DROP_OLDEST` never suspends the SDK callback thread.
     */
    private val _customerInfoUpdates = MutableSharedFlow<CustomerInfo>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val customerInfoUpdates: SharedFlow<CustomerInfo> = _customerInfoUpdates.asSharedFlow()

    /**
     * Attaches the SDK listener that pushes renewals / expirations while the app runs.
     * Idempotent, and safe to call again once [Purchases.isConfigured] flips to true — the old
     * implementation gave up permanently if it ran one instruction too early.
     */
    fun registerCustomerInfoListener() {
        if (!Purchases.isConfigured) {
            log("registerCustomerInfoListener skipped — Purchases not configured yet")
            return
        }
        if (!listenerRegistered.compareAndSet(false, true)) return

        Purchases.sharedInstance.updatedCustomerInfoListener =
            UpdatedCustomerInfoListener { customerInfo ->
                log("UpdatedCustomerInfoListener fired (requestDate=${customerInfo.requestDate})")
                _customerInfoUpdates.tryEmit(customerInfo)
            }
        log("UpdatedCustomerInfoListener registered (appUserID=${Purchases.sharedInstance.appUserID})")
    }

    fun isReady(): Boolean {
        val configured = Purchases.isConfigured
        if (configured) registerCustomerInfoListener()
        return configured
    }

    /**
     * Reads the entitlement state. [CacheFetchPolicy.FETCH_CURRENT] is required for any read whose
     * result is allowed to revoke access.
     */
    suspend fun fetchCustomerInfo(
        policy: CacheFetchPolicy = CacheFetchPolicy.FETCH_CURRENT,
    ): Result<CustomerInfo> = runCatchingPurchases("fetchCustomerInfo") {
        Purchases.sharedInstance.awaitCustomerInfo(policy)
    }

    /**
     * Posts the receipts this device holds onto the current RevenueCat app user id.
     *
     * Idempotent and cheap, but it cannot claim a receipt that another app user id already owns —
     * that requires [restorePurchases]. So a "not entitled" result here is not proof of ownership.
     */
    suspend fun syncPurchases(): Result<CustomerInfo> = runCatchingPurchases("syncPurchases") {
        Purchases.sharedInstance.awaitSyncPurchases()
    }

    /**
     * Claims the Google account's purchases for the current app user id, transferring or aliasing a
     * receipt that belongs to another one. That makes it the only operation which can recover a
     * purchase after a reinstall, where the receipt still belongs to the previous anonymous id.
     *
     * Used by the Restore button and by the startup recovery. RevenueCat warns that restore may
     * raise OS-level sign-in prompts, but that applies to StoreKit, not Google Play.
     */
    suspend fun restorePurchases(): Result<CustomerInfo> = runCatchingPurchases("restorePurchases") {
        Purchases.sharedInstance.awaitRestore()
    }

    suspend fun loadProducts(): Result<List<ProductInfo>> {
        if (!isReady()) return Result.failure(BillingException(BillingError.NotReady))

        return runCatchingPurchases("loadProducts") {
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val offering = offerings.current
                ?: offerings[BillingConstants.OFFERING_DEFAULT]
                ?: offerings.all.values.firstOrNull()

            if (offering == null) {
                Log.w(BillingConstants.TAG, "No offerings configured in the RevenueCat Dashboard")
                _products.value = emptyList()
                return@runCatchingPurchases emptyList()
            }

            val mapped = mutableListOf<ProductInfo>()
            val resolvedPackages = mutableMapOf<String, Package>()
            val advertised = mutableSetOf<String>()
            for (pkg in offering.availablePackages) {
                val productId = resolveStoreProductId(pkg.product)
                advertised += productId
                if (productId !in BillingConstants.ALL_PRODUCT_IDS) {
                    log("Skip package productId=$productId (rawId=${pkg.product.id})")
                    continue
                }
                resolvedPackages[productId] = pkg
                mapped += pkg.product.toProductInfo(productId)
            }
            // Recorded before the allowlist filter, so ownership checks still recognise a product
            // whose Play id differs from BillingConstants.
            offeringProductIds = advertised

            // Only swap the cache once the new offering parsed, so a failed reload cannot leave the
            // paywall unable to start a purchase.
            synchronized(packageCache) {
                packageCache.clear()
                packageCache.putAll(resolvedPackages)
            }

            // Keep paywall order: week → month → year → lifetime.
            val ordered = PremiumPlan.displayOrder().mapNotNull { plan ->
                mapped.find { it.productId == plan.productId }
            } + mapped.filter { PremiumPlan.fromProductId(it.productId) == null }

            _products.value = ordered
            log("loadProducts: ${ordered.size} plan(s) from offering=${offering.identifier}")
            if (ordered.isEmpty()) {
                Log.w(
                    BillingConstants.TAG,
                    "No plans found. Attach these Play products to the offering: " +
                        "${BillingConstants.ALL_PRODUCT_IDS}",
                )
            }
            ordered
        }
    }

    /**
     * Runs the store purchase sheet and returns its outcome directly, instead of scattering the
     * result across a shared flow where it could be reordered against a background refresh.
     */
    suspend fun purchase(activity: Activity, productId: String): Result<PurchaseOutcome> {
        if (!isReady()) return Result.failure(BillingException(BillingError.NotReady))

        val pkg = synchronized(packageCache) { packageCache[productId] }
        if (pkg == null) {
            val known = synchronized(packageCache) { packageCache.keys.toList() }
            log("Package not found for productId=$productId (cache=$known)")
            return Result.failure(BillingException(BillingError.ProductNotFound))
        }

        return try {
            val result = Purchases.sharedInstance.awaitPurchase(
                PurchaseParams.Builder(activity, pkg).build(),
            )
            Result.success(PurchaseOutcome(result.customerInfo, result.storeTransaction))
        } catch (e: PurchasesTransactionException) {
            val error = if (e.userCancelled) BillingError.UserCancelled else mapError(e.error.code, e.error.message)
            logFailure("purchase", e.error)
            Result.failure(BillingException(error, e))
        } catch (e: PurchasesException) {
            val error = mapError(e.error.code, e.error.message)
            logFailure("purchase", e.error)
            Result.failure(BillingException(error, e))
        }
    }

    private inline fun <T> runCatchingPurchases(operation: String, block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: PurchasesException) {
            val error = mapError(e.error.code, e.error.message)
            logFailure(operation, e.error)
            Result.failure(BillingException(error, e))
        } catch (e: Exception) {
            log("$operation failed: ${e.message}")
            Result.failure(BillingException(BillingError.Unknown(-1, e.message.orEmpty()), e))
        }

    /**
     * RevenueCat puts the actionable detail in [PurchasesError.underlyingErrorMessage], not in
     * `message` — without it a credentials failure reads only as "check the underlying error", which
     * is exactly the detail that was missing.
     */
    private fun logFailure(operation: String, error: PurchasesError) {
        Log.e(
            BillingConstants.TAG,
            "$operation failed: ${error.code.name}(${error.code.code})\n" +
                "  message=${error.message}\n" +
                "  underlying=${error.underlyingErrorMessage}",
        )
        if (error.code == PurchasesErrorCode.InvalidCredentialsError) {
            Log.e(
                BillingConstants.TAG,
                "InvalidCredentialsError(11) is a RevenueCat/Play configuration problem, not a " +
                    "client bug. Verify, in order: (1) the Google public SDK key in " +
                    "BillingConstants.REVENUECAT_API_KEY matches Dashboard → Project settings → " +
                    "API keys, (2) Play Service Credentials JSON is uploaded for this app, " +
                    "(3) the service account holds the required Play Console permissions, " +
                    "(4) freshly created credentials need up to 36 hours to propagate.",
            )
        }
    }

    private fun StoreProduct.toProductInfo(productId: String): ProductInfo {
        val isSub = productId in BillingConstants.SUBSCRIPTION_PRODUCT_IDS
        return ProductInfo(
            productId = productId,
            title = title,
            description = description,
            formattedPrice = price.formatted,
            priceAmountMicros = price.amountMicros,
            priceCurrencyCode = price.currencyCode,
            productType = if (isSub) BillingConstants.PRODUCT_TYPE_SUBS else BillingConstants.PRODUCT_TYPE_INAPP,
            offerToken = defaultSubscriptionOptionId(),
            billingPeriod = period?.iso8601,
        )
    }

    private fun StoreProduct.defaultSubscriptionOptionId(): String? {
        val option: SubscriptionOption? = defaultOption ?: subscriptionOptions?.firstOrNull()
        return option?.id
    }

    /**
     * Maps a RevenueCat [StoreProduct] to the Play Console product id used by [PremiumPlan].
     * Google subscriptions report `StoreProduct.id` as "productId:basePlanId".
     */
    private fun resolveStoreProductId(product: StoreProduct): String {
        val googleId = product.googleProduct?.productId
        if (!googleId.isNullOrBlank()) return googleId
        return product.id.substringBefore(':').ifBlank { product.id }
    }

    private fun mapError(code: PurchasesErrorCode, message: String): BillingError = when (code) {
        PurchasesErrorCode.PurchaseCancelledError -> BillingError.UserCancelled
        PurchasesErrorCode.ProductNotAvailableForPurchaseError -> BillingError.ProductNotFound
        PurchasesErrorCode.ProductAlreadyPurchasedError,
        // The Play receipt belongs to another RevenueCat app user id — only a restore can claim it.
        PurchasesErrorCode.ReceiptAlreadyInUseError,
        -> BillingError.AlreadyOwned
        PurchasesErrorCode.NetworkError -> BillingError.NetworkFailure
        PurchasesErrorCode.StoreProblemError,
        PurchasesErrorCode.PurchaseNotAllowedError,
        -> BillingError.BillingUnavailable
        PurchasesErrorCode.PurchaseInvalidError -> BillingError.PurchaseFailed
        else -> BillingError.Unknown(responseCode = code.code, debugMessage = message)
    }

    private fun log(message: String) {
        if (BuildConfig.DEBUG || BillingConstants.ENABLE_DEBUG_LOGGING) {
            Log.d(BillingConstants.TAG, message)
        }
    }

    /** Result of a completed store purchase. */
    data class PurchaseOutcome(
        val customerInfo: CustomerInfo,
        val storeTransaction: StoreTransaction?,
    )
}

/** Carries a typed [BillingError] so no caller has to pattern-match on exception text. */
class BillingException(
    val error: BillingError,
    cause: Throwable? = null,
) : Exception(error.toString(), cause)
