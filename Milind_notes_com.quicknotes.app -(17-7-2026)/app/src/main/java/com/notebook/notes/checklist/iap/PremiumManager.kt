package com.notebook.notes.checklist.iap

import android.app.Activity
import android.util.Log
import com.notebook.notes.checklist.BuildConfig
import com.notebook.notes.checklist.iap.model.BillingError
import com.notebook.notes.checklist.iap.model.PremiumState
import com.notebook.notes.checklist.iap.model.ProductInfo
import com.notebook.notes.checklist.iap.model.PurchaseEvent
import com.notebook.notes.checklist.iap.model.grantsPremium
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium entitlement facade for the whole app: lifetime + weekly / monthly / yearly.
 *
 * Data flows in one direction, through one funnel:
 *
 *     RevenueCat → RevenueCatRepository → PremiumEntitlementResolver → PremiumStateStore
 *               → isPremiumFlow → UI, feature gates, ads
 *
 * Every producer of entitlement information (purchase result, SDK customer-info listener, startup
 * sync, foreground refresh, manual restore) ends up in [applyCustomerInfo]. Nothing else may change
 * premium access, and a request that fails produces no snapshot at all, so a network error or a
 * stale response can never downgrade a paying user.
 */
@Singleton
class PremiumManager @Inject constructor(
    private val revenueCatRepository: RevenueCatRepository,
    private val entitlementResolver: PremiumEntitlementResolver,
    private val premiumStateStore: PremiumStateStore,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Serializes the sync/restore routines so two of them never interleave store calls. */
    private val syncMutex = Mutex()

    /**
     * Guards the revocation reconciliation. `syncPurchases` itself makes the SDK emit a fresh
     * CustomerInfo through this same funnel, so without this a "not entitled" answer would
     * reconcile forever.
     */
    private val reconciling = AtomicBoolean(false)

    /**
     * Effective access. Seeded from the persisted value so a returning subscriber is premium from
     * the first frame, and lowered only by an authoritative revocation.
     */
    val isPremiumFlow: StateFlow<Boolean> = premiumStateStore.isPremium

    /** Authoritative entitlement snapshot; [PremiumState.Unknown] until RevenueCat answers. */
    val premiumState: StateFlow<PremiumState> = premiumStateStore.state

    /**
     * Flips to true once the first entitlement sync of this process has finished, whether it found
     * an entitlement or not. Startup waits on this instead of guessing from the stale cache.
     */
    private val _initialSyncComplete = MutableStateFlow(false)
    val initialSyncComplete: StateFlow<Boolean> = _initialSyncComplete.asStateFlow()

    /** Exposed straight from the repository — one copy of the catalogue, no mirror to drift. */
    val products: StateFlow<List<ProductInfo>> = revenueCatRepository.products

    /**
     * One-shot outcomes of *user-initiated* actions only. Background refreshes deliberately do not
     * emit here, or every Home resume would raise a "Purchase successful" toast nobody asked for.
     */
    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    @Volatile
    private var started = false

    init {
        instance = this
    }

    fun isPremium(): Boolean = premiumStateStore.isPremium.value

    fun currentPlan(): PremiumPlan? = premiumStateStore.currentPlan

    /**
     * Persisted once-per-install flag. False on a fresh install / reinstall until Splash (or the
     * Paywall Restore button) has completed a restore attempt.
     */
    fun isPurchaseRestoreChecked(): Boolean = premiumStateStore.isInitialSyncDone()

    fun markPurchaseRestoreChecked() {
        premiumStateStore.markInitialSyncDone()
    }

    /**
     * Called once from the Application, and harmlessly again from screens that may be the process
     * entry point (for example [PaywallActivity] after process death).
     */
    @Synchronized
    fun start() {
        if (started) return
        started = true

        revenueCatRepository.registerCustomerInfoListener()

        // Renewals, expirations, cancellations and refunds arrive here for the whole process
        // lifetime — no screen has to poll for them.
        scope.launch {
            revenueCatRepository.customerInfoUpdates.collect { customerInfo ->

                applyCustomerInfo(customerInfo, trigger = "customerInfoListener")
            }
        }
        scope.launch { revenueCatRepository.loadProducts() }
        scope.launch { performInitialSync() }
    }

    /**
     * Suspends until the first CustomerInfo fetch of this process has finished, or until
     * [timeoutMs] elapses. Fresh-install restore itself is owned by Splash — see
     * [restorePurchasesForSplash] — so this wait only covers configure + fetch readiness.
     *
     * @return true when RevenueCat answered in time.
     */
    suspend fun awaitInitialSync(
        timeoutMs: Long = if (premiumStateStore.isInitialSyncDone()) {
            BillingConstants.STARTUP_SYNC_TIMEOUT_MS
        } else {
            BillingConstants.FIRST_LAUNCH_SYNC_TIMEOUT_MS
        },
    ): Boolean {
        if (_initialSyncComplete.value) return true
        val completed = withTimeoutOrNull(timeoutMs) { _initialSyncComplete.first { it } } != null
        if (!completed) {
            Log.w(
                BillingConstants.TAG,
                "Initial entitlement sync did not finish within ${timeoutMs}ms — " +
                    "continuing with cached premium=${isPremium()}",
            )
        }
        return completed
    }

    /**
     * Same restore condition as [PaywallActivity]'s Restore button, run once from Splash before ads.
     *
     * - Already premium → mark checked, skip store call.
     * - Already checked this install → skip store call.
     * - Otherwise → [restorePurchases] (identical SDK path to Paywall).
     *
     * @return true when the user is premium after this call.
     */
    suspend fun restorePurchasesForSplash(): Boolean {
        if (isPremium()) {
            markPurchaseRestoreChecked()
            Log.d(BillingConstants.TAG, "Splash restore skipped — already premium")
            return true
        }
        if (isPurchaseRestoreChecked()) {
            Log.d(BillingConstants.TAG, "Splash restore skipped — already checked this install")
            return isPremium()
        }
        if (!revenueCatRepository.isReady()) {
            Log.e(BillingConstants.TAG, "Splash restore skipped — RevenueCat not configured")
            return false
        }

        // Bounded so a store that keeps answering 503 can never hold the splash screen open.
        return withTimeoutOrNull(SPLASH_RESTORE_TIMEOUT_MS) { restoreWithRetries() } ?: run {
            Log.w(
                BillingConstants.TAG,
                "Splash restore exceeded ${SPLASH_RESTORE_TIMEOUT_MS}ms — the flag stays unchecked " +
                    "so the next cold start retries",
            )
            isPremium()
        }
    }

    private suspend fun restoreWithRetries(): Boolean {
        // [resolve] scores ownership against the ids the live offering advertises, and [start] loads
        // that offering in a coroutine running parallel to this one. Restoring while the set is
        // still empty lets a genuinely owned purchase resolve as "nothing owned", which then burns
        // the once-per-install flag and disables restore for the rest of the install.
        if (revenueCatRepository.offeringProductIds.isEmpty()) {
            revenueCatRepository.loadProducts()
        }

        repeat(SPLASH_RESTORE_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                delay(SPLASH_RESTORE_RETRY_DELAY_MS * attempt)
                Log.d(BillingConstants.TAG, "Retrying splash restore (attempt ${attempt + 1})")
            }

            when (val event = restorePurchases()) {
                is PurchaseEvent.Restored -> {
                    Log.d(BillingConstants.TAG, "Splash restore: Restored on attempt ${attempt + 1}")
                    return true
                }
                PurchaseEvent.NoneFound -> {
                    markPurchaseRestoreChecked()
                    Log.d(BillingConstants.TAG, "Splash restore: NoneFound")
                    return false
                }
                is PurchaseEvent.Failed -> {
                    Log.w(
                        BillingConstants.TAG,
                        "Splash restore: Failed (${event.error}) on attempt ${attempt + 1}",
                    )
                    // The flag stays unchecked either way, so the next cold start tries again.
                    if (!event.error.isRetryableRestore()) return false
                }
                else -> {
                    markPurchaseRestoreChecked()
                    return isPremium()
                }
            }
        }
        return false
    }

    /**
     * Whether a failed restore is worth immediately repeating.
     *
     * RevenueCat validates a restored receipt by calling Google Play from its own backend, and that
     * hop answers 503 while it is cold — surfacing as InvalidCredentialsError(11) under
     * [BillingError.Unknown]. A single attempt at the first second of a cold start is the worst
     * possible moment to ask, which is why the identical call from the Paywall succeeds later.
     */
    private fun BillingError.isRetryableRestore(): Boolean = when (this) {
        BillingError.NetworkFailure,
        BillingError.BillingUnavailable,
        BillingError.NotReady,
        is BillingError.Unknown,
        -> true
        else -> false
    }

    /**
     * First CustomerInfo fetch of the process. Does not restore — Splash owns that via
     * [restorePurchasesForSplash], matching PaywallActivity's Restore button.
     */
    private suspend fun performInitialSync() {
        try {
            syncMutex.withLock {
                if (!revenueCatRepository.isReady()) {
                    Log.e(BillingConstants.TAG, "RevenueCat is not configured — cannot sync entitlements")
                    return@withLock
                }

                val fetched = revenueCatRepository.fetchCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
                val entitled = fetched.getOrNull()
                    ?.let { applyCustomerInfo(it, trigger = "startupFetch") } == true

                if (entitled) {
                    markPurchaseRestoreChecked()
                }
            }
        } finally {
            // Startup must be released even if the sync threw, or Splash would wait for the
            // full timeout on every launch.
            _initialSyncComplete.value = true
        }
    }

    /**
     * Hook for [androidx.lifecycle.ProcessLifecycleOwner] ON_START.
     *
     * On a cold start the initial sync is already doing this work, so refreshing again would just
     * cost a second round trip. Only a genuine return from background triggers a refresh.
     */
    fun onAppForegrounded() {
        if (!_initialSyncComplete.value) return
        scope.launch { refreshPremiumStatus() }
    }

    /**
     * Re-reads entitlements, typically once per app foreground. Safe to call at any time: a failure
     * leaves the current state untouched.
     */
    suspend fun refreshPremiumStatus(): Boolean = syncMutex.withLock {
        if (!revenueCatRepository.isReady()) return isPremium()

        // Never downgrade because the store or network was unavailable.
        val customerInfo = revenueCatRepository.fetchCustomerInfo(CacheFetchPolicy.FETCH_CURRENT)
            .getOrNull() ?: return isPremium()

        applyCustomerInfo(customerInfo, trigger = "refresh")
        return isPremium()
    }

    fun purchasePlan(activity: Activity, productId: String): BillingError? {
        PremiumPlan.fromProductId(productId) ?: return BillingError.ProductNotFound
        if (currentPlan() == PremiumPlan.LIFETIME) return BillingError.AlreadyOwned

        scope.launch {
            revenueCatRepository.purchase(activity, productId).fold(
                onSuccess = { outcome ->
                    // The purchase response is the freshest possible snapshot, so it always wins
                    // the watermark comparison against a refresh that was already in flight.
                    applyCustomerInfo(outcome.customerInfo, trigger = "purchase")
                    _events.emit(PurchaseEvent.Success)
                },
                onFailure = { throwable ->
                    Log.e(BillingConstants.TAG, "Purchase failed for $productId", throwable)
                    _events.emit(PurchaseEvent.Failed(throwable.toBillingError()))
                },
            )
        }
        return null
    }

    /** User tapped "Restore Purchases". */
    suspend fun restorePurchases(): PurchaseEvent = syncMutex.withLock {
        if (!revenueCatRepository.isReady()) return PurchaseEvent.Failed(BillingError.NotReady)

        val customerInfo = revenueCatRepository.restorePurchases().getOrElse { throwable ->
            return PurchaseEvent.Failed(throwable.toBillingError())
        }

        if (applyCustomerInfo(customerInfo, trigger = "manualRestore")) {
            // A successful reclaim is proof enough that Play answered for this install.
            markPurchaseRestoreChecked()
            return PurchaseEvent.Restored
        }

        // "Nothing owned" must not permanently disable the automatic startup recovery until the
        // process-owned fetch has finished. An early restore against an empty anonymous customer
        // used to set the Boolean and skip Splash restore forever after a reinstall.
        if (_initialSyncComplete.value) markPurchaseRestoreChecked()
        return PurchaseEvent.NoneFound
    }

    suspend fun refreshProducts(): List<ProductInfo> =
        // A failed reload must not blank out prices that are already on screen.
        revenueCatRepository.loadProducts().getOrNull() ?: products.value

    /**
     * The single funnel every entitlement update passes through.
     *
     * Granting is immediate. Revoking is not: the RevenueCat customer record alone is not enough
     * evidence to take access away from someone who currently has it. The record can legitimately
     * not show a purchase that Google Play still owns — the dashboard entitlement may not be
     * attached to the product, or the receipt may not be linked to this app user id yet — which is
     * how premium could survive a purchase and then vanish on the next cold start.
     *
     * @return true when the app is premium after applying this snapshot.
     */
    private suspend fun applyCustomerInfo(customerInfo: CustomerInfo, trigger: String): Boolean {
        val snapshot = resolve(customerInfo)
        logSnapshot(customerInfo, snapshot, trigger)

        // A grant, or a "not entitled" for someone who has no access to lose, is applied directly.
        if (snapshot.entitled || !isPremium()) return commit(snapshot, trigger)

        if (!reconciling.compareAndSet(false, true)) {
            // A reconciliation is already deciding this exact question and it is the authority.
            // Its own syncPurchases call re-emits CustomerInfo through this same funnel, so
            // committing here would write the unconfirmed revocation the guard exists to prevent.
            Log.d(
                BillingConstants.TAG,
                "$trigger reports no entitlement while a reconciliation is in flight — deferring",
            )
            return isPremium()
        }
        return try {
            confirmRevocationAgainstStore(customerInfo, trigger)
        } finally {
            reconciling.set(false)
        }
    }

    /**
     * Asks Google Play, not just RevenueCat, before believing that access is gone. If the
     * reconciliation cannot run at all, access is left alone rather than dropped on unverified data.
     */
    private suspend fun confirmRevocationAgainstStore(
        customerInfo: CustomerInfo,
        trigger: String,
    ): Boolean {
        Log.w(
            BillingConstants.TAG,
            "$trigger reports no entitlement while access is granted — " +
                "reconciling Play purchases before revoking",
        )

        // The offering loads in parallel with the startup sync, so its product ids may not have
        // arrived yet. A Play product id that differs from BillingConstants must not be the reason
        // someone loses access.
        if (revenueCatRepository.offeringProductIds.isEmpty()) {
            revenueCatRepository.loadProducts()
            val reResolved = resolve(customerInfo)
            if (reResolved.entitled) {
                Log.w(
                    BillingConstants.TAG,
                    "Ownership recognised once the offering loaded — premium kept. The owned " +
                        "product id is missing from BillingConstants; add it.",
                )
                return commit(reResolved, "$trigger→offeringReresolve")
            }
        }

        val reconciled = revenueCatRepository.syncPurchases().getOrNull() ?: run {
            Log.w(BillingConstants.TAG, "Reconciliation failed — keeping current access")
            return isPremium()
        }

        val synced = resolve(reconciled)
        logSnapshot(reconciled, synced, "$trigger→syncPurchases")
        Log.w(
            BillingConstants.TAG,
            if (synced.entitled) {
                "Entitlement recovered by syncPurchases — the customer record was out of sync " +
                    "with the Play account, premium kept"
            } else {
                "Google Play holds no active purchase for this account — revocation confirmed"
            },
        )
        return commit(synced, "$trigger→syncPurchases")
    }

    /**
     * Always resolves against the product ids the live offering advertises, so a Play Console id
     * that BillingConstants does not list still counts as ownership.
     */
    private fun resolve(customerInfo: CustomerInfo): PremiumState.Resolved =
        entitlementResolver.resolve(customerInfo, revenueCatRepository.offeringProductIds)

    private fun commit(snapshot: PremiumState.Resolved, trigger: String): Boolean {
        val accepted = premiumStateStore.applyResolved(snapshot, trigger)
        return if (accepted) snapshot.entitled else premiumStateStore.state.value.grantsPremium
    }

    /**
     * Prints exactly what RevenueCat returned. Misconfigured dashboards and rotated app user ids
     * are indistinguishable from an expired subscription without this.
     */
    private fun logSnapshot(
        customerInfo: CustomerInfo,
        snapshot: PremiumState.Resolved,
        trigger: String,
    ) {
        if (!BuildConfig.DEBUG && !BillingConstants.ENABLE_DEBUG_LOGGING) return
        val entitlement = customerInfo.entitlements[BillingConstants.ENTITLEMENT_PREMIUM]
        Log.d(
            BillingConstants.TAG,
            "CustomerInfo[$trigger] entitled=${snapshot.entitled} plan=${snapshot.productId}\n" +
                "  appUserID=${runCatching { Purchases.sharedInstance.appUserID }.getOrNull()}" +
                " originalAppUserId=${customerInfo.originalAppUserId}\n" +
                "  requestDate=${customerInfo.requestDate}\n" +
                "  entitlement '${BillingConstants.ENTITLEMENT_PREMIUM}'=" +
                (entitlement?.let {
                    "isActive=${it.isActive} product=${it.productIdentifier} " +
                        "expires=${it.expirationDate} willRenew=${it.willRenew} " +
                        "sandbox=${it.isSandbox} verification=${it.verification}"
                } ?: "MISSING — check the entitlement identifier in the RevenueCat Dashboard") + "\n" +
                "  allEntitlements=${customerInfo.entitlements.all.keys}" +
                " active=${customerInfo.entitlements.active.keys}\n" +
                "  activeSubscriptions=${customerInfo.activeSubscriptions}\n" +
                "  nonSubscriptions=${customerInfo.nonSubscriptionTransactions.map { it.productIdentifier }}\n" +
                "  allPurchased=${customerInfo.allPurchasedProductIds}\n" +
                "  appExpects=${BillingConstants.ALL_PRODUCT_IDS}",
        )
    }

    private fun Throwable.toBillingError(): BillingError =
        (this as? BillingException)?.error ?: BillingError.PurchaseFailed

    companion object {
        /** Splash restore attempts before the launch gives up and defers to the next cold start. */
        private const val SPLASH_RESTORE_ATTEMPTS = 3
        private const val SPLASH_RESTORE_RETRY_DELAY_MS = 700L

        /** Ceiling for the whole retry sequence, so the splash never waits on a failing store. */
        private const val SPLASH_RESTORE_TIMEOUT_MS = 8_000L

        @Volatile
        private var instance: PremiumManager? = null

        /**
         * Bridge for the Java ads layer, which cannot be injected. Reads the same StateFlow value
         * as every Kotlin consumer, so there is still a single source of truth.
         */
        @JvmStatic
        fun isPremiumUser(): Boolean = instance?.isPremium() == true
    }
}
