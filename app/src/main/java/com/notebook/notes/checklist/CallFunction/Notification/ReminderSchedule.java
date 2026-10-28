package com.notebook.notes.checklist.CallFunction.Notification;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

public class ReminderSchedule {
    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleReminder(Context context, String txt, String title, Calendar calendar, int notificationId) {
        Intent intent = new Intent(context, RemindReceiver.class);
        intent.putExtra("txt", txt);
        intent.putExtra("title", title);
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }
}

