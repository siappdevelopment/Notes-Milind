package com.notebook.notes.checklist.Advertisement.CallendClass;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.notebook.notes.checklist.Advertisement.ADSMainClass;
import com.notebook.notes.checklist.CallFunction.Activity.CallEndActivity;


public final class CallEndPendingLaunch {

    private static final String TAG = "CallEndPendingLaunch";
    private static final String PREFS = "call_end_pending_launch";
    private static final String KEY_PENDING = "pending";
    private static final String KEY_NUMBER = "mobile_number";
    private static final String KEY_START = "start_ms";
    private static final String KEY_END = "end_ms";
    private static final String KEY_TYPE = "call_type";
    private static final String KEY_DURATION = "formatted_duration";

    public static final int NOTIFICATION_ID = 1001;

    private CallEndPendingLaunch() {
    }

    public static void save(
            Context context,
            String number,
            long startMs,
            long endMs,
            String callType,
            String formattedDuration
    ) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PENDING, true)
                .putString(KEY_NUMBER, number != null ? number : "")
                .putLong(KEY_START, startMs)
                .putLong(KEY_END, endMs)
                .putString(KEY_TYPE, callType != null ? callType : "")
                .putString(KEY_DURATION, formattedDuration != null ? formattedDuration : "00:00")
                .apply();
    }

    public static void clear(Context context) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    public static boolean hasPending(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_PENDING, false);
    }

    public static void cancelCallEndNotification(Context context) {
        android.app.NotificationManager nm = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID);
        }
    }

    public static void tryLaunchAndClear(Context context) {
        Context app = context.getApplicationContext();
        if (!hasPending(app)) {
            return;
        }
        if (!ADSMainClass.getIsShowCallEnd()) {
            clear(app);
            cancelCallEndNotification(app);
            return;
        }
        SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String number = p.getString(KEY_NUMBER, "");
        long startMs = p.getLong(KEY_START, 0L);
        long endMs = p.getLong(KEY_END, 0L);
        String callType = p.getString(KEY_TYPE, "");
        String duration = p.getString(KEY_DURATION, "00:00");

        Intent intent = new Intent(app, CallEndActivity.class);
        intent.putExtra("mobile_number", number);
        intent.putExtra("StartTime", startMs);
        intent.putExtra("EndTime", endMs);
        intent.putExtra("CallType", callType);
        intent.putExtra("formattedDuration", duration);
        intent.putExtra(CallEndActivity.EXTRA_SKIP_OVERLAY_PROMPT, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        try {
            app.startActivity(intent);
            clear(app);
            cancelCallEndNotification(app);
        } catch (Exception e) {
//            Log.e(TAG, "tryLaunchAndClear: startActivity failed", e);
        }
    }
}
