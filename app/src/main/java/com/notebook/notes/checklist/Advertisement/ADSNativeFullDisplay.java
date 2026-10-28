package com.notebook.notes.checklist.Advertisement;


import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.nativead.NativeAd;

public class ADSNativeFullDisplay {

    public static NativeAd AdmobNativeAd;


    public interface PreloadCallback {
        void onAdLoaded();

        void onAdFailed();
    }

    public static void preloadNativeAd(final Context context, String adsId, PreloadCallback callback) {
        if (ADSMainClass.getAds_Free()) {
            if (callback != null) callback.onAdFailed();
            return;
        }
        if (adsId == null || adsId.isEmpty()) {
            if (callback != null) callback.onAdFailed();
            return;
        }

//        Log.d("FCM_AD", "Starting preload for Native Ad. ID: " + adsId);
        AdLoader adLoader = new AdLoader.Builder(context, adsId)
                .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(@NonNull NativeAd nativeAd) {
                        if (AdmobNativeAd != null) {
                            AdmobNativeAd.destroy();
                        }
                        AdmobNativeAd = nativeAd;
                        AdmobNativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                ADSUtilitis.logAdRevenue(context, adValue);
                            }
                        });
                        ADSUtilitis.trackScreen(context, "FullNative_Load");
                        if (callback != null) callback.onAdLoaded();
                    }
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
//                        Log.e("FCM_AD", "Native Ad preloading failed: " + adError.getMessage() + " (Code: " + adError.getCode() + ")");
                        ADSUtilitis.trackScreen(context, "FullNative_Fail");
                        if (callback != null) callback.onAdFailed();
                        super.onAdFailedToLoad(adError);
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }


}
