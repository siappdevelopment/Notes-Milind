package com.notebook.notes.checklist.CallFunction.Other;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;



public class ReminderProviderWidget extends AppWidgetProvider {

    @Override
    public void onDisabled(Context context) {

    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if ("android.appwidget.action.APPWIDGET_UPDATE".equals(intent.getAction())) {
            refreshWidgetData(context);
        }
    }

    private void refreshWidgetData(Context context) {

    }
}
