package com.notebook.notes.checklist.CallFunction.Other;

import android.content.Context;
import android.content.Intent;

public class ReminderUtilMain {

    public static void refreshWidget(Context context) {
        Intent updateIntent = new Intent(context, ReminderProviderWidget.class);
        updateIntent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        context.sendBroadcast(updateIntent);
    }
}
