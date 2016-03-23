package net.c306.done;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;
import net.c306.done.sync.IDTAccountManager;
import net.c306.done.sync.IDTSyncAdapter;

import java.util.ArrayList;
import java.util.List;

// TODO: 20/02/16 Group dones under date headers in listView
public class MainActivity
        extends
        AppCompatActivity
        implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        AbsListView.MultiChoiceModeListener,
        AdapterView.OnItemClickListener,
        AbsListView.OnScrollListener {
    
    private static final int DONE_LIST_LOADER = 0;
    private static final String SELECTED_KEY = "selected_position";
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private Snackbar mSnackbar;
    private ListView mListView;
    private DoneListAdapter mDoneListAdapter;
    private int mPosition = ListView.INVALID_POSITION;
    
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    // Src: http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
    
            int action = intent.getIntExtra("action", -1);
            String sender = intent.getStringExtra("sender");
            String message = intent.getStringExtra("message");
    
            if (
                    (
                            // If fetch finished, stop refresher
                            sender.equals(Utils.SENDER_FETCH_TASKS) &&
                                    action != Utils.STATUS_TASK_STARTED
                    ) ||
                            (
                                    // If some other task exited with non-success status, stop refresher
                                    action != Utils.STATUS_TASK_STARTED &&
                                            action != Utils.STATUS_TASK_SUCCESSFUL &&
                                            action != Utils.CHECK_TOKEN_SUCCESSFUL &&
                                            action != Utils.CHECK_TOKEN_STARTED
                            )
                    ) {
                
                SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        
                //if (swp.isRefreshing())
                    swp.setRefreshing(false);
        
            }
    
            // If successfully logged in, replace Login item by Logout
            if (action == Utils.CHECK_TOKEN_SUCCESSFUL) {
                invalidateOptionsMenu();
            }
            
            switch (action) {
                case Utils.STATUS_TASK_UNAUTH:
                case Utils.CHECK_TOKEN_FAILED:
                    // Unauthorised, i.e. token issues, show snackbar
        
                    Log.w(LOG_TAG, "Broadcast Receiver - Unauthorised! " + message);
        
                    // Reset token status
                    Utils.setTokenValidity(getApplicationContext(), false);
        
                    // Show warning snack bar on to show offline/unauthorised status 
                    if (mSnackbar != null && mSnackbar.isShown())
                        mSnackbar.dismiss();
        
                    mSnackbar = Snackbar.make(findViewById(R.id.fab), "Authorisation failed! Please check token.", Snackbar.LENGTH_LONG);
        
                    mSnackbar.setAction("Settings", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                            settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                            settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                            startActivity(settingsIntent);
                        }
                    })
                            .setActionTextColor(ContextCompat.getColor(getApplicationContext(), R.color.link_colour))
                            .show();
                    break;
    
                case Utils.STATUS_TASK_SUCCESSFUL:
                    if (sender.equals(Utils.SENDER_CREATE_TASK)) {
                        // PostDone successful, show snackbar
                        int count = intent.getIntExtra("count", -1);
            
                        if (count > 0) {
                            if (mSnackbar != null && mSnackbar.isShown())
                                mSnackbar.dismiss();
                
                            mSnackbar = Snackbar.make(findViewById(R.id.fab), (count > 1 ? count + " tasks " : "Task ") + getString(R.string.done_sent_toast_message), Snackbar.LENGTH_LONG);
                            mSnackbar.setAction("Action", null).show();
                        }
                    }
                    break;
    
                case Utils.STATUS_TASK_STARTED:
                    if (sender.equals(Utils.SENDER_FETCH_TASKS)) {
                        
                        SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
            
                        if (!swp.isRefreshing()) {
                            // If fetch started, and refresher not showing, start it
                            swp.setRefreshing(true);
                        }
                    }
                    break;
    
                case Utils.STATUS_TASK_CANCELLED_OFFLINE:
                    if (sender.equals(Utils.SENDER_FETCH_TASKS)) {
                        Toast.makeText(getApplicationContext(), R.string.offline_toast_message, Toast.LENGTH_SHORT)
                                .show();
                    }
            }
    
            Log.v(LOG_TAG, "Sender: " + sender + "\nAction: " + action + "\nMessage: " + message);
        }
    };
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Setup List View
        mDoneListAdapter = new DoneListAdapter(this, R.layout.list_row_layout, null, 0);
        
        mListView = (ListView) findViewById(R.id.dones_list_view);
        
        mListView.setAdapter(mDoneListAdapter);
        
        // If there's instance state, mine it for useful information.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        
        // Initialize the Loader with id DONE_LIST_LOADER and callbacks 'this'.
        getLoaderManager().initLoader(DONE_LIST_LOADER, null, this);
        
        
        // Set colours for refresher
        SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swp.setColorSchemeResources(
                R.color.accent,
                R.color.link_colour,
                R.color.team1,
                R.color.primary
        );
        
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
    
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Utils.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
    
        // Register to save click location (for now), and edit/delete buttons later
        mListView.setOnItemClickListener(this);
        mListView.setMultiChoiceModeListener(this);
        
        mListView.setOnScrollListener(this);
        
        // Register to get on swiped events
        SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swp.setOnRefreshListener(this);
    }
    
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }
    
    
    public void toNewDone(View view) {
        Intent newDoneIntent = new Intent(MainActivity.this, NewDoneActivity.class);
        startActivityForResult(newDoneIntent, RESULT_CANCELED);
    }
    
    
    public void onLogout(MenuItem item) {
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        builder.setTitle("Are you sure?")
                .setMessage("All local data will be deleted.")
                .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Context context = getApplicationContext();
                        // User confirmed Logout
                        Log.v(LOG_TAG, "Logging out...");
    
                        // remove Account from system account manager
                        IDTAccountManager.removeSyncAccount(context);
                        Log.v(LOG_TAG, "Account removed...");
                        
                        // empty database
                        getContentResolver().delete(DoneListContract.DoneEntry.CONTENT_URI, null, null);
                        Log.v(LOG_TAG, "Database deleted.");
                        
                        // empty sharedPreferences
                        Utils.resetSharedPreferences(context);
                        Log.v(LOG_TAG, "SharedPreferences emptied.");
    
                        // Replace Logout with Login in action bar
                        invalidateOptionsMenu();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Cancel button
                        // Do nothing :)
                    }
                })
                .create()
                .show();
    }
    
    
    /*
    * 
    * Inflate action bar and Act on clicks from action bar / menu
    * 
    * */
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        
        Log.v(LOG_TAG, String.valueOf(Utils.haveValidToken(this)));
        
        // Check if logged in, and show Login/Logout buttons accordingly
        if (Utils.haveValidToken(this))
            menu.removeItem(R.id.action_login);
        else
            menu.removeItem(R.id.action_logout);
        
        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        
        switch (id) {
            case R.id.action_settings:
            case R.id.action_login: {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                
                if (id == R.id.action_login)
                    settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, Utils.ACTION_SHOW_AUTH);
                
                startActivity(settingsIntent);
                return true;
            }
            
            case R.id.action_logout: {
                onLogout(item);
            }
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    
    /* Save scroll state */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION)
            outState.putInt(SELECTED_KEY, mPosition);
        
        super.onSaveInstanceState(outState);
    }
    
    
    /*
    * 
    * Implementing methods for onScrollListener - to save scroll state for activity restarts
    * 
    * */
    
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Do Nothing
    }
    
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Save position
        mPosition = firstVisibleItem;
    }
    
    
    /*
    * 
    * AdapterView.OnItemClickListener - to save scroll state for activity restarts
    * 
    * */
    
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        // CursorAdapter returns a cursor at the correct position for getItem(), or null
        // if it cannot seek to that position.
        //Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        mPosition = position;
        
        mListView.setItemChecked(position, !mListView.isItemChecked(position));
    }
    
    
    /*
    * 
    * AbsListView.MultiChoiceModeListener methods - for edit & delete selections
    * 
    * */
    
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        // Here you can do something when items are selected/de-selected,
        // such as update the title in the CAB
        int checkedCount = mListView.getCheckedItemCount();
        mode.setTitle(checkedCount + " tasks selected");
        
        if (checkedCount == 2 && checked) {
            // add edit
            mode.invalidate();
        } else if (checkedCount == 1 && !checked) {
            // remove edit
            mode.invalidate();
        }
    }
    
    
    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        // Inflate the menu for the CAB
        getMenuInflater().inflate(R.menu.listview_context, menu);
        return true;
    }
    
    
    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        // Hide or show edit button, based on whether more than one items is selected
        menu.findItem(R.id.menu_edit_done).setVisible(mListView.getCheckedItemCount() <= 1);
        return true;
    }
    
    
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Respond to clicks on the actions in the CAB
        final long[] selectedTasks = mListView.getCheckedItemIds();
        
        switch (item.getItemId()) {
    
            case R.id.menu_delete_done:
                // Show alert message to confirm delete
                new AlertDialog.Builder(this)
                        .setMessage("Are you sure you want to delete these tasks?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteSelectedItems(selectedTasks);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
        
                mode.finish(); // Close the CAB
                return true;
    
            case R.id.menu_edit_done:
                editSelectedItems(selectedTasks[0]);
                Log.v(LOG_TAG, "Edit selected item");
                mode.finish(); // Action picked, so close the CAB
                return true;
    
            default:
                return false;
        }
    }
    
    
    /**
     * If owner is same as current user, fetch details from database and send to NewDoneActivity
     *
     * @param id row id of selected task in listView
     */
    private void editSelectedItems(long id) {
        
        // Fetch done details from database
        Cursor cursor = getContentResolver().query(
                DoneListContract.DoneEntry.CONTENT_URI,                         // URI
                new String[]{                                                   // Projection
                        DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT,
                        DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                        DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS
                },
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " IS ? AND " +      // Selection
                        DoneListContract.DoneEntry.COLUMN_NAME_OWNER + " IS ?",
                new String[]{String.valueOf(id), Utils.getUsername(this)},      // Selection Args
                null                                                            // Sort Order
        );
        
        if (cursor != null) {
            if (cursor.getCount() == 0) {
                // No such task, or user not owner, so not allowed.
                Toast.makeText(
                        this,
                        "Not allowed - only task creater can edit tasks.",
                        Toast.LENGTH_LONG)
                        .show();
            } else {
                
                Intent editDoneIntent = new Intent(MainActivity.this, NewDoneActivity.class);
                cursor.moveToNext();
                
                // Add details to bundle
                editDoneIntent.putExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, id);
                editDoneIntent.putExtra(
                        DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT))
                );
                editDoneIntent.putExtra(
                        DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_TEAM))
                );
                editDoneIntent.putExtra(
                        DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE))
                );
                
                String editedFieldsString = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS));
                List<String> editedFields;
                
                if (editedFieldsString != null && !editedFieldsString.equals("")) {
                    editedFields = new Gson().fromJson(editedFieldsString, new TypeToken<ArrayList<String>>() {
                    }.getType());
                } else {
                    editedFields = new ArrayList<>();
                }
                
                editDoneIntent.putStringArrayListExtra(
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS,
                        (ArrayList<String>) editedFields
                );
                
                // Start edit activity
                startActivityForResult(editDoneIntent, RESULT_CANCELED);
            }
            cursor.close();
        }
    }
    
    
    private void deleteSelectedItems(long[] ids) {
        Log.v(LOG_TAG, "Delete selected items: " + ids.length);
        
        // Delete selected ids from database, then from server, finishing with updating tasks from server on success
        int deletedCount = new DoneActions(this).delete(ids);
        
        if (mSnackbar != null && mSnackbar.isShown())
            mSnackbar.dismiss();
        
        mSnackbar = Snackbar.make(findViewById(R.id.fab), (deletedCount > 1 ? deletedCount + " tasks " : "Task ") + getString(R.string.done_deleted_toast_message), Snackbar.LENGTH_SHORT);
        mSnackbar.setAction("Action", null).show();
        
    }
    
    
    public void onDestroyActionMode(ActionMode mode) {
        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
    }
    
    
    /*
    *  
    *  LoaderManager.LoaderCallbacks<Cursor> methods - to setup cursor and adapter 
    *  for populating listview
    *  
    * */
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.
    
        // Sort order:  Descending, by date.
        String sortOrder = DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " DESC, " +
                DoneListContract.DoneEntry.COLUMN_NAME_UPDATED + " DESC";
        
        Uri donesUri = DoneListContract.DoneEntry.buildDoneListUri();
        
        String[] cursorProjectionString = new String[]{
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " AS _id",
                DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT,
                DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                //DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME,
                DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL,
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS
        };
        
        return new CursorLoader(this,
                donesUri,
                cursorProjectionString,
                DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'FALSE'",
                null,
                sortOrder);
    }
    
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mDoneListAdapter.swapCursor(data);
        
        if (mPosition != ListView.INVALID_POSITION)
            mListView.smoothScrollToPosition(mPosition);
    }
    
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mDoneListAdapter.swapCursor(null);
    }
    
    
    /*
    * SwipeRefreshLayout.OnRefreshListener method - to start a sync when swiped-to-refresh
    * */
    @Override
    public void onRefresh() {
        Log.v(LOG_TAG, "Calling syncImmediately from swipe to refresh");
    
        IDTSyncAdapter.syncImmediately(this, true);
    }
    
}
