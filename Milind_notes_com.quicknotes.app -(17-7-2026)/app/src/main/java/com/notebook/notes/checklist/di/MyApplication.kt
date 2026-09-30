package com.notebook.notes.checklist.di

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.facebook.ads.AdSettings
import com.notebook.notes.checklist.Advertisement.ADSAppManage
import com.notebook.notes.checklist.Advertisement.PremiumAdsSynchronizer
import com.notebook.notes.checklist.BuildConfig
import com.notebook.notes.checklist.flow.SessionManager
import com.notebook.notes.checklist.iap.BillingConstants
import com.notebook.notes.checklist.iap.PremiumManager
import com.notebook.notes.checklist.reminder.ReminderNotificationHelper
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.ui.richtext.RichTextFormats
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.util.LocaleHelper
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : ADSAppManage() {

    @Inject lateinit var reminderNotificationHelper: ReminderNotificationHelper
    @Inject lateinit var noteRepository: NoteRepository
    @Inject lateinit var premiumManager: PremiumManager
    @Inject lateinit var premiumAdsSynchronizer: PremiumAdsSynchronizer
    @Inject lateinit var sessionManager: SessionManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {


        @JvmField
        var _isAppInForeground: Boolean = false


        fun isAppInForeground(): Boolean {
            return _isAppInForeground
        }
    }


    override fun onCreate() {
        super.onCreate()
        // Force light mode for this app only; system dark mode is ignored.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        LocaleHelper.applySavedLanguage(this)
        DateTimeUtils.init(this)
        RichTextFormats.init(this)
        reminderNotificationHelper.ensureChannel()

        // Order matters: configure the SDK, then start the entitlement pipeline, then let ads
        // subscribe to it. The entitlement sync begins here — the earliest possible moment in the
        // process — so it is normally finished before the splash screen needs the answer.
        configureRevenueCat()

        Handler(Looper.getMainLooper()).postDelayed({
            premiumManager.start()
        }, 2000) // 1 second delay


        premiumAdsSynchronizer.start()

        appScope.launch(Dispatchers.IO) {
            noteRepository.purgeExpiredDeletedNotes()
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(processObserver)

        //facebook
        if (com.facebook.ads.BuildConfig.DEBUG) {
            AdSettings.setTestMode(false)
        }
    }


    /**
     * Kept as a separate observer rather than implementing [DefaultLifecycleObserver] on the
     * Application: its `onCreate` would collide with [Application.onCreate].
     */
    private val processObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            _isAppInForeground = true
            sessionManager.onAppForegrounded()
            // One entitlement refresh per foreground, owned by the process lifecycle rather than by
            // a screen. Catches renewals, cancellations and expiries that happened in background.
            premiumManager.onAppForegrounded()
        }

        override fun onStop(owner: LifecycleOwner) {
            _isAppInForeground = false
            sessionManager.onAppBackgrounded()
        }
    }

    /**
     * Initializes RevenueCat once at process start.
     * Must run before [PremiumManager.start], which attaches the customer-info listener and
     * triggers the automatic entitlement recovery for fresh installs / reinstalls.
     */
    private fun configureRevenueCat() {
        if (Purchases.isConfigured) return

        Purchases.logLevel = if (BuildConfig.DEBUG || BillingConstants.ENABLE_DEBUG_LOGGING) {
            LogLevel.DEBUG
        } else {
            LogLevel.INFO
        }

        // No appUserID: RevenueCat manages an anonymous id and persists it. A reinstall therefore
        // gets a new id, which is exactly why PremiumManager syncs Play purchases onto it at launch.
        Purchases.configure(
            PurchasesConfiguration.Builder(this, BillingConstants.REVENUECAT_API_KEY)
                .build(),
        )


        if (BuildConfig.DEBUG || BillingConstants.ENABLE_DEBUG_LOGGING) {
            Log.d(BillingConstants.TAG, "RevenueCat configured (Google Play SDK key)")
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
    }

}
