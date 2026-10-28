package com.notebook.notes.checklist.Advertisement;

import android.app.Activity;
import android.app.Application;
import android.app.KeyguardManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

/**
 * All permission background analytics live here only.
 * Registers itself via {@link InitProvider} — no Application / Activity code elsewhere.
 */
public final class PermissionAnalyticsHelper {

    private static final String TAG = "PermissionAnalytics";

    public static final String SOURCE_PERMISSION = "permission_screen";

    private static final String PREFS_NAME = "permission_analytics_prefs";
    private static final String KEY_FIRST_EVENT_LOGGED = "first_permission_event_logged";
    private static final String KEY_SECOND_EVENT_LOGGED = "second_permission_event_logged";
    private static final String KEY_FIRST_NOTIFICATION_DENIED = "first_event_notification_denied";
    private static final String KEY_FIRST_OVERLAY_DENIED = "first_event_overlay_denied";

    private static final long BACKGROUND_SIGNAL_DEBOUNCE_MS = 1500L;

    private static volatile boolean installed = false;
    private static int startedActivityCount = 0;
    private static boolean backgroundEventsReady = false;
    private static long lastBackgroundSignalHandledMs = 0L;

    private PermissionAnalyticsHelper() {
    }

    /** Auto-starts analytics when the app process starts (see AndroidManifest provider entry). */
    public static final class InitProvider extends ContentProvider {
        @Override
        public boolean onCreate() {
            Context context = getContext();
            if (context != null) {
                install((Application) context.getApplicationContext());
            }
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection,
                            String[] selectionArgs, String sortOrder) {
            return null;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }
    }

    private static void install(Application application) {
        if (installed) {
            return;
        }
        synchronized (PermissionAnalyticsHelper.class) {
            if (installed) {
                return;
            }

            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                    if (isStartupOnlyActivity(activity)) {
                        backgroundEventsReady = false;
                    }
                }

                @Override
                public void onActivityStarted(Activity activity) {
                    startedActivityCount++;
                }

                @Override
                public void onActivityResumed(Activity activity) {
                    if (!isStartupOnlyActivity(activity)) {
                        backgroundEventsReady = true;
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    startedActivityCount = Math.max(0, startedActivityCount - 1);
                    if (startedActivityCount == 0) {
                        logPermissionEventsOnAppBackground(
                                activity.getApplicationContext(), SOURCE_PERMISSION);
                    }
                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                }

                @Override
                public void onActivityDestroyed(Activity activity) {
                    if (activity.isFinishing() && startedActivityCount <= 0) {
                        logPermissionEventsOnAppBackground(
                                activity.getApplicationContext(), SOURCE_PERMISSION);
                    }
                }
            });

            ProcessLifecycleOwner.get().getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
                void onAppBackgrounded() {
                    logPermissionEventsOnAppBackground(application, SOURCE_PERMISSION);
                }
            });

            installed = true;
//            Log.d(TAG, "Background permission analytics installed");
        }
    }

    public static void logPermissionEventsOnAppBackground(Context context, String source) {
        if (context == null) {
            return;
        }

        if (ADSAppManage.isAppOpenBlocked) {
//            Log.d(TAG, "Skipped → isAppOpenBlocked (permission settings open)");
            return;
        }

        if (!backgroundEventsReady) {
//            Log.d(TAG, "Skipped → app still in splash / startup flow");
            return;
        }

        if (isScreenOffOrDeviceLocked(context)) {
//            Log.d(TAG, "Skipped → screen off / device locked (power button)");
            return;
        }

        if (isDuplicateBackgroundSignal()) {
//            Log.d(TAG, "Skipped → debounced duplicate background signal");
            return;
        }

        if (!isFirstEventLogged(context)) {
            persistFirstEventSnapshot(context);
            logEvent(context, source, false);
            return;
        }

        if (isSecondEventLogged(context)) {
//            Log.d(TAG, "Skipped → second event already logged (3rd time onwards)");
            return;
        }

        if (isSecondEventEligible(context)) {
            markSecondEventLogged(context);
            logEvent(context, source, true);
        } else {
//            Log.d(TAG, "Second event skipped → notification/overlay not newly allowed");
        }
    }

    @Deprecated
    public static void logFirstTimePermissionCombination(Context context, String source) {
        logPermissionEventsOnAppBackground(context, source);
    }

    private static boolean isStartupOnlyActivity(Activity activity) {
        String className = activity.getClass().getSimpleName();
        return className.equals("SplashActivity")
//                || className.equals("FullMActivity")
                || className.equals("CommingActivity")
                || className.equals("CallEndActivity")
                || className.equals("OverlayPermissionActivity");
    }

    private static boolean isDuplicateBackgroundSignal() {
        long now = System.currentTimeMillis();
        synchronized (PermissionAnalyticsHelper.class) {
            if (now - lastBackgroundSignalHandledMs < BACKGROUND_SIGNAL_DEBOUNCE_MS) {
                return true;
            }
            lastBackgroundSignalHandledMs = now;
            return false;
        }
    }

    private static boolean isSecondEventEligible(Context context) {
        boolean wasNotificationDenied = wasNotificationDeniedAtFirstEvent(context);
        boolean wasOverlayDenied = wasOverlayDeniedAtFirstEvent(context);

        if (!wasNotificationDenied && !wasOverlayDenied) {
//            Log.d(TAG, "Second event skipped → notification and overlay were allow at first event");
            return false;
        }

        if (wasNotificationDenied && isNotificationGranted(context)) {
            return true;
        }
        return wasOverlayDenied && isOverlayGranted(context);
    }

    private static void logEvent(Context context, String source, boolean secondTime) {
        String eventName = buildCombinationEventName(context);
        if (secondTime) {
            eventName = eventName + "_2";
        }
        String eventType = secondTime ? "SECOND_TIME" : "FIRST_TIME";
        Log.d(TAG, "Firebase Analytics → event_name=\"" + eventName + "\" type=" + eventType
                + " source=" + source);

        Bundle bundle = new Bundle();
        bundle.putString("source", source);
//        FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle);
    }

    private static String buildCombinationEventName(Context context) {
        return "notification_" + notificationLabel(context)
                + "_call_" + callLabel(context)
                + "_overlay_" + overlayLabel(context);
    }

    private static String notificationLabel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return "allow";
        }
        return isNotificationGranted(context) ? "allow" : "deny";
    }

    private static String callLabel(Context context) {
        return isCallStateGranted(context) ? "allow" : "deny";
    }

    private static String overlayLabel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return "allow";
        }
        return isOverlayGranted(context) ? "allow" : "deny";
    }

    private static boolean isOverlayGranted(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return Settings.canDrawOverlays(context);
    }

    private static boolean isNotificationGranted(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isCallStateGranted(Context context) {
        return ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean isScreenOffOrDeviceLocked(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null && !powerManager.isInteractive()) {
            return true;
        }
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager != null && keyguardManager.isKeyguardLocked();
    }

    private static void persistFirstEventSnapshot(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_FIRST_EVENT_LOGGED, true)
                .putBoolean(KEY_FIRST_NOTIFICATION_DENIED, !isNotificationGranted(context))
                .putBoolean(KEY_FIRST_OVERLAY_DENIED, !isOverlayGranted(context))
                .commit();
    }

    private static boolean wasNotificationDeniedAtFirstEvent(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRST_NOTIFICATION_DENIED, false);
    }

    private static boolean wasOverlayDeniedAtFirstEvent(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRST_OVERLAY_DENIED, false);
    }

    private static boolean isFirstEventLogged(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_FIRST_EVENT_LOGGED, false);
    }

    private static boolean isSecondEventLogged(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_SECOND_EVENT_LOGGED, false);
    }

    private static void markSecondEventLogged(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SECOND_EVENT_LOGGED, true)
                .commit();
    }
}
