package com.awwstream.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A {@link BroadcastReceiver} that receiving reminder.
 */
public final class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if ("com.awwstream.android.ALARM".equals(action)) {
            final Intent activityIntent = new Intent(context, EmptyActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);

            // Schedule another alarm the next day.
            Utils.setAlarm(context, 1);
        } else {
            // Device just booted, schedule an alarm in 2 weeks.
            Utils.setAlarm(context, 14);
        }
    }
}
