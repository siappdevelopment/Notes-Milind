package com.notebook.notes.checklist.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.ads.module.R
import com.ads.module.ads.NotesAd
import com.ads.module.ads.wrapper.ApInterstitialAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.billing.AppPurchase
import com.ads.module.funtion.AdCallback
import com.ads.module.funtion.RewardCallback
import com.ads.module.util.AppConstant
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.utils.ext.goneView
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object AdsManager {

    val nativeLanguageAdLive = MutableLiveData<ApNativeAd?>()
    val nativeLanguageClickAdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding1AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding2AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding3AdLive = MutableLiveData<ApNativeAd?>()
    val nativeAdOnBoardingFullLive = MutableLiveData<ApNativeAd?>()
    val nativeAdOnBoardingFull2Live = MutableLiveData<ApNativeAd?>()
    val nativeSurveyAdLive = MutableLiveData<ApNativeAd?>()
    val nativeConfirmUninstallAdLive = MutableLiveData<ApNativeAd?>()
    val nativeWelcomeAdLive = MutableLiveData<ApNativeAd?>()
    val nativePermissionAdLive = MutableLiveData<ApNativeAd?>()
    val nativeHomeAdLive = MutableLiveData<ApNativeAd?>()
    val nativeCreateNoteAdLive = MutableLiveData<ApNativeAd?>()
    val nativeDrawingAdLive = MutableLiveData<ApNativeAd?>()
    var interWelcomeAdLive = MutableLiveData<ApInterstitialAd?>()
    // Auto-resolve config for each loaded native ad
    private val adConfigMap = mutableMapOf<ApNativeAd, AdUnitConfig>()
    fun getAdConfig(ad: ApNativeAd): AdUnitConfig? = adConfigMap[ad]

    /** Lets [loadScreenAd] register the config a natively loaded ad belongs to, so its CTA/components apply. */
    internal fun rememberConfig(ad: ApNativeAd, config: AdUnitConfig) {
        adConfigMap[ad] = config
    }

    private var interSplashAd: ApInterstitialAd? = null
    private var interOnboarding: ApInterstitialAd? = null
    private var interOnboardingLoading = false
    private var interOnboardingWaiting = false
    private const val INTER_ONBOARDING_MAX_WAIT_MS = 5000L
    private const val INTER_ONBOARDING_POLL_MS = 150L

    private var rewardExample :RewardedAd? = null

    private fun loadNativeInternal(
        activity: Activity,
        config: AdUnitConfig,
        layoutRes: Int,
        liveData: MutableLiveData<ApNativeAd?>,
        shouldDisplay: Boolean = true,
    ) {
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(activity)
            || !activity.isNetworkAvailable()
            || !shouldDisplay
        ) {
            liveData.postValue(null)
            return
        }
        NotesAd.getInstance()
            .loadNativeAdResultCallback(activity, config.id, layoutRes, object : AdCallback() {
                override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    adConfigMap[nativeAd] = config
                    liveData.postValue(nativeAd)
                }

                override fun onAdFailedToLoad(adError: LoadAdError?) {
                    super.onAdFailedToLoad(adError)
                    liveData.postValue(null)
                }
            })
    }

    fun loadNativeLanguage(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_language_1 else AdRemoteConfig.native_language_2
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeLanguageAdLive
        )
    }

    fun loadNativeLanguageClick(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_language_1_click else AdRemoteConfig.native_language_2_click
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeLanguageClickAdLive
        )
    }

    fun loadNativeOnboarding1(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_1 else AdRemoteConfig.native_onboarding_2_1
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding1AdLive
        )
    }

    fun loadNativeOnboarding2(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_2 else AdRemoteConfig.native_onboarding_2_2
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding2AdLive,
            NotesAd.getInstance().getShouldDisplayNativeOnboardingNormal2(config.enableUaCheck)
        )
    }

    fun loadNativeOnboarding3(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_3 else AdRemoteConfig.native_onboarding_2_3
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding3AdLive,
            NotesAd.getInstance().getShouldDisplayNativeOnboardingNormal2(config.enableUaCheck)
        )
    }

    fun loadNativeOnboardingFull(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_3 else AdRemoteConfig.native_onboarding_fullscreen_2_3
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeAdOnBoardingFullLive,
            NotesAd.getInstance().getShouldDisplayNativeOnboardingFull1(config.enableUaCheck)
        )
    }

    fun loadNativeOnboardingFull2(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_fullscreen_1_4 else AdRemoteConfig.native_onboarding_fullscreen_2_4
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeAdOnBoardingFull2Live,
            NotesAd.getInstance().getShouldDisplayNativeOnboardingFull2(config.enableUaCheck)
        )
    }

    fun loadNativePermission(activity: Activity, layoutRes: Int) {
        val config = AdRemoteConfig.native_permission
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativePermissionAdLive,
            NotesAd.getInstance().getShouldDisplayNativePermission(config.enableUaCheck)
            )
    }

    fun loadNativeHome(activity: Activity, layoutRes: Int) {
        val config = AdRemoteConfig.home_screen.forNative()
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeHomeAdLive,
            NotesAd.getInstance().getShouldDisplayNativeHome(config.enableUaCheck)
        )
    }

    fun loadNativeCreateNote(activity: Activity, layoutRes: Int) {
        val config = AdRemoteConfig.create_note.forNative()
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeCreateNoteAdLive,
            NotesAd.getInstance().getShouldDisplayNativeHome(config.enableUaCheck)
        )
    }

    fun loadNativeDrawing(activity: Activity, layoutRes: Int) {
        val config = AdRemoteConfig.native_drawing
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeDrawingAdLive,
            NotesAd.getInstance().getShouldDisplayNativeHome(config.enableUaCheck)
        )
    }

    fun loadNativeSurvey(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity, AdRemoteConfig.native_survey, layoutRes, nativeSurveyAdLive,
            NotesAd.getInstance()
                .getShouldDisplayWidgetUninstall(AdRemoteConfig.native_survey.enableUaCheck)
        )
    }

    fun loadNativeConfirmUninstall(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_confirm_uninstall,
            layoutRes,
            nativeConfirmUninstallAdLive,
            NotesAd.getInstance()
                .getShouldDisplayWidgetUninstall(AdRemoteConfig.native_confirm_uninstall.enableUaCheck)
        )
    }

    fun loadNativeWelcome(activity: Activity, layoutRes: Int) {
        loadNativeInternal(
            activity,
            AdRemoteConfig.native_welcome,
            layoutRes,
            nativeWelcomeAdLive,
            NotesAd.getInstance().getShouldDisplayNativeWelcomeBack(AdRemoteConfig.native_welcome.enableUaCheck)
        )
    }

    // ── Dashboard / Test helpers (ignore shouldDisplay) ──

    /** Dedicated LiveData for customization preview – won't collide with real flows */
    val nativeDashboardPreviewLive = MutableLiveData<ApNativeAd?>()

    /**
     * Load a native ad for dashboard preview purposes.
     * Bypasses all shouldDisplay checks so it always loads.
     */
    fun loadNativeForDashboard(activity: Activity, configKey: String, layoutRes: Int) {
        val config = try {
            AdRemoteConfig.getInstance().ads[configKey]
                ?: AdUnitConfig(id = "", isEnable = false)
        } catch (_: Exception) {
            AdUnitConfig(id = "", isEnable = false)
        }
        // Force shouldDisplay = true to bypass SDK limits
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeDashboardPreviewLive,
            shouldDisplay = true
        )
    }

    /** Load native language ad for dashboard – ignores shouldDisplay */
    fun loadNativeLanguageForDashboard(activity: Activity, layoutRes: Int) {
        loadNativeForDashboard(activity, "native_language_1", layoutRes)
    }

    /** Load native onboarding full for dashboard – ignores shouldDisplay */
    fun loadNativeFullForDashboard(activity: Activity, layoutRes: Int) {
        loadNativeForDashboard(activity, "native_onboarding_fullscreen_1_3", layoutRes)
    }

    fun loadInterOnboarding(context: Context, ignoreLimit: Boolean = false) {
        val config = AdRemoteConfig.inter_onboarding
        Timber.tag("InterOnboarding").d(
            "load: isEnable=${config.isEnable} purchased=${AppPurchase.getInstance().isPurchased(context)} " +
                "uaCheck=${config.enableUaCheck} shouldDisplay=${NotesAd.getInstance().getShouldDisplayInterOnboarding(config.enableUaCheck)} id=${config.id}"
        )
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || (!ignoreLimit && !NotesAd.getInstance().getShouldDisplayInterOnboarding(config.enableUaCheck))
        ) {
            interOnboarding = null
            return
        }
        interOnboardingLoading = true
        interOnboarding =
            NotesAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {
                override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                    super.onApInterstitialLoad(apInterstitialAd)
                    interOnboardingLoading = false
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    interOnboardingLoading = false
                }
            })
    }

    /**
     * Shows `inter_onboarding`, then calls [onAction] once. Ready: show now. Still loading: loading dialog
     * and wait up to [INTER_ONBOARDING_MAX_WAIT_MS]. Not allowed, failed or never requested: [onAction] at once.
     */
    fun showInterOnboarding(context: Context, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interOnboarding
        val allowed = interstitial != null &&
            !AppPurchase.getInstance().isPurchased(context) &&
            (ignoreLimit || NotesAd.getInstance().getShouldDisplayInterOnboarding(AdRemoteConfig.inter_onboarding.enableUaCheck))
        if (!allowed || interstitial == null) {
        Timber.tag("InterOnboarding").d(
            "show: ad=${interstitial != null} ready=${interstitial?.isReady} loading=$interOnboardingLoading allowed=$allowed"
        )
            onAction()
            return
        }
        if (interstitial.isReady) {
            forceShowInterOnboarding(context, interstitial, onAction)
            return
        }
        if (!interOnboardingLoading) {
            onAction()
            return
        }
        if (interOnboardingWaiting) return
        interOnboardingWaiting = true
        ADSUtilitis.MassageBoxFull(context)
        val handler = Handler(Looper.getMainLooper())
        val startedAt = SystemClock.elapsedRealtime()
        handler.post(object : Runnable {
            override fun run() {
                val gone = (context as? Activity)?.let { it.isFinishing || it.isDestroyed } == true
                val waitedOut = SystemClock.elapsedRealtime() - startedAt >= INTER_ONBOARDING_MAX_WAIT_MS
                when {
                    gone -> {
                        interOnboardingWaiting = false
                        ADSUtilitis.MassageBoxFullDismiss()
                    }
                    interstitial.isReady -> {
                        interOnboardingWaiting = false
                        ADSUtilitis.MassageBoxFullDismiss()
                        forceShowInterOnboarding(context, interstitial, onAction)
                    }
                    waitedOut || !interOnboardingLoading -> {
                        interOnboardingWaiting = false
                        ADSUtilitis.MassageBoxFullDismiss()
                        onAction()
                    }
                    else -> handler.postDelayed(this, INTER_ONBOARDING_POLL_MS)
                }
            }
        })
    }

    private fun forceShowInterOnboarding(context: Context, interstitial: ApInterstitialAd, onAction: () -> Unit) {
        NotesAd.getInstance()
            .forceShowInterstitial(context, interstitial, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onAction()
                }
            }, true)
    }

    /**
     * A remote-config interstitial shown on every `clickCount`-th trigger (e.g. 3rd, 6th, 9th).
     * Each instance has its own ad and its own counter, so every trigger type is controlled separately.
     * A slot is skipped (not retried early) when no ad is ready; [onAction] always runs.
     */
    private class CountedInter(
        private val tag: String,
        private val config: () -> AdUnitConfig,
    ) {
        private var ad: ApInterstitialAd? = null
        private var triggerCount = 0

        /** Preloads the ad; no-op while one is already loaded. */
        fun load(context: Context) {
            val cfg = config()
            if (!cfg.isEnable
                || AppPurchase.getInstance().isPurchased(context)
                || !NotesAd.getInstance().getShouldDisplayInterOnboarding(cfg.enableUaCheck)
                || ad?.isReady == true
            ) return
            ad = NotesAd.getInstance().getInterstitialAds(context, cfg.id, object : AdCallback() {})
        }

        fun show(context: Context, onAction: () -> Unit) {
            val cfg = config()
            if (!cfg.isEnable
                || AppPurchase.getInstance().isPurchased(context)
                || !NotesAd.getInstance().getShouldDisplayInterOnboarding(cfg.enableUaCheck)
            ) {
                onAction()
                return
            }
            triggerCount++
            Timber.tag(tag).d("trigger=$triggerCount clickCount=${cfg.clickCount} ready=${ad?.isReady}")
            if (triggerCount % cfg.clickCount != 0) {
                onAction()
                return
            }
            val interstitial = ad
            if (interstitial == null || !interstitial.isReady) {
                Timber.tag(tag).d("slot missed: ad not ready")
                load(context)
                onAction()
                return
            }
            // shouldReloadAds = true: the SDK reloads this same wrapper after it closes.
            NotesAd.getInstance().forceShowInterstitial(context, interstitial, object : AdCallback() {
                override fun onNextAction() {
                    super.onNextAction()
                    onAction()
                }
            }, true)
        }
    }

    private val interBottomNav = CountedInter("InterBottomNav") { AdRemoteConfig.inter_bottom_nav }
    private val interScreenOpen = CountedInter("InterScreenOpen") { AdRemoteConfig.inter_screen_open }
    // Same config (`inter_screen_open`) as screen-open, but its own ad and counter, so Create Note back presses
    // do not shift which screen-open trigger lands on `clickCount` (the inter must show when a note opens).
    private val interCreateNoteBack = CountedInter("InterCreateNoteBack") { AdRemoteConfig.inter_screen_open }

    /** Bottom-navigation tab change (`inter_bottom_nav`). */
    fun loadInterBottomNav(context: Context) = interBottomNav.load(context)

    fun showInterBottomNav(context: Context, onAction: () -> Unit) =
        interBottomNav.show(context, onAction)

    /**
     * Screen changes: opening another screen and back presses of the other screens share this one control
     * (`inter_screen_open`), so they use the same ad and the same counter (Create Note back has its own, below).
     */
    fun loadInterScreenOpen(context: Context) = interScreenOpen.load(context)

    fun showInterScreenOpen(context: Context, onAction: () -> Unit) =
        interScreenOpen.show(context, onAction)

    /** Create Note back press: `inter_screen_open` config with a separate ad and counter. */
    fun loadInterCreateNoteBack(context: Context) = interCreateNoteBack.load(context)

    fun showInterCreateNoteBack(context: Context, onAction: () -> Unit) =
        interCreateNoteBack.show(context, onAction)

    fun loadInterWelcome(context: Context, ignoreLimit: Boolean = false) {
        val cached = interWelcomeAdLive.value?.takeIf { it.isReady }
        val request = MutableLiveData<ApInterstitialAd?>()
        interWelcomeAdLive = request
        fun emit(ad: ApInterstitialAd?) {
            request.value = ad
        }
        val config = AdRemoteConfig.inter_welcome
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || (!ignoreLimit && !NotesAd.getInstance()
                .getShouldDisplayInterWelcomeBack(config.enableUaCheck))
        ) {
            emit(null)
            return
        }
        if (cached != null) {
            emit(cached)
            return
        }
        var loaded: ApInterstitialAd? = null
        loaded = NotesAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {
            override fun onApInterstitialLoad(apInterstitialAd: ApInterstitialAd?) {
                super.onApInterstitialLoad(apInterstitialAd)
                emit(apInterstitialAd ?: loaded)
            }

            override fun onAdFailedToLoad(adError: LoadAdError?) {
                super.onAdFailedToLoad(adError)
                emit(null)
            }
        })
        if (loaded?.isReady == true) emit(loaded)
    }

    fun showInterWelcome(context: Context, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interWelcomeAdLive.value
        val allowShow = ignoreLimit || NotesAd.getInstance()
            .getShouldDisplayInterWelcomeBack(AdRemoteConfig.inter_welcome.enableUaCheck)
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context) && allowShow
        ) {
            NotesAd.getInstance()
                .forceShowInterstitial(context, interstitial, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        interWelcomeAdLive = MutableLiveData()
                        onAction()
                    }
                }, false)
        } else {
            onAction()
        }
    }

    fun loadAndShowReward(
        activity: Activity,
        onSuccess: () -> Unit,
        onFailed: () -> Unit
    ) {
        if (AdRemoteConfig.reward_example.isEnable.not() || AppPurchase.getInstance()
                .isPurchased(activity)
        ) {
            onFailed()
            return
        }

        ADSUtilitis.MassageBoxFull(activity)
        NotesAd.getInstance().initRewardAds(
            activity,
            AdRemoteConfig.reward_example.id,
            object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                    super.onRewardAdLoaded(rewardedAd)
                    ADSUtilitis.MassageBoxFullDismiss()
                    rewardExample = rewardedAd

                    var isEarn = false
                    NotesAd.getInstance().showRewardAds(
                        activity,
                        rewardExample,
                        object : RewardCallback {
                            override fun onUserEarnedReward(var1: RewardItem?) {
                                isEarn = true
                                rewardExample = null
                            }

                            override fun onRewardedAdClosed() {
                                if (isEarn) onSuccess()
                                else onFailed()
                            }

                            override fun onRewardedAdFailedToShow(codeError: Int) {
                                rewardExample = null
                                onFailed()
                            }

                            override fun onAdClicked() {

                            }
                        }
                    )
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    ADSUtilitis.MassageBoxFullDismiss()
                    onFailed()
                }
            }
        )
    }

    fun loadBanner(
        activity: AppCompatActivity,
        adUnitConfig: AdUnitConfig,
        frAds: FrameLayout,
        isCollapse: Boolean,
    ) {
        if (adUnitConfig.isEnable) {
            removeBannerView(activity, frAds)
            if (isCollapse) NotesAd.getInstance().loadCollapsibleBanner(
                activity,
                adUnitConfig.id,
                AppConstant.CollapsibleGravity.BOTTOM,
                object : AdCallback() {
                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        frAds.goneView()
                        Timber.tag("AdsManager_Banner")
                            .d("Load banner on ${activity.javaClass.simpleName} failed by : ${i?.message}")
                    }
                })
            else NotesAd.getInstance()
                .loadBanner(activity, adUnitConfig.id, object : AdCallback() {
                    override fun onAdFailedToLoad(i: LoadAdError?) {
                        super.onAdFailedToLoad(i)
                        frAds.goneView()
                        Timber.tag("AdsManager_Banner")
                            .d("Load banner on ${activity.javaClass.simpleName} failed by : ${i?.message}")
                    }
                })
        } else {
            frAds.removeAllViews()
            frAds.goneView()
        }
    }

    @SuppressLint("InflateParams")
    private fun removeBannerView(activity: Activity, frAds: FrameLayout) {
        try {
            val container = frAds.findViewById<FrameLayout>(R.id.banner_container)
            if (container != null) {
                for (i in 0 until container.childCount) {
                    val view = container.getChildAt(i)
                    if (view is AdView) {
                        view.destroy()
                        container.removeView(view)
                    }
                }
            }
            val shimmerFrameLayout = LayoutInflater.from(activity)
                .inflate(R.layout.layout_banner_control, null)
            frAds.removeAllViews()
            frAds.addView(shimmerFrameLayout)
        } catch (_: Exception) {
        }
    }

    fun clearAll() {
        nativeLanguageAdLive.postValue(null)
        nativeLanguageClickAdLive.postValue(null)
        nativeOnboarding1AdLive.postValue(null)
        nativeOnboarding2AdLive.postValue(null)
        nativeOnboarding3AdLive.postValue(null)
        nativeAdOnBoardingFullLive.postValue(null)
        nativeSurveyAdLive.postValue(null)
        nativeConfirmUninstallAdLive.postValue(null)
        nativeWelcomeAdLive.postValue(null)
        interWelcomeAdLive = MutableLiveData()
        interSplashAd = null
        interOnboarding = null
    }

    private fun Context.isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
        }
    }
}
