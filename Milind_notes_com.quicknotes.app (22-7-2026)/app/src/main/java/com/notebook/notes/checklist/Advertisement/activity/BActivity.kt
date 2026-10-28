package com.notebook.notes.checklist.Advertisement.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity


import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.notebook.notes.checklist.Advertisement.ADSAppManage
import com.notebook.notes.checklist.Advertisement.ADSAppStarting
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.Advertisement.ManegeUtilsView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.AESHelper
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

open class BActivity : AppCompatActivity() {
    var iv_firebaseRemoteConfig: FirebaseRemoteConfig? = null
    private lateinit var version: String

    /** Called after ads remote-config / consent / splash app-open finish. */
    open fun initActivity() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        ADSMainClass.initAppContext(applicationContext)
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LOW_PROFILE
            )
        setContentView(R.layout.activity_splash)

        // Fetch FCM Token immediately on startup
//        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
//            .addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    String token = task.getResult();
//                    android.util.Log.d("FCM_TOKEN", "Startup FCM Token: " + token);
//                } else {
//                    android.util.Log.e("FCM_TOKEN", "Startup FCM Token fetch failed", task.getException());
//                }
//            });
        FirebaseApp.initializeApp(this)
        ADSAppManage.FastStart = true
        ADSAppStarting.ADSIsShowing = false
        ADSAppManage.AppStartingScreenOpen = false

        ManegeUtilsView.isUtilsManege(this)
//         val original = "AppVersion_"
//        val encrypted = AESHelper.encrypt("hngldvbgtyhnmjui",original)
//        val decrypted = AESHelper.decrypt("hngldvbgtyhnmjui","U2FsdGVkX19OHZJG6rf+ca+YFZm+Zgiq6B99COPmDHo=" ?: "")

//        Log.d("ddddd", "Encrypted: $encrypted")
//        Log.d("ddddd", "Decrypted: $decrypted")

        val versionName: String? = getPackageManager().getPackageInfo(getPackageName(), 0).versionName
        val result = versionName?.replace(".", "_")

        version = ManegeUtilsView.ssfsfsfsf + result
//        Log.d("ddddd", "versionName: $versionName")
//        Log.d("ddddd", "result: $result")
//        Log.d("ddddd", "result: $version")


        showing_to_all_data()
        //Key Hash
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures!!) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun showing_to_all_data() {
        if (!ADSUtilitis.IsNetworkConnected(this)) {
            Handler(Looper.getMainLooper()).postDelayed({
                continueAfterConsent { app_next_screen_showing() }
            }, 500)
            return
        }
        iv_firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()
        iv_firebaseRemoteConfig!!.setConfigSettingsAsync(configSettings)

        iv_firebaseRemoteConfig!!.setDefaultsAsync(R.xml.default_config)

        iv_firebaseRemoteConfig!!.fetchAndActivate()
            .addOnCompleteListener(this, OnCompleteListener<Boolean?> { task ->
                if (task.isSuccessful) {
                    try {

                        val jsonobject: JSONObject =
                            JSONArray(iv_firebaseRemoteConfig!!.getString(version)).getJSONObject(0)
                        Log.e("Milind", "showing_to_all_data: ${jsonobject}", )
                        val admob_native_btn_color = jsonobject.getString("Admob_Native_ButtonColor")
                        val admob_native_btn_text_color = jsonobject.getString("Admob_Native_ButtonTextColor")
                        val ads_type_load = jsonobject.getString("Ads_Loading_Types")
                        val privacy_policy = jsonobject.getString("App_PrivacyPolicy")
                        val splash_ads_type = jsonobject.getString("Splash_Ad_LoadType")
                        val ads_click = jsonobject.getLong("ADSClick")
                        val back_click = jsonobject.getLong("BacksClick")
                        val native_by_page = jsonobject.getLong("Native_ByPage")
                        val ads_blocker = jsonobject.getBoolean("AdsBlocks")
                        val coming_soon = jsonobject.getBoolean("Coming_Soon")
                        val exit_ads = jsonobject.getBoolean("ExitDialog")
                        val firebaseanalytics = jsonobject.getBoolean("Firebaseanalytics")
                        val update_coming_soon = jsonobject.getBoolean("Update_Coming_Soon")
                        val consent_form_show_screen = jsonobject.getString("consent_form_show_screen")



                        val screen = jsonobject.getJSONObject("screen")

                        // Splash Screen
                        val splash_screen = screen.getJSONObject("splash_screen")
                        val splah_app_open_ads_show = splash_screen.getBoolean("splash_app_open_ads_show")
                        val splash_app_open_after_count = splash_screen.getInt("splash_app_open_after_count")
                        val app_open_show_in_background = splash_screen.getBoolean("app_open_show_in_background")
                        val app_open_daily_show_count = splash_screen.getInt("app_open_daily_show_count")
                        val app_open_id = splash_screen.getString("app_open_id")
                        ADSMainClass.setSplashAppOpenShow(splah_app_open_ads_show)
                        ADSMainClass.setSplashAppOpenAfterCount(splash_app_open_after_count)
                        ADSMainClass.setAppOpenBackgroundShow(app_open_show_in_background)
                        ADSMainClass.setAppOpenAdDailyLimit(app_open_daily_show_count)

                        // Language Screen
                        val language_screen = screen.getJSONObject("language_screen")
                        val splash_to_language = language_screen.getBoolean("splash_to_language")
                        val language_screen_bottom_ad_show =
                            language_screen.getBoolean("language_screen_bottom_ad_show")
                        val language_ads_type = language_screen.getString("language_ads_type")
                        val language_inter_ads_show =
                            language_screen.getBoolean("language_inter_ads_show")
                        val language_banner_id = language_screen.getString("language_banner_id")
                        val language_native_id = language_screen.getString("language_native_id")
                        ADSMainClass.setSplashToLanguage(splash_to_language)
                        ADSMainClass.setLanguageScreenBottomAdShow(language_screen_bottom_ad_show)
                        ADSMainClass.setLanguageAdsType(language_ads_type)
                        ADSMainClass.setLanguageInterAdsShow(language_inter_ads_show)



                        // onboarding Screen


                        val onboarding_screen = screen.getJSONObject("language_screen")

                        val onboarding_screen_bottom_ad_show =
                            language_screen.getBoolean("language_screen_bottom_ad_show")
                        val onboarding_ads_type = language_screen.getString("language_ads_type")
                        val onboarding_inter_ads_show =
                            language_screen.getBoolean("language_inter_ads_show")
                        val onboarding_banner_id = language_screen.getString("language_banner_id")
                        val onboarding_native_id = language_screen.getString("language_native_id")

                        ADSMainClass.setOnboardingScreenBottomAdShow(onboarding_screen_bottom_ad_show)
                        ADSMainClass.setOnboardingType(onboarding_ads_type)
                        ADSMainClass.setOnboardingInterAdsShow(onboarding_inter_ads_show)


                        // Home Screen
                        val home_screen = screen.getJSONObject("home_screen")
                        val home_screen_bottom_show =
                            home_screen.getBoolean("home_screen_bottom_show")
                        val home_screen_ads_type = home_screen.getString("home_screen_ads_type")
                        val home_banner_id = home_screen.getString("home_banner_id")
                        val home_native_id = home_screen.getString("home_native_id")
                        val home_screen_native1 = home_screen.getString("home_screen_native1")
                        val home_screen_native2 = home_screen.getString("home_screen_native2")
                        val home_screen_native1_show =
                            home_screen.optBoolean("home_screen_native1_show", true)
                        val home_screen_native2_show =
                            home_screen.optBoolean("home_screen_native2_show", true)
                        ADSMainClass.setHomeScreenBottomShow(home_screen_bottom_show)
                        ADSMainClass.setHomeScreenAdsType(home_screen_ads_type)
                        ADSMainClass.setHomeScreenNative1Show(home_screen_native1_show)
                        ADSMainClass.setHomeScreenNative2Show(home_screen_native2_show)

                        // Add Alaram Screen
                        val chat_screen = screen.getJSONObject("chat_screen")
                        val chat_screen_bottom_ads_show =
                            chat_screen.getBoolean("chat_screen_bottom_ads_show")
//                        val add_alaram_inter_ads_show = add_alaram_screen.getBoolean("add_alaram_inter_ads_show")
                        val chat_screen_ads_type = chat_screen.getString("chat_screen_ads_type")
                        val chat_screen_banner_id = chat_screen.getString("chat_screen_banner_id")
                        val chat_screen_native_id = chat_screen.getString("chat_screen_native_id")
                        ADSMainClass.setChatSmallAdsShow(chat_screen_bottom_ads_show)
                        ADSMainClass.setChatAdsType(chat_screen_ads_type)

                        // Setting Screen
                        val setting_screen = screen.getJSONObject("setting_screen")
                        val setting_bottom_ads_show =
                            setting_screen.getBoolean("setting_bottom_ads_show")
                        val setting_ads_type = setting_screen.getString("setting_ads_type")
                        val setting_banner_id = setting_screen.getString("setting_banner_id")
                        val setting_native_id = setting_screen.getString("setting_native_id")
                        ADSMainClass.setSettingBottomAdsShow(setting_bottom_ads_show)
                        ADSMainClass.setSettingAdsType(setting_ads_type)


                        val callend_screen = screen.getJSONObject("callend_screen")

                        val is_callend_show = callend_screen.getBoolean("is_callend_show")
                        val app_logo_show = callend_screen.getBoolean("app_logo_show")
                        val is_callend_bottom_ad_show = callend_screen.getBoolean("is_callend_bottom_ad_show")
                        val callend_bottom_ads_type = callend_screen.getString("callend_bottom_ads_type")
                        val callend_banner_ad_id = callend_screen.getString("callend_banner_ad_id")
                        val callend_native_ad_id = callend_screen.getString("callend_native_ad_id")
                        val notification_install_days =  callend_screen.getInt("notification_install_days")
                        val notification_call_install_days =  callend_screen.getInt("notification_call_install_days")
                        val notification_call_overlay_install_days = callend_screen.getInt("notification_call_overlay_install_days")
                        val country_get_with_ip =  callend_screen.getBoolean("country_get_with_ip")

                        ADSMainClass.setIsShowCallEnd(is_callend_show)
                        ADSMainClass.setCallEndBottomAdsShow(is_callend_bottom_ad_show)
                        ADSMainClass.setCallEndBottomAdsType(callend_bottom_ads_type)
                        ADSMainClass.setNotificationInstallDays(notification_install_days)
                        ADSMainClass.setNotificationCallInstallDays(notification_call_install_days)
                        ADSMainClass.setNotificationCallOverlayInstallDays(notification_call_overlay_install_days)
                        val all_allow_notif = callend_screen.optBoolean(ADSMainClass.ALL_ALLOW_PERMISSION_SHOW_FB_NOTIFICATION)
                        ADSMainClass.setAllAllowPermissionShowFbNotification(all_allow_notif)
                        val close_button_show_on_full_native_ads =callend_screen.getBoolean(ADSMainClass.CLOSE_BUTTON_SHOW_ON_FULL_NATIVE_ADS)
                        ADSMainClass.setCloseButtonShowOnFullNativeAds(close_button_show_on_full_native_ads)
                        ADSMainClass.setCountryGetWithIp(country_get_with_ip)
                        ADSMainClass.setCallEndLogoShow(app_logo_show)

                        val notification_country_list = ArrayList<String>()
                        val notification_country_array = callend_screen.getJSONArray("notification_country")
                        for (i in 0 until notification_country_array.length()) {
                            notification_country_list.add(notification_country_array.getString(i))
                        }
                        ADSMainClass.setNotificationCountries(notification_country_list)

                        val notification_call_country_list = ArrayList<String>()
                        val notification_call_country_array = callend_screen.getJSONArray("notification_call_country")
                        for (i in 0 until notification_call_country_array.length()) {
                            notification_call_country_list.add(notification_call_country_array.getString(i))
                        }
                        ADSMainClass.setNotificationCallCountries(notification_call_country_list)

                        val notification_call_overlay_country_list = ArrayList<String>()
                        val notification_call_overlay_country_array = callend_screen.getJSONArray("notification_call_overlay_country")
                        for (i in 0 until notification_call_overlay_country_array.length()) {
                            notification_call_overlay_country_list.add(notification_call_overlay_country_array.getString(i))
                        }
                        ADSMainClass.setNotificationCallOverlayCountries(notification_call_overlay_country_list)


                        val call_end_bacK_inter = callend_screen.getJSONObject("call_end_bacK_inter")
                        val call_end_inter_ads_show = call_end_bacK_inter.getBoolean("call_end_inter_ads_show")
                        val notification_screen_back_ads_show = call_end_bacK_inter.getBoolean("notification_screen_back_ads_show")
                        val call_end_inter_ads_type = call_end_bacK_inter.getString("call_end_inter_ads_type")
                        val call_end_inter_day_count = call_end_bacK_inter.getLong("call_end_inter_day_count")
                        val call_end_inter_active_total_show_count = call_end_bacK_inter.getLong("call_end_inter_active_total_show_count")
                        val callend_inter_ad_id = call_end_bacK_inter.getString("callend_inter_ad_id")
                        val callend_again_open_count = call_end_bacK_inter.getInt("callend_again_open_count")


                        val call_end_inter_ads_show_country = ArrayList<String>()
                        try {
                            val countryArray = call_end_bacK_inter.getJSONArray("call_end_inter_ads_show_country")
                            for (i in 0 until countryArray.length()) {
                                call_end_inter_ads_show_country.add(countryArray.getString(i))
                            }
                        } catch (e: Exception) {
                            // Parameter might be missing
                        }
                        ADSMainClass.setCallEndAdCountries(call_end_inter_ads_show_country)
                        ADSMainClass.setCallEndInterAdsShow(call_end_inter_ads_show)
                        ADSMainClass.setNotificationScreenBackAdsShow(notification_screen_back_ads_show)
                        ADSMainClass.setCallEndInterAdsType(call_end_inter_ads_type)
                        ADSMainClass.setCallEndInterDayCount(Math.toIntExact(call_end_inter_day_count))
                        ADSMainClass.setCallEndInterShowCount(Math.toIntExact(call_end_inter_active_total_show_count))
                        ADSMainClass.setCallEndAgainOpenCount(callend_again_open_count)



                        val other_screen = screen.getJSONObject("other_screen")
                        val inter_first_time = other_screen.getString("inter_first_time")
                        val inter_second_time = other_screen.getString("inter_second_time")
                        val other_native_id = other_screen.getString("other_native_id")
                        val other_banner_id = other_screen.getString("other_banner_id")
                        val inter_ads_load_type = other_screen.getString("inter_ads_load_type")
                        val other_bottom_ads_type = other_screen.getString("other_bottom_ads_type")
                        val other_bottom_ads_show = other_screen.getBoolean("other_bottom_ads_show")
                        val inter_ads_show = other_screen.getBoolean("inter_ads_show")
                        val inter_ads_show_on_back = other_screen.getBoolean("inter_ads_show_on_back")
                        ADSMainClass.setInterAdsShow(inter_ads_show)
                        ADSMainClass.setInterAdsOnBackShow(inter_ads_show_on_back)
                        ADSMainClass.setOtherAdsType(other_bottom_ads_type)
                        ADSMainClass.setOtherAdsShow(other_bottom_ads_show)
                        ADSMainClass.setInterAdsLoadType(inter_ads_load_type)


                        val quick_message_screen = screen.getJSONObject("quick_message_screen")
                        val full_Intent_screen_close_time = quick_message_screen.getInt(ADSMainClass.FULL_INTENT_SCREEN_CLOSE_TIME)
                        val message_callend_show = quick_message_screen.getBoolean(ADSMainClass.MESSAGE_CALLEND_SHOW)
                        val message_callend_ad_show = quick_message_screen.getBoolean(ADSMainClass.MESSAGE_CALLEND_AD_SHOW)
                        val show_full_screen_intent_permission = quick_message_screen.optBoolean(ADSMainClass.SHOW_FULL_SCREEN_INTENT_PERMISSION, true)
                        val quick_ads_type = quick_message_screen.optString(ADSMainClass.QUICK_ADS_TYPE)
                        val quick_banner_id = quick_message_screen.optString(ADSMainClass.QUICK_BANNER_ID)
                        val quick_native_id = quick_message_screen.optString(ADSMainClass.QUICK_NATIVE_ID)

                        val update_app = screen.getJSONObject("update_app")
                        val in_app_update_show = update_app.getBoolean("in_app_update_show")
                        val in_app_update_type = update_app.getString("in_app_update_type")
                        val in_appp_dailog_daily_show_count = update_app.getInt("in_appp_dailog_daily_show_count")

                        ADSMainClass.setInAppUpdateShow(in_app_update_show)
                        ADSMainClass.setInAppUpdateType(in_app_update_type)
                        ADSMainClass.setInApppDialogDailyShowCount(in_appp_dailog_daily_show_count)

                        ADSMainClass.setIntTIMEValue(ADSMainClass.FULL_INTENT_SCREEN_CLOSE_TIME, full_Intent_screen_close_time)
                        ADSMainClass.setMessageCallEndShow(message_callend_show)
                        ADSMainClass.setMessageCallEndADShow(message_callend_ad_show)
                        ADSMainClass.setBooleanValue(ADSMainClass.SHOW_FULL_SCREEN_INTENT_PERMISSION, show_full_screen_intent_permission)
                        ADSMainClass.setQuickAdsType(quick_ads_type)

                        val overlay_notification_show = try { jsonobject.getBoolean("overlay_permission_notification_show") } catch (e: Exception) { false }
                        ADSMainClass.setOverlayPermissionNotificationShow(overlay_notification_show)

                        val overlay_notification_days = try { jsonobject.getInt("overlay_permission_notification_days_show_count") } catch (e: Exception) { 0 }
                        ADSMainClass.setOverlayPermissionNotificationDaysShowCount(overlay_notification_days)


//
//                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE, language_native_id)
//                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER, language_banner_id)
//
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE, home_native_id)
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_BANNER, home_banner_id)
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE1, home_screen_native1)
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE2, home_screen_native2)
//                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_NATIVE, chat_screen_native_id)
//                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_BANNER, chat_screen_banner_id)
//
//                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_NATIVE, setting_native_id)
//                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_BANNER, setting_banner_id)
//
//                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Native, callend_native_ad_id)
//                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_BANNER, callend_banner_ad_id)
//                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Inter, callend_inter_ad_id)
//
//                        ADSMainClass.setStringValue(ADSMainClass.QUICK_BANNER_ID, quick_banner_id)
//                        ADSMainClass.setStringValue(ADSMainClass.QUICK_NATIVE_ID, quick_native_id)
//
//                        ADSMainClass.setStringValue(ADSMainClass.APP_OPEN_ID, app_open_id)
//                        ADSMainClass.setStringValue(ADSMainClass.INTER_FIRST_TIME, inter_first_time)
//                        ADSMainClass.setStringValue(ADSMainClass.INTER_SECOND_TIME, inter_second_time)
//                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_NATIVE, other_native_id)
//                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_BANNER, other_banner_id)
//
//                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_NATIVE, onboarding_native_id)
//                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_BANNER, onboarding_banner_id)


                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")

                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE1, "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE2, "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")

                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")

                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")

                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Native, "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_BANNER, "ca-app-pub-3940256099942544/9214589741")
                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Inter, "ca-app-pub-3940256099942544/1033173712")

                        ADSMainClass.setStringValue(ADSMainClass.QUICK_BANNER_ID, "ca-app-pub-3940256099942544/9214589741")
                        ADSMainClass.setStringValue(ADSMainClass.QUICK_NATIVE_ID, "ca-app-pub-3940256099942544/2247696110")

                        ADSMainClass.setStringValue(ADSMainClass.APP_OPEN_ID, "ca-app-pub-3940256099942544/9257395921")
                        ADSMainClass.setStringValue(ADSMainClass.INTER_FIRST_TIME, "ca-app-pub-3940256099942544/1033173712")
                        ADSMainClass.setStringValue(ADSMainClass.INTER_SECOND_TIME, "ca-app-pub-3940256099942544/1033173712")
                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_NATIVE,  "ca-app-pub-3940256099942544/2247696110")
                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_BANNER,  "ca-app-pub-3940256099942544/9214589741")
                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")


                        ADSMainClass.setExitAds(exit_ads)
                        ADSMainClass.setComingSoon(coming_soon)
                        ADSMainClass.setPrivacyPolicy(privacy_policy)
                        FirebaseAnalytics.getInstance(this@BActivity)
                            .setAnalyticsCollectionEnabled(firebaseanalytics)
                        ADSMainClass.setSplashADType(splash_ads_type)
                        ADSMainClass.setAds_Free(ads_blocker)
                        ADSMainClass.setAdsTypeManage(ads_type_load)
                        ADSMainClass.setNativeButtonColor(admob_native_btn_color)
                        ADSMainClass.setNativeButtonTextColor(admob_native_btn_text_color)
                        ADSMainClass.setAdsClick(Math.toIntExact(ads_click))
                        ADSMainClass.setAdsBackClick(Math.toIntExact(back_click))
                        ADSMainClass.setNativeByPage(Math.toIntExact(native_by_page))
                        ADSMainClass.setConsentScreenShow(consent_form_show_screen)


                        if (update_coming_soon) {
                            ADSMainClass.setComingSoon(true)
                            startActivity(
                                Intent(
                                    this@BActivity,
                                    CommingActivity::class.java
                                )
                            )
                            finish()
                            return@OnCompleteListener
                        }
                        if (ADSMainClass.getComingSoon()) {
                            continueAfterConsent { app_next_screen_showing() }
                        } else {
                            continueAfterConsent { app_open_showing() }
                        }
                    } catch (e: java.lang.Exception) {
//                        Log.d("TAG", "showing_to_all_data: " + e.message.toString())
                        e.printStackTrace()
                        continueAfterConsent { app_next_screen_showing() }
                    }
                } else {
                    continueAfterConsent { app_next_screen_showing() }
                }
            })
    }

    private fun continueAfterConsent(action: () -> Unit) {
        if (isFinishing || isDestroyed) return

        val showFormOnSplash = ADSMainClass.isConsentOnSplash()
//        Log.d(
//            "ADSConsentManager",
//            "Splash consent screen=${ADSMainClass.getConsentScreenShow()}, showForm=$showFormOnSplash"
//        )

        ADSConsentManager.gatherConsent(this, showFormOnSplash) { formShown ->
            if (isFinishing || isDestroyed) return@gatherConsent

            if (showFormOnSplash) {
                if (formShown) {
                    ADSMainClass.setConsentPendingOnLanguage(false)
                } else if (ADSConsentManager.shouldRetryConsentOnLanguage(this)) {
                    ADSMainClass.setConsentPendingOnLanguage(true)
//                    Log.d(
//                        "ADSConsentManager",
//                        "Consent not shown on splash — will retry on language screen"
//                    )
                }
            }

            proceedAfterConsentGathered(action)
        }
    }

    private fun proceedAfterConsentGathered(action: () -> Unit) {
        if (ADSConsentManager.canRequestAds(this) && !ADSMainClass.getAds_Free()) {
            ADSAppManage.initializeMobileAdsIfNeeded(applicationContext) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        action()
                    }
                }
            }
        } else {
            runOnUiThread {
                if (!isFinishing && !isDestroyed) {
                    action()
                }
            }
        }
    }

    private fun app_open_showing() {

        if (!ADSConsentManager.canRequestAds(this)) {
            app_next_screen_showing()
            return
        }

        if (!ADSMainClass.shouldShowSplashAd()) {
            app_next_screen_showing()
            return
        }

        if (ADSMainClass.getAds_Free()) {
            app_next_screen_showing()
            return
        }

        if (ADSMainClass.getSplashADType() == "Appopen") {
            ADSAppStarting.AdsSplashAppStartingDisplay(this@BActivity, {
                ADSAppManage.FastStart = true
                app_next_screen_showing()
            }
            )

        } else {
            ADSAppManage.FastStart = true
            app_next_screen_showing()
        }
    }

    private fun app_next_screen_showing() {
        ADSAppManage.AppStartingScreenOpen = true
        initActivity()
    }
}
