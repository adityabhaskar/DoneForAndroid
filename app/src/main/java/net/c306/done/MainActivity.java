package net.c306.done;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    
    private Snackbar newDoneSnackbar;
    
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    // Src: http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get count of messages sent - >0 means success
            int count = intent.getIntExtra("count", -1);
            String message = intent.getStringExtra("message");
            
            if(count > 0){
                if(newDoneSnackbar != null && newDoneSnackbar.isShown())
                    newDoneSnackbar.dismiss();
                
                newDoneSnackbar = Snackbar.make(findViewById(R.id.fab), (count > 1 ? count + " dones " : "Done ") + getString(R.string.done_sent_toast_message), Snackbar.LENGTH_LONG);
                newDoneSnackbar.setAction("Action", null).show();
                
                Log.wtf(getString(R.string.app_log_identifier) + " BroadcastReceiver", "Got message: " + count + " " + message);
                
                //// TODO: 15/02/16 re-fetch list from server
                
            } else {
                Log.wtf(getString(R.string.app_log_identifier) + " BroadcastReceiver", "Got error: " + count);
                
                //// TODO: 15/02/16 Schedule another post attempt - on reconnection or timer
                
            }
            
        }
    };
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    
        // Register to receive messages.
        // We are registering an observer (mMessageReceiver) to receive Intents
        // with actions named "custom-event-name".
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(getString(R.string.done_posted_intent)));
        
        //// TODO: 15/02/16 Update dones from server and update in sqllite 
        new FetchDones(this).execute();
    }
    
    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }
    
    public void toNewDone(View view){
        Intent newDoneIntent = new Intent(MainActivity.this, NewDone.class);
        startActivityForResult(newDoneIntent, 1);
        
    }
    
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if(newDoneSnackbar != null && newDoneSnackbar.isShown())
            newDoneSnackbar.dismiss();
        
        switch (resultCode){
            case RESULT_OK:
                newDoneSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.done_saved_toast_message, Snackbar.LENGTH_LONG);
                newDoneSnackbar.setAction("Action", null).show();
                break;
            
            case R.integer.result_offline:
                newDoneSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.done__offline_saved_toast_message, Snackbar.LENGTH_LONG);
                newDoneSnackbar.setAction("Action", null).show();
                break;
            
            default:
                Log.wtf(getString(R.string.app_log_identifier) + " Main Activity", "Result Code: " + resultCode);
    
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
    
    
    private void writeDoneToDb(String doneText){
        // From http://developer.android.com/training/basics/data-storage/databases.html
/*
        DoneListDbHelper mDbHelper = new DoneListDbHelper(getApplicationContext());
        
        // Gets the data repository in write mode
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
    
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        //values.put(DoneListContract.DoneEntry.COLUMN_NAME_ENTRY_ID, id);
        //values.put(DoneListContract.DoneEntry.COLUMN_NAME_TITLE, title);
    
        // Insert the new row, returning the primary key value of the new row
        long newRowId;
        newRowId = db.insert(
                DoneListContract.DoneEntry.TABLE_NAME,
                DoneListContract.DoneEntry.COLUMN_NAME_NULLABLE,
                values);
        
*/
    }
    
    private String readDonesFromDb(){
        // From http://developer.android.com/training/basics/data-storage/databases.html
/*
        DoneListDbHelper mDbHelper = new DoneListDbHelper(getApplicationContext());
    
        // Gets the data repository in read mode
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                DoneListContract.DoneEntry._ID,
                DoneListContract.DoneEntry.COLUMN_NAME_TITLE,
                //DoneListContract.DoneEntry.COLUMN_NAME_UPDATED,
        };

// How you want the results sorted in the resulting Cursor
        String sortOrder =
                DoneListContract.DoneEntry.COLUMN_NAME_UPDATED + " DESC";
    
        Cursor c = db.query(
                DoneListContract.DoneEntry.TABLE_NAME,  // The table to query
                projection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
    
        cursor.moveToFirst();
        long itemId = cursor.getLong(
                cursor.getColumnIndexOrThrow(FeedEntry._ID)
        );
*/
        return " ";
    }
    
}

/*
        //Fetch all entries, change keys to NewDone-id
        SharedPreferences settings = getSharedPreferences(getString(R.string.done_file_name_shared_preferences), 0);
        SharedPreferences.Editor editor = settings.edit();
        
        editor.putString("authToken", "");
        editor.apply();
        
        Map<String, ?> prefs = settings.getAll();
        for ( Map.Entry<String, ?> entry : prefs.entrySet()) {
            String key = entry.getKey();
            //if(key != "id"){
            //    editor.putString("NewDone-" + key, entry.getValue().toString());
            //    editor.remove(key);
            //}
            //Log.v(getString(R.string.app_log_identifier), key + ": \n" + entry.getValue());
        }
        editor.apply();
        
        prefs = settings.getAll();
        for ( Map.Entry<String, ?> entry : prefs.entrySet()) {
            Log.v(getString(R.string.app_log_identifier), entry.getKey() + ": \n" + entry.getValue());
        }
*/
