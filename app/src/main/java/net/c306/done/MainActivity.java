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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.ArrayMap;
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
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.Tracker;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;
import net.c306.done.sync.IDTAccountManager;
import net.c306.done.sync.IDTSyncAdapter;

import java.util.Map;

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
    
    // Saved State Bundle keys
    private static final String TASK_SELECTED_KEY = "selected_position";    // Saved state id for mTaskListPosition
    private static final String NAV_SELECTED_ID_KEY = "nav_selected_tag_position";  // Saved state id for mNavSelectedId
    // Loader ids
    private static final int TASK_LIST_LOADER = 0;
    private static final int TEAM_LIST_LOADER = 1;
    private static final int TAG_LIST_LOADER = 2;
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private ActionBarDrawerToggle mNavDrawerToggle;      // Toggle for nav drawer
    private Snackbar mSnackbar;                 // Snackbar to show messages
    private Tracker mTracker;                   // Google Analytics tracker
    
    
    private String mFilterTitle = null;          // Title to display - team name, #tag or search phrase
    
    // Search filter strings
    private String mSearchFilter = null;
    private String mRestoredSearchFilter = null;
    
    // Nav item identifiers
    private int mNavFilterType = Utils.NAV_LAYOUT_ALL;  // All, Team, or Tag
    private String mNavFilterString = null;             // SQL query string
    private int mNavSelectedId = -1;                    // int id of item to locate in nav linear layout
    
    
    private ListView mTasksListView;                    // List of tasks in main view
    private TaskListAdapter mTaskListAdapter;           // Adapter for list of tasks
    private int mTaskListPosition = ListView.INVALID_POSITION;  // Position of selected item in task list
    
    private Map<Integer, Integer> idToLayoutMap = new ArrayMap<>(); // Mapping type of item (team / tag) to id of respective linear layout in view 
    private NavTagsClickListener navTagsClickListener = new NavTagsClickListener();
    private NavTeamsClickListener navTeamsClickListener = new NavTeamsClickListener();
    
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
                case Utils.CHECK_TOKEN_FAILED: {
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
                }
    
                case Utils.STATUS_TASK_SUCCESSFUL: {
                    if (sender.equals(Utils.SENDER_CREATE_TASK)) {
                        // PostDone successful, show snackbar
                        int count = intent.getIntExtra(Utils.INTENT_COUNT, -1);
    
                        if (count > 0) {
                            if (mSnackbar != null && mSnackbar.isShown())
                                mSnackbar.dismiss();
    
                            mSnackbar = Snackbar.make(findViewById(R.id.fab), (count > 1 ? count + " " + getString(R.string.tasks_string) : getString(R.string.task_string)) + " " + getString(R.string.task_sent_toast_message), Snackbar.LENGTH_LONG);
                            mSnackbar.setAction("Action", null).show();
                        }
                    }
                    break;
                }
    
                case Utils.STATUS_TASK_STARTED: {
                    if (sender.equals(Utils.SENDER_FETCH_TASKS)) {
    
                        SwipeRefreshLayout swp = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
    
                        if (swp != null && !swp.isRefreshing()) {
                            // If fetch started, and refresher not showing, start it
                            swp.setRefreshing(true);
                        }
                    }
                    break;
                }
    
                case Utils.STATUS_TASK_CANCELLED_OFFLINE: {
                    if (sender.equals(Utils.SENDER_FETCH_TASKS)) {
                        Toast.makeText(getApplicationContext(), R.string.offline_toast_message, Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;
                }
    
                case Utils.EDITED_TASK_SAVED:
                case Utils.NEW_TASK_SAVED: {
                    if (mSnackbar != null && mSnackbar.isShown())
                        mSnackbar.dismiss();
        
                    mSnackbar = Snackbar.make(findViewById(R.id.fab),
                            action == Utils.NEW_TASK_SAVED ? R.string.new_task_saved_toast_message : R.string.edited_task_saved_toast_message,
                            Snackbar.LENGTH_SHORT);
                    mSnackbar.setAction("Action", null).show();
                    break;
                }
                
                case Utils.TASK_DELETED_SNACKBAR: {
                    if (mSnackbar != null && mSnackbar.isShown())
                        mSnackbar.dismiss();
    
                    int deletedCount = intent.getIntExtra(Utils.INTENT_COUNT, -1);
    
                    if (deletedCount > 0) {
                        mSnackbar = Snackbar.make(findViewById(R.id.fab), (deletedCount > 1 ? deletedCount + " " + getString(R.string.tasks_string) : getString(R.string.task_string)) + " " + getString(R.string.task_deleted_toast_message), Snackbar.LENGTH_SHORT);
                        mSnackbar.setAction("Action", null).show();
                    }
                    break;
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
    
        idToLayoutMap.put(Utils.NAV_LAYOUT_TEAMS, R.id.nav_drawer_teams);
        idToLayoutMap.put(Utils.NAV_LAYOUT_TAGS, R.id.nav_drawer_tags);
    
        // Restore variables from saved state, if any
        if (savedInstanceState != null) {
    
            // Get tasks listview location
            if (savedInstanceState.containsKey(TASK_SELECTED_KEY))
                mTaskListPosition = savedInstanceState.getInt(TASK_SELECTED_KEY);
    
            // Get title string, if not null (then it is 'all')
            if (savedInstanceState.containsKey(Utils.KEY_FILTER_TITLE)) {
                mFilterTitle = savedInstanceState.getString(Utils.KEY_FILTER_TITLE);
                setTitle(mFilterTitle);
            }
    
            // Get filter type
            if (savedInstanceState.containsKey(Utils.KEY_NAV_FILTER_TYPE)) {
                mNavFilterType = savedInstanceState.getInt(Utils.KEY_NAV_FILTER_TYPE);
        
                // If filter type is All, no need to read other values
                if (mNavFilterType != Utils.NAV_LAYOUT_ALL) {
            
                    // Get selected item's id
                    if (savedInstanceState.containsKey(NAV_SELECTED_ID_KEY))
                        mNavSelectedId = savedInstanceState.getInt(NAV_SELECTED_ID_KEY);
            
                    // Get selected item's filter string
                    if (savedInstanceState.containsKey(Utils.KEY_NAV_FILTER))
                        mNavFilterString = savedInstanceState.getString(Utils.KEY_NAV_FILTER);
                    
                }
            }
    
            // Get search filter string
            if (savedInstanceState.containsKey(Utils.KEY_SEARCH_FILTER))
                mRestoredSearchFilter = savedInstanceState.getString(Utils.KEY_SEARCH_FILTER);
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
    
        mNavDrawerToggle.syncState();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mNavDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Utils.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
        
        // Register to save click location, and edit/delete buttons
        mTasksListView.setOnItemClickListener(this);
        mTasksListView.setMultiChoiceModeListener(this);
        mTasksListView.setOnScrollListener(this);
        
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
    
        } else if (mNavFilterString != null) {
            // First 'back' goes back to showing all tasks
            filterByTagOrTeam(Utils.NAV_LAYOUT_ALL, null, false, false);
            
        } else {
            // Second 'back' closes activity
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
            if (searchView != null) {
                searchView.setOnQueryTextListener(this);
                if (mRestoredSearchFilter != null && !mRestoredSearchFilter.isEmpty()) {
                    searchItem.expandActionView();
                    searchView.setQuery(mRestoredSearchFilter, true);
                    mRestoredSearchFilter = null;
                    searchView.clearFocus();
                }
            }
    
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Duplicated from onResume (since it's called later, which means this broadcast isn't fired
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(Utils.DONE_LOCAL_BROADCAST_LISTENER_INTENT));
        
        switch (requestCode) {
            case Utils.NEW_DONE_ACTIVITY_IDENTIFIER: {
                if (resultCode == RESULT_OK && data != null) {
                    Utils.sendMessage(
                            MainActivity.this,
                            Utils.SENDER_NEW_DONE_ACTIVITY,
                            "Task created, or edited successfully.",
                            data.getIntExtra(Utils.INTENT_ACTION, Utils.NEW_TASK_SAVED),
                            1);
                }
                break;
            }
        
            case Utils.TASK_DETAILS_ACTIVITY_IDENTIFIER: {
                if (resultCode == Utils.RESULT_TASK_DELETED && data != null) {
                    int deletedCount = data.getIntExtra(Utils.INTENT_COUNT, -1);
                    Utils.sendMessage(
                            MainActivity.this,
                            Utils.SENDER_TASK_DETAILS_ACTIVITY,
                            "Task deleted, or marked for deletion successfully",
                            Utils.TASK_DELETED_SNACKBAR,
                            deletedCount);
                }
                break;
            }
        
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    /**
     * My methods
     */
    
    private void setupNavBar() {
        
        // Add hamburger icon toggle to action bar for the drawer
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mNavDrawerToggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null)
            drawer.addDrawerListener(mNavDrawerToggle);
    
    
        // Add name and email address to nav header
        TextView navHeaderName = (TextView) findViewById(R.id.username_textview);
        if (navHeaderName != null)
            navHeaderName.setText(Utils.getUsername(MainActivity.this));
    
    
        // Set up ALL TASKS item
        View allTasksView = findViewById(R.id.nav_drawer_all_tasks);
        if (allTasksView != null) {
            
            // Set text for All Tasks TextView
            TextView allTasksTextView = (TextView) allTasksView.findViewById(R.id.team_name_text_view);
            if (allTasksTextView != null)
                allTasksTextView.setText(R.string.all_tasks);
            
            // Set image resource for All Tasks icon
            ImageView allTasksImageView = (ImageView) allTasksView.findViewById(R.id.nav_team_color_patch);
            if (allTasksImageView != null)
                allTasksImageView.setImageDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.nav_list_alltasks_icon));
            
            // Add click listener
            allTasksView.setOnClickListener(this);
        }
    
    
        // Set up SETTINGS item
        View settingsView = findViewById(R.id.nav_settings);
        if (settingsView != null) {
            // Set text for Settings TextView
            TextView settingsTextView = (TextView) settingsView.findViewById(R.id.team_name_text_view);
            if (settingsTextView != null)
                settingsTextView.setText(R.string.action_settings);
    
            // Set image resource for settings icon
            ImageView settingsImageView = (ImageView) settingsView.findViewById(R.id.nav_team_color_patch);
            BitmapDrawable settingsIcon = (BitmapDrawable) ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_settings_black_24dp).mutate();
            settingsIcon.setAlpha(0x8A);
            if (settingsImageView != null)
                settingsImageView.setImageDrawable(settingsIcon);
    
            // Add click listener
            settingsView.setOnClickListener(this);
        }
    
    
        // Set up EMPTY TAGS item
        View tagsPlaceholderView = findViewById(R.id.nav_tags_empty);
        if (tagsPlaceholderView != null) {
            // Set text for Settings TextView
            TextView tagsPlaceholderTextView = (TextView) tagsPlaceholderView.findViewById(R.id.team_name_text_view);
            if (tagsPlaceholderTextView != null)
                tagsPlaceholderTextView.setText(R.string.tags_placeholder);
            
            // Set image resource for settings icon
            ImageView tagsPlaceholderImageView = (ImageView) tagsPlaceholderView.findViewById(R.id.nav_team_color_patch);
            BitmapDrawable settingsIcon = (BitmapDrawable) ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_label_black_24dp).mutate();
            settingsIcon.setAlpha(0x8A);
            if (tagsPlaceholderImageView != null)
                tagsPlaceholderImageView.setImageDrawable(settingsIcon);
    
            // Add click listener
            tagsPlaceholderView.setOnClickListener(this);
        }
    
        // Load tags
        getLoaderManager().initLoader(TAG_LIST_LOADER, null, this);
    
        // Load teams
        getLoaderManager().initLoader(TEAM_LIST_LOADER, null, this);
    
        // Set initial highlighting
        filterByTagOrTeam(mNavFilterType, null, false, true);
    }
    
    
    private void setupTasksListView() {
        
        // Add listview adapter
        mTaskListAdapter = new TaskListAdapter(this, R.layout.tasks_list_row_layout, null, 0);
        
        mTasksListView = (ListView) findViewById(R.id.dones_list_view);
        if (mTasksListView != null)
            mTasksListView.setAdapter(mTaskListAdapter);
        
        // Initialize the Loader with id TASK_LIST_LOADER and callbacks 'this'.
        getLoaderManager().initLoader(TASK_LIST_LOADER, null, this);
        
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
    
    /**
     * filters task list (and highlights selected nav drawer item)
     *
     * @param filterType             Tag or Team filter. Value from Utils.NAV_LAYOUT_TEAMS or Utils.NAV_LAYOUT_TAGS
     * @param teamOrTagIdentifier    Bundle with either team id or tag id, both saved with same key as filterType
     * @param isInit                 If called from init, to restart loader
     * @param changeNavHighlightOnly called from loaderFinished when only nav highlighting needs to be changed - fields are already filtered
     */
    private void filterByTagOrTeam(int filterType, Bundle teamOrTagIdentifier, boolean isInit, boolean changeNavHighlightOnly) {
        
        // Uncheck previously selected items
        switch (mNavFilterType) {
            case Utils.NAV_LAYOUT_TEAMS: {
                View teamsLinearLayout = findViewById(R.id.nav_drawer_teams);
                if (teamsLinearLayout != null) {
                    View selectedTeamView = teamsLinearLayout.findViewById(mNavSelectedId);
                    if (selectedTeamView != null)
                        selectedTeamView.setSelected(false);
                }
                break;
            }
            
            case Utils.NAV_LAYOUT_TAGS: {
                View tagsLinearLayout = findViewById(R.id.nav_drawer_tags);
                if (tagsLinearLayout != null) {
                    View selectedTagView = tagsLinearLayout.findViewById(mNavSelectedId);
                    if (selectedTagView != null)
                        selectedTagView.setSelected(false);
                }
                break;
            }
            
            case Utils.NAV_LAYOUT_ALL: {
                View alltasksItem = findViewById(R.id.nav_drawer_all_tasks);
                if (alltasksItem != null)
                    alltasksItem.setSelected(false);
                break;
            }
        }
        
        // Select new selected item, and set title
        switch (filterType) {
            
            case Utils.NAV_LAYOUT_TEAMS: {
                // Get item url
                int teamId = changeNavHighlightOnly ? mNavSelectedId : teamOrTagIdentifier.getInt(DoneListContract.TeamEntry.COLUMN_NAME_ID);
                String teamURL = changeNavHighlightOnly ? mNavFilterString : teamOrTagIdentifier.getString(DoneListContract.TeamEntry.COLUMN_NAME_URL);
                
                // Check selected item
                View teamsLinearLayout = findViewById(R.id.nav_drawer_teams);
                if (teamsLinearLayout != null) {
                    View selectedTeamView = teamsLinearLayout.findViewById(teamId);
                    if (selectedTeamView != null)
                        selectedTeamView.setSelected(true);
                }
                
                // Set filter
                if (!changeNavHighlightOnly) {
                    mNavFilterString = teamURL;
                    mFilterTitle = teamOrTagIdentifier.getString(DoneListContract.TeamEntry.COLUMN_NAME_NAME);
                    mNavSelectedId = teamId;
                    setTitle(mFilterTitle);
                }
                
                break;
            }
            
            case Utils.NAV_LAYOUT_TAGS: {
                // Get item url
                int tagId = changeNavHighlightOnly ? mNavSelectedId : teamOrTagIdentifier.getInt(DoneListContract.TagEntry.COLUMN_NAME_ID);
                
                // Check selected item
                View tagsLinearLayout = findViewById(R.id.nav_drawer_tags);
                if (tagsLinearLayout != null) {
                    View selectedTagView = tagsLinearLayout.findViewById(tagId);
                    if (selectedTagView != null)
                        selectedTagView.setSelected(true);
                }
                
                // Set filter
                if (!changeNavHighlightOnly) {
                    mNavFilterString = DoneListContract.TagEntry.TAG_ID_PRE + tagId + DoneListContract.TagEntry.TAG_ID_POST;
                    mFilterTitle = "#" + teamOrTagIdentifier.getString(DoneListContract.TagEntry.COLUMN_NAME_NAME);
                    mNavSelectedId = tagId;
                    setTitle(mFilterTitle);
                }
                
                break;
            }
            
            case Utils.NAV_LAYOUT_ALL: {
                // Check selected item
                View alltasksItem = findViewById(R.id.nav_drawer_all_tasks);
                if (alltasksItem != null)
                    alltasksItem.setSelected(true);
                
                // Set filter
                if (!changeNavHighlightOnly) {
                    mNavFilterString = null;
                    mFilterTitle = null;
                    mNavSelectedId = -1;
                    setTitle(R.string.app_name);
                }
                
                break;
            }
            
            default:
                Log.w(LOG_TAG, "filterByTagOrTeam: Unidentified filterType: " + filterType);
        }
        
        mNavFilterType = filterType;
        
        // Restart loader to filter tasks, if not equals "init"
        if (!isInit && !changeNavHighlightOnly)
            getLoaderManager().restartLoader(TASK_LIST_LOADER, null, this);
        
        // Close drawer
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null)
            drawer.closeDrawer(GravityCompat.START);
    }
    
    public void toNewDone(View view) {
        Intent newDoneIntent = new Intent(MainActivity.this, NewDoneActivity.class);
        startActivityForResult(newDoneIntent, Utils.NEW_DONE_ACTIVITY_IDENTIFIER);
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
                        mTaskListAdapter.notifyDataSetChanged();
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
        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
        startActivity(aboutIntent);
    }
    
    /**
     * If owner is same as current user, fetch details from database and send to NewDoneActivity
     *
     * @param id row id of selected task in listView
     */
    private void editSelectedItems(long id) {
        Intent editDoneIntent = new Intent(MainActivity.this, NewDoneActivity.class);
        editDoneIntent.putExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, id);
    
        // Start edit activity
        startActivityForResult(editDoneIntent, Utils.NEW_DONE_ACTIVITY_IDENTIFIER);
    }
    
    private void deleteSelectedItems(long[] ids) {
        Log.v(LOG_TAG, "Delete selected items: " + ids.length);
    
        // Log event in analytics
        Utils.sendEvent(mTracker, "Action", "Task Deleted - MainActivity", String.valueOf(ids.length));
        
        // Delete selected ids from database, then from server, finishing with updating tasks from server on success
        int deletedCount = new DoneActions(getApplicationContext()).delete(ids);
    
        Utils.sendMessage(
                MainActivity.this,
                Utils.SENDER_MAIN_ACTIVITY,
                "Task deleted, or marked for deletion successfully",
                Utils.TASK_DELETED_SNACKBAR,
                deletedCount);
    }
    
    /**
     *
     *  LoaderManager.LoaderCallbacks<Cursor> methods - to setup cursor and adapter 
     *  for populating listview
     *
     * */
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        
        switch (id) {
        
            case TASK_LIST_LOADER: {
                
                // Sort order:  Descending, by date.
                String sortOrder = DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " DESC, " +
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " DESC, " +
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " DESC, " +
                        DoneListContract.DoneEntry.COLUMN_NAME_UPDATED + " DESC, " +
                        "_id DESC";
            
                Uri donesUri = DoneListContract.DoneEntry.buildDoneListUri();
            
                String[] cursorProjectionString = new String[]{
                        DoneListContract.DoneEntry.COLUMN_NAME_ID + " AS _id",
                        DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT,
                        DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL,
                        DoneListContract.DoneEntry.COLUMN_NAME_OWNER,
                        DoneListContract.DoneEntry.COLUMN_NAME_UPDATED,
                        DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS
                };
            
                // Filter out deleted tasks
                String selection = DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'FALSE'";
    
                switch (mNavFilterType) {
                    case Utils.NAV_LAYOUT_ALL: {
                        // Show all, no filter required
                        break;
                    }
        
                    case Utils.NAV_LAYOUT_TEAMS: {
                        // Add team filtering string
                        selection += " AND " +
                                DoneListContract.DoneEntry.COLUMN_NAME_TEAM +
                                " IS '" +
                                mNavFilterString +
                                "'";
                        break;
                    }
        
                    case Utils.NAV_LAYOUT_TAGS: {
                        // Add tag filtering string
                        selection += " AND " +
                                DoneListContract.DoneEntry.COLUMN_NAME_TAGS +
                                " LIKE '%" +
                                mNavFilterString +
                                "%'";
                        break;
                    }
        
                }
                
                // Search filter
                if (mSearchFilter != null) {
                    String[] filterArr = mSearchFilter.split(" +");
                
                    for (String filterString : filterArr) {
                        selection += " AND (" +
                                DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT +
                                " LIKE '%" +
                                filterString +
                                "%' OR " +
                                DoneListContract.DoneEntry.COLUMN_NAME_OWNER +
                                " LIKE '%" +
                                filterString +
                                "%')";
                    }
                }
                
                return new CursorLoader(this,
                        donesUri,
                        cursorProjectionString,
                        selection,
                        null,
                        sortOrder);
            }
            
            case TEAM_LIST_LOADER: {
                
                Uri teamUri = DoneListContract.TeamEntry.CONTENT_URI;
                
                String sortOrder = DoneListContract.TeamEntry.COLUMN_NAME_NAME + " COLLATE NOCASE ASC, " +
                        "_id ASC";
                
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
                        sortOrder);
            }
    
            case TAG_LIST_LOADER: {
                
                Uri tagUri = DoneListContract.TagEntry.CONTENT_URI;
                
                String sortOrder = DoneListContract.TagEntry.COLUMN_NAME_NAME + " COLLATE NOCASE ASC";
                
                String[] cursorProjectionString = new String[]{
                        DoneListContract.TagEntry.COLUMN_NAME_ID + " AS _id",
                        DoneListContract.TagEntry.COLUMN_NAME_NAME
                };
            
                return new CursorLoader(this,
                        tagUri,
                        cursorProjectionString,
                        null,
                        null,
                        sortOrder);
            }
            
            default:
                Log.e(LOG_TAG, "No such loader. id: " + id);
                return null;
        }
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        int loaderId = loader.getId();
        
        switch (loaderId) {
            
            case TASK_LIST_LOADER: {
                mTaskListAdapter.swapCursor(data);
    
                if (mTaskListPosition != ListView.INVALID_POSITION)
                    mTasksListView.smoothScrollToPosition(mTaskListPosition);
    
                break;
            }
    
            case TEAM_LIST_LOADER:
            case TAG_LIST_LOADER: {
                populateLayoutFromCursor(data, loaderId);
                
                break;
            }
            
            default:
                Log.e(LOG_TAG, "No such loader.");
        }
    }
    
    /**
     * Populate linear layouts in nav drawer with teams and tags
     *
     * @param cursor   database cursor to tags or teams
     * @param itemType identifier for tags or teams - Utils.NAV_LAYOUT_TEAMS or Utils.NAV_LAYOUT_TAGS
     */
    private void populateLayoutFromCursor(Cursor cursor, int itemType) {
        // Get linear layout to populate
        LinearLayout linearLayout = (LinearLayout) findViewById(idToLayoutMap.get(itemType));
        
        if (linearLayout != null && cursor != null) {
            // Clear out previous data in linear layout
            linearLayout.removeAllViews();
            
            // Set height for list items
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) getResources().getDimension(R.dimen._48dp));
            
            switch (itemType) {
                case TAG_LIST_LOADER: {
                    // Iterate over cursor to populate
                    
                    int idColIndex = cursor.getColumnIndex("_id");
                    int nameColIndex = cursor.getColumnIndex(DoneListContract.TagEntry.COLUMN_NAME_NAME);
                    
                    // When loader is already initialised (recreate activity on finished/rotated), 
                    // cursor is beyond last (from previous iteration). 
                    // Move it to first before starting.
                    cursor.moveToFirst();
                    
                    do {
                        // Set item id
                        View view = getLayoutInflater().inflate(R.layout.nav_list_row_layout, null);
                        view.setId(cursor.getInt(idColIndex));
                        view.setLayoutParams(layoutParams);
                        view.setOnClickListener(navTagsClickListener);
                        
                        // Set image resource for tag icon
                        ImageView tagIconImageView = (ImageView) view.findViewById(R.id.nav_team_color_patch);
                        BitmapDrawable settingsIcon = (BitmapDrawable) ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_label_black_24dp).mutate();
                        settingsIcon.setAlpha(0x8A);
                        if (tagIconImageView != null)
                            tagIconImageView.setImageDrawable(settingsIcon);
                        
                        // Set tag name
                        TextView tagNameTextView = (TextView) view.findViewById(R.id.team_name_text_view);
                        tagNameTextView.setText(cursor.getString(nameColIndex));
                        
                        linearLayout.addView(view);
                        
                    } while (cursor.moveToNext());
                    
                    if (mNavFilterType == Utils.NAV_LAYOUT_TAGS) {
                        Bundle tagDetailsBundle = new Bundle();
                        tagDetailsBundle.putInt(DoneListContract.TagEntry.COLUMN_NAME_ID, mNavSelectedId);
                        tagDetailsBundle.putString(DoneListContract.TagEntry.COLUMN_NAME_NAME, mFilterTitle);
                        
                        filterByTagOrTeam(mNavFilterType, tagDetailsBundle, false, true);
                    }
                    
                    break;
                }
                
                case TEAM_LIST_LOADER: {
                    // Iterate over cursor to populate
                    
                    int idColIndex = cursor.getColumnIndex("_id");
                    int nameColIndex = cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_NAME);
                    int urlColIndex = cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_URL);
                    
                    // When loader is already initialised (recreate activity on finished/rotated), 
                    // cursor is beyond last (from previous iteration). 
                    // Move it to first before starting.
                    cursor.moveToFirst();
                    
                    do {
                        // Set item id
                        View view = getLayoutInflater().inflate(R.layout.nav_list_row_layout, null);
                        view.setId(cursor.getInt(idColIndex));
                        view.setLayoutParams(layoutParams);
                        view.setOnClickListener(navTeamsClickListener);
                        
                        // Set team colour
                        int teamColor = Utils.findTeam(getApplicationContext(), cursor.getString(urlColIndex));
                        
                        ImageView teamSpace = (ImageView) view.findViewById(R.id.nav_team_color_patch);
                        
                        GradientDrawable teamCircle = (GradientDrawable) teamSpace.getDrawable().mutate();
                        teamCircle.setColor(ContextCompat.getColor(getApplicationContext(), Utils.colorArray[teamColor == -1 ? 0 : teamColor % Utils.colorArray.length]));
                        
                        // Set team name
                        TextView teamNameTextView = (TextView) view.findViewById(R.id.team_name_text_view);
                        teamNameTextView.setText(cursor.getString(nameColIndex));
                        
                        linearLayout.addView(view);
                    } while (cursor.moveToNext());
                    
                    if (mNavFilterType == Utils.NAV_LAYOUT_TEAMS) {
                        Bundle teamDetailsBundle = new Bundle();
                        teamDetailsBundle.putInt(DoneListContract.TagEntry.COLUMN_NAME_ID, mNavSelectedId);
                        teamDetailsBundle.putString(DoneListContract.TagEntry.COLUMN_NAME_NAME, mFilterTitle);
                        teamDetailsBundle.putString(DoneListContract.TeamEntry.COLUMN_NAME_URL, mNavFilterString);
                        
                        filterByTagOrTeam(mNavFilterType, teamDetailsBundle, false, true);
                    }
                    
                    break;
                }
            }
        } else
            Log.w(LOG_TAG, "Either layout, or cursor are null");
    }
    
    private void clearLayout(int itemType) {
        // Get linear layout to populate
        LinearLayout linearLayout = (LinearLayout) findViewById(idToLayoutMap.get(itemType));
        
        if (linearLayout != null) {
            // Clear out previous data in linear layout
            linearLayout.removeAllViews();
        }
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case TASK_LIST_LOADER: {
                mTaskListAdapter.swapCursor(null);
                break;
            }
    
            case TEAM_LIST_LOADER: {
                clearLayout(Utils.NAV_LAYOUT_TEAMS);
                break;
            }
    
            case TAG_LIST_LOADER: {
                clearLayout(Utils.NAV_LAYOUT_TAGS);
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
        mSearchFilter = !TextUtils.isEmpty(newText) ? newText : null;
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
        // Track event in analytics
        Utils.sendEvent(mTracker, "Action", "PullToRefresh");
        
        IDTSyncAdapter.syncImmediately(this);
    }
    
    /**
     * Save & restore scroll state
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        // When no item is selected, mTaskListPosition will be set to Listview.INVALID_POSITION,
        // so check for that before storing.
        if (mTaskListPosition != ListView.INVALID_POSITION)
            outState.putInt(TASK_SELECTED_KEY, mTaskListPosition);
    
        // Store variables only if in nav filtered state 
        if (mNavFilterType != Utils.NAV_LAYOUT_ALL) {
            outState.putInt(Utils.KEY_NAV_FILTER_TYPE, mNavFilterType);
        
            // Get selected item's id
            outState.putInt(NAV_SELECTED_ID_KEY, mNavSelectedId);
        
            // Get selected item's filter string
            outState.putString(Utils.KEY_NAV_FILTER, mNavFilterString);
        } else {
            outState.remove(Utils.KEY_NAV_FILTER_TYPE);
            outState.remove(NAV_SELECTED_ID_KEY);
            outState.remove(Utils.KEY_NAV_FILTER);
        }
    
        // Store filter string name
        if (mFilterTitle != null)
            outState.putString(Utils.KEY_FILTER_TITLE, mFilterTitle);
        else
            outState.remove(Utils.KEY_FILTER_TITLE);
    
        // Store search filter string
        if (mSearchFilter != null)
            outState.putString(Utils.KEY_SEARCH_FILTER, mSearchFilter);
        else
            outState.remove(Utils.KEY_SEARCH_FILTER);
        
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
        mTaskListPosition = firstVisibleItem;
    }
    
    /**
     * AbsListView.MultiChoiceModeListener methods - for edit & delete selections
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {
    
        mTaskListPosition = position;
    
        Intent taskDetailsActivity = new Intent(MainActivity.this, TaskDetailsActivity.class);
        // Initial task to show details of 
        taskDetailsActivity.putExtra(Utils.KEY_SELECTED_TASK_ID, rowId);
        // Title string - search string, team name, or #tag
        taskDetailsActivity.putExtra(Utils.KEY_FILTER_TITLE, mFilterTitle);
        // Search string to filter in sql query
        taskDetailsActivity.putExtra(Utils.KEY_SEARCH_FILTER, mSearchFilter);
        // Actual string to filter in sqlite query
        taskDetailsActivity.putExtra(Utils.KEY_NAV_FILTER, mNavFilterString);
        // Type - tags, teams, or all
        taskDetailsActivity.putExtra(Utils.KEY_NAV_FILTER_TYPE, mNavFilterType);
    
        startActivityForResult(taskDetailsActivity, Utils.TASK_DETAILS_ACTIVITY_IDENTIFIER);
    }
    
    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        // If selected item's owner is not the same as user, unselect and show toast
        if (checked) {
            Cursor cursor = (Cursor) mTaskListAdapter.getItem(position);
            String taskOwner = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_OWNER));
        
            if (!taskOwner.equals(Utils.getUsername(getApplicationContext()))) {
                // Uncheck the item
                mTasksListView.setItemChecked(position, false);
            
                // Show error toast
                Toast.makeText(
                        this,
                        R.string.task_owner_mismatch_toast,
                        Toast.LENGTH_LONG)
                        .show();
            
                return;
            }
        }
    
    
        // Show edit and delete buttons based on number of checked items
        int checkedCount = mTasksListView.getCheckedItemCount();
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
        menu.findItem(R.id.menu_edit_done).setVisible(mTasksListView.getCheckedItemCount() <= 1);
        return true;
    }
    
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        // Respond to clicks on the actions in the CAB
        final long[] selectedTasks = mTasksListView.getCheckedItemIds();
        
        switch (item.getItemId()) {
            
            case R.id.menu_delete_done:
                // Show alert message to confirm delete
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.delete_tasks_message))
                        .setPositiveButton(getString(R.string.delete_string), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteSelectedItems(selectedTasks);
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel_string), null)
                        .create()
                        .show();
                
                // Close the CAB
                mode.finish();
                return true;
            
            case R.id.menu_edit_done:
                Log.v(LOG_TAG, "Edit selected item");
    
                // Log event in analytics
                Utils.sendEvent(mTracker, "Action", "Edit Item Clicked on MainActivity");
                
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
            case R.id.nav_drawer_all_tasks: {
                mFilterTitle = null;
                filterByTagOrTeam(Utils.NAV_LAYOUT_ALL, null, false, false);
                
                break;
            }
            
            case R.id.nav_settings: {
                Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                break;
            }
        
            case R.id.nav_tags_empty: {
                Utils.sendEvent(mTracker, "Action", "Empty Tags Clicked");
                Log.i(LOG_TAG, "Tags empty placeholder clicked");
                break;
            }
            
            default:
                Log.w(LOG_TAG, "Unhandled view click: " + v);
        }
    }
    
    
    private class NavTagsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int tagId = v.getId();
            
            Bundle tagDetailsBundle = new Bundle();
            tagDetailsBundle.putInt(DoneListContract.TagEntry.COLUMN_NAME_ID, tagId);
            
            Cursor cursor = getContentResolver().query(
                    DoneListContract.TagEntry.CONTENT_URI,
                    new String[]{
                            DoneListContract.TagEntry.COLUMN_NAME_NAME,
                            DoneListContract.TagEntry.COLUMN_NAME_ID,
                    },
                    DoneListContract.TagEntry.COLUMN_NAME_ID + " IS ?",
                    new String[]{
                            String.valueOf(tagId)
                    },
                    null
            );
            
            if (cursor != null) {
                cursor.moveToNext();
                
                tagDetailsBundle.putString(DoneListContract.TagEntry.COLUMN_NAME_NAME,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.TagEntry.COLUMN_NAME_NAME)));
                //mFilterTitle = cursor.getString(cursor.getColumnIndex(DoneListContract.TagEntry.COLUMN_NAME_NAME));
                
                cursor.close();
                
                filterByTagOrTeam(Utils.NAV_LAYOUT_TAGS, tagDetailsBundle, false, false);
                
            } else
                Log.w(LOG_TAG, "NavTeamsClickListener.onClick: No such team found!");
        }
    }
    
    private class NavTeamsClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            int teamId = v.getId();
            
            Bundle teamDetailsBundle = new Bundle();
            teamDetailsBundle.putInt(DoneListContract.TeamEntry.COLUMN_NAME_ID, teamId);
            
            Cursor cursor = getContentResolver().query(
                    DoneListContract.TeamEntry.CONTENT_URI,
                    new String[]{
                            DoneListContract.TeamEntry.COLUMN_NAME_NAME,
                            DoneListContract.TeamEntry.COLUMN_NAME_URL,
                            DoneListContract.TeamEntry.COLUMN_NAME_SHORT_NAME,
                            DoneListContract.TeamEntry.COLUMN_NAME_ID,
                    },
                    DoneListContract.TeamEntry.COLUMN_NAME_ID + " IS ?",
                    new String[]{
                            String.valueOf(teamId)
                    },
                    null
            );
            
            if (cursor != null) {
                cursor.moveToNext();
                
                teamDetailsBundle.putString(DoneListContract.TeamEntry.COLUMN_NAME_URL,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_URL)));
                
                teamDetailsBundle.putString(DoneListContract.TeamEntry.COLUMN_NAME_NAME,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_NAME)));
                //mFilterTitle = cursor.getString(cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_NAME));
                
                cursor.close();
                
                filterByTagOrTeam(Utils.NAV_LAYOUT_TEAMS, teamDetailsBundle, false, false);
            } else
                Log.w(LOG_TAG, "NavTeamsClickListener.onClick: No such team found!");
        }
    }
}
