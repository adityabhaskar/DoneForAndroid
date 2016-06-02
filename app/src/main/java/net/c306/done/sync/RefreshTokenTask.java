package net.c306.done.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.Utils;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;


public class RefreshTokenTask extends AsyncTask<Void, Void, Boolean> {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    // Holds application context, passed in constructor
    private Context mContext;
    private Gson gson = new Gson();
    private String mRefreshToken;
    
    public RefreshTokenTask(Context c) {
        mContext = c;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        // Check if internet connection is available else cancel refresh 
        if (!Utils.isOnline(mContext)) {
            Log.w(LOG_TAG, "Offline, so cancelling token check");
            cancel(true);
            return;
        }
        
        // Get refresh token from SharedPrefs
        mRefreshToken = Utils.getRefreshToken(mContext);
        
        // Token not present or invalid
        if (mRefreshToken == null) {
            Log.e(LOG_TAG, "No Valid Refresh Token Found!");
            cancel(true);
            return;
        }
    
        Log.v(LOG_TAG, "Starting token refresh... ");
    }
    
    @Override
    protected Boolean doInBackground(Void... voids) {
        
        if (!isCancelled()) {
            
            HttpsURLConnection httpcon = null;
            
            //grant_type: 'refresh_token', refresh_token: REFRESH_TOKEN
            String url = Utils.IDT_ACCESS_TOKEN_URL + "?"
                    + "grant_type=refresh_token" + "&"
                    + "refresh_token=" + mRefreshToken;
            
            Log.v(LOG_TAG, "URL: " + url);
            
            try {
                //Connect
                httpcon = (HttpsURLConnection) (new URL(url).openConnection());
                httpcon.setDoOutput(true);
                httpcon.setRequestProperty("Authorization", IDTAuthStrings.AUTH_HEADER);
                httpcon.setRequestProperty("Cache-Control", "no-cache");
                httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpcon.setRequestProperty("Accept", "application/json");
                httpcon.setRequestMethod("POST");
                httpcon.setUseCaches(false);
                
                httpcon.connect();
                
                //Response Code
                int responseCode = httpcon.getResponseCode();
                String responseMessage = httpcon.getResponseMessage();
                String responseContent = Utils.readResponse(httpcon);
                
                Log.v(LOG_TAG, "Response from server: " + responseContent);
                
                if (responseCode == HttpURLConnection.HTTP_OK && !responseContent.equals("")) {
                    
                    Log.v(LOG_TAG, "Received refresh token response - " + responseCode + ": " + responseMessage);
                    
                    // Convert the string result to a JSON Object
                    JSONObject responseObject = new JSONObject(responseContent);
                    
                    // Extract data from JSON Response
                    int expiresIn = responseObject.has("expires_in") ? responseObject.getInt("expires_in") : 0;
                    String accessToken = responseObject.has("access_token") ? responseObject.getString("access_token") : null;
                    String refreshToken = responseObject.has("refresh_token") ? responseObject.getString("refresh_token") : null;
                    
                    if (expiresIn > 0 && accessToken != null) {
                        
                        Log.v(LOG_TAG, "This is the access Token: " + accessToken + ". It will expires in " + expiresIn + " secs");
                        
                        //Calculate date of expiration
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.SECOND, expiresIn);
                        long expireDate = calendar.getTimeInMillis();
                        
                        //Store both expires in and access token in shared preferences
                        SharedPreferences preferences = mContext.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putLong("expires", expireDate);
                        editor.putString("accessToken", accessToken);
                        editor.putString("refreshToken", refreshToken);
                        editor.apply();
                        
                        httpcon.disconnect();
                        return true;
                    }
                    
                } else {
                    Log.w(LOG_TAG, "Didn't receive new access token - " + responseCode + ": " + responseMessage);
                    Log.w(LOG_TAG, "Error message from server: " + responseContent);
                }
                
            } catch (Exception e) {
                Log.w(LOG_TAG, e.getMessage());
                e.printStackTrace();
            } finally {
                if (httpcon != null)
                    httpcon.disconnect();
            }
        }
        
        return false;
    }
    
    @Override
    protected void onPostExecute(Boolean status) {
        if (status) {
            Log.v(LOG_TAG, "Refreshed access token successfully.");
            
            // Update token and sync
            if (null != IDTAccountManager.updateSyncAccountToken(mContext)) {
                
                IDTSyncAdapter.syncImmediately(mContext);
                
                return;
            }
        }
        // Error handling - cancel activity, notify user.
        Log.v(LOG_TAG, "Error occurred, cancelling login attempt.");
        // TODO: 06/04/16 Notify user of refresh token error
        // TODO: Remove local accessToken, ask user to login again
        
        super.onPostExecute(status);
    }
}
