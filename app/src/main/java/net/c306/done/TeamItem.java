package net.c306.done;

/**
 * Created by raven on 18/02/16.
 * POJO to parse the incoming JSON dones into before entering into the database;
 */
public class TeamItem {
    public String url;
    public String name; // unique
    public String short_name;
    public String dones;
    public boolean is_personal;
    public String permalink;
}
