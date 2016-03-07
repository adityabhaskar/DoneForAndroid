package net.c306.done;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewDoneActivity extends AppCompatActivity {
    
    //Temporary, till I implement date & team selectors
    Bundle mPreEditBundle = new Bundle();
    private String LOG_TAG;
    private long mId;
    private List<String> mEditedFields;
    private String mTeam = null;
    private String mDoneDate = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG_TAG = getString(R.string.APP_LOG_IDENTIFIER) + " " + this.getClass().getSimpleName();
        
        
        setContentView(R.layout.activity_new_done);
    
        Intent sender = getIntent();
        mId = sender.getLongExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, -1);
    
        if (mId > -1)
            populateForEdit(sender);
    }
    
    private void populateForEdit(Intent sender) {
        // Edit activity title to edit task, instead of 'Done!'
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Edit Task");
        }
        
        // Populate activity view with details
        EditText editText = (EditText) findViewById(R.id.done_edit_text);
        editText.setText(sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT));
        mPreEditBundle.putString(
                DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT,
                sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)
        );
        
        // Set cursor at end of text
        editText.setSelection(editText.getText().length());
    
        mTeam = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_TEAM);
        mDoneDate = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
        mEditedFields = sender.getStringArrayListExtra(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS);
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, mTeam);
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
        mPreEditBundle.putStringArrayList(
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS,
                (ArrayList<String>) mEditedFields
        );
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
                onSaveClicked();
                return true;
            
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            
        }
    }
    
    private void onSaveClicked() {
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        String doneText = doneEditText.getText().toString();
        
        if (doneText.isEmpty()) {
            
            doneEditText.setError(getString(R.string.done_edit_text_empty_error));
            
        } else {
            // Edit or Add done based on if mId > -1
            
            if (mId > -1) {
                // Case: Edit this done
    
                if (doneText.equals(mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT))) {
                    // No change made
                    setResult(RESULT_OK);
                    finish();
                    return;
                }
                
                if (!mEditedFields.contains(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)) {
                    mEditedFields.add(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
                }
                
                // Save edited values to database, mark as edited
                Bundle editedDetails = new Bundle();
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, doneText);
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, mTeam);
                editedDetails.putLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, mId);
                editedDetails.putStringArrayList(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS, (ArrayList<String>) mEditedFields);
                
                boolean result = new DoneActions(this).edit(editedDetails);
                
                if (result) {
                    setResult(RESULT_OK);
                } else {
                    setResult(R.integer.RESULT_ERROR);
                }
                
            } else {
                // Case: Add new done
                
                String dateOfDone = null;
                Bundle newDoneDetails = new Bundle();
                
                // Get counter from sharedPreferences for new local done's id
                int localDoneIdCounter = Utils.getLocalDoneIdCounter(this) + 1;
                Utils.setLocalDoneIdCounter(this, localDoneIdCounter);
    
                // If done text starts with yyyy-mm-dd, set date to that date instead of today
                Pattern startsWithDatePattern = Pattern.compile("^(today|yesterday|((?:(\\d{4})(-?)(?:(?:(0[13578]|1[02]))(-?)(0[1-9]|[12]\\d|3[01])|(0[13456789]|1[012])(-?)(0[1-9]|[12]\\d|30)|(02)(-?)(0[1-9]|1\\d|2[0-8])))|([02468][048]|[13579][26])(-?)(0229))) +", Pattern.CASE_INSENSITIVE);
                Matcher matcher = startsWithDatePattern.matcher(doneText);
                
                if (matcher.find()) {
                    dateOfDone = matcher.group().trim().toLowerCase();
    
                    // Remove date string from done text, capitalize first character
                    String rawText = doneText.replaceFirst(matcher.group(), "").trim();
                    doneText = rawText.substring(0, 1).toUpperCase() + rawText.substring(1);
                    
                    switch (dateOfDone) {
                        case "yesterday": {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            Calendar c = Calendar.getInstance();
                            c.setTime(new Date());
                            c.add(Calendar.DATE, -1);  // get yesterday's date
                            dateOfDone = sdf.format(c.getTime());
                            
                            break;
                        }
                        
                        case "today": {
                            dateOfDone = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                            break;
                        }
    
                    }
                } else {
                    dateOfDone = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                }
    
                // Add temporary id to new done bundle
                newDoneDetails.putLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, localDoneIdCounter);
                // Add team to bundle. Later, to be got from selector
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME, getString(R.string.DEV_TEAM_NAME));
                // Add done_date to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, dateOfDone);
                // Add cleaned (of date strings) doneText to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, doneText);
    
                boolean result = new DoneActions(this).create(newDoneDetails);
    
                if (result) {
                    setResult(RESULT_OK);
                } else {
                    setResult(R.integer.RESULT_ERROR);
                }
            }
            
            finish();
        }
    }
}
