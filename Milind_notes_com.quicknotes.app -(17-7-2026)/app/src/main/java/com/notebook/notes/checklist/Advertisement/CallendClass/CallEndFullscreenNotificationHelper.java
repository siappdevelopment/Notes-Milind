package com.notebook.notes.checklist.Advertisement.CallendClass;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public final class CallEndFullscreenNotificationHelper {

    public static final String CHANNEL_CALL_END = "call_end_full_screen";
    public static final String CHANNEL_FCM_FULLSCREEN = "fcm_fullscreen_high";

    private CallEndFullscreenNotificationHelper() {
    }

    public static boolean canUseHighPriorityFullScreenIntent(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.canUseFullScreenIntent();
    }

    public static void ensureChannel(
            Context context,
            String channelId,
            CharSequence channelName,
            String description
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null || nm.getNotificationChannel(channelId) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(description);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLightColor(Color.BLUE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
    }

    /**
     * Call-end style: {@link NotificationCompat#CATEGORY_CALL}, missed-call icon.
     */
    public static void notifyCallEndStyle(
            Context context,
            Intent activityIntent,
            String formattedDuration,
            int notificationId
    ) {
        ensureChannel(
                context,
                CHANNEL_CALL_END,
                "Call end",
                "Shows call summary on lock screen (full-screen intent)"
        );
//        String big = "Tap to open the call end screen. Duration: " + formattedDuration;
        post(
                context,
                activityIntent,
                notificationId,
                CHANNEL_CALL_END,
                "See call Information",
                "",
                "",
                NotificationCompat.CATEGORY_CALL,
                android.R.drawable.sym_call_missed,
                null,
                true
        );
    }

//    public static void notifyCallEndNotificationOnly(
//            Context context,
//            Intent activityIntent,
//            String formattedDuration,
//            int notificationId
//    ) {
//        ensureChannel(
//                context,
//                CHANNEL_CALL_END,
//                "Call end",
//                "Shows call summary notification (no full-screen intent)"
//        );
//        String big = "Tap to open the call end screen. Duration: " + formattedDuration;
//        post(
//                context,
//                activityIntent,
//                notificationId,
//                CHANNEL_CALL_END,
//                "Call summary",
//                "Tap to view call details (" + formattedDuration + ")",
//                big,
//                NotificationCompat.CATEGORY_CALL,
//                android.R.drawable.sym_call_missed,
//                null,
//                false
//        );
//    }

    public static void notifyGenericFullscreen(
            Context context,
            Intent activityIntent,
            int notificationId,
            CharSequence contentTitle,
            CharSequence contentText,
            String bigText,
            int smallIcon,
            Uri soundUri
    ) {
        ensureChannel(
                context,
                CHANNEL_FCM_FULLSCREEN,
                "Push notifications",
                "High-priority alerts that may show over lock screen"
        );
        post(
                context,
                activityIntent,
                notificationId,
                CHANNEL_FCM_FULLSCREEN,
                contentTitle,
                contentText,
                bigText,
                NotificationCompat.CATEGORY_MESSAGE,
                smallIcon,
                soundUri,
                true
        );
    }

    /**
     * Generic notification WITHOUT full-screen intent (no UI on top of lock screen).
     */
//    public static void notifyGenericNotificationOnly(
//            Context context,
//            Intent activityIntent,
//            int notificationId,
//            CharSequence contentTitle,
//            CharSequence contentText,
//            String bigText,
//            int smallIcon,
//            Uri soundUri
//    ) {
//        ensureChannel(
//                context,
//                CHANNEL_FCM_FULLSCREEN,
//                "Push notifications",
//                "High-priority alerts (no full-screen intent)"
//        );
//        post(
//                context,
//                activityIntent,
//                notificationId,
//                CHANNEL_FCM_FULLSCREEN,
//                contentTitle,
//                contentText,
//                bigText,
//                NotificationCompat.CATEGORY_MESSAGE,
//                smallIcon,
//                soundUri,
//                false
//        );
//    }

    private static void post(
            Context context,
            Intent activityIntent,
            int notificationId,
            String channelId,
            CharSequence contentTitle,
            CharSequence contentText,
            String bigText,
            String category,
            int smallIcon,
            Uri soundUri,
            boolean useFullScreenIntent
    ) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) {
            return;
        }

        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent tapIntent = new Intent(activityIntent);
        PendingIntent contentPi = PendingIntent.getActivity(
                context, notificationId * 10 + 3, tapIntent, piFlags);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(smallIcon)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setAutoCancel(true)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(category)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentPi);

        if (useFullScreenIntent) {
            Intent fsIntent = new Intent(activityIntent);
            PendingIntent fullScreenPi = PendingIntent.getActivity(
                    context, notificationId * 10 + 2, fsIntent, piFlags);
            boolean highPriorityFs = canUseHighPriorityFullScreenIntent(context);
            b.setFullScreenIntent(fullScreenPi, highPriorityFs);
        }

        if (soundUri != null) {
            b.setSound(soundUri);
        }

        nm.notify(notificationId, b.build());
    }

    public static Uri defaultNotificationSound() {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }
}
