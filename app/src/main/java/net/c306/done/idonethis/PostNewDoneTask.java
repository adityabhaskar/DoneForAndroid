package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.R;
import net.c306.done.Utils;
import net.c306.done.db.DoneListContract;
import net.c306.done.sync.IDTAccountManager;
import net.c306.done.sync.IDTSyncAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class PostNewDoneTask extends AsyncTask<Void, Void, Integer> {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    // Holds application context, passed in constructor
    private Context mContext;
    private Gson gson = new Gson();
    private String mAuthToken;
    
    public PostNewDoneTask(Context c){
        mContext = c;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    
        // Check if internet connection is available else cancel fetch 
        if (!Utils.isOnline(mContext)) {
            Log.w(LOG_TAG, "Offline, so cancelling token check");
            Utils.sendMessage(mContext, Utils.SENDER_CREATE_TASK, "Offline", Utils.STATUS_TASK_CANCELLED_OFFLINE, -1);
            cancel(true);
            return;
        }
    
    
        // Get auth token from SharedPrefs
        mAuthToken = IDTAccountManager.getAuthToken(mContext);
    
        // Token not present or invalid
        if (mAuthToken == null) {
            Log.e(LOG_TAG, "No Valid Auth Token Found!");
            Utils.sendMessage(mContext, Utils.SENDER_CREATE_TASK, "No valid auth token found!", Utils.STATUS_TASK_UNAUTH, -1);
            cancel(true);
            return;
        }
    
        Utils.sendMessage(mContext, Utils.SENDER_CREATE_TASK, "Starting to send... ", Utils.STATUS_TASK_STARTED, -1);
    }
    
    @Override
    protected Integer doInBackground(Void... voids) {
    
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        // Contains server response (or error message)
        String result = "";
        int sentTaskCount = 0; // 0 - init/noerror-nosent, -1 - error, +xxx - xxx sent
        
        String newDoneString;
        NewTaskClass newDoneObj;
        List<Integer> sentDonesList = new ArrayList<>();
    
        // Get local, undeleted dones from database
        Cursor cursor = mContext.getContentResolver().query(
                DoneListContract.DoneEntry.buildDoneListUri(),                          // URI
                new String[]{                                                           // Projection
                        DoneListContract.DoneEntry.COLUMN_NAME_ID,
                        DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT,
                        DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                        DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE
                },
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'TRUE'",   // Selection
                null,                                                                   // Selection Args
                null                                                                    // Sort Order
        );
    
        if (cursor != null) {
    
            if (cursor.getCount() > 0) {
                
                int resultStatus;
                int columnIndexID = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_ID);
                int columnIndexRawText = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
                int columnIndexDoneDate = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
                int columnIndexTeam = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_TEAM);
            
                Log.v(LOG_TAG, "Got " + cursor.getCount() + " pending dones to post to server");
            
                // Iterate over unsent dones 
                while (cursor.moveToNext() && !isCancelled()) {
                
                    // Get next done
                    newDoneObj = new NewTaskClass(
                            cursor.getString(columnIndexRawText),
                            cursor.getString(columnIndexDoneDate),
                            cursor.getString(columnIndexTeam)
                    );
                
                    // Convert to json
                    newDoneString = gson.toJson(newDoneObj, NewTaskClass.class);
                    Log.v(LOG_TAG, "Unsent done: " + newDoneString);
                
                    // Send
                    try {
                        //Connect
                        httpcon = (HttpURLConnection) (new URL(Utils.IDT_DONE_URL).openConnection());
                        httpcon.setDoOutput(true);
                        httpcon.setRequestProperty("Authorization", "Bearer " + mAuthToken);
                        httpcon.setRequestProperty("Content-Type", "application/json");
                        httpcon.setRequestProperty("Accept", "application/json");
                        httpcon.setRequestMethod("POST");
                        httpcon.connect();
                    
                        //Write         
                        OutputStream os = httpcon.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(newDoneString);
                        writer.close();
                        os.close();
                    
                        //Response Code
                        resultStatus = httpcon.getResponseCode();
                        String responseMessage = httpcon.getResponseMessage();
                    
                        switch (resultStatus) {
                            case HttpURLConnection.HTTP_ACCEPTED:
                            case HttpURLConnection.HTTP_CREATED:
                            case HttpURLConnection.HTTP_OK:
                                Log.v(LOG_TAG, "Sent Done - " + resultStatus + ": " + responseMessage);
                                sentTaskCount++;
                                // Add id to sentDonesList
                                sentDonesList.add(cursor.getInt(columnIndexID));
                                break; // fine
                        
                            case HttpURLConnection.HTTP_UNAUTHORIZED:
                                Log.w(LOG_TAG, "Didn't Send Done - " + resultStatus + ": " + responseMessage);
                                // Set token invalid
                                Utils.setTokenValidity(mContext, false);
                                Utils.sendMessage(mContext, Utils.SENDER_CREATE_TASK, responseMessage, Utils.STATUS_TASK_UNAUTH, -1);
                                cancel(true);
                                sentTaskCount = -1;
                                break;
                        
                            default:
                                Log.w(LOG_TAG, "Didn't Send Done - " + resultStatus + ": " + responseMessage);
                        }
                    
                        //Read      
                        br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
    
                        String line;
                        StringBuilder sb = new StringBuilder();
                    
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
    
                        result += sb.toString() + "\n";
                    
                    } catch (Exception e) {
                        result = e.getMessage();
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
                }
            
                if (sentDonesList.size() == cursor.getCount()) {
                    //Reset counter for
                    Utils.setLocalDoneIdCounter(mContext, 0);
                }
            }
    
            cursor.close();
    
    
            if (sentDonesList.size() > 0) {
                // Set is_local to false for ids in sentDonesList
                ContentValues updateValues = new ContentValues();
                updateValues.put(DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL, "FALSE");
    
                String sentDonesIdString = TextUtils.join(",", sentDonesList);
    
                mContext.getContentResolver().update(
                        DoneListContract.DoneEntry.CONTENT_URI,
                        updateValues,
                        DoneListContract.DoneEntry.COLUMN_NAME_ID + " IN (" + sentDonesIdString + ")",
                        null
                );
            }
        }
    
        Log.v(LOG_TAG, "Response from server: " + result);
        
        return sentTaskCount;
    }
    
    @Override
    protected void onPostExecute(Integer sentCount) {
        super.onPostExecute(sentCount);
    
        if (sentCount > -1) {
            // Send message to MainActivity saying done(s) have been posted, so Snackbar can be shown/updated
            Utils.sendMessage(
                    mContext,
                    Utils.SENDER_CREATE_TASK,
                    "Sent " + sentCount + " tasks.",
                    Utils.STATUS_TASK_SUCCESSFUL,
                    sentCount);
        
            // Update local doneList from server
            IDTSyncAdapter.syncImmediately(mContext, true);
        }
    }
    
    public class NewTaskClass {
        private String done_date;
        private String team;
        private String raw_text;
        private String meta_data = "{\"from\":\"" + mContext.getString(R.string.app_name) + "\"}";
        
        public NewTaskClass(String doneText, String doneDate, String teamURL) {
            this.raw_text = doneText;
            
            if (doneDate != null && !doneDate.isEmpty())
                this.done_date = doneDate;
            else {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                
                this.done_date = sdf.format(new Date());
            }
        
            if (teamURL != null && !teamURL.isEmpty())
                this.team = teamURL;
            
        }
    }
    
}
