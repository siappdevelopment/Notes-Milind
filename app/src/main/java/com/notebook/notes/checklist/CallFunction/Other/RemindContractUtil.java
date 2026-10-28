package com.notebook.notes.checklist.CallFunction.Other;

import android.net.Uri;
import android.provider.BaseColumns;


public final class RemindContractUtil {

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://com.notebook.notes.checklist.provider");
    public static final String CONTENT_AUTHORITY = "com.notebook.notes.checklist.provider";
    public static final String PATH_REMINDER = "reminder";

    public static final class ReminderEntry implements BaseColumns {
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_COLOR = "color";
        public static final String COLUMN_REMINDER_DATE_TIME = "date_time";
        public static final String COLUMN_REMINDER_DONE = "reminder_done";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.notebook.notes.checklist.provider/reminder";
        public static final String CONTENT_LIST_TYPE = "vnd.android.cursor.dir/com.notebook.notes.checklist.provider/reminder";
        public static final Uri CONTENT_URI = Uri.withAppendedPath(RemindContractUtil.BASE_CONTENT_URI, RemindContractUtil.PATH_REMINDER);
        public static final String TABLE_NAME = "reminderTable";
    }

    private RemindContractUtil() {
    }
}
