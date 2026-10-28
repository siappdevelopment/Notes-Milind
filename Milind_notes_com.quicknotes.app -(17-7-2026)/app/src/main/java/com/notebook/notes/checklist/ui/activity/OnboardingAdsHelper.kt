package com.notebook.notes.checklist.ui.activity

import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.ads.module.ads.wrapper.ApNativeAd
import com.facebook.shimmer.ShimmerFrameLayout
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.populateNativeAdView

/**
 * Onboarding native ads, driven by remote config ([AdsManager]).
 * Loads in the activity, renders from the LiveData, and falls back to a hidden ad band
 * (never a crash or an empty card) when the ad is disabled, purchased, offline or failed.
 */
object OnboardingAdsHelper {

    /** Page 1 uses `native_onboarding_*_1`; later pages use `native_onboarding_*_4`. */
    fun load(
        activity: AppCompatActivity,
        page: Int,
        isFirst: Boolean,
        adsContainer: View,
        cardView: CardView,
        shimmer: ShimmerFrameLayout,
        nativePlaceholder: FrameLayout,
        bannerPlaceholder: FrameLayout,
    ) {
        showLoading(adsContainer, cardView, shimmer, nativePlaceholder, bannerPlaceholder)

        val live: MutableLiveData<ApNativeAd?> =
            if (page == 1) AdsManager.nativeOnboarding1AdLive else AdsManager.nativeOnboarding4AdLive
        // Page 1 keeps the ad SplashActivity preloaded; later pages drop the previous ad so it is not shown twice.
        if (page != 1) live.value = null

        var skippedInitialNull = true
        live.observe(activity, Observer { ad ->
            if (ad != null) {
                adsContainer.visibility = View.VISIBLE
                cardView.visibility = View.VISIBLE
                nativePlaceholder.visibility = View.VISIBLE
                populateNativeAdView(activity, ad, nativePlaceholder, shimmer)
            } else if (!skippedInitialNull) {
                hide(adsContainer, cardView, shimmer, nativePlaceholder, bannerPlaceholder)
            }
            skippedInitialNull = false
        })

        // Page 1 was preloaded on splash, so only later pages request here.
        if (page != 1) {
            AdsManager.loadNativeOnboarding4(activity, isFirst, R.layout.layout_native_onboarding)
        }
    }

    private fun showLoading(
        adsContainer: View,
        cardView: CardView,
        shimmer: ShimmerFrameLayout,
        nativePlaceholder: FrameLayout,
        bannerPlaceholder: FrameLayout,
    ) {
        adsContainer.visibility = View.VISIBLE
        cardView.visibility = View.VISIBLE
        shimmer.visibility = View.VISIBLE
        nativePlaceholder.visibility = View.GONE
        bannerPlaceholder.visibility = View.GONE
        if (!shimmer.isShimmerStarted) {
            shimmer.startShimmer()
        }
    }

    private fun hide(
        adsContainer: View,
        cardView: CardView,
        shimmer: ShimmerFrameLayout,
        nativePlaceholder: FrameLayout,
        bannerPlaceholder: FrameLayout,
    ) {
        // INVISIBLE (not GONE): the reserved ads band stays so onboarding content does not reflow.
        adsContainer.visibility = View.INVISIBLE
        cardView.visibility = View.GONE
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        nativePlaceholder.visibility = View.GONE
        bannerPlaceholder.visibility = View.GONE
    }
}
