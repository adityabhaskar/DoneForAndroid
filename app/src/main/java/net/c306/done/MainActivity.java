package net.c306.done;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.Tracker;
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
        SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener,
        SwipeRefreshLayout.OnRefreshListener,
        AbsListView.OnScrollListener,
        AdapterView.OnItemClickListener,
        AbsListView.MultiChoiceModeListener,
        View.OnClickListener {
    
    private static final String SELECTED_KEY = "selected_position";
    private static final String NAV_SELECTED_KEY = "selected_nav_position";
    private static final String NAV_SELECTED_TEAM = "selected_nav_team";
    
    private static final int DONE_LIST_LOADER = 0;
    private static final int NAV_LIST_LOADER = 1;
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    
    private ActionBarDrawerToggle mToggle;
    private String mCurFilter = null;
    private Snackbar mSnackbar;
    private Tracker mTracker;
    private String mSelectedTeamName;
    
    private String mTeamFilter = null;
    private ListView mListView;
    private DoneListAdapter mDoneListAdapter;
    
    private int mPosition = ListView.INVALID_POSITION;
    private ListView mNavListView;
    private NavListAdapter mNavListAdapter;
    private int mNavPosition = ListView.INVALID_POSITION;
    
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
        
                if (swp != null)
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
                            //settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                            //settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
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
    
                        if (swp != null && !swp.isRefreshing()) {
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
    
            //Log.v(LOG_TAG, "Sender: " + sender + "\nAction: " + action + "\nMessage: " + message);
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_nav_drawer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // If there's instance state, mine it for listView location
        if (savedInstanceState != null) {
            // The listview probably hasn't even been populated yet.  Actually perform the
            // swapout in onLoadFinished.
            if (savedInstanceState.containsKey(SELECTED_KEY))
                mPosition = savedInstanceState.getInt(SELECTED_KEY);
        
            if (savedInstanceState.containsKey(NAV_SELECTED_KEY))
                mNavPosition = savedInstanceState.getInt(NAV_SELECTED_KEY);
        
            if (savedInstanceState.containsKey(NAV_SELECTED_TEAM))
                mTeamFilter = savedInstanceState.getString(NAV_SELECTED_TEAM);
        }
    
        // Setup Nav Bar
        setupNavBar();
    
        // Setup tasks listview
        setupTasksListView();
    
        // Show snackbar if coming from successful login activity 
        Intent fromIntent = getIntent();
    
        // Check if coming from a successful login
        if (fromIntent.hasExtra(Utils.INTENT_FROM_ACTIVITY_IDENTIFIER) &&
                fromIntent.getIntExtra(Utils.INTENT_FROM_ACTIVITY_IDENTIFIER, -1) == Utils.LOGIN_ACTIVITY_IDENTIFIER) {
            mSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.LOGIN_SUCCESSFUL, Snackbar.LENGTH_SHORT);
            mSnackbar.setAction("Dismiss", null).show();
        }
    
        // Setup default preferences, if not already set/changed
        PreferenceManager.setDefaultValues(MainActivity.this, R.xml.preferences, false);
    
        // Analytics Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }
    
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        
        mToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Utils.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
        
        // Register to save click location, and edit/delete buttons
        mListView.setOnItemClickListener(this);
        mListView.setMultiChoiceModeListener(this);
        
        mListView.setOnScrollListener(this);
        
        // Add click listener to nav list
        mNavListView.setOnItemClickListener(this);
        
        // Register to get on swiped events
        SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        if (swp != null) {
            swp.setOnRefreshListener(MainActivity.this);
            swp.setRefreshing(false);
        }
        
        // Log screen open in Analytics
        Utils.sendScreen(mTracker, getClass().getSimpleName());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
    
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }
    
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
    
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            SearchView searchView =
                    (SearchView) MenuItemCompat.getActionView(searchItem);
            if (searchView != null)
                searchView.setOnQueryTextListener(this);
    
            MenuItemCompat.setOnActionExpandListener(searchItem, MainActivity.this);
        }
    
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
    
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
        
            case R.id.action_about: {
                showAbout();
                return true;
            }
            
            case R.id.action_logout: {
                onLogout(item);
                return true;
            }
        
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    
    /**
     * My methods
     */
    
    private void setupNavBar() {
        
        // Add hamburger icon toggle to action bar for the drawer
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null)
            drawer.addDrawerListener(mToggle);
        
        // Add name and email address to nav header
        TextView navHeaderName = (TextView) findViewById(R.id.username_textview);
        if (navHeaderName != null)
            navHeaderName.setText(Utils.getUsername(MainActivity.this));
        
        View allTasksView = findViewById(R.id.nav_all_tasks);
        if (allTasksView != null) {
            
            // Set text for All Tasks TextView
            TextView allTasksTextView = (TextView) allTasksView.findViewById(R.id.team_name_text_view);
            allTasksTextView.setText(R.string.all_tasks);
            
            // Set image resource for All Tasks icon
            ImageView allTasksImageView = (ImageView) allTasksView.findViewById(R.id.nav_team_color_patch);
            allTasksImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.nav_list_alltasks_icon));
            
            // Add click listener
            allTasksView.setOnClickListener(this);
        }
        
        // Set text for Settings TextView
        View settingsView = findViewById(R.id.nav_settings);
        if (settingsView != null) {
            TextView settingsTextView = (TextView) settingsView.findViewById(R.id.team_name_text_view);
            settingsTextView.setText(R.string.action_settings);
            
            ImageView settingsImageView = (ImageView) settingsView.findViewById(R.id.nav_team_color_patch);
            BitmapDrawable settingsIcon = (BitmapDrawable) ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_settings_black_24dp).mutate();
            settingsIcon.setAlpha(0x8A);
            if (settingsImageView != null)
                settingsImageView.setImageDrawable(settingsIcon);
            
            settingsView.setOnClickListener(this);
        }
        
        // Setup Nav TeamListView
        mNavListAdapter = new NavListAdapter(this, R.layout.nav_list_row_layout, null, 0);
        
        mNavListView = (ListView) findViewById(R.id.nav_drawer_listview);
        if (mNavListView != null)
            mNavListView.setAdapter(mNavListAdapter);
        
        getLoaderManager().initLoader(NAV_LIST_LOADER, null, this);
    }
    
    private void setupTasksListView() {
        
        // Add listview adapter
        mDoneListAdapter = new DoneListAdapter(this, R.layout.tasks_list_row_layout, null, 0);
        
        mListView = (ListView) findViewById(R.id.dones_list_view);
        if (mListView != null)
            mListView.setAdapter(mDoneListAdapter);
        
        // Initialize the Loader with id DONE_LIST_LOADER and callbacks 'this'.
        getLoaderManager().initLoader(DONE_LIST_LOADER, null, this);
        
        // Set colours for swipe-to-refresh rotator
        SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        if (swp != null)
            swp.setColorSchemeResources(
                    R.color.accent,
                    R.color.link_colour,
                    R.color.team1,
                    R.color.primary
            );
    }
    
    private void showTeam(String teamURL, boolean isInit) {
        if (teamURL != mTeamFilter || isInit) {
            
            if (teamURL == null) {
                
                // Show all teams
                mTeamFilter = null;
                
                // Clear listview selection
                mNavListView.clearChoices();
                mNavListView.requestLayout();
                
                // Set app bar title
                setTitle(R.string.app_name);
                
            } else {
                
                // Show selected teams
                mTeamFilter = teamURL;
                
                // Highlight item in listview
                if (mNavPosition != ListView.INVALID_POSITION)
                    mNavListView.setItemChecked(mNavPosition, true);
                
                // Set app bar title
                setTitle(mSelectedTeamName);
                
            }
            
            // Mark all tasks selected/unselected
            View alltasksItem = findViewById(R.id.nav_all_tasks);
            if (alltasksItem != null)
                alltasksItem.setSelected(teamURL == null);
            
            
            // Restart loader if not equals "init"
            if (!isInit)
                getLoaderManager().restartLoader(0, null, this);
        }
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
                        
                        // Track event in analytics
                        Utils.sendEvent(mTracker, "Action", "Logout");
                        
                        // Remove alarm & any notifications
                        Utils.cancelNotificationAlarm(MainActivity.this);
                        
                        // Stop periodic sync
                        IDTSyncAdapter.stopPeriodicSync(getApplicationContext());
                        
                        // Remove Account from system account manager
                        IDTAccountManager.removeSyncAccount(context);
                        Log.v(LOG_TAG, "Account removed...");
                        
                        // Empty database
                        getContentResolver().delete(DoneListContract.DoneEntry.CONTENT_URI, null, null);
                        getContentResolver().delete(DoneListContract.TeamEntry.CONTENT_URI, null, null);
                        mDoneListAdapter.notifyDataSetChanged();
                        //mListView.setAdapter(null);
                        Log.v(LOG_TAG, "Database deleted.");
                        
                        // Empty sharedPreferences & set to default
                        Utils.resetSharedPreferences(context);
                        Log.v(LOG_TAG, "SharedPreferences emptied.");
                        
                        // Replace Logout with Login in action bar
                        invalidateOptionsMenu();
                        
                        Intent splashScreenActivity = new Intent(MainActivity.this, SplashScreenActivity.class);
                        startActivity(splashScreenActivity);
                        finish();
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
    
    private void showAbout() {
        Log.v(LOG_TAG, "Opening about dialog");
        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
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
                startActivityForResult(editDoneIntent, Utils.NEW_DONE_ACTIVITY_IDENTIFIER);
            }
            cursor.close();
        }
    }
    
    private void deleteSelectedItems(long[] ids) {
        Log.v(LOG_TAG, "Delete selected items: " + ids.length);
    
        // Log event in analytics
        Utils.sendEvent(mTracker, "Action", "DeleteDones", String.valueOf(ids.length));
        
        // Delete selected ids from database, then from server, finishing with updating tasks from server on success
        int deletedCount = new DoneActions(getApplicationContext()).delete(ids);
        
        if (mSnackbar != null && mSnackbar.isShown())
            mSnackbar.dismiss();
        
        mSnackbar = Snackbar.make(findViewById(R.id.fab), (deletedCount > 1 ? deletedCount + " tasks " : "Task ") + getString(R.string.done_deleted_toast_message), Snackbar.LENGTH_SHORT);
        mSnackbar.setAction("Action", null).show();
        
    }
    
    /**
     *
     *  LoaderManager.LoaderCallbacks<Cursor> methods - to setup cursor and adapter 
     *  for populating listview
     *
     * */
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // fragment only uses one loader, so we don't care about checking the id.
    
        switch (id) {
        
            case DONE_LIST_LOADER: {
            
                // Sort order:  Descending, by date.
                String sortOrder = DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " DESC, " +
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " DESC, " +
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " DESC, " +
                        DoneListContract.DoneEntry.COLUMN_NAME_UPDATED + " DESC, " +
                        "_id DESC";
                //DoneListContract.DoneEntry.COLUMN_NAME_ID + " DESC";
                //" CASE WHEN " + DoneListContract.DoneEntry.COLUMN_NAME_ID + " > " + Utils.LOCAL_DONE_ID_COUNTER + " THEN 1 ELSE 0 END DESC, "
            
                Uri donesUri = DoneListContract.DoneEntry.buildDoneListUri();
            
                String[] cursorProjectionString = new String[]{
                        DoneListContract.DoneEntry.COLUMN_NAME_ID + " AS _id",
                        DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT,
                        DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        //DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME,
                        DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL,
                        DoneListContract.DoneEntry.COLUMN_NAME_UPDATED,
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS
                };
            
                String selection = DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'FALSE'";
            
                if (mCurFilter != null) {
                    String[] filterArr = mCurFilter.split(" +");
                
                    for (String filterString : filterArr) {
                        selection += " AND " +
                                DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT +
                                " LIKE '%" +
                                filterString +
                                "%'";
                    }
                }
            
                if (mTeamFilter != null) {
                    selection += " AND " +
                            DoneListContract.DoneEntry.COLUMN_NAME_TEAM +
                            " IS '" +
                            mTeamFilter +
                            "'";
                }
            
                return new CursorLoader(this,
                        donesUri,
                        cursorProjectionString,
                        selection,
                        null,
                        sortOrder);
            }
        
            case NAV_LIST_LOADER: {
            
                Uri teamUri = DoneListContract.TeamEntry.CONTENT_URI;
            
                String[] cursorProjectionString = new String[]{
                        DoneListContract.TeamEntry.COLUMN_NAME_ID + " AS _id",
                        DoneListContract.TeamEntry.COLUMN_NAME_NAME,
                        DoneListContract.TeamEntry.COLUMN_NAME_URL,
                        DoneListContract.TeamEntry.COLUMN_NAME_DONE_COUNT,
                        DoneListContract.TeamEntry.COLUMN_NAME_IS_PERSONAL
                };
            
                return new CursorLoader(this,
                        teamUri,
                        cursorProjectionString,
                        null,
                        null,
                        null);
            }
        
            default:
                Log.e(LOG_TAG, "No such loader. id: " + id);
                return null;
        }
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
        
            case DONE_LIST_LOADER: {
                mDoneListAdapter.swapCursor(data);
            
                if (mPosition != ListView.INVALID_POSITION)
                    mListView.smoothScrollToPosition(mPosition);
            
                break;
            }
        
            case NAV_LIST_LOADER: {
                mNavListAdapter.swapCursor(data);
            
                if (mNavPosition != ListView.INVALID_POSITION)
                    mNavListView.smoothScrollToPosition(mNavPosition);
            
                showTeam(mTeamFilter, true);
            
                break;
            }
        
            default:
                Log.e(LOG_TAG, "No such loader.");
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        
            case DONE_LIST_LOADER: {
                mDoneListAdapter.swapCursor(null);
                break;
            }
        
            case NAV_LIST_LOADER: {
                mNavListAdapter.swapCursor(null);
                break;
            }
        
            default:
                Log.e(LOG_TAG, "No such loader.");
        }
    }
    
    /**
     * Methods implementing search & tracking for AppBar SearchView 
     */
    
    @Override
    public boolean onQueryTextSubmit(String query) {
        // do nothing
        return false;
    }
    
    @Override
    public boolean onQueryTextChange(String newText) {
        // search in database and change loader cursor accordingly
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }
    
    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            Log.i(LOG_TAG, "Search expanded");
            Utils.sendEvent(mTracker, "Action", "Search");
        }
        return true;
    }
    
    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        return true;
    }
    
    /**
     * SwipeRefreshLayout.OnRefreshListener method - to start a sync when swiped-to-refresh
     */
    @Override
    public void onRefresh() {
        Log.v(LOG_TAG, "Calling syncImmediately from swipe to refresh");
        
        // Track event in analytics
        Utils.sendEvent(mTracker, "Action", "PullToRefresh");
        
        IDTSyncAdapter.syncImmediately(this);
    }
    
    /**
     * Save & restore scroll state
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When no item is selected, mPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mPosition != ListView.INVALID_POSITION)
            outState.putInt(SELECTED_KEY, mPosition);
    
        if (mNavPosition != ListView.INVALID_POSITION)
            outState.putInt(NAV_SELECTED_KEY, mNavPosition);
    
        if (mTeamFilter != null)
            outState.putString(NAV_SELECTED_TEAM, mTeamFilter);
        else
            outState.remove(NAV_SELECTED_TEAM);
        
        super.onSaveInstanceState(outState);
    }
    
    /**
     * Implementing methods for onScrollListener - to save scroll state for activity restarts
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // Do Nothing
    }
    
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // Save position
        mPosition = firstVisibleItem;
    }
    
    /**
     * AbsListView.MultiChoiceModeListener methods - for edit & delete selections
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    
        switch (adapterView.getId()) {
            case R.id.dones_list_view: {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                //Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
            
                mPosition = position;
            
                mListView.setItemChecked(position, !mListView.isItemChecked(position));
                break;
            }
        
            case R.id.nav_drawer_listview: {
                // Set selected option position for next drawer open
                mNavPosition = position;
            
                // Set item as selected
                //mNavListView.setItemChecked(position, true);
            
                TextView teamNameTextView = (TextView) view.findViewById(R.id.team_name_text_view);
                if (teamNameTextView != null)
                    mSelectedTeamName = (String) teamNameTextView.getText();
            
                showTeam(Utils.getTeams(MainActivity.this)[position], false);
            
                // Close drawer
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer != null)
                    drawer.closeDrawer(GravityCompat.START);
            
                break;
            }
        }
    }
    
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
                
                // Close the CAB
                mode.finish();
                return true;
            
            case R.id.menu_edit_done:
                Log.v(LOG_TAG, "Edit selected item");
                
                // Log event in analytics
                Utils.sendEvent(mTracker, "Action", "EditDone");
                
                // Action picked, so close the CAB
                mode.finish();
                
                editSelectedItems(selectedTasks[0]);
                return true;
            
            default:
                return false;
        }
    }
    
    @Override
    public void onDestroyActionMode(ActionMode mode) {
        // Here you can make any necessary updates to the activity when
        // the CAB is removed. By default, selected items are deselected/unchecked.
    }
    
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.nav_all_tasks: {
                showTeam(null, false);
                
                // Close drawer
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer != null)
                    drawer.closeDrawer(GravityCompat.START);
                
                break;
            }
            
            case R.id.nav_settings: {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            }
            
            default:
                Log.w(LOG_TAG, "Unhandled view click: " + v);
        }
    }
}
