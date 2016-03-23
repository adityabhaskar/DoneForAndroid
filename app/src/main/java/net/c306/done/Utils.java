package net.c306.done;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.Map;

public class Utils {
    //<!--Save non-translating internal constant values here-->
    public static final String LOG_TAG = "AppMessage ";
    
    public static final String DONE_LOCAL_BROADCAST_LISTENER_INTENT = "net.c306.done.mainActivityListenerIntent";
    
    public static final int RESULT_ERROR = -1;
    public static final int MIN_LOCAL_DONE_ID = 100000000;
    
    //<!-- SharedPreferences string names -->
    public static final String AUTH_TOKEN = "authToken";
    public static final String DEFAULT_TEAM = "defaultTeam";
    public static final String TEAMS = "teams";
    public static final String VALID_TOKEN = "validToken";
    public static final String LAST_UPDATED_SETTING_NAME = "lastUpdated";
    public static final String USERNAME = "username";
    public static final String LOCAL_DONE_ID_COUNTER = "localDoneIdCounter";
    public static final String NEW_TASK_ACTIVITY_STATE = "newTaskActivityState";
    
    //<!-- Intent extra identifiers -->
    public static final String INTENT_EXTRA_FROM_DONE_DELETE_OR_EDIT_TASKS = "fromDoneDeleteOrEditTasks";
    public static final String INTENT_EXTRA_FETCH_TEAMS = "fetchTeams";
    public static final String ACTION_SHOW_AUTH = "showAuthScreen";
    
    //<!-- Local Broadcast identifier strings-->
    public static final int STATUS_TASK_STARTED = 19701;
    public static final int STATUS_TASK_SUCCESSFUL = 19702;
    public static final int STATUS_TASK_FAILED = 19703;
    public static final int STATUS_TASK_UNAUTH = 19704;
    public static final int STATUS_TASK_CANCELLED_OFFLINE = 19705;
    public static final int STATUS_TASK_OTHER_ERROR = 19706;
    
    public static final String SENDER_FETCH_TASKS = "senderFetchTasks";
    public static final String SENDER_FETCH_TEAMS = "senderFetchTeams";
    public static final String SENDER_CHECK_TOKEN = "senderCheckToken";
    public static final String SENDER_CREATE_TASK = "senderCreateTask";
    public static final String SENDER_EDIT_TASK = "senderEditTask";
    public static final String SENDER_DELETE_TASK = "senderDeleteTask";
    
    public static final int CHECK_TOKEN_STARTED = 19711;
    public static final int CHECK_TOKEN_SUCCESSFUL = 19712;
    public static final int CHECK_TOKEN_FAILED = 19713;
    public static final int CHECK_TOKEN_CANCELLED_OFFLINE = 19714;
    public static final int CHECK_TOKEN_OTHER_ERROR = 19715;
    
    //<!-- SyncAdapter related -->
    public static final int SYNC_INTERVAL = 15 * 60; // every 15 minutes
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    
    public static boolean haveValidToken(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getBoolean(Utils.VALID_TOKEN, false);
    }
    
    public static void setTokenValidity(Context c, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Utils.VALID_TOKEN, flag);
        editor.apply();
    }
    
    @Nullable
    public static String getAuthToken(Context c) {
        if (haveValidToken(c)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            return prefs.getString(Utils.AUTH_TOKEN, null);
        } else {
            Log.w(LOG_TAG + "Utils", "Get Auth Token: Didn't find valid token");
            return null;
        }
    }
    
    @Nullable
    public static String getAuthTokenWithoutValidityCheck(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(Utils.AUTH_TOKEN, null);
    }
    
    public static int getLocalDoneIdCounter(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getInt(Utils.LOCAL_DONE_ID_COUNTER, Utils.MIN_LOCAL_DONE_ID);
    }
    
    public static void setLocalDoneIdCounter(Context c, int localDoneIdCounter) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Utils.LOCAL_DONE_ID_COUNTER, localDoneIdCounter);
        editor.apply();
    }
    
    public static void setLastUpdated(Context c, String lastUpdated) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.LAST_UPDATED_SETTING_NAME, lastUpdated);
        editor.apply();
    }
    
/*
    @Nullable
    public static String getLastUpdated(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(Utils.LAST_UPDATED_SETTING_NAME, null);
    }
*/
    
    public static void setUsername(Context c, String username) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.USERNAME, username);
        editor.apply();
    }
    
    @Nullable
    public static String getUsername(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(Utils.USERNAME, null);
    }
    
    @Nullable
    public static String getDefaultTeam(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(Utils.DEFAULT_TEAM, null);
    }
    
    public static void setDefaultTeam(Context c, String team) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.DEFAULT_TEAM, team);
        editor.apply();
    }
    
    public static void setTeams(Context c, String[] teams) {
        if (teams.length > 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Utils.TEAMS, new Gson().toJson(teams));
            editor.apply();
        }
    }
    
    public static String[] getTeams(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return new Gson().fromJson(prefs.getString(Utils.TEAMS, "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static int findTeam(Context c, String team) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String[] teams = new Gson().fromJson(prefs.getString(Utils.TEAMS, "[]"), new TypeToken<String[]>() {
        }.getType());
        
        return Arrays.asList(teams).indexOf(team);
    }
    
    public static String[] getNewTaskActivityState(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return new Gson().fromJson(prefs.getString(Utils.NEW_TASK_ACTIVITY_STATE, "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static void setNewTaskActivityState(Context c, String[] state) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.NEW_TASK_ACTIVITY_STATE, new Gson().toJson(state));
        editor.apply();
    }
    
    public static void clearNewTaskActivityState(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Utils.NEW_TASK_ACTIVITY_STATE);
        editor.apply();
    }
    
    public static void sendMessage(Context mContext, String sender, String message, int action, int fetchedTaskCount) {
        //Log.v(LOG_TAG, sender + " :: " + message + " :: " + action + " :: " + fetchedTaskCount);
        Intent intent = new Intent(Utils.DONE_LOCAL_BROADCAST_LISTENER_INTENT);
        
        // You can also include some extra data.
        intent.putExtra("sender", sender);
        intent.putExtra("count", fetchedTaskCount);
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
    
    
    
    
    
    public static void resetSharedPreferences(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> prefsAll = prefs.getAll();
        
        for (Map.Entry<String, ?> entry :
                prefsAll.entrySet()) {
            editor.remove(entry.getKey());
        }
        
        editor.apply();
    }
    
}
