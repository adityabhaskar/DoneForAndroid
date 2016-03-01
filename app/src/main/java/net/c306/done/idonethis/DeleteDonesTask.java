package net.c306.done.idonethis;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.c306.done.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

// TODO: 29/02/16 Correct strings for messaging and logs 
public class DeleteDonesTask extends AsyncTask<long[], Void, String> {
    
    // Holds server response code, or -1 for error 
    private int resultStatus;
    // Holds application context, passed in constructor
    private Context mContext;
    //private long[] idList;
    private String authToken;
    private String LOG_TAG;
    private int deletedCounter = 0;
    
    public DeleteDonesTask(Context c) {
        mContext = c;
        //this.idList = idList;
        LOG_TAG = mContext.getString(R.string.app_log_identifier) + " " + DeleteDonesTask.class.getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        //// DONE: 22/02/16 Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            // TODO: 29/02/16 If offline, add to pending - do in DeleteDonesTask
            Log.w(LOG_TAG, "Offline, so cancelling token check");
            sendMessage("Offline", mContext.getString(R.string.postnewdone_cancelled_offline));
            cancel(true);
            return;
        }
        
        //// DONE: 15/02/16 Load new dones and token from shared preferences, and save in class variables 
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        String newDoneArrayString = settings.getString(mContext.getString(R.string.pending_done_array_name), "");
        authToken = settings.getString(mContext.getString(R.string.auth_token), "");
        
        if (authToken.equals("")) {
            Log.e(LOG_TAG, "No Auth Token Found!");
            sendMessage("No auth token found!", mContext.getString(R.string.fetch_teams_unauth));
            cancel(true);
            return;
        }
        
        Log.v(LOG_TAG, "Starting delete...");
        sendMessage("Starting delete...", mContext.getString(R.string.fetch_teams_started));
    }
    
    @Override
    protected String doInBackground(long[]... idList) {
        
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        final String url = "https://idonethis.com/api/v0.1/dones/";
        // Contains server response (or error message)
        String result = "";
        
        Log.v(LOG_TAG, "Received delete-from-server list: " + Arrays.toString(idList[0]));
        
        for (long id : idList[0]) {
            
            try {
                //Connect
                httpcon = (HttpURLConnection) ((new URL(url + id + "/").openConnection()));
                httpcon.setRequestProperty("Authorization", "Token " + authToken);
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
                        // increment deleted dones counter
                        deletedCounter += 1;
                        break; // fine
                    
                    case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                    case HttpURLConnection.HTTP_UNAVAILABLE:
                        Log.w(LOG_TAG, "Didn't delete task - " + resultStatus + ": " + responseMessage);
                        //sendMessage("Server/gateway unavailable", mContext.getString(R.string.postnewdone_other_error));
                        //cancel(true);
                        return null;
                    
                    case HttpURLConnection.HTTP_FORBIDDEN:
                    case HttpURLConnection.HTTP_UNAUTHORIZED:
                        Log.w(LOG_TAG, "Didn't delete task - " + resultStatus + ": " + responseMessage);
                        //sendMessage(null, mContext.getString(R.string.postnewdone_unauth));
                        //cancel(true);
                        return null;
                    
                    default:
                        Log.w(LOG_TAG, "Didn't delete task - " + resultStatus + ": " + responseMessage);
                        //sendMessage(null, mContext.getString(R.string.postnewdone_other_error));
                    
                }
                
                //Read      
                br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
                
                String line = null;
                StringBuilder sb = new StringBuilder();
                
                while ((line = br.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                
                result += sb.toString();
                
            } catch (Exception e) {
                result += e.getMessage();
                resultStatus = -1;
                e.printStackTrace();
                Log.w(LOG_TAG, "delete id: " + id + ", " + e.getMessage());
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
        
        
        return result;
    }
    
    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        
        // If done(s) sent successfully, update local doneList from server
        if (deletedCounter > 0) {
            // Send message to MainActivity saying done(s) have been posted, so Snackbar can be shown/updated
            sendMessage(response, mContext.getString(R.string.postnewdone_finished));
            
            new FetchDonesTask(mContext, R.string.main_activity_listener_intent).execute();
        }
    }
    
    private void sendMessage(String message, String action) {
        Intent intent = new Intent(mContext.getString(R.string.main_activity_listener_intent));
        
        // You can also include some extra data.
        intent.putExtra("sender", "PostNewDoneTask");
        intent.putExtra("action", action);
        intent.putExtra("count", deletedCounter);
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
