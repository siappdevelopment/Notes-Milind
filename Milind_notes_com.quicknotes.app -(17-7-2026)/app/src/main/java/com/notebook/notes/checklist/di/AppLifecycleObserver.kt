package com.notebook.notes.checklist.di

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.ads.module.admob.AppOpenManager
import com.ads.module.ads.NotesAd
import com.ads.module.billing.AppPurchase

import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.inter_welcome
import com.notebook.notes.checklist.ui.activity.LanguageActivity
import com.notebook.notes.checklist.ui.activity.Onboarding1Activity
import com.notebook.notes.checklist.ui.activity.Onboarding2Activity
import com.notebook.notes.checklist.ui.activity.Onboarding3Activity
import com.notebook.notes.checklist.ui.activity.OnboardingFullAdActivity
import com.notebook.notes.checklist.ui.activity.SplashActivity
import com.notebook.notes.checklist.utils.Routes
import kotlin.jvm.java

class AppLifecycleObserver : DefaultLifecycleObserver {

    private val listActivityDisableResume = arrayListOf(
        SplashActivity::class.java,
        LanguageActivity::class.java,
        Onboarding1Activity::class.java,
        Onboarding2Activity::class.java,
        Onboarding3Activity::class.java,
        OnboardingFullAdActivity::class.java,
    )

    override fun onStart(owner: LifecycleOwner) {
        val currentActivity = GlobalApp.currentActivity
        if (currentActivity != null) {
            val isDisable = listActivityDisableResume.any { clazz ->
                clazz.isInstance(currentActivity)
            }
            if (!isDisable && ResumeAdsEntryRule.shouldShowWelcomeOnResume()
                && !AppOpenManager.getInstance().isInterstitialShowing
                && !AppPurchase.getInstance().isPurchased(currentActivity.applicationContext)
                && NotesAd.getInstance().getShouldDisplayInterWelcomeBack(AdRemoteConfig.inter_welcome.enableUaCheck)
            ) {
                if (AppOpenManager.getInstance().isDisableAdResumeByClickAction)
                    AppOpenManager.getInstance().isDisableAdResumeByClickAction = false
                else
                    Routes.startMainActivity(currentActivity)
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {}
}
