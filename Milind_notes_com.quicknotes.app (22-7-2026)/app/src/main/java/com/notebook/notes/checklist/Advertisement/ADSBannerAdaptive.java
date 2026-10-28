package com.notebook.notes.checklist.Advertisement;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;


public class ADSBannerAdaptive {

    @SuppressLint("MissingPermission")
    public static void loadAdMobBanner(String adUint, FrameLayout adContainerView, ShimmerFrameLayout shimmerFrameLayout, Activity activity) {


        if (TextUtils.isEmpty(adUint)) {
            if (shimmerFrameLayout.isShimmerStarted()) {
                shimmerFrameLayout.stopShimmer();
            }
            shimmerFrameLayout.setVisibility(View.GONE);
            return;
        }


        //BannerAd
        AdView admobManagerAdView = new AdView(activity);
        admobManagerAdView.setAdUnitId(adUint);
        adContainerView.addView(admobManagerAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        AdSize adSize = getAdaptiveAdSize(activity);
        admobManagerAdView.setAdSize(adSize);
        admobManagerAdView.loadAd(adRequest);
        admobManagerAdView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                ADSUtilitis.trackScreen(activity, "Banner_Load");
                admobManagerAdView.setOnPaidEventListener(new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(AdValue adValue) {
                        ADSUtilitis.logAdRevenue(activity, adValue);
                    }
                });
                if (shimmerFrameLayout.isShimmerStarted()) {
                    shimmerFrameLayout.stopShimmer();
                }
                shimmerFrameLayout.setVisibility(View.GONE);
                adContainerView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                ADSUtilitis.trackScreen(activity, "Banner_Fail");
                if (shimmerFrameLayout.isShimmerStarted()) {
                    shimmerFrameLayout.stopShimmer();
                }
                shimmerFrameLayout.setVisibility(View.GONE);
                adContainerView.setVisibility(View.VISIBLE);

            }
        });
    }

    public static AdSize getAdaptiveAdSize(Activity context) {
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = outMetrics.density;
        int adWidth = (int) (outMetrics.widthPixels / density);
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, adWidth);
    }

}
