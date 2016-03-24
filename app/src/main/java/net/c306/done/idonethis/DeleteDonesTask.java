package net.c306.done.idonethis;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import net.c306.done.Utils;
import net.c306.done.db.DoneListContract;
import net.c306.done.sync.IDTAccountManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class DeleteDonesTask extends AsyncTask<Void, Void, Integer> {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    // Holds application context, passed in constructor
    private Context mContext;
    private String mAuthToken;
    
    public DeleteDonesTask(Context c) {
        mContext = c;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    
        // Check if internet connection is available else cancel fetch 
        if (!Utils.isOnline(mContext)) {
            Log.w(LOG_TAG, "Offline, so cancelling token check");
            Utils.sendMessage(mContext, Utils.SENDER_DELETE_TASK, "Offline", Utils.STATUS_TASK_CANCELLED_OFFLINE, -1);
            cancel(true);
            return;
        }
    
        // Get auth token from SharedPrefs
        mAuthToken = IDTAccountManager.getAuthToken(mContext);
    
        // Token not present or invalid
        if (mAuthToken == null) {
            Log.e(LOG_TAG, "No Valid Auth Token Found!");
            Utils.sendMessage(mContext, Utils.SENDER_DELETE_TASK, "No valid auth token found!", Utils.STATUS_TASK_UNAUTH, -1);
            cancel(true);
            return;
        }
        
        Log.v(LOG_TAG, "Starting delete...");
        Utils.sendMessage(mContext, Utils.SENDER_DELETE_TASK, "Starting delete...", Utils.STATUS_TASK_STARTED, 0);
    }
    
    @Override
    protected Integer doInBackground(Void... voids) {
        
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        final String url = "https://idonethis.com/api/v0.1/dones/";
        List<Integer> deletedDonesList = new ArrayList<>();
        int returnCount = 0;
        
        // Contains server response (or error message)
        String result = "";
    
        // Get deleted, non-local dones from database
        Cursor cursor = mContext.getContentResolver().query(
                DoneListContract.DoneEntry.buildDoneListUri(),                          // URI
                new String[]{                                                           // Projection
                        DoneListContract.DoneEntry.COLUMN_NAME_ID,
                },
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'FALSE' AND " +   // Selection
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'TRUE'",
                null,                                                                   // Selection Args
                null                                                                    // Sort Order
        );
    
        if (cursor != null) {
    
            Log.v(LOG_TAG, "Got " + cursor.getCount() + " pending dones to delete on server");
    
            if (cursor.getCount() > 0) {
        
                int resultStatus;
                int columnIndexID = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_ID);
        
                // Iterate over unsent dones 
                while (cursor.moveToNext() && !isCancelled()) {
                    int id = cursor.getInt(columnIndexID);
            
                    try {
                        //Connect
                        httpcon = (HttpURLConnection) ((new URL(url + id + "/").openConnection()));
                        httpcon.setRequestProperty("Authorization", "Token " + mAuthToken);
                        httpcon.setRequestProperty("Content-Type", "application/json");
                        httpcon.setRequestProperty("Accept", "application/json");
                        httpcon.setRequestMethod("DELETE");
                        httpcon.connect();
                
                        //Response Code
                        resultStatus = httpcon.getResponseCode();
                        String responseMessage = httpcon.getResponseMessage();
                
                
                        switch (resultStatus) {
                            case HttpURLConnection.HTTP_ACCEPTED:
                            case HttpURLConnection.HTTP_NO_CONTENT:
                            case HttpURLConnection.HTTP_OK:
                                Log.v(LOG_TAG, "Task Deleted " + id + " - " + resultStatus + ": " + responseMessage);
                                deletedDonesList.add(id);
                                returnCount++;
                                break; // fine
                    
                            case HttpURLConnection.HTTP_UNAUTHORIZED:
                                Log.w(LOG_TAG, "Didn't delete task - " + resultStatus + ": " + responseMessage);
                                // Set token invalid
                                Utils.setTokenValidity(mContext, false);
                                Utils.sendMessage(mContext, Utils.SENDER_DELETE_TASK, responseMessage, Utils.STATUS_TASK_UNAUTH, -1);
                                cancel(true);
                                returnCount = -1;
                                break;
                    
                            default:
                                Log.w(LOG_TAG, "Didn't delete task - " + resultStatus + ": " + responseMessage);
                                //returnCount = -1;
                                
                        }
                
                        //Read      
                        br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
                
                        String line;
                        StringBuilder sb = new StringBuilder();
                
                        while ((line = br.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                
                        result += sb.toString();
                
                    } catch (Exception e) {
                
                        result += e.getMessage();
                        e.printStackTrace();
                        Log.w(LOG_TAG, "delete id: " + id + ", " + e.getMessage());
                
                    } finally {
                
                        if (httpcon != null)
                            httpcon.disconnect();
                
                        if (br != null) {
                            try {
                                br.close();
                            } catch (final IOException e) {
                                Log.e(LOG_TAG, "Error closing stream", e);
                            }
                        }
                    }
                }
            }
            cursor.close();
    
            if (deletedDonesList.size() > 0) {
                // Delete from database dones sent to server
                String sentDonesIdString = TextUtils.join(",", deletedDonesList);
        
                mContext.getContentResolver().delete(
                        DoneListContract.DoneEntry.CONTENT_URI,
                        DoneListContract.DoneEntry.COLUMN_NAME_ID + " IN (" + sentDonesIdString + ")",
                        null
                );
            }
        }
    
        Log.v(LOG_TAG, "Response from server: " + result);
    
        return returnCount;
    }
    
    @Override
    protected void onPostExecute(Integer deletedCount) {
        super.onPostExecute(deletedCount);
        
        // If done(s) sent successfully, update local doneList from server
        if (deletedCount > -1) {
            // Send message to MainActivity saying done(s) have been posted, so Snackbar can be shown/updated
            Utils.sendMessage(mContext, Utils.SENDER_DELETE_TASK, "Deleted " + deletedCount + " tasks on server", Utils.STATUS_TASK_SUCCESSFUL, deletedCount);
        
            // If no error, call next chained task
            new PostEditedDoneTask(mContext).execute();
        }
    }
}
