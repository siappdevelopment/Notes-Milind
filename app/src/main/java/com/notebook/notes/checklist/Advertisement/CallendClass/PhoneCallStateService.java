package com.notebook.notes.checklist.Advertisement.CallendClass;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.notebook.notes.checklist.Advertisement.ADSAppManage;
import com.notebook.notes.checklist.Advertisement.ADSMainClass;

import java.util.Date;


public class PhoneCallStateService extends BroadcastReceiver {
    private static final String TAG = "PhoneCallStateService";

    public static boolean isIncomingCallSend = false;
    static boolean isIncomingCallMain = false;
    static boolean isMissCalls = false;
    static boolean isOutgoingCall = false;
    static boolean isRingingCall = false;
    static boolean isShowScreenCall = false;
    static String outgoingNumber;
    protected Context savedContext;
    Date callStartTime = new Date();

    private void sendToMixpanel(String str) {
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        ADSAppManage.dialogboolean = false;
        this.savedContext = context;
        if ("android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            try {
                String stringExtra = intent.getStringExtra("state");
                String str = outgoingNumber;
                if (str == null || str.length() == 0) {
                    outgoingNumber = intent.getStringExtra("incoming_number");

                }
                if (TelephonyManager.EXTRA_STATE_IDLE.equals(stringExtra)) {

                    if (!isIncomingCallMain && isRingingCall) {
                        isMissCalls = true;
                        isIncomingCallMain = false;
                        isOutgoingCall = false;
                    }
                    String str2 = isIncomingCallMain ? "Incoming" : isMissCalls ? "Missed Call" : "Outgoing";
                    if (!isShowScreenCall) {
                        sendToMixpanel("Call_End");
                        openNewActivity(context, outgoingNumber, this.callStartTime, new Date(), str2);
                        isShowScreenCall = true;


                        return;
                    }
                    outgoingNumber = null;
                } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(stringExtra)) {
                    this.callStartTime = new Date();
                    if (!isRingingCall) {
                        isOutgoingCall = true;
                    } else {
                        isIncomingCallSend = true;
                        sendToMixpanel("Incoming_Call");
                        isIncomingCallMain = true;
                    }
                    isShowScreenCall = false;
                } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(stringExtra)) {
                    this.callStartTime = new Date();
                    isRingingCall = true;
                    isShowScreenCall = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private void openNewActivity(Context context, String str, Date date, Date date2, String str2) {
        if (isIncomingCallSend) {
            sendToMixpanel("Request_to_native_ad_load");
        }
        if (!ADSMainClass.getIsShowCallEnd()) {
            outgoingNumber = null;
            isMissCalls = false;
            isIncomingCallMain = false;
            isOutgoingCall = false;
            return;
        }

        CallEndLaunchHelper.openAfterCallEnded(context, str, date, date2, str2);

        outgoingNumber = null;
        isMissCalls = false;
        isIncomingCallMain = false;
        isOutgoingCall = false;
    }
}
