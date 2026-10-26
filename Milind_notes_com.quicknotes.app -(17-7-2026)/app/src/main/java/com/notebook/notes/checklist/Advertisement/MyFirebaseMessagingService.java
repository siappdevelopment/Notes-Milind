package com.notebook.notes.checklist.Advertisement;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.notebook.notes.checklist.Advertisement.CallendClass.CallEndLaunchHelper;
import com.notebook.notes.checklist.R;
import com.notebook.notes.checklist.di.GlobalApp;


import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("MyFCMService", "onMessageReceived: from " + remoteMessage.getFrom());
        // Message handling is moved to handleIntent.
    }

    @Override
    public void handleIntent(Intent intent) {
        Log.d("MyFCMService", "handleIntent: received intent");
        Map<String, String> data = null;
        RemoteMessage remoteMessage = null;
        if (intent != null && intent.getExtras() != null) {
            remoteMessage = new RemoteMessage(intent.getExtras());
        }

        if (remoteMessage != null) {
            Log.d("MyFCMService", "handleIntent: RemoteMessage found");
            // 1. Check if App is in Foreground (Using ProcessLifecycleObserver)
            if (GlobalApp._isAppInForeground) {
                Log.d("MyFCMService", "handleIntent: App is in foreground, returning");
                return;
            }

            // 2. Check if Device is Unlocked
            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null && !km.isKeyguardLocked()) {
                Log.d("MyFCMService", "handleIntent: Device is unlocked, returning");
                return;
            }

            if (!ADSMainClass.isCallEndPerformanceAllowed(this, true)) {
                Log.d("MyFCMService", "handleIntent: CallEnd performance NOT allowed, returning");
                return;
            }

            String nativeId = ADSMainClass.getStringValue(ADSMainClass.CALL_END_Native);
            Log.d("MyFCMService", "handleIntent: Preloading native ad with ID: " + nativeId);
            final Map<String, String> finalData = data;
            final RemoteMessage finalRemoteMessage = remoteMessage;
            
            ADSNativeFullDisplay.preloadNativeAd(this, nativeId, new ADSNativeFullDisplay.PreloadCallback() {
                @Override
                public void onAdLoaded() {
                    Log.d("MyFCMService", "preloadNativeAd: onAdLoaded");
                    launchCallEnd(finalData, finalRemoteMessage);
                }

                @Override
                public void onAdFailed() {
                    Log.d("MyFCMService", "preloadNativeAd: onAdFailed");
                }
            });
            return;
        }

        Log.d("MyFCMService", "handleIntent: RemoteMessage is null, calling super");
        super.handleIntent(intent);
    }

    private void launchCallEnd(Map<String, String> data, RemoteMessage remoteMessage) {
        Log.d("MyFCMService", "launchCallEnd: triggered");
        if (CallEndLaunchHelper.tryOpenFromFcmData(this, data)) {


            Log.d("MyFCMService", "launchCallEnd: Opened from FCM data");
            return;
        }

        if (remoteMessage.getNotification() != null) {
            Log.d("MyFCMService", "launchCallEnd: Opening from Notification data");
            CallEndLaunchHelper.openGenericForFirebaseNotification(
                    this,
                    remoteMessage.getNotification().getTitle(),
                    remoteMessage.getNotification().getBody());
            return;
        }

        Log.d("MyFCMService", "launchCallEnd: Opening generic notification");
        CallEndLaunchHelper.openGenericForFirebaseNotification(
                this,
                getString(R.string.app_name),
                "");
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d("MyFCMService", "onNewToken: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        Log.d("MyFCMService", "sendRegistrationToServer: " + token);
    }
}
