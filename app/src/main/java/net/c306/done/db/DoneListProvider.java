package net.c306.done.db;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

import net.c306.done.Utils;

public class DoneListProvider extends ContentProvider {
    
    static final int DONE_ITEM = 100;
    static final int DONES = 101;
    static final int DONES_WITH_TEAM = 102;
    static final int DONES_WITH_DATE = 103;
    static final int DONES_WITH_TEAM_AND_DATE = 104;
    static final int TEAMS = 300;
    
    // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();
    private static final SQLiteQueryBuilder sDonesByTeamQueryBuilder;
    //done_id = ?
    private static final String sDoneSelection =
            DoneListContract.DoneEntry.COLUMN_NAME_ID + " = ? ";
    //team_short_name = ?
    private static final String sTeamSelection =
            DoneListContract.DoneEntry.COLUMN_NAME_TEAM + " = ? ";
    //team_short_name = ? AND done_date >= ?
    private static final String sTeamWithStartDateSelection =
            DoneListContract.DoneEntry.COLUMN_NAME_TEAM + " = ? AND " +
                            DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " >= ? ";
    //team_short_name = ? AND done_date = ?
    private static final String sTeamAndDateSelection =
            DoneListContract.DoneEntry.COLUMN_NAME_TEAM + " = ? AND " +
                    DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " = ? ";
    //done_date = ?
    private static final String sDateSelection =
                    DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " = ? ";
    
    static{
        sDonesByTeamQueryBuilder = new SQLiteQueryBuilder();
        
        sDonesByTeamQueryBuilder.setTables(
                DoneListContract.DoneEntry.TABLE_NAME);
    
    
        //This is an inner join which looks like
        //dones INNER JOIN teams ON dones.team_id = teams.id
        //sDonesByTeamQueryBuilder.setTables(
        //        DoneListContract.DoneEntry.TABLE_NAME + " INNER JOIN " +
        //                DoneListContract.TeamEntry.TABLE_NAME +
        //                " ON " + DoneListContract.DoneEntry.TABLE_NAME +
        //                "." + DoneListContract.DoneEntry.COLUMN_NAME_TEAM +
        //                " = " + DoneListContract.TeamEntry.TABLE_NAME +
        //                "." + DoneListContract.TeamEntry.COLUMN_NAME_URL);
    }
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private DoneListDbHelper mOpenHelper;
    
    /*
    Students: Here is where you need to create the UriMatcher. This UriMatcher will
    match each URI to the DONES, DONES_WITH_TEAM, DONES_WITH_DATE, DONES_WITH_TEAM_AND_DATE,
    and TEAMS integer constants defined above.
*/
    static UriMatcher buildUriMatcher() {
        // I know what you're thinking.  Why create a UriMatcher when you can use regular
        // expressions instead?  Because you're not crazy, that's why.
        
        // All paths added to the UriMatcher have a corresponding code to return when a match is
        // found.  The code passed into the constructor represents the code to return for the root
        // URI.  It's common to use NO_MATCH as the code for this case.
        
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = DoneListContract.CONTENT_AUTHORITY;
        
        // For each type of URI you want to add, create a corresponding code.
        matcher.addURI(authority, DoneListContract.PATH_DONES, DONES);
        matcher.addURI(authority, DoneListContract.PATH_TEAMS, TEAMS);
        matcher.addURI(authority, DoneListContract.PATH_DONES + "/#", DONE_ITEM);
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
    
    private Cursor getThisDone(Uri uri, String[] projection, String sortOrder) {
        String id = DoneListContract.DoneEntry.getDoneIDFromUri(uri);
        
        return sDonesByTeamQueryBuilder.query(mOpenHelper.getReadableDatabase(),
                projection,
                sDoneSelection,
                new String[]{id},
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
    
        switch (sUriMatcher.match(uri)) {
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
            case TEAMS:
                return DoneListContract.DoneEntry.CONTENT_TYPE;
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
                // Return default list of dones 
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DoneListContract.DoneEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder,
                        null
                );
                break;
            }

            // "dones?id"
            case DONE_ITEM: {
                // Return one done item
                retCursor = getThisDone(uri, projection, sortOrder);
                break;
            }
    
            case TEAMS: {
                retCursor = mOpenHelper.getReadableDatabase().query(
                        DoneListContract.TeamEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder,
                        null
                );
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
    
        switch (sUriMatcher.match(uri)) {
        
            // "dones/*/*"
            case DONES: {
                long _id = db.insert(DoneListContract.DoneEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = DoneListContract.DoneEntry.buildDoneListWithIdUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
        
            case TEAMS: {
                long _id = db.insert(DoneListContract.TeamEntry.TABLE_NAME, null, values);
                if (_id > 0)
                    returnUri = DoneListContract.TeamEntry.buildTeamUri(_id);
                else
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                break;
            }
        
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    
        getContext().getContentResolver().notifyChange(uri, null);
        
        return returnUri;
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        //// DONE: 18/02/16
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        int rowsDeleted;
    
        // this makes delete all rows return the number of rows deleted
        if (null == selection) selection = "1";
    
        switch (sUriMatcher.match(uri)) {
        
            // "dones/*/*"
            case DONES:
                rowsDeleted = db.delete(
                        DoneListContract.DoneEntry.TABLE_NAME, selection, selectionArgs);
                break;
        
            case TEAMS:
                rowsDeleted = db.delete(
                        DoneListContract.TeamEntry.TABLE_NAME, selection, selectionArgs);
                break;
        
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    
        
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
    
        switch (sUriMatcher.match(uri)) {
        
            case DONES:
                rowsUpdated = db.update(
                        DoneListContract.DoneEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
        
            case TEAMS:
                rowsUpdated = db.update(
                        DoneListContract.TeamEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
        
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return rowsUpdated;
    }
    
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        
        int returnCount = 0;
    
        String tableName = null;
    
        switch (sUriMatcher.match(uri)) {
        
            case DONES:
                tableName = DoneListContract.DoneEntry.TABLE_NAME;
                break;
        
            case TEAMS:
                tableName = DoneListContract.TeamEntry.TABLE_NAME;
                break;
        
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        
        
        db.beginTransaction();
        try {
            for (ContentValues value : values) {
                //Use insert or replace, instead of insert, so can retrieve updates from server 
                //long _id = db.insert(DoneListContract.DoneEntry.TABLE_NAME, null, value);
                long _id = db.insertWithOnConflict(
                        tableName, null, value, SQLiteDatabase.CONFLICT_REPLACE);
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
