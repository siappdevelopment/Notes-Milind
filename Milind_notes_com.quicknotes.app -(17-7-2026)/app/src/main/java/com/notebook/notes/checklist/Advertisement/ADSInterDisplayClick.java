package com.notebook.notes.checklist.Advertisement;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class ADSInterDisplayClick {

    public static int ADSClick = 0;
    public static int BackADClick = 0;
    public static int BottomNavADClick = 0;
    public static int LauncherSwipeADClick = 0;
    public static Boolean ADSDisplayCheck = false;
    public static int ArrayIndexInterstitialId = 0;

    public static OnFinishAds onFinishAds;
    public static com.facebook.ads.InterstitialAd ADSFBInterstitial;

    private static InterstitialAd ADSAdmobmInterstitial;
    public static boolean LoadingCheck = true;


    public static void ADSInterstitialShowing(Activity context, String adsId, OnFinishAds onFinishAd) {

        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getInterAdsShow() ) {
            onFinishAds.onFinishAds(true);
            return;
        }

        if (ADSInterDisplayClick.ADSClick == ADSMainClass.getAdsClick()) {
            ADSInterDisplayClick.ADSClick = 0;
            ADSInterDisplayClick.ArrayIndexInterstitialId = 0;

//            if (ADSMainClass.getAdsDisplayType().equals("admob")) {
                ADSMedDisplay(context, adsId);
//            } else {
//                onFinishAds.onFinishAds(true);
//            }
        } else {
            onFinishAds.onFinishAds(true);
            ADSInterDisplayClick.ADSClick++;
        }
    }


    public static void ADSBackDisplayInterstitial(Activity context, String adsId, OnFinishAds onFinishAd) {
        onFinishAds = onFinishAd;
        Log.e("akfdamskmdkasmdkamdm", "ADSBackDisplayInterstitial: ");
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getInterAdsOnBackShow() ) {
            onFinishAds.onFinishAds(true);
            return;
        }

        if (ADSInterDisplayClick.BackADClick == ADSMainClass.getAdsBackClick()) {
            ADSInterDisplayClick.BackADClick = 0;
            ADSInterDisplayClick.ArrayIndexInterstitialId = 0;

//            if (ADSMainClass.getAdsDisplayType().equals("admob")) {
            ADSMedDisplay(context, adsId);


        } else {
            onFinishAds.onFinishAds(true);
            ADSInterDisplayClick.BackADClick++;
        }
    }

    public static void ADSBottomNavDisplayInterstitial(Activity context, String adsId, OnFinishAds onFinishAd) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free()
                || !ADSMainClass.getHomeInterAdsShow()
                || !ADSMainClass.getHomeBottomNavInterShow()
                || !ADSMainClass.getInterAdsShow()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        if (BottomNavADClick == ADSMainClass.getHomeAdsClick()) {
            BottomNavADClick = 0;
            ADSAppManage.isActivityChecked = true;
            ADSMedDisplay(context, adsId);
        } else {
            onFinishAds.onFinishAds(true);
            BottomNavADClick++;
        }
    }

    public static void ADSLauncherSwipeDisplayInterstitial(Activity context, String adsId, OnFinishAds onFinishAd) {
        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free()
                || !ADSMainClass.getInterAdsShow()
                || !ADSMainClass.getLanguageInterAdsShow()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        if (LauncherSwipeADClick == ADSMainClass.getAdsClick()) {
            LauncherSwipeADClick = 0;
            ADSAppManage.isActivityChecked = true;
            ADSMedDisplay(context, adsId);
        } else {
            onFinishAds.onFinishAds(true);
            LauncherSwipeADClick++;
        }
    }

    public static void ADSMedDisplay(Activity context, String adsId) {
        if (ADSMainClass.isInterLoad()) {
            if (LoadingCheck) {
                LoadingCheck = false;
                AdmobInterstitialAd(context,adsId);
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
                        ADSUtilitis.trackScreen(context, "Inter_Click_Preload");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        LoadingCheck = true;
                        ADSUtilitis.trackScreen(context, "Inter_Click_Preload_Fail");
                    }
                });
    }

    public static void AdmobInterstitialAd(Activity context, String AdsID) {

        if (ADSMainClass.isInterLoad()) {
//            if (context instanceof FullMActivity) {
////                return;
//            } else {
                ADSUtilitis.MassageBoxFull(context);
//            }
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        ADSAdmobmInterstitial = null;
        InterstitialAd.load(context, AdsID.trim(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        ADSUtilitis.trackScreen(context, "Inter_Load");
                        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                ADSUtilitis.logAdRevenue(context, adValue);
                            }
                        });

                        LoadingCheck = true;

                        ADSAdmobmInterstitial = interstitialAd;
                        if (ADSMainClass.isInterLoad()) {
                            ADSUtilitis.MassageBoxFullDismiss();

                            if (ADSAppManage.isActivityChecked) {
                                ADSAppManage.isActivityChecked = false;

                                ADSAdmobmInterstitial.show((Activity) context);
                                ADSInterDisplayClick.AdsDisplayCheck(false);
                            }
                            onAdsLoadAdListener(context);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        ADSUtilitis.trackScreen(context, "Inter_Fail");
                        ADSUtilitis.MassageBoxFullDismiss();

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
