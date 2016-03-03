package net.c306.done.idonethis;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import net.c306.done.DoneItem;
import net.c306.done.R;
import net.c306.done.db.DoneListContract;

import java.util.Arrays;

/**
 * Created by raven on 29/02/16.
 */
public class DoneActions {
    
    private Context mContext;
    private String mUsername;
    private String LOG_TAG;
    
    public DoneActions(Context mContext) {
        this.mContext = mContext;
        
        LOG_TAG = mContext.getString(R.string.app_log_identifier) + " " + DoneActions.class.getSimpleName();
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mUsername = prefs.getString(mContext.getString(R.string.username), null);
    }
    
/*
    public boolean add(DoneItem doneItem) {
        return false;
    }
*/
    
    public boolean edit(DoneItem doneItem) {
        // TODO: 29/02/16 For each, if owner is same as user, edit in local database with isLocal = true, editedFields = '[...]'
        // TODO: 29/02/16 If online, submit to server, update local database from server
        // TODO: 29/02/16 For any, if owner was not the same as user, show error toast
        return false;
    }
    
    public int delete(long[] taskIds) {
        if (mUsername == null) {
            Log.e(LOG_TAG, "No username!");
            return -1;
        }
        
        long allowedIdList[];
        
        // Convert idList to a set in a String (...)
        String providedIdListString = Arrays.toString(taskIds).replace("[", "(").replace("]", ")");
        Log.v(LOG_TAG, "Delete requested list: " + providedIdListString);
        
        // DONE: 29/02/16 Get subset of selected tasks from database, where owner is same as username 
        // DONE: these are the only ones deleted on server
        Uri queryUri = DoneListContract.DoneEntry.buildDoneListUri();
        
        String[] mProjection = new String[]{
                DoneListContract.DoneEntry.COLUMN_NAME_ID,
                DoneListContract.DoneEntry.COLUMN_NAME_OWNER
        };
        
        String querySelectionClause = DoneListContract.DoneEntry.COLUMN_NAME_ID + " IN " + providedIdListString + " AND " +
                DoneListContract.DoneEntry.COLUMN_NAME_OWNER + " is ?"; // delete only if owner is same as current user
        
        String[] querySelectionArgs = {mUsername};
        
        Cursor cursor = mContext.getContentResolver().query(queryUri, mProjection, querySelectionClause, querySelectionArgs, null);
        
        if (cursor != null) {
            //Log.v(LOG_TAG, "Delete allowed item count: " + cursor.getCount());
            
            int idIndex = cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_ID);
            allowedIdList = new long[cursor.getCount()];
            
            int i = 0;
            while (cursor.moveToNext()) {
                allowedIdList[i] = cursor.getLong(idIndex);
                i++;
            }
            
            cursor.close();
            
        } else {
            Log.w(LOG_TAG, "No cursor returned from query, so no delete.");
            return -1;
        }
        
        
        // DONE: 29/02/16 If owner is same as user, delete from local database
        String allowedIdListString = Arrays.toString(allowedIdList).replace("[", "(").replace("]", ")");
        Log.v(LOG_TAG, "Delete allowed list: " + allowedIdListString);
        
        Uri deleteUri = DoneListContract.DoneEntry.CONTENT_URI;
        
        String deleteSelectionClause = DoneListContract.DoneEntry.COLUMN_NAME_ID + " IN " + allowedIdListString;
        
        //String[] deleteSelectionArgs = {allowedIdListString};
        
        //int rowsDeleted = mContext.getContentResolver().delete(deleteUri, deleteSelectionClause, deleteSelectionArgs);
        int rowsDeleted = mContext.getContentResolver().delete(deleteUri, deleteSelectionClause, null);
        
        Log.v(LOG_TAG, "Deleted rows from database: " + rowsDeleted);
        
        // DONE: 29/02/16 If online, delete on server, update local database from server
        if (rowsDeleted > 0) {
            new DeleteDonesTask(mContext).execute(allowedIdList);
        }
        
        // DONE: 29/02/16 For any, if owner was not the same as user, show error toast
        if (allowedIdList.length < taskIds.length) {
            Log.w(LOG_TAG, "Some tasks not deleted as you were not the owner.");
            Toast.makeText(mContext, "Some tasks not deleted as you were not the owner.", Toast.LENGTH_LONG).show();
        }
        
        return rowsDeleted;
    }
    
}
