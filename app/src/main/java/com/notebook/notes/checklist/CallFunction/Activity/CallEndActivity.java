package com.notebook.notes.checklist.CallFunction.Activity;

import static android.content.Intent.getIntent;


import static com.notebook.notes.checklist.Advertisement.ADSInterDisplay.AdsDisplayCheck;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;


import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.notebook.notes.checklist.Advertisement.ADSAdpater;
import com.notebook.notes.checklist.Advertisement.ADSAppManage;
import com.notebook.notes.checklist.Advertisement.ADSMainClass;
import com.notebook.notes.checklist.Advertisement.ADSNativeFullDisplay;
import com.notebook.notes.checklist.Advertisement.ADSUtilitis;
import com.notebook.notes.checklist.Advertisement.CallEndInterAd;
import com.notebook.notes.checklist.Advertisement.CallendClass.CallEndPendingLaunch;
import com.notebook.notes.checklist.Advertisement.activity.BaseActivity;
import com.notebook.notes.checklist.CallFunction.Adapter.CallShowTabAdapter;
import com.notebook.notes.checklist.CallFunction.Service.PreferenceDayCycle;
import com.notebook.notes.checklist.R;
import com.notebook.notes.checklist.ui.activity.HomeActivity;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CallEndActivity extends BaseActivity {

    private ImageView ic_call;
    private TabLayout tabLayout;
    private TextView txtCurrentTime;
    private TextView textViewDuration;
    private ViewPager viewPager;

    public static final String EXTRA_SKIP_OVERLAY_PROMPT = "skip_overlay_prompt";

    public static final String EXTRA_ALLOW_SHOW_ON_LOCKSCREEN = "allow_show_on_lockscreen";
    private Boolean isAdsShowComplete = false;
    private List<Integer> defaultIcons = Arrays.asList(
        R.drawable.icon_img_call_first_icon,
        R.drawable.icon_img_call_second_icon,
        R.drawable.icon_img_call_four_icon,
        R.drawable.icon_img_call_third_icon
    );
    private ShimmerFrameLayout linearBannerShimmer;
    private FrameLayout adContainer;
    private AdView adView;

    private LinearLayout header;
    private LinearLayout mainView;
    private FrameLayout fullAdContainer;
    private RelativeLayout rlFullAd;
    private LinearLayout ivClose;
    public static NativeAd AdmobNativeAd;


    public static void sendFirebaseEvent(Activity activity, String str, String str2) {
        FirebaseAnalytics instance = FirebaseAnalytics.getInstance(activity);
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, str);
        bundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, str2);
        instance.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        try {
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } catch (IllegalStateException e) {
            // Ignore the exception if the device still complains about non-fullscreen opaque activities
        }
        super.onCreate(savedInstanceState);
        Log.e("Millind", "onCreate: " );
        boolean allowShowWhenLocked = getIntent() != null
            && getIntent().getBooleanExtra(EXTRA_ALLOW_SHOW_ON_LOCKSCREEN, false);
        if (allowShowWhenLocked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
            } else {
                @SuppressWarnings("deprecation")
                int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
                getWindow().addFlags(flags);
            }
        }
//        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_end);


        CallEndPendingLaunch.cancelCallEndNotification(this);

        if (getIntent() != null && getIntent().getBooleanExtra("is_from_fcm", false)) {
            boolean notif = ADSMainClass.isNotificationGranted(this);
            boolean call = ADSMainClass.isCallStateGranted(this);
            boolean overlay = ADSMainClass.isOverlayGranted(this);

            if (notif && call && overlay) {
                ADSUtilitis.trackScreen(this, "FB_Notification_show_CallEnd_Allow3");
            } else if (notif && call) {
                ADSUtilitis.trackScreen(this, "FB_Notification_show_CallEnd_Allow2");
            } else if (notif) {
                ADSUtilitis.trackScreen(this, "FB_Notification_show_CallEnd_Allow1");
            }
        }
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.white));


        adContainer = findViewById(R.id.flNativeSmallPlaceholder);
        linearBannerShimmer = findViewById(R.id.shimmer_container_small);
        header = findViewById(R.id.header);
        mainView = findViewById(R.id.mainView);
        fullAdContainer = findViewById(R.id.fullAdContainer);
        rlFullAd = findViewById(R.id.rlFullAd);
        ivClose = findViewById(R.id.ivClose);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View window = getWindow().getDecorView();
            window.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        ic_call = findViewById(R.id.ic_call);
        textViewDuration = findViewById(R.id.textViewDuration);
//        getIntent().getStringExtra("StartTime");
//        getIntent().getStringExtra("EndTime");

        MobileAds.initialize(this, (InitializationStatus initializationStatus) -> {
        });

        setCallEndEvent();
        setAppRetention();

        String stringExtra2 = getIntent().getStringExtra("formattedDuration");
        textViewDuration.setText("Duration : " + stringExtra2);
        txtCurrentTime.setText(
                new SimpleDateFormat("hh:mm a", Locale.getDefault())
                        .format(Calendar.getInstance().getTime())
        );
        ic_call.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            startActivity(intent);
            finish();
        });


        tabLayout = findViewById(R.id.wsfw_tabslayout);
        viewPager = findViewById(R.id.wsfw_viewpager);

        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_img_call_first_icon));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_img_call_second_icon));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_img_call_four_icon));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.icon_img_call_third_icon));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        viewPager.setAdapter(new CallShowTabAdapter(this, getSupportFragmentManager(), tabLayout.getTabCount()));
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                if (tab.getPosition() == 0) {
                    tab.setIcon(R.drawable.icon_img_call_first_icon);
                } else if (tab.getPosition() == 1) {
                    tab.setIcon(R.drawable.icon_img_call_second_icon);
                } else if (tab.getPosition() == 2) {
                    tab.setIcon(R.drawable.icon_img_call_four_icon);
                } else {
                    tab.setIcon(R.drawable.icon_img_call_third_icon);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                tab.setIcon(defaultIcons.get(tab.getPosition()));
            }
        });

        if (ADSUtilitis.IsNetworkConnected(this) && ADSMainClass.shouldShowCallEndAd(this)) {
            CallEndInterAd.loadAd(this);
        }

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getBack();
            }
        });

        header.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMainActivity();
            }
        });

        linearBannerShimmer.setOnClickListener(v -> {
            // Do nothing, just consume the click
        });

        ivClose.setOnClickListener(v -> finish());

        String callType = getIntent().getStringExtra("CallType");
        boolean isFirebaseFlow = "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);
        Log.d("DDDDDDDDD", "onCreate:  1111111111" );
        if (isFirebaseFlow) {
//            Log.d("FCM_AD", "Firebase flow detected in CallEndActivity.");
            mainView.setVisibility(View.GONE);
            fullAdContainer.setVisibility(View.VISIBLE);

            // Show preloaded ad in fullAdContainer if available
            if (ADSNativeFullDisplay.AdmobNativeAd != null) {
                Log.d("DDDDDDDDD", "onCreate:  222222" );
//                Log.d("FCM_AD", "Using preloaded Native Ad from ADSNativeFullDisplay.");
                NativeAd preloadedAd = ADSNativeFullDisplay.AdmobNativeAd;
                ADSNativeFullDisplay.AdmobNativeAd = null;

                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                NativeAdView adView = (NativeAdView) inflater.inflate(R.layout.ads_full_native_load, null);
                PopulateUnifiedFullNativeAdView(preloadedAd, adView, false);
                fullAdContainer.removeAllViews();
                fullAdContainer.addView(adView);
//                Log.d("FCM_AD", "Preloaded Ad displayed in fullAdContainer.");
            } else {
//                Log.e("FCM_AD", "Preloaded Native Ad is NULL in CallEndActivity!");
                mainView.setVisibility(View.VISIBLE);
                fullAdContainer.setVisibility(View.GONE);
                Log.d("DDDDDDDDD", "onCreate:  " +ADSMainClass.getCallEndBottomAdsShow());
                if (ADSMainClass.getCallEndBottomAdsShow()) {
                    if (ADSMainClass.getCallEndBottomAdsType().equalsIgnoreCase("native")) {
                        loadAdmobNative(this);
                    } else {
                        loadAdmobBannerAd();
                    }
                } else {
                    linearBannerShimmer.setVisibility(View.GONE);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowInsetsController controller = getWindow().getInsetsController();
                    if (controller != null) {
                        controller.hide(WindowInsets.Type.navigationBars());
                        controller.setSystemBarsBehavior(
                            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                    }
                }

            }

            rlFullAd.setVisibility(View.VISIBLE);
            if (ADSMainClass.getCloseButtonShowOnFullNativeAds()) {
                ivClose.setVisibility(View.VISIBLE);
            } else {
                ivClose.setVisibility(View.GONE);
            }
        } else {
            Log.d("DDDDDDDDD", "onCreate:  3333333" );
//            Log.d("FCM_AD", "Local flow detected in CallEndActivity.");
            mainView.setVisibility(View.VISIBLE);
            fullAdContainer.setVisibility(View.GONE);
            rlFullAd.setVisibility(View.GONE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    );
                }
            }
        }

        if (!isFirebaseFlow) {

            if (ADSMainClass.getCallEndBottomAdsShow()) {

                if (ADSMainClass.getCallEndBottomAdsType().equalsIgnoreCase("native")) {
                    loadAdmobNative(this);
                } else {

                    loadAdmobBannerAd();
                }
            } else {
                linearBannerShimmer.setVisibility(View.GONE);
            }
        }


        sendFirebaseEvent(this, "CallEndActivity", "CallEndActivity");
    }


    private void setAppRetention() {
        PreferenceDayCycle.checkAndUpdateDayCount(this);
        int dayCount = PreferenceDayCycle.getDayCount(this);
        if (dayCount > 0 && dayCount <= 7) {
            setAppRetentionLogEvent(dayCount);
        }
    }

    private void setAppRetentionLogEvent(int dayCount) {
        FirebaseApp.initializeApp(this);
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();

        if (dayCount == 1 && !ADSMainClass.getCallRetention1Day()) {
            bundle.putBoolean("OneDayCallEndRetention", true);
            firebaseAnalytics.logEvent("OneDayCallEndRetention", bundle);
            ADSMainClass.setCallRetention1Day(true);
        } else if (dayCount == 3 && !ADSMainClass.getCallRetention3Day()) {
            bundle.putBoolean("ThreeDayCallEndRetention", true);
            firebaseAnalytics.logEvent("ThreeDayCallEndRetention", bundle);
            ADSMainClass.setCallRetention3Day(true);
        } else if (dayCount == 7 && !ADSMainClass.getCallRetention7Day()) {
            bundle.putBoolean("SevenDayCallEndRetention", true);
            firebaseAnalytics.logEvent("SevenDayCallEndRetention", bundle);
            ADSMainClass.setCallRetention7Day(true);
        }
    }

    private void setCallEndEvent() {
        int countCallEnd = ADSMainClass.getCallEndShowEvent() + 1;
        ADSMainClass.setCallEndShowEvent(countCallEnd);
        FirebaseApp.initializeApp(this);
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();

        if (countCallEnd == 1 && !ADSMainClass.getCallEndShow1()) {
            bundle.putBoolean("OneTimeCallEndShow", true);
            firebaseAnalytics.logEvent("OneTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow1(true);
        } else if (countCallEnd == 5 && !ADSMainClass.getCallEndShow5()) {
            bundle.putBoolean("FiveTimeCallEndShow", true);
            firebaseAnalytics.logEvent("FiveTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow5(true);
        } else if (countCallEnd == 10 && !ADSMainClass.getCallEndShow10()) {
            bundle.putBoolean("TenTimeCallEndShow", true);
            firebaseAnalytics.logEvent("TenTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow10(true);
        } else if (countCallEnd == 50 && !ADSMainClass.getCallEndShow50()) {
            bundle.putBoolean("FiftyTimeCallEndShow", true);
            firebaseAnalytics.logEvent("FiftyTimeCallEndShow", bundle);
            ADSMainClass.setCallEndShow50(true);
        } else if (countCallEnd == 100 && !ADSMainClass.getCallEndShow100()) {
            bundle.putBoolean("HundredTimeCallEndShow", true);
            firebaseAnalytics.logEvent("HundredTimeCallEndShow", bundle);

            ADSMainClass.setCallEndShow100(true);
        }
    }



    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Do not cancel notification here; if activity was started via full-screen intent while locked,
        // we want the notification to remain until the user actually unlocks and sees the UI.
    }

    @Override
    public void onResume() {
        ADSAppManage.isAppOpenBlocked = true;
        super.onResume();
        try {
            // Cancel notification unconditionally on resume to ensure it is always cleared
            CallEndPendingLaunch.cancelCallEndNotification(this);
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            boolean locked = km != null && km.isKeyguardLocked();
            if (!locked) {
                CallEndPendingLaunch.clear(this);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onStart() {
        ADSAppManage.isAppOpenBlocked = true;
        super.onStart();
    }

    public void getBack() {

        String callType = getIntent().getStringExtra("CallType");
        boolean isFirebaseFlow = "Notification".equalsIgnoreCase(callType) || "call_end".equalsIgnoreCase(callType);

        boolean showAd;
        if (isFirebaseFlow) {
            showAd = ADSMainClass.getNotificationScreenBackAdsShow();
        } else {
            showAd = ADSMainClass.shouldShowCallEndAd(this);
        }
        int openCount = getIntent().getIntExtra("open_count", 0);
        int limit = ADSMainClass.getCallEndAgainOpenCount();

        if (openCount < limit) {
            if (!isAdsShowComplete) {
                if (ADSUtilitis.IsNetworkConnected(this)) {
                    Context appContext = getApplicationContext();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        try {
                            ADSAppManage.isAppOpenBlocked = true;
                            Intent intent = new Intent(appContext, CallEndActivity.class);
                            if (getIntent().getExtras() != null) {
                                intent.putExtras(getIntent().getExtras());
                            }
                            intent.putExtra("open_count", openCount + 1);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            appContext.startActivity(intent);
                        } catch (Exception e) {
//                            Log.e("TAG", "Failed to start activity from app context", e);
                        }
                    }, 2000);
                    finish();
                } else {
                    finish();
                }

            } else {
                if (showAd && !ADSMainClass.getAds_Free()) {
                    CallEndInterAd.fullScreenAdShow(this, b -> {
                        if (b) {
                            ADSMainClass.updateCallEndAdCount(this);
                        }
                        new Handler().postDelayed(this::finish, 1000);
                    });
                } else {
                    finish();
                }
            }
        } else {
            finish();
        }
    }


    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        if (AdmobNativeAd != null) {
            AdmobNativeAd.destroy();
            AdmobNativeAd = null;
        }
        if (adContainer != null) {
            adContainer.removeAllViews();
        }
        ADSAppManage.dialogboolean = true;
        super.onDestroy();
    }


    private void loadAdmobBannerAd() {
        linearBannerShimmer.setVisibility(View.VISIBLE);
        adView = new AdView(this);
        adView.setAdUnitId(ADSMainClass.getStringValue(ADSMainClass.CALL_END_BANNER));
        // "big_banner" / "mrec" in callend_bottom_ads_type shows the 300x250 medium rectangle;
        // plain "banner" keeps the adaptive banner.
        String bottomType = ADSMainClass.getCallEndBottomAdsType();
        boolean bigBanner = "big_banner".equalsIgnoreCase(bottomType) || "mrec".equalsIgnoreCase(bottomType);
        AdSize adSize = bigBanner ? AdSize.MEDIUM_RECTANGLE : getAdaptiveAdSize();
        adView.setAdSize(adSize);
        adView.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        adContainer.clearFocus();
        adContainer.removeAllViews();
        adContainer.addView(adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);


        adView.setOnPaidEventListener(new OnPaidEventListener() {
            @Override
            public void onPaidEvent(AdValue adValue) {
                ADSUtilitis.logAdRevenue(CallEndActivity.this, adValue);
            }
        });
        Log.d("DDDDDDDDDD", "onAdFailedToLoad: "  +ADSMainClass.getStringValue(ADSMainClass.CALL_END_BANNER));
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                Log.d("DDDDDDDDDD", "onAdFailedToLoad: ");

                isAdsShowComplete = false;
                linearBannerShimmer.setVisibility(View.GONE);
                ADSUtilitis.trackScreen(CallEndActivity.this, "CallEndBanner_Fail");
//                Log.d("banner_ads_load_check", "onAdFailedToLoad: " + loadAdError.toString());
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                Log.d("DDDDDDDDDD", "onAdLoaded: ");
                isAdsShowComplete = true;
                linearBannerShimmer.setVisibility(View.GONE);
                ADSUtilitis.trackScreen(CallEndActivity.this, "CallEndBanner_Load");
//                Log.d("banner_ads_load_check", "onAdToLoad: " + "success Load");
            }
        });


    }

    public void loadAdmobNative(final Context context) {
        Log.d("DDDDDDDDD", "onCreate:  666666"  +ADSMainClass.getStringValue(ADSMainClass.CALL_END_Native) );
//        Log.d("TAG", "loadAdmobNative: " +ADSMainClass.getStringValue(ADSMainClass.CALL_END_Native));
        AdLoader.Builder builder = new AdLoader.Builder(context, ADSMainClass.getStringValue(ADSMainClass.CALL_END_Native).trim())
            .forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                @Override
                public void onNativeAdLoaded(NativeAd nativeAd) {
                    isAdsShowComplete = true;
//                        Log.d("TAG", "loadAdmobNative: 666  " );
                    ADSUtilitis.trackScreen(CallEndActivity.this, "CallEndNative_Load");
                    linearBannerShimmer.setVisibility(View.GONE);
                    nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                        @Override
                        public void onPaidEvent(AdValue adValue) {
                            ADSUtilitis.logAdRevenue(context, adValue);
                        }
                    });

                    AdmobNativeAd = nativeAd;

//                        if (ADSMainClass.getAdsTypeManage().equals("Load")) {
                    if (AdmobNativeAd != null) {

                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                        NativeAdView adView;
                        adView = (NativeAdView) inflater.inflate(R.layout.admob_big_native_ad, null);

                        adContainer.setVisibility(View.VISIBLE);
                        PopulateUnifiedFullNativeAdView(AdmobNativeAd, adView, false);

                        adView.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                        adContainer.clearFocus();
                        adContainer.removeAllViews();
                        adContainer.addView(adView);
                        ADSAdpater.admob_nativehashmap.put(ADSAdpater.pos, adView);
                    }
//                        }
                }
            });

        AdLoader adLoader = builder.withAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    isAdsShowComplete = false;
//                        Log.d("TAG", "loadAdmobNative: 777  "  +adError.getMessage());
                    if (AdmobNativeAd != null) {
                        AdmobNativeAd = null;
                    }
                    ADSUtilitis.trackScreen(CallEndActivity.this, "CallEndNative_Fail");
                    linearBannerShimmer.setVisibility(View.GONE);
//                        AdmobFullNative(context, adsId, linearLayout, banner_flag);

                }

                @Override
                public void onAdClicked() {
                    ADSAppManage.FastStart = true;
                    AdsDisplayCheck(false);
                }
            })
            .build();
        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void PopulateUnifiedFullNativeAdView(NativeAd nativeAd, NativeAdView adView, boolean flag) {

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
            ((TextView) adView.getCallToActionView()).setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(ADSMainClass.getNativeButtonColor())));
            ((TextView) adView.getCallToActionView()).setTextColor(Color.parseColor(ADSMainClass.getNativeButtonTextColor()));
        } catch (NullPointerException n) {
            n.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
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

    private AdSize getAdaptiveAdSize() {
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = outMetrics.density;
        int adWidth = (int) (outMetrics.widthPixels / density);
        return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(this, adWidth);
    }

    private void openMainActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private static String normalizePhoneNumber(String number) {
        if (number == null) {
            return null;
        }
        String trimmed = number.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }


}
