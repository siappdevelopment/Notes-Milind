package com.notebook.notes.checklist.ads

import android.content.Context
import android.util.Log
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import org.json.JSONObject

/**
 * Applies the top-level `App_PrivacyPolicy` and `App_TermsOfUse` URLs of the Firebase ad JSON to the
 * values the Settings and Paywall screens already read. Blank or missing keys keep the previous
 * value. Never throws.
 */
object AppLinksConfigApplier {

    private const val TAG = "AppLinksConfig"
    private const val KEY_PRIVACY_POLICY = "App_PrivacyPolicy"
    private const val KEY_TERMS_OF_USE = "App_TermsOfUse"

    fun apply(context: Context, adsJson: String?) {
        if (adsJson.isNullOrBlank()) return
        try {
            val json = JSONObject(adsJson)
            ADSMainClass.initAppContext(context)
            json.url(KEY_PRIVACY_POLICY)?.let(ADSMainClass::setPrivacyPolicy)
            json.url(KEY_TERMS_OF_USE)?.let(ADSMainClass::setTermsOfUse)
        } catch (e: Exception) {
            Log.w(TAG, "Could not apply app links; keeping previous values", e)
        }
    }

    private fun JSONObject.url(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).trim().takeIf { it.isNotEmpty() } else null
}
