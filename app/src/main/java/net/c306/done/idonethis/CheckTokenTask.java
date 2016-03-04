package net.c306.done.idonethis;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
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
        LOG_TAG = mContext.getString(R.string.APP_LOG_IDENTIFIER) + " " + this.getClass().getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        // DONE: 22/02/16 Check if internet connection is available else cancel fetch 
        if (!isOnline()) {
            Log.v(LOG_TAG, "Offline, so cancelling token check");
    
            sendMessage("Offline", R.string.CHECK_TOKEN_CANCELLED_OFFLINE);
            
            cancel(true);
            return;
        }
        
        // Get authtoken from SharedPrefs
        mAuthToken = Utils.getAuthTokenWithoutValidityCheck(mContext);
    
        if (mAuthToken == null) {
            Log.e(LOG_TAG, "No Auth Token Found!");
            sendMessage("Empty Token", R.string.CHECK_TOKEN_FAILED);
            cancel(true);
        }
    
        // Reset validity to false before starting check
        Utils.setTokenValidity(mContext, false);
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
                    // Set as valid
                    Utils.setTokenValidity(mContext, true);
                    
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
                
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG + " Authcode invalid", " **invalid auth code** - " + resultStatus + ": " + responseMessage);
                    // Set as invalid
                    Utils.setTokenValidity(mContext, false);
    
                    sendMessage(responseMessage, R.string.CHECK_TOKEN_FAILED);
                    result = null;
                    break;
                
                default:
                    Log.w(LOG_TAG + " Authcode invalid", " **unknown response code** - " + resultStatus + ": " + responseMessage);
                    sendMessage(responseMessage, R.string.CHECK_TOKEN_OTHER_ERROR);
                    result = null;
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            sendMessage(e.getMessage(), R.string.CHECK_TOKEN_OTHER_ERROR);
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
    
    
    private String getUsernameFromJson(String userDetailsJsonStr) throws JSONException {
        
        JSONObject masterObj = new JSONObject(userDetailsJsonStr);
        String username = masterObj.getString("user");
        
        if (null != username && !username.equals("")) {
    
            // Save username in shared preferences
            Utils.setUsername(mContext, username);
            
            Log.v(LOG_TAG, "Received username: " + username);
            
        } else {
    
            Utils.setUsername(mContext, null);
            Utils.setTokenValidity(mContext, false);
            
        }
    
        return username;
    }
    
    
    //Result should be a) username or b) null
    @Override
    protected void onPostExecute(String username) {
        super.onPostExecute(username);
    
        if (username != null) {
            // Notify any local listeners about successful login
            sendMessage(username, R.string.CHECK_TOKEN_SUCCESSFUL);
        
            // Fetch teams
            new FetchTeamsTask(mContext).execute();
            
        }
    }
    
    private void sendMessage(String message, int action) {
        Intent intent = new Intent(mContext.getString(R.string.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
        
        // You can also include some extra data.
        intent.putExtra("sender", this.getClass().getSimpleName());
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