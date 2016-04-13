package net.c306.done.notificationalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import net.c306.done.Utils;

public class BootAlarmReceiver extends BroadcastReceiver {
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            // Set the alarm here.
            Utils.setNotificationAlarm(context, null);
        }
    }
}
