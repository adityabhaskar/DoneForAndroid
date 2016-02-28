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
import net.c306.done.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * Created by raven on 16/02/16.
 */
public class CheckTokenTask extends AsyncTask<Void, Void, String> {
    
    private String LOG_TAG;
    private Context mContext;
    private String mAuthToken;
    
    public CheckTokenTask(Context c) {
        mContext = c;
        LOG_TAG = mContext.getString(R.string.app_log_identifier) + " " + CheckTokenTask.class.getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        // DONE: 22/02/16 Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            Log.v(LOG_TAG, "Offline, so cancelling token check");
            
            sendMessage("Offline", mContext.getString(R.string.check_token_cancelled_offline));
            
            Utils.addToPendingActions(mContext, mContext.getString(R.string.pending_action_authenticate_token), 0);
            
            cancel(true);
            return;
        }
        
        // Get authtoken from SharedPrefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mAuthToken = prefs.getString(mContext.getString(R.string.auth_token), "");
        
        if (mAuthToken.equals("")) {
            Log.e(LOG_TAG, "No Auth Token Found!");
            sendMessage("Empty Token", mContext.getString(R.string.check_token_failed));
            cancel(true);
        }
    }
    
    @Override
    protected String doInBackground(Void... params) {
        
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        final String NOOP_URL = "https://idonethis.com/api/v0.1/noop";
        
        
        // Contains server response (or error message)
        String result = "";
        
        try {
            //Connect
            httpcon = (HttpURLConnection) ((new URL(NOOP_URL).openConnection()));
            httpcon.setRequestProperty("Authorization", "Token " + mAuthToken);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("GET");
            httpcon.connect();
            
            //Response Code
            int resultStatus = httpcon.getResponseCode();
            String responseMessage = httpcon.getResponseMessage();
            
            switch (resultStatus) {
                case HttpURLConnection.HTTP_OK:
                    Log.v(LOG_TAG + " Token accepted", " **OK** - " + resultStatus + ": " + responseMessage);
                    
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
                    } else {
                        result = getUsernameFromJson(result);
                    }
                    
                    break; // fine
                
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                case HttpURLConnection.HTTP_UNAVAILABLE:
                    Log.w(LOG_TAG + " Couldn't check done", " **gateway timeout** - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.check_token_other_error));
                    result = null;
                    break;
                
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG + " Authcode invalid", " **invalid auth code** - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.check_token_failed));
                    result = null;
                    break;
                
                default:
                    Log.w(LOG_TAG + " Authcode invalid", " **unknown response code** - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, mContext.getString(R.string.check_token_other_error));
                    result = null;
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            sendMessage(e.getMessage(), mContext.getString(R.string.check_token_other_error));
            result = null;
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
    private String getUsernameFromJson(String userDetailsJsonStr) throws JSONException {
        
        JSONObject masterObj = new JSONObject(userDetailsJsonStr);
        String username = masterObj.getString("user");
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (null != username && !username.equals("")) {
            // Save username in shared preferences
            editor.putString("username", username);
            editor.apply();
            
            Log.v(LOG_TAG, "Received username: " + username);
            
        } else {
            
            editor.remove("username");
            
        }
        return username;
    }
    
    
    //Result should be a) username or b) null
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        
        if (result != null) {
            // Notify any local listeners about successful login
            sendMessage(result, mContext.getString(R.string.check_token_successful));
            
            Utils.removeFromPendingActions(mContext, mContext.getString(R.string.pending_action_authenticate_token));
            
            // DONE: 27/02/16 Fetch teams
            new FetchTeamsTask(mContext).execute();
        } else {
            Utils.addToPendingActions(mContext, mContext.getString(R.string.pending_action_authenticate_token), 0);
        }
    }
    
    private void sendMessage(String message, String action) {
        Intent intent = new Intent(mContext.getString(R.string.settings_activity_listener_intent));
        
        // You can also include some extra data.
        intent.putExtra("sender", "CheckTokenTask");
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