package net.c306.done;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewDone extends AppCompatActivity {
    
    private String LOG_TAG;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_done);
    
        LOG_TAG = getString(R.string.app_log_identifier) + " " + FetchDonesTask.class.getSimpleName();
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
            
            //case R.id.action_settings:
            //    // User chose the "Favorite" action, mark the current item
            //    // as a favorite...
            //    Intent settingsIntent = new Intent(NewDone.this, SettingsActivity.class);
            //    startActivity(settingsIntent);
            //    return true;
            
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            
        }
    }
    
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }
    
    private void saveDone(){
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        String doneText = doneEditText.getText().toString();
        
        if(doneText.isEmpty())
            doneEditText.setError(getString(R.string.done_edit_text_empty_error));
        else {
            
            //// DONE: 14/02/16 Convert done to JSON string with list as personal, date as Today
            NewDoneClass newDoneObj = new NewDoneClass(doneText);
            Gson gson = new Gson();
            String doneAsJSON = gson.toJson(newDoneObj);
            
            //// DONE: 14/02/16 Save new done to SharedPreferences
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
            String pendingDonesArrayString = settings.getString(getString(R.string.pending_done_array_name), "");
            
            // Get pending Dones ArrayList as JSON String 
            List<String> pendingDonesArray = new ArrayList<>();
            if(!pendingDonesArrayString.equals(""))
                pendingDonesArray = gson.fromJson(pendingDonesArrayString, ArrayList.class);
            
            // Add new done to ArrayList
            pendingDonesArray.add(doneAsJSON);
            
            // Serialize ArrayList back to String for storage
            pendingDonesArrayString = gson.toJson(pendingDonesArray, List.class);
            
            // Save pending Done ArrayList to SharedPrefs
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(getString(R.string.pending_done_array_name), pendingDonesArrayString);
            editor.apply();
            
            Log.v(LOG_TAG, "Saved Done Text" + doneAsJSON);
            Log.v(LOG_TAG, "Pending List Size" + pendingDonesArray.size());
            
            //// DONE: 14/02/16 Start async task to send new done to server. On success, remove done from SharedPreferences
            if(isOnline()) {
                //// DONE: 15/02/16 Don't send taskString to async task - let it read from storage and process all pending 
                new PostNewDone(this).execute();
                setResult(RESULT_OK);
            
            } else{
                
                setResult(R.integer.result_offline);
                
                //// TODO: 15/02/16 Set alarm to check when back online, to sync messages 
            }
            
            
            finish();
        }
    }
    
    
    public class NewDoneClass {
        private transient final String dateFormat = "yyyy-MM-dd";
        private String done_date;
        private String team = "adityabhaskar";
        private String raw_text;
        private String meta_data = "{\"from\":\""+ getString(R.string.app_name) +"\"}";
    
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
                SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
            
                this.done_date = sdf.format(new Date());
            }
        }
    
        public NewDoneClass(String doneText){
            this.raw_text = doneText;
            
            SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.US);
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

