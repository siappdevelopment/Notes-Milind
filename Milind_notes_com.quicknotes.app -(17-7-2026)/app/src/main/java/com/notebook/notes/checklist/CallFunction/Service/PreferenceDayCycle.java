package com.notebook.notes.checklist.CallFunction.Service;

import android.content.Context;
import android.content.SharedPreferences;
import android.icu.util.Calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PreferenceDayCycle {
    private static final String PREF_NAME = "AppRetentionPrefe";
    private static final String CallEnd_KEY_FIRST_DAY = "first_day_player";
    private static final String CallEnd_KEY_DAY_COUNT = "day_count_player";
    private static final String Home_KEY_FIRST_DAY = "home_first_day_player";
    private static final String Home_KEY_DAY_COUNT = "home_day_count_player";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()); // Updated format

    public static void checkAndUpdateDayCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Calendar calendar = Calendar.getInstance();
        String currentDateStr = sdf.format(calendar.getTime());
        String firstDayStr = prefs.getString(CallEnd_KEY_FIRST_DAY, null);


        if (firstDayStr == null) {
            editor.putString(CallEnd_KEY_FIRST_DAY, currentDateStr);
            editor.putInt(CallEnd_KEY_DAY_COUNT, 1);
            editor.apply();
        } else {
            try {
                Date firstDate = sdf.parse(firstDayStr);
                Date currentDate = sdf.parse(currentDateStr);

                if (firstDate != null && currentDate != null) {
                    long diff = currentDate.getTime() - firstDate.getTime();
                    int daysPassed = (int) (diff / (1000 * 60 * 60 * 24));

                    if (daysPassed >= 1 && daysPassed <= 7) {
                        editor.putInt(CallEnd_KEY_DAY_COUNT, daysPassed);
                        editor.apply();
                    }

                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getDayCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(CallEnd_KEY_DAY_COUNT, 0);
    }

    public static void HomeCheckAndUpdateDayCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Calendar calendar = Calendar.getInstance();
        String currentDateStr = sdf.format(calendar.getTime());
        String firstDayStr = prefs.getString(Home_KEY_FIRST_DAY, null);


        if (firstDayStr == null) {
            editor.putString(Home_KEY_FIRST_DAY, currentDateStr);
            editor.putInt(Home_KEY_DAY_COUNT, 1);
            editor.apply();
        } else {
            try {
                Date firstDate = sdf.parse(firstDayStr);
                Date currentDate = sdf.parse(currentDateStr);

                if (firstDate != null && currentDate != null) {
                    long diff = currentDate.getTime() - firstDate.getTime();
                    int daysPassed = (int) (diff / (1000 * 60 * 60 * 24));

                    if (daysPassed >= 1 && daysPassed <= 7) {
                        editor.putInt(Home_KEY_DAY_COUNT, daysPassed);
                        editor.apply();
                    }

                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getHomeDayCount(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(Home_KEY_DAY_COUNT, 0);
    }


}
