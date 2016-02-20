package net.c306.done;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PostNewDone extends AsyncTask<String, Void, String> {
    
    // Holds server response code, or -1 for error 
    private int resultStatus;
    // Holds application context, passed in constructor
    private Context mContext;
    private Gson gson = new Gson();
    private List<String> pendingDonesArray = new ArrayList<String>();
    private String authToken;
    private String LOG_TAG;
    private int sentDoneCounter = 0;
    
    public PostNewDone(Context c){
        mContext = c;
        LOG_TAG = mContext.getString(R.string.app_log_identifier) + " " + FetchDonesTask.class.getSimpleName();
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        //// DONE: 15/02/16 Load new dones and token from shared preferences, and save in class variables 
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        String newDoneArrayString = settings.getString(mContext.getString(R.string.pending_done_array_name), "");
        authToken = settings.getString("authToken", "");
        
        if(authToken.equals("")){
            Log.e(LOG_TAG, "No Auth Token Found!");
            cancel(true);
        }
        
        // Get pending Dones ArrayList as JSON String 
        if(!newDoneArrayString.equals("")){
            pendingDonesArray = gson.fromJson(newDoneArrayString, ArrayList.class);
        }
    }
    
    @Override
    protected String doInBackground(String... doneJSON) {
        
        HttpURLConnection httpcon;
        final String url = "https://idonethis.com/api/v0.1/dones/";
        // Contains server response (or error message)
        String result = "";
        
        String newDoneString;
        
        for (Iterator<String> iterator = pendingDonesArray.iterator(); iterator.hasNext(); ) {
            newDoneString = iterator.next();
            
            try{
                //Connect
                httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
                httpcon.setDoOutput(true);
                httpcon.setRequestProperty("Authorization", "Token " + authToken);
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
                        Log.v(LOG_TAG + " Sent Done" , " **OK** - " + resultStatus + ": " + responseMessage);
                        // increment sent dones counter
                        sentDoneCounter += 1;
                        // remove current item from doneList 
                        iterator.remove();
                        break; // fine
                        
                    case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                        Log.w(LOG_TAG + " Didn't Send Done" , " **gateway timeout** - " + resultStatus + ": " + responseMessage);
                        break;
                    
                    case HttpURLConnection.HTTP_UNAVAILABLE:
                        Log.w(LOG_TAG + " Didn't Send Done" , " **unavailable** - " + resultStatus + ": " + responseMessage);
                        break;// retry, server is unstable
                    
                    default:
                        Log.w(LOG_TAG + " Didn't Send Done" , " **unknown response code** - " + resultStatus + ": " + responseMessage);
                        Log.wtf(LOG_TAG , " newDoneString - " + newDoneString);
                        
                        try {
                            //Read      
                            BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
    
                            String line = null;
                            StringBuilder sb = new StringBuilder();
    
                            while ((line = br.readLine()) != null) {
                                sb.append(line);
                            }
    
                            br.close();
                            Log.wtf(LOG_TAG, "Response message: " + sb);
                        } catch (Exception e){
                            Log.wtf(LOG_TAG, e.toString());
                        }
                }
                
                Log.wtf(LOG_TAG , " newDoneString - " + newDoneString);
                //Read      
                BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(),"UTF-8"));
                
                String line = null;
                StringBuilder sb = new StringBuilder();
                
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
        
                br.close();
                result += sb.toString() + "\n";
                
                httpcon.disconnect();
                
            } catch (UnsupportedEncodingException e) {
                result = e.getMessage();
                resultStatus = -1;
                e.printStackTrace();
            } catch (IOException e) {
                result = e.getMessage();
                resultStatus = -1;
                e.printStackTrace();
            }
            
        }
        
        
        return result;
    }
    
    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        
        // Serialize pendingDonesArray back to String for storage
        String pendingDonesArrayString = gson.toJson(pendingDonesArray, List.class);
        
        // Save remaining/empty pendingDoneList to SharedPrefs
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(mContext);
        //SharedPreferences settings = mContext.getSharedPreferences(mContext.getString(R.string.done_file_name_shared_preferences), 0);
        SharedPreferences.Editor editor = settings.edit();
        
        editor.putString(mContext.getString(R.string.pending_done_array_name), pendingDonesArrayString);
        editor.apply();
        
        // Send message to MainActivity saying done(s) have been posted, so Snackbar can be shown/updated
        sendMessage(response);
        
        // If done(s) sent successfully, update local doneList from server
        if(sentDoneCounter > 0) {
            new FetchDonesTask(mContext).execute();
        }
    }
    
    private void sendMessage(String message) {
        Intent intent = new Intent(mContext.getString(R.string.done_posted_intent));
        
        // You can also include some extra data.
        intent.putExtra("count", sentDoneCounter);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(intent);
    }
}
