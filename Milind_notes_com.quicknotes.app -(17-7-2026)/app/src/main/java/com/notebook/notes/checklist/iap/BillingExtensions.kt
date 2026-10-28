package com.notebook.notes.checklist.iap

import android.content.Context
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.iap.model.BillingError

fun BillingError.toUserMessage(context: Context): String = when (this) {
    BillingError.BillingUnavailable -> context.getString(R.string.iap_error_billing_unavailable)
    BillingError.NetworkFailure -> context.getString(R.string.iap_error_network)
    BillingError.ProductNotFound -> context.getString(R.string.iap_error_product_not_found)
    BillingError.UserCancelled -> context.getString(R.string.iap_error_user_cancelled)
    BillingError.PurchaseFailed -> context.getString(R.string.iap_error_purchase_failed)
    BillingError.PlayStoreMissing -> context.getString(R.string.iap_error_play_store_missing)
    BillingError.NotReady -> context.getString(R.string.iap_error_not_ready)
    BillingError.AlreadyOwned -> context.getString(R.string.iap_error_already_owned)
    is BillingError.Unknown -> context.getString(R.string.iap_error_purchase_failed)
}
