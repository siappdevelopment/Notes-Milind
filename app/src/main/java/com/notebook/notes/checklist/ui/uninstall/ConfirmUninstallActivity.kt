package com.notebook.notes.checklist.ui.uninstall

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.populateNativeAdView
import com.notebook.notes.checklist.databinding.ActivityConfirmUninstallBinding
import com.notebook.notes.checklist.ui.activity.BaseActivity
import com.notebook.notes.checklist.ui.activity.HomeActivity

/**
 * Step 1 of the uninstall-shortcut flow: "Try again" / back → Home, "Still uninstall" → [SurveyActivity].
 */
class ConfirmUninstallActivity : BaseActivity() {

    private lateinit var binding: ActivityConfirmUninstallBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmUninstallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.btnTryAgain.setOnClickListener { openHome() }
        binding.btnStillUninstall.setOnClickListener {
            startActivity(Intent(this, SurveyActivity::class.java))
            finish()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = openHome()
        })

        loadAd()
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    /** No ad (disabled, premium, offline, failed) simply hides the ad card. */
    private fun loadAd() {
        val card = binding.adContainer.cardAd
        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            card.visibility = View.GONE
            return
        }
        var skippedInitialNull = true
        AdsManager.nativeConfirmUninstallAdLive.observe(this) { ad ->
            if (ad != null) {
                card.visibility = View.VISIBLE
                populateNativeAdView(this, ad, binding.adContainer.flAdPlaceholder, binding.adContainer.shimmerAd)
            } else if (!skippedInitialNull) {
                card.visibility = View.GONE
            }
            skippedInitialNull = false
        }
        AdsManager.nativeConfirmUninstallAdLive.value = null
        AdsManager.loadNativeConfirmUninstall(this, R.layout.layout_native_language)
        // Warm the next screen's ad while the user decides.
        AdsManager.nativeSurveyAdLive.value = null
        AdsManager.loadNativeSurvey(this, R.layout.layout_native_language)
    }
}
