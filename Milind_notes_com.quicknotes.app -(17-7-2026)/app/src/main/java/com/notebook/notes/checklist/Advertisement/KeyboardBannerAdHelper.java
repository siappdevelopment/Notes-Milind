package com.notebook.notes.checklist.Advertisement;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;

import com.notebook.notes.checklist.R;


public final class KeyboardBannerAdHelper {

    private static final int SHOW_BANNER_DELAY_MS = 250;
    private static final float KEYBOARD_HEIGHT_RATIO = 0.15f;

    private KeyboardBannerAdHelper() {
    }

    public static void attach(Activity activity) {
        attach(activity, R.id.banner);
    }

    public static void attach(Activity activity, int bannerContainerId) {
        if (activity == null || !ADSMainClass.getOtherAdsShow()) {
            return;
        }
        View bannerContainer = activity.findViewById(bannerContainerId);
        View contentView = activity.findViewById(android.R.id.content);
        if (bannerContainer == null || contentView == null) {
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable showBannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!ADSMainClass.getOtherAdsShow()) {
                    bannerContainer.setVisibility(View.GONE);
                    return;
                }
                if (!isKeyboardOpen(contentView) && bannerContainer.getVisibility() != View.VISIBLE) {
                    bannerContainer.setVisibility(View.VISIBLE);
                }
            }
        };

        contentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!ADSMainClass.getOtherAdsShow()) {
                    handler.removeCallbacks(showBannerRunnable);
                    bannerContainer.setVisibility(View.GONE);
                    return;
                }

                if (isKeyboardOpen(contentView)) {
                    handler.removeCallbacks(showBannerRunnable);
                    if (bannerContainer.getVisibility() != View.GONE) {
                        bannerContainer.setVisibility(View.GONE);
                    }
                    return;
                }

                if (bannerContainer.getVisibility() == View.VISIBLE) {
                    return;
                }

                handler.removeCallbacks(showBannerRunnable);
                handler.postDelayed(showBannerRunnable, SHOW_BANNER_DELAY_MS);
            }
        });
    }

    private static boolean isKeyboardOpen(View contentView) {
        Rect visibleFrame = new Rect();
        contentView.getWindowVisibleDisplayFrame(visibleFrame);
        int screenHeight = contentView.getRootView().getHeight();
        if (screenHeight <= 0) {
            return false;
        }
        int keyboardHeight = screenHeight - visibleFrame.bottom;
        return keyboardHeight > screenHeight * KEYBOARD_HEIGHT_RATIO;
    }
}
