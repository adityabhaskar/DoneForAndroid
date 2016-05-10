package net.c306.done;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import com.google.android.gms.analytics.Tracker;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewDoneActivity extends AppCompatActivity {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    Bundle mPreEditBundle = new Bundle();
    List<String> teamNames = new ArrayList<>();
    List<String> teamURLs = new ArrayList<>();
    private Tracker mTracker;
    private SimpleDateFormat userDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
    private SimpleDateFormat idtDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    
    private long mId;
    private String mDoneDate = idtDateFormat.format(new Date());
    private String mFormattedDoneDate = userDateFormat.format(new Date());
    private String mTeam;
    private List<String> mEditedFields;
    
    private DatePickerDialog mDatePicker;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_new_done);
    
        Intent sender = getIntent();
        mId = sender.getLongExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, -1);
        mTeam = Utils.getDefaultTeam(NewDoneActivity.this);
    
        populateTeamPicker();
        
        if (mId > -1)
            populateForEdit(sender);
        else {
            // Populate task text from saved data, if available
            String[] state = Utils.getNewTaskActivityState(this);
    
            if (state.length > 0) {
                Log.v(LOG_TAG, "Founds saved items: " + Arrays.toString(state));
                EditText editText = (EditText) findViewById(R.id.done_edit_text);
                if (editText != null) {
                    editText.setText(state[0]);
                    editText.setSelection(editText.getText().length());
                }
        
                // Get currentTeam from state
                mTeam = state[1];
        
                // Get currentDate from state
                if (state.length >= 3 && state[2] != null && !state[2].equals("")) {
                    String[] dateParts = state[2].split("\\-");
    
                    Calendar c = Calendar.getInstance();
                    c.set(
                            Integer.parseInt(dateParts[0]),
                            Integer.parseInt(dateParts[1]) - 1,
                            Integer.parseInt(dateParts[2])
                    );
                    mFormattedDoneDate = userDateFormat.format(c.getTime());
                }
            } else
                Log.i(LOG_TAG, "No saved state found.");
    
            EditText teamEditText = (EditText) findViewById(R.id.team_picker);
            if (teamEditText != null) {
                String teamName = teamNames.get(teamURLs.indexOf(mTeam));
                teamEditText.setText(teamName);
            }
    
            EditText doneDateText = (EditText) findViewById(R.id.done_date_text);
            if (doneDateText != null)
                doneDateText.setText(mFormattedDoneDate);
            
        }
    
        // Analytics Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Utils.sendScreen(mTracker, getClass().getSimpleName());
    }
    
    public void openDatePicker(View view) {
        String[] baseDatesString = new String[]{
                "Yesterday",
                "Today",
                "Select a date ..."
        };
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(baseDatesString,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        switch (which) {
    
                            case 0: {
                                // Yesterday
                                Calendar c = Calendar.getInstance();
                                c.setTime(new Date());
                                c.add(Calendar.DATE, -1);
                                mDoneDate = idtDateFormat.format(c.getTime());
                                mFormattedDoneDate = userDateFormat.format(c.getTime());
                                
                                EditText dateEditText = (EditText) findViewById(R.id.done_date_text);
                                if (dateEditText != null)
                                    dateEditText.setText(mFormattedDoneDate);
                                
                                break;
                            }
                            
                            case 1: {
                                // Today
                                mDoneDate = idtDateFormat.format(new Date());
                                mFormattedDoneDate = userDateFormat.format(new Date());
                                
                                EditText dateEditText = (EditText) findViewById(R.id.done_date_text);
                                if (dateEditText != null)
                                    dateEditText.setText(mFormattedDoneDate);
                                
                                break;
                            }
                            
                            default: {
                                String[] dates = mDoneDate.split("\\-");
                                
                                mDatePicker = new DatePickerDialog(
                                        NewDoneActivity.this,
                                        new DatePickerDialog.OnDateSetListener() {
                                            @Override
                                            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                                                
                                                Calendar c = Calendar.getInstance();
                                                c.set(year, monthOfYear, dayOfMonth);
                                                mFormattedDoneDate = userDateFormat.format(c.getTime());
                                                
                                                EditText dateEditText = (EditText) findViewById(R.id.done_date_text);
                                                if (dateEditText != null)
                                                    dateEditText.setText(mFormattedDoneDate);
                                                
                                                monthOfYear = monthOfYear + 1;
                                                
                                                mDoneDate = year + "-" +
                                                        (monthOfYear < 10 ? "0" : "") + monthOfYear + "-" +
                                                        (dayOfMonth < 10 ? "0" : "") + dayOfMonth;
                                                
                                                Log.v(LOG_TAG, "Changed date to " + mDoneDate);
                                                
                                                mDatePicker.hide();
                                            }
                                        },
                                        Integer.parseInt(dates[0]),
                                        Integer.parseInt(dates[1]) - 1,
                                        Integer.parseInt(dates[2])
                                );
                                mDatePicker.show();
                            }
                        }
                    }
                })
                .setTitle(R.string.date_label)
                .show();
    }
    
    
    public void openTeamPicker(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
    
        // TODO: 06/05/16 Customize list item layout - add team colour & restrict to single line with ellipises - http://stackoverflow.com/questions/9329156/customize-dialog-list-in-android 
        builder.setItems(teamNames.toArray(new CharSequence[teamNames.size()]),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        
                        mTeam = teamURLs.get(which);
                        
                        EditText teamEditText = (EditText) findViewById(R.id.team_picker);
                        if (teamEditText != null) {
                            teamEditText.setText(teamNames.get(which));
                        }
                    }
                })
                .setTitle(R.string.team_label)
                .show();
    }
    
    
    /**
     * Fetch teams from database and populate into arrays.
     * Arrays are used to populate picker dialog
     */
    private void populateTeamPicker() {
        Cursor teamsCursor = getContentResolver().query(
                DoneListContract.TeamEntry.CONTENT_URI,
                new String[]{
                        DoneListContract.TeamEntry.COLUMN_NAME_ID + " AS _id",
                        DoneListContract.TeamEntry.COLUMN_NAME_NAME,
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
    }
    
    
    private void populateForEdit(Intent sender) {
        // Edit activity title to edit task, instead of 'Done!'
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setTitle("Edit Task");
        
        // Populate activity view with details
        
        // Set text
        String rawText = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, rawText);
        EditText editText = (EditText) findViewById(R.id.done_edit_text);
        if (editText != null) {
            editText.setText(rawText);
            // Set cursor at end of text
            editText.setSelection(editText.getText().length());
        }
    
    
        // Set team
        mTeam = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_TEAM);
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, mTeam);
        EditText teamEditText = (EditText) findViewById(R.id.team_picker);
        if (teamEditText != null) {
            String teamName = teamNames.get(teamURLs.indexOf(mTeam));
            teamEditText.setText(teamName);
        }
        
        // Set date
        String editDate = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
        if (!mDoneDate.equals(editDate)) {
            mDoneDate = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
        
            String[] dateParts = mDoneDate.split("\\-");
            Calendar c = Calendar.getInstance();
            c.set(
                    Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2])
            );
            mFormattedDoneDate = userDateFormat.format(c.getTime());
        }
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
        
        EditText doneDateText = (EditText) findViewById(R.id.done_date_text);
        if (doneDateText != null)
            doneDateText.setText(mFormattedDoneDate);
        
        
        // Set edited fields
        mEditedFields = sender.getStringArrayListExtra(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS);
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
    
            case R.id.action_settings:
                Intent settingsIntent = new Intent(NewDoneActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
    
            case android.R.id.home:
                // User pressed up button, save state if in add mode
                if (mId == -1) {
                    saveActivityState();
                }
            
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            
        }
    }
    
    private void saveActivityState() {
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        if (doneEditText != null) {
            String taskText = doneEditText.getText().toString();
        
            if (!taskText.equals("")) {
            
                String[] state = new String[]{
                        taskText,
                        mTeam,
                        mDoneDate
                };
                Utils.setNewTaskActivityState(this, state);
            
                Log.v(LOG_TAG, "Saved: " + state[0] + ", " + state[1]);
            } else
                Utils.clearNewTaskActivityState(this);
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mId == -1)
            saveActivityState();
    }
    
    private void onSaveClicked() {
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        String taskText = doneEditText.getText().toString();
    
        if (taskText.isEmpty() && doneEditText != null)
    
            doneEditText.setError(getString(R.string.task_edit_text_empty_error));
    
        else {
            // Edit or Add done based on if mId > -1
            
            if (mId > -1) {
                // Case: Edit this done
    
                if (taskText.equals(mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)) &&
                        mTeam.equals(mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM)) &&
                        mDoneDate.equals(mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE))
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
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, mTeam);
                editedDetails.putLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, mId);
                editedDetails.putStringArrayList(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS, (ArrayList<String>) mEditedFields);
                
                boolean result = new DoneActions(this).edit(editedDetails);
                
                if (result) {
                    Intent data = new Intent();
                    data.putExtra(Utils.INTENT_ACTION, Utils.EDITED_TASK_SAVED);
                    setResult(RESULT_OK, data);
                } else {
                    setResult(Utils.RESULT_ERROR);
                }
                
            } else {
                // Case: Add new done
    
                Bundle newDoneDetails = new Bundle();
                
                // Get counter from sharedPreferences for new local done's id
                int localTaskIdCounter = Utils.getLocalDoneIdCounter(this) + 1;
                Utils.setLocalDoneIdCounter(this, localTaskIdCounter);
                
                // Add temporary id to new done bundle
                newDoneDetails.putLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, localTaskIdCounter);
                // Add team to bundle. Later, to be got from selector
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, mTeam);
                // Add done_date to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
                // Add cleaned (of date strings) taskText to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, taskText);
    
                boolean result = new DoneActions(this).create(newDoneDetails);
    
                if (result) {
                    Intent data = new Intent();
                    data.putExtra(Utils.INTENT_ACTION, Utils.NEW_TASK_SAVED);
                    setResult(RESULT_OK, data);
                } else {
                    setResult(Utils.RESULT_ERROR);
                }
            }
    
            Utils.clearNewTaskActivityState(this);
            
            finish();
        }
    }
    
}
