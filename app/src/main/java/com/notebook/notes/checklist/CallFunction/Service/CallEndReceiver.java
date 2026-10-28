package com.notebook.notes.checklist.CallFunction.Service;//package com.alarmclock.reminder.clock.CallFunction.Service;
//
//import android.content.Intent;
//import android.os.Handler;
//import android.os.Looper;
//import android.telecom.Call;
//import android.telecom.CallScreeningService;
//
//public class CallEndReceiver extends CallScreeningService {
//
//    @Override
//    public void onScreenCall(Call.Details details) {
//        String uri = details.getHandle() != null ? details.getHandle().toString() : "";
//        CallResponse.Builder builder = new CallResponse.Builder();
//        startCallerScreen(uri, "", "0");
//        respondToCall(details, builder.build());
//    }
//
//    private void startCallerScreen(final String str, final String str2, final String str3) {
//        try {
//            Looper myLooper = Looper.myLooper();
//            if (myLooper != null) {
//                new Handler(myLooper).postDelayed(new Runnable() { //.SimpleCallScreeningService$$ExternalSyntheticLambda0
//                    @Override
//                    public final void run() {
//                        CallEndReceiver.this.showCallIdTheme(str, str2, str3);
//                    }
//                }, 1000L);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//     void showCallIdTheme(String str, String str2, String str3) {
//    }
//
//
//
//    @Override
//    public void onTaskRemoved(Intent intent) {
//        super.onTaskRemoved(intent);
//    }
//
//
//}
