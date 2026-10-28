package com.notebook.notes.checklist.Advertisement;


import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.facebook.ads.NativeAdLayout;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.notebook.notes.checklist.R;


import java.util.Objects;

public class ADSNativeDisplay {
    public static int ArrayIndex = 0;
    public static boolean FailArrayId = false;
    public static Context contexts;
    public static int NativeByPage = 0;
    //    public static com.facebook.ads.NativeAd FBNativeAdpater;
    public static NativeAdLayout NativeAdLayout;
    public static boolean LoadingCheck = true;
    public static NativeAd AdmobNativeAd;
    public static String AdsDisplayType = "small";

    public static void loadSplashBottomNative(String adsId, final FrameLayout linearLayout,
                                              ShimmerFrameLayout shimmerFrameLayout, final Context context) {
        AdsDisplayType = "small";
        contexts = context;

        if (ADSMainClass.getAds_Free()) {
            hideNativePlaceholder(linearLayout, shimmerFrameLayout);
            return;
        }
        if (adsId == null || adsId.isEmpty()) {
            hideNativePlaceholder(linearLayout, shimmerFrameLayout);
            return;
        }

        if (AdmobNativeAd != null) {
            bindNativeAdToContainer(context, linearLayout, shimmerFrameLayout, AdmobNativeAd);
            AdmobFullNative(context, adsId, null, null, true);
            return;
        }

        showNativeShimmer(shimmerFrameLayout);
        linearLayout.setVisibility(View.GONE);
        loadAndDisplayNativeAd(context, adsId, shimmerFrameLayout, linearLayout);
    }

    private static void loadAndDisplayNativeAd(final Context context, String adsId,
                                               ShimmerFrameLayout shimmerFrameLayout,
                                               final FrameLayout linearLayout) {
        AdLoader.Builder builder = new AdLoader.Builder(context, adsId)
            .forNativeAd(nativeAd -> {
                nativeAd.setOnPaidEventListener(adValue -> ADSUtilitis.logAdRevenue(context, adValue));

                LoadingCheck = true;
                if (AdmobNativeAd != null) {
                    AdmobNativeAd.destroy();
                    AdmobNativeAd = null;
                }
                AdmobNativeAd = nativeAd;
                ADSUtilitis.trackScreen(context, "Native_Load");

                if (!(context instanceof Activity)) {
                    return;
                }
                Activity activity = (Activity) context;
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                bindNativeAdToContainer(context, linearLayout, shimmerFrameLayout, nativeAd);
            });

        builder.withAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                ADSUtilitis.trackScreen(context, "Native_Fail");
                hideNativePlaceholder(linearLayout, shimmerFrameLayout);
                LoadingCheck = true;
                if (AdmobNativeAd != null) {
                    AdmobNativeAd.destroy();
                    AdmobNativeAd = null;
                }
            }

            @Override
            public void onAdClicked() {
                ADSAppManage.FastStart = true;
            }
        }).build().loadAd(new AdRequest.Builder().build());
    }

    private static void bindNativeAdToContainer(Context context, FrameLayout linearLayout,
                                                ShimmerFrameLayout shimmerFrameLayout, NativeAd nativeAd) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.layout_native_ad_small, null);
        hideNativeShimmer(shimmerFrameLayout);
        linearLayout.setVisibility(View.VISIBLE);
        populateUnifiedNativeSmallAdView(nativeAd, adView, context);
        linearLayout.removeAllViews();
        linearLayout.addView(adView);
        ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
    }

    private static void showNativeShimmer(ShimmerFrameLayout shimmerFrameLayout) {
        if (shimmerFrameLayout == null) {
            return;
        }
        shimmerFrameLayout.setVisibility(View.VISIBLE);
        shimmerFrameLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        shimmerFrameLayout.post(shimmerFrameLayout::startShimmer);
    }

    private static void hideNativeShimmer(ShimmerFrameLayout shimmerFrameLayout) {
        if (shimmerFrameLayout == null) {
            return;
        }
        shimmerFrameLayout.setVisibility(View.GONE);
        shimmerFrameLayout.stopShimmer();
    }

    private static void hideNativePlaceholder(FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout) {
        hideNativeShimmer(shimmerFrameLayout);
        if (linearLayout != null) {
            linearLayout.setVisibility(View.GONE);
        }
    }

    public static void preloadNativeAd(Context context, String adsId) {
        if (!ADSMainClass.isNativePreLoad() || adsId == null || adsId.isEmpty()) {
            return;
        }
        if (AdmobNativeAd != null || !LoadingCheck) {
            return;
        }
        LoadingCheck = false;
        AdmobFullNative(context, adsId, null, null, true);
    }

    public static void loadAdmobNativeAdBig(String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, String ads_type, final Context context) {
        AdsDisplayType = ads_type;
        contexts = context;
        ArrayIndex = 0;
        if (ADSMainClass.getAds_Free()) {
//            lnr_view.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.GONE);
            return;
        }
        if (adsId.isEmpty()) {
            linearLayout.setVisibility(View.GONE);
            shimmerFrameLayout.setVisibility(View.GONE);
            return;
        }

        if (NativeByPage == ADSMainClass.getNativeByPage()) {
            NativeByPage = 0;
            if (ads_type.equals("big")) {
                AdmobBigDisplay(context, adsId, linearLayout, shimmerFrameLayout, false);
            } else {
                AdmobSmallDisplay(context, adsId, linearLayout, shimmerFrameLayout, true);
            }
//            }

        } else {
            NativeByPage++;
            shimmerFrameLayout.setVisibility(View.GONE);
            linearLayout.setVisibility(View.GONE);
        }
    }


    public static void AdmobBigDisplay(Context context, String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, Boolean banner_flag) {

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            shimmerFrameLayout.post(new Runnable() {
                @Override
                public void run() {
                    shimmerFrameLayout.startShimmer();
                }
            });
        }
        linearLayout.setVisibility(View.GONE);

        if (ADSMainClass.isNativeLoad()) {

            if (LoadingCheck) {
                LoadingCheck = false;
                AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
            }
            return;
        }

        if (AdmobNativeAd != null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView;
            if (AdsDisplayType.equals("big")) {
                adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
            } else {
                adView = (NativeAdView) inflater.inflate(R.layout.layout_native_ad_small, null);
            }

            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
            }
            linearLayout.setVisibility(View.VISIBLE);
            if (AdsDisplayType.equals("big")) {

                PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false,context);
            } else {
                populateUnifiedNativeSmallAdView(AdmobNativeAd,adView,context);
            }
            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        }
        if (LoadingCheck) {
            LoadingCheck = false;
            AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
        }
    }

    public static void AdmobSmallDisplay(Context context, String adsId, final FrameLayout linearLayout, ShimmerFrameLayout shimmerFrameLayout, Boolean banner_flag) {

        if (shimmerFrameLayout != null) {
            shimmerFrameLayout.setVisibility(View.VISIBLE);
            shimmerFrameLayout.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            shimmerFrameLayout.post(new Runnable() {
                @Override
                public void run() {
                    shimmerFrameLayout.startShimmer();
                }
            });
        }
        linearLayout.setVisibility(View.GONE);

        if (ADSMainClass.isNativeLoad()) {
            AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
            return;
        }

        if (AdmobNativeAd != null) {
            LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.layout_native_ad_small, null);
            if (shimmerFrameLayout != null) {
                shimmerFrameLayout.setVisibility(View.GONE);
                shimmerFrameLayout.stopShimmer();
            }
            linearLayout.setVisibility(View.VISIBLE);
            populateUnifiedNativeSmallAdView(AdmobNativeAd, adView, context);

            linearLayout.removeAllViews();
            linearLayout.addView(adView);
            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
        }
        AdmobFullNative(context, adsId, shimmerFrameLayout, linearLayout, banner_flag);
    }

    public static void AdmobFullNative(final Context context, String adsId, ShimmerFrameLayout shimmerFrameLayout, final FrameLayout linearLayout, boolean banner_flag) {
        AdLoader.Builder builder = new AdLoader.Builder(context, adsId)
            .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                @Override
                public void onNativeAdLoaded(NativeAd nativeAd) {
                    nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(AdValue adValue) {
                            ADSUtilitis.logAdRevenue(context, adValue);
                        }
                    });

                    LoadingCheck = true;
                    if (AdmobNativeAd != null) {
                        AdmobNativeAd = null;
                    }
                    AdmobNativeAd = nativeAd;

                    ADSUtilitis.trackScreen(context, "Native_Load");

                    if (ADSMainClass.isNativeLoad() && linearLayout != null) {
                        if (AdmobNativeAd != null) {

                            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                            NativeAdView adView;
                            if (AdsDisplayType.equals("big")) {
                                adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);
                            } else {
                                adView = (NativeAdView) inflater.inflate(R.layout.layout_native_ad_small, null);
                            }

                            if (shimmerFrameLayout != null) {
                                shimmerFrameLayout.setVisibility(View.GONE);
                                shimmerFrameLayout.stopShimmer();
                            }
                            linearLayout.setVisibility(View.VISIBLE);
//                            PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);

                            if (AdsDisplayType.equals("big")) {

                                PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false, context);
                            } else {
                                populateUnifiedNativeSmallAdView(AdmobNativeAd,adView, context);
                            }

                            linearLayout.removeAllViews();
                            linearLayout.addView(adView);
                            ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
                        }
                    }
                }
            });

        AdLoader adLoader = builder.withAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    Log.d("NativeADSSSSSs", "onAdFailedToLoad: " );
                    ADSUtilitis.trackScreen(context, "Native_Fail");
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout.setVisibility(View.GONE);
                        shimmerFrameLayout.stopShimmer();
                    }
                    if (linearLayout != null) {
                        linearLayout.setVisibility(View.GONE);
                    }
                    if (AdmobNativeAd != null) {
                        AdmobNativeAd = null;
                    }
                }

                @Override
                public void onAdClicked() {
                    ADSAppManage.FastStart = true;
                }
            })
            .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }
    public static boolean isDarkMode(Context context) {
        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;

        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
    public static void PopulateUnifiedFullNativeAdView(NativeAd nativeAd, NativeAdView adView, boolean flag, Context context) {

        com.google.android.gms.ads.nativead.MediaView mediaView = adView.findViewById(R.id.ad_media);

        adView.setMediaView(mediaView);
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

        try {

            TextView headline = (TextView) adView.getHeadlineView();
            TextView body = (TextView) adView.getBodyView();
            TextView cta = (TextView) adView.getCallToActionView();

            TextView price = adView.getPriceView() instanceof TextView
                    ? (TextView) adView.getPriceView() : null;

            TextView store = adView.getStoreView() instanceof TextView
                    ? (TextView) adView.getStoreView() : null;

            TextView advertiser = adView.getAdvertiserView() instanceof TextView
                    ? (TextView) adView.getAdvertiserView() : null;

            int backgroundColor;
            int textColor;
            int buttonColor;
            int buttonTextColor;

            if (isDarkMode(context)) {
                backgroundColor = Color.parseColor(ADSMainClass.getDarkNativeBackgroundColor());
                textColor = Color.parseColor(ADSMainClass.getDarkNativeAllTextColor());
                buttonColor = Color.parseColor(ADSMainClass.getDarkNativeButtonColor());
                buttonTextColor = Color.parseColor(ADSMainClass.getDarkNativeButtonTextColor());
            } else {
                backgroundColor = Color.parseColor(ADSMainClass.getLightNativeBackgroundColor());
                textColor = Color.parseColor(ADSMainClass.getLightNativeAllTextColor());
                buttonColor = Color.parseColor(ADSMainClass.getLightNativeButtonColor());
                buttonTextColor = Color.parseColor(ADSMainClass.getLightNativeButtonTextColor());
            }

            int strokeWidth = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1.7f,
                    context.getResources().getDisplayMetrics()
            );

            float adCornerRadiusPx = context.getResources().getDimension(R.dimen.onboarding_dot_size);



            if (isDarkMode(context)) {
                GradientDrawable adBackground = new GradientDrawable();
                adBackground.setShape(GradientDrawable.RECTANGLE);
                adBackground.setColor(backgroundColor);
                adBackground.setCornerRadius(adCornerRadiusPx);
                adBackground.setStroke(strokeWidth, Color.parseColor("#252525"));
                adView.setBackground(adBackground);
            } else {
                GradientDrawable adBackground = new GradientDrawable();
                adBackground.setShape(GradientDrawable.RECTANGLE);
                adBackground.setColor(backgroundColor);
                adBackground.setCornerRadius(adCornerRadiusPx);
                adBackground.setStroke(strokeWidth, Color.parseColor("#CCE6E6E6"));
                adView.setBackground(adBackground);
            }
            if (cta != null) {

                int radius = Integer.parseInt(ADSMainClass.getButtonMargins());

                int dimenId = context.getResources().getIdentifier(
                        "_" + radius + "sdp",
                        "dimen",
                        context.getPackageName()
                );

                float radiusPx = 20f;

                if (dimenId != 0) {
                    radiusPx = context.getResources().getDimension(dimenId);
                }

                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                drawable.setColor(buttonColor);
                drawable.setCornerRadius(radiusPx);
//                drawable.setStroke(
//                        strokeWidth,
//                        Color.parseColor("#CCE6E6E6")
//                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    cta.setBackground(drawable);
                } else {
                    cta.setBackgroundDrawable(drawable);
                }

                cta.setTextColor(buttonTextColor);
            }
            if (headline != null) headline.setTextColor(textColor);

        } catch (Exception e) {

        }


        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(
                nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.GONE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            if (flag) {
                adView.getStarRatingView().setVisibility(View.GONE);
            } else {
                adView.getStarRatingView().setVisibility(View.GONE);
            }
        } else {
            ((RatingBar) adView.getStarRatingView())
                .setRating(nativeAd.getStarRating().floatValue());
            if (flag) {
                adView.getStarRatingView().setVisibility(View.GONE);
            } else {
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        }


        if (nativeAd.getAdvertiser() == null) {
            if (flag) {
                adView.getAdvertiserView().setVisibility(View.GONE);
            } else {
                adView.getAdvertiserView().setVisibility(View.GONE);
            }
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            if (flag) {
                adView.getAdvertiserView().setVisibility(View.GONE);
            } else {
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        }

        adView.setNativeAd(nativeAd);

        MediaContent vc = nativeAd.getMediaContent();

        if (vc.hasVideoContent()) {
            nativeAd.getMediaContent().getVideoController().setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    super.onVideoEnd();
                }
            });
        } else {
            mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

//    public static void displayAlreadyLoadedAdSmall(NativeAd ad, FrameLayout fl_adplaceholder, NativeAdLayout flNativeFBSmall, Activity activity, ShimmerFrameLayout shimmerFrameLayout) {
//        if (shimmerFrameLayout != null) {
//            shimmerFrameLayout.setVisibility(View.GONE);
//            shimmerFrameLayout.stopShimmer();
//        }
//        if (ad != null) {
//            try {
//                // Optimization: If the same ad is already displayed, skip re-inflation to prevent blinking
//                if (fl_adplaceholder.getChildCount() > 0) {
//                    View firstChild = fl_adplaceholder.getChildAt(0);
//                    if (firstChild instanceof NativeAdView && firstChild.getTag() == ad) {
//                        fl_adplaceholder.setVisibility(View.VISIBLE);
//                        return;
//                    }
//                }
//
//                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_unifiled_small, null);
//                adView.setTag(ad); // Tag it to identify during refreshes
//                populateUnifiedNativeSmallAdView(ad, adView, activity);
//                fl_adplaceholder.setVisibility(View.VISIBLE);
//                fl_adplaceholder.removeAllViews();
//                fl_adplaceholder.addView(adView);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

//    public static void showNativeAdsSmallMainWithListener(String adunit, FrameLayout fl_adplaceholder, Activity activity, ShimmerFrameLayout shimmerFrameLayout, AdLoadListener listener) {
//        if (shimmerFrameLayout != null) {
//            shimmerFrameLayout.setVisibility(View.VISIBLE);
//            if (!shimmerFrameLayout.isShimmerStarted()) {
//                shimmerFrameLayout.startShimmer();
//            }
//        }
//        if (!ADSUtilitis.IsNetworkConnected(activity)) {
//            if (shimmerFrameLayout != null && shimmerFrameLayout.isShimmerStarted()) {
//                shimmerFrameLayout.stopShimmer();
//            }
//            if (shimmerFrameLayout != null) shimmerFrameLayout.setVisibility(View.GONE);
//            if (listener != null) listener.onAdFailed();
//            return;
//        }
//        /*if (AdmobNativeAd != null) {
//            AdmobNativeAd.destroy();
//            AdmobNativeAd = null;
//        }*/
//        loadAdmobNativeAdSmallWithListener(adunit, fl_adplaceholder, shimmerFrameLayout, activity, listener);
//    }

    public interface AdLoadListener {
        void onAdLoaded(NativeAd nativeAd);

        void onAdFailed();
    }

//    @SuppressLint("MissingPermission")
//    public static void loadAdmobNativeAdSmallWithListener(String adId, FrameLayout fl_adplaceholder, ShimmerFrameLayout shimmerFrameLayout, Activity activity, AdLoadListener listener) {
//        String adUnitId;
//        adUnitId = adId;
//        if (TextUtils.isEmpty(adUnitId)) {
//            shimmerFrameLayout.setVisibility(View.GONE);
//            if (listener != null) listener.onAdFailed();
//            return;
//        }
//
//        AdLoader.Builder builder = new AdLoader.Builder(activity, adUnitId);
//
//        builder.forNativeAd(nativeAd -> {
//            AdmobNativeAd = nativeAd;
//            showNativeOptionAdSmall(nativeAd, fl_adplaceholder, activity);
//            if (shimmerFrameLayout.isShimmerStarted()) {
//                shimmerFrameLayout.stopShimmer();
//            }
//            shimmerFrameLayout.setVisibility(View.GONE);
//            ADSUtilitis.trackScreen(activity, "Native_Load");
//            if (listener != null) listener.onAdLoaded(nativeAd);
//        }).withAdListener(new AdListener() {
//
//            @Override
//            public void onAdLoaded() {
//                super.onAdLoaded();
//            }
//
//            @Override
//            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
//                super.onAdFailedToLoad(loadAdError);
//            }
//        });
//
//        VideoOptions videoOptions = new VideoOptions.Builder().setStartMuted(true).build();
//
//        NativeAdOptions adOptions = new NativeAdOptions.Builder().setVideoOptions(videoOptions).build();
//
//        builder.withNativeAdOptions(adOptions);
//        AdLoader adLoader = builder.withAdListener(new AdListener() {
//
//            @Override
//            public void onAdLoaded() {
//                super.onAdLoaded();
////                Log.d("TAGNative", "onAdLoaded: ");
//                shimmerFrameLayout.setVisibility(View.GONE);
//                shimmerFrameLayout.stopShimmer();
//                AdmobNativeAd.setOnPaidEventListener(new OnPaidEventListener() {
//                    @Override
//                    public void onPaidEvent(AdValue adValue) {
//                        ADSUtilitis.logAdRevenue(activity, adValue);
//                    }
//                });
//            }
//
//            @Override
//            public void onAdFailedToLoad(@NonNull LoadAdError adError) {
////                Log.d("TAGNative", "onAdLoaded: " +adError);
////                loadAdxNativeAdSmall(fl_adplaceholder, flNativeFBSmall, shimmerFrameLayout, activity);
//                ADSUtilitis.trackScreen(activity, "Native_Fail");
//                shimmerFrameLayout.setVisibility(View.GONE);
//                shimmerFrameLayout.stopShimmer();
////                Log.d("loadAdmobNativeAdSmall", "onAdFailedToLoad: 2222222222  " + adError);
//
//            }
//        }).build();
//
//        adLoader.loadAd(new AdRequest.Builder().build());
//    }

//    public static void showNativeOptionAdSmall(NativeAd nativeAd, FrameLayout fl_adplaceholder, Activity activity) {
//        try {
//            if (nativeAd != null) {
//                // Optimization: If already showing the same ad, don't re-inflate
//                if (fl_adplaceholder.getChildCount() > 0) {
//                    View firstChild = fl_adplaceholder.getChildAt(0);
//                    if (firstChild instanceof NativeAdView && firstChild.getTag() == nativeAd) {
//                        fl_adplaceholder.setVisibility(View.VISIBLE);
//                        return;
//                    }
//                }
//
//                NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(R.layout.ad_unifiled_small, null);
//                adView.setTag(nativeAd);
//                populateUnifiedNativeSmallAdView(nativeAd, adView, activity);
//                fl_adplaceholder.setVisibility(View.VISIBLE);
//                fl_adplaceholder.removeAllViews();
//                fl_adplaceholder.addView(adView);
//
//            } else {
//                fl_adplaceholder.setVisibility(View.GONE);
////                flNativeFBSmall.setVisibility(View.GONE);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            fl_adplaceholder.setVisibility(View.GONE);
////            flNativeFBSmall.setVisibility(View.GONE);
//
//        }
//    }

    public static void populateUnifiedNativeSmallAdView(NativeAd nativeAd, NativeAdView adView, Context context) {
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));
        try {

            TextView headline = (TextView) adView.getHeadlineView();
            TextView body = (TextView) adView.getBodyView();
            TextView cta = (TextView) adView.getCallToActionView();

            TextView price = adView.getPriceView() instanceof TextView
                    ? (TextView) adView.getPriceView() : null;

            TextView store = adView.getStoreView() instanceof TextView
                    ? (TextView) adView.getStoreView() : null;

            TextView advertiser = adView.getAdvertiserView() instanceof TextView
                    ? (TextView) adView.getAdvertiserView() : null;

            int backgroundColor;
            int textColor;
            int buttonColor;
            int buttonTextColor;

            if (isDarkMode(context)) {

                backgroundColor = Color.parseColor(ADSMainClass.getDarkNativeBackgroundColor());
                textColor = Color.parseColor(ADSMainClass.getDarkNativeAllTextColor());
                buttonColor = Color.parseColor(ADSMainClass.getDarkNativeButtonColor());
                buttonTextColor = Color.parseColor(ADSMainClass.getDarkNativeButtonTextColor());
            } else {
                backgroundColor = Color.parseColor(ADSMainClass.getLightNativeBackgroundColor());
                textColor = Color.parseColor(ADSMainClass.getLightNativeAllTextColor());
                buttonColor = Color.parseColor(ADSMainClass.getLightNativeButtonColor());
                buttonTextColor = Color.parseColor(ADSMainClass.getLightNativeButtonTextColor());
            }

            int strokeWidth = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1.7f,
                    context.getResources().getDisplayMetrics()
            );

            float adCornerRadiusPx = context.getResources().getDimension(R.dimen.onboarding_dot_size);

//            GradientDrawable adBackground = new GradientDrawable();
//            adBackground.setShape(GradientDrawable.RECTANGLE);
//            adBackground.setColor(backgroundColor);
//            adBackground.setCornerRadius(adCornerRadiusPx);
//            adBackground.setStroke(strokeWidth, Color.parseColor("#CCE6E6E6"));
//            adView.setBackground(adBackground);
            if (isDarkMode(context)) {
                GradientDrawable adBackground = new GradientDrawable();
                adBackground.setShape(GradientDrawable.RECTANGLE);
                adBackground.setColor(backgroundColor);
                adBackground.setCornerRadius(adCornerRadiusPx);
                adBackground.setStroke(strokeWidth, Color.parseColor("#252525"));
                adView.setBackground(adBackground);
            } else {


                GradientDrawable adBackground = new GradientDrawable();
                adBackground.setShape(GradientDrawable.RECTANGLE);
                adBackground.setColor(backgroundColor);
                adBackground.setCornerRadius(adCornerRadiusPx);
                adBackground.setStroke(strokeWidth, Color.parseColor("#CCE6E6E6"));
                adView.setBackground(adBackground);
            }

            if (cta != null) {
                cta.setBackgroundTintList(ColorStateList.valueOf(buttonColor));
                cta.setTextColor(buttonTextColor);
            }
            int radius = Integer.parseInt(ADSMainClass.getButtonMargins());

            int dimenId = context.getResources().getIdentifier(
                    "_" + radius + "sdp",
                    "dimen",
                    context.getPackageName()
            );

            if (dimenId != 0) {

                float radiusPx = context.getResources().getDimension(dimenId);

                Drawable bg = cta.getBackground().mutate();

                if (bg instanceof GradientDrawable) {
                    GradientDrawable drawable = (GradientDrawable) bg;
                    drawable.setCornerRadius(radiusPx);
                }

                cta.setTextColor(buttonTextColor);
            }
            if (headline != null) headline.setTextColor(textColor);
//            if (body != null) body.setTextColor(textColor);
//            if (price != null) price.setTextColor(textColor);
//            if (store != null) store.setTextColor(textColor);
//            if (advertiser != null) advertiser.setTextColor(textColor);
        } catch (Exception e) {

        }
        try {
            ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (nativeAd.getBody() == null) {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getBodyView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.INVISIBLE);
        } else {
            Objects.requireNonNull(adView.getCallToActionView()).setVisibility(View.VISIBLE);
            ((TextView) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }
        if (nativeAd.getIcon() == null) {
            Objects.requireNonNull(adView.getIconView()).setVisibility(View.GONE);
        } else {
            ((ImageView) Objects.requireNonNull(adView.getIconView())).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        // Price, store and stars are optional: layout_native_ad_small does not have them.
        if (adView.getPriceView() != null) {
            if (nativeAd.getPrice() == null) {
                adView.getPriceView().setVisibility(View.INVISIBLE);
            } else {
                adView.getPriceView().setVisibility(View.VISIBLE);
                ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
            }
        }

        if (adView.getStoreView() != null) {
            if (nativeAd.getStore() == null) {
                adView.getStoreView().setVisibility(View.INVISIBLE);
            } else {
                adView.getStoreView().setVisibility(View.VISIBLE);
                ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
            }
        }

        if (adView.getStarRatingView() != null) {
            if (nativeAd.getStarRating() == null) {
                adView.getStarRatingView().setVisibility(View.INVISIBLE);
            } else {
                ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
                adView.getStarRatingView().setVisibility(View.VISIBLE);
            }
        }

        if (adView.getAdvertiserView() != null) {
            if (nativeAd.getAdvertiser() == null) {
                adView.getAdvertiserView().setVisibility(View.INVISIBLE);
            } else {
                ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
                adView.getAdvertiserView().setVisibility(View.VISIBLE);
            }
        }
        if (adView.getStoreView() != null) adView.getStoreView().setVisibility(View.GONE);
        if (adView.getPriceView() != null) adView.getPriceView().setVisibility(View.GONE);
        adView.setNativeAd(nativeAd);
    }
}
