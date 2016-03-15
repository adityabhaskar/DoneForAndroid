package net.c306.done;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by raven on 28/02/16.
 */
public class Utils {
    
    public static boolean haveValidToken(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getBoolean(c.getString(R.string.VALID_TOKEN), false);
    }
    
    public static void setTokenValidity(Context c, boolean flag) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(c.getString(R.string.VALID_TOKEN), flag);
        editor.apply();
    }
    
    @Nullable
    public static String getAuthToken(Context c) {
        if (haveValidToken(c)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            return prefs.getString(c.getString(R.string.AUTH_TOKEN), null);
        } else {
            return null;
        }
    }
    
    @Nullable
    public static String getAuthTokenWithoutValidityCheck(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(c.getString(R.string.AUTH_TOKEN), null);
    }
    
    @Nullable
    public static int getLocalDoneIdCounter(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getInt(c.getString(R.string.LOCAL_DONE_ID_COUNTER), R.integer.MIN_LOCAL_DONE_ID);
    }
    
    public static void setLocalDoneIdCounter(Context c, int localDoneIdCounter) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(c.getString(R.string.LOCAL_DONE_ID_COUNTER), localDoneIdCounter);
        editor.apply();
    }
    
    public static void setLastUpdated(Context c, String lastUpdated) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(c.getString(R.string.LAST_UPDATED_SETTING_NAME), lastUpdated);
        editor.apply();
    }
    
    @Nullable
    public static String getLastUpdated(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(c.getString(R.string.LAST_UPDATED_SETTING_NAME), null);
    }
    
    public static void setUsername(Context c, String username) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(c.getString(R.string.USERNAME), username);
        editor.apply();
    }
    
    @Nullable
    public static String getUsername(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(c.getString(R.string.USERNAME), null);
    }
    
    @Nullable
    public static String getDefaultTeam(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return prefs.getString(c.getString(R.string.DEFAULT_TEAM), null);
    }
    
    public static void setDefaultTeam(Context c, String team) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(c.getString(R.string.DEFAULT_TEAM), team);
        editor.apply();
    }
    
    public static void setTeams(Context c, String[] teams) {
        if (teams.length > 0) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(c.getString(R.string.TEAMS), new Gson().toJson(teams));
            editor.apply();
        }
    }
    
    public static String[] getTeams(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return new Gson().fromJson(prefs.getString(c.getString(R.string.TEAMS), "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static int findTeam(Context c, String team) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        String[] teams = new Gson().fromJson(prefs.getString(c.getString(R.string.TEAMS), "[]"), new TypeToken<String[]>() {
        }.getType());
        
        return Arrays.asList(teams).indexOf(team);
    }
    
    @Nullable
    public static String[] getNewTaskActivityState(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        return new Gson().fromJson(prefs.getString(c.getString(R.string.NEW_TASK_ACTIVITY_STATE), "[]"), new TypeToken<String[]>() {
        }.getType());
    }
    
    public static void setNewTaskActivityState(Context c, String[] state) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(c.getString(R.string.NEW_TASK_ACTIVITY_STATE), new Gson().toJson(state));
        editor.apply();
    }
    
    public static void clearNewTaskActivityState(Context c) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(c.getString(R.string.NEW_TASK_ACTIVITY_STATE));
        editor.apply();
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
