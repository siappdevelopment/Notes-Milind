package com.notebook.notes.checklist.Advertisement;


import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesClass {

    private static SharedPreferencesClass instance;
    private final SharedPreferences settings;
    private final SharedPreferences.Editor editor;

    private SharedPreferencesClass(Context context) {
        Context appContext = context.getApplicationContext();
        settings = appContext.getSharedPreferences("MY_PREFERANCE", Context.MODE_PRIVATE);
        editor = settings.edit();
    }

    public static SharedPreferencesClass getInstance() {
        if (instance == null) {
            Context context = ADSMainClass.getAppContext();
            if (context == null) {
                throw new IllegalStateException("Application context not initialized");
            }
            instance = new SharedPreferencesClass(context);
        }
        return instance;
    }

    public static SharedPreferencesClass getInstance(Context context) {
        if (context != null) {
            ADSMainClass.initAppContext(context);
        }
        return getInstance();
    }

    public String getString(String key, String defValue) {

        return settings.getString(key, defValue);
    }

    public SharedPreferencesClass setString(String key, String value) {

        editor.putString(key, value);
        editor.commit();

        return this;
    }

    public SharedPreferencesClass setStatus(String key, boolean value) {

        editor.putBoolean(key, value);
        editor.commit();

        return this;
    }

    public int getInt(String key, int defValue) {

        return settings.getInt(key, defValue);
    }

    public SharedPreferencesClass setInt(String key, int value) {

        editor.putInt(key, value);
        editor.commit();

        return this;
    }

    public boolean getBoolean(String key, boolean defValue) {

        return settings.getBoolean(key, defValue);
    }

    public SharedPreferencesClass setBoolean(String key, boolean value) {

        editor.putBoolean(key, value);
        editor.commit();

        return this;
    }

    public SharedPreferencesClass setLong(String key, long value) {

        editor.putLong(key, value);
        editor.commit();

        return this;
    }

    public long getLong(String key, long defValue) {

        return settings.getLong(key, defValue);
    }

    public void clearData() {

        editor.clear();
        editor.commit();
    }

}
