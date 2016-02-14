package net.c306.done;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

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
import java.text.SimpleDateFormat;
import java.util.Date;

public class NewDone extends AppCompatActivity {
    
    private final String NEW_DONE_LOG = "Done Text";
    private final String DONE_FILE_NAME = "PendingDonesFile";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_done);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_done_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        
        switch (item.getItemId()) {
            case R.id.action_add_dones:
                // User chose the "Add" item, save
                saveDone();
                return true;
            
            case R.id.action_settings:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                Intent settingsIntent = new Intent(NewDone.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            
        }
    }
    
    private void saveDone(){
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        String doneText = doneEditText.getText().toString();
        if(doneText.isEmpty()){
            doneEditText.setError(getString(R.string.done_edit_text_empty_error));
        } else {
            
            //// DONE: 14/02/16 Convert done to JSON string 
            NewDoneClass newDoneObj = new NewDoneClass(doneText);
            Gson gson = new Gson();
            String doneAsJSON = gson.toJson(newDoneObj);
            
            //// DONE: 14/02/16 Save new done to SharedPreferences with date as today and list as personal
            SharedPreferences settings = getSharedPreferences(DONE_FILE_NAME, 0);
            int doneId = settings.getInt("id", 0) + 1;
            
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("" + doneId, doneAsJSON);
            editor.putInt("id", doneId);
            editor.apply();
            
            Log.wtf(NEW_DONE_LOG, doneId + ": " + doneAsJSON);
            
            //// TODO: 14/02/16 Start async task to send new done to server. On success, remove done from SharedPreferences
            new PostNewDone().execute(doneAsJSON);
            
            
            setResult(RESULT_OK);
            finish();
        }
    }
    
    

    class PostNewDone extends AsyncTask<String, Void, String> {
        
        @Override
        protected String doInBackground(String... doneJSON) {
            
            HttpURLConnection httpcon;
            final String url = "https://idonethis.com/api/v0.1/dones/";
            String data = doneJSON[0];
            String result = null;
            
            try{
                //Connect
                httpcon = (HttpURLConnection) ((new URL(url).openConnection()));
                httpcon.setDoOutput(true);
                //Authorization: Token 9944b09199c62bcf9418ad846dd0e4bbdfc6ee4b
                httpcon.setRequestProperty("Authorization", "");
                httpcon.setRequestProperty("Content-Type", "application/json");
                httpcon.setRequestProperty("Accept", "application/json");
                httpcon.setRequestMethod("POST");
                httpcon.connect();
                
                //Write         
                OutputStream os = httpcon.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                writer.write(data);
                writer.close();
                os.close();
                
                //Read      
                BufferedReader br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(),"UTF-8"));
                
                String line = null;
                StringBuilder sb = new StringBuilder();
                
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                
                br.close();
                result = sb.toString();
                
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            return result;
        }
        
        @Override
        protected void onPostExecute(String aVoid) {
            super.onPostExecute(aVoid);
            
            sendMessage();
            // Send an Intent with an action named "custom-event-name". The Intent sent should 
            // be received by the ReceiverActivity.
        }
        
        private void sendMessage() {
            Intent intent = new Intent(getString(R.string.done_posted_intent));
            
            // You can also include some extra data.
            intent.putExtra("message", true);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    
    
    
    public class NewDoneClass {
        private transient final String dateFormat = "yyyy-MM-dd";
        private String done_date;
        private String team = "adityabhaskar";
        private String raw_text;
        private String meta_data = "{\"from\":\"doneFromAndroid\"}";
    
        public NewDoneClass(String doneText, String doneDate, String teamName){
            this.raw_text = doneText;
        
            if (doneDate != null && !doneDate.isEmpty())
                this.done_date = doneDate;
            else {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            
                this.done_date = sdf.format(new Date());
            }
        
            if (teamName != null && !teamName.isEmpty())
                this.team = teamName;
        
        }
        
        public NewDoneClass(String doneText, String doneDate){
            this.raw_text = doneText;
        
            if (doneDate != null && !doneDate.isEmpty())
                this.done_date = doneDate;
            else {
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            
                this.done_date = sdf.format(new Date());
            }
        }
    
        public NewDoneClass(String doneText){
            this.raw_text = doneText;
            
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
            this.done_date = sdf.format(new Date());
        }
    
        public String getDone_date() {
            return done_date;
        }
    
        public void setDone_date(String done_date) {
            if (done_date != null && !done_date.isEmpty())
                this.done_date = done_date;
        }
    
        public void setRaw_text(String raw_text) {
            if (raw_text != null && !raw_text.isEmpty())
                this.raw_text = raw_text;
        }
    
        public void setTeamName(String teamName) {
            if (teamName != null && !teamName.isEmpty())
                this.team = teamName;
        }
    }


}
