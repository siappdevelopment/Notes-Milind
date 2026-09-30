package com.notebook.notes.checklist.ui.activity

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import androidx.cardview.widget.CardView
import com.facebook.shimmer.ShimmerFrameLayout
import com.notebook.notes.checklist.Advertisement.ADSBannerAdaptive
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay

/**
 * Shared onboarding ads loader so page 1/2/3 all reset static native state
 * and avoid every-other-page empty ads from NativeByPage / AdmobNativeAd cache.
 */
object OnboardingAdsHelper {

    fun showDefaultLayout(
        adsContainer: View,
        cardView: CardView,
        shimmer: ShimmerFrameLayout,
        nativePlaceholder: FrameLayout,
        bannerPlaceholder: FrameLayout
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

    fun hideAdsLayout(
        adsContainer: View,
        cardView: CardView,
        shimmer: ShimmerFrameLayout,
        nativePlaceholder: FrameLayout,
        bannerPlaceholder: FrameLayout
    ) {
        // INVISIBLE (not GONE): reserved ads band stays so onboarding content does not reflow
        adsContainer.visibility = View.INVISIBLE
        cardView.visibility = View.GONE
        shimmer.visibility = View.GONE
        nativePlaceholder.visibility = View.GONE
        bannerPlaceholder.visibility = View.GONE
    }

    fun resetNativeLoadState() {
        ADSNativeDisplay.LoadingCheck = true
        ADSNativeDisplay.NativeByPage = ADSMainClass.getNativeByPage()
        ADSNativeDisplay.AdmobNativeAd = null
    }

    fun load(
        activity: Activity,
        adsContainer: View,
        cardView: CardView,
        shimmer: ShimmerFrameLayout,
        nativePlaceholder: FrameLayout,
        bannerPlaceholder: FrameLayout
    ) {
        showDefaultLayout(adsContainer, cardView, shimmer, nativePlaceholder, bannerPlaceholder)

        if (!ADSMainClass.getOnboardingScreenBottomAdShow() || ADSMainClass.getAds_Free()) {
            hideAdsLayout(adsContainer, cardView, shimmer, nativePlaceholder, bannerPlaceholder)
            return
        }

        if (!ADSConsentManager.canRequestAds(activity)) {
            return
        }

        if (ADSMainClass.getOnboardingType().equals("native", ignoreCase = true)) {
            cardView.visibility = View.VISIBLE
            nativePlaceholder.visibility = View.GONE
            shimmer.visibility = View.VISIBLE
            bannerPlaceholder.visibility = View.GONE

            resetNativeLoadState()
            ADSNativeDisplay.loadAdmobNativeAdBig(
                ADSMainClass.getStringValue(ADSMainClass.ONBOARDING_SCREEN_NATIVE),
                nativePlaceholder,
                shimmer,
                "big",
                activity
            )

            // If NativeByPage / LoadingCheck hid shimmer without binding, restore it.
            shimmer.post {
                if (activity.isFinishing || activity.isDestroyed) return@post
                ensureVisibleIfEmpty(
                    adsContainer, cardView, shimmer, nativePlaceholder, bannerPlaceholder
                )
            }
            // Preload race: ad may arrive just after the first post — check again.
            shimmer.postDelayed({
                if (activity.isFinishing || activity.isDestroyed) return@postDelayed
                ensureVisibleIfEmpty(
                    adsContainer, cardView, shimmer, nativePlaceholder, bannerPlaceholder
                )
            }, 600)
        } else {
            cardView.visibility = View.GONE
            nativePlaceholder.visibility = View.GONE
            shimmer.visibility = View.GONE
            bannerPlaceholder.visibility = View.VISIBLE
            ADSBannerAdaptive.loadAdMobBanner(
                ADSMainClass.getStringValue(ADSMainClass.ONBOARDING_SCREEN_BANNER),
                bannerPlaceholder,
                shimmer,
                activity
            )
        }
    }

    private fun ensureVisibleIfEmpty(
        adsContainer: View,
        cardView: CardView,
        shimmer: ShimmerFrameLayout,
        nativePlaceholder: FrameLayout,
        bannerPlaceholder: FrameLayout
    ) {
        if (!ADSMainClass.getOnboardingScreenBottomAdShow() || ADSMainClass.getAds_Free()) {
            return
        }
        adsContainer.visibility = View.VISIBLE
        cardView.visibility = View.VISIBLE
        val hasNativeAd = nativePlaceholder.childCount > 0
        val hasBannerAd = bannerPlaceholder.childCount > 0
        if (!hasNativeAd && !hasBannerAd) {
            shimmer.visibility = View.VISIBLE
            nativePlaceholder.visibility = View.GONE
            bannerPlaceholder.visibility = View.GONE
            if (!shimmer.isShimmerStarted) {
                shimmer.startShimmer()
            }
        }
    }
}
