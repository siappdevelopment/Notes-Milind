package com.notebook.notes.checklist.CallFunction.Other;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class RemindProviderMain extends ContentProvider {

    private static final UriMatcher URI_MATCHER;
    private static final int CODE_REMINDERS = 1001;
    private static final int CODE_REMINDER_ID = 1002;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(RemindContractUtil.CONTENT_AUTHORITY, RemindContractUtil.PATH_REMINDER, CODE_REMINDERS);
        URI_MATCHER.addURI(RemindContractUtil.CONTENT_AUTHORITY, RemindContractUtil.PATH_REMINDER + "/#", CODE_REMINDER_ID);
    }

    private RemindDatabaseHelper databaseHelper;

    @Override
    public boolean onCreate() {
        databaseHelper = new RemindDatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor cursor;

        switch (URI_MATCHER.match(uri)) {
            case CODE_REMINDERS:
                cursor = db.query(RemindContractUtil.ReminderEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case CODE_REMINDER_ID:
                selection = RemindContractUtil.ReminderEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(RemindContractUtil.ReminderEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if (URI_MATCHER.match(uri) != CODE_REMINDERS) {
            throw new IllegalArgumentException("Insertion not supported for URI: " + uri);
        }

        validateContentValues(values);

        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        long id = db.insert(RemindContractUtil.ReminderEntry.TABLE_NAME, null, values);

        if (id == -1) {
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int rowsUpdated;
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        switch (URI_MATCHER.match(uri)) {
            case CODE_REMINDERS:
                rowsUpdated = db.update(RemindContractUtil.ReminderEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case CODE_REMINDER_ID:
                selection = RemindContractUtil.ReminderEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsUpdated = db.update(RemindContractUtil.ReminderEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Update not supported for URI: " + uri);
        }

        if (rowsUpdated > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted;
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        switch (URI_MATCHER.match(uri)) {
            case CODE_REMINDERS:
                rowsDeleted = db.delete(RemindContractUtil.ReminderEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CODE_REMINDER_ID:
                selection = RemindContractUtil.ReminderEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = db.delete(RemindContractUtil.ReminderEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Delete not supported for URI: " + uri);
        }

        if (rowsDeleted > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case CODE_REMINDERS:
                return RemindContractUtil.ReminderEntry.CONTENT_LIST_TYPE;
            case CODE_REMINDER_ID:
                return RemindContractUtil.ReminderEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI: " + uri);
        }
    }

    private void validateContentValues(ContentValues values) {
        if (values.containsKey(RemindContractUtil.ReminderEntry.COLUMN_TITLE)) {
            String title = values.getAsString(RemindContractUtil.ReminderEntry.COLUMN_TITLE);
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Reminder requires a valid title");
            }
        }

        if (values.containsKey(RemindContractUtil.ReminderEntry.COLUMN_COLOR)) {
            String color = values.getAsString(RemindContractUtil.ReminderEntry.COLUMN_COLOR);
            if (color == null || color.trim().isEmpty()) {
                throw new IllegalArgumentException("Reminder requires a valid title");
            }
        }


        if (values.containsKey(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DATE_TIME)) {
            Long dateTime = values.getAsLong(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DATE_TIME);
            if (dateTime == null || dateTime <= 0) {
                throw new IllegalArgumentException("Reminder requires a valid date and time");
            }
        }

        if (values.containsKey(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DONE)) {
            Integer isDone = values.getAsInteger(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DONE);
            if (isDone == null || (isDone != 0 && isDone != 1)) {
                throw new IllegalArgumentException("Reminder requires a valid completion status");
            }
        }
    }
}
