package com.notebook.notes.checklist.Advertisement;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.notebook.notes.checklist.util.Utils;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class ADSAppManage extends Application  implements Configuration.Provider{

    private static ADSAppManage EMIControllerOurInstance;
    public static boolean FastStart = false;
    public static boolean isActivityChecked = false;
    public static boolean AppStartingScreenOpen = false;
    private int NumStarted = 0;
    public static boolean dialogboolean = true;
    public static long appOpenBlockUntil = 0;
    public static boolean isAppOpenBlocked = false;

    public static void blockAppOpenAd(long durationMs) {
        appOpenBlockUntil = Math.max(appOpenBlockUntil, System.currentTimeMillis() + durationMs);
    }

    private static boolean shouldSkipAppOpenAd() {
        if (isAppOpenBlocked) {
            isAppOpenBlocked = false;
            return true;
        }
        return System.currentTimeMillis() < appOpenBlockUntil;
    }

    public static boolean isAppOpenAdCurrentlyBlocked() {
        return isAppOpenBlocked || System.currentTimeMillis() < appOpenBlockUntil;
    }
    private static boolean mobileAdsInitialized = false;

    public static boolean isMobileAdsInitialized() {
        return mobileAdsInitialized;
    }

    public static void initializeMobileAdsIfNeeded(Context context, @Nullable Runnable onComplete) {
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable complete = () -> {
            if (completed.compareAndSet(false, true) && onComplete != null) {
                onComplete.run();
            }
        };

        if (mobileAdsInitialized) {
            complete.run();
            return;
        }

        // Safe point: without WebView the ad SDK never initializes, so never wait for it.
        if (!AdSdkInitHelper.isWebViewAvailable(context)) {
            complete.run();
            return;
        }

        // Safe point: offline the init callback may never arrive, so start it without waiting.
        boolean waitForInit = ADSUtilitis.IsNetworkConnected(context);

        try {
            List<String> testDeviceIds = Arrays.asList("");
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build();
            MobileAds.setRequestConfiguration(configuration);
            MobileAds.initialize(context, initializationStatus -> {
                mobileAdsInitialized = true;
                preloadAppOpenAdIfNeeded();
                complete.run();
            });
        } catch (Throwable t) {
            // Safe point: init threw, so let the caller continue instead of waiting.
//            Log.w("ADSAppManage", "Ad SDK init failed", t);
            complete.run();
            return;
        }

        if (!waitForInit) {
            complete.run();
        }
    }

    public static ADSAppManage getApp() {
        return EMIControllerOurInstance;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        EMIControllerOurInstance = this;
        initializeAdMob();
        ADSMainClass.initAppContext(this);
        SetADSAppStarting();
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(this, getWorkManagerConfiguration());
        }
        try {
            ADSMainClass.scheduleInstallDayWorker(this);
        } catch (Exception e) {
            // Timber.e(e, "Failed to schedule InstallDayWorker")
        }
    }

    private void initializeAdMob() {
        AdSdkInitHelper.runIfWebViewAvailable(this, () -> {
            List<String> testDeviceIds = Arrays.asList("");
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build();
            MobileAds.setRequestConfiguration(configuration);
            MobileAds.initialize(this, initializationStatus -> {
                preloadAppOpenAdIfNeeded();
            });
        });
    }

    public static void preloadAppOpenAdIfNeeded() {
        if (EMIControllerOurInstance == null) {
            return;
        }
        if (ADSMainClass.getAds_Free()) {
            return;
        }
        if (!ADSMainClass.isAppOpenPreLoad()) {
            return;
        }
        if (!canShowAppOpenAd()) {
            return;
        }
        String appOpenId = ADSMainClass.getStringValue(ADSMainClass.APP_OPEN_ID);
        if (appOpenId == null || appOpenId.isEmpty()) {
            return;
        }
        ADSAppBackground.preloadAppOpenAd(EMIControllerOurInstance);
    }

    public static void preloadSplashAdsIfNeeded(Activity activity) {
        if (activity == null || ADSMainClass.getAds_Free()) {
            return;
        }

        String splashAdType = ADSMainClass.getSplashADType();
        if (splashAdType == null || splashAdType.isEmpty()) {
            return;
        }

        if (splashAdType.equalsIgnoreCase("Appopen") && ADSMainClass.isAppOpenPreLoad()) {
            preloadAppOpenAdIfNeeded();
        }

        String interId = ADSMainClass.getStringValue(ADSMainClass.INTER_FIRST_TIME);
        if (interId != null && !interId.isEmpty()
                && ADSMainClass.isSplashInterPreLoad()
                && (splashAdType.equalsIgnoreCase("Inter")
                || splashAdType.equalsIgnoreCase("Interstitial")
                || splashAdType.equalsIgnoreCase("Appopen"))) {
            ADSAppStarting.preloadSplashInterstitialAd(activity, interId);
        }

        if (interId != null && !interId.isEmpty() && ADSMainClass.isInterPreLoad()) {
            ADSInterDisplay.preloadInterstitialAd(activity, interId);
            ADSInterDisplayClick.preloadInterstitialAd(activity, interId);
        }

        if (ADSMainClass.getBottomSplashShow()) {
            if ("native".equalsIgnoreCase(ADSMainClass.getSplashAdsType()) && ADSMainClass.isNativePreLoad()) {
                String nativeId = ADSMainClass.getStringValue(ADSMainClass.splash_native_id);
                if (nativeId != null && !nativeId.isEmpty()) {
                    ADSNativeDisplay.preloadNativeAd(activity, nativeId);
                }
            } else if (!"native".equalsIgnoreCase(ADSMainClass.getSplashAdsType()) && ADSMainClass.isBannerPreLoad()) {
                String bannerId = ADSMainClass.getStringValue(ADSMainClass.splash_banner_id);
                if (bannerId != null && !bannerId.isEmpty()) {
                    ADSBannerSmall.preloadAdMobBanner(bannerId, activity);
                }
            }
        }
    }

    private void SetADSAppStarting() {

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                isActivityChecked = true;
                NumStarted++;
                String className = activity.getClass().getSimpleName();
                if (className.equals("LanguageActivity") ||
                        className.equals("CallEndActivity") ||
                        className.equals("SplashActivity") ||
                        className.equals("AlarmReminderActivity") ||
                        className.equals("MainReminderActivity") ||
                        className.equals("OverlayPermissionActivity") ||
                        className.equals("Intro1Activity") ||
                        className.equals("Intro2Activity") ||
                        className.equals("Intro3Activity") ||
                        className.equals("PermissionActivity") ||
                        className.equals("DefaultAppActivity") ||
                        className.equals("LauncherHomeActivity")
                ) {
                    return;
                }
                if (AppStartingScreenOpen) {
                    preloadAppOpenAdIfNeeded();
                }
                if (shouldSkipAppOpenAd()) {
                    return;
                }
                if (!ADSMainClass.getComingSoon()) {
                    if (AppStartingScreenOpen) {
                        if (NumStarted == 1) {
                            if (!FastStart) {
                                if (ADSUtilitis.IsNetworkConnected(activity)) {
                                    if (!ADSMainClass.getAppOpenBackgroundShow()) {
                                        return;
                                    }
                                    if (!canShowAppOpenAd()) {
                                        return;
                                    }
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (shouldSkipAppOpenAd()) {
                                                return;
                                            }
                                            if (ADSAppBackground.isAppOpenAdReady()) {
                                                ADSAppBackground.showPreloadedAppOpen(activity);
                                            } else {
                                                preloadAppOpenAdIfNeeded();
                                            }
                                        }
                                    }, 100);
                                }
                            } else {
                                FastStart = false;
                            }
//                            }
                        }
                    }
                }
//                NumStarted++;

            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {

                NumStarted--;
                if (NumStarted == 0) {
                    preloadAppOpenAdIfNeeded();
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });

    }


    public static boolean canShowAppOpenAd() {
        if (EMIControllerOurInstance == null) return false;

        int limit = ADSMainClass.getAppOpenAdDailyLimit();
        if (limit <= 0) {
            return false;
        }

        android.content.SharedPreferences prefs = EMIControllerOurInstance.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE);
        String todayString = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_ad_date", "");

        if (!todayString.equals(savedDate)) {
            return true;
        }

        int count = prefs.getInt("ad_count_today", 0);
        return count < limit;
    }

    public static void incrementAdCount() {
        if (EMIControllerOurInstance == null) return;
        android.content.SharedPreferences prefs = EMIControllerOurInstance.getSharedPreferences("AdPrefs", Context.MODE_PRIVATE);
        String todayString = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date());
        String savedDate = prefs.getString("last_ad_date", "");
        int count = prefs.getInt("ad_count_today", 0);

        if (!todayString.equals(savedDate)) {
            count = 0;
            prefs.edit().putString("last_ad_date", todayString).apply();
        }
        prefs.edit().putInt("ad_count_today", count + 1).apply();
    }



    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        EMIControllerOurInstance = this;
        ADSMainClass.initAppContext(this);
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build();
    }


    @Override
    public Intent registerReceiver(@Nullable BroadcastReceiver receiver, IntentFilter filter) {
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            return super.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            return super.registerReceiver(receiver, filter);
        }
    }

}
