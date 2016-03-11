package net.c306.done;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

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
    List<String> teamNames = new ArrayList<>();
    List<String> teamURLs = new ArrayList<>();
    private String LOG_TAG;
    private long mId;
    private List<String> mEditedFields;
    private String mDoneDate = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOG_TAG = getString(R.string.APP_LOG_IDENTIFIER) + " " + this.getClass().getSimpleName();
        
        
        setContentView(R.layout.activity_new_done);
    
        Intent sender = getIntent();
        mId = sender.getLongExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, -1);
    
        populateTeamPicker();
    
        if (mId > -1)
            populateForEdit(sender);
    }
    
    private void populateTeamPicker() {
        Cursor teamsCursor = getContentResolver().query(
                DoneListContract.TeamEntry.CONTENT_URI,
                new String[]{
                        DoneListContract.TeamEntry.COLUMN_NAME_ID + " AS _id",
                        DoneListContract.TeamEntry.COLUMN_NAME_NAME,
                        DoneListContract.TeamEntry.COLUMN_NAME_SHORT_NAME,
                        DoneListContract.TeamEntry.COLUMN_NAME_URL
                },
                null,
                null,
                null
        );
        
        if (teamsCursor != null && teamsCursor.getCount() > 0) {
            
            int columnTeamName = teamsCursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_NAME);
            int columnTeamURL = teamsCursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_URL);
            
            while (teamsCursor.moveToNext()) {
                teamNames.add(teamsCursor.getString(columnTeamName));
                teamURLs.add(teamsCursor.getString(columnTeamURL));
            }
            
            teamsCursor.close();
        } else
            Log.w(LOG_TAG, "Team cursor returned empty");
        
        Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
        
        ArrayAdapter adapter = new ArrayAdapter(this, R.layout.team_selector_spinner, android.R.id.text1, teamNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamPicker.setAdapter(adapter);
        
        // Set default team as initial selection
        teamPicker.setSelection(
                teamURLs.indexOf(Utils.getDefaultTeam(this))
        );
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
    
        String team = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_TEAM);
        mDoneDate = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
        mEditedFields = sender.getStringArrayListExtra(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS);
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, team);
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
        mPreEditBundle.putStringArrayList(
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS,
                (ArrayList<String>) mEditedFields
        );
    
        Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
        teamPicker.setSelection(
                teamURLs.indexOf(team)
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
        String taskText = doneEditText.getText().toString();
    
        if (taskText.isEmpty()) {
            
            doneEditText.setError(getString(R.string.done_edit_text_empty_error));
            
        } else {
            // Edit or Add done based on if mId > -1
            
            if (mId > -1) {
                // Case: Edit this done
    
                Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
                String taskTeam = teamURLs.get(teamPicker.getSelectedItemPosition());
    
                if (taskText.equals(mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)) &&
                        taskTeam.equals(mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM))
                        ) {
                    // No change made
                    setResult(RESULT_OK);
                    Log.v(LOG_TAG, "No change made.");
                    finish();
                    return;
                }
                
                if (!mEditedFields.contains(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)) {
                    mEditedFields.add(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
                }
    
                // Save edited values to database, mark as edited
                Bundle editedDetails = new Bundle();
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, taskText);
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, taskTeam);
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
    
                String taskDate = null;
                Bundle newDoneDetails = new Bundle();
                
                // Get counter from sharedPreferences for new local done's id
                int localTaskIdCounter = Utils.getLocalDoneIdCounter(this) + 1;
                Utils.setLocalDoneIdCounter(this, localTaskIdCounter);
    
                // If done text starts with yyyy-mm-dd, set date to that date instead of today
                Pattern startsWithDatePattern = Pattern.compile("^(today|yesterday|((?:(\\d{4})(-?)(?:(?:(0[13578]|1[02]))(-?)(0[1-9]|[12]\\d|3[01])|(0[13456789]|1[012])(-?)(0[1-9]|[12]\\d|30)|(02)(-?)(0[1-9]|1\\d|2[0-8])))|([02468][048]|[13579][26])(-?)(0229))) +", Pattern.CASE_INSENSITIVE);
                Matcher matcher = startsWithDatePattern.matcher(taskText);
                
                if (matcher.find()) {
                    taskDate = matcher.group().trim().toLowerCase();
    
                    // Remove date string from done text, capitalize first character
                    String rawText = taskText.replaceFirst(matcher.group(), "").trim();
                    taskText = rawText.substring(0, 1).toUpperCase() + rawText.substring(1);
    
                    switch (taskDate) {
                        case "yesterday": {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            Calendar c = Calendar.getInstance();
                            c.setTime(new Date());
                            c.add(Calendar.DATE, -1);  // get yesterday's date
                            taskDate = sdf.format(c.getTime());
                            
                            break;
                        }
                        
                        case "today": {
                            taskDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                            break;
                        }
    
                    }
                } else {
                    taskDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
                }
    
                Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
                String taskTeam = teamURLs.get(teamPicker.getSelectedItemPosition());
                
                // Add temporary id to new done bundle
                newDoneDetails.putLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, localTaskIdCounter);
                // Add team to bundle. Later, to be got from selector
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, taskTeam);
                // Add done_date to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, taskDate);
                // Add cleaned (of date strings) taskText to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, taskText);
    
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
