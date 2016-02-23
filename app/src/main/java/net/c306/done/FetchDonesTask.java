package net.c306.done;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.db.DoneListContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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
    
    private final int DAYS_TO_FETCH = -7;
    // We're fetching only latest 100 entries. May change in future to import whole history by iterating till previous == null
    private final String URL = "https://idonethis.com/api/v0.1/dones/?page_size=100&done_date_after=";
    //private final String URL = "https://idonethis.com/api/v0.1/dones/?page_size=100";
    
    private String LOG_TAG;
    private Context mContext;
    private String mAuthToken;
    private int resultStatus;
    private String lastUpdated = "";
    private SimpleDateFormat sdf;
    private int fetchedDoneCounter = 0;
    
    public FetchDonesTask(Context c){
        mContext = c;
        LOG_TAG = mContext.getString(R.string.app_log_identifier) + " " + FetchDonesTask.class.getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    
        // Get authtoken from SharedPrefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAuthToken = prefs.getString("authToken", "");
    
        if(mAuthToken.equals("")){
            Log.e(LOG_TAG, "No Auth Token Found!");
            cancel(true);
        }
    
        sendMessage(mContext.getString(R.string.fetch_started));
    
        //// TODO: 22/02/16 Check if internet connection is available else cancel fetch 
        /*
        * Not to be used till we can get all updates from server, including deletes
        * 
        * */
        //lastUpdated = prefs.getString("lastUpdate", "");
    }
    
    @Override
    protected String doInBackground(Void... params) {
    
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        
        
        /*
        * Not to be used till we can get all updates from server, including deletes
        * 
        * */
        //String requestURL = URL + (lastUpdated.equals("") ? "" : "&updated_after=" + lastUpdated);
        
        sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
        
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, DAYS_TO_FETCH);  // get tasks from last 7 days
        String dayBeforeYesterday = sdf.format(c.getTime());  // dt is now the new date
        
        String requestURL = URL + dayBeforeYesterday;
        Log.v(LOG_TAG, requestURL);
        
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
                    Log.v(LOG_TAG + " Sent Done" , " **OK** - " + resultStatus + ": " + responseMessage);
                    
                    //Read      
                    br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(),"UTF-8"));
                    
                    String line = null;
                    StringBuilder sb = new StringBuilder();
                    
                    while ((line = br.readLine()) != null) {
                        sb.append(line + "\n");
                    }
                    
                    result = sb.toString();
                    
                    if(sb.length() == 0){
                        httpcon.disconnect();
                        br.close();
                        return null;
                    }

                    /*
                    * Not to be used till we can get all updates from server, including deletes
                    * 
                    * */
                    // Update lastUpdate timestamp in SharedPrefs in case another fetchDone is triggered
                    sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
                    lastUpdated = sdf.format(new Date());
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("lastUpdate");
                    editor.putString(mContext.getString(R.string.last_updated_setting_name), lastUpdated);
                    editor.apply();
                    Log.v(LOG_TAG, "LastUpdated: " + lastUpdated);
    
                    if(resultStatus > 0 && null != result && !result.equals("")){
                        return getDoneListFromJson(result).toString();
                    }
                    break; // fine
                
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                    Log.w(LOG_TAG + " Didn't Send Done" , " **gateway timeout** - " + resultStatus + ": " + responseMessage);
                    //return null;
                    break;
                
                case HttpURLConnection.HTTP_UNAVAILABLE:
                    Log.w(LOG_TAG + " Didn't Send Done" , " **unavailable** - " + resultStatus + ": " + responseMessage);
                    //return null;
                    break;// retry, server is unstable
                
                default:
                    Log.w(LOG_TAG + " Didn't Send Done" , " **unknown response code** - " + resultStatus + ": " + responseMessage);
                    //return null;
            }
            
        } catch (UnsupportedEncodingException e) {
            result = e.getMessage();
            resultStatus = -1;
            e.printStackTrace();
        } catch (IOException e) {
            result = e.getMessage();
            resultStatus = -1;
            e.printStackTrace();
        } catch (JSONException e) {
            result = e.getMessage();
            resultStatus = -1;
            e.printStackTrace();
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
            mContext.getContentResolver().delete(DoneListContract.DoneEntry.CONTENT_URI, null, null);
            
            // Add newly fetched entries to the server
            mContext.getContentResolver().bulkInsert(DoneListContract.DoneEntry.CONTENT_URI, cvArray);
            
            //notifyWeather();
        }
        
        String[] resultStrs = convertContentValuesToUXFormat(cVVector);
        return resultStrs;
    }
    
    private String[] convertContentValuesToUXFormat(Vector<ContentValues> cVVector) {
        return new String[]{cVVector.toArray().toString()};
    }
    
    
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        //mContext.getContentResolver().notifyChange(DoneListContract.DoneEntry.buildDoneListUri(), null);
        Log.v(LOG_TAG, "GET Result: " + result);
        sendMessage(mContext.getString(R.string.fetch_finished));
    }
    
    private void sendMessage(String message) {
        Intent intent = new Intent(mContext.getString(R.string.done_posted_intent));
        
        // You can also include some extra data.
        intent.putExtra("sender", "FetchDonesTask");
        intent.putExtra("count", fetchedDoneCounter);
        intent.putExtra("action", message);
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(intent);
    }
    
}

