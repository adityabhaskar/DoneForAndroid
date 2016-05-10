package net.c306.done;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
import android.view.ViewGroup;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DoneActions;

public class TaskDetailsActivity 
        extends AppCompatActivity
        implements TaskDetailsFragment.OnFragmentInteractionListener {
    
    private final String LOG_TAG = Utils.LOG_TAG + getClass().getSimpleName();
    private final String CURRENT_TASK_INDEX_KEY = "currentTaskIndex";
    
    private long mId;
    private String mTeamFilter = null;
    private String mSearchFilter = null;
    
    private long[] mTaskIdList;
    private int mCurrentTaskIndex = -1;
    
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);
    
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    
        // Instantiate variables
        Intent receivedData = getIntent();
        mId = receivedData.getLongExtra(Utils.TASK_DETAILS_TASK_ID, -1);
        mSearchFilter = receivedData.getStringExtra(Utils.TASK_DETAILS_SEARCH_FILTER);
        mTeamFilter = receivedData.getStringExtra(Utils.TASK_DETAILS_TEAM_FILTER);
        
        // Get saved instance state if recreating
        if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
            if (savedInstanceState.containsKey(CURRENT_TASK_INDEX_KEY))
                mCurrentTaskIndex = savedInstanceState.getInt(CURRENT_TASK_INDEX_KEY);
        }
    
        // Set title to search phrase, team name, or app name, in order
        if (mSearchFilter != null && !mSearchFilter.isEmpty())
            setTitle(mSearchFilter);
        else if (mTeamFilter != null && !mTeamFilter.isEmpty())
            setTitle(receivedData.getStringExtra(Utils.TASK_DETAILS_TEAM_NAME));
        else
            setTitle(R.string.app_name);
        
        // Get task id list
        getTaskList();
    
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
    
        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        if (mViewPager != null) {
            mViewPager.setAdapter(mSectionsPagerAdapter);
        
            // Set to display selected task, instead of first task 
            if (mCurrentTaskIndex > -1)
                mViewPager.setCurrentItem(mCurrentTaskIndex);
        
            mViewPager.setClipChildren(false);
        }
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
                //DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT,
                //DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                ////DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME,
                //DoneListContract.DoneEntry.COLUMN_NAME_TEAM,
                //DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL,
                //DoneListContract.DoneEntry.COLUMN_NAME_UPDATED,
                //DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS
        };
        
        String selection = DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'FALSE'";
        
        if (mSearchFilter != null) {
            String[] filterArr = mSearchFilter.split(" +");
            
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
                // TODO: 09/05/16 get current activity id, and start edit activity for that id 
                return true;
            }
            
            case R.id.action_delete: {
                // Show alert message to confirm delete
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.delete_task_message))
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO: 10/05/16 Analytics tracking 
                                //Utils.sendEvent(mTracker, "Action", "DeleteDones", String.valueOf(ids.length));
                                
                                // Delete selected ids from database, then from server, finishing with updating tasks from server on success
                                int deletedCount = new DoneActions(getApplicationContext()).delete(new long[]{mTaskIdList[mCurrentTaskIndex]});
                                
                                Log.v(LOG_TAG, deletedCount + " task deleted. \n Id: " + mTaskIdList[mCurrentTaskIndex]);
                                
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
    public void onFragmentInteraction(Uri uri) {
        // nothing to do yet
    }
    
    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {
        
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        
        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            // Set the primary visible task as current task
            // Can't do this in getItem since it fetches multiple items - previous and next too - 
            // to maintain speed of transitions
            mCurrentTaskIndex = position;
            super.setPrimaryItem(container, position, object);
        }
        
        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            // Create a new Fragment to be placed in the activity layout
            
            // Get the item with id at 'position' in mTaskIdList 
            
            return TaskDetailsFragment.newInstance(mTaskIdList[position]);
        }
        
        @Override
        public int getCount() {
            return mTaskIdList.length;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return getString(R.string.app_name);
        }
    }
}
