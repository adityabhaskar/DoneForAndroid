package net.c306.done;

import android.provider.BaseColumns;

/**
 * Created by raven on 13/02/16.
 */
public class DoneListContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DoneListContract() {}
    
    /* Inner class that defines the table contents */
    public static abstract class DoneEntry implements BaseColumns {
        public static final String TABLE_NAME = "doneList";
        public static final String COLUMN_NAME_ENTRY_ID = "entryid";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_NULLABLE = "nullable";
    
        public static final String ID = "id";
        public static final String COMMENTS = "comments";
        public static final String CREATED = "created";
        public static final String DONE_DATE = "done_date";
        public static final String GOAL_COMPLETED = "goal_completed";
        public static final String IS_GOAL = "is_goal";
        public static final String LIKES = "likes";
        public static final String MARKEDUP_TEXT = "markedup_text";
        public static final String META_DATA = "meta_data";
        public static final String OWNER = "owner";
        public static final String PERMALINK = "permalink";
        public static final String RAW_TEXT = "raw_text";
        public static final String TAGS = "tags";
        public static final String TEAM = "team";
        public static final String TEAM_SHORT_NAME = "team_short_name";
        public static final String UPDATED = "updated";
        public static final String URL = "url";
//        comments: Array[0]
//        created: "2016-02-13T14:57:57.268"
//        done_date: "2016-02-13"
//        goal_completed: true
//        id: 25602280
//        is_goal: false
//        likes: Array[0]
//        markedup_text: "Added 2FA to github"
//        meta_data: Object
//        owner: "adityabhaskar"
//        permalink: "https://idonethis.com/done/25602280/"
//        raw_text: "Added 2FA to github"
//        tags: Array[0]
//        team: "https://idonethis.com/api/v0.1/teams/adityabhaskar/"
//        team_short_name: "adityabhaskar"
//        updated: "2016-02-13T14:57:57.268"
//        url: "https://idonethis.com/api/v0.1/dones/25602280/"
    }
}
