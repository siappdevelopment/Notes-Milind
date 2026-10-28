package com.notebook.notes.checklist.Advertisement;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;



public class ADSAppBackground {

    private static final String TAG = "ADSAppBackground";
    private static boolean isLoading = false;

    public static boolean isAppOpenAdReady() {
        return ADSAppStarting.AppStartingAd != null && !ADSAppStarting.ADSIsShowing;
    }

    public static void preloadAppOpenAd(Context context) {
        if (ADSMainClass.getAds_Free()) {
            return;
        }
        if (!ADSMainClass.isAppOpenPreLoad()) {
            return;
        }
        if (!ADSAppManage.canShowAppOpenAd()) {
            return;
        }
        if (isAppOpenAdReady() || isLoading) {
            return;
        }

        String appOpenId = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);
        if (appOpenId == null || appOpenId.isEmpty()) {
            return;
        }

        isLoading = true;
        try {
            Bundle bundle = new Bundle();
            AdRequest adRequest = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, bundle)
                    .build();

            AppOpenAd.load(context, appOpenId, adRequest, new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull AppOpenAd ad) {
                    isLoading = false;
                    ad.setOnPaidEventListener(adValue -> ADSUtilitis.logAdRevenue(context, adValue));
                    ADSAppStarting.AppStartingAd = ad;
                    ADSAppStarting.ADSIsShowing = false;
                    ADSUtilitis.trackScreen(context, "App_AppOpen_Preload");
                }

                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    isLoading = false;
                    ADSAppStarting.AppStartingAd = null;
                    ADSAppStarting.ADSIsShowing = false;
                    ADSUtilitis.trackScreen(context, "App_AppOpen_Preload_Fail");
                }
            });
        } catch (Exception e) {
            isLoading = false;
            Log.e(TAG, "preloadAppOpenAd", e);
        }
    }

    private static void preloadAfterShow(Context context) {
        if (ADSAppManage.canShowAppOpenAd()) {
            preloadAppOpenAd(context);
        }
    }

    public static void showPreloadedAppOpen(Activity activity) {
        if (ADSMainClass.getAds_Free() || ADSAppStarting.ADSIsShowing) {
            ADSUtilitis.MassageBoxFullDismiss();
            return;
        }

        if (ADSAppManage.isAppOpenAdCurrentlyBlocked()) {
            ADSUtilitis.MassageBoxFullDismiss();
            return;
        }

        if (!ADSAppManage.canShowAppOpenAd()) {
            ADSUtilitis.MassageBoxFullDismiss();
            return;
        }

        if (!isAppOpenAdReady()) {
            ADSUtilitis.MassageBoxFullDismiss();
            return;
        }

        ADSUtilitis.MassageBoxFullDismiss();
        ADSUtilitis.trackScreen(activity, "App_AppOpen_Show");

        ADSAppStarting.AppStartingAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                ADSAppStarting.ADSIsShowing = false;
                ADSAppStarting.AppStartingAd = null;
                preloadAfterShow(activity.getApplicationContext());
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                ADSAppStarting.AppStartingAd = null;
                ADSAppStarting.ADSIsShowing = false;
                preloadAfterShow(activity.getApplicationContext());
            }

            @Override
            public void onAdShowedFullScreenContent() {
                ADSAppStarting.ADSIsShowing = true;
                ADSAppManage.incrementAdCount();
            }
        });

        ADSAppStarting.AppStartingAd.show(activity);
    }

    public static void showPreloadedAppOpenForSplash(Activity activity, ADSAppStarting.OnFinishAds onFinishAds) {
        if (ADSMainClass.isAppOpenLoad()) {
            ADSUtilitis.MassageBoxFullDismiss();
        }

        if (!isAppOpenAdReady()) {
            onFinishAds.onFinishAds(true);
            return;
        }

        ADSUtilitis.trackScreen(activity, "Splash_AppOpen_Show");

        ADSAppStarting.AppStartingAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                ADSAppStarting.ADSIsShowing = false;
                ADSAppStarting.AppStartingAd = null;
                preloadAfterShow(activity.getApplicationContext());
                onFinishAds.onFinishAds(true);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                ADSAppStarting.AppStartingAd = null;
                ADSAppStarting.ADSIsShowing = false;
                preloadAfterShow(activity.getApplicationContext());
                onFinishAds.onFinishAds(true);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                ADSAppStarting.ADSIsShowing = true;
            }
        });

        ADSAppStarting.AppStartingAd.show(activity);
    }
}
