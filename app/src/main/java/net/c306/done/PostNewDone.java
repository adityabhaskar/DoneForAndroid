package net.c306.done;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
    private Context context;
    private Gson gson = new Gson();
    private List<String> pendingDonesArray = new ArrayList<String>();
    private String authToken;
    private int sentDoneCounter = 0;
    
    public PostNewDone(Context c){
        context = c;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        
        //// TODO: 15/02/16 Load new dones and token from shared preferences, and save in class variables 
        
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.done_file_name_shared_preferences), 0);
        
        String newDoneArrayString = settings.getString(context.getString(R.string.pending_done_array_name), "");
        authToken = settings.getString("authToken", "");
        
        if(authToken.equals("")){
            Log.e(context.getString(R.string.app_log_identifier), "No Auth Token Found!");
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
        
        Log.v(context.getString(R.string.app_log_identifier) + " pending dones #:", "" + pendingDonesArray.size());
        Log.v(context.getString(R.string.app_log_identifier) + " pending dones array:", "" + pendingDonesArray);
        
        String newDoneString;
        
        for (Iterator<String> iterator = pendingDonesArray.iterator(); iterator.hasNext(); ) {
            newDoneString = iterator.next();
            
            try{
                //Connect
                httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
                httpcon.setDoOutput(true);
                httpcon.setRequestProperty("Authorization", authToken);
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
                Log.wtf(context.getString(R.string.app_log_identifier) + " Send Done", resultStatus + ": " + httpcon.getResponseMessage());
                
                switch (resultStatus) {
                    case HttpURLConnection.HTTP_ACCEPTED:
                    case HttpURLConnection.HTTP_CREATED:
                    case HttpURLConnection.HTTP_OK:
                        Log.v(context.getString(R.string.app_log_identifier) + " Send Done" , " **OK**");
                        // increment sent dones counter
                        sentDoneCounter += 1;
                        // remove current item from doneList 
                        iterator.remove();
                        break; // fine
                    
                    case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                        Log.w(context.getString(R.string.app_log_identifier) + " Send Done" , " **gateway timeout**");
                        break;
                    
                    case HttpURLConnection.HTTP_UNAVAILABLE:
                        Log.w(context.getString(R.string.app_log_identifier) + " Send Done" , " **unavailable**");
                        break;// retry, server is unstable
                    
                    default:
                        Log.w(context.getString(R.string.app_log_identifier) + " Send Done" , " **unknown response code**.");
                }
                
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
        SharedPreferences settings = context.getSharedPreferences(context.getString(R.string.done_file_name_shared_preferences), 0);
        SharedPreferences.Editor editor = settings.edit();
        
        editor.putString(context.getString(R.string.pending_done_array_name), pendingDonesArrayString);
        editor.apply();
        
        
        sendMessage(response);
        // Send an Intent with an action named "custom-event-name". The Intent sent should 
        // be received by the ReceiverActivity.
    }
    
    private void sendMessage(String message) {
        Intent intent = new Intent(context.getString(R.string.done_posted_intent));
        
        // You can also include some extra data.
        intent.putExtra("count", sentDoneCounter);
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(intent);
    }
}
