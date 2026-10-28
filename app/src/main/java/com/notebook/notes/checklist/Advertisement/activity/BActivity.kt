package com.notebook.notes.checklist.Advertisement.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope


import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.notebook.notes.checklist.Advertisement.ADSAppManage
import com.notebook.notes.checklist.Advertisement.ADSAppStarting
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.Advertisement.ManegeUtilsView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.AESHelper
import com.notebook.notes.checklist.BuildConfig
import android.os.CountDownTimer
import com.ads.module.ads.NotesAd
import com.ads.module.billing.AppPurchase
import com.ads.module.funtion.AdCallback
import com.notebook.notes.checklist.ads.inter_splash
import com.notebook.notes.checklist.ads.inter_splash_uninstall
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AppConstants
import com.notebook.notes.checklist.ads.AppLinksConfigApplier
import com.notebook.notes.checklist.ads.CallEndConfigApplier
import com.notebook.notes.checklist.ads.RemoteConfigUtils
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import com.notebook.notes.checklist.flow.RemoteConfigEntryPoint
import com.notebook.notes.checklist.flow.RemoteConfigManager
import com.notebook.notes.checklist.iap.PremiumManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

open class BActivity : AppCompatActivity() {

    /** Called after ads remote-config / consent / splash app-open finish. */
    open fun initActivity() {}

    /** True when launched from the long-press uninstall shortcut (see UninstallShortcutManager). */
    protected val isFromUninstallShortcut: Boolean
        get() = intent?.getStringExtra(AppConstants.FROM_SHORTCUT) == AppConstants.ACTION_OPEN_UNINSTALL

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
        // Safety net: if the splash interstitial was dismissed or failed while we were away, move on.
        NotesAd.getInstance().onCheckShowSplashWhenFail(this, object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                app_next_screen_showing()
            }
        }, 1000)
    }

    private var getConfigSuccess = false
    private val remoteConfigHandled = AtomicBoolean(false)

    /** Splash-time hook: preload any ads that later screens will show. Runs after [AdRemoteConfig] is loaded. */
    protected open fun preloadAds() {}

    /**
     * Splash remote config, same flow as the AdLogic template: [RemoteConfigUtils] sets defaults and
     * fetches once, consent runs, then a countdown waits for the fetch (or the hard cap) before
     * [checkRemoteConfigResult] loads [AdRemoteConfig] and preloads ads.
     */
    private fun showing_to_all_data() {
        RemoteConfigUtils.init(
            this,
            object : RemoteConfigUtils.Listener {
                override fun loadSuccess() {
                    getConfigSuccess = true
                }
            },
            flowRemoteConfigManager().buildMergedDefaultsMap()
        )
        continueAfterConsent { loadingRemoteConfig() }
    }

    private fun loadingRemoteConfig() {
        object : CountDownTimer(AppConstants.DEFAULT_TIME_SPLASH, 100) {
            override fun onTick(millisUntilFinished: Long) {
                if (getConfigSuccess && millisUntilFinished < AppConstants.DEFAULT_LIMIT_TIME_SPLASH) {
                    checkRemoteConfigResult()
                    cancel()
                }
            }

            override fun onFinish() {
                if (!getConfigSuccess) {
                    checkRemoteConfigResult()
                }
            }
        }.start()
    }

    private fun checkRemoteConfigResult() {
        if (!remoteConfigHandled.compareAndSet(false, true) || isFinishing || isDestroyed) return
        val adsJson = RemoteConfigUtils.getAdRemoteConfig()
        AdRemoteConfig.initialize(this, adsJson)
        CallEndConfigApplier.apply(this, adsJson)
        AppLinksConfigApplier.apply(this, adsJson)
        // Same master switch for the legacy ads, which gate on getAds_Free() rather than AdUnitConfig.
        ADSMainClass.initAppContext(this)
        ADSMainClass.setRemoteAdsBlocker(!AdRemoteConfig.getInstance().adsEnabled)
        try {
            // Flow keys (e.g. onboarding_repeat_count) may sit inside the ads JSON; this also falls
            // back to separate Remote Config parameters and then the XML defaults per key.
            flowRemoteConfigManager().cacheFromAdsJson(JSONObject(adsJson))
        } catch (e: Exception) {
            flowRemoteConfigManager().cacheXmlDefaultsOnly()
        }
        preloadAds()

        val splashInter = if (isFromUninstallShortcut) AdRemoteConfig.inter_splash_uninstall else AdRemoteConfig.inter_splash
        if (splashInter.isEnable && ADSConsentManager.canRequestAds(this) && ADSUtilitis.IsNetworkConnected(this) &&
            !AppPurchase.getInstance().isPurchased(this)
        ) {
            NotesAd.getInstance().loadSplashInterstitialAds(
                this, splashInter.id, 30000, 5000, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        app_next_screen_showing()
                    }
                })
        } else {
            app_next_screen_showing()
        }
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

    private fun app_next_screen_showing() {
        ADSAppManage.AppStartingScreenOpen = true
        initActivity()
    }
}
