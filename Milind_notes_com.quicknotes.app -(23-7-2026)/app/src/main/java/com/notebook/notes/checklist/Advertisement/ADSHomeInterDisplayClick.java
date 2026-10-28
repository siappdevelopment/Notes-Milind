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

/**
 * Home fragment-change interstitial. Uses {@link ADSMainClass#getHomeAdsClick()} and
 * {@link ADSMainClass#getHomeInterAdsShow()} — fully separate from {@link ADSInterDisplayClick}.
 */
public class ADSHomeInterDisplayClick {

    public static int HomeADSClick = 0;
    public static Boolean ADSDisplayCheck = false;
    public static int ArrayIndexInterstitialId = 0;

    public static OnFinishAds onFinishAds;
    public static com.facebook.ads.InterstitialAd ADSFBInterstitial;

    private static InterstitialAd ADSAdmobmInterstitial;
    public static boolean LoadingCheck = true;


    public static void ADSInterstitialShowing(Activity context, String adsId, OnFinishAds onFinishAd, boolean... doShowAds) {

        onFinishAds = onFinishAd;
        if (ADSMainClass.getAds_Free() || !ADSMainClass.getHomeInterAdsShow()) {
            onFinishAds.onFinishAds(true);
            return;
        }
        Log.e("Milind", "ADSInterstitialShowing: "+ADSMainClass.getHomeAdsClick() );
        Log.e("Milind", "ADSInterstitialShowing: count "+ADSHomeInterDisplayClick.HomeADSClick );
        if (ADSHomeInterDisplayClick.HomeADSClick == ADSMainClass.getHomeAdsClick()) {
            ADSHomeInterDisplayClick.HomeADSClick = 0;
            ADSHomeInterDisplayClick.ArrayIndexInterstitialId = 0;

            ADSMedDisplay(context, adsId);
        } else {
            onFinishAds.onFinishAds(true);
            ADSHomeInterDisplayClick.HomeADSClick++;
        }
    }

    public static void ADSMedDisplay(Activity context, String adsId) {
        if (ADSMainClass.getInterAdsLoadType().equals("Load")) {
            if (LoadingCheck) {
                LoadingCheck = false;
                AdmobInterstitialAd(context, adsId);
            }
            return;
        }


        if (ADSAdmobmInterstitial != null) {

            AdsDisplayCheck(false);
            onAdsLoadAdListener(context);
            ADSAdmobmInterstitial.show(context);

        } else {
            onFinishAds.onFinishAds(true);
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

    public static void AdmobInterstitialAd(Activity context, String AdsID) {

        if (ADSMainClass.getInterAdsLoadType().equals("Load")) {
            ADSUtilitis.MassageBoxFull(context);
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        ADSAdmobmInterstitial = null;
        InterstitialAd.load(context, AdsID.trim(), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        ADSUtilitis.trackScreen(context, "Home_Inter_Load");
                        interstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(AdValue adValue) {
                                ADSUtilitis.logAdRevenue(context, adValue);
                            }
                        });

                        LoadingCheck = true;

                        ADSAdmobmInterstitial = interstitialAd;
                        if (ADSMainClass.getInterAdsLoadType().equals("Load")) {
                            ADSUtilitis.MassageBoxFullDismiss();

                            ADSAdmobmInterstitial.show((Activity) context);
                            ADSHomeInterDisplayClick.AdsDisplayCheck(false);
                            onAdsLoadAdListener(context);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        ADSUtilitis.trackScreen(context, "Home_Inter_Fail");
                        ADSUtilitis.MassageBoxFullDismiss();
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
