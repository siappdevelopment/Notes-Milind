package com.notebook.notes.checklist.ads

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.ads.module.ads.NotesAd
import com.ads.module.ads.wrapper.ApNativeAd
import com.ads.module.billing.AppPurchase
import com.ads.module.funtion.AdCallback
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.LoadAdError
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.R
import timber.log.Timber

/**
 * Bottom ad of a secondary screen (Settings, Set Password, ...), driven by an [AdUnitConfig] from the
 * Firebase ad JSON: `isEnable` switches it, `type` is "native" or "banner", `nativeId` / `bannerId`
 * hold the unit IDs. The screen must include `layout_other_screen_ads`.
 */
fun AdsManager.loadScreenAd(activity: AppCompatActivity, config: AdUnitConfig) {
    val container = activity.findViewById<ViewGroup>(R.id.llOtherScreenAds) ?: return

    fun hide() {
        container.visibility = View.GONE
    }

    if (!config.isEnable
        || AppPurchase.getInstance().isPurchased(activity)
        || ADSMainClass.getAds_Free()
        || !ADSConsentManager.canRequestAds(activity)
    ) {
        hide()
        return
    }

    container.visibility = View.VISIBLE
    val shimmer = activity.findViewById<ShimmerFrameLayout>(R.id.shimmer_container_banner)
    val nativeSlot = activity.findViewById<FrameLayout>(R.id.flNativeSmallPlaceholder)
    // Old banner slot of this layout; the SDK banner below brings its own container.
    activity.findViewById<View>(R.id.flBannerSmallPlaceholder)?.visibility = View.GONE

    if (config.type == "banner") {
        val bannerConfig = config.forBanner()
        if (bannerConfig.id.isBlank()) {
            hide()
            return
        }
        // The SDK resolves shimmer_container_banner with Activity.findViewById (first match); the native
        // card owns that id too, so drop the card or the banner's own shimmer would stay on top.
        (nativeSlot?.parent as? View)?.let { (it.parent as? ViewGroup)?.removeView(it) }
        val bannerSlot = FrameLayout(activity)
        container.addView(
            bannerSlot,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        loadBanner(activity, bannerConfig, bannerSlot, isCollapse = false)
        return
    }

    val nativeConfig = config.forNative()
    if (nativeConfig.id.isBlank() || nativeSlot == null || shimmer == null) {
        hide()
        return
    }
    shimmer.visibility = View.VISIBLE
    shimmer.startShimmer()
    nativeSlot.visibility = View.GONE
    NotesAd.getInstance().loadNativeAdResultCallback(
        activity,
        nativeConfig.id,
        R.layout.layout_native_ad_small,
        object : AdCallback() {
            override fun onNativeAdLoaded(nativeAd: ApNativeAd) {
                super.onNativeAdLoaded(nativeAd)
                if (activity.isFinishing || activity.isDestroyed) return
                rememberConfig(nativeAd, nativeConfig)
                populateNativeAdView(activity, nativeAd, nativeSlot, shimmer)
            }

            override fun onAdFailedToLoad(adError: LoadAdError?) {
                super.onAdFailedToLoad(adError)
                Timber.tag("ScreenAds").d("Native on ${activity.javaClass.simpleName} failed: ${adError?.message}")
                hide()
            }
        }
    )
}
