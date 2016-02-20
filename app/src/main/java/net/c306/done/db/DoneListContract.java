package net.c306.done.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by raven on 13/02/16.
 */
public class DoneListContract {
    
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "net.c306.done.app";
    
    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    
    // Possible paths (appended to base content URI for possible URI's)
    // For instance, content://com.example.android.sunshine.app/weather/ is a valid path for
    // looking at weather data. content://com.example.android.sunshine.app/givemeroot/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "givemeroot".
    // At least, let's hope not.  Don't be that dev, reader.  Don't be that dev.
    public static final String PATH_DONES = "dones";
    
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DoneListContract() {}
    
    /* Inner class that defines the table contents */
    public static abstract class DoneEntry implements BaseColumns {
        
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_DONES).build();
        
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DONES;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DONES;
        
        public static final String TABLE_NAME = "dones";
        
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_CREATED = "created";
        public static final String COLUMN_NAME_UPDATED = "updated";
        public static final String COLUMN_NAME_MARKEDUP_TEXT = "markedup_text";
        public static final String COLUMN_NAME_DONE_DATE = "done_date";
        public static final String COLUMN_NAME_OWNER = "owner";
        public static final String COLUMN_NAME_TEAM_SHORT_NAME = "team_short_name";
        public static final String COLUMN_NAME_TAGS = "tags";
        public static final String COLUMN_NAME_LIKES = "likes";
        public static final String COLUMN_NAME_COMMENTS = "comments";
        public static final String COLUMN_NAME_META_DATA = "meta_data";
        public static final String COLUMN_NAME_IS_GOAL = "is_goal";
        public static final String COLUMN_NAME_GOAL_COMPLETED = "goal_completed";
        public static final String COLUMN_NAME_URL = "url";
        public static final String COLUMN_NAME_TEAM = "team";
        public static final String COLUMN_NAME_RAW_TEXT = "raw_text";
        public static final String COLUMN_NAME_PERMALINK = "permalink";
        public static final String COLUMN_NAME_IS_LOCAL = "is_local";
        
        // .../dones/1234
        // Returns individual done
        public static Uri buildDoneListWithIdUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    
    
        public static Uri buildDoneListUri() {
            return CONTENT_URI;
        }
    
    
        public static Uri buildDoneListWithTeam(String team, long startDate) {
            return CONTENT_URI.buildUpon().appendPath(team)
                    .appendQueryParameter(COLUMN_NAME_DONE_DATE, Long.toString(startDate)).build();
        }
    
        public static Uri buildDoneListWithTeamAndDate(String team, long date) {
            return CONTENT_URI.buildUpon().appendPath(team)
                    .appendPath(Long.toString(date)).build();
        }
        
        public static Uri buildDoneListWithDate(long date) {
            return CONTENT_URI.buildUpon().appendPath("null")
                    .appendPath(Long.toString(date)).build();
        }
    
    
        public static String getStartDateFromUri(Uri uri) {
            return uri.getQueryParameter(COLUMN_NAME_DONE_DATE);
        }
        
        public static String getTeamFromUri(Uri uri) {
            return uri.getPathSegments().get(1);
        }
        
        public static String getDateFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }
}
