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
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewDoneActivity extends AppCompatActivity {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    Bundle mPreEditBundle = new Bundle();
    List<String> teamNames = new ArrayList<>();
    List<String> teamURLs = new ArrayList<>();
    private SimpleDateFormat userDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
    private SimpleDateFormat idtDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    private long mId;
    private List<String> mEditedFields;
    private String mDoneDate = idtDateFormat.format(new Date());
    private String mFormattedDoneDate = userDateFormat.format(new Date());
    
    private DatePickerDialog mDatePicker;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_new_done);
    
        Intent sender = getIntent();
        mId = sender.getLongExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, -1);
    
        populateTeamSpinner();
        
        if (mId > -1)
            populateForEdit(sender);
        else {
            // Populate task text from saved data, if available
            String[] state = Utils.getNewTaskActivityState(this);
    
            if (state.length > 0) {
                EditText editText = (EditText) findViewById(R.id.done_edit_text);
                if (editText != null) {
                    editText.setText(state[0]);
                    editText.setSelection(editText.getText().length());
                }
                
                Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
                if (teamPicker != null)
                    teamPicker.setSelection(teamURLs.indexOf(state[1]));
                
            }
    
            EditText doneDateText = (EditText) findViewById(R.id.done_date_text);
            if (doneDateText != null) {
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
    
                doneDateText.setText(mFormattedDoneDate);
            }
        }
    }
    
    public void openDatePicker(View view) {
        String[] baseDatesString = new String[]{
                "Today",
                "Yesterday",
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
                                mDoneDate = idtDateFormat.format(new Date());
                                mFormattedDoneDate = userDateFormat.format(new Date());
                                
                                EditText dateEditText = (EditText) findViewById(R.id.done_date_text);
                                if (dateEditText != null)
                                    dateEditText.setText(mFormattedDoneDate);
                                
                                break;
                            }
                            
                            case 1: {
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
                });
        builder.show();
    }
    
    private void populateTeamSpinner() {
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
        
        Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
        if (teamPicker != null) {
            ArrayAdapter adapter = new ArrayAdapter(this, R.layout.team_selector_spinner, android.R.id.text1, teamNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            teamPicker.setAdapter(adapter);
    
            // Set default team as initial selection
            teamPicker.setSelection(
                    teamURLs.indexOf(Utils.getDefaultTeam(this))
            );
        }
    }
    
    private void populateForEdit(Intent sender) {
        // Edit activity title to edit task, instead of 'Done!'
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Edit Task");
        }
    
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
        String team = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_TEAM);
        mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, team);
        Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
        if (teamPicker != null)
            teamPicker.setSelection(teamURLs.indexOf(team));
    
        // Set date
        String editDate = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
        if (!mDoneDate.equals(editDate)) {
            mDoneDate = sender.getStringExtra(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
            mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
        
            String[] dateParts = mDoneDate.split("\\-");
            Calendar c = Calendar.getInstance();
            c.set(
                    Integer.parseInt(dateParts[0]),
                    Integer.parseInt(dateParts[1]) - 1,
                    Integer.parseInt(dateParts[2])
            );
            mFormattedDoneDate = userDateFormat.format(c.getTime());
        }
    
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
    
            case android.R.id.home:
                // User pressed up button, save state if in add mode
                if (mId == -1) {
                    saveState();
                }
            
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            
        }
    }
    
    private void saveState() {
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        if (doneEditText != null) {
            String taskText = doneEditText.getText().toString();
        
            if (!taskText.equals("")) {
            
                // Get team
                String taskTeam;
                Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
                if (teamPicker != null)
                    taskTeam = teamURLs.get(teamPicker.getSelectedItemPosition());
                else
                    taskTeam = Utils.getDefaultTeam(this);
            
                String[] state = new String[]{
                        taskText,
                        taskTeam,
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
            saveState();
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
                        taskTeam.equals(mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM)) &&
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
                editedDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, taskTeam);
                editedDetails.putLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, mId);
                editedDetails.putStringArrayList(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS, (ArrayList<String>) mEditedFields);
                
                boolean result = new DoneActions(this).edit(editedDetails);
                
                if (result) {
                    setResult(RESULT_OK);
                } else {
                    setResult(Utils.RESULT_ERROR);
                }
                
            } else {
                // Case: Add new done
    
                Bundle newDoneDetails = new Bundle();
                
                // Get counter from sharedPreferences for new local done's id
                int localTaskIdCounter = Utils.getLocalDoneIdCounter(this) + 1;
                Utils.setLocalDoneIdCounter(this, localTaskIdCounter);
                
                Spinner teamPicker = (Spinner) findViewById(R.id.team_picker);
                String taskTeam = teamURLs.get(teamPicker.getSelectedItemPosition());
                
                // Add temporary id to new done bundle
                newDoneDetails.putLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, localTaskIdCounter);
                // Add team to bundle. Later, to be got from selector
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, taskTeam);
                // Add done_date to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, mDoneDate);
                // Add cleaned (of date strings) taskText to bundle
                newDoneDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, taskText);
    
                boolean result = new DoneActions(this).create(newDoneDetails);
    
                if (result) {
                    setResult(RESULT_OK);
                } else {
                    setResult(Utils.RESULT_ERROR);
                }
            }
    
            Utils.clearNewTaskActivityState(this);
            
            finish();
        }
    }
    
}
