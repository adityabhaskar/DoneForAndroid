package net.c306.done;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.c306.done.db.DoneListContract;
import net.c306.done.notifications.NotificationsBroadcastReceiver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
public class Utils {
    
    /****
     * CONSTANTS FOR THE AUTHORIZATION PROCESS
     ****/
    
    // This is the public api key of our application
    public static final String CLIENT_ID = "";
    
    // This is the authorisation key for the header
    public static final String AUTH_HEADER = "";
    
    // This is any string we want to use. This will be used for avoid CSRF attacks. You can generate one here: http://strongpasswordgenerator.com/
    public static final String STATE = "";
    
    // This is the url that Auth process will redirect to. 
    // We can put whatever we want that starts with https:// .
    // We use a made up url that we will intercept when redirecting. Avoid Uppercases. 
    public static final String REDIRECT_URI = "";
    
    /*************************************************/
    
    // SyncAdapter related
    public static final int SYNC_DEFAULT_INTERVAL = 60 * 60; // every 15 minutes
    public static final int SYNC_SYNCABLE = 1;
    public static final int SYNC_NOT_SYNCABLE = 0;
    public static final int TASKS_TO_FETCH = 100;
    
    
    // Activity identifiers in startActivityForResult
    public static final int MAIN_ACTIVITY_IDENTIFIER = 7001;
    public static final int LOGIN_ACTIVITY_IDENTIFIER = 7002;
    public static final int NEW_DONE_ACTIVITY_IDENTIFIER = 7003;
    public static final int SETTINGS_ACTIVITY_IDENTIFIER = 7004;
    public static final int TASK_DETAILS_ACTIVITY_IDENTIFIER = 7005;
    
    // ACTIVITY RESULT STATUS
    public static final int LOGIN_FINISHED = 0;
    public static final int LOGIN_UNFINISHED = 1;
    public static final int LOGIN_CANCELLED_OR_ERROR = -1;
    public static final int RESULT_ERROR = -1;
    public static final int RESULT_TASK_DELETED = 29001;
    
    // Intent & extra identifiers
    public static final String INTENT_FROM_ACTIVITY_IDENTIFIER = "fromActivity";
    public static final String INTENT_EXTRA_FROM_DONE_DELETE_EDIT_TASKS = "fromDoneDeleteOrEditTasks";
    public static final String INTENT_EXTRA_FETCH_TEAMS = "fetchTeams";
    public static final String DONE_LOCAL_BROADCAST_LISTENER_INTENT = "net.c306.done.mainActivityListenerIntent";
    public static final String NOTIFICAION_ALARM_INTENT = "net.c306.done.notificationAlarms";
    public static final String NOTIFICAION_ALARM_SNOOZED_INTENT = "net.c306.done.notificationAlarms.Snoozed";
    public static final String INTENT_ACTION = "intent_action";
    public static final String INTENT_COUNT = "intent_count";
    public static final int ACTION_SNOOZE = 9001;
    public static final int ACTION_LOG_DONE = 9002;
    public static final int ACTION_OPEN_SETTINGS = 9003;
    public static final int ACTION_SHOW_AUTH = 9004;
    
    // IDT URLs
    public static final String IDT_NOOP_URL = "https://idonethis.com/api/v0.1/noop/";
    public static final String IDT_TEAM_URL = "https://idonethis.com/api/v0.1/teams/";
    public static final String IDT_DONE_URL = "https://idonethis.com/api/v0.1/dones/";
    public static final String IDT_ACCESS_TOKEN_URL = "https://idonethis.com/api/oauth2/token/";
    public static final String IDT_AUTHORIZATION_URL = "https://idonethis.com/api/oauth2/authorize/";
    
    // OTHER CONSTANTS
    public static final String LOG_TAG = "AppMessage ";
    public static final String AUTH_TOKEN = "authToken";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String EXPIRES_TOKEN = "expires";
    public static final String USER_DETAILS_PREFS_FILENAME = "user_info";
    public static final int MIN_LOCAL_DONE_ID = 100000000;
    
    // SharedPreferences property names 
    public static final String PREF_SYNC_FREQUENCY = "sync_frequency"; // Same value as @R/constants/PREF_SYNC_FREQUENCY
    public static final String PREF_SNOOZE_DURATION = "snooze_duration"; // Same value as @R/constants/PREF_SNOOZE_DURATION
    public static final String PREF_DEFAULT_TEAM = "defaultTeam";
    public static final String PREF_SYNC_ON_STARTUP = "sync_on_startup"; // Same value as @R/constants/PREF_SHOW_NOTIFICATION
    public static final String PREF_SHOW_NOTIFICATION = "show_notification"; // Same value as @R/constants/PREF_SHOW_NOTIFICATION
    public static final String PREF_NOTIFICATION_DAYS = "notification_days_of_week"; // Same value as @R/constants/PREF_NOTIFICATION_DAYS
    public static final String PREF_NOTIFICATION_TIME = "notification_time"; // Same value as @R/constants/PREF_NOTIFICATION_DAYS
    public static final String PREF_NOTIFICATION_SOUND = "notifications_new_message_ringtone";
    
    // User Preferences file property names
    public static final String TEAMS = "teams";
    public static final String VALID_TOKEN = "validToken";
    public static final String USERNAME = "username";
    public static final String LOCAL_DONE_ID_COUNTER = "localDoneIdCounter";
    public static final String NEW_TASK_ACTIVITY_STATE = "newTaskActivityState";
    
    // Default Notification Alarm Constants
    public static final boolean DEFAULT_SYNC_ON_STARTUP = false;
    public static final boolean DEFAULT_SHOW_NOTIFICATION = true;
    public static final String DEFAULT_ALARM_TIME = "19:00";
    public static final int DEFAULT_SNOOZE_IN_SECONDS = 15 * 60; // Same as android:defaultValue in pref_notification.xml > ListPreference
    public static final int NOTIFICATION_ID = 1320015027;
    public static final Set<String> DEFAULT_NOTIFICATION_DAYS = new HashSet(Arrays.asList("1", "2", "3", "4", "5", "6", "7"));
    public static final Set<String> WEEKDAY_VALUES = new HashSet(Arrays.asList("1", "2", "3", "4", "5"));
    public static final Set<String> WEEKEND_VALUES = new HashSet(Arrays.asList("6", "7"));
    
    //<!-- Local Broadcast identifier strings-->
    public static final int STATUS_TASK_STARTED = 19701;
    public static final int STATUS_TASK_SUCCESSFUL = 19702;
    public static final int STATUS_TASK_FAILED = 19703;
    public static final int STATUS_TASK_UNAUTH = 19704;
    public static final int STATUS_TASK_CANCELLED_OFFLINE = 19705;
    public static final int STATUS_TASK_OTHER_ERROR = 19706;
    public static final int TASK_DELETED_SNACKBAR = 19707;
    public static final int NEW_TASK_SAVED = 19708;
    public static final int EDITED_TASK_SAVED = 19709;
    
    public static final String SENDER_FETCH_TASKS = "senderFetchTasks";
    public static final String SENDER_FETCH_TEAMS = "senderFetchTeams";
    public static final String SENDER_CHECK_TOKEN = "senderCheckToken";
    public static final String SENDER_CREATE_TASK = "senderCreateTask";
    public static final String SENDER_EDIT_TASK = "senderEditTask";
    public static final String SENDER_DELETE_TASK = "senderDeleteTask";
    public static final String SENDER_TASK_DETAILS_ACTIVITY = "senderTaskDetailsActivity";
    public static final String SENDER_MAIN_ACTIVITY = "senderMainActivity";
    public static final String SENDER_NEW_DONE_ACTIVITY = "senderNewDoneActivity";
    
    public static final int CHECK_TOKEN_STARTED = 19811;
    public static final int CHECK_TOKEN_SUCCESSFUL = 19812;
    public static final int CHECK_TOKEN_FAILED = 19813;
    public static final int CHECK_TOKEN_CANCELLED_OFFLINE = 19814;
    public static final int CHECK_TOKEN_OTHER_ERROR = 19815;
    
    // TASK DETAILS FRAGMENT RELATED CONSTANTS
    public static final String KEY_SELECTED_TASK_ID = "task_id";
    public static final String KEY_FILTER_TITLE = "filter_string_to_display";
    public static final String KEY_SEARCH_FILTER = "search_filter_phrase";
    public static final String KEY_NAV_FILTER_TYPE = "nav_filter_type";
    public static final String KEY_NAV_FILTER = "nav_filter_query_string";
    
    // Constants to determine how to inflate layout
    public static final int NAV_LAYOUT_TEAMS = 1;
    public static final int NAV_LAYOUT_TAGS = 2;
    public static final int NAV_LAYOUT_ALL = 3;
    
    // For Analytics
    public static final String ANALYTICS_CATEGORY_ACTION = "Action";
    
    public static final int colorArray[] = {
            R.color.team1,
            R.color.team2,
            R.color.team3,
            R.color.team4,
            R.color.team5,
            R.color.team6,
            R.color.team7,
            R.color.team8,
            R.color.team9,
            R.color.team10
    };
    
    /**
    *           
    *           Methods start here
    *           
    * */
    
    public static int getSyncInterval(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        
        return Integer.parseInt(prefs.getString(
                Utils.PREF_SYNC_FREQUENCY,
                String.valueOf(Utils.SYNC_DEFAULT_INTERVAL)
        ));
    }
    
    public static int getSnoozeDuration(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        
        return Integer.parseInt(prefs.getString(
                Utils.PREF_SNOOZE_DURATION,
                String.valueOf(Utils.DEFAULT_SNOOZE_IN_SECONDS)
        ));
    }
    
    public static String getAlarmTime(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        
        return prefs.getString(
                Utils.PREF_NOTIFICATION_TIME,
                String.valueOf(Utils.DEFAULT_ALARM_TIME)
        );
    }
    
    public static String getRingtone(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        
        return prefs.getString(
                "notifications_new_message_ringtone",
                "content://settings/system/notification_sound"
        );
    }
    
    public static boolean getSyncOnStartup(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        
        return prefs.getBoolean(
                Utils.PREF_SYNC_ON_STARTUP,
                Utils.DEFAULT_SYNC_ON_STARTUP
        );
    }
    
    public static boolean getShowNotification(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        
        return prefs.getBoolean(
                Utils.PREF_SHOW_NOTIFICATION,
                Utils.DEFAULT_SHOW_NOTIFICATION
        );
    }
    
    public static Set<String> getNotificationDays(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        
        return prefs.getStringSet(
                Utils.PREF_NOTIFICATION_DAYS,
                Utils.DEFAULT_NOTIFICATION_DAYS
        );
    }
    
    // TODO: 06/04/16 Remove/Edit this function 
    public static void setTokenValidity(Context c, boolean flag) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Utils.VALID_TOKEN, flag);
        editor.apply();
    }
    
    @Nullable
    public static String getAccessToken(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        return prefs.getString(Utils.ACCESS_TOKEN, null);
    }
    
    public static void setAccessToken(Context c, String token) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.ACCESS_TOKEN, token);
        editor.apply();
    }
    
    @Nullable
    public static String getRefreshToken(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        return prefs.getString("refreshToken", null);
    }
    
    public static void setRefreshToken(Context c, String token) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("refreshToken", token);
        editor.apply();
    }
    
    public static boolean isTokenExpired(Context c) {
        long expiry = getExpiryTime(c);
        return new Date().after(
                new Date(expiry)
        );
    }
    
    public static long getExpiryTime(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        return prefs.getLong("expires", -1);
    }
    
    public static void setExpiryTime(Context c, long time) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("expires", time);
        editor.apply();
    }
    
    public static void setUsername(Context c, String username) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.USERNAME, username);
        editor.apply();
    }
    
    @Nullable
    public static String getUsername(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        return prefs.getString(Utils.USERNAME, null);
    }
    
    @Nullable
    public static String getDefaultTeam(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(Utils.PREF_DEFAULT_TEAM, null);
    }
    
    public static void setDefaultTeam(Context c, String team) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.PREF_DEFAULT_TEAM, team);
        editor.apply();
    }
    
    public static void setTeams(Context c, String[] teams) {
        if (teams.length > 0) {
            SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Utils.TEAMS, new Gson().toJson(teams));
            editor.apply();
        }
    }
    
    public static String[] getTeams(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        return new Gson().fromJson(prefs.getString(Utils.TEAMS, "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static int findTeam(Context c, String team) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        String[] teams = new Gson().fromJson(prefs.getString(Utils.TEAMS, "[]"), new TypeToken<String[]>() {
        }.getType());
        
        return Arrays.asList(teams).indexOf(team);
    }
    
    public static String[] getNewTaskActivityState(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        return new Gson().fromJson(prefs.getString(Utils.NEW_TASK_ACTIVITY_STATE, "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static void setNewTaskActivityState(Context c, String[] state) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.NEW_TASK_ACTIVITY_STATE, new Gson().toJson(state));
        editor.apply();
    }
    
    public static void clearNewTaskActivityState(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Utils.NEW_TASK_ACTIVITY_STATE);
        editor.apply();
    }
    
    public static int getLocalDoneIdCounter(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        return prefs.getInt(Utils.LOCAL_DONE_ID_COUNTER, Utils.MIN_LOCAL_DONE_ID);
    }
    
    public static void setLocalDoneIdCounter(Context c, int localDoneIdCounter) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Utils.LOCAL_DONE_ID_COUNTER, localDoneIdCounter);
        editor.apply();
    }
    
    public static void setNotificationAlarm(Context context, String when) {
        
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (alarmMgr == null) {
            // Something went wrong
            return;
        }
        
        Intent intent = new Intent(Utils.NOTIFICAION_ALARM_INTENT);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        String[] alarmTimeParts;
        
        alarmTimeParts = (
                when == null || when.isEmpty() ?
                        Utils.getAlarmTime(context) :
                        when
        ).split("\\:");
        
        Log.v(LOG_TAG, "Setting alarm for: " + alarmTimeParts[0] + ":" + alarmTimeParts[1]);
        
        // Set the alarm to start at alarmTime
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(alarmTimeParts[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(alarmTimeParts[1]));
        
        alarmMgr.setRepeating(
                AlarmManager.RTC_WAKEUP,        // Type of alarm 
                calendar.getTimeInMillis(),     // Wakeup time
                AlarmManager.INTERVAL_DAY,      // Interval
                alarmIntent                     // Intent to call
        );
        
        // Enable boot notification receiver to re-set the alarm
        ComponentName receiver = new ComponentName(context, NotificationsBroadcastReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        
    }
    
    public static void cancelNotificationAlarm(Context context) {
        
        clearNotification(context);
        
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (alarmMgr == null) {
            // Something went wrong
            return;
        }
        
        Intent intent = new Intent(Utils.NOTIFICAION_ALARM_INTENT);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        
        Log.v(LOG_TAG, "Cancelling notification alarm");
        
        // Cancel any alarms
        alarmMgr.cancel(alarmIntent);
        
        // Disable boot notification receiver
        ComponentName receiver = new ComponentName(context, NotificationsBroadcastReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
    
    public static void showNotification(Context context, Intent intent) {
        /*
        * Create notification builder
        * */
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_notification_done)
                        .setContentTitle(context.getString(R.string.new_done_hint))
                        .setTicker(context.getString(R.string.new_done_hint))
                        .setAutoCancel(true)
                        //.setPriority(Notification.PRIORITY_HIGH)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_splash_icon_120);
        mBuilder.setLargeIcon(bm);
        
        String ringtone = Utils.getRingtone(context);
        if (ringtone != null && !ringtone.isEmpty())
            mBuilder.setSound(Uri.parse(ringtone));
        
        
        /*
        * Get number of dones logged today
        * */
        String notificationText = context.getString(R.string.notification_text_zero);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(new Date());
        
        Cursor cursor = context.getContentResolver().query(
                DoneListContract.DoneEntry.CONTENT_URI,
                new String[]{DoneListContract.DoneEntry.COLUMN_NAME_ID},
                DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " IS '" + today + "'",
                null,
                null
        );
        
        if (cursor != null && cursor.getCount() > 0) {
            notificationText = cursor.getCount() + " " +
                    context.getString(
                            (cursor.getCount() > 1 ?
                                    R.string.notification_text_non_one :
                                    R.string.notification_text_one
                            )
                    );
            cursor.close();
        }
        
        //Log.v(LOG_TAG, "Setting notification text to " + notificationText);
        mBuilder.setContentText(notificationText);
        
        /*
        * Add main action for notification - add new done
        * */
        Intent newDoneIntent = new Intent(context, NewDoneActivity.class);
        
        TaskStackBuilder newDoneStackBuilder = TaskStackBuilder.create(context);
        newDoneStackBuilder.addParentStack(NewDoneActivity.class);
        newDoneStackBuilder.addNextIntent(newDoneIntent);
        
        PendingIntent resultPendingIntent = newDoneStackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);
        
        
        /*
        * Add Snooze intent
        * */
        Intent snoozeIntent = new Intent(Utils.NOTIFICAION_ALARM_SNOOZED_INTENT);
        snoozeIntent.putExtra(Utils.INTENT_ACTION, Utils.ACTION_SNOOZE);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, 0, snoozeIntent,
                PendingIntent.FLAG_CANCEL_CURRENT
        );
        
        mBuilder.addAction(new NotificationCompat.Action(
                R.drawable.ic_access_time_white_24dp,
                "Snooze",
                snoozePendingIntent
        ));
        
        
        /*
        * Add Settings intent
        * */
        Intent settingsIntent = new Intent(context, SettingsActivity.class);
        
        // Create backstack
        TaskStackBuilder settingsStackBuilder = TaskStackBuilder.create(context);
        settingsStackBuilder.addParentStack(SettingsActivity.class);
        settingsStackBuilder.addNextIntent(settingsIntent);
        
        PendingIntent settingsPendingIntent = settingsStackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        mBuilder.addAction(new NotificationCompat.Action(
                R.drawable.ic_settings_white_24dp,
                "Settings",
                settingsPendingIntent
        ));
        
        /*
        * Show notification
        * */
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        mNotificationManager.notify(Utils.NOTIFICATION_ID, mBuilder.build());
    }
    
    public static void clearNotification(Context context) {
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(Utils.NOTIFICATION_ID);
    }
    
    public static void snoozeNotification(Context context) {
        
        // 1. Dismiss current notification
        Utils.clearNotification(context);
        
        // 2. Add alarm for X minutes from now
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (alarmMgr == null) {
            // Something went wrong
            Log.w(LOG_TAG, "AlarmManager returned null");
            return;
        }
        
        int secondsToSnooze = Utils.getSnoozeDuration(context);
        
        Intent intent = new Intent(Utils.NOTIFICAION_ALARM_SNOOZED_INTENT);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        
        alarmMgr.cancel(alarmIntent);
        alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + secondsToSnooze * 1000,
                alarmIntent
        );
        
        // 3. Show Toast confirming 'Snoozed for X minutes'
        Toast.makeText(context,
                "Snoozed for " + secondsToSnooze / 60 + " minute" + (secondsToSnooze / 60 > 1 ? "s" : ""),
                Toast.LENGTH_SHORT)
                .show();
        Log.v(LOG_TAG, "Snoozing notification for " + secondsToSnooze / 60 + " minutes");
    }
    
    public static void sendMessage(Context mContext, String sender, String message, int action, int fetchedTaskCount) {
        Intent intent = new Intent(Utils.DONE_LOCAL_BROADCAST_LISTENER_INTENT);
        
        // You can also include some extra data.
        intent.putExtra("sender", sender);
        intent.putExtra(Utils.INTENT_COUNT, fetchedTaskCount);
        intent.putExtra("message", message);
        intent.putExtra("action", action);
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(intent);
    }
    
    public static boolean isOnline(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }
    
    public static String readResponse(HttpURLConnection httpcon) {
        String response;
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new InputStreamReader(
                            httpcon.getResponseCode() == HttpURLConnection.HTTP_OK ?
                                    httpcon.getInputStream() : httpcon.getErrorStream(),
                            "UTF-8")
            );
            
            String line;
            StringBuilder sb = new StringBuilder();
            
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            response = sb.toString();
            
        } catch (Exception e) {
            response = e.toString() + e.getMessage();
            Log.w(LOG_TAG, response);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        
        return response;
    }
    
    public static void resetSharedPreferences(Context c) {
        // Clear app settings
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear()
                .apply();
    
        // Set default values
        PreferenceManager.setDefaultValues(c, R.xml.preferences, true);
        
        // Delete user details shared prefs
        prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
        editor.clear()
                .apply();
        
    }
    
    public static void sendEvent(Tracker tracker, String category, String action) {
        sendEvent(tracker, category, action, null);
    }
    
    public static void sendEvent(Tracker tracker, String category, String action, String label) {
        if (tracker == null)
            return;
        
        if (category == null || category.isEmpty())
            category = Utils.ANALYTICS_CATEGORY_ACTION;
        
        HitBuilders.EventBuilder analyticsEvent = new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action);
        
        if (label != null && !label.isEmpty())
            analyticsEvent.setLabel(label);
        
        tracker.send(analyticsEvent.build());
    }
    
    public static void sendScreen(Tracker tracker, String screenName) {
        if (tracker == null)
            return;
        
        tracker.setScreenName("Image~" + screenName);
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }
}
