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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import net.c306.done.db.DoneListContract;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

//// TODO: 20/02/16 Group dones under date headers in listView
//// TODO: 22/02/16 Add date of done in small font under the done 
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, SwipeRefreshLayout.OnRefreshListener{
    
    private static final int DONE_LIST_LOADER = 0;
    private static final String SELECTED_KEY = "selected_position";
    private static final int REFRESH_LIST_DELAY = -15; // Except for new dones, fetch only every 15 mins.
    private Snackbar newDoneSnackbar;
    private ListView listView;
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
    
                    if (action == getString(R.string.fetch_started) && !swp.isRefreshing()) {
                        //Log.v(LOG_TAG, "Refresh started, showing SWP");
                        swp.setRefreshing(true);
                    } else if (action == getString(R.string.fetch_finished)) {
                        if (swp.isRefreshing())
                            swp.setRefreshing(false);
                        //Log.v(LOG_TAG, "Broadcast Receiver - Fetched " + count + " messages");
                    } else if (action == getString(R.string.fetch_cancelled_offline)) {
                        //Log.w(LOG_TAG, "Fetch cancelled because offline");
                        if (swp.isRefreshing()) {
                            //Log.v(LOG_TAG, "SWP is refreshing. Cancelling...");
                            swp.setRefreshing(false);
                        }
        
                        Toast t = Toast.makeText(getApplicationContext(), "Offline! Please check your connection.", Toast.LENGTH_SHORT);
                        t.show();
                    }
                    
                    break;
                }
                case "PostNewDoneTask":{
                    // Get count of messages sent - >0 means success
                    int count = intent.getIntExtra("count", -1);
                    String message = intent.getStringExtra("message");
                    
                    if (count > 0) {
                        if (newDoneSnackbar != null && newDoneSnackbar.isShown())
                            newDoneSnackbar.dismiss();
                        
                        newDoneSnackbar = Snackbar.make(findViewById(R.id.fab), (count > 1 ? count + " dones " : "Done ") + getString(R.string.done_sent_toast_message), Snackbar.LENGTH_LONG);
                        newDoneSnackbar.setAction("Action", null).show();
                        
                        Log.v(LOG_TAG, "Broadcast Receiver - Got message: " + count + " " + message);
                    } else {
                        Log.v(LOG_TAG, "Broadcast Receiver - Got error: " + count);
                    }
                    break;
                }
                default:
                    Log.w(LOG_TAG, "Broadcast receiver got message from unknown sender");
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
        
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(getString(R.string.done_posted_intent)));
        
        /*
        * 
        * Precede these with deleting all entries ('delete from dones') from database to do a 
        * fetchAll.
        * 
        * */
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String lastUpdated = prefs.getString(getString(R.string.last_updated_setting_name), "");
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
                    new FetchDonesTask(this).execute();
                }
            } catch(ParseException e){
                Log.w(LOG_TAG, "Couldn't parse lastUpdated: " + lastUpdated + ".");
                new FetchDonesTask(this).execute();
            }
        } else {
            Log.w(LOG_TAG, "No lastUpdated found, starting fetch.");
            new FetchDonesTask(this).execute();
        }
        
        //mDoneListAdapter = new DoneListAdapter(this, R.layout.list_row_layout, cur, 0);
        mDoneListAdapter = new DoneListAdapter(this, R.layout.list_row_layout, null, 0);
        
        listView = (ListView) findViewById(R.id.dones_list_view);
        
        listView.setAdapter(mDoneListAdapter);
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                //Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                Log.v(LOG_TAG, "Item Clicked: " + position + ", l: " + l);
                mPosition = position;
            }
        });
        
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
        swp.setOnRefreshListener(this);
    }
    
    @Override
    protected void onStart() {
        super.onStart();
    }
    
    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
    
    public void toNewDone(View view) {
        Intent newDoneIntent = new Intent(MainActivity.this, NewDone.class);
        startActivityForResult(newDoneIntent, 1);
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (newDoneSnackbar != null && newDoneSnackbar.isShown())
            newDoneSnackbar.dismiss();
        
        switch (resultCode) {
            case RESULT_OK:
                newDoneSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.done_saved_toast_message, Snackbar.LENGTH_LONG);
                newDoneSnackbar.setAction("Action", null).show();
                break;
            
            case R.integer.result_offline:
                newDoneSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.done_offline_saved_toast_message, Snackbar.LENGTH_LONG);
                newDoneSnackbar.setAction("Action", null).show();
                break;
            
            default:
                Log.v(LOG_TAG, "Result Code: " + resultCode);
    
        }
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
                DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME
        };
        
        Log.wtf(LOG_TAG, "From Loader: " + cursorProjectionString[0]);
        
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
            Log.v(LOG_TAG, "Scroll to " + mPosition);
            listView.smoothScrollToPosition(mPosition);
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mDoneListAdapter.swapCursor(null);
    }
    
    
    @Override
    public void onRefresh() {
        Log.v(LOG_TAG, "Calling fetch from swipe to refresh");
        new FetchDonesTask(this).execute();
    }
}