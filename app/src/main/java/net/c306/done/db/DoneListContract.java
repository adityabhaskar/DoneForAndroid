package net.c306.done.db;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

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
    public static final String PATH_TEAMS = "teams";
    
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DoneListContract() {}
    
    public static abstract class TeamEntry implements BaseColumns {
        public static final String TABLE_NAME = "teams";
    
        public static final String COLUMN_NAME_ID = "teams_id";   // assign locally
        public static final String COLUMN_NAME_URL = "teams_team_url"; // unique on server - api url - same as dones.team
        public static final String COLUMN_NAME_NAME = "teams_team_name";
        public static final String COLUMN_NAME_SHORT_NAME = "teams_short_name";
        public static final String COLUMN_NAME_DONES = "teams_team_dones"; // api url to dones in team
        public static final String COLUMN_NAME_IS_PERSONAL = "teams_is_personal";
        public static final String COLUMN_NAME_DONE_COUNT = "teams_team_done_count"; // total done count in team, on server
        public static final String COLUMN_NAME_PERMALINK = "teams_team_permalink"; // url to web page
        
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_TEAMS).build();
        
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TEAMS;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_TEAMS;
    
        public static Uri buildTeamUri(long id) {
            return ContentUris.withAppendedId(CONTENT_URI, id);
        }
    }
    
    /* Inner class that defines the table contents */
    public static abstract class DoneEntry implements BaseColumns {
        
        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_DONES).build();
        
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DONES;
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_DONES;
        
        public static final String TABLE_NAME = "dones";
    
        public static final String COLUMN_NAME_ID = "dones_id";
        public static final String COLUMN_NAME_CREATED = "dones_created";
        public static final String COLUMN_NAME_UPDATED = "dones_updated";
        public static final String COLUMN_NAME_MARKEDUP_TEXT = "dones_markedup_text";
        public static final String COLUMN_NAME_DONE_DATE = "dones_done_date";
        public static final String COLUMN_NAME_OWNER = "dones_owner";
        public static final String COLUMN_NAME_TEAM_SHORT_NAME = "dones_team_short_name";
        public static final String COLUMN_NAME_TAGS = "dones_tags";
        public static final String COLUMN_NAME_LIKES = "dones_likes";
        public static final String COLUMN_NAME_COMMENTS = "dones_comments";
        public static final String COLUMN_NAME_META_DATA = "dones_meta_data";
        public static final String COLUMN_NAME_IS_GOAL = "dones_is_goal";
        public static final String COLUMN_NAME_GOAL_COMPLETED = "dones_goal_completed";
        public static final String COLUMN_NAME_URL = "dones_url";
        public static final String COLUMN_NAME_TEAM = "dones_team"; // same as teams.url
        public static final String COLUMN_NAME_RAW_TEXT = "dones_raw_text";
        public static final String COLUMN_NAME_PERMALINK = "dones_permalink";
        public static final String COLUMN_NAME_IS_LOCAL = "dones_is_local";
        public static final String COLUMN_NAME_IS_DELETED = "dones_is_deleted";
        public static final String COLUMN_NAME_EDITED_FIELDS = "dones_edited_fields";
        
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
    
        public static String getDoneIDFromUri(Uri uri) {
            return uri.getPathSegments().get(2);
        }
    }
}
