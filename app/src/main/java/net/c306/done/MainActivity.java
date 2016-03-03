package net.c306.done;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
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

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;
import net.c306.done.idonethis.FetchDonesTask;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

// TODO: 20/02/16 Group dones under date headers in listView
// TODO: 28/02/16 Add edit functions 
public class MainActivity
        extends
        AppCompatActivity
        implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener,
        AbsListView.MultiChoiceModeListener,
        AdapterView.OnItemClickListener {
    
    private static final int DONE_LIST_LOADER = 0;
    private static final String SELECTED_KEY = "selected_position";
    private static final int REFRESH_LIST_DELAY = -15; // Except for new dones, fetch only every 15 mins.
    private Snackbar mSnackbar;
    private ListView mListView;
    private String LOG_TAG;
    private DoneListAdapter mDoneListAdapter;
    private int mPosition = ListView.INVALID_POSITION;
    
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    // Src: http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            // Who is sending message?
            String sender = intent.getStringExtra("sender");
            
            switch (sender){
                case "FetchDonesTask":{
                    // Get action - fetch started or finished
                    String action = intent.getStringExtra("action");
                    // Get count of messages fetched - >0 means success
                    int count = intent.getIntExtra("count", -1);
                    
                    SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
    
                    if (action.equals(getString(R.string.fetch_tasks_started)) && !swp.isRefreshing()) {
                        swp.setRefreshing(true);
                    } else if (action.equals(getString(R.string.fetch_tasks_finished))) {
                        if (swp.isRefreshing()) {
                            swp.setRefreshing(false);
                        }
                    } else if (action.equals(getString(R.string.fetch_tasks_cancelled_offline))) {
                        if (swp.isRefreshing()) {
                            swp.setRefreshing(false);
                        }
        
                        Toast t = Toast.makeText(getApplicationContext(), R.string.offline_toast_message, Toast.LENGTH_SHORT);
                        t.show();
                    }
                    
                    break;
                }
                case "PostNewDoneTask":{
                    String action = intent.getStringExtra("action");
                    // Get count of messages sent - >0 means success
                    String message = intent.getStringExtra("message");
    
                    if (action.equals(getString(R.string.postnewdone_started))) {
                        // Do nothing for now
        
                    } else if (action.equals(getString(R.string.postnewdone_finished))) {
        
                        int count = intent.getIntExtra("count", -1);
        
                        if (count > 0) {
                            if (mSnackbar != null && mSnackbar.isShown())
                                mSnackbar.dismiss();
            
                            mSnackbar = Snackbar.make(findViewById(R.id.fab), (count > 1 ? count + " tasks " : "Task ") + getString(R.string.done_sent_toast_message), Snackbar.LENGTH_LONG);
                            mSnackbar.setAction("Action", null).show();
                        }
        
                    } else if (action.equals(getString(R.string.postnewdone_cancelled_offline))) {
        
                        Log.v(LOG_TAG, "Broadcast Receiver - Offline. Will post done when connected");
        
                    } else if (action.equals(getString(R.string.postnewdone_unauth))) {
                        Log.w(LOG_TAG, "Broadcast Receiver - Unauthorised. Check auth token!");
                        // TODO: 27/02/16 Reset token, and show warning bar on top of screen to show offline/unauthorised status 
                    } else {
                        Log.v(LOG_TAG, "Broadcast Receiver - Some other error happened while posting done: " + intent.getIntExtra("count", -1));
                    }
    
                    break;
                }
                case "DeleteDonesTask": {
        
                    String action = intent.getStringExtra("action");
        
                    if (action.equals(getString(R.string.task_started))) {
                        // Do nothing for now
            
                    } else if (action.equals(getString(R.string.task_successful))) {
            
                        Log.v(LOG_TAG, "Broadcast Receiver - " + intent.getIntExtra("count", -1) + " tasks deleted.");
            
                    } else if (action.equals(getString(R.string.offline_toast_message))) {
            
                        Log.v(LOG_TAG, "Broadcast Receiver - Offline. Will post done when connected");
            
                    } else if (action.equals(getString(R.string.postnewdone_unauth))) {
                        Log.w(LOG_TAG, "Broadcast Receiver - Unauthorised. Check auth token!");
            
                        // Show warning snack bar on to show offline/unauthorised status 
                        if (mSnackbar != null && mSnackbar.isShown())
                            mSnackbar.dismiss();
            
                        mSnackbar = Snackbar.make(findViewById(R.id.fab), "Authorisation failed! Please check token.", Snackbar.LENGTH_LONG);
                        mSnackbar.setAction("Settings", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                                startActivity(settingsIntent);
                            }
                        }).show();
                    } else {
                        Log.w(LOG_TAG, "Broadcast Receiver - Some other error happened while posting done: " + intent.getIntExtra("count", -1));
                    }
        
                    break;
                }
                
                default:
                    Log.w(LOG_TAG, "Broadcast receiver got message from unknown sender" + intent.getStringExtra("message"));
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    
        LOG_TAG = getString(R.string.app_log_identifier) + " " + MainActivity.class.getSimpleName();
    
        /*
        * 
        * Precede these with deleting all entries ('delete from dones') from database to do a 
        * fetchAll.
        * 
        * */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String lastUpdated = prefs.getString(getString(R.string.last_updated_setting_name), "");
        Log.v(LOG_TAG, "Last Updated: " + lastUpdated);
        //SharedPreferences.Editor editor = prefs.edit();
        //editor.remove("newDones");
        //editor.apply();
        
        //// DONE: 15/02/16 Update dones from server and update in sqllite
        //// DONE: 20/02/16 Execute fetch only if no fetch within last 15 minutes 
        if(!lastUpdated.equals("")){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.UK);
            try {
                Date lastUpdatedDate = sdf.parse(lastUpdated);
                
                Calendar c = Calendar.getInstance();
                c.setTime(new Date());
                c.add(Calendar.MINUTE, REFRESH_LIST_DELAY);  // create timestamp 15 mins back
                if (lastUpdatedDate.before(c.getTime())) {
                    new FetchDonesTask(this, R.string.main_activity_listener_intent, false).execute();
                }
            } catch(ParseException e){
                Log.w(LOG_TAG, "Couldn't parse lastUpdated: " + lastUpdated + ".");
                new FetchDonesTask(this, R.string.main_activity_listener_intent, false).execute();
            }
        } else {
            Log.w(LOG_TAG, "No lastUpdated found, starting fetch.");
            new FetchDonesTask(this, R.string.main_activity_listener_intent, false).execute();
        }
        
        //mDoneListAdapter = new DoneListAdapter(this, R.layout.list_row_layout, cur, 0);
        mDoneListAdapter = new DoneListAdapter(this, R.layout.list_row_layout, null, 0);
    
        mListView = (ListView) findViewById(R.id.dones_list_view);
    
        mListView.setAdapter(mDoneListAdapter);
        
        // If there's instance state, mine it for useful information.
        // The end-goal here is that the user never knows that turning their device sideways
        // does crazy lifecycle related things.  It should feel like some stuff stretched out,
        // or magically appeared to take advantage of room, but data or place in the app was never
        // actually *lost*.
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }
        
        // Initialize the Loader with id DONE_LIST_LOADER and callbacks 'this'.
        // If the loader doesn't already exist, one is created. Otherwise,
        // the already created Loader is reused. In either case, the
        // LoaderManager will manage the Loader across the Activity/Fragment
        // lifecycle, will receive any new loads once they have completed,
        // and will report this new data back to the 'this' object.
        getLoaderManager().initLoader(DONE_LIST_LOADER, null, this);
    
        SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        swp.setColorSchemeResources(
                R.color.accent,
                android.R.color.holo_blue_dark,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                R.color.primary
        );
    
        //new CheckTokenTask(this).execute();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(getString(R.string.main_activity_listener_intent)));
    
        // Register to save click location (for now), and edit/delete buttons later
        mListView.setOnItemClickListener(this);
        mListView.setMultiChoiceModeListener(this);
        
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
        startActivityForResult(newDoneIntent, 1);
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
/*
        if (mSnackbar != null && mSnackbar.isShown())
            mSnackbar.dismiss();
        
        switch (resultCode) {
            case RESULT_OK:
                mSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.done_saved_toast_message, Snackbar.LENGTH_LONG);
                mSnackbar.setAction("Action", null).show();
                break;
            
            case R.integer.result_offline:
                mSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.done_offline_saved_toast_message, Snackbar.LENGTH_LONG);
                mSnackbar.setAction("Action", null).show();
                break;
            
            default:
                Log.v(LOG_TAG, "Result Code: " + resultCode);
    
        }
*/
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When tablets rotate, the currently selected list item needs to be saved.
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }
    
    /*
    * AdapterView.OnItemClickListener
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
    *  AbsListView.MultiChoiceModeListener methods
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
        // Here you can perform updates to the CAB due to
        // an invalidate() request
        // DONE: 28/02/16 Hide edit button
        menu.findItem(R.id.menu_edit_done).setVisible(mListView.getCheckedItemCount() <= 1);
        return true;
    }
    
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Respond to clicks on the actions in the CAB
        switch (item.getItemId()) {
            case R.id.menu_delete_done:
                deleteSelectedItems();
                mode.finish(); // Action picked, so close the CAB
                return true;
            case R.id.menu_edit_done:
                //editSelectedItems();
                Log.v(LOG_TAG, "Edit selected item");
                mode.finish(); // Action picked, so close the CAB
                return true;
            default:
                return false;
        }
        //return false;
    }
    
    private void deleteSelectedItems() {
        long[] ids = mListView.getCheckedItemIds();
        Log.v(LOG_TAG, "Delete selected items: " + ids.length);
    
        // Delete selected ids from database, then from server, finishing with updating tasks from server on success
        int deletedCount = new DoneActions(this).delete(ids);
    
        if (mSnackbar != null && mSnackbar.isShown())
            mSnackbar.dismiss();
    
        mSnackbar = Snackbar.make(findViewById(R.id.fab), (deletedCount > 1 ? deletedCount + " tasks " : "Task ") + getString(R.string.done_deleted_toast_message), Snackbar.LENGTH_SHORT);
        mSnackbar.setAction("Action", null).show();
        
    }
    
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
    }
    
    /*
    *  LoaderManager.LoaderCallbacks<Cursor> methods
    * */
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.
    
        // To only show current and future dates, filter the query to return weather only for
        // dates after or including today.
        
        // Sort order:  Ascending, by date.
        String sortOrder = DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " DESC, " +
                DoneListContract.DoneEntry.COLUMN_NAME_UPDATED + " DESC";
        
        Uri donesUri = DoneListContract.DoneEntry.buildDoneListUri();
        
        String[] cursorProjectionString = new String[]{
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " AS _id",
                DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT,
                DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME,
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL
        };
        
        return new CursorLoader(this,
                donesUri,
                cursorProjectionString,
                null,
                null,
                sortOrder);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mDoneListAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            // If we don't need to restart the loader, and there's a desired position to restore
            // to, do so now.
            //Log.v(LOG_TAG, "Scroll to " + mPosition);
            mListView.smoothScrollToPosition(mPosition);
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mDoneListAdapter.swapCursor(null);
    }
    
    
    /*
    *          SwipeRefreshLayout.OnRefreshListener methods
    * */
    @Override
    public void onRefresh() {
        Log.v(LOG_TAG, "Calling fetch from swipe to refresh");
        new FetchDonesTask(this, R.string.main_activity_listener_intent, false).execute();
    }
}