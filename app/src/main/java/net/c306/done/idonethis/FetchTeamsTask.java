package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
import java.util.Vector;

/**
 * Created by raven on 27/02/16.
 */
public class FetchTeamsTask extends AsyncTask<Void, Void, String> {
    
    
    private String LOG_TAG;
    private Context mContext;
    private String mAuthToken;
    
    private SimpleDateFormat sdf;
    private int fetchedTeamsCounter = 0;
    
    public FetchTeamsTask(Context c) {
        mContext = c;
        LOG_TAG = mContext.getString(R.string.app_log_identifier) + " " + FetchTeamsTask.class.getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        //// DONE: 22/02/16 Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            Log.v(LOG_TAG, "Offline, so cancelling fetch");
            sendMessage("Offline", mContext.getString(R.string.fetch_teams_cancelled_offline));
            
            Utils.addToPendingActions(mContext, mContext.getString(R.string.pending_action_fetch_teams));
            
            cancel(true);
            return;
        }
        
        // Get authtoken from SharedPrefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAuthToken = prefs.getString(mContext.getString(R.string.auth_token), "");
        
        if (mAuthToken.equals("")) {
            Log.e(LOG_TAG, "No Auth Token Found!");
            sendMessage("No auth token found!", mContext.getString(R.string.fetch_teams_unauth));
            cancel(true);
            return;
        }
        
        sendMessage("Starting fetch...", mContext.getString(R.string.fetch_teams_started));
        
        /*
        * Not to be used till we can get all updates from server, including deletes
        * 
        * */
        //lastUpdated = prefs.getString("lastUpdate", "");
    }
    
    @Override
    protected String doInBackground(Void... params) {
        
        final String TEAMS_URL = "https://idonethis.com/api/v0.1/teams/?page_size=100";
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        int resultStatus;
        String result = "";
        
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
                    Log.v(LOG_TAG, Thread.currentThread().getStackTrace()[2].getLineNumber() + " Got teams " + " **OK** - " + resultStatus + ": " + responseMessage);
                    
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
                        
                        result = getTeamsFromJson(result).toString();
                        
                    }
                    break; // fine
                
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                case HttpURLConnection.HTTP_UNAVAILABLE:
                    Log.w(LOG_TAG, "Couldn't fetch teams" + " **server unavailable/unreachable** - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.fetch_teams_other_error));
                    result = null;
                    break;
                
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG + " Authcode invalid", " **invalid auth code** - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.fetch_teams_unauth));
                    result = null;
                    break;
                
                default:
                    Log.w(LOG_TAG, "Couldn't fetch teams" + " **unknown response code** - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.fetch_teams_other_error));
                    result = null;
            }
            
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            sendMessage(e.getMessage(), mContext.getString(R.string.check_token_other_error));
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
        
        return result;
    }
    
    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getTeamsFromJson(String teamsJsonStr) throws JSONException {
        
        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.
        
        // These are the names of the JSON objects that need to be extracted.
        
        Gson gson = new Gson();
        String teamItemString;
        TeamItem teamItem;
        
        JSONObject masterObj = new JSONObject(teamsJsonStr);
        JSONArray teamsListArray = masterObj.getJSONArray("results");
        
        // Insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(teamsListArray.length());
        
        for (int i = 0; i < teamsListArray.length(); i++) {
            teamItemString = teamsListArray.getJSONObject(i).toString();
            Log.v(LOG_TAG, "Team " + (i + 1) + ": " + teamItemString);
            
            teamItem = gson.fromJson(teamItemString, TeamItem.class);
            
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
            fetchedTeamsCounter = cVVector.size();
            
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            
            // TODO: 27/02/16 Add newly fetched entries to the database  
            //mContext.getContentResolver().bulkInsert(DoneListContract.DoneEntry.CONTENT_URI, cvArray);
        }
        
        return new String[]{cVVector.toArray().toString()};
    }
    
    
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        
        if (result != null) {
            sendMessage(result, mContext.getString(R.string.fetch_teams_finished));
            
            Utils.removeFromPendingActions(mContext, mContext.getString(R.string.pending_action_fetch_teams));
            
            new FetchDonesTask(mContext, R.string.settings_activity_listener_intent).execute();
        } else {
            Utils.addToPendingActions(mContext, mContext.getString(R.string.pending_action_fetch_teams));
        }
    }
    
    private void sendMessage(String message, String action) {
        Intent intent = new Intent(mContext.getString(R.string.settings_activity_listener_intent));
        
        // You can also include some extra data.
        intent.putExtra("sender", "FetchTeamsTask");
        intent.putExtra("count", fetchedTeamsCounter);
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

