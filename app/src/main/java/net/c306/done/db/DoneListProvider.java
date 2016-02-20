package net.c306.done.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by raven on 18/02/16.
 */
public class DoneListProvider extends ContentProvider {
    
    static final int DONE_ITEM = 100;
    static final int DONES = 101;
    static final int DONES_WITH_TEAM = 102;
    static final int DONES_WITH_DATE = 103;
    static final int DONES_WITH_TEAM_AND_DATE = 104;
    static final int TEAM = 300;
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sDonesByTeamQueryBuilder;
    //team_short_name = ?
    private static final String sTeamSelection = 
                    DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME+ " = ? ";
    //team_short_name = ? AND done_date >= ?
    private static final String sTeamWithStartDateSelection =
                    DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME + " = ? AND " +
                    DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE+ " >= ? ";
    //team_short_name = ? AND done_date = ?
    private static final String sTeamAndDateSelection =
                    DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME + " = ? AND " +
                    DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " = ? ";
    //done_date = ?
    private static final String sDateSelection =
                    DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " = ? ";
    
    static{
        sDonesByTeamQueryBuilder = new SQLiteQueryBuilder();
        
        sDonesByTeamQueryBuilder.setTables(
                DoneListContract.DoneEntry.TABLE_NAME);
    }
    
    private DoneListDbHelper mOpenHelper;
    
/*
    Students: Here is where you need to create the UriMatcher. This UriMatcher will
    match each URI to the DONES, DONES_WITH_TEAM, DONES_WITH_DATE, DONES_WITH_TEAM_AND_DATE,
    and TEAMS integer constants defined above.
*/
    static UriMatcher buildUriMatcher() {
        //// DONE: 18/02/16  
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.
        
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DoneListContract.CONTENT_AUTHORITY;
        
        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, DoneListContract.PATH_DONES, DONES);
        matcher.addURI(authority, DoneListContract.PATH_DONES + "/null/*", DONES_WITH_DATE);
        matcher.addURI(authority, DoneListContract.PATH_DONES + "/*", DONES_WITH_TEAM);
        matcher.addURI(authority, DoneListContract.PATH_DONES + "/*/*", DONES_WITH_TEAM_AND_DATE);
        
        return matcher;
    }
    
    private Cursor getDonesByTeam(Uri uri, String[] projection, String sortOrder) {
        String teamSetting = DoneListContract.DoneEntry.getTeamFromUri(uri);
        String startDate = DoneListContract.DoneEntry.getStartDateFromUri(uri);
        
        String[] selectionArgs;
        String selection;
        
        if (null == startDate || startDate.equals("")) {
            selection = sTeamSelection;
            selectionArgs = new String[]{teamSetting};
        } else {
            selectionArgs = new String[]{teamSetting, startDate};
            selection = sTeamWithStartDateSelection;
        }
        
        return sDonesByTeamQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
    }
    
    private Cursor getDonesByTeamAndDate(Uri uri, String[] projection, String sortOrder) {
        String TeamSetting = DoneListContract.DoneEntry.getTeamFromUri(uri);
        String date = DoneListContract.DoneEntry.getDateFromUri(uri);
        
        return sDonesByTeamQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sTeamAndDateSelection,
                new String[]{TeamSetting, date},
                null,
                null,
                sortOrder
        );
    }
    
    private Cursor getDonesByDate(Uri uri, String[] projection, String sortOrder) {
        String date = DoneListContract.DoneEntry.getDateFromUri(uri);
        
        return sDonesByTeamQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sDateSelection,
                new String[]{date},
                null,
                null,
                sortOrder
        );
    }
    
    private Cursor getAllDones(Uri uri, String[] projection, String sortOrder) {
        return sDonesByTeamQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );
    }
    
    @Override
    public boolean onCreate() {
        mOpenHelper = DoneListDbHelper.getInstance(getContext());
        return true;
    }
    
    
    @Nullable
    @Override
    public String getType(Uri uri) {
        // Use the Uri Matcher to determine what kind of URI this is.
        final int match = sUriMatcher.match(uri);
    
        switch (match) {
            // Student: Uncomment and fill out these two cases
            case DONES_WITH_TEAM_AND_DATE:
                return DoneListContract.DoneEntry.CONTENT_TYPE;
            case DONES_WITH_TEAM:
                return DoneListContract.DoneEntry.CONTENT_TYPE;
            case DONES_WITH_DATE:
                return DoneListContract.DoneEntry.CONTENT_TYPE;
            case DONES:
                return DoneListContract.DoneEntry.CONTENT_TYPE;
            case DONE_ITEM:
                return DoneListContract.DoneEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }
    
    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor;
        
        switch (sUriMatcher.match(uri)) {
            // "dones/*/*"
            case DONES_WITH_TEAM_AND_DATE:
            {
                retCursor = getDonesByTeamAndDate(uri, projection, sortOrder);
                break;
            }
            // "dones/*"
            case DONES_WITH_TEAM: {
                retCursor = getDonesByTeam(uri, projection, sortOrder);
                break;
            }
            // "dones/null/*"
            case DONES_WITH_DATE: {
                retCursor = getDonesByDate(uri, projection, sortOrder);
                break;
            }
            // "dones"
            case DONES: {
                //// DONE: 18/02/16 Return default list of dones 
                retCursor = getAllDones(uri, projection, sortOrder);
                break;
            }
            // "dones?id"
            case DONE_ITEM: {
                //// TODO: 18/02/16 Return list of teams
                retCursor = null;
                break;
            }
            
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        
        return retCursor;
    }
    
    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri returnUri;
    
        long _id = db.insert(DoneListContract.DoneEntry.TABLE_NAME, null, values);
        if ( _id > 0 )
            returnUri = DoneListContract.DoneEntry.buildDoneListUri(_id);
        else
            throw new android.database.SQLException("Failed to insert row into " + uri);
        
        try {
            getContext().getContentResolver().notifyChange(uri, null);
        } catch (NullPointerException e){
            Log.e("..", "No content resolver found in context");
        }
        
        return returnUri;
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //// DONE: 18/02/16
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        int rowsDeleted;
        // this makes delete all rows return the number of rows deleted
        
        //if ( null == selection ) selection = "1";
        
        rowsDeleted = db.delete(
                DoneListContract.DoneEntry.TABLE_NAME, selection, selectionArgs);
        
        // Because a null deletes all rows
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsDeleted;
    }
    
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        int rowsUpdated;
        
        //normalizeDate(values);
        rowsUpdated = db.update(DoneListContract.DoneEntry.TABLE_NAME, values, selection, selectionArgs);
        
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
    
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        int returnCount = 0;
        
        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                //// DONE: 19/02/16 Use insert or replace, instead of insert, so can retrieve updates only from server 
                //long _id = db.insert(DoneListContract.DoneEntry.TABLE_NAME, null, value);
                long _id = db.insertWithOnConflict(DoneListContract.DoneEntry.TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                if (_id != -1) {
                    returnCount++;
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        
        getContext().getContentResolver().notifyChange(uri, null);
        return returnCount;
    }
    
    @Override
    public void shutdown() {
        mOpenHelper.close();
        super.shutdown();
    }
}
