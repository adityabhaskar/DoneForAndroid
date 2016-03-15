package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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
public class FetchDonesTask extends AsyncTask<Void, Void, Integer> {
    
    
    private String LOG_TAG;
    private Context mContext;
    private String mAuthToken;
    private boolean mFetchTeams = false;
    private boolean mFromDoneDeleteOrEditTasks = false;
    
    public FetchDonesTask(Context context, boolean fromDoneDeleteOrEditTasks, boolean fetchTeams) {
        this.mContext = context;
        this.mFetchTeams = fetchTeams;
        this.mFromDoneDeleteOrEditTasks = fromDoneDeleteOrEditTasks;
        LOG_TAG = context.getString(R.string.APP_LOG_IDENTIFIER) + " " + FetchDonesTask.class.getSimpleName();
    }
    
    public FetchDonesTask(Context context, boolean fromDoneDeleteOrEditTasks) {
        this.mContext = context;
        this.mFromDoneDeleteOrEditTasks = fromDoneDeleteOrEditTasks;
        LOG_TAG = context.getString(R.string.APP_LOG_IDENTIFIER) + " " + FetchDonesTask.class.getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    
        // Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            Log.w(LOG_TAG, "Offline, so cancelling fetch");
            sendMessage("Offline", R.string.TASK_CANCELLED_OFFLINE, -1);
            
            cancel(true);
            return;
        }
    
        if (Utils.haveValidToken(mContext)) {
            // proceed as normal
        
            mAuthToken = Utils.getAuthToken(mContext);
        
            if (mFetchTeams) {
                new FetchTeamsTask(mContext, mFromDoneDeleteOrEditTasks).execute();
                cancel(true);
                return;
            }
            
            // Check if there are any unposted local changes  
            Cursor cursor = mContext.getContentResolver().query(
                    DoneListContract.DoneEntry.buildDoneListUri(),                  // URI
                    new String[]{DoneListContract.DoneEntry.COLUMN_NAME_ID},        // Projection
                    DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'TRUE' OR " + // Selection
                            DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'TRUE' OR " +
                            DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " IS NOT NULL",
                    null, // Selection Args
                    null  // Sort Order
            );
        
            if (cursor != null && cursor.getCount() > 0) {
                Log.v(LOG_TAG, "Found " + cursor.getCount() + " unsent tasks, post them before fetching");
                cancel(true);
                // Check to prevent cyclicity
                if (!mFromDoneDeleteOrEditTasks) {
                    new DeleteDonesTask(mContext).execute(true);
                }
                cursor.close();
                return;
            }
        
            sendMessage("Starting fetch... ", R.string.TASK_STARTED, -1);
            
            /*
            * Not to be used till we can get all updates from server, including deletes
            * 
            * */
            //lastUpdated = prefs.getString("lastUpdate", "");
        
        } else if (Utils.getAuthTokenWithoutValidityCheck(mContext) != null) {
            // token available, but not validated, so validate
            new CheckTokenTask(mContext).execute();
            cancel(true);
        
        } else {
            // no token available
        
            Log.e(LOG_TAG, "No Valid Auth Token Found!");
            sendMessage("No valid auth token found!", R.string.TASK_UNAUTH, -1);
            cancel(true);
        }
    }
    
    @Override
    protected Integer doInBackground(Void... params) {
    
        // We're fetching only latest 100 entries. May change in future to import whole history by iterating till previous == null
        final String URL = "https://idonethis.com/api/v0.1/dones/?page_size=100&done_date_after=";
        final int DAYS_TO_FETCH = -7;
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
    
        int resultStatus;
        int fetchedTaskCount = -1;
        
        /*
        * Not to be used till we can get all updates from server, including deletes
        * 
        * */
        //String requestURL = URL + (lastUpdated.equals("") ? "" : "&updated_after=" + lastUpdated);
        
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, DAYS_TO_FETCH);  // get tasks from last 7 days
        String dayOneWeekEarlier = new SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(c.getTime());  // dt is now the new date
        
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
                    
                    break; // fine
                
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG, "Authcode invalid - " + resultStatus + ": " + responseMessage);
                    // Set token invalid
                    Utils.setTokenValidity(mContext, false);
                    sendMessage(responseMessage, R.string.TASK_UNAUTH, -1);
                    
                default:
                    Log.w(LOG_TAG, "Couldn't fetch dones" + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, R.string.TASK_OTHER_ERROR, -1);
            }
    
            //Read      
            br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
    
            String line;
            StringBuilder sb = new StringBuilder();
    
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
    
            result = sb.toString();
    
            if (result.equals("")) {
        
                result = null;
        
            } else if (resultStatus == HttpURLConnection.HTTP_OK) {
                        
                /*
                * Not to be used till we can get all updates from server, including deletes
                * 
                * */
                // Update lastUpdate timestamp in SharedPrefs in case another fetchDone is triggered
                String lastUpdated = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK).format(new Date());
                Utils.setLastUpdated(mContext, lastUpdated);
                Log.v(LOG_TAG, "New LastUpdated Time: " + lastUpdated);
        
                fetchedTaskCount = getDoneListFromJson(result);
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
    
        return fetchedTaskCount;
    }
    
    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private int getDoneListFromJson(String forecastJsonStr) throws JSONException {
        
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
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            
            /*
            * To be used only till we can get all updates from server, including deletes
            * 
            * */
            // Delete all dones from local, to be replaced by freshly retrieved ones 
            // (shouldn't have gotten this far if there were unposted dones)
            mContext.getContentResolver().delete(DoneListContract.DoneEntry.CONTENT_URI, null, null);
            
            // Add newly fetched entries to the server
            mContext.getContentResolver().bulkInsert(DoneListContract.DoneEntry.CONTENT_URI, cvArray);
            
        }
    
        return cVVector.size();
    }
    
    @Override
    protected void onPostExecute(Integer fetchedTaskCount) {
        super.onPostExecute(fetchedTaskCount);
    
        if (fetchedTaskCount > -1) {
            sendMessage("Got " + fetchedTaskCount + " tasks from server.", R.string.TASK_SUCCESSFUL, fetchedTaskCount);
        }
    }
    
    private void sendMessage(String message, int action, int fetchedTaskCount) {
        Intent intent = new Intent(mContext.getString(R.string.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
        
        // You can also include some extra data.
        intent.putExtra("sender", "FetchDonesTask");
        intent.putExtra("count", fetchedTaskCount);
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
