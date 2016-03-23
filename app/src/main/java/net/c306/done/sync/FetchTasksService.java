package net.c306.done.sync;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.DoneItem;
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

@Deprecated
public class FetchTasksService extends IntentService {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    
    public FetchTasksService() {
        super("FetchTasksService");
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        String mAuthToken = IDTAccountManager.getAuthToken(this);
        
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
        
        //String requestURL = URL + dayOneWeekEarlier;
        String requestURL = URL;
        
        // Contains server response (or error message)
        String result = "";
        
        try {
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
                    Utils.setTokenValidity(this, false);
                    sendMessage(responseMessage, Utils.STATUS_TASK_UNAUTH, -1);
                
                default:
                    Log.w(LOG_TAG, "Couldn't fetch dones" + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, Utils.STATUS_TASK_OTHER_ERROR, -1);
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
                Utils.setLastUpdated(this, lastUpdated);
                Log.v(LOG_TAG, "New LastUpdated Time: " + lastUpdated);
                
                fetchedTaskCount = getDoneListFromJson(result);
            }
            
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            sendMessage(e.getMessage(), Utils.STATUS_TASK_OTHER_ERROR, -1);
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
        
        sendMessage("Fetch Successful.", Utils.STATUS_TASK_SUCCESSFUL, fetchedTaskCount);
        
/*
        // Playing with Notifications
        
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);
        
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        // mId allows you to update the notification later on.
        int mId = 1;
        mNotificationManager.notify(mId, mBuilder.build());
*/
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
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            
            /*
            * To be used only till we can get all updates from server, including deletes
            * 
            * */
            // Delete all dones from local, to be replaced by freshly retrieved ones 
            // (shouldn't have gotten this far if there were unposted dones)
            getContentResolver().delete(DoneListContract.DoneEntry.CONTENT_URI, null, null);
            
            // Add newly fetched entries to the server
            getContentResolver().bulkInsert(DoneListContract.DoneEntry.CONTENT_URI, cvArray);
            
        }
        
        return cVVector.size();
    }
    
    
    private void sendMessage(String message, int action, int fetchedTaskCount) {
        Intent intent = new Intent(Utils.DONE_LOCAL_BROADCAST_LISTENER_INTENT);
        
        // You can also include some extra data.
        intent.putExtra("sender", this.getClass().getSimpleName());
        intent.putExtra("count", fetchedTaskCount);
        intent.putExtra("message", message);
        intent.putExtra("action", action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    
    static public class AlarmReceiver extends BroadcastReceiver {
        String LOG_TAG = "AppMessage " + this.getClass().getSimpleName();
        
        @Override
        public void onReceive(Context context, Intent intent) {
            
            Log.wtf(LOG_TAG, "Got called by alarm");
            Intent fetchTasksServiceIntent = new Intent(context, FetchTasksService.class);
            fetchTasksServiceIntent.putExtra(Utils.INTENT_EXTRA_FROM_DONE_DELETE_OR_EDIT_TASKS, intent.getBooleanExtra(Utils.INTENT_EXTRA_FROM_DONE_DELETE_OR_EDIT_TASKS, false));
            fetchTasksServiceIntent.putExtra(Utils.INTENT_EXTRA_FETCH_TEAMS, intent.getBooleanExtra(Utils.INTENT_EXTRA_FETCH_TEAMS, true));
            context.startService(fetchTasksServiceIntent);
        }
    }
}
