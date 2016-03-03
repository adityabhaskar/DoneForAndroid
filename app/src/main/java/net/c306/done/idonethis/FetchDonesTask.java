package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.DoneItem;
import net.c306.done.R;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

/**
 * Created by raven on 16/02/16.
 */
public class FetchDonesTask extends AsyncTask<Void, Void, String> {
    
    
    private String LOG_TAG;
    private Context mContext;
    private String mAuthToken;
    private int mIntentId; // Id that identifies which listener to send intent to - MainActivity or SettingsActivity
    private int fetchedDoneCounter = 0;
    private boolean mFromPostNewDone = false;
    
    public FetchDonesTask(Context c, int intentId, boolean fromPostNewDone) {
        mContext = c;
        mIntentId = intentId;
        mFromPostNewDone = fromPostNewDone;
        LOG_TAG = mContext.getString(R.string.app_log_identifier) + " " + FetchDonesTask.class.getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    
        //// DONE: 22/02/16 Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            Log.w(LOG_TAG, "Offline, so cancelling fetch");
            sendMessage("Offline", mContext.getString(R.string.fetch_tasks_cancelled_offline));
    
            Utils.addToPendingActions(mContext, mContext.getString(R.string.pending_action_fetch_dones));
            
            cancel(true);
            return;
        }
        
        // Get authtoken from SharedPrefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAuthToken = prefs.getString(mContext.getString(R.string.auth_token), "");
    
        if(mAuthToken.equals("")){
            Log.e(LOG_TAG, "No Auth Token Found!");
            sendMessage("No auth token found!", mContext.getString(R.string.fetch_tasks_unauth));
            cancel(true);
            return;
        }
    
        // Check to prevent cyclicity
        if (!mFromPostNewDone) {
            // Check if there are any unsent local tasks  
            Cursor cursor = mContext.getContentResolver().query(
                    DoneListContract.DoneEntry.buildDoneListUri(),
                    new String[]{DoneListContract.DoneEntry.COLUMN_NAME_ID},
                    DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'true'",
                    null,
                    null
            );
        
            if (cursor != null && cursor.getCount() > 0) {
                Log.v(LOG_TAG, "Found " + cursor.getCount() + " unsent tasks, post them before fetching");
                cancel(true);
                new PostNewDoneTask(mContext).execute(true);
                cursor.close();
                return;
            }
        }
    
        sendMessage("Starting fetch... ", mContext.getString(R.string.fetch_tasks_started));
        
        /*
        * Not to be used till we can get all updates from server, including deletes
        * 
        * */
        //lastUpdated = prefs.getString("lastUpdate", "");
    }
    
    @Override
    protected String doInBackground(Void... params) {
    
        // We're fetching only latest 100 entries. May change in future to import whole history by iterating till previous == null
        final String URL = "https://idonethis.com/api/v0.1/dones/?page_size=100&done_date_after=";
        final int DAYS_TO_FETCH = -7;
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        SimpleDateFormat sdf;
        int resultStatus;
        
        
        /*
        * Not to be used till we can get all updates from server, including deletes
        * 
        * */
        //String requestURL = URL + (lastUpdated.equals("") ? "" : "&updated_after=" + lastUpdated);
        
        sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, DAYS_TO_FETCH);  // get tasks from last 7 days
        String dayOneWeekEarlier = sdf.format(c.getTime());  // dt is now the new date
    
        String requestURL = URL + dayOneWeekEarlier;
        
        // Contains server response (or error message)
        String result = "";
        
        try{
            //Connect
            httpcon = (HttpURLConnection) ((new URL(requestURL).openConnection()));
            httpcon.setRequestProperty("Authorization", "Token " + mAuthToken);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("GET");
            httpcon.connect();
            
            //Response Code
            resultStatus = httpcon.getResponseCode();
            String responseMessage = httpcon.getResponseMessage();
            
            switch (resultStatus) {
                case HttpURLConnection.HTTP_ACCEPTED:
                case HttpURLConnection.HTTP_CREATED:
                case HttpURLConnection.HTTP_OK:
                    Log.v(LOG_TAG, "Got Done List - " + resultStatus + ": " + responseMessage);
                    
                    //Read      
                    br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(),"UTF-8"));
    
                    String line;
                    StringBuilder sb = new StringBuilder();
                    
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    
                    result = sb.toString();
    
                    if (result.equals("")) {
        
                        result = null;
        
                    } else {
                        
                        /*
                        * Not to be used till we can get all updates from server, including deletes
                        * 
                        * */
                        // Update lastUpdate timestamp in SharedPrefs in case another fetchDone is triggered
                        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
                        String lastUpdated = sdf.format(new Date());
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(mContext.getString(R.string.last_updated_setting_name), lastUpdated);
                        editor.apply();
                        Log.v(LOG_TAG, "New LastUpdated Time: " + lastUpdated);
        
                        result = getDoneListFromJson(result).toString();
                        
                    }
    
                    break; // fine
                
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                case HttpURLConnection.HTTP_UNAVAILABLE:
                    Log.w(LOG_TAG, "Couldn't fetch dones - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.fetch_tasks_other_error));
                    result = null;
                    break;
    
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG, "Authcode invalid - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.fetch_tasks_unauth));
                    result = null;
                    break;
                
                default:
                    Log.w(LOG_TAG, "Couldn't fetch dones" + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.fetch_tasks_other_error));
                    result = null;
                    
            }
    
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            sendMessage(e.getMessage(), mContext.getString(R.string.fetch_tasks_other_error));
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
    private String[] getDoneListFromJson(String forecastJsonStr) throws JSONException {
        
        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.
        
        // These are the names of the JSON objects that need to be extracted.
    
        Gson gson = new Gson();
        String doneItemString;
        DoneItem doneItem;
        
        JSONObject masterObj = new JSONObject(forecastJsonStr);
        JSONArray donesListArray = masterObj.getJSONArray("results");
        
        // Insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(donesListArray.length());
        
        for (int i = 0; i < donesListArray.length(); i++) {
            doneItemString = donesListArray.getJSONObject(i).toString();
            
            doneItem = gson.fromJson(doneItemString, DoneItem.class);
            
            ContentValues doneItemValues = new ContentValues();
            
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_ID, doneItem.id);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_CREATED, doneItem.created);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_UPDATED, doneItem.updated);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT, doneItem.markedup_text);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, doneItem.done_date);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_OWNER, doneItem.owner);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME, doneItem.team_short_name);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TAGS, gson.toJson(doneItem.tags, DoneItem.DoneTags[].class));
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_LIKES, "");
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS, "");
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_META_DATA, gson.toJson(doneItem.meta_data, DoneItem.DoneMeta.class));
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_IS_GOAL, doneItem.is_goal);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_GOAL_COMPLETED, doneItem.goal_completed);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_URL, doneItem.url);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, doneItem.team);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, Html.fromHtml(doneItem.raw_text).toString());
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_PERMALINK, doneItem.permalink);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL, doneItem.is_local);
    
            cVVector.add(doneItemValues);
        }
        
        Log.v(LOG_TAG, "Fetched " + cVVector.size() + " items");
        
        // add to database
        if(cVVector.size() > 0) {
            fetchedDoneCounter = cVVector.size();
            
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            
            /*
            * To be used only till we can get all updates from server, including deletes
            * 
            * */
            // Delete non-local dones from local, to be replaced by freshly retrieved ones
            mContext.getContentResolver().delete(DoneListContract.DoneEntry.CONTENT_URI,
                    DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'false'",
                    null);
            
            // Add newly fetched entries to the server
            mContext.getContentResolver().bulkInsert(DoneListContract.DoneEntry.CONTENT_URI, cvArray);
            
            //notifyWeather();
        }
    
        return new String[]{cVVector.toArray().toString()};
    }
    
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
    
        if (result != null) {
            sendMessage(result, mContext.getString(R.string.fetch_tasks_finished));
        
            Utils.removeFromPendingActions(mContext, mContext.getString(R.string.pending_action_fetch_dones));
        
        } else {
            Utils.addToPendingActions(mContext, mContext.getString(R.string.pending_action_fetch_dones));
        }
    }
    
    private void sendMessage(String message, String action) {
        Intent intent = new Intent(mContext.getString(mIntentId));
        
        // You can also include some extra data.
        intent.putExtra("sender", "FetchDonesTask");
        intent.putExtra("count", fetchedDoneCounter);
        intent.putExtra("message", message);
        intent.putExtra("action", action);
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

