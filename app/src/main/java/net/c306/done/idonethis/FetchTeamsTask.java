package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.R;
import net.c306.done.TeamItem;
import net.c306.done.Utils;
import net.c306.done.db.DoneListContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by raven on 27/02/16.
 */
public class FetchTeamsTask extends AsyncTask<Void, Void, Integer> {
    
    
    private String LOG_TAG;
    private Context mContext;
    private String mAuthToken;
    private boolean mFromDoneDeleteOrEditTasks = false;
    
    private SimpleDateFormat sdf;
    
    public FetchTeamsTask(Context c) {
        mContext = c;
        LOG_TAG = mContext.getString(R.string.APP_LOG_IDENTIFIER) + " " + this.getClass().getSimpleName();
    }
    
    public FetchTeamsTask(Context mContext, boolean mFromDoneDeleteOrEditTasks) {
        this.mContext = mContext;
        this.mFromDoneDeleteOrEditTasks = mFromDoneDeleteOrEditTasks;
        LOG_TAG = mContext.getString(R.string.APP_LOG_IDENTIFIER) + " " + this.getClass().getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        //// DONE: 22/02/16 Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            Log.w(LOG_TAG, "Offline, so cancelling fetch");
            sendMessage("Offline", R.string.TASK_CANCELLED_OFFLINE, -1);
            cancel(true);
            return;
        }
        
        // Get authtoken from SharedPrefs
        mAuthToken = Utils.getAuthToken(mContext);
    
        if (mAuthToken == null) {
            Log.e(LOG_TAG, "No Auth Token Found!");
            sendMessage("No auth token found!", R.string.TASK_UNAUTH, -1);
            cancel(true);
            return;
        }
    
        sendMessage("Starting fetch...", R.string.TASK_STARTED, -1);
        
        /*
        * Not to be used till we can get all updates from server, including deletes
        * 
        * */
        //lastUpdated = prefs.getString("lastUpdate", "");
    }
    
    @Override
    protected Integer doInBackground(Void... params) {
        
        final String TEAMS_URL = "https://idonethis.com/api/v0.1/teams/?page_size=100";
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        int resultStatus;
        String result = "";
        int teamCount = 0;
        
        try {
            //Connect
            httpcon = (HttpURLConnection) ((new URL(TEAMS_URL).openConnection()));
            httpcon.setRequestProperty("Authorization", "Token " + mAuthToken);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("GET");
            httpcon.connect();
            
            //Response Code
            resultStatus = httpcon.getResponseCode();
            String responseMessage = httpcon.getResponseMessage();
            
            switch (resultStatus) {
                case HttpURLConnection.HTTP_OK:
                    Log.v(LOG_TAG, " Got teams - " + resultStatus + ": " + responseMessage);
                    
                    //Read      
                    br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
                    
                    String line = null;
                    StringBuilder sb = new StringBuilder();
                    
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    
                    result = sb.toString();
                    
                    httpcon.disconnect();
                    br.close();
                    
                    if (result.equals("")) {
                        
                        result = null;
                        
                    } else {
    
                        teamCount = getTeamsFromJson(result);
                        
                    }
                    break; // fine
                
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG, " Authcode invalid - " + resultStatus + ": " + responseMessage);
                    // Set invalid token
                    Utils.setTokenValidity(mContext, false);
                    sendMessage(responseMessage, R.string.TASK_UNAUTH, -1);
                    // Cancel everything and return to app with alarm message
                    cancel(true);
                    return null;
                
                default:
                    Log.w(LOG_TAG, "Couldn't fetch teams - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, R.string.TASK_OTHER_ERROR, -1);
                    result = null;
            }
            
        } catch (Exception e) {
    
            result = null;
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            sendMessage(e.getMessage(), R.string.TASK_OTHER_ERROR, -1);
            
        } finally {
    
            if (httpcon != null) {
                httpcon.disconnect();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
    
        }
    
        Log.v(LOG_TAG, "Message from server: " + result);
    
        return teamCount;
    }
    
    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private int getTeamsFromJson(String teamsJsonStr) throws JSONException {
        
        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.
        
        // These are the names of the JSON objects that need to be extracted.
        
        Gson gson = new Gson();
        String teamItemString;
        TeamItem teamItem;
        List<String> teams = new ArrayList<>();
        
        JSONObject masterObj = new JSONObject(teamsJsonStr);
        JSONArray teamsListArray = masterObj.getJSONArray("results");
        
        // Insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(teamsListArray.length());
        
        for (int i = 0; i < teamsListArray.length(); i++) {
            teamItemString = teamsListArray.getJSONObject(i).toString();
            
            teamItem = gson.fromJson(teamItemString, TeamItem.class);
    
            teams.add(teamItem.url);
            
            ContentValues teamItemValues = new ContentValues();
            
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_URL, teamItem.url);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_NAME, teamItem.name);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_SHORT_NAME, teamItem.short_name);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_DONES, teamItem.dones);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_IS_PERSONAL, teamItem.is_personal);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_PERMALINK, teamItem.permalink);
            
            cVVector.add(teamItemValues);
        }
        
        Log.v(LOG_TAG, "Fetched " + cVVector.size() + " items");
        
        // add to database
        if (cVVector.size() > 0) {
            
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
    
            // Add newly fetched teams to the database  
            mContext.getContentResolver().bulkInsert(DoneListContract.TeamEntry.CONTENT_URI, cvArray);
            
        }
    
        // Set teams in sharedPrefs for colouring
        Utils.setTeams(mContext, teams.toArray(new String[teams.size()]));
    
        String defaultTeam = Utils.getDefaultTeam(mContext);
    
        if (defaultTeam == null) {
            // No default team set, so set first team as default
            Utils.setDefaultTeam(mContext, teams.get(0));
        
        } else if (Utils.findTeam(mContext, defaultTeam) == -1) {
            // Previous default team not in fetched teams. Else raise error!
            sendMessage("Default team not in teams", -1, -1);
        
        }
        
        // Return number of fetched teams
        return cVVector.size();
    }
    
    
    @Override
    protected void onPostExecute(Integer teamCount) {
        super.onPostExecute(teamCount);
    
        if (teamCount > 0) {
            sendMessage("Got " + teamCount + " teams.", R.string.TASK_SUCCESSFUL, teamCount);
    
            new FetchDonesTask(mContext, mFromDoneDeleteOrEditTasks).execute();
        }
    }
    
    private void sendMessage(String message, int action, int count) {
        Intent intent = new Intent(mContext.getString(R.string.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
        
        // You can also include some extra data.
        intent.putExtra("sender", this.getClass().getSimpleName());
        intent.putExtra("count", count);
        intent.putExtra("action", action);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(intent);
    }
    
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }
    
}
