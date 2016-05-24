package net.c306.done;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.MultiAutoCompleteTextView;

import com.google.android.gms.analytics.Tracker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewDoneActivity extends AppCompatActivity {
    
    private final String ANALYTICS_TAG = this.getClass().getSimpleName();
    private final String LOG_TAG = Utils.LOG_TAG + ANALYTICS_TAG;
    
    private Bundle mPreEditBundle = new Bundle();
    private List<String> teamNames = new ArrayList<>();
    private List<String> teamURLs = new ArrayList<>();
    
    private Tracker mTracker;
    private SimpleDateFormat userDateFormat = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
    private SimpleDateFormat idtDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    
    private long mId = -1;
    private String mDoneDate = idtDateFormat.format(new Date());
    private String mFormattedDoneDate = userDateFormat.format(new Date());
    private String mTeam;
    private List<String> mEditedFields;
    
    private DatePickerDialog mDatePicker;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_new_done);
    
        // Populate teams from database into team selector
        populateTeamPicker();
    
        // Setup tag suggestions
        setupTagSuggestions();
    
        // Analytics Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        Utils.sendScreen(mTracker, ANALYTICS_TAG);
    
        Intent starterIntent = getIntent();
        mId = starterIntent.getLongExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, -1);
        
        if (mId > -1)
            populateForEdit();

        else {
    
            // Set default team
            mTeam = Utils.getDefaultTeam(NewDoneActivity.this);
    
            String action = starterIntent.getAction();
            String type = starterIntent.getType();
    
            // If started from intent shared by another activity
            if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
                Utils.sendEvent(mTracker, Utils.ANALYTICS_CATEGORY_ACTION, ANALYTICS_TAG + Utils.ANALYTICS_ACTION_CREATE_TASK_FROM_INTENT);
        
                // Handle text being sent
                String taskText = starterIntent.getStringExtra(Intent.EXTRA_TEXT);
                if (taskText != null && !taskText.isEmpty()) {
                    MultiAutoCompleteTextView taskTextEditText = (MultiAutoCompleteTextView) findViewById(R.id.task_text_edit_text);
                    if (taskTextEditText != null) {
                        taskText = taskText.trim();
                        taskTextEditText.setText(taskText);
                        taskTextEditText.setSelection(taskText.length());
                    }
                }
        
            } else {
                // Started with create or edit from our own activity
        
                // Get stored state
                String[] state = Utils.getNewTaskActivityState(this);
        
                // If stored state found, get task text, team and date
                if (state.length > 0) {
                    MultiAutoCompleteTextView taskTextEditText = (MultiAutoCompleteTextView) findViewById(R.id.task_text_edit_text);
                    if (taskTextEditText != null) {
                        taskTextEditText.setText(state[0]);
                        taskTextEditText.setSelection(taskTextEditText.getText().length());
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
                    Log.i(LOG_TAG, "No previous saved state or team intent data found.");
        
                // If intent was called from a team filtered state, set that team, else default/stored-state team
                if (starterIntent.hasExtra(Utils.KEY_NAV_FILTER_TYPE) && starterIntent.getIntExtra(Utils.KEY_NAV_FILTER_TYPE, Utils.NAV_LAYOUT_ALL) == Utils.NAV_LAYOUT_TEAMS)
                    mTeam = starterIntent.getStringExtra(Utils.KEY_NAV_FILTER);
        
            }
    
            // Set team in UI
            EditText teamEditText = (EditText) findViewById(R.id.team_picker);
            if (teamEditText != null) {
                String teamName = teamNames.get(teamURLs.indexOf(mTeam));
                teamEditText.setText(teamName);
            }
    
            // Set date in UI
            EditText doneDateText = (EditText) findViewById(R.id.done_date_text);
            if (doneDateText != null)
                doneDateText.setText(mFormattedDoneDate);
    
        }
    
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
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
    
    private void populateForEdit() {
        
        // Edit activity title to edit task, instead of 'Done!'
        ActionBar ab = getSupportActionBar();
        if (ab != null)
            ab.setTitle("Edit Task");
        
        
        // Fetch task details from database
        Cursor cursor = getContentResolver().query(
                DoneListContract.DoneEntry.CONTENT_URI,                         // URI
                new String[]{                                                   // Projection
                        DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT,
                        DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                        DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS
                },
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " IS ? ",      // Selection
                new String[]{String.valueOf(mId),},      // Selection Args
                null                                                            // Sort Order
        );
        
        if (cursor != null) {
            if (cursor.getCount() < 1) {
                Log.w(LOG_TAG, "No data returned. Some error happened!");
            } else {
                
                cursor.moveToNext();
                
                // Set text
                String rawText = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT));
                mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, rawText);
                MultiAutoCompleteTextView taskTextEditText = (MultiAutoCompleteTextView) findViewById(R.id.task_text_edit_text);
                if (taskTextEditText != null) {
                    taskTextEditText.setText(rawText);
                    // Set cursor at end of text
                    taskTextEditText.setSelection(taskTextEditText.getText().length());
                }
                
                // Set team
                mTeam = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_TEAM));
                mPreEditBundle.putString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, mTeam);
                EditText teamEditText = (EditText) findViewById(R.id.team_picker);
                if (teamEditText != null) {
                    String teamName = teamNames.get(teamURLs.indexOf(mTeam));
                    teamEditText.setText(teamName);
                }
                
                // Set date
                String editDate = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE));
                if (!mDoneDate.equals(editDate)) {
                    mDoneDate = editDate;
                    
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
                String editedFieldsString = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS));
                
                if (editedFieldsString != null && !editedFieldsString.equals(""))
                    mEditedFields = new Gson().fromJson(editedFieldsString, new TypeToken<ArrayList<String>>() {
                    }.getType());
                else
                    mEditedFields = new ArrayList<>();
                
                mPreEditBundle.putStringArrayList(
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS,
                        (ArrayList<String>) mEditedFields);
                
            }
            cursor.close();
        }
    }
    
    /**
     * Fetch tags from database and populate into an ArrayAdapter to suggest
     * while entering task text.
     */
    private void setupTagSuggestions() {
        
        MultiAutoCompleteTextView taskTextEditText = (MultiAutoCompleteTextView) findViewById(R.id.task_text_edit_text);
        if (taskTextEditText == null)
            return;
        
        // Retrieve all tags from database
        Cursor cursor = getContentResolver().query(
                DoneListContract.TagEntry.CONTENT_URI,
                new String[]{
                        "DISTINCT " + DoneListContract.TagEntry.COLUMN_NAME_NAME
                },
                null,
                null,
                null
        );
        
        // Copy tags from cursor to local array
        if (cursor != null) {
            String[] tagNames = new String[cursor.getCount()];
            int nameColIndex = cursor.getColumnIndex(DoneListContract.TagEntry.COLUMN_NAME_NAME);
            int i = 0;
            
            while (cursor.moveToNext()) {
                tagNames[i++] = cursor.getString(nameColIndex);
            }
            
            cursor.close();
    
            if (tagNames.length > 0) {
                
                // Create arrayAdapter and assign it to task edit text
                ArrayAdapter tagsAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, tagNames);
                
                taskTextEditText.setAdapter(tagsAdapter);
                
                // Create custom tokenizer - to suggest tags on '#'
                taskTextEditText.setTokenizer(new MultiAutoCompleteTextView.Tokenizer() {
                    private char suggestStarter = '#';
                    
                    @Override
                    public int findTokenStart(CharSequence text, int cursor) {
                        int i = cursor;
                        
                        while (i > 0 && text.charAt(i - 1) != suggestStarter) {
                            i--;
                        }
                        while (i < cursor && text.charAt(i) == ' ') {
                            i++;
                        }
                        return i;
                    }
                    
                    @Override
                    public int findTokenEnd(CharSequence text, int cursor) {
                        int i = cursor;
                        int len = text.length();
                        
                        while (i < len) {
                            if (text.charAt(i) == ' ') {
                                return i;
                            } else {
                                i++;
                            }
                        }
                        return len;
                    }
                    
                    @Override
                    public CharSequence terminateToken(CharSequence text) {
                        int i = text.length();
                        
                        while (i > 0 && text.charAt(i - 1) == ' ') {
                            i--;
                        }
                        
                        if (i > 0 && text.charAt(i - 1) == ',') {
                            return text;
                        } else {
                            if (text instanceof Spanned) {
                                SpannableString sp = new SpannableString(text + " ");
                                TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                                        Object.class, sp, 0);
                                return sp;
                            } else {
                                return text + " ";
                            }
                        }
                    }
                });
            }
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_done, menu);
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
        MultiAutoCompleteTextView taskTextEditText = (MultiAutoCompleteTextView) findViewById(R.id.task_text_edit_text);
        if (taskTextEditText != null) {
            String taskText = taskTextEditText.getText().toString();
        
            if (!taskText.equals("")) {
            
                String[] state = new String[]{
                        taskText,
                        mTeam,
                        mDoneDate
                };
                Utils.setNewTaskActivityState(this, state);
    
                //Log.i(LOG_TAG, "Saved: " + state[0] + ", " + state[1]);
            } else
                Utils.clearNewTaskActivityState(this);
        }
    }
    
    @Override
    public void onBackPressed() {
        // If new task, save state
        if (mId == -1)
            saveActivityState();
    
        else {
            // If editing, and changes made, prompt about discarding changes
            MultiAutoCompleteTextView taskTextEditText = (MultiAutoCompleteTextView) findViewById(R.id.task_text_edit_text);
            if (taskTextEditText != null) {
            
                String taskText = taskTextEditText.getText().toString().trim();
            
                String preEditTaskText = mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
                if (preEditTaskText != null)
                    preEditTaskText = preEditTaskText.trim();
            
                if (!taskText.equals(preEditTaskText)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                
                    builder.setTitle(getString(R.string.edit_discard_changes_dialog_title))
                            .setPositiveButton(getString(R.string.edit_discard_changes_dialog_discard_button), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    setResult(RESULT_CANCELED);
                                    finish();
                                }
                            })
                            .setNegativeButton(getString(R.string.edit_discard_changes_dialog_cancel_button), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // User clicked Cancel button
                                    // Do nothing :)
                                }
                            })
                            .create()
                            .show();
                    return;
                }
            }
        }
    
        super.onBackPressed();
    }
    
    private void onSaveClicked() {
        MultiAutoCompleteTextView taskTextEditText = (MultiAutoCompleteTextView) findViewById(R.id.task_text_edit_text);
        if (taskTextEditText == null) {
            Log.e(LOG_TAG, "onSaveClicked: Task textview is null!");
            return;
        }
        String taskText = taskTextEditText.getText().toString().trim();
    
        if (taskText.isEmpty())
            taskTextEditText.setError(getString(R.string.task_edit_text_empty_error));
    
        else {
            // Edit or Add done based on if mId > -1
            
            if (mId > -1) {
                // Case: Edit this done
                String preEditTaskText = mPreEditBundle.getString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
                if (preEditTaskText != null)
                    preEditTaskText = preEditTaskText.trim();
    
                if (taskText.equals(preEditTaskText) &&
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
