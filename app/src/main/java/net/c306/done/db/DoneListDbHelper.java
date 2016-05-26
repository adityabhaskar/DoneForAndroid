package net.c306.done.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.c306.done.Utils;
import net.c306.done.sync.IDTSyncAdapter;

public class DoneListDbHelper extends SQLiteOpenHelper {
    
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 15;
    public static final String DATABASE_NAME = "DoneList.db";
    private static DoneListDbHelper sInstance;
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private Context mContext = null;
    
    private DoneListDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }
    
    public static synchronized DoneListDbHelper getInstance(Context context) {
        // Use the application context, which will ensure that you 
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DoneListDbHelper(context.getApplicationContext());
        }
    
        return sInstance;
    }
    
    public void onCreate(SQLiteDatabase db) {
        createTeamsTable(db);
        createTagsTable(db);
        createTasksTable(db);
    }
    
    private void createTasksTable(SQLiteDatabase db) {
        // Create dones table 
        final String SQL_CREATE_TABLE_DONES = "CREATE TABLE " + DoneListContract.DoneEntry.TABLE_NAME +
                "(" +
                // Main Fields
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " INTEGER NOT NULL UNIQUE PRIMARY KEY, " +
                DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " INTEGER, " +
                DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT + " TEXT NOT NULL, " +
                DoneListContract.DoneEntry.COLUMN_NAME_TEAM + " TEXT NOT NULL, " +
                // Action Manager fields
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " TEXT NOT NULL DEFAULT 'FALSE', " +
                DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " TEXT NOT NULL DEFAULT 'FALSE', " +
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " TEXT, " +
                // Other Fields
                DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_CREATED + " INTEGER, " +
                DoneListContract.DoneEntry.COLUMN_NAME_UPDATED + " INTEGER, " +
                DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_OWNER + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_TAGS + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_LIKES + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_META_DATA + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_IS_GOAL + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_GOAL_COMPLETED + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_URL + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_PERMALINK + " TEXT " +
                // Team reference fields
                // Set up the dones.team column as a foreign key to teams.url.
                //" FOREIGN KEY (" + DoneListContract.DoneEntry.COLUMN_NAME_TEAM + ") REFERENCES " +
                //DoneListContract.TeamEntry.TABLE_NAME + " (" + DoneListContract.TeamEntry.COLUMN_NAME_URL + ") " +
                ")";
    
        db.execSQL(SQL_CREATE_TABLE_DONES);
    }
    
    private void createTagsTable(SQLiteDatabase db) {
        // Create tags table 
        final String SQL_CREATE_TABLE_TAGS = "CREATE TABLE " + DoneListContract.TagEntry.TABLE_NAME +
                "(" +
                // Main Fields
                DoneListContract.TagEntry.COLUMN_NAME_ID + " INTEGER NOT NULL UNIQUE PRIMARY KEY, " +
                DoneListContract.TagEntry.COLUMN_NAME_NAME + " TEXT NOT NULL, " +
                DoneListContract.TagEntry.COLUMN_NAME_TEAM + " TEXT NOT NULL " +
                ")";
        
        db.execSQL(SQL_CREATE_TABLE_TAGS);
    }
    
    private void createTeamsTable(SQLiteDatabase db) {
        // Create teams table 
        final String SQL_CREATE_TABLE_TEAMS = "CREATE TABLE " + DoneListContract.TeamEntry.TABLE_NAME +
                "(" +
                // Main Fields
                DoneListContract.TeamEntry.COLUMN_NAME_ID + " INTEGER NOT NULL UNIQUE PRIMARY KEY, " +
                DoneListContract.TeamEntry.COLUMN_NAME_URL + " TEXT NOT NULL UNIQUE, " +
                DoneListContract.TeamEntry.COLUMN_NAME_NAME + " TEXT NOT NULL, " +
                DoneListContract.TeamEntry.COLUMN_NAME_SHORT_NAME + " TEXT NOT NULL, " +
                DoneListContract.TeamEntry.COLUMN_NAME_DONES + " TEXT, " +
                DoneListContract.TeamEntry.COLUMN_NAME_IS_PERSONAL + " TEXT NOT NULL DEFAULT 'FALSE', " +
                DoneListContract.TeamEntry.COLUMN_NAME_DONE_COUNT + " INTEGER, " +
                DoneListContract.TeamEntry.COLUMN_NAME_PERMALINK + " TEXT " +
                ")";
        
        db.execSQL(SQL_CREATE_TABLE_TEAMS);
    }
    
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
    }
    
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
/*
        //// DONE: 16/02/16 
        switch(oldVersion) {
            case 1:
                //upgrade logic from version 1 to 2
            case 2:
                //upgrade logic from version 2 to 3
            case 3:
                //upgrade logic from version 3 to 4
                break;
            default:
                throw new IllegalStateException(
                        "onUpgrade() with unknown oldVersion " + oldVersion);
        }
        db.execSQL("ALTER TABLE" + DoneListContract.DoneEntry.TABLE_NAME + " ADD Column " + "COL_D INTEGER DEFAULT 0");
        db.execSQL("DROP TABLE IF EXISTS " + DoneListContract.DoneEntry.TABLE_NAME);
        onCreate(db);
*/
        if (oldVersion == 14 && newVersion == 15) {
            db.execSQL("ALTER TABLE " + DoneListContract.TagEntry.TABLE_NAME + " ADD COLUMN " + DoneListContract.TagEntry.COLUMN_NAME_TEAM + " TEXT NOT NULL DEFAULT ''");
        } else {
            db.execSQL("DROP TABLE IF EXISTS " + DoneListContract.TeamEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DoneListContract.TagEntry.TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + DoneListContract.DoneEntry.TABLE_NAME);
            onCreate(db);
        }
    
        // Re-fetch data on table upgrade - old data just got cleared :(  
        if (mContext != null)
            IDTSyncAdapter.syncImmediately(mContext.getApplicationContext(), true, false);
        else
            Log.e(LOG_TAG, "onUpgrade: No Context available");
    }
    
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    
}
