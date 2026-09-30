package com.notebook.notes.checklist.Advertisement.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope


import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.messaging.FirebaseMessaging
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
import com.notebook.notes.checklist.BuildConfig
import com.notebook.notes.checklist.flow.RemoteConfigEntryPoint
import com.notebook.notes.checklist.flow.RemoteConfigManager
import com.notebook.notes.checklist.iap.PremiumManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
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
        setContentView(R.layout.activity_splash)

        // Fetch FCM Token immediately on startup
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM_TOKEN", "Startup FCM Token: $token")
                } else {
                    Log.e(
                        "FCM_TOKEN",
                        "Startup FCM Token fetch failed",
                        task.exception
                    )
                }
            }
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
            // Offline: flow keys from XML defaults; never block startup.
            try {
                flowRemoteConfigManager().cacheXmlDefaultsOnly()
            } catch (e: Exception) {

            }
            Handler(Looper.getMainLooper()).postDelayed({
                continueAfterConsent { app_next_screen_showing() }
            }, 500)
            return
        }
        iv_firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()
        iv_firebaseRemoteConfig!!.setConfigSettingsAsync(configSettings)

        // Ads defaults + flow defaults (screen_flow, onboarding_count, …) in one setDefaults.
        iv_firebaseRemoteConfig!!.setDefaultsAsync(flowRemoteConfigManager().buildMergedDefaultsMap())

        iv_firebaseRemoteConfig!!.fetchAndActivate()
            .addOnCompleteListener(this, OnCompleteListener<Boolean?> { task ->
                if (task.isSuccessful) {
                    try {

                        val jsonobject: JSONObject =
                            JSONArray(iv_firebaseRemoteConfig!!.getString(version)).getJSONObject(0)

                        // ContactManager key → notes screen_flow (RemoteConfigManager).
                        val introFlow = jsonobject.optJSONArray("show_intro_screen_flow")
                        if (introFlow != null && jsonobject.opt("screen_flow") == null) {
                            jsonobject.put("screen_flow", introFlow)
                        }

                        // App-flow keys are nested inside this same JSON — cache them here.
                        cacheAppFlowFromAdsJson(jsonobject)


                        val ads_type_load = jsonobject.getString("Ads_Loading_Types")
                        val privacy_policy = jsonobject.getString("App_PrivacyPolicy")
                        val terms_of_use = jsonobject.optString("App_TermsOfUse")
                            .ifBlank { jsonobject.optString("TermsConditions") }
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

                        // ads_layout (light / dark native colors + button margin)
                        val adsLayout = screen.getJSONObject("ads_layout")
                        val lightMode = adsLayout.getJSONObject("light_mode")
                        val button_margin = adsLayout.getString("button_margin")
                        val lightNativeButtonColor =
                            lightMode.getString("Native_Ads_Button_Color")
                        val lightNativeBackgroundColor =
                            lightMode.getString("Native_Ad_Background_Color")
                        val lightNativeAllTextColor =
                            lightMode.getString("Native_Ad_All_Text_Color")
                        val lightNativeButtonTextColor =
                            lightMode.getString("Native_Ad_Button_Text_Color")
                        val darkMode = adsLayout.getJSONObject("dark_mode")
                        val darkNativeButtonColor =
                            darkMode.getString("Native_Ads_Button_Color")
                        val darkNativeBackgroundColor =
                            darkMode.getString("Native_Ad_Background_Color")
                        val darkNativeAllTextColor =
                            darkMode.getString("Native_Ad_All_Text_Color")
                        val darkNativeButtonTextColor =
                            darkMode.getString("Native_Ad_Button_Text_Color")
                        ADSMainClass.setButtonMargins(button_margin)
                        ADSMainClass.setLightNativeButtonColor(lightNativeButtonColor)
                        ADSMainClass.setLightNativeBackgroundColor(lightNativeBackgroundColor)
                        ADSMainClass.setLightNativeAllTextColor(lightNativeAllTextColor)
                        ADSMainClass.setLightNativeButtonTextColor(lightNativeButtonTextColor)
                        ADSMainClass.setDarkNativeButtonColor(darkNativeButtonColor)
                        ADSMainClass.setDarkNativeBackgroundColor(darkNativeBackgroundColor)
                        ADSMainClass.setDarkNativeAllTextColor(darkNativeAllTextColor)
                        ADSMainClass.setDarkNativeButtonTextColor(darkNativeButtonTextColor)

                        // Splash Screen
                        val splash_screen = screen.getJSONObject("splash_screen")
                        val splah_app_open_ads_show = splash_screen.getBoolean("splash_app_open_ads_show")
                        val splash_app_open_after_count = splash_screen.getInt("splash_app_open_after_count")
                        val app_open_show_in_background = splash_screen.getBoolean("app_open_show_in_background")
                        val app_open_daily_show_count = splash_screen.getInt("app_open_daily_show_count")
                        val app_open_id = splash_screen.getString("app_open_id")
                        val splash_banner_id = splash_screen.getString("splash_banner_id")
                        val splash_native_id = splash_screen.getString("splash_native_id")
                        val splash_ads_types = splash_screen.getString("splash_ads_type")
                        val splash_screen_bottom_ad_show =
                            splash_screen.getBoolean("splash_screen_bottom_ad_show")
                        ADSMainClass.setSplashAdsType(splash_ads_types)
                        ADSMainClass.setSplashAppOpenShow(splah_app_open_ads_show)
                        ADSMainClass.setSplashAppOpenAfterCount(splash_app_open_after_count)
                        ADSMainClass.setAppOpenBackgroundShow(app_open_show_in_background)
                        ADSMainClass.setAppOpenAdDailyLimit(app_open_daily_show_count)
                        ADSMainClass.setBottomSplashShow(splash_screen_bottom_ad_show)

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
                        val language_inter_id = language_screen.getString("language_inter_id")

                        ADSMainClass.setSplashToLanguage(splash_to_language)
                        ADSMainClass.setLanguageScreenBottomAdShow(language_screen_bottom_ad_show)
                        ADSMainClass.setLanguageAdsType(language_ads_type)
                        ADSMainClass.setLanguageInterAdsShow(language_inter_ads_show)


                        // onboarding Screen


                        val onboarding_screen = screen.getJSONObject("onboarding_screen")

                        val onboarding_screen_bottom_ad_show =
                            onboarding_screen.getBoolean("onboarding_bottom_ad_show")
                        val onboarding_ads_type = onboarding_screen.getString("onboarding_ads_type")
                        val onboarding_inter_ads_show =
                            onboarding_screen.getBoolean("onboarding_inter_ads_show")
                        val onboarding_banner_id = onboarding_screen.getString("onboarding_banner_id")
                        val onboarding_native_id = onboarding_screen.getString("onboarding_native_id")
                        val onboarding_inter_id =
                            onboarding_screen.optString("onboarding_inter_id", "")

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
                        val home_ads_click = home_screen.optInt("home_bottom_count", 0)
                        val home_inter_ads_show =
                            home_screen.optBoolean("home_inter_ads_show", true)
                        val home_inter_ad_id =
                            home_screen.optString("home_inter_ad_id", "")
                        val home_bottom_nav_inter_show =
                            home_screen.optBoolean("home_bottom_nav_inter_show", false)
                        ADSMainClass.setHomeScreenBottomShow(home_screen_bottom_show)
                        ADSMainClass.setHomeScreenAdsType(home_screen_ads_type)
                        ADSMainClass.setHomeScreenNative1Show(home_screen_native1_show)
                        ADSMainClass.setHomeScreenNative2Show(home_screen_native2_show)
                        ADSMainClass.setHomeAdsClick(home_ads_click)
                        ADSMainClass.setHomeInterAdsShow(home_inter_ads_show)


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
                        ADSMainClass.setHomeBottomNavInterShow(home_bottom_nav_inter_show)


                        // Setting Screen
                        val setting_screen = screen.getJSONObject("setting_screen")
                        val setting_bottom_ads_show =
                            setting_screen.getBoolean("setting_bottom_ads_show")
                        val setting_ads_type = setting_screen.getString("setting_ads_type")
                        val setting_banner_id = setting_screen.getString("setting_banner_id")
                        val setting_native_id = setting_screen.getString("setting_native_id")
                        val setting_inter_ads_show = setting_screen.getBoolean("setting_inter_ads_show")
                        ADSMainClass.setSettingBottomAdsShow(setting_bottom_ads_show)
                        ADSMainClass.setSettingAdsType(setting_ads_type)
                        ADSMainClass.setSettingInterShow(setting_inter_ads_show)


                        val callend_screen = screen.getJSONObject("callend_screen")

                        val is_callend_show = callend_screen.getBoolean("is_call_end_show")
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
                        val splash_inter_ads_load_type =
                            jsonobject.optString("inter_ads_load_type", "PreLoad")
                        val app_inter_ads_load_type =
                            other_screen.optString("inter_ads_load_type", "PreLoad")
                        val native_ads_load_type =
                            jsonobject.optString("native_ads_load_type", "PreLoad")
                        val banner_ads_load_type =
                            jsonobject.optString("banner_ads_load_type", "PreLoad")
                        val appopen_ads_load_type =
                            jsonobject.optString("appopen_ads_load_type", "PreLoad")
                        val other_bottom_ads_type = other_screen.getString("other_bottom_ads_type")
                        val other_bottom_ads_show = other_screen.getBoolean("other_bottom_ads_show")
                        val inter_ads_show = other_screen.getBoolean("inter_ads_show")
                        val inter_ads_show_on_back = other_screen.getBoolean("inter_ads_show_on_back")
                        val rewarded_ad_id = other_screen.optString("rewarded_ad_id", "")
                        val rewarded_ads_show = other_screen.optBoolean("rewarded_ads_show", true)
                        val rewarded_ads_load_type = other_screen.optString("rewarded_ads_load_type", "preLoad")

                        ADSMainClass.setInterAdsShow(inter_ads_show)
                        ADSMainClass.setInterAdsOnBackShow(inter_ads_show_on_back)
                        ADSMainClass.setOtherAdsType(other_bottom_ads_type)
                        ADSMainClass.setOtherAdsShow(other_bottom_ads_show)
                        ADSMainClass.setSplashInterAdsLoadType(splash_inter_ads_load_type)
                        ADSMainClass.setInterAdsLoadType(app_inter_ads_load_type)
                        ADSMainClass.setNativeAdsLoadType(native_ads_load_type)
                        ADSMainClass.setBannerAdsLoadType(banner_ads_load_type)
                        ADSMainClass.setAppOpenAdsLoadType(appopen_ads_load_type)
                        ADSMainClass.setRewardedAdsShow(rewarded_ads_show)
                        ADSMainClass.setRewardedAdsLoadType(rewarded_ads_load_type)
                        // Create Notes Screen
                        val create_notes_screen = screen.optJSONObject("create_notes_screen")
                        val create_notes_screen_bottom_show =
                            create_notes_screen?.optBoolean("create_notes_screen_bottom_show", true) ?: true
                        val create_notes_screen_ads_type =
                            create_notes_screen?.optString("create_notes_screen_ads_type", "native") ?: "native"
                        val create_notes_screen_native1_show =
                            create_notes_screen?.optBoolean("create_notes_screen_native1_show", false) ?: false
                        val create_notes_screen_native2_show =
                            create_notes_screen?.optBoolean("create_notes_screen_native2_show", false) ?: false
                        val create_notes_screen_native1 =
                            create_notes_screen?.optString("create_notes_screen_native1", "") ?: ""
                        val create_notes_screen_native2 =
                            create_notes_screen?.optString("create_notes_screen_native2", "") ?: ""
                        val create_notes_banner_id =
                            create_notes_screen?.optString("create_notes_banner_id", "") ?: ""
                        val create_notes_native_id =
                            create_notes_screen?.optString("create_notes_native_id", "") ?: ""
                        val create_notes_inter_id =
                            create_notes_screen?.optString("create_notes_inter_id", "") ?: ""
                        val create_notes_ads_click =
                            create_notes_screen?.optInt("CREATE_NOTES_ADSClick", 0) ?: 0
                        val create_notes_inter_ads_show =
                            create_notes_screen?.optBoolean("create_notes_inter_ads_show", false) ?: false
                        ADSMainClass.setCreateNotesScreenBottomShow(create_notes_screen_bottom_show)
                        ADSMainClass.setCreateNotesScreenAdsType(create_notes_screen_ads_type)
                        ADSMainClass.setCreateNotesScreenNative1Show(create_notes_screen_native1_show)
                        ADSMainClass.setCreateNotesScreenNative2Show(create_notes_screen_native2_show)
                        ADSMainClass.setCreateNotesAdsClick(create_notes_ads_click)
                        ADSMainClass.setCreateNotesInterAdsShow(create_notes_inter_ads_show)

                        // Set Password Screen
                        val set_password_screen = screen.optJSONObject("set_password_screen")
                        val set_password_inter_ads_show =
                            set_password_screen?.optBoolean("set_password_inter_ads_show", false) ?: false
                        ADSMainClass.setSetPasswordInterAdsShow(set_password_inter_ads_show)

                        // Drawing Screen
                        val drawing_screen = screen.optJSONObject("drawing_screen")
                        val drawing_inter_ads_show =
                            drawing_screen?.optBoolean("drawing_inter_ads_show", false) ?: false
                        ADSMainClass.setDrawingInterAdsShow(drawing_inter_ads_show)

                        // Image Viewer Screen
                        val image_viewer_screen = screen.optJSONObject("image_viewer_screen")
                        val image_viewer_inter_ads_show = image_viewer_screen?.optBoolean("image_viewer_inter_ads_show", false) ?: false
                        ADSMainClass.setImageViewerInterAdsShow(image_viewer_inter_ads_show)

                        // Video Player Screen
                        val video_player_screen = screen.optJSONObject("video_player_screen")
                        val video_player_inter_ads_show =
                            video_player_screen?.optBoolean("video_player_inter_ads_show", false) ?: false
                        ADSMainClass.setVideoPlayerInterAdsShow(video_player_inter_ads_show)

                        // Recycle Bin Screen
                        val recycle_bin_screen = screen.optJSONObject("recycle_bin_screen")
                        val recycle_bin_inter_ads_show =
                            recycle_bin_screen?.optBoolean("recycle_bin_inter_ads_show", false) ?: false
                        ADSMainClass.setRecycleBinInterAdsShow(recycle_bin_inter_ads_show)
                        // Edit Label Screen
                        val edit_label_screen = screen.optJSONObject("edit_label_screen")
                        val edit_label_inter_ads_show =
                            edit_label_screen?.optBoolean("edit_label_inter_ads_show", false) ?: false
                        ADSMainClass.setEditLabelInterAdsShow(edit_label_inter_ads_show)

                        // Note Background Picker Screen
                        val note_background_picker_screen =
                            screen.optJSONObject("note_background_picker_screen")
                        val note_background_picker_inter_ads_show =
                            note_background_picker_screen?.optBoolean(
                                "note_background_picker_inter_ads_show",
                                false
                            ) ?: false
                        ADSMainClass.setNoteBackgroundPickerInterAdsShow(
                            note_background_picker_inter_ads_show
                        )


                        val update_app = screen.getJSONObject("update_app")
                        val in_app_update_show = update_app.getBoolean("in_app_update_show")
                        val in_app_update_type = update_app.getString("in_app_update_type")
                        val in_appp_dailog_daily_show_count = update_app.getInt("in_appp_dailog_daily_show_count")

                        ADSMainClass.setInAppUpdateShow(in_app_update_show)
                        ADSMainClass.setInAppUpdateType(in_app_update_type)
                        ADSMainClass.setInApppDialogDailyShowCount(in_appp_dailog_daily_show_count)



                        val overlay_notification_show = try { jsonobject.getBoolean("overlay_permission_notification_show") } catch (e: Exception) { false }
                        ADSMainClass.setOverlayPermissionNotificationShow(overlay_notification_show)

                        val overlay_notification_days = try { jsonobject.getInt("overlay_permission_notification_days_show_count") } catch (e: Exception) { 0 }
                        ADSMainClass.setOverlayPermissionNotificationDaysShowCount(overlay_notification_days)

//
                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE, language_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER, language_banner_id)
                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_INTER, language_inter_id)

                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE, home_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_BANNER, home_banner_id)
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE1, home_screen_native1)
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE2, home_screen_native2)
                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_INTER, home_inter_ad_id)
                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_NATIVE, chat_screen_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_BANNER, chat_screen_banner_id)

                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_NATIVE, setting_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_BANNER, setting_banner_id)

                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Native, callend_native_ad_id)
                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_BANNER, callend_banner_ad_id)
                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Inter, callend_inter_ad_id)


                        // Use Google's test unit in debug; release builds use Remote Config.
                        ADSMainClass.setStringValue(
                            ADSMainClass.APP_OPEN_ID,
                            app_open_id
                        )
                        ADSMainClass.setStringValue(ADSMainClass.splash_banner_id, splash_banner_id)
                        ADSMainClass.setStringValue(ADSMainClass.splash_native_id, splash_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.INTER_FIRST_TIME, inter_first_time)
                        ADSMainClass.setStringValue(ADSMainClass.INTER_SECOND_TIME, inter_second_time)
                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_NATIVE, other_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_BANNER, other_banner_id)
                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_NATIVE, create_notes_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_BANNER, create_notes_banner_id)
                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_NATIVE1, create_notes_screen_native1)
                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_NATIVE2, create_notes_screen_native2)
                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_INTER, create_notes_inter_id)
//                        ADSMainClass.setStringValue(ADSMainClass.REWARDED_AD_ID, rewarded_ad_id)
                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_NATIVE, onboarding_native_id)
                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_BANNER, onboarding_banner_id)


//                        ADSMainClass.setStringValue(ADSMainClass.splash_banner_id, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.splash_native_id, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.LANGUAGE_SCREEN_INTER, "ca-app-pub-3940256099942544/1033173712")
//
//
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE1, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE2, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.HOME_SCREEN_INTER, "ca-app-pub-3940256099942544/1033173712")
//
//                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.CHAT_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
//
//                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.SETTING_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
//
//                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Native, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_BANNER, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.CALL_END_Inter, "ca-app-pub-3940256099942544/1033173712")
//
//                        ADSMainClass.setStringValue(ADSMainClass.QUICK_BANNER_ID, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.QUICK_NATIVE_ID, "ca-app-pub-3940256099942544/2247696110")
//
//                        ADSMainClass.setStringValue(ADSMainClass.INTER_FIRST_TIME, "ca-app-pub-3940256099942544/1033173712")
//                        ADSMainClass.setStringValue(ADSMainClass.INTER_SECOND_TIME, "ca-app-pub-3940256099942544/1033173712")
//                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_NATIVE,  "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.OTHER_SCREEN_BANNER,  "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_NATIVE1, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_NATIVE2, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.CREATE_NOTES_SCREEN_INTER, "ca-app-pub-3940256099942544/1033173712")
//                        ADSMainClass.setStringValue(ADSMainClass.REWARDED_AD_ID, "ca-app-pub-3940256099942544/5224354917")
//                        ADSMainClass.setStringValue(ADSMainClass.PERMISSION_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.PERMISSION_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setPermissionSmallAdsShow(true)
//                        ADSMainClass.setPermissionAdsType("native")
//                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_BANNER, "ca-app-pub-3940256099942544/9214589741")
//                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_NATIVE, "ca-app-pub-3940256099942544/2247696110")
//                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_INTER, "ca-app-pub-3940256099942544/1033173712")
//                        ADSMainClass.setStringValue(ADSMainClass.ONBOARDING_SCREEN_INTER, onboarding_inter_id)


                        ADSMainClass.setExitAds(exit_ads)
                        ADSMainClass.setComingSoon(coming_soon)
                        ADSMainClass.setPrivacyPolicy(privacy_policy)
                        ADSMainClass.setTermsOfUse(terms_of_use)
                        Log.e("Milind", "showing_to_all_data: ${terms_of_use}", )
                        FirebaseAnalytics.getInstance(this@BActivity)
                            .setAnalyticsCollectionEnabled(firebaseanalytics)
                        ADSMainClass.setSplashADType(splash_ads_type)
                        // Stores the RC input and recomputes the derived ads-free flag, so a premium
                        // user's ads-free state survives every Remote Config fetch.
                        ADSMainClass.setRemoteAdsBlocker(ads_blocker)
                        ADSMainClass.setAdsTypeManage(ads_type_load)

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
                        e.printStackTrace()
                        // Ads JSON parse failed; try separate RC keys, else XML defaults.
                        cacheAppFlowFromAdsJson(null)
                        continueAfterConsent { app_next_screen_showing() }
                    }
                } else {
                    // Fetch failed; use separate RC keys if present, else XML defaults.
                    cacheAppFlowFromAdsJson(null)
                    continueAfterConsent { app_next_screen_showing() }
                }
            })
    }

    /**
     * Resolves [RemoteConfigManager] without requiring @AndroidEntryPoint on this base Activity.
     */
    private fun flowRemoteConfigManager(): RemoteConfigManager {
        return EntryPointAccessors.fromApplication(
            applicationContext,
            RemoteConfigEntryPoint::class.java
        ).remoteConfigManager()
    }

    private fun premiumManager(): PremiumManager {
        return EntryPointAccessors.fromApplication(
            applicationContext,
            RemoteConfigEntryPoint::class.java
        ).premiumManager()
    }

    /**
     * Stores screen_flow / splash_permission_mode / permission_screen_permissions /
     * onboarding_count / show_paywall_after_onboarding into [RemoteConfigManager].
     *
     * [adsJson] is the parsed versioned Remote Config JSON (flow keys are nested inside it).
     * Pass null when the JSON is unavailable to fall back to separate RC keys / XML.
     */
    private fun cacheAppFlowFromAdsJson(adsJson: JSONObject?) {
        try {
            flowRemoteConfigManager().cacheFromAdsJson(adsJson, iv_firebaseRemoteConfig)
        } catch (e: Exception) {
            try {
                flowRemoteConfigManager().cacheXmlDefaultsOnly()
            } catch (_: Exception) {
                // Never block ads / splash continuation.
            }
        }
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

            awaitPremiumThen { proceedAfterConsentGathered(action) }
        }
    }

    /**
     * Holds the ads bootstrap until Splash has run the same restore path as PaywallActivity.
     *
     * Boolean [PremiumManager.isPurchaseRestoreChecked] is false on a fresh install / reinstall.
     * Until it flips, MobileAds init and the splash app-open ad must not treat the user as free.
     */
    private fun awaitPremiumThen(action: () -> Unit) {
        // No network means no restore is possible, so waiting would only stall the splash.
        if (!ADSUtilitis.IsNetworkConnected(this)) {
            action()
            return
        }
        lifecycleScope.launch {
            val manager = premiumManager()
            // Wait for Application configure + CustomerInfo fetch readiness.
            manager.awaitInitialSync()
            if (isFinishing || isDestroyed) return@launch

            // Same condition as PaywallActivity.restorePurchase() — once per install.
            val isPremium = manager.restorePurchasesForSplash()
            if (isFinishing || isDestroyed) return@launch

            ADSMainClass.applyAdsFree()
            Log.d(
                "Milind",
                "Splash restore gate: premium=$isPremium " +
                    "restoreChecked=${manager.isPurchaseRestoreChecked()} " +
                    "adsFree=${ADSMainClass.getAds_Free()}",
            )
            action()
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

    /** Premium is already resolved here — [awaitPremiumThen] gates every caller. */
    private fun app_open_showing() {

        if (!ADSConsentManager.canRequestAds(this)) {
            app_next_screen_showing()
            return
        }
        if (!ADSMainClass.shouldShowSplashAd()) {
            app_next_screen_showing()
            return
        }

        if (PremiumManager.isPremiumUser() || ADSMainClass.getAds_Free()) {
            app_next_screen_showing()
            return
        }
        if (!ADSUtilitis.IsNetworkConnected(this)) {
            app_next_screen_showing()
            return
        }

        if (ADSMainClass.getSplashADType().equals("Appopen", ignoreCase = true)) {
            ADSAppStarting.AdsSplashAppStartingDisplay(this@BActivity, {
                ADSAppManage.FastStart = true
                app_next_screen_showing()
            })
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
