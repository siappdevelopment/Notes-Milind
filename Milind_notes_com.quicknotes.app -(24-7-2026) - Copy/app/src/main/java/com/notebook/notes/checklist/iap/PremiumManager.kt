package com.notebook.notes.checklist.iap

import android.app.Activity
import android.util.Log
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.iap.model.BillingError
import com.notebook.notes.checklist.iap.model.ProductInfo
import com.notebook.notes.checklist.iap.model.PurchaseEvent
import com.notebook.notes.checklist.iap.model.PurchaseInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Premium entitlement for lifetime + weekly / monthly / yearly subscriptions.
 */
@Singleton
class PremiumManager @Inject constructor(
    private val preferenceHelper: PreferenceHelper,
    private val billingManager: BillingManager,
    private val purchaseManager: PurchaseManager,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isPremium = MutableStateFlow(preferenceHelper.getBoolean(BillingConstants.PREF_IS_PREMIUM, false))
    val isPremiumFlow: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _products = MutableStateFlow<List<ProductInfo>>(emptyList())
    val products: StateFlow<List<ProductInfo>> = _products.asStateFlow()

    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    @Volatile
    private var observing = false

    init {
        instance = this
    }

    fun isPremium(): Boolean = _isPremium.value

    fun currentPlan(): PremiumPlan? =
        preferenceHelper.getString(BillingConstants.PREF_PREMIUM_PRODUCT_ID)
            ?.let { PremiumPlan.fromProductId(it) }

    fun start() {
        billingManager.start()
        if (observing) return
        observing = true

        // Keep ads flag in sync with existing local premium (e.g. after reinstall prefs restore).
        if (_isPremium.value) {
            ADSMainClass.setAds_Free(true)
        }

        scope.launch {
            billingManager.products.collect { _products.value = it }
        }
        scope.launch {
            billingManager.purchaseUpdates.collect { result ->
                result.fold(
                    onSuccess = { purchases ->
                        val valid = purchases.filter { purchaseManager.isValidPremiumPurchase(it) }
                        if (valid.isNotEmpty()) {
                            val latest = pickBestEntitlement(valid)
                            grantPremium(latest)
                            val event = if (latest.isRestore) {
                                PurchaseEvent.Restored(latest)
                            } else {
                                PurchaseEvent.Success(latest)
                            }
                            _events.emit(event)
                        } else {
                            // Successful empty query (expired sub / nothing owned).

                            clearLocalPremium()
                        }
                    },
                    onFailure = { throwable ->
                        Log.e("Milind", "start: ${throwable}" )
                        _events.emit(PurchaseEvent.Failed(mapFailure(throwable)))
                    },
                )
            }
        }

        scope.launch(Dispatchers.IO) {
            autoRestoreFromPlay()
        }
    }

    suspend fun autoRestoreFromPlay(): Boolean {
        if (!billingManager.ensureConnected()) return isPremium()
        val result = billingManager.queryPurchases(isRestore = true)
        val purchases = result.getOrNull().orEmpty()
        val valid = purchases.filter { purchaseManager.isValidPremiumPurchase(it) }
        return if (valid.isNotEmpty()) {
            grantPremium(pickBestEntitlement(valid).copy(isRestore = true))
            true
        } else {
            if (result.isSuccess) clearLocalPremium()
            isPremium()
        }
    }

    fun purchasePlan(activity: Activity, productId: String): BillingError? {
        val plan = PremiumPlan.fromProductId(productId)
            ?: return BillingError.ProductNotFound

        // Lifetime owners already have permanent access.
        if (currentPlan() == PremiumPlan.LIFETIME && plan == PremiumPlan.LIFETIME) {
            return BillingError.AlreadyOwned
        }

        return billingManager.launchPurchaseFlow(activity, productId)
    }

    /** @deprecated Prefer [purchasePlan]. Kept for older call sites. */
    fun purchasePremium(activity: Activity): BillingError? =
        purchasePlan(activity, BillingConstants.PRODUCT_PREMIUM_LIFETIME)

    suspend fun restorePurchases(): PurchaseEvent {
        val result = billingManager.queryPurchases(isRestore = true)
        return result.fold(
            onSuccess = { purchases ->
                val valid = purchases.filter { purchaseManager.isValidPremiumPurchase(it) }
                if (valid.isEmpty()) {
                    clearLocalPremium()
                    PurchaseEvent.NoneFound
                } else {
                    val latest = pickBestEntitlement(valid).copy(isRestore = true)
                    grantPremium(latest)
                    PurchaseEvent.Restored(latest)
                }
            },
            onFailure = {
                PurchaseEvent.Failed(BillingError.NetworkFailure)
            },
        )
    }

    suspend fun refreshProducts(): List<ProductInfo> {
        billingManager.ensureConnected()
        val loaded = billingManager.loadProducts().getOrDefault(emptyList())
        _products.value = loaded
        return loaded
    }

    private fun pickBestEntitlement(purchases: List<PurchaseInfo>): PurchaseInfo {
        // Prefer lifetime over any subscription.
        return purchases.firstOrNull { it.productId == BillingConstants.PRODUCT_PREMIUM_LIFETIME }
            ?: purchases.maxByOrNull { it.purchaseTime }
            ?: purchases.first()
    }

    private fun grantPremium(info: PurchaseInfo) {
        preferenceHelper.putBoolean(BillingConstants.PREF_IS_PREMIUM, true)
        preferenceHelper.putString(BillingConstants.PREF_PREMIUM_PRODUCT_ID, info.productId)
        preferenceHelper.putString(BillingConstants.PREF_PREMIUM_PURCHASE_TOKEN, info.purchaseToken)
        preferenceHelper.putString(BillingConstants.PREF_PREMIUM_ORDER_ID, info.orderId)
        preferenceHelper.putBoolean(BillingConstants.PREF_PREMIUM_IS_SUBSCRIPTION, info.isSubscription)
        _isPremium.value = true
        // Purchase + restore both grant premium → disable ads.
        ADSMainClass.setAds_Free(true)
    }

    fun clearLocalPremium() {
        preferenceHelper.putBoolean(BillingConstants.PREF_IS_PREMIUM, false)
        preferenceHelper.remove(BillingConstants.PREF_PREMIUM_PRODUCT_ID)
        preferenceHelper.remove(BillingConstants.PREF_PREMIUM_PURCHASE_TOKEN)
        preferenceHelper.remove(BillingConstants.PREF_PREMIUM_ORDER_ID)
        preferenceHelper.putBoolean(BillingConstants.PREF_PREMIUM_IS_SUBSCRIPTION, false)
        _isPremium.value = false
        ADSMainClass.setAds_Free(false)
    }

    private fun mapFailure(throwable: Throwable): BillingError {
        val message = throwable.message.orEmpty()
        return when {
            message.contains("USER_CANCELED", ignoreCase = true) -> BillingError.UserCancelled
            message.contains("PRODUCT_NOT_FOUND", ignoreCase = true) -> BillingError.ProductNotFound
            message.contains("AlreadyOwned", ignoreCase = true) -> BillingError.AlreadyOwned
            message.contains("BillingUnavailable", ignoreCase = true) -> BillingError.BillingUnavailable
            message.contains("NetworkFailure", ignoreCase = true) -> BillingError.NetworkFailure
            message.contains("PlayStore", ignoreCase = true) -> BillingError.PlayStoreMissing
            message.contains("NotReady", ignoreCase = true) -> BillingError.NotReady
            else -> BillingError.PurchaseFailed
        }
    }

    companion object {
        @Volatile
        private var instance: PremiumManager? = null

        @JvmStatic
        fun isPremiumUser(): Boolean = instance?.isPremium() == true
    }
}
