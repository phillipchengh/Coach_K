package com.coachksrun.Notifications;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.app.PendingIntent;
import android.R.drawable;

public class NotificationManager
{
    public void notify(String message, Activity callerActivity)
    {
        int notificationId = 001;
        // Build intent for notification content

        //Intent viewIntent = new Intent(callerActivity, callerActivity.getClass());
        Intent viewIntent = new Intent();
        //viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
        PendingIntent viewPendingIntent =
            PendingIntent.getActivity(callerActivity, 0, viewIntent, 0);

        long[] pattern = {0, 100, 200, 300};

        NotificationCompat.Builder notificationBuilder = 
            new NotificationCompat.Builder(callerActivity)
            .setSmallIcon(drawable.ic_dialog_alert)
            .setContentTitle("Coach K's Run")
            .setContentText(message)
            .setContentIntent(viewPendingIntent)
            .setVibrate(pattern)
            .setAutoCancel(true);

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
            NotificationManagerCompat.from(callerActivity);

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
