package com.notebook.notes.checklist.Advertisement;


import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class ADSInterDisplay {

//    public static int ADSClick = 0;
//    public static int BackADClick = 0;
    public static Boolean ADSDisplayCheck = false;
    public static int ArrayIndexInterstitialId = 0;

    public static OnFinishAds onFinishAds;
    public static com.facebook.ads.InterstitialAd ADSFBInterstitial;

    private static InterstitialAd ADSAdmobmInterstitial;
    public static boolean LoadingCheck = true;

    public static boolean hasPreloadedInterstitial() {
        return ADSAdmobmInterstitial != null;
    }

    @Nullable
    public static InterstitialAd takePreloadedInterstitial() {
        InterstitialAd preloaded = ADSAdmobmInterstitial;
        ADSAdmobmInterstitial = null;
        LoadingCheck = true;
        return preloaded;
    }

    public static void ADSInterstitialShowing(Activity context, String adsId, OnFinishAds onFinishAd) {

        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getInterAdsShow() ) {
            onFinishAds.onFinishAds(true);
            return;
        }

//        if (ADSInterDisplay.ADSClick == ADSMainClass.getAdsClick()) {
//            ADSInterDisplay.ADSClick = 0;
//            ADSInterDisplay.ArrayIndexInterstitialId = 0;

//            if (ADSMainClass.getAdsDisplayType().equals("admob")) {
                ADSMedDisplay(context, adsId);
//            } else {
//                onFinishAds.onFinishAds(true);
//            }
//        } else {
//            onFinishAds.onFinishAds(true);
//            ADSInterDisplay.ADSClick++;
//        }
    }


//    public static void ADSBackDisplayInterstitial(Activity context, String adsId, OnFinishAds onFinishAd, boolean... doShowAds) {
//        onFinishAds = onFinishAd;
//        if (ADSMainClass.getAds_Free()) {
//            onFinishAds.onFinishAds(true);
//            return;
//        }
//
//        if (ADSInterDisplay.BackADClick == ADSMainClass.getAdsBackClick()) {
//            ADSInterDisplay.BackADClick = 0;
//            ADSInterDisplay.ArrayIndexInterstitialId = 0;
//
////            if (ADSMainClass.getAdsDisplayType().equals("admob")) {
//            ADSMedDisplay(context, adsId);
//
//
//        } else {
//            onFinishAds.onFinishAds(true);
//            ADSInterDisplay.BackADClick++;
//        }
//    }

    public static void ADSMedDisplay(Activity context, String adsId) {
        if (ADSMainClass.isInterLoad()) {
            if (ADSAdmobmInterstitial != null) {
                AdsDisplayCheck(false);
                onAdsLoadAdListener(context);
                ADSAdmobmInterstitial.show(context);
                return;
            }
            if (LoadingCheck) {
                LoadingCheck = false;
                AdmobInterstitialAd(context, adsId);
            } else if (onFinishAds != null) {
                onFinishAds.onFinishAds(true);
            }
            return;
        }



        if (ADSAdmobmInterstitial != null) {

            AdsDisplayCheck(false);
            onAdsLoadAdListener(context);
            ADSAdmobmInterstitial.show(context);

//        } else if (ADSFBInterstitial != null && ADSFBInterstitial.isAdLoaded()) {
//
//            AdsDisplayCheck(false);
//            ADSFBInterstitial.show();

        } else {
//            if (ADSMainClass.getAdsInterstitialType().equals("none")) {
//                onFinishAds.onFinishAds(true);
//            } else {
                onFinishAds.onFinishAds(true);
//            }
        }


        if (LoadingCheck) {
            LoadingCheck = false;
            AdmobInterstitialAd(context, adsId);
        }
    }

    public static void AdsDisplayCheck(Boolean aBoolean) {

        if (aBoolean) {
            ADSAppStarting.ADSIsShowing = false;
            ADSAppManage.FastStart = false;
            ADSDisplayCheck = true;
        } else {
            ADSAppStarting.ADSIsShowing = true;
            ADSDisplayCheck = false;
            ADSAppManage.FastStart = true;
        }

    }

    public static void preloadInterstitialAd(Activity context, String adsId) {
        if (adsId == null || adsId.isEmpty() || ADSMainClass.getAds_Free()) {
            return;
        }
        if (!LoadingCheck) {
            return;
        }

        LoadingCheck = false;
        AdRequest adRequest = new AdRequest.Builder().build();
        ADSAdmobmInterstitial = null;
        InterstitialAd.load(context, adsId.trim(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.setOnPaidEventListener(adValue ->
                                ADSUtilitis.logAdRevenue(context, adValue));
                        LoadingCheck = true;
                        ADSAdmobmInterstitial = interstitialAd;
                        ADSUtilitis.trackScreen(context, "Inter_Preload");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        LoadingCheck = true;
                        ADSUtilitis.trackScreen(context, "Inter_Preload_Fail");
                    }
                });
    }

    public static void AdmobInterstitialAd(Activity context, String AdsID) {

        if (ADSMainClass.isInterLoad()) {
            ADSUtilitis.MassageBoxFull(context);
        }
        AdRequest adRequest = new AdRequest.Builder().build();

        ADSAdmobmInterstitial = null;
        InterstitialAd.load(context, AdsID.trim(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                ADSUtilitis.logAdRevenue(context, adValue);
                            }
                        });

                        LoadingCheck = true;

                        ADSAdmobmInterstitial = interstitialAd;
                        ADSUtilitis.trackScreen(context, "Inter_Load");
                        if (ADSMainClass.isInterLoad()) {
                            ADSUtilitis.MassageBoxFullDismiss();

                            if (ADSAppManage.isActivityChecked) {
                                ADSAppManage.isActivityChecked = false;

                                ADSAdmobmInterstitial.show((Activity) context);
                                ADSInterDisplay.AdsDisplayCheck(false);
                            }

                        }
                        onAdsLoadAdListener(context);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        ADSUtilitis.trackScreen(context, "Inter_Fail");
                        ADSUtilitis.MassageBoxFullDismiss();
                        LoadingCheck = true;
                        if (onFinishAds != null) {
                            onFinishAds.onFinishAds(true);
                        }
                    }
                });
    }

    public static void onAdsLoadAdListener(Context context) {
        ADSAdmobmInterstitial.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                AdsDisplayCheck(true);
                onFinishAds.onFinishAds(true);
                if (context instanceof Activity && ADSMainClass.isInterPreLoad()) {
                    String interId = ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME);
                    if (interId != null && !interId.isEmpty()) {
                        preloadInterstitialAd((Activity) context, interId);
                    }
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // ADSUtilitis.trackScreen(context, "Inter_FailedToShow");
                AdsDisplayCheck(true);
                onFinishAds.onFinishAds(true);
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
            }
        });
    }

    public interface OnFinishAds {
        void onFinishAds(boolean b);

    }
}
