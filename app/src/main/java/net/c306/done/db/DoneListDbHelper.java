package net.c306.done.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.c306.done.DoneItem;
import net.c306.done.R;

/**
 * Created by raven on 13/02/16.
 */
public class DoneListDbHelper extends SQLiteOpenHelper {
    
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "DoneList.db";
    private static DoneListDbHelper sInstance;
    private static String LOG_TAG;
    String SQL_CREATE_ENTRIES;
    
    private DoneListDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    public static synchronized DoneListDbHelper getInstance(Context context) {
        // Use the application context, which will ensure that you 
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (sInstance == null) {
            sInstance = new DoneListDbHelper(context.getApplicationContext());
        }
        LOG_TAG = context.getString(R.string.APP_LOG_IDENTIFIER) + " " + DoneListDbHelper.class.getSimpleName();
        return sInstance;
    }
    
    public void onCreate(SQLiteDatabase db) {
        //// DONE: 18/02/16 Create query for dones table creation 
        SQL_CREATE_ENTRIES = "CREATE TABLE " + DoneListContract.DoneEntry.TABLE_NAME +
            "(" +
                // Main Fields
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " INTEGER NOT NULL UNIQUE PRIMARY KEY, " +
                DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " INTEGER, " +
                DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME + " TEXT NOT NULL, " +
                DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT + " TEXT NOT NULL, " +
                // Action Manager fields
                DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " TEXT NOT NULL DEFAULT 'FALSE', " +
                DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " TEXT NOT NULL DEFAULT 'FALSE', " +
                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " TEXT, " +
                // Other Fields
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
                DoneListContract.DoneEntry.COLUMN_NAME_TEAM + " TEXT, " +
                DoneListContract.DoneEntry.COLUMN_NAME_PERMALINK + " TEXT " +
            ")";
        db.execSQL(SQL_CREATE_ENTRIES);
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
        db.execSQL("DROP TABLE IF EXISTS " + DoneListContract.DoneEntry.TABLE_NAME);
        onCreate(db);
    }
    
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    
    // Insert a post into the database
    // Src: https://guides.codepath.com/android/Local-Databases-with-SQLiteOpenHelper
    public void addDone(DoneItem done) {
        // Create and/or open the database for writing
        SQLiteDatabase db = getWritableDatabase();
        
        // It's a good idea to wrap our insert in a transaction. This helps with performance and ensures
        // consistency of the database.
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(DoneListContract.DoneEntry.COLUMN_NAME_ID, done.id);
            values.put(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, done.raw_text);
            
            // Notice how we haven't specified the primary key. SQLite auto increments the primary key column.
            db.insertOrThrow(DoneListContract.DoneEntry.TABLE_NAME, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error while trying to add post to database");
        } finally {
            db.endTransaction();
        }
    }
    
}
