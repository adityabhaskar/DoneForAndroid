package net.c306.done;

/**
 * Created by raven on 18/02/16.
 * POJO to parse the incoming JSON dones into before entering into the database; 
 */
public class DoneItem {
    public int id;
    public String done_date;
    public String team_short_name;
    public String raw_text;
    public String created;
    public String updated;
    public String markedup_text;
    public String owner;
    public DoneTags[] tags;
    //public String likes;
    //public String comments;
    public DoneMeta meta_data;
    public String is_goal;
    public String goal_completed;
    public String url;
    public String team;
    public String permalink;
    public String is_local = "FALSE";
    
    public class DoneTags{
        public int id;
        public String name;
    }
    
    public class DoneMeta{
        public String from;
    }
}
