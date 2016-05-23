package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.Utils;
import net.c306.done.db.DoneListContract;
import net.c306.done.sync.IDTAccountManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class PostEditedDoneTask extends AsyncTask<Void, Void, Integer> {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    // Holds application context, passed in constructor
    private Context mContext;
    private Gson gson = new Gson();
    private String mAuthToken;
    
    public PostEditedDoneTask(Context c) {
        mContext = c.getApplicationContext();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        // Check if internet connection is available else cancel fetch 
        if (!Utils.isOnline(mContext)) {
            Log.w(LOG_TAG, "Offline, so cancelling token check");
            Utils.sendMessage(mContext, Utils.SENDER_EDIT_TASK, "Offline", Utils.STATUS_TASK_CANCELLED_OFFLINE, -1);
            cancel(true);
            return;
        }
        
        
        // Get auth token from SharedPrefs
        mAuthToken = IDTAccountManager.getAuthToken(mContext);
        
        // Token not present or invalid
        if (mAuthToken == null) {
            Log.e(LOG_TAG, "No Valid Auth Token Found!");
            Utils.sendMessage(mContext, Utils.SENDER_EDIT_TASK, "No valid auth token found!", Utils.STATUS_TASK_UNAUTH, -1);
            cancel(true);
            return;
        }
    
        Utils.sendMessage(mContext, Utils.SENDER_EDIT_TASK, "Starting to patch... ", Utils.STATUS_TASK_STARTED, -1);
    }
    
    @Override
    protected Integer doInBackground(Void... voids) {
        
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        
        String result = "";
        int patchedTaskCount = 0;
        String patchedDoneString = null;
        EditedTaskClass patchedDoneObj = null;
        List<Integer> patchedDonesList = new ArrayList<>();
        
        // Get local, undeleted dones from database
        Cursor cursor = mContext.getContentResolver().query(
                DoneListContract.DoneEntry.buildDoneListUri(),                          // URI
                new String[]{                                                           // Projection
                        DoneListContract.DoneEntry.COLUMN_NAME_ID,
                        DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT,
                        DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                        DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS,
                        DoneListContract.DoneEntry.COLUMN_NAME_URL
                },
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " IS NOT NULL AND " +   // Selection
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'FALSE'",
                null,                                                                   // Selection Args
                null                                                                    // Sort Order
        );
        
        if (cursor != null) {
    
            Log.v(LOG_TAG, "Got " + cursor.getCount() + " pending dones to post to server");
    
            if (cursor.getCount() > 0) {
                
                int resultStatus;
                int columnIndexID = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_ID);
                int columnIndexRawText = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
                int columnIndexDoneDate = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
                int columnIndexTeamURL = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_TEAM);
                int columnIndexDoneURL = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_URL);
                
                // Iterate over unsent dones 
                while (cursor.moveToNext() && !isCancelled()) {
                    // Get next done
                    
                    // Create editdone object with edited fields
                    patchedDoneObj = new EditedTaskClass();
                    patchedDoneObj.setRaw_text(cursor.getString(columnIndexRawText));
                    patchedDoneObj.setDone_date(cursor.getString(columnIndexDoneDate));
                    patchedDoneObj.setTeam(cursor.getString(columnIndexTeamURL));
                    
                    // Convert to json
                    patchedDoneString = gson.toJson(patchedDoneObj, EditedTaskClass.class);
                    
                    // Send
                    try {
                        URL patchTaskURL = new URL(cursor.getString(columnIndexDoneURL));
                        //Log.v(LOG_TAG, "Request URL: " + patchTaskURL);
    
                        //Connect
                        httpcon = (HttpURLConnection) (patchTaskURL.openConnection());
                        httpcon.setRequestMethod("PUT");
                        httpcon.setDoOutput(true);
                        httpcon.setRequestProperty("Authorization", "Bearer " + mAuthToken);
                        httpcon.setRequestProperty("Content-Type", "application/json");
                        httpcon.setRequestProperty("Accept", "application/json");
                        
                        //Write         
                        Log.v(LOG_TAG, "Request body: " + patchedDoneString);
                        OutputStream os = httpcon.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                        writer.write(patchedDoneString);
                        writer.close();
                        os.close();
                        
                        httpcon.connect();
                        
                        //Response Code
                        resultStatus = httpcon.getResponseCode();
                        String responseMessage = httpcon.getResponseMessage();
                        
                        switch (resultStatus) {
                            case HttpURLConnection.HTTP_ACCEPTED:
                            case HttpURLConnection.HTTP_CREATED:
                            case HttpURLConnection.HTTP_OK:
                                Log.v(LOG_TAG, "Updated Done - " + resultStatus + ": " + responseMessage);
                                patchedTaskCount++;
                                // Add id to sentDonesList
                                patchedDonesList.add(cursor.getInt(columnIndexID));
                                break; // fine
                            
                            case HttpURLConnection.HTTP_UNAUTHORIZED:
                                Log.w(LOG_TAG, "Didn't Update Done - " + resultStatus + ": " + responseMessage);
                                // Set token invalid
                                Utils.setTokenValidity(mContext, false);
                                Utils.sendMessage(mContext, Utils.SENDER_EDIT_TASK, responseMessage, Utils.STATUS_TASK_UNAUTH, -1);
                                cancel(true);
                                patchedTaskCount = -1;
                                break;
                            
                            default:
                                Log.w(LOG_TAG, "Didn't Update Done - " + resultStatus + ": " + responseMessage);
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
    
                if (patchedDonesList.size() > 0) {
                    // Set edited_fields to null for ids in sentDonesList
                    ContentValues updateValues = new ContentValues();
                    updateValues.putNull(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS);
        
                    String patchedDonesIdString = TextUtils.join(",", patchedDonesList);
        
                    mContext.getContentResolver().update(
                            DoneListContract.DoneEntry.CONTENT_URI,
                            updateValues,
                            DoneListContract.DoneEntry.COLUMN_NAME_ID + " IN (" + patchedDonesIdString + ")",
                            null
                    );
                }
            }
            
            cursor.close();
            
        }
    
        if (patchedTaskCount > -1)
            Log.v(LOG_TAG, "Patched " + patchedTaskCount + " dones");
        else
            Log.v(LOG_TAG, "Response from server: " + result);
        
        return patchedTaskCount;
    }
    
    @Override
    protected void onPostExecute(Integer patchedCount) {
        super.onPostExecute(patchedCount);
        
        if (patchedCount > -1) {
            // Send message to MainActivity saying done(s) have been posted, so Snackbar can be shown/updated
            Utils.sendMessage(mContext, Utils.SENDER_EDIT_TASK, "Sent " + patchedCount + " tasks.", Utils.STATUS_TASK_SUCCESSFUL, patchedCount);
    
            // No errors, so call next chained task
            new PostNewDoneTask(mContext).execute();
        }
    }
    
    
    public class EditedTaskClass {
        private transient final String dateFormat = "yyyy-MM-dd";
        private String raw_text;
        private String team;
        private String done_date;
        
        public EditedTaskClass() {
        }
        
        public void setDone_date(String done_date) {
            if (done_date != null && !done_date.isEmpty())
                this.done_date = done_date;
        }
    
        public String getTeam() {
            return team;
        }
    
        public void setTeam(String team) {
            if (team != null && !team.isEmpty())
                this.team = team;
        }
    
        public void setRaw_text(String raw_text) {
            if (raw_text != null && !raw_text.isEmpty())
                this.raw_text = raw_text;
        }
    }
}
