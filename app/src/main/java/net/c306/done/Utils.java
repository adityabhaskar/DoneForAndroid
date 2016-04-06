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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

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
    public static final int SYNC_INTERVAL = 15 * 60; // every 15 minutes
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    public static final int TASKS_TO_FETCH = 100;
    
    // Activity identifiers in startActivityForResult
    public static final int MAIN_ACTIVITY_IDENTIFIER = 7001;
    public static final int LOGIN_ACTIVITY_IDENTIFIER = 7002;
    public static final int NEW_DONE_ACTIVITY_IDENTIFIER = 7003;
    public static final int SETTINGS_ACTIVITY_IDENTIFIER = 7004;
    
    // ACTIVITY RESULT STATUS
    public static final int LOGIN_FINISHED = 0;
    public static final int LOGIN_UNFINISHED = 1;
    public static final int LOGIN_CANCELLED_OR_ERROR = -1;
    public static final int RESULT_ERROR = -1;
    
    // Intent & extra identifiers
    public static final String INTENT_EXTRA_FROM_DONE_DELETE_EDIT_TASKS = "fromDoneDeleteOrEditTasks";
    public static final String INTENT_EXTRA_FETCH_TEAMS = "fetchTeams";
    public static final String ACTION_SHOW_AUTH = "showAuthScreen";
    public static final String DONE_LOCAL_BROADCAST_LISTENER_INTENT = "net.c306.done.mainActivityListenerIntent";
    
    // IDT URLs
    public static final String IDT_NOOP_URL = "https://idonethis.com/api/v0.1/noop/";
    public static final String IDT_TEAM_URL = "https://idonethis.com/api/v0.1/teams/";
    public static final String IDT_DONE_URL = "https://idonethis.com/api/v0.1/dones/";
    public static final String IDT_ACCESS_TOKEN_URL = "https://idonethis.com/api/oauth2/token/";
    public static final String IDT_AUTHORIZATION_URL = "https://idonethis.com/api/oauth2/authorize/";
    
    // OTHER CONSTANTS
    public static final String LOG_TAG = "AppMessage ";
    public static final String USER_DETAILS_PREFS_FILENAME = "user_info";
    public static final int MIN_LOCAL_DONE_ID = 100000000;
    
    // SharedPreferences property names 
    public static final String AUTH_TOKEN = "authToken";
    public static final String DEFAULT_TEAM = "defaultTeam";
    public static final String TEAMS = "teams";
    public static final String VALID_TOKEN = "validToken";
    public static final String USERNAME = "username";
    public static final String LOCAL_DONE_ID_COUNTER = "localDoneIdCounter";
    public static final String NEW_TASK_ACTIVITY_STATE = "newTaskActivityState";
    
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
    
    
    // TODO: 06/04/16 Remove/Edit this function 
    public static void setTokenValidity(Context c, boolean flag) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Utils.VALID_TOKEN, flag);
        editor.apply();
    }
    
    @Nullable
    public static String getAccessToken(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        return prefs.getString("accessToken", null);
    }
    
    public static void setAccessToken(Context c, String token) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("accessToken", token);
        editor.apply();
    }
    
    @Nullable
    public static String getRefreshToken(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        return prefs.getString("refreshToken", null);
    }
    
    public static void setRefreshToken(Context c, String token) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
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
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        return prefs.getLong("expires", -1);
    }
    
    public static void setExpiryTime(Context c, long time) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("expires", time);
        editor.apply();
    }
    
    public static void setUsername(Context c, String username) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.USERNAME, username);
        editor.apply();
    }
    
    @Nullable
    public static String getUsername(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
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
            SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(Utils.TEAMS, new Gson().toJson(teams));
            editor.apply();
        }
    }
    
    public static String[] getTeams(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        return new Gson().fromJson(prefs.getString(Utils.TEAMS, "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static int findTeam(Context c, String team) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        String[] teams = new Gson().fromJson(prefs.getString(Utils.TEAMS, "[]"), new TypeToken<String[]>() {
        }.getType());
        
        return Arrays.asList(teams).indexOf(team);
    }
    
    public static String[] getNewTaskActivityState(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        return new Gson().fromJson(prefs.getString(Utils.NEW_TASK_ACTIVITY_STATE, "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static void setNewTaskActivityState(Context c, String[] state) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Utils.NEW_TASK_ACTIVITY_STATE, new Gson().toJson(state));
        editor.apply();
    }
    
    public static void clearNewTaskActivityState(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Utils.NEW_TASK_ACTIVITY_STATE);
        editor.apply();
    }
    
    public static int getLocalDoneIdCounter(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        return prefs.getInt(Utils.LOCAL_DONE_ID_COUNTER, Utils.MIN_LOCAL_DONE_ID);
    }
    
    public static void setLocalDoneIdCounter(Context c, int localDoneIdCounter) {
        SharedPreferences prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Utils.LOCAL_DONE_ID_COUNTER, localDoneIdCounter);
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
        // Delete app settings shared prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        Map<String, ?> prefsAll = prefs.getAll();
        
        for (Map.Entry<String, ?> entry :
                prefsAll.entrySet()) {
            editor.remove(entry.getKey());
        }
        
        editor.apply();
    
    
        // Delete user details shared prefs
        prefs = c.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
        editor = prefs.edit();
        prefsAll = prefs.getAll();
    
        for (Map.Entry<String, ?> entry :
                prefsAll.entrySet()) {
            editor.remove(entry.getKey());
        }
    
        editor.apply();
    
    }
    
}
