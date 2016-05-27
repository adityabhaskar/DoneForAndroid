package net.c306.done.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import com.google.gson.Gson;

import net.c306.done.R;
import net.c306.done.Utils;
import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.DeleteDonesTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Vector;


public class IDTSyncAdapter extends AbstractThreadedSyncAdapter {
    
    private static final String LOG_TAG = Utils.LOG_TAG + IDTSyncAdapter.class.getSimpleName();
    
    public IDTSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }
    
    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     * @param fetchTeams Flag indicating whether or not to fetch teams - true only when coming from idt AsyncTasks, or database.onUpgrade
     * @param fromDoneActions Flag indicating if called from DoneActions - don't call update in that case.
     * @param fromNotificationAlarm Flag indicating if called from NotificationAlarmReceiver - if true, show notification after sync
     */
    public static void syncImmediately(Context context, boolean fetchTeams, boolean fromDoneActions, boolean fromNotificationAlarm) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        bundle.putBoolean(Utils.INTENT_EXTRA_FROM_DONE_DELETE_EDIT_TASKS, fetchTeams);
        bundle.putBoolean(Utils.INTENT_EXTRA_FROM_DONE_ACTIONS, fromDoneActions);
        bundle.putBoolean(Utils.INTENT_EXTRA_FROM_NOTIFICATION_ALARM, fromNotificationAlarm);
        
        context = context.getApplicationContext();
        
        Account syncAccount = IDTAccountManager.getSyncAccount(context);
        
        if (syncAccount != null)
            ContentResolver.requestSync(syncAccount, context.getString(R.string.content_authority), bundle);
        else
            Log.w(LOG_TAG, "No sync account found!");
    }
    
    public static void syncImmediately(Context context) {
        syncImmediately(context.getApplicationContext(), false, false, false);
    }
    
    public static void onAccountCreated(Account newAccount, Context context) {
        Log.v(LOG_TAG, "In onAccountCreated...");
        /*
         * Since we've created an account, configure & initialize sync
         */
        IDTSyncAdapter.configurePeriodicSync(context, Utils.getSyncInterval(context));
        
        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context.getApplicationContext(), false, false, false);
    }
    
    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval) {
        context = context.getApplicationContext();
        String authority = context.getString(R.string.content_authority);
        Account account = IDTAccountManager.getSyncAccount(context);
        
        if (account == null) {
            Log.e(LOG_TAG, "No account found to configure periodic sync");
            return;
        }
        
        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setIsSyncable(account, authority, Utils.SYNC_SYNCABLE);
        ContentResolver.setSyncAutomatically(account, authority, true);
    
        ContentResolver.addPeriodicSync(account, authority, new Bundle(), syncInterval);
    }
    
    public static void stopPeriodicSync(Context context) {
        context = context.getApplicationContext();
        Account account = IDTAccountManager.getSyncAccount(context);
        
        if (account == null) {
            Log.e(LOG_TAG, "No account found to remove periodic sync");
            return;
        }
        
        String authority = context.getString(R.string.content_authority);
        
        ContentResolver.removePeriodicSync(
                account,
                authority,
                new Bundle()
        );
        ContentResolver.setSyncAutomatically(account, authority, false);
    }
    
    public static void initializeSyncAdapter(Context context) {
        Log.v(LOG_TAG, "Initialising sync adapter");
        context = context.getApplicationContext();
        
        Account account = IDTAccountManager.getSyncAccount(context);
    
        if (account != null)
            IDTSyncAdapter.configurePeriodicSync(context, Utils.getSyncInterval(context));
        else
            Log.w(LOG_TAG, "No sync account found");
    }
    
    /**
     * Save parsed #tags to database in table tags
     *
     * @param context
     * @param tagsList       An ArrayList of DoneItem.DoneTags
     * @param deleteExisting
     * @return number of tags saved to database
     */
    public static int saveTagsToDatabase(Context context, List<DoneItem.DoneTags> tagsList, boolean deleteExisting) {
        // TODO: 23/05/16 Add team field 
        int tagCount = tagsList.size();
        Vector<ContentValues> cVVector = new Vector<>(tagCount);
        
        for (DoneItem.DoneTags tag : tagsList) {
            ContentValues tagValues = new ContentValues();
            
            tagValues.put(DoneListContract.TagEntry.COLUMN_NAME_ID, tag.id);
            tagValues.put(DoneListContract.TagEntry.COLUMN_NAME_NAME, tag.name);
            tagValues.put(DoneListContract.TagEntry.COLUMN_NAME_TEAM, tag.team);
            cVVector.add(tagValues);
            
        }
        
        Log.v(LOG_TAG, "Tags found " + cVVector.size());
        
        // add tags to database
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            
            // Delete previous tags from database 
            // There could be some local non-posted dones that were typed while sync was on
            if (deleteExisting)
                context.getContentResolver().delete(
                        DoneListContract.TagEntry.CONTENT_URI, // Table uri
                        null,
                        null); // Selection args
            
            // Add newly fetched entries to the server
            return context.getContentResolver().bulkInsert(DoneListContract.TagEntry.CONTENT_URI, cvArray);
        } else
            return 0;
    }
    
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.v(LOG_TAG, "onPerformSync Called.");
    
        Context context = getContext().getApplicationContext();
    
        String authToken = IDTAccountManager.getAuthToken(context);
        
        if (authToken == null)
            return;
    
        if (Utils.isTokenExpired(context)) {
            // Launch refresh token task
            new RefreshTokenTask(context).execute();
            return;
        }
    
        boolean fromDoneDeleteEditTasks = extras.getBoolean(Utils.INTENT_EXTRA_FROM_DONE_DELETE_EDIT_TASKS, false);
        boolean fromDoneActions = extras.getBoolean(Utils.INTENT_EXTRA_FROM_DONE_ACTIONS, false);
        int teamCount = 0;
    
        if (fromDoneActions) {
        
            // Check for unsent task actions - add, edit, delete
            Cursor cursor = context.getContentResolver().query(
                    DoneListContract.DoneEntry.buildDoneListUri(),                  // URI
                    new String[]{DoneListContract.DoneEntry.COLUMN_NAME_ID},        // Projection
                    DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'TRUE' OR " + // Selection
                            DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'TRUE' OR " +
                            DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " IS NOT NULL",
                    null, // Selection Args
                    null  // Sort Order
            );
        
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    Log.v(LOG_TAG, "Found " + cursor.getCount() + " unsent tasks, post them before fetching");
    
                    new DeleteDonesTask(context, true).execute();
                }
                cursor.close();
            }
        
            // No other actions (sync team/tasks) required, so return
        
        } else {
        
            // Check to prevent cyclicity
            if (!fromDoneDeleteEditTasks) {
            
                // 1. Refresh team list
                teamCount = fetchTeams(authToken);
            
                // 2. Check for unsent task actions - add, edit, delete
                Cursor cursor = context.getContentResolver().query(
                        DoneListContract.DoneEntry.buildDoneListUri(),                  // URI
                        new String[]{DoneListContract.DoneEntry.COLUMN_NAME_ID},        // Projection
                        DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'TRUE' OR " + // Selection
                                DoneListContract.DoneEntry.COLUMN_NAME_IS_DELETED + " IS 'TRUE' OR " +
                                DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS + " IS NOT NULL",
                        null, // Selection Args
                        null  // Sort Order
                );
            
                if (cursor != null) {
                    if (cursor.getCount() > 0) {
                        Log.v(LOG_TAG, "Found " + cursor.getCount() + " unsent tasks, post them before fetching");
                    
                        new DeleteDonesTask(context, false).execute();
                    
                        cursor.close();
                        return;
                    }
                }
            }
        
            // 3. Refresh task list
            if (teamCount > -1)
                fetchTasks(authToken);
        
            if (extras.containsKey(Utils.INTENT_EXTRA_FROM_NOTIFICATION_ALARM) && extras.getBoolean(Utils.INTENT_EXTRA_FROM_NOTIFICATION_ALARM, false)) {
                Utils.showNotification(context);
            }
        }
    }
    
    private int fetchTasks(String authToken) {
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        Context context = getContext().getApplicationContext();
        
        Utils.sendMessage(context, Utils.SENDER_FETCH_TASKS, "Starting fetch... ", Utils.STATUS_TASK_STARTED, -1);
        
        int resultStatus;
        int fetchedTaskCount = -1;
        
        // Contains server response (or error message)
        String result = "";
    
        String fetchUrl = Utils.IDT_DONE_URL + "?page_size=" + Utils.getFetchValue(context, Utils.PREF_FETCH_BY_COUNT);
    
        if (Utils.getFetchType(context).equals(Utils.PREF_FETCH_BY_DAYS)) {
            int daysToFetch = Integer.parseInt(Utils.getFetchValue(context, Utils.PREF_FETCH_BY_DAYS));
        
            if (daysToFetch < 999) {
            
                String dateSince;
            
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, -daysToFetch);
            
                SimpleDateFormat idtDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
                dateSince = idtDateFormat.format(calendar.getTime());
            
                fetchUrl += "&done_date_after=" + dateSince;
            }
        }
    
        //Log.i(LOG_TAG, "fetchTasks: url:\n" + fetchUrl);
        
        try {
            //Connect
            httpcon = (HttpURLConnection) new URL(fetchUrl).openConnection();
            httpcon.setRequestProperty("Authorization", "Bearer " + authToken);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("GET");
            httpcon.connect();
            
            //Response Code
            resultStatus = httpcon.getResponseCode();
            String responseMessage = httpcon.getResponseMessage();
            
            switch (resultStatus) {
                case HttpURLConnection.HTTP_ACCEPTED:
                case HttpURLConnection.HTTP_CREATED:
                case HttpURLConnection.HTTP_OK:
                    Log.v(LOG_TAG, "Got Done List - " + resultStatus + ": " + responseMessage);
                    break; // fine
                
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG, "Authcode invalid - " + resultStatus + ": " + responseMessage);
                    // Set token invalid
                    Utils.setTokenValidity(context, false);
                    Utils.sendMessage(context, Utils.SENDER_FETCH_TASKS, responseMessage, Utils.STATUS_TASK_UNAUTH, -1);
    
                default:
                    Log.w(LOG_TAG, "Couldn't fetch dones" + resultStatus + ": " + responseMessage);
                    Utils.sendMessage(context, Utils.SENDER_FETCH_TASKS, responseMessage, Utils.STATUS_TASK_OTHER_ERROR, -1);
            }
            
            //Read      
            br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
            
            String line;
            StringBuilder sb = new StringBuilder();
            
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            result = sb.toString();
            
            if (result.equals("")) {
                
                result = null;
    
            } else if (resultStatus == HttpURLConnection.HTTP_OK)
                fetchedTaskCount = getTaskListFromJson(result);
    
    
        } catch (Exception e) {
            result = null;
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            Utils.sendMessage(context, Utils.SENDER_FETCH_TASKS, e.getMessage(), Utils.STATUS_TASK_OTHER_ERROR, -1);
        } finally {
            if (httpcon != null) {
                httpcon.disconnect();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        
        if (fetchedTaskCount == -1)
            Log.v(LOG_TAG, "Message from server: " + result);
        
        Utils.sendMessage(context, Utils.SENDER_FETCH_TASKS, "Fetch Successful.", Utils.STATUS_TASK_SUCCESSFUL, fetchedTaskCount);
        return fetchedTaskCount;
    }
    
    private int fetchTeams(String authToken) {
        HttpURLConnection httpcon = null;
        BufferedReader br = null;
        int resultStatus;
        String result = "";
        int teamCount = 0;
        Context context = getContext().getApplicationContext();
        
        Utils.sendMessage(context, Utils.SENDER_FETCH_TEAMS, "Starting fetch... ", Utils.STATUS_TASK_STARTED, -1);
        
        try {
            //Connect
            httpcon = (HttpURLConnection) ((new URL(Utils.IDT_TEAM_URL + "?page_size=100").openConnection()));
            httpcon.setRequestProperty("Authorization", "Bearer " + authToken);
            httpcon.setRequestProperty("Content-Type", "application/json");
            httpcon.setRequestProperty("Accept", "application/json");
            httpcon.setRequestMethod("GET");
            httpcon.connect();
            
            //Response Code
            resultStatus = httpcon.getResponseCode();
            String responseMessage = httpcon.getResponseMessage();
            
            switch (resultStatus) {
                case HttpURLConnection.HTTP_OK:
                    Log.v(LOG_TAG, "Got teams - " + resultStatus + ": " + responseMessage);
                    
                    //Read      
                    br = new BufferedReader(new InputStreamReader(httpcon.getInputStream(), "UTF-8"));
                    
                    String line;
                    StringBuilder sb = new StringBuilder();
                    
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    
                    result = sb.toString();
                    
                    httpcon.disconnect();
                    br.close();
                    
                    if (result.equals("")) {
                        
                        result = null;
                        
                    } else {
                        
                        teamCount = getTeamsFromJson(result);
                        
                    }
                    break; // fine
                
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    Log.w(LOG_TAG, " Authcode invalid - " + resultStatus + ": " + responseMessage);
                    // Set invalid token
                    Utils.setTokenValidity(context, false);
                    Utils.sendMessage(context, Utils.SENDER_FETCH_TEAMS, responseMessage, Utils.STATUS_TASK_UNAUTH, -1);
                    teamCount = -1;
                
                default:
                    Log.w(LOG_TAG, "Couldn't fetch teams - " + resultStatus + ": " + responseMessage);
                    Utils.sendMessage(context, Utils.SENDER_FETCH_TEAMS, responseMessage, Utils.STATUS_TASK_OTHER_ERROR, -1);
                    teamCount = -1;
            }
            
        } catch (Exception e) {
            
            result = null;
            e.printStackTrace();
            Log.e(LOG_TAG, e.getMessage());
            Utils.sendMessage(context, Utils.SENDER_FETCH_TEAMS, e.getMessage(), Utils.STATUS_TASK_OTHER_ERROR, -1);
            
        } finally {
            
            if (httpcon != null) {
                httpcon.disconnect();
            }
            if (br != null) {
                try {
                    br.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
            
        }
        
        if (teamCount == 0)
            Log.v(LOG_TAG, "Message from server: " + result);
        
        return teamCount;
    }
    
    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private int getTaskListFromJson(String forecastJsonStr) throws JSONException {
        
        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.
        
        // These are the names of the JSON objects that need to be extracted.
        
        Gson gson = new Gson();
        String doneItemString;
        DoneItem doneItem;
    
        List<DoneItem.DoneTags> allTagsArray = new ArrayList<>();
        List<String> taskTagsArray = new ArrayList<>();
        
        JSONObject masterObj = new JSONObject(forecastJsonStr);
        JSONArray donesListArray = masterObj.getJSONArray("results");
        
        Vector<ContentValues> cVVector = new Vector<>(donesListArray.length());
        
        for (int i = 0; i < donesListArray.length(); i++) {
            doneItemString = donesListArray.getJSONObject(i).toString();
            
            doneItem = gson.fromJson(doneItemString, DoneItem.class);
            
            ContentValues doneItemValues = new ContentValues();
            
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_ID, doneItem.id);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_CREATED, doneItem.created);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_UPDATED, doneItem.updated);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT, doneItem.markedup_text);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE, doneItem.done_date);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_OWNER, doneItem.owner);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME, doneItem.team_short_name);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_LIKES, "");
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS, "");
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_META_DATA, gson.toJson(doneItem.meta_data, DoneItem.DoneMeta.class));
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_IS_GOAL, doneItem.is_goal);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_GOAL_COMPLETED, doneItem.goal_completed);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_URL, doneItem.url);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TEAM, doneItem.team);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT, Html.fromHtml(doneItem.raw_text).toString());
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_PERMALINK, doneItem.permalink);
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL, doneItem.is_local);
    
            taskTagsArray.clear();
            for (int j = 0; j < doneItem.tags.length; j++) {
                taskTagsArray.add(DoneListContract.TagEntry.TAG_ID_PRE + doneItem.tags[j].id + DoneListContract.TagEntry.TAG_ID_POST);
                doneItem.tags[j].team = doneItem.team;
            }
            allTagsArray.addAll(Arrays.asList(doneItem.tags));
            doneItemValues.put(DoneListContract.DoneEntry.COLUMN_NAME_TAGS, gson.toJson(taskTagsArray.toArray(), String[].class));
            
            cVVector.add(doneItemValues);
        }
        
        Log.v(LOG_TAG, "Fetched " + cVVector.size() + " tasks");
    
        // add tasks to database
        if (cVVector.size() > 0) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
    
            Context context = getContext().getApplicationContext();
            
            /**
            * To be used only till we can get all updates from server, including deletes
            * 
            * */
            // Delete non-local dones from local database, to be replaced by freshly retrieved ones 
            // There could be some local non-posted dones that were typed while sync was on
            context.getContentResolver().delete(
                    DoneListContract.DoneEntry.CONTENT_URI, // Table uri
                    DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL + " IS 'FALSE'", // Selection
                    null); // Selection args
    
            // Add newly fetched entries to the database
            context.getContentResolver().bulkInsert(DoneListContract.DoneEntry.CONTENT_URI, cvArray);
            
        }
    
        if (allTagsArray.size() > 0)
            saveTagsToDatabase(getContext().getApplicationContext(), allTagsArray, true);
    
        return cVVector.size();
    }
    
    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private int getTeamsFromJson(String teamsJsonStr) throws JSONException {
        
        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.
        
        // These are the names of the JSON objects that need to be extracted.
        
        Gson gson = new Gson();
        String teamItemString;
        TeamItem teamItem;
        List<String> teams = new ArrayList<>();
        Context context = getContext().getApplicationContext();
        
        JSONObject masterObj = new JSONObject(teamsJsonStr);
        JSONArray teamsListArray = masterObj.getJSONArray("results");
        
        Vector<ContentValues> cVVector = new Vector<>(teamsListArray.length());
        
        for (int i = 0; i < teamsListArray.length(); i++) {
            teamItemString = teamsListArray.getJSONObject(i).toString();
            
            teamItem = gson.fromJson(teamItemString, TeamItem.class);
            
            teams.add(teamItem.url);
            
            ContentValues teamItemValues = new ContentValues();
            
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_URL, teamItem.url);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_NAME, teamItem.name);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_SHORT_NAME, teamItem.short_name);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_DONES, teamItem.dones);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_IS_PERSONAL, teamItem.is_personal);
            teamItemValues.put(DoneListContract.TeamEntry.COLUMN_NAME_PERMALINK, teamItem.permalink);
            
            cVVector.add(teamItemValues);
        }
        
        Log.v(LOG_TAG, "Fetched " + cVVector.size() + " teams");
        
        // add to database
        if (cVVector.size() > 0) {
            
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            
            // Add newly fetched teams to the database  
            context.getContentResolver().bulkInsert(DoneListContract.TeamEntry.CONTENT_URI, cvArray);
            
        }
        
        // Set teams in sharedPrefs for colouring
        Utils.setTeams(context, teams.toArray(new String[teams.size()]));
        
        String defaultTeam = Utils.getDefaultTeam(context);
        
        if (defaultTeam == null) {
            // No default team set, so set first team as default
            Utils.setDefaultTeam(context, teams.get(0));
            
        } else if (Utils.findTeam(context, defaultTeam) == -1) {
            // Previous default team not in fetched teams. Else raise error!
            Utils.setDefaultTeam(context, teams.get(0));
            Utils.sendMessage(context, Utils.SENDER_FETCH_TEAMS, "Default team not in teams", Utils.STATUS_TASK_OTHER_ERROR, -1);
        }
        
        // Return number of fetched teams
        return cVVector.size();
    }
    
    /**
     * POJO to parse the incoming JSON dones into before entering into the database;
     */
    public static class DoneItem {
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
        public String is_deleted = "FALSE";
        public String edited_fields;
        
        public class DoneTags {
            public int id;
            public String name;
            public String team;
        }
        
        public class DoneMeta {
            public String from;
        }
    }
    
    public class TeamItem {
        public String url;
        public String name; // unique
        public String short_name;
        public String dones;
        public boolean is_personal;
        public String permalink;
    }
}