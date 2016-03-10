package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.R;
import net.c306.done.Utils;
import net.c306.done.db.DoneListContract;

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


public class PostNewDoneTask extends AsyncTask<Boolean, Void, Integer> {
    
    // Holds application context, passed in constructor
    private Context mContext;
    private Gson gson = new Gson();
    private String mAuthToken;
    private String LOG_TAG;
    private boolean mFromPreFetch = false;
    
    public PostNewDoneTask(Context c){
        mContext = c;
        LOG_TAG = mContext.getString(R.string.APP_LOG_IDENTIFIER) + " " + this.getClass().getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    
        // Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            Log.w(LOG_TAG, "Offline, so cancelling token check");
            sendMessage("Offline", R.string.TASK_CANCELLED_OFFLINE, -1);
            cancel(true);
            return;
        }
    
    
        // Get auth token from SharedPrefs
        mAuthToken = Utils.getAuthToken(mContext);
    
        // Token not present or invalid
        if (mAuthToken == null) {
            Log.e(LOG_TAG, "No Valid Auth Token Found!");
            sendMessage("No valid auth token found!", R.string.TASK_UNAUTH, -1);
            cancel(true);
            return;
        }
    
        sendMessage("Starting to send... ", R.string.TASK_STARTED, -1);
    }
    
    @Override
    protected Integer doInBackground(Boolean... fromPreFetch) {
    
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        final String url = "https://idonethis.com/api/v0.1/dones/";
        // Contains server response (or error message)
        String result = "";
        int sentTaskCount = -1;
        
        String newDoneString = null;
        NewDoneClass newDoneObj = null;
        List<Integer> sentDonesList = new ArrayList<>();
    
        this.mFromPreFetch = fromPreFetch[0];
    
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
        
            if (cursor.getCount() == 0) {
                sentTaskCount = 0;
            } else {
            
                int resultStatus;
                int columnIndexID = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_ID);
                int columnIndexRawText = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
                int columnIndexDoneDate = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
                int columnIndexTeam = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_TEAM);
            
                Log.v(LOG_TAG, "Got " + cursor.getCount() + " pending dones to post to server");
            
                // Iterate over unsent dones 
                while (cursor.moveToNext()) {
                
                    // Get next done
                    newDoneObj = new NewDoneClass(
                            cursor.getString(columnIndexRawText),
                            cursor.getString(columnIndexDoneDate),
                            cursor.getString(columnIndexTeam)
                    );
                
                    // Convert to json
                    newDoneString = gson.toJson(newDoneObj, NewDoneClass.class);
                    Log.v(LOG_TAG, "Unsent done: " + newDoneString);
                
                    // Send
                    try {
                        //Connect
                        httpcon = (HttpURLConnection) (new URL(url).openConnection());
                        httpcon.setDoOutput(true);
                        httpcon.setRequestProperty("Authorization", "Token " + mAuthToken);
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
                                sendMessage(responseMessage, R.string.TASK_UNAUTH, -1);
                                cancel(true);
                                return null;
                        
                            default:
                                Log.w(LOG_TAG, "Didn't Send Done - " + resultStatus + ": " + responseMessage);
                        }
                    
                        //Read      
                        br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
                    
                        String line = null;
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
    
        Log.v(LOG_TAG, "Response from server: " + result);
    
        return sentTaskCount;
    }
    
    @Override
    protected void onPostExecute(Integer sentCount) {
        super.onPostExecute(sentCount);
    
        if (sentCount > -1) {
            // Send message to MainActivity saying done(s) have been posted, so Snackbar can be shown/updated
            sendMessage("Sent " + sentCount + " tasks.", R.string.TASK_SUCCESSFUL, sentCount);
        }
    
        if (mFromPreFetch || sentCount > -1) {
            // Update local doneList from server
            new FetchDonesTask(mContext, mFromPreFetch).execute();
        }
    }
    
    
    private void sendMessage(String message, int action, int sentDoneCounter) {
        Intent intent = new Intent(mContext.getString(R.string.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
        
        // You can also include some extra data.
        intent.putExtra("sender", this.getClass().getSimpleName());
        intent.putExtra("action", action);
        intent.putExtra("count", sentDoneCounter);
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
    
    public class NewDoneClass {
        private transient final String dateFormat = "yyyy-MM-dd";
        private String done_date;
        private String team;
        private String raw_text;
        private String meta_data = "{\"from\":\"" + mContext.getString(R.string.app_name) + "\"}";
    
        public NewDoneClass(String doneText, String doneDate, String teamURL) {
            this.raw_text = doneText;
            
            if (doneDate != null && !doneDate.isEmpty())
                this.done_date = doneDate;
            else {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                
                this.done_date = sdf.format(new Date());
            }
        
            if (teamURL != null && !teamURL.isEmpty())
                this.team = teamURL;
            
        }
        
        public String getDone_date() {
            return done_date;
        }
        
        public void setDone_date(String done_date) {
            if (done_date != null && !done_date.isEmpty())
                this.done_date = done_date;
        }
        
        public void setTeamName(String teamName) {
            if (teamName != null && !teamName.isEmpty())
                this.team = teamName;
        }
        
        public String getRaw_text() {
            return raw_text;
        }
        
        public void setRaw_text(String raw_text) {
            if (raw_text != null && !raw_text.isEmpty())
                this.raw_text = raw_text;
        }
    }
    
}
