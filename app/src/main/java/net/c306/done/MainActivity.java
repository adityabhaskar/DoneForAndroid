package net.c306.done;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity {
    
    private Snackbar newDoneSnackbar;
    private ListView listView;
    private Cursor cursor;
    private String LOG_TAG;
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    // Src: http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
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
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    
        LOG_TAG = getString(R.string.app_log_identifier) + " " + FetchDonesTask.class.getSimpleName();
        
        /*
        * 
        * Precede these with deleting all entries ('delete from dones') from database to do a 
        * fetchAll.
        * 
        * */
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //SharedPreferences.Editor editor = prefs.edit();
        //editor.remove("lastUpdate");
        //editor.apply();
        
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(getString(R.string.done_posted_intent)));
        
        //// DONE: 15/02/16 Update dones from server and update in sqllite 
        new FetchDonesTask(this).execute();
        
        //listView = (ListView) findViewById(R.id.dones_list_view);
    
        // Initialize the adapter. Note that we pass a 'null' Cursor as the
        // third argument. We will pass the adapter a Cursor only when the
        // data has finished loading for the first time (i.e. when the
        // LoaderManager delivers the data to onLoadFinished). Also note
        // that we have passed the '0' flag as the last argument. This
        // prevents the adapter from registering a ContentObserver for the
        // Cursor (the CursorLoader will do this for us!).
        //listViewAdapter = new DoneListAdapter(this, R.layout.list_row_layout, doneListCursor, 0);
        
        
        // Associate the (now empty) adapter with the ListView.
        //listView.setAdapter(listViewAdapter);
    
        // The Activity (which implements the LoaderCallbacks<Cursor>
        // interface) is the callbacks object through which we will interact
        // with the LoaderManager. The LoaderManager uses this object to
        // instantiate the Loader and to notify the client when data is made
        // available/unavailable.
        //mCallbacks = this;
    
        // Initialize the Loader with id '1' and callbacks 'mCallbacks'.
        // If the loader doesn't already exist, one is created. Otherwise,
        // the already created Loader is reused. In either case, the
        // LoaderManager will manage the Loader across the Activity/Fragment
        // lifecycle, will receive any new loads once they have completed,
        // and will report this new data back to the 'mCallbacks' object.
        //android.app.LoaderManager lm = getLoaderManager();
        //lm.initLoader(LOADER_ID, null, mCallbacks);
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
    
    
    
}