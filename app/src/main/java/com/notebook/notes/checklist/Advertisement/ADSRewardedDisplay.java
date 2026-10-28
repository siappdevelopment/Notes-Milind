package com.notebook.notes.checklist.Advertisement;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Rewarded ads manager — mirrors interstitial preload / show / reload patterns
 * with lifecycle-safe state, exponential retry, and callback-based reward delivery.
 */
public class ADSRewardedDisplay {

    private static final String TAG = "ADSRewardedDisplay";

    private static final long RETRY_BASE_MS = 5_000L;
    private static final long RETRY_MAX_MS = 60_000L;

    public enum State {
        IDLE,
        LOADING,
        LOADED,
        SHOWING,
        FAILED,
        RELOADING
    }

    private static RewardedAd rewardedAd;
    private static volatile State state = State.IDLE;
    private static boolean isLoading = false;
    private static boolean isShowing = false;
    private static int retryAttempt = 0;
    private static String lastAdsId = "";

    private static OnRewardedAdsCallback pendingCallback;
    private static boolean showWhenLoaded = false;
    @Nullable
    private static Activity pendingShowActivity;

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static Runnable retryRunnable;

    public static boolean isRewardedReady() {
        return rewardedAd != null && state == State.LOADED && !isShowing;
    }

    public static boolean isRewardedLoading() {
        return isLoading || state == State.LOADING || state == State.RELOADING;
    }

    public static State getState() {
        return state;
    }

    /**
     * Preload using Remote Config / AdsConfig ad unit id.
     */
    public static void preload(Context context) {
        preload(context, resolveAdsId());
    }

    public static void preload(Context context, String adsId) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();

        if (!canLoadRewarded()) {
            Log.d(TAG, "Rewarded preload skipped (disabled / premium)");
            return;
        }

        if (adsId == null || adsId.trim().isEmpty()) {
            Log.d(TAG, "Rewarded preload skipped (empty ad unit)");
            setState(State.FAILED);
            return;
        }

        if (isRewardedReady() || isLoading) {
            return;
        }

        if (!ADSUtilitis.IsNetworkConnected(appContext)) {
            Log.d(TAG, "Rewarded Failed (no internet)");
            setState(State.FAILED);
            scheduleRetry(appContext, adsId);
            notifyFailed();
            return;
        }

        lastAdsId = adsId.trim();
        loadRewardedAd(appContext, lastAdsId, false);
    }

    /**
     * Show a preloaded rewarded ad, or load-then-show when load type is {@code Load}.
     */
    public static void show(Activity activity, OnRewardedAdsCallback callback) {
        show(activity, resolveAdsId(), callback);
    }

    public static void show(Activity activity, String adsId, OnRewardedAdsCallback callback) {
        pendingCallback = callback;

        if (activity == null || activity.isFinishing()) {
            notifyNotAvailable();
            return;
        }

        if (ADSMainClass.getAds_Free() || !ADSMainClass.getRewardedAdsShow()) {
            Log.d(TAG, "Rewarded Not Available (premium / RC disabled)");
            notifyNotAvailable();
            return;
        }

        if (adsId == null || adsId.trim().isEmpty()) {
            notifyNotAvailable();
            return;
        }

        lastAdsId = adsId.trim();

        if (ADSMainClass.getRewardedAdsLoadType().equals("Load")) {
            if (isLoading) {
                showWhenLoaded = true;
                pendingShowActivity = activity;
                ADSUtilitis.MassageBoxFull(activity);
                return;
            }
            if (isRewardedReady()) {
                showLoadedAd(activity);
                return;
            }
            showWhenLoaded = true;
            pendingShowActivity = activity;
            ADSUtilitis.MassageBoxFull(activity);
            loadRewardedAd(activity.getApplicationContext(), lastAdsId, true);
            return;
        }

        // preLoad mode — show if ready; otherwise wait for preload onLoad then show
        if (isRewardedReady()) {
            showLoadedAd(activity);
            return;
        }

        showWhenLoaded = true;
        pendingShowActivity = activity;
        ADSUtilitis.MassageBoxFull(activity);
        if (!isLoading) {
            loadRewardedAd(activity.getApplicationContext(), lastAdsId, true);
        }
    }

    public static void destroy() {
        cancelRetry();
        showWhenLoaded = false;
        pendingShowActivity = null;
        pendingCallback = null;
        rewardedAd = null;
        isLoading = false;
        isShowing = false;
        retryAttempt = 0;
        setState(State.IDLE);
        Log.d(TAG, "Rewarded Destroyed");
    }

    private static boolean canLoadRewarded() {
        return !ADSMainClass.getAds_Free() && ADSMainClass.getRewardedAdsShow();
    }

    private static String resolveAdsId() {
        String id = ADSMainClass.getStringValue(ADSMainClass.REWARDED_AD_ID);
        return id != null ? id.trim() : "";
    }

    private static void loadRewardedAd(Context context, String adsId, boolean fromShowFlow) {
        if (isLoading) {
            return;
        }
        if (rewardedAd != null && state == State.LOADED) {
            return;
        }

        isLoading = true;
        setState(fromShowFlow && showWhenLoaded ? State.LOADING : (retryAttempt > 0 ? State.RELOADING : State.LOADING));
        if (retryAttempt > 0) {
            Log.d(TAG, "Reload Started");
        } else {
            Log.d(TAG, "Rewarded Loading");
        }

        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(context, adsId, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                isLoading = false;
                retryAttempt = 0;
                cancelRetry();
                rewardedAd = ad;
                setState(State.LOADED);
                Log.d(TAG, "Rewarded Loaded");
                ADSUtilitis.trackScreen(context, "Rewarded_Load");

                ad.setOnPaidEventListener(new OnPaidEventListener() {
                    @Override
                    public void onPaidEvent(@NonNull AdValue adValue) {
                        ADSUtilitis.logAdRevenue(context, adValue);
                    }
                });

                notifyLoaded();

                if (showWhenLoaded && pendingShowActivity != null) {
                    Activity act = pendingShowActivity;
                    showWhenLoaded = false;
                    pendingShowActivity = null;
                    ADSUtilitis.MassageBoxFullDismiss();
                    if (!act.isFinishing()) {
                        showLoadedAd(act);
                    }
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                isLoading = false;
                rewardedAd = null;
                setState(State.FAILED);
                Log.d(TAG, "Rewarded Failed: " + loadAdError.getMessage());
                ADSUtilitis.trackScreen(context, "Rewarded_Fail");
                ADSUtilitis.MassageBoxFullDismiss();

                if (showWhenLoaded) {
                    showWhenLoaded = false;
                    pendingShowActivity = null;
                    notifyNotAvailable();
                } else {
                    notifyFailed();
                }

                scheduleRetry(context, adsId);
            }
        });
    }

    private static void showLoadedAd(Activity activity) {
        if (rewardedAd == null) {
            notifyNotAvailable();
            return;
        }

        RewardedAd adToShow = rewardedAd;
        rewardedAd = null;
        isShowing = true;
        setState(State.SHOWING);
        AdsDisplayCheck(false);
        Log.d(TAG, "Rewarded Showing");
        ADSUtilitis.trackScreen(activity, "Rewarded_Show");

        adToShow.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded Dismissed");
                ADSUtilitis.trackScreen(activity, "Rewarded_Dismiss");
                finishShow(activity.getApplicationContext());
                notifyDismiss();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.d(TAG, "Rewarded Failed To Show: " + adError.getMessage());
                ADSUtilitis.trackScreen(activity, "Rewarded_FailedToShow");
                finishShow(activity.getApplicationContext());
                notifyFailed();
            }

            @Override
            public void onAdImpression() {
            }

            @Override
            public void onAdShowedFullScreenContent() {
                notifyShow();
            }
        });

        try {
            adToShow.show(activity, rewardItem -> {
                Log.d(TAG, "Reward Earned");
                ADSUtilitis.trackScreen(activity, "Rewarded_Earned");
                notifyRewardEarned();
            });
        } catch (Exception e) {
            Log.e(TAG, "Rewarded show exception", e);
            finishShow(activity.getApplicationContext());
            notifyFailed();
        }
    }

    private static void finishShow(Context context) {
        isShowing = false;
        AdsDisplayCheck(true);
        setState(State.IDLE);
        // Always preload next after consume / dismiss / show failure
        Log.d(TAG, "Reload Started");
        preload(context, lastAdsId.isEmpty() ? resolveAdsId() : lastAdsId);
    }

    private static void scheduleRetry(Context context, String adsId) {
        cancelRetry();
        if (!canLoadRewarded()) {
            return;
        }
        long delay = Math.min(RETRY_BASE_MS * (1L << Math.min(retryAttempt, 4)), RETRY_MAX_MS);
        retryAttempt++;
        Log.d(TAG, "Rewarded retry in " + delay + "ms (attempt " + retryAttempt + ")");
        final Context appContext = context.getApplicationContext();
        final String id = adsId;
        retryRunnable = () -> {
            retryRunnable = null;
            if (!isRewardedReady() && !isLoading && canLoadRewarded()) {
                loadRewardedAd(appContext, id, false);
            }
        };
        mainHandler.postDelayed(retryRunnable, delay);
    }

    private static void cancelRetry() {
        if (retryRunnable != null) {
            mainHandler.removeCallbacks(retryRunnable);
            retryRunnable = null;
        }
    }

    private static void AdsDisplayCheck(Boolean available) {
        if (available) {
            ADSAppStarting.ADSIsShowing = false;
            ADSAppManage.FastStart = false;
        } else {
            ADSAppStarting.ADSIsShowing = true;
            ADSAppManage.FastStart = true;
        }
    }

    private static void setState(State newState) {
        state = newState;
    }

    private static void notifyLoaded() {
        if (pendingCallback != null) {
            try {
                pendingCallback.onLoaded();
            } catch (Exception ignored) {
            }
        }
    }

    private static void notifyFailed() {
        OnRewardedAdsCallback cb = pendingCallback;
        if (!isShowing) {
            pendingCallback = null;
        }
        if (cb != null) {
            try {
                cb.onFailed();
            } catch (Exception ignored) {
            }
        }
    }

    private static void notifyShow() {
        if (pendingCallback != null) {
            try {
                pendingCallback.onShow();
            } catch (Exception ignored) {
            }
        }
    }

    private static void notifyDismiss() {
        OnRewardedAdsCallback cb = pendingCallback;
        pendingCallback = null;
        if (cb != null) {
            try {
                cb.onDismiss();
            } catch (Exception ignored) {
            }
        }
    }

    private static void notifyRewardEarned() {
        if (pendingCallback != null) {
            try {
                pendingCallback.onRewardEarned();
            } catch (Exception ignored) {
            }
        }
    }

    private static void notifyNotAvailable() {
        OnRewardedAdsCallback cb = pendingCallback;
        pendingCallback = null;
        if (cb != null) {
            try {
                cb.onNotAvailable();
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Full callback API. Defaults are no-ops so callers can override only what they need.
     */
    public interface OnRewardedAdsCallback {
        default void onLoaded() {
        }

        default void onFailed() {
        }

        default void onShow() {
        }

        default void onDismiss() {
        }

        void onRewardEarned();

        default void onNotAvailable() {
        }
    }
}
