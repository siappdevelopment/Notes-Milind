package com.notebook.notes.checklist.ui.uninstall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSUtilitis
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.populateNativeAdView
import com.notebook.notes.checklist.databinding.ActivitySurveyBinding
import com.notebook.notes.checklist.ui.activity.BaseActivity
import com.notebook.notes.checklist.ui.activity.HomeActivity

/**
 * Step 2 of the uninstall-shortcut flow: pick a reason, then open the system App Info screen
 * (where the user can uninstall). Cancel / back → Home.
 */
class SurveyActivity : BaseActivity() {

    private lateinit var binding: ActivitySurveyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        binding.btnUninstall.setOnClickListener { openAppInfo() }
        binding.btnCancel.setOnClickListener { openHome() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = openHome()
        })

        loadAd()
    }

    private fun openAppInfo() {
        try {
            ADSUtilitis.trackScreen(this, "Survey_Uninstall_Click")
        } catch (_: Exception) {
            // analytics must never block navigation
        }
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
        finish()
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    /** Uses the ad ConfirmUninstallActivity preloaded; requests it here if it is not there yet. */
    private fun loadAd() {
        val card = binding.adContainer.cardAd
        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            card.visibility = View.GONE
            return
        }
        var skippedInitialNull = true
        AdsManager.nativeSurveyAdLive.observe(this) { ad ->
            if (ad != null) {
                card.visibility = View.VISIBLE
                populateNativeAdView(this, ad, binding.adContainer.flAdPlaceholder, binding.adContainer.shimmerAd)
            } else if (!skippedInitialNull) {
                card.visibility = View.GONE
            }
            skippedInitialNull = false
        }
        if (AdsManager.nativeSurveyAdLive.value == null) {
            AdsManager.loadNativeSurvey(this, com.notebook.notes.checklist.R.layout.layout_native_language)
        }
    }
}
