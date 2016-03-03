package net.c306.done;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.PostNewDoneTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewDoneActivity extends AppCompatActivity {
    
    private String LOG_TAG;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_done);
        
        LOG_TAG = getString(R.string.app_log_identifier) + " " + NewDoneActivity.class.getSimpleName();
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
            
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            
        }
    }
    
    private void saveDone() {
        
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        String doneText = doneEditText.getText().toString();
        
        if (doneText.isEmpty()) {
            
            doneEditText.setError(getString(R.string.done_edit_text_empty_error));
            
        } else {
            
            //NewDoneClass newDoneObj = new NewDoneClass(doneText);
            String dateOfDone = null;
            
            // Get counter from sharedPreferences for new local done's id
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int localDoneIdCounter = prefs.getInt(getString(R.string.localDoneIdCounter), 0);
            localDoneIdCounter++;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(getString(R.string.localDoneIdCounter), localDoneIdCounter);
            editor.apply();
            
            //// DONE: 20/02/16 If done text starts with yyyy-mm-dd, set date to that date instead of today
            Pattern startsWithDatePattern = Pattern.compile("^(today|yesterday|((?:(\\d{4})(-?)(?:(?:(0[13578]|1[02]))(-?)(0[1-9]|[12]\\d|3[01])|(0[13456789]|1[012])(-?)(0[1-9]|[12]\\d|30)|(02)(-?)(0[1-9]|1\\d|2[0-8])))|([02468][048]|[13579][26])(-?)(0229))) ", Pattern.CASE_INSENSITIVE);
            Matcher matcher = startsWithDatePattern.matcher(doneText);
            
            if (matcher.find()) {
                dateOfDone = matcher.group().trim().toLowerCase();
                switch (dateOfDone) {
                    case "yesterday": {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        Calendar c = Calendar.getInstance();
                        c.setTime(new Date());
                        c.add(Calendar.DATE, -1);  // get yesterday's date
                        dateOfDone = sdf.format(c.getTime());
                        
                        String rawText = doneText.replaceFirst("[yY]esterday +", "").trim();
                        doneText = rawText.substring(0, 1).toUpperCase() + rawText.substring(1);
                        break;
                    }
                    
                    case "today": {
                        dateOfDone = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                        
                        String rawText = doneText.replaceFirst("[tT]oday +", "").trim();
                        doneText = rawText.substring(0, 1).toUpperCase() + rawText.substring(1);
                        break;
                    }
                }
            }
            
            // Save done with is_local = true to database
            ContentValues newDoneValues = new ContentValues();
            newDoneValues.put(DoneListContract.DoneEntry.COLUMN_NAME_ID, localDoneIdCounter);
            newDoneValues.put(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, doneText);
            newDoneValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME, "adityabhaskar");
            newDoneValues.put(DoneListContract.DoneEntry.COLUMN_NAME_OWNER, "adityabhaskar");
            newDoneValues.put(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, dateOfDone != null ? dateOfDone : new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            newDoneValues.put(DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL, "true");
            getContentResolver().insert(DoneListContract.DoneEntry.CONTENT_URI, newDoneValues);
            
            // Send DoneItem class to PostNewDoneTask
            
            // Don't send taskString to async task - let it read from database 
            new PostNewDoneTask(this).execute(false);
            
            setResult(RESULT_OK);
            finish();
        }
    }
    
    private boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        
        return activeNetwork != null &&
                activeNetwork.isConnected();
    }
    
}
