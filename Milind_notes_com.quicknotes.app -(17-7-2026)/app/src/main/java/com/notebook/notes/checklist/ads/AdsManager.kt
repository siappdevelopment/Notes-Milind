package com.notebook.notes.checklist.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
import com.notebook.notes.checklist.utils.ext.goneView
import timber.log.Timber

@SuppressLint("StaticFieldLeak")
object AdsManager {

    val nativeLanguageAdLive = MutableLiveData<ApNativeAd?>()
    val nativeLanguageClickAdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding1AdLive = MutableLiveData<ApNativeAd?>()
    val nativeOnboarding4AdLive = MutableLiveData<ApNativeAd?>()
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

    private var interSplashAd: ApInterstitialAd? = null
    private var interOnboarding: ApInterstitialAd? = null

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

    fun loadNativeOnboarding4(activity: Activity, isFirst: Boolean, layoutRes: Int) {
        val config =
            if (isFirst) AdRemoteConfig.native_onboarding_1_4 else AdRemoteConfig.native_onboarding_2_4
        loadNativeInternal(
            activity,
            config,
            layoutRes,
            nativeOnboarding4AdLive,
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
        val config = AdRemoteConfig.native_home
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
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || (!ignoreLimit && !NotesAd.getInstance().getShouldDisplayInterOnboarding(config.enableUaCheck))
        ) {
            interOnboarding = null
            return
        }
        interOnboarding =
            NotesAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    fun showInterOnboarding(context: Context, ignoreLimit: Boolean = false, onAction: () -> Unit) {
        val interstitial = interOnboarding
        if (interstitial != null && interstitial.isReady && !AppPurchase.getInstance()
                .isPurchased(context) && (ignoreLimit || NotesAd.getInstance().getShouldDisplayInterOnboarding(AdRemoteConfig.inter_onboarding.enableUaCheck))
        ) {
            NotesAd.getInstance()
                .forceShowInterstitial(context, interstitial, object : AdCallback() {
                    override fun onNextAction() {
                        super.onNextAction()
                        onAction()
                    }
                }, true)
        } else {
            onAction()
        }
    }

    private var interBottomNav: ApInterstitialAd? = null
    private var bottomNavChangeCount = 0

    /** Preloads the bottom-navigation interstitial (`inter_bottom_nav`). No-op while one is already loaded. */
    fun loadInterBottomNav(context: Context) {
        val config = AdRemoteConfig.inter_bottom_nav
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || !NotesAd.getInstance().getShouldDisplayInterOnboarding(config.enableUaCheck)
            || interBottomNav?.isReady == true
        ) return
        interBottomNav =
            NotesAd.getInstance().getInterstitialAds(context, config.id, object : AdCallback() {})
    }

    /**
     * Call on every bottom-navigation tab change. The interstitial shows on every
     * `clickCount`-th change (remote config), e.g. 3rd, 6th, 9th; otherwise, or when no ad is ready,
     * [onAction] runs straight away.
     */
    fun showInterBottomNav(context: Context, onAction: () -> Unit) {
        val config = AdRemoteConfig.inter_bottom_nav
        if (!config.isEnable
            || AppPurchase.getInstance().isPurchased(context)
            || !NotesAd.getInstance().getShouldDisplayInterOnboarding(config.enableUaCheck)
        ) {
            onAction()
            return
        }
        // Fixed schedule: 3rd, 6th, 9th... change. A missed slot (ad not ready) is not retried early.
        bottomNavChangeCount++
        Timber.tag("BottomNavInter").d(
            "change=$bottomNavChangeCount clickCount=${config.clickCount} ready=${interBottomNav?.isReady}"
        )
        if (bottomNavChangeCount % config.clickCount != 0) {
            onAction()
            return
        }
        val interstitial = interBottomNav
        if (interstitial == null || !interstitial.isReady) {
            Timber.tag("BottomNavInter").d("slot missed: ad not ready (ad=${interstitial != null})")
            loadInterBottomNav(context)
            onAction()
            return
        }
        // shouldReloadAds = true: the SDK reloads this same wrapper after it closes.
        NotesAd.getInstance().forceShowInterstitial(context, interstitial, object : AdCallback() {
            override fun onNextAction() {
                super.onNextAction()
                Timber.tag("BottomNavInter").d("onNextAction")
                onAction()
            }

            override fun onInterstitialLoad(interstitialAd: InterstitialAd?) {
                super.onInterstitialLoad(interstitialAd)
                Timber.tag("BottomNavInter").d("reloaded after close: ${interstitialAd != null}")
            }

            override fun onAdFailedToLoad(i: LoadAdError?) {
                super.onAdFailedToLoad(i)
                Timber.tag("BottomNavInter").d("reload failed: ${i?.message}")
            }
        }, true)
    }

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

        NotesAd.getInstance().initRewardAds(
            activity,
            AdRemoteConfig.reward_example.id,
            object : AdCallback() {
                override fun onRewardAdLoaded(rewardedAd: RewardedAd?) {
                    super.onRewardAdLoaded(rewardedAd)
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
        nativeOnboarding4AdLive.postValue(null)
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
