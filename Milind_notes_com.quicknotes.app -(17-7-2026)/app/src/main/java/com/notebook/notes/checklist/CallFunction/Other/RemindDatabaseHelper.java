package com.notebook.notes.checklist.CallFunction.Other;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RemindDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "reminder_list_database.db";
    private static final int DATABASE_VERSION = 1;

    public RemindDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + RemindContractUtil.ReminderEntry.TABLE_NAME + " ("
                + RemindContractUtil.ReminderEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + RemindContractUtil.ReminderEntry.COLUMN_TITLE + " TEXT NOT NULL, "
                + RemindContractUtil.ReminderEntry.COLUMN_COLOR + " TEXT NOT NULL, "
                + RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DATE_TIME + " INTEGER, "
                + RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DONE + " INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
