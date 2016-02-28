package net.c306.done;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by raven on 28/02/16.
 */
public class Utils {
    
    public static void addToPendingActions(Context c, String action, int position) {
        // DONE: 27/02/16 Add delayed authentication to pending actions list 
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        Type pendingActionsListType = new TypeToken<ArrayList<String>>() {
        }.getType();
        
        String pendingActionsString = prefs.getString(c.getString(R.string.pending_actions), "");
        
        List<String> pendingActions;
        Gson gson = new Gson();
        
        if (pendingActionsString.equals("")) {
            pendingActions = new ArrayList<String>();
        } else {
            pendingActions = gson.fromJson(pendingActionsString, pendingActionsListType);
        }
        
        if (!pendingActions.contains(action)) {
            if (position == -1) {
                pendingActions.add(action);
            } else {
                pendingActions.add(position, action);
            }
            pendingActionsString = gson.toJson(pendingActions, pendingActionsListType);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(c.getString(R.string.pending_actions), pendingActionsString);
            editor.apply();
        }
    }
    
    public static void addToPendingActions(Context c, String action) {
        addToPendingActions(c, action, -1);
    }
    
    public static void removeFromPendingActions(Context c, String action) {
        // DONE: 27/02/16 Add delayed authentication to pending actions list 
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        Type pendingActionsListType = new TypeToken<ArrayList<String>>() {
        }.getType();
        
        Gson gson = new Gson();
        String pendingActionsString = prefs.getString(c.getString(R.string.pending_actions), "");
        
        // Empty string, nothing to remove
        if (pendingActionsString.equals("")) {
            return;
        }
        
        List<String> pendingActions = gson.fromJson(pendingActionsString, pendingActionsListType);
        
        if (pendingActions.contains(action)) {
            pendingActions.remove(action);
            pendingActionsString = gson.toJson(pendingActions, pendingActionsListType);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(c.getString(R.string.pending_actions), pendingActionsString);
            editor.apply();
        }
    }
    
    
}
