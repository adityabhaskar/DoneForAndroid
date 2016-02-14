package net.c306.done;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

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
    
            Log.wtf(NEW_DONE_LOG, doneId + " " + doneAsJSON);
            //// TODO: 14/02/16 Start async task to send new done to server. On success, remove done from SharedPreferences
            
            
            Toast t = Toast.makeText(getApplicationContext(), R.string.done_toast_message, Toast.LENGTH_SHORT);
            t.show();
            finish();
        }
    }
    
    
    /* Inner class to get response */
    //import org.apache.http.HttpResponse;
    //import org.apache.http.NameValuePair;
    //import org.apache.http.client.HttpClient;
    //import org.apache.http.client.entity.UrlEncodedFormEntity;
    //import org.apache.http.client.methods.HttpPost;
    //import org.apache.http.impl.client.DefaultHttpClient;
    //import org.apache.http.message.BasicNameValuePair;
    //import org.json.JSONObject;
/*
    class AsyncT extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("<YOUR_SERVICE_URL>");
            
            try {
                
                JSONObject jsonobj = new JSONObject();
                
                jsonobj.put("name", "Aneh");
                jsonobj.put("age", "22");
                
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("req", jsonobj.toString()));
                
                Log.e("mainToPost", "mainToPost" + nameValuePairs.toString());
                
                // Use UrlEncodedFormEntity to send in proper format which we need
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                
                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                InputStream inputStream = response.getEntity().getContent();
                //InputStreamToStringExample str = new InputStreamToStringExample();
                //responseServer = str.getStringFromInputStream(inputStream);
                //Log.e("response", "response -----" + responseServer);
                
                
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            
            txt.setText(responseServer);
        }
    }
*/
    
    
    
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
