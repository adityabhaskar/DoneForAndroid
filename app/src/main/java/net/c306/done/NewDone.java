package net.c306.done;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class NewDone extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_done);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.new_done_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add_dones:
                // User chose the "Add" item, save
                //// TODO: 13/02/16 Save text to storage, start a background sync to server, return to parent 
                saveDone();
                return true;
            
            case R.id.action_settings:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                Intent settingsIntent = new Intent(NewDone.this, SettingsActivity.class);
                startActivity(settingsIntent);
                return true;
            
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
            
        }
    }
    
    private void saveDone(){
        EditText doneEditText = (EditText) findViewById(R.id.done_edit_text);
        String doneText = doneEditText.getText().toString();
        if(doneText.isEmpty()){
            doneEditText.setError("Enter a completed task");
        } else {
            Toast t = Toast.makeText(getApplicationContext(), "Done saved!", Toast.LENGTH_SHORT);
            t.show();
            Log.wtf("Done Text",doneText);
            finish();
            
        }
    }
}
