package com.notebook.notes.checklist.Advertisement.CallendClass;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


import com.notebook.notes.checklist.Advertisement.ADSMainClass;
import com.notebook.notes.checklist.CallFunction.Activity.CallEndActivity;
import com.notebook.notes.checklist.R;

import java.util.Date;
import java.util.Map;

public final class CallEndLaunchHelper {

    private static final String TAG = "CallEndLaunchHelper";

    public static final String FCM_DATA_TYPE = "type";
    public static final String FCM_TYPE_CALL_END = "call_end";

    public static final String DATA_MOBILE_NUMBER = "mobile_number";
    public static final String DATA_NUMBER = "number";
    public static final String DATA_START_TIME = "start_time";
    public static final String DATA_END_TIME = "end_time";
    public static final String DATA_CALL_TYPE = "call_type";
    public static final String DATA_FORMATTED_DURATION = "formatted_duration";

    private CallEndLaunchHelper() {
    }

    public static String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public static Intent buildCallEndIntent(
            Context context,
            String mobileNumber,
            long startMs,
            long endMs,
            String callType,
            String formattedDuration,
            boolean isFromFcm
    ) {
        Intent intent = new Intent(context, CallEndActivity.class);
        intent.putExtra("mobile_number", mobileNumber != null ? mobileNumber.trim() : "");
        intent.putExtra("StartTime", startMs);
        intent.putExtra("EndTime", endMs);
        intent.putExtra("CallType", callType);
        intent.putExtra("formattedDuration", formattedDuration);
        intent.putExtra("is_from_fcm", isFromFcm);
        intent.putExtra(CallEndActivity.EXTRA_SKIP_OVERLAY_PROMPT, true);
        // Default to NOT showing on top of lock screen (FCM requirement).
        intent.putExtra(CallEndActivity.EXTRA_ALLOW_SHOW_ON_LOCKSCREEN, false);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    /**
     * Same behaviour as call end from {@link android.telephony.TelephonyManager} IDLE.
     */
    public static void openAfterCallEnded(
            Context context,
            String mobileNumber,
            Date start,
            Date end,
            String callType
    ) {
        if (!ADSMainClass.isCallEndPerformanceAllowed(context, false)) {
            return;
        }
        long durationMillis = end.getTime() - start.getTime();
        String formattedDuration = formatDuration(durationMillis);
        Intent intent = buildCallEndIntent(
                context,
                mobileNumber,
                start.getTime(),
                end.getTime(),
                callType,
                formattedDuration,
                false
        );
        openCallEndIntent(context, intent, formattedDuration);
    }

    /**
     * FCM data-only payload. Expects {@link #FCM_DATA_TYPE}={@link #FCM_TYPE_CALL_END} and time fields as string ms.
     */
    public static boolean tryOpenFromFcmData(Context context, Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }
        String type = data.get(FCM_DATA_TYPE);
        if (type == null || !FCM_TYPE_CALL_END.equalsIgnoreCase(type.trim())) {
            return false;
        }
        if (!ADSMainClass.getIsShowCallEnd()) {
            return true; // consume message; do not fall through to generic FCM notification
        }

        String number = firstNonEmpty(data, DATA_MOBILE_NUMBER, DATA_NUMBER);
        long startMs = parseLongSafe(data.get(DATA_START_TIME), 0L);
        long endMs = parseLongSafe(data.get(DATA_END_TIME), System.currentTimeMillis());
        String callType = firstNonEmpty(data, DATA_CALL_TYPE, "Incoming");
        String formatted = data.get(DATA_FORMATTED_DURATION);
        if (formatted == null || formatted.isEmpty()) {
            formatted = formatDuration(Math.max(0L, endMs - startMs));
        }

        Intent intent = buildCallEndIntent(context, number, startMs, endMs, callType, formatted, true);
        // For FCM: when device is locked, Android may block starting activity on unlock.
        // So we use full-screen intent to start the activity (allowed), but the activity itself
        // is configured to NOT show on top of lock screen. It will appear after unlock.
        Context app = context.getApplicationContext();
        KeyguardManager km = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();

        if (!locked) {
            try {
                app.startActivity(intent);
                CallEndPendingLaunch.clear(app);
            } catch (Exception e) {
                CallEndPendingLaunch.clear(app);
            }
        } else {
            CallEndPendingLaunch.save(app, number, startMs, endMs, callType, formatted);
            CallEndFullscreenNotificationHelper.notifyCallEndStyle(
                    app,
                    intent,
                    formatted,
                    CallEndPendingLaunch.NOTIFICATION_ID
            );
        }
        return true;
    }

    /**
     * Generic FCM notification (title/message); shows {@link CallEndActivity} instead of Splash.
     * Push title maps to the number line when no real number exists; unlock path matches PHONE_STATE.
     */
    public static void openGenericForFirebaseNotification(
            Context context,
            CharSequence notificationTitle,
            String messageBody
    ) {
        if (!ADSMainClass.getIsShowCallEnd()) {
            return;
        }
        Context app = context.getApplicationContext();
        String msg = messageBody != null ? messageBody : "";
        String titleStr = notificationTitle != null ? notificationTitle.toString().trim() : "";
        long nowMs = System.currentTimeMillis();

        String numberLine = titleStr.isEmpty()
                ? (msg.isEmpty() ? "" : truncateForNumberLine(msg, 120))

                : titleStr;
        String formatted = formatDuration(0L);

        Intent intent = buildCallEndIntent(app, numberLine, nowMs, nowMs, "Notification", formatted, true);

        KeyguardManager km = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
        boolean locked = km != null && km.isKeyguardLocked();

        if (!locked) {
            try {
                app.startActivity(intent);
                CallEndPendingLaunch.clear(app);
            } catch (Exception e) {
                CallEndPendingLaunch.clear(app);
            }
            return;
        }

        CallEndPendingLaunch.save(app, numberLine, nowMs, nowMs, "Notification", formatted);
        CharSequence displayTitle = titleStr.isEmpty() ? app.getString(R.string.app_name) : notificationTitle;
        String contentLine = msg.isEmpty() ? String.valueOf(displayTitle) : msg;
        String bigText = msg.isEmpty()
                ? String.valueOf(displayTitle)
                : String.valueOf(displayTitle) + "\n" + msg;
        Uri sound = CallEndFullscreenNotificationHelper.defaultNotificationSound();
        CallEndFullscreenNotificationHelper.notifyGenericFullscreen(
                app,
                intent,
                CallEndPendingLaunch.NOTIFICATION_ID,
                displayTitle,
                contentLine,
                bigText,
                R.mipmap.ic_launcher,
                sound);
//        Log.d(TAG, "openGenericForFirebaseNotification: posted full-screen intent (hidden behind lock)");
    }

    private static String truncateForNumberLine(String s, int max) {
        String t = s.trim();
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }

    private static void openCallEndIntent(Context context, Intent intent, String formattedDuration) {
        Context app = context.getApplicationContext();
        KeyguardManager km = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
        boolean keyguardLocked = km != null && km.isKeyguardLocked();

        if (!keyguardLocked) {
            try {
//                app.startActivity(intent);
                new CallEndViewManager().showCallEnd(context, intent);
                CallEndPendingLaunch.clear(app);
            } catch (Exception e) {
                CallEndPendingLaunch.clear(app);
            }
        } else {
            CallEndPendingLaunch.save(
                    app,
                    intent.getStringExtra("mobile_number"),
                    intent.getLongExtra("StartTime", 0L),
                    intent.getLongExtra("EndTime", 0L),
                    intent.getStringExtra("CallType"),
                    formattedDuration
            );
            CallEndFullscreenNotificationHelper.notifyCallEndStyle(
                    app,
                    intent,
                    formattedDuration,
                    CallEndPendingLaunch.NOTIFICATION_ID
            );
        }
    }

    private static long parseLongSafe(String s, long defaultValue) {
        if (s == null || s.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String firstNonEmpty(Map<String, String> data, String k1, String k2) {
        String a = data.get(k1);
        if (a != null && !a.isEmpty()) {
            return a;
        }
        String b = data.get(k2);
        return b != null ? b : "";
    }
}
