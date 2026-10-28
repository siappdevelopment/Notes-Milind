package com.notebook.notes.checklist.Advertisement;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.notebook.notes.checklist.R;

public class InstallDayWorker extends Worker {

    public InstallDayWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // દિવસો ગણો
        long days = ADSMainClass.getDaysSinceInstall();
        // Log.d("InstallDayWorker", "બેકગ્રાઉન્ડમાં દિવસ ચેક કર્યા: " + days);

        // ટેસ્ટિંગ માટે દરેક વખતે નોટિફિકેશન બતાવો
//        showNotification("WorkManager Testing", "Worker રન થયો છે! ઇન્સ્ટોલ દિવસ: " + days);

//        OneTimeWorkRequest nextWork = new OneTimeWorkRequest.Builder(InstallDayWorker.class)
//                .setInitialDelay(1, TimeUnit.MINUTES)
//                .build();
//        WorkManager.getInstance(getApplicationContext()).enqueue(nextWork);


//        if (days == 8) {
//            performEightDayTask();
//        }

        return Result.success();
    }

//    private void performEightDayTask() {
//        Log.d("InstallDayWorker", "૮મો દિવસ છે! બેકગ્રાઉન્ડ ટાસ્ક શરૂ થાય છે...");
//    }

    private void showNotification(String title, String message) {
        android.app.NotificationManager notificationManager = (android.app.NotificationManager)
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "testing_channel";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId, "Testing", android.app.NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(
                getApplicationContext(), channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
