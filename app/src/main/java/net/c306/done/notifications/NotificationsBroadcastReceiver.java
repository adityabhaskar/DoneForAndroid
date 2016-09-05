package net.c306.done.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import net.c306.done.Utils;
import net.c306.done.sync.IDTSyncAdapter;

import java.util.Calendar;
import java.util.Set;

public class NotificationsBroadcastReceiver extends BroadcastReceiver {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    
    @Override
    public void onReceive(Context context, Intent intent) {
        
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // 1a. From Boot completed - re-set the alarm here
            Log.i(LOG_TAG, "onReceive: Device restarted");
            Utils.setNotificationAlarm(context, null);
    
        } else if (intent.getAction().equals("android.intent.action.MY_PACKAGE_REPLACED")) {
            // 1b. From Application updated - re-set the alarm here
            Log.i(LOG_TAG, "onReceive: App updated");
            Utils.setNotificationAlarm(context, null);
    
            if (!Utils.getNotificationSeen(context)) {
                Log.i(LOG_TAG, "onReceive: show EoS post notification");
                Utils.showEndOfServiceNotification(context);
            }
    
        } else if (intent.getAction().equals(Utils.NOTIFICAION_EOS_CANCELLED_INTENT)) {
            // 1c. From EoS notification clicked on and cancelled
            if (intent.hasExtra(Utils.INTENT_ACTION)) {
                switch (intent.getIntExtra(Utils.INTENT_ACTION, -1)) {
                    case Utils.ACTION_OPEN_EOS_POST: {
                        Log.i(LOG_TAG, "onReceive: Notification opened from action, so dismiss it");
                        Utils.setNotificationSeen(context, true);
                        Utils.clearNotification(context, Utils.EOS_NOTIFICATION_ID);
                
                        Intent openBrowserPost = new Intent(Intent.ACTION_VIEW);
                        openBrowserPost.setData(Uri.parse(Utils.EOS_POST_URL));
                        openBrowserPost.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(openBrowserPost);
                
                        break;
                    }
            
                    //case Utils.ACTION_NOTIFICATION_DELETED: {
                    //    Log.i(LOG_TAG, "onReceive: Notification opened and dismissed");
                    //    Utils.setNotificationSeen(context, false);
                    //    break;
                    //}
                    //
                    default: {
                        Log.i(LOG_TAG, "onReceive: Unexpected action from notification " + Utils.NOTIFICAION_EOS_CANCELLED_INTENT);
                    }
                }
            }
        
            
        } else
            // 2. From NotificationAlarm
    
            if (intent.hasExtra(Utils.INTENT_ACTION) &&
                intent.getIntExtra(Utils.INTENT_ACTION, -1) == Utils.ACTION_SNOOZE) {
            // 2a. From snooze button
            Utils.snoozeNotification(context);
        
            } else {
            // 2b. From alarm called - show notification if allowed
        
                if (showNotificationToday(context))
                IDTSyncAdapter.syncImmediately(context, false, false, true);
            else
                Log.v(LOG_TAG, "User doesn't want any notifications today.");
        
            }
    }
    
    private boolean showNotificationToday(Context context) {
        // Check if today is allowed under notification_days
        Set<String> notificationDays = Utils.getNotificationDays(context);
        
        // Get today's day of week
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Subtract 1 to go from Sunday starting to Monday starting
        
        // If day is 0, i.e. Sunday, set it to 7
        today = today == 0 ? 7 : today;
        
        return notificationDays.contains(String.valueOf(today));
    }
}
