package com.notebook.notes.checklist.CallFunction.Model;

import android.database.Cursor;


import com.notebook.notes.checklist.CallFunction.Other.RemindContractUtil;

import java.util.Calendar;


public class ReminderStrModel {

    private Long reminderId;
    private String reminderTitle;
    private String color;

    private Calendar reminderDateTime;
    private Boolean reminderDone;

    public static ReminderStrModel fromCursor(Cursor cursor) {
        ReminderStrModel reminderModel = new ReminderStrModel();
        reminderModel.setReminderId(Long.valueOf(cursor.getLong(cursor.getColumnIndex("_id"))));
        reminderModel.setReminderTitle(cursor.getString(cursor.getColumnIndex("title")));
        reminderModel.setColor(cursor.getString(cursor.getColumnIndex("color")));
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DATE_TIME)));
        reminderModel.setReminderDateTime(calendar2);
        if (cursor.getInt(cursor.getColumnIndex(RemindContractUtil.ReminderEntry.COLUMN_REMINDER_DONE)) == 0) {
            reminderModel.setreminderDone(false);
        } else {
            reminderModel.setreminderDone(true);
        }
        return reminderModel;
    }

    public Long getReminderId() {
        return reminderId;
    }

    public void setReminderId(Long reminderId) {
        this.reminderId = reminderId;
    }

    public String getReminderTitle() {
        return reminderTitle;
    }

    public void setReminderTitle(String reminderTitle) {
        this.reminderTitle = reminderTitle;
    }

    public Calendar getReminderDateTime() {
        return reminderDateTime;
    }

    public void setReminderDateTime(Calendar reminderDateTime) {
        this.reminderDateTime = reminderDateTime;
    }

    public Boolean getreminderDone() {
        return reminderDone;
    }

    public void setreminderDone(Boolean done) {
        this.reminderDone = done;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

}
