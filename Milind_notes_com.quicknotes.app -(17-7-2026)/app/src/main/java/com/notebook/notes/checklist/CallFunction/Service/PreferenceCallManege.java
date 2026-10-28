package com.notebook.notes.checklist.CallFunction.Service;

import android.content.Context;
import android.content.SharedPreferences;


import com.notebook.notes.checklist.R;

import javax.inject.Singleton;

import kotlin.Unit;
import kotlin.jvm.internal.DefaultConstructorMarker;


public final class PreferenceCallManege {
    public static final Companion CompanionMain = new Companion(null);
    public static volatile PreferenceCallManege preferenceCallManager;
    private SharedPreferences.Editor myEdit;
    private SharedPreferences sharedPreferences;

    public PreferenceCallManege(Context context, DefaultConstructorMarker defaultConstructorMarker) {
        this(context);
    }

    public static PreferenceCallManege getInstance(Context context) {
        return CompanionMain.getInstance(context);
    }

    @Singleton
    private PreferenceCallManege(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getResources().getString(R.string.app_name), 0);
        this.sharedPreferences = sharedPreferences;
        SharedPreferences.Editor edit = sharedPreferences.edit();
        this.myEdit = edit;
        edit.apply();
    }

    public final boolean getMissedCall() {
        return this.sharedPreferences.getBoolean("Missed Call", true);
    }

    public final boolean getIncoming() {
        return this.sharedPreferences.getBoolean("Incoming", true);
    }

    public final boolean getOutgoing() {
        return this.sharedPreferences.getBoolean("Outgoing", true);
    }

    
    public static final class Companion {
        public Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        public PreferenceCallManege getInstance(Context context) {
            if (PreferenceCallManege.preferenceCallManager == null) {
                synchronized (this) {
                    Companion companion = PreferenceCallManege.CompanionMain;
                    DefaultConstructorMarker defaultConstructorMarker = null;
                    PreferenceCallManege.preferenceCallManager = new PreferenceCallManege(context, null);
                    Unit unit = Unit.INSTANCE;
                }
            }
            return PreferenceCallManege.preferenceCallManager;
        }
    }
}
