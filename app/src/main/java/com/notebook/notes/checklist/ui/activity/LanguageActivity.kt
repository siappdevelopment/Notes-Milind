package com.notebook.notes.checklist.ui.activity

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.notebook.notes.checklist.Advertisement.ADSAppManage
import com.notebook.notes.checklist.Advertisement.ADSBannerAdaptive
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSInterDisplay
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.R
import com.ads.module.ads.wrapper.ApNativeAd
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.AppSharedPref
import com.notebook.notes.checklist.ads.populateNativeAdView
import com.notebook.notes.checklist.databinding.ActivityLanguageBinding
import com.notebook.notes.checklist.flow.FlowManager
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.model.LanguageCatalog
import com.notebook.notes.checklist.ui.adapter.LanguageAdapter
import com.notebook.notes.checklist.util.LocaleHelper
import com.notebook.notes.checklist.utils.Constant
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LanguageActivity : BaseActivity() {
    private lateinit var binding: ActivityLanguageBinding
    private lateinit var adapter: LanguageAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val showDoneRunnable = Runnable { updateDoneVisibility(true) }

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    @Inject
    lateinit var flowManager: FlowManager

    @Inject
    lateinit var appSharedPref: AppSharedPref

    private var languageClickRequested = false
    private var hasClickAd = false

    private val languages = LanguageCatalog.languages.map { it.copy() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLanguageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        applySavedSelection()
        setupRecyclerView()
        setupListeners()

        if (preferenceHelper.getBoolean(Constant.PENDING_LANGUAGE_NAVIGATION, false)) {
            preferenceHelper.putBoolean(Constant.PENDING_LANGUAGE_NAVIGATION, false)
            val fromSettings = preferenceHelper.getBoolean(
                Constant.PENDING_LANGUAGE_FROM_SETTINGS,
                false
            )
            preferenceHelper.putBoolean(Constant.PENDING_LANGUAGE_FROM_SETTINGS, false)
            navigateAfterLanguageSelection(fromSettings)
        }
        if (ADSMainClass.shouldShowConsentOnLanguage()) {
            continueAfterConsent { loadLanguageAds() }
        } else {
            initializeAdsAndLoad { loadLanguageAds() }
        }
    }
    private fun continueAfterConsent(action: () -> Unit) {
        if (isFinishing || isDestroyed) return

        ADSConsentManager.gatherConsent(this, showForm = true) { formShown ->
            if (isFinishing || isDestroyed) return@gatherConsent

            if (formShown || ADSConsentManager.canRequestAds(this)) {
                ADSMainClass.setConsentPendingOnLanguage(false)
            }

            initializeAdsAndLoad(action)
        }
    }
    private fun initializeAdsAndLoad(action: () -> Unit) {
        if (ADSConsentManager.canRequestAds(this) && !ADSMainClass.getAds_Free() ) {
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


    private fun applySavedSelection() {
        // Fresh install: no language selected until the user taps one.
        if (!flowManager.isLanguageCompleted()) {
            languages.forEach { it.isSelected = false }
            return
        }
        val savedCode = preferenceHelper.getString(
            Constant.PREF_SELECTED_LANGUAGE_CODE,
            Constant.DEFAULT_LANGUAGE_CODE
        ) ?: Constant.DEFAULT_LANGUAGE_CODE
        languages.forEach { it.isSelected = it.code == savedCode }
        if (languages.none { it.isSelected }) {
            languages.first().isSelected = true
        }
    }

    private fun updateDoneVisibility(hasSelection: Boolean) {
        binding.btnDone.visibility = if (hasSelection) View.VISIBLE else View.INVISIBLE
        binding.btnDone.isEnabled = hasSelection
    }

    private fun scheduleDoneIconVisible() {
        handler.removeCallbacks(showDoneRunnable)
        updateDoneVisibility(false)
        handler.postDelayed(showDoneRunnable, DONE_ICON_DELAY_MS)
    }

    private fun setupRecyclerView() {
        adapter = LanguageAdapter(languages) {
            scheduleDoneIconVisible()
            loadLanguageClickAd()
        }
        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(this@LanguageActivity)
            adapter = this@LanguageActivity.adapter
        }
        // Fresh install: keep Done hidden until user selects a language (then delay).
        // Returning users: show Done immediately for the already-saved selection.
        if (flowManager.isLanguageCompleted() && adapter.hasSelection()) {
            updateDoneVisibility(true)
        } else {
            updateDoneVisibility(false)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(showDoneRunnable)
        super.onDestroy()
    }

    private fun setupListeners() {
        binding.btnDone.setOnClickListener {
            if (!adapter.hasSelection()) return@setOnClickListener

            if (ADSMainClass.getLanguageInterAdsShow() && ADSUtilitis.IsNetworkConnected(this)){
                ADSInterDisplay.ADSInterstitialShowing(
                    this@LanguageActivity,
                    ADSMainClass.getStringValue(ADSMainClass.LANGUAGE_SCREEN_INTER),
                    ADSInterDisplay.OnFinishAds {
                        continueToNextScreen()
                    },

                )

            }else{
                continueToNextScreen()
            }


        }
    }
    fun continueToNextScreen(){
        val selected = adapter.getSelectedLanguage() ?: return
        val currentCode = LocaleHelper.getSavedLanguageCode(this)
        val wasLanguageSelected = flowManager.isLanguageCompleted()

        preferenceHelper.putStringSync(Constant.PREF_SELECTED_LANGUAGE_CODE, selected.code)
        preferenceHelper.putStringSync(Constant.PREF_SELECTED_LANGUAGE_NAME, selected.nativeName)
        flowManager.markLanguageCompleted()

        if (selected.code != currentCode) {
            preferenceHelper.putBoolean(Constant.PENDING_LANGUAGE_NAVIGATION, true)
            preferenceHelper.putBoolean(Constant.PENDING_LANGUAGE_FROM_SETTINGS, wasLanguageSelected)
            LocaleHelper.applyLanguage(selected.code)
            return
        }

        navigateAfterLanguageSelection(wasLanguageSelected)
    }

    private fun navigateAfterLanguageSelection(fromSettings: Boolean) {
        if (!fromSettings) {
            flowManager.next(this)
        } else {
            finish()
        }
    }

    /**
     * Language native, driven by remote config ([AdsManager]). SplashActivity preloads it; if it is not
     * there yet we request it here. A second, "click" ad unit loads after the first language tap and
     * replaces the first ad. No ad (disabled, premium, offline, failed) simply hides the ad band.
     */
    private fun loadLanguageAds() {
        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            hideLanguageAds()
            return
        }
        val isFirst = appSharedPref.firstLanguage
        var skippedInitialNull = true
        AdsManager.nativeLanguageAdLive.observe(this) { ad ->
            if (ad != null) renderLanguageAd(ad)
            else if (!skippedInitialNull && !hasClickAd) hideLanguageAds()
            skippedInitialNull = false
        }
        AdsManager.nativeLanguageClickAdLive.observe(this) { ad ->
            if (ad != null) {
                hasClickAd = true
                renderLanguageAd(ad)
            }
        }
        if (AdsManager.nativeLanguageAdLive.value == null) {
            AdsManager.loadNativeLanguage(this, isFirst, R.layout.layout_native_language)
        }
    }

    /** Loads the "click" ad once, after the user first picks a language. */
    private fun loadLanguageClickAd() {
        if (languageClickRequested) return
        languageClickRequested = true
        AdsManager.nativeLanguageClickAdLive.value = null
        AdsManager.loadNativeLanguageClick(this, appSharedPref.firstLanguage, R.layout.layout_native_language_click)
    }

    private fun renderLanguageAd(ad: ApNativeAd) {
        binding.llAdsContainer.visibility = View.VISIBLE
        binding.cardView.visibility = View.VISIBLE
        binding.flBannerSmallPlaceholder.visibility = View.GONE
        populateNativeAdView(this, ad, binding.flNativeSmallPlaceholder, binding.shimmerContainerBanner)
    }

    private fun hideLanguageAds() {
        binding.shimmerContainerBanner.stopShimmer()
        binding.llAdsContainer.visibility = View.GONE
    }

    companion object {
        private const val DONE_ICON_DELAY_MS = 1500L
    }
}
