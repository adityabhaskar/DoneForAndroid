package net.c306.done.idonethis;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;

import net.c306.done.R;
import net.c306.done.Utils;
import net.c306.done.db.DoneListContract;

import java.util.Arrays;
import java.util.List;

/**
 * Created by raven on 29/02/16.
 */
public class DoneActions {
    
    private Context mContext;
    private String mUsername;
    private String LOG_TAG;
    
    public DoneActions(Context mContext) {
        this.mContext = mContext;
    
        LOG_TAG = mContext.getString(R.string.APP_LOG_IDENTIFIER) + " " + DoneActions.class.getSimpleName();
    
        mUsername = Utils.getUsername(mContext);
    }
    
/*
    public boolean add(DoneItem doneItem) {
        return false;
    }
*/
    
    /**
     * Save edited task in database, then send to server with PostEditedDoneTask
     *
     * @param editedDetails Bundle with edited details and editedFields ArrayList
     * @return boolean for success or failure
     */
    public boolean edit(Bundle editedDetails) {
        long id = editedDetails.getLong(DoneListContract.DoneEntry.COLUMN_NAME_ID, -1);
        
        if (id == -1) {
            Log.w(LOG_TAG, "No id passed to edit()");
            return false;
        }
        
        String doneText = editedDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT);
        String doneDate = editedDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
        String teamShortName = editedDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME);
        List<String> editedFields = editedDetails.getStringArrayList(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS);
        
        // Save to database 
        ContentValues editedContentValues = new ContentValues();
        editedContentValues.put(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, doneText);
        //editedContentValues.put(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, doneDate);
        //editedContentValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME, teamShortName);
        
        editedContentValues.put(
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS,
                new Gson().toJson(editedFields)
        );
        
        mContext.getContentResolver().update(
                DoneListContract.DoneEntry.CONTENT_URI,
                editedContentValues,
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " IS ?",
                new String[]{String.valueOf(id)}
        );
        
        // TODO: 29/02/16 Submit to server, update local database from server
        
        return true;
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
    
        // Get subset of selected tasks from database, where owner is same as username 
        // these are the only ones deleted on server
        Uri queryUri = DoneListContract.DoneEntry.buildDoneListUri();
        
        String[] mProjection = new String[]{
                DoneListContract.DoneEntry.COLUMN_NAME_ID,
                DoneListContract.DoneEntry.COLUMN_NAME_OWNER,
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
    
    
        // If owner is same as user, mark is_deleted as true in local database 
        String allowedIdListString = Arrays.toString(allowedIdList).replace("[", "(").replace("]", ")");
        Log.v(LOG_TAG, "Delete allowed list: " + allowedIdListString);
        
        String deleteSelectionClause = DoneListContract.DoneEntry.COLUMN_NAME_ID + " IN " + allowedIdListString;
        ContentValues contentValues = new ContentValues();
        contentValues.put(DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED, "TRUE");
    
        // mark to be deleted items as is_deleted = true
        int rowsMarkedAsDeleted = mContext.getContentResolver().update(
                DoneListContract.DoneEntry.CONTENT_URI,
                contentValues,
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " IN " + allowedIdListString,
                null
        );
    
        // items where is_deleted and is_local are both true, delete entirely
        int rowsDeleted = mContext.getContentResolver().delete(
                DoneListContract.DoneEntry.CONTENT_URI,
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'TRUE' AND " +
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'TRUE'",
                null
        );
    
        Log.v(LOG_TAG, "Rows marked as deleted in database: " + rowsMarkedAsDeleted);
        Log.v(LOG_TAG, "Rows completely deleted from database: " + rowsDeleted);
    
        // Delete on server, update local database from server
        if (rowsMarkedAsDeleted > 0) {
            new DeleteDonesTask(mContext).execute(false);
        }
    
        // For any, if owner was not the same as user, show error toast
        if (allowedIdList.length < taskIds.length) {
            Log.w(LOG_TAG, "Some tasks not deleted as you were not the owner.");
            Toast.makeText(mContext, "Some tasks not deleted as you were not the owner.", Toast.LENGTH_LONG).show();
        }
    
        return rowsMarkedAsDeleted;
    }
    
}
