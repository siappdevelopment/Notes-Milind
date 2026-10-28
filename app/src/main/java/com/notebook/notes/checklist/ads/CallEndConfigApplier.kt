package com.notebook.notes.checklist.ads

import android.content.Context
import android.util.Log
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import org.json.JSONArray
import org.json.JSONObject

/**
 * Applies the `callend_screen` block of the Firebase ad JSON to the settings the Call End screen
 * already reads ([ADSMainClass]). Only keys present in the JSON are written, so a partial or
 * missing block leaves earlier values untouched. Never throws.
 */
object CallEndConfigApplier {

    private const val TAG = "CallEndConfig"
    private const val KEY_SCREEN = "callend_screen"

    /** Back-click interstitial block. The key is spelled `call_end_bacK_inter` in the live JSON. */
    private val BACK_INTER_KEYS = listOf("call_end_bacK_inter", "call_end_back_inter")

    fun apply(context: Context, adsJson: String?) {
        if (adsJson.isNullOrBlank()) return
        try {
            val screen = JSONObject(adsJson).optJSONObject(KEY_SCREEN) ?: return
            ADSMainClass.initAppContext(context)
            applyScreen(screen)
            BACK_INTER_KEYS.firstNotNullOfOrNull { screen.optJSONObject(it) }?.let(::applyBackInter)
            Log.d(TAG, "Applied $KEY_SCREEN from Firebase JSON")
        } catch (e: Exception) {
            Log.w(TAG, "Could not apply $KEY_SCREEN; keeping previous values", e)
        }
    }

    private fun applyScreen(o: JSONObject) {
        o.bool("is_call_end_show")?.let(ADSMainClass::setIsShowCallEnd)
        o.bool("app_logo_show")?.let(ADSMainClass::setCallEndLogoShow)
        o.bool("is_callend_bottom_ad_show")?.let(ADSMainClass::setCallEndBottomAdsShow)
        o.str("callend_bottom_ads_type")?.let(ADSMainClass::setCallEndBottomAdsType)
        o.str("callend_banner_ad_id")?.let { ADSMainClass.setStringValue(ADSMainClass.CALL_END_BANNER, it) }
        o.str("callend_native_ad_id")?.let { ADSMainClass.setStringValue(ADSMainClass.CALL_END_Native, it) }
        o.int("notification_install_days")?.let(ADSMainClass::setNotificationInstallDays)
        o.int("notification_call_install_days")?.let(ADSMainClass::setNotificationCallInstallDays)
        o.int("notification_call_overlay_install_days")?.let(ADSMainClass::setNotificationCallOverlayInstallDays)
        o.bool("All_allow_permission_show_fb_notification")?.let(ADSMainClass::setAllAllowPermissionShowFbNotification)
        o.bool("close_button_show_on_full_native_ads")?.let(ADSMainClass::setCloseButtonShowOnFullNativeAds)
        o.bool("country_get_with_ip")?.let(ADSMainClass::setCountryGetWithIp)
        o.list("notification_country")?.let(ADSMainClass::setNotificationCountries)
        o.list("notification_call_country")?.let(ADSMainClass::setNotificationCallCountries)
        o.list("notification_call_overlay_country")?.let(ADSMainClass::setNotificationCallOverlayCountries)
    }

    private fun applyBackInter(o: JSONObject) {
        o.bool("call_end_inter_ads_show")?.let(ADSMainClass::setCallEndInterAdsShow)
        o.bool("notification_screen_back_ads_show")?.let(ADSMainClass::setNotificationScreenBackAdsShow)
        o.int("callend_again_open_count")?.let(ADSMainClass::setCallEndAgainOpenCount)
        o.str("call_end_inter_ads_type")?.let(ADSMainClass::setCallEndInterAdsType)
        o.str("callend_inter_ad_id")?.let { ADSMainClass.setStringValue(ADSMainClass.CALL_END_Inter, it) }
        o.int("call_end_inter_day_count")?.let(ADSMainClass::setCallEndInterDayCount)
        o.int("call_end_inter_active_total_show_count")?.let(ADSMainClass::setCallEndInterShowCount)
        o.list("call_end_inter_ads_show_country")?.let(ADSMainClass::setCallEndAdCountries)
        o.list("call_end_inter_ads_exclude_country")?.let(ADSMainClass::setCallEndAdExcludeCountries)
    }

    private fun JSONObject.bool(key: String): Boolean? =
        if (has(key) && !isNull(key)) optBoolean(key) else null

    private fun JSONObject.int(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null

    private fun JSONObject.str(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).trim() else null

    private fun JSONObject.list(key: String): List<String>? {
        val array: JSONArray = optJSONArray(key) ?: return null
        return List(array.length()) { array.optString(it) }
    }
}
