package net.c306.done;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by raven on 16/02/16.
 */
public class FetchDones extends AsyncTask<Void, Void, String> {
    
    private Context context;
    private String authToken;
    private int resultStatus;
    
    public FetchDones(Context c){
        context = c;
        
        // Get authtoken from SharedPrefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        authToken = prefs.getString("authToken", "");
        
        if(authToken.equals("")){
            Log.e(context.getString(R.string.app_log_identifier), "No Auth Token Found!");
            cancel(true);
        }
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    
    @Override
    protected String doInBackground(Void... params) {
        HttpURLConnection httpcon;
        final String url = "https://idonethis.com/api/v0.1/dones/";
        // Contains server response (or error message)
        String result = "";
        
            try{
                //Connect
                httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
                httpcon.setRequestProperty("Authorization", "Token " + authToken);
                httpcon.setRequestProperty("Content-Type", "application/json");
                httpcon.setRequestProperty("Accept", "application/json");
                httpcon.setRequestMethod("GET");
                httpcon.connect();
                
                //Write         
                //OutputStream os = httpcon.getOutputStream();
                //BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                //writer.write(newDoneString);
                //writer.close();
                //os.close();
                
                //Response Code
                resultStatus = httpcon.getResponseCode();
                String responseMessage = httpcon.getResponseMessage();
                //Log.wtf(context.getString(R.string.app_log_identifier) + " Sent Done", resultStatus + ": " + responseMessage);
    
                switch (resultStatus) {
                    case HttpURLConnection.HTTP_ACCEPTED:
                    case HttpURLConnection.HTTP_CREATED:
                    case HttpURLConnection.HTTP_OK:
                        Log.v(context.getString(R.string.app_log_identifier) + " Sent Done" , " **OK** - " + resultStatus + ": " + responseMessage);
                        break; // fine
                    
                    case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                        Log.w(context.getString(R.string.app_log_identifier) + " Didn't Send Done" , " **gateway timeout** - " + resultStatus + ": " + responseMessage);
                        break;
                    
                    case HttpURLConnection.HTTP_UNAVAILABLE:
                        Log.w(context.getString(R.string.app_log_identifier) + " Didn't Send Done" , " **unavailable** - " + resultStatus + ": " + responseMessage);
                        break;// retry, server is unstable
                    
                    default:
                        Log.w(context.getString(R.string.app_log_identifier) + " Didn't Send Done" , " **unknown response code** - " + resultStatus + ": " + responseMessage);
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
        
        return result;
    }
    
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Log.wtf(context.getString(R.string.app_log_identifier) + "GET Result", result);
    }
}
