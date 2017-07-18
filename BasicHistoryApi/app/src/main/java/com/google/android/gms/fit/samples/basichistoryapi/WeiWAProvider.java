package com.google.android.gms.fit.samples.basichistoryapi;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Random;

/**
 * Created by dirk on 17.07.2017.
 */

public class WeiWAProvider extends AppWidgetProvider {

    public static final String UPDATE_ACTION = "UPDATE_ACTION_WEIWA";
    private static final String ACTION_CLICK = "ACTION_CLICK";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){

        ComponentName thisWidget = new ComponentName(context, WeiWAProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
        for (int widgetId : allWidgetIds) {

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);

            remoteViews.setTextViewText(R.id.update, "Get The Data somehow!");

            Intent intent = new Intent(context, WeiWAProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }


    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        float averageWeightPrev = extras.getFloat("averageWeightPrev");
        float averageWeightPrevPrev = extras.getFloat("averageWeightPrevPrev");
        long refDate = extras.getLong("refDate");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(refDate);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, WeiWAProvider.class);
        int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        for (int widgetId : allWidgetIds) {

            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);

            remoteViews.setTextViewText(R.id.update,
                    "" + cal.get(Calendar.DAY_OF_MONTH) + "." + cal.get(Calendar.MONTH) + "." + cal.get(Calendar.YEAR) + "\n" +
                    "Änderung: " + (Math.round( (averageWeightPrev - averageWeightPrevPrev) * 100) / 100.0) + "kg");

            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

            /**
            if (action != null && action.equals(UPDATE_ACTION)) {
                final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName name = new ComponentName(context, WeiWAProvider.class);
                int[] appWidgetId = AppWidgetManager.getInstance(context).getAppWidgetIds(name);
                final int N = appWidgetId.length;
                if (N < 1)
                {
                    return ;
                }
                else {
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widgetlayout);
                    remoteViews.setTextViewText(R.id.update, "Get other Data somehow!");
                }
            }

            else {
                super.onReceive(context, intent);
            }
             */
    }



}
