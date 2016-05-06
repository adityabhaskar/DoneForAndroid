package net.c306.done;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class TaskDetailsActivity
        extends AppCompatActivity
        implements TaskDetailsFragment.OnFragmentInteractionListener {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_details);
        
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.task_fragment_container) != null) {
            
            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null)
                return;
            
            // Create a new Fragment to be placed in the activity layout
            TaskDetailsFragment taskDetailsFragment = TaskDetailsFragment.newInstance(
                    getIntent().getLongExtra(Utils.TASK_DETAILS_TASK_ID, -1),
                    getIntent().getStringExtra(Utils.TASK_DETAILS_SEARCH_FILTER),
                    getIntent().getStringExtra(Utils.TASK_DETAILS_TEAM_FILTER)
            );
            
            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.task_fragment_container, taskDetailsFragment).commit();
        }
    }
    
    @Override
    public void onFragmentInteraction(Uri uri) {
        // nothing to do yet
    }
}
