package net.c306.done.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import net.c306.done.Utils;

import java.util.Calendar;
import java.util.Set;

public class NotificationsBroadcastReceiver extends BroadcastReceiver {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(LOG_TAG, "Received Notification Alarm");
        Log.v(LOG_TAG, "Intent Extra: " + (intent.hasExtra(Utils.INTENT_ACTION) ? intent.getIntExtra(Utils.INTENT_ACTION, -1) : "none"));
        Log.v(LOG_TAG, "Action: " + intent.getAction());
    
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // 1. From Boot completed - re-set the alarm here
            Utils.setNotificationAlarm(context, null);
        
        } else
            // 2. From NotificationAlarm
        
        if (intent.hasExtra(Utils.INTENT_ACTION) &&
                intent.getIntExtra(Utils.INTENT_ACTION, -1) == Utils.ACTION_SNOOZE) {
            // 2a. From snooze button
            Utils.snoozeNotification(context);
            
        } else {
            // 2b. From alarm called - show notification if allowed
            
            // Check if today is allowed under notification_days
            Set<String> notificationDays = Utils.getNotificationDays(context);
            
            Calendar calendar = Calendar.getInstance();
            int day = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Subtract 1 to go from Sunday starting to Monday starting
            
            if (notificationDays.contains(String.valueOf(day)))
                Utils.showNotification(context, intent);
            else
                Log.v(LOG_TAG, "User doesn't want any notifications today.");
        }
    }
}