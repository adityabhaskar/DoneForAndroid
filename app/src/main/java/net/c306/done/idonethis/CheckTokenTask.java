package net.c306.done.idonethis;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.c306.done.Utils;
import net.c306.done.sync.IDTAccountManager;
import net.c306.done.sync.IDTSyncAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class CheckTokenTask extends AsyncTask<Void, Void, String> {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private Context mContext;
    private String mAuthToken;
    
    public CheckTokenTask(Context c) {
        mContext = c;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    
        // Check if internet connection is available else cancel fetch 
        if (!Utils.isOnline(mContext)) {
            Log.v(LOG_TAG, "Offline, so cancelling token check");
        
            Utils.sendMessage(mContext, Utils.SENDER_CHECK_TOKEN, "Offline", Utils.CHECK_TOKEN_CANCELLED_OFFLINE, -1);
            
            cancel(true);
            return;
        }
    
        // Reset validity to false before starting check
        Utils.setTokenValidity(mContext, false);
        
        // Get authtoken from SharedPrefs
        mAuthToken = Utils.getAuthTokenWithoutValidityCheck(mContext);
    
        if (mAuthToken == null) {
            Log.e(LOG_TAG, "No Auth Token Found!");
            Utils.sendMessage(mContext, Utils.SENDER_CHECK_TOKEN, "Empty Token", Utils.CHECK_TOKEN_FAILED, -1);
            cancel(true);
            return;
        }
    
        Utils.sendMessage(mContext, Utils.SENDER_CHECK_TOKEN, "Starting token check", Utils.CHECK_TOKEN_STARTED, 0);
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
                    Log.v(LOG_TAG, "Token accepted: " + " **OK** - " + resultStatus + ": " + responseMessage);
                    
                    // Set as valid
                    Utils.setTokenValidity(mContext, true);
    
                    // Read response for username      
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
                    Log.w(LOG_TAG, "Authcode invalid: " + " **invalid auth code** - " + resultStatus + ": " + responseMessage);
                    // Set as invalid
                    Utils.setTokenValidity(mContext, false);
    
                    Utils.sendMessage(mContext, Utils.SENDER_CHECK_TOKEN, responseMessage, Utils.CHECK_TOKEN_FAILED, -1);
                    result = null;
                    break;
                
                default:
                    Log.w(LOG_TAG, "Authcode invalid: " + " **unknown response code** - " + resultStatus + ": " + responseMessage);
                    Utils.sendMessage(mContext, Utils.SENDER_CHECK_TOKEN, responseMessage, Utils.CHECK_TOKEN_OTHER_ERROR, -1);
                    result = null;
                
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            Utils.sendMessage(mContext, Utils.SENDER_CHECK_TOKEN, e.getMessage(), Utils.CHECK_TOKEN_OTHER_ERROR, -1);
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
    
            // Create sync account with username
            IDTAccountManager.createSyncAccount(mContext, username);
            
            Log.v(LOG_TAG, "Received username: " + username);
            
        } else {
    
            Utils.setUsername(mContext, null);
            Utils.setTokenValidity(mContext, false);
            IDTAccountManager.removeSyncAccount(mContext);
            
        }
    
        return username;
    }
    
    
    //Result should be a) username or b) null
    @Override
    protected void onPostExecute(String username) {
        super.onPostExecute(username);
    
        if (username != null && !username.equals("")) {
            // Notify any local listeners about successful login
            Utils.sendMessage(mContext, Utils.SENDER_CHECK_TOKEN, username, Utils.CHECK_TOKEN_SUCCESSFUL, 1);
        
            IDTSyncAdapter.initializeSyncAdapter(mContext, username);
        }
    }
}