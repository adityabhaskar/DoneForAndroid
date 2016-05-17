package net.c306.done;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.analytics.Tracker;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;

public class TaskDetailsActivity 
        extends AppCompatActivity
        implements
        TaskDetailsFragment.OnFragmentInteractionListener,
        ViewPager.OnPageChangeListener {
    
    private final String LOG_TAG = Utils.LOG_TAG + getClass().getSimpleName();
    private final String CURRENT_TASK_INDEX_KEY = "currentTaskIndex";
    
    private long mId;                                   // Current task's db id
    private String mSearchFilter = null;                // Search phrase to filter
    private String mNavFilter = null;                   // Team or tag filter query string
    private int mNavFilterType = Utils.NAV_LAYOUT_ALL;  // Nav filter type - team, tag, or all 
    private String mFilterTitle = null;                  // Title string - team name, #tag or search phrase
    
    private boolean mIsOwner = false;                   // Is user owner of mId task
    
    private long[] mTaskIdList;                         // List of db ids for all tasks given current filters
    private int mCurrentTaskIndex = -1;                 // Index of mId in mTaskIdList
    
    private Snackbar mSnackbar;                         // Snackbar instance to show messages
    private Tracker mTracker;                           // Google Analytics tracker
    
    private SectionsPagerAdapter mSectionsPagerAdapter; // Adapter to provide views for view pager
    
    private ViewPager mViewPager;                       // View pager to display task details in
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);
    
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    
        // Instantiate variables
        Intent receivedData = getIntent();
        mId = receivedData.getLongExtra(Utils.KEY_SELECTED_TASK_ID, -1);
        mSearchFilter = receivedData.getStringExtra(Utils.KEY_SEARCH_FILTER);
        mNavFilter = receivedData.getStringExtra(Utils.KEY_NAV_FILTER);
        mNavFilterType = receivedData.getIntExtra(Utils.KEY_NAV_FILTER_TYPE, mNavFilterType);
        mFilterTitle = receivedData.getStringExtra(Utils.KEY_FILTER_TITLE);
        
        // Get saved instance state if recreating
        if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
            if (savedInstanceState.containsKey(CURRENT_TASK_INDEX_KEY))
                mCurrentTaskIndex = savedInstanceState.getInt(CURRENT_TASK_INDEX_KEY);
        }
    
        // Set title to search phrase, team name, or app name, in order
        mFilterTitle = (mSearchFilter != null && !mSearchFilter.isEmpty()) ? mSearchFilter :
                (mNavFilter != null && !mNavFilter.isEmpty()) ? mFilterTitle :
                        getString(R.string.app_name);
        setTitle(mFilterTitle);
        
        // Get task id list
        getTaskList();
    
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.view_pager_container);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
    
            // Set to display selected task, instead of first task 
            if (mCurrentTaskIndex > -1)
                mViewPager.setCurrentItem(mCurrentTaskIndex);
    
            mViewPager.setClipChildren(false);
        }
    
        // Analytics Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    
        // Log screen open in Analytics
        Utils.sendScreen(mTracker, getClass().getSimpleName());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        if (mViewPager != null)
            mViewPager.addOnPageChangeListener(this);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (mViewPager != null)
            mViewPager.removeOnPageChangeListener(this);
    }
    
    /**
     * Gets from database the list of task ids matching current filters and saves them in an array
     * Also sets the current task index for mId's position in the array
     */
    private void getTaskList() {
        
        // Sort order:  Descending, by date - same as MainActivity
        String sortOrder = DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " DESC, " +
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " DESC, " +
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " DESC, " +
                DoneListContract.DoneEntry.COLUMN_NAME_UPDATED + " DESC, " +
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " DESC";
        
        String[] cursorProjectionString = new String[]{
                DoneListContract.DoneEntry.COLUMN_NAME_ID
        };
        
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
                        mNavFilter +
                        "'";
                break;
            }
        
            case Utils.NAV_LAYOUT_TAGS: {
                // Add tag filtering string
                selection += " AND " +
                        DoneListContract.DoneEntry.COLUMN_NAME_TAGS +
                        " LIKE '%" +
                        mNavFilter +
                        "%'";
                break;
            }
        
        }
        
        
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
        
        Cursor cursor = getContentResolver().query(
                DoneListContract.DoneEntry.CONTENT_URI,
                cursorProjectionString,
                selection,
                null,
                sortOrder
        );
        
        if (cursor != null) {
            
            mTaskIdList = new long[cursor.getCount()];
            int i = 0;
            int columnIndex = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_ID);
            
            while (cursor.moveToNext()) {
                mTaskIdList[i] = cursor.getLong(columnIndex);
                if (mTaskIdList[i] == mId) {
                    mCurrentTaskIndex = i;
                }
                
                i++;
            }
            
            cursor.close();
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mCurrentTaskIndex != -1)
            outState.putInt(CURRENT_TASK_INDEX_KEY, mCurrentTaskIndex);
        else
            outState.remove(CURRENT_TASK_INDEX_KEY);
    
        super.onSaveInstanceState(outState);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task_details, menu);
    
        // If user not owner, don't show edit & delete actions 
        if (!mIsOwner) {
            menu.findItem(R.id.action_edit).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
        }
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        
        switch (id) {
            
            case R.id.action_settings: {
                Intent settingsIntent = new Intent(TaskDetailsActivity.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            }
            
            case R.id.action_edit: {
                Intent editDoneIntent = new Intent(TaskDetailsActivity.this, NewDoneActivity.class);
                editDoneIntent.putExtra(DoneListContract.DoneEntry.COLUMN_NAME_ID, mTaskIdList[mCurrentTaskIndex]);
    
                // Start edit activity
                startActivityForResult(editDoneIntent, Utils.NEW_DONE_ACTIVITY_IDENTIFIER);
                
                return true;
            }
            
            case R.id.action_delete: {
                // Show alert message to confirm delete
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.delete_task_message))
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Delete selected ids from database, then from server, finishing with updating tasks from server on success
                                int deletedCount = new DoneActions(getApplicationContext()).delete(new long[]{mTaskIdList[mCurrentTaskIndex]});
                                
                                Log.v(LOG_TAG, deletedCount + " task deleted. \n Id: " + mTaskIdList[mCurrentTaskIndex]);
    
                                Utils.sendEvent(mTracker, Utils.ANALYTICS_CATEGORY_ACTION, "Task Deleted - Details", String.valueOf(1));
    
                                Intent dataIntent = new Intent();
                                dataIntent.putExtra(Utils.INTENT_COUNT, deletedCount);
                                setResult(Utils.RESULT_TASK_DELETED, dataIntent);
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
                
                return true;
            }
            
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If task edited successfully
        if (requestCode == Utils.NEW_DONE_ACTIVITY_IDENTIFIER && resultCode == RESULT_OK) {
            Utils.sendEvent(mTracker, Utils.ANALYTICS_CATEGORY_ACTION, "Task Edited - Details");
        
            // Show snackbar
            if (mSnackbar != null && mSnackbar.isShown())
                mSnackbar.dismiss();
        
            mSnackbar = Snackbar.make(findViewById(R.id.view_pager_container),
                    R.string.edited_task_saved_toast_message,
                    Snackbar.LENGTH_SHORT);
            mSnackbar.setAction("Action", null).show();
    
    
            // TODO: 16/05/16 Ask the adapter to remove all items and recreate - team/tag/text changes may have rendered current filtered list incorrect 
            // Update view
            mSectionsPagerAdapter.notifyDataSetChanged();
        
            // Get currently visible fragment (which was edited, and ask it to refresh its contents)
            TaskDetailsFragment taskDetailsFragment = mSectionsPagerAdapter.getFragment(mCurrentTaskIndex);
            if (taskDetailsFragment != null)
                taskDetailsFragment.refreshContent();
        
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override
    public void setOwnerMenu(boolean isOwner) {
        boolean prevIsOwner = mIsOwner;
        mIsOwner = isOwner;
    
        if (mIsOwner != prevIsOwner)
            invalidateOptionsMenu();
    }
    
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        
    }
    
    @Override
    public void onPageSelected(int position) {
        mCurrentTaskIndex = position;
    }
    
    @Override
    public void onPageScrollStateChanged(int state) {
        
    }
    
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            // TODO: 16/05/16 Create an observer to listen to data set changes and recreate the fragments 
            //SectionsPagerAdapter.this.registerDataSetObserver(something);
        }
        
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            // Create a new Fragment to be placed in the activity layout
            
            // Get the item with id at 'position' in mTaskIdList 
            
            return TaskDetailsFragment.newInstance(mTaskIdList[position]);
        }
    
    
        public TaskDetailsFragment getFragment(int index) {
            /**
             * Fragments are cached in Adapter, so instantiateItem will return 
             * the cached one (if any) or a new instance if necessary. 
             */
        
            return (TaskDetailsFragment) instantiateItem(mViewPager, index);
        }
    
        @Override
        public int getCount() {
            return mTaskIdList.length;
        }
        
    }
}
