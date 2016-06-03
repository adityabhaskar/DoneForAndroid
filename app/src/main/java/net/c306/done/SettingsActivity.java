package net.c306.done;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.analytics.Tracker;

import net.c306.done.db.DoneListContract;
import net.c306.done.sync.IDTSyncAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (value instanceof String) {
                String stringValue = value.toString();
    
                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);
            
                    // Set the summary to reflect the new value.
                    if (index >= 0)
                        preference.setSummary(listPreference.getEntries()[index]);
            
                } else if (preference instanceof RingtonePreference) {
                    // For ringtone preferences, look up the correct display value
                    // using RingtoneManager.
                    if (TextUtils.isEmpty(stringValue)) {
                        // Empty values correspond to 'silent' (no ringtone).
                        preference.setSummary(R.string.pref_ringtone_silent);
                
                    } else {
                        Ringtone ringtone = RingtoneManager.getRingtone(
                                preference.getContext(), Uri.parse(stringValue));
                
                        if (ringtone == null) {
                            // Clear the summary if there was a lookup error.
                            preference.setSummary(null);
                        } else {
                            // Set the summary to reflect the new ringtone display
                            // name.
                            String name = ringtone.getTitle(preference.getContext());
                            preference.setSummary(name);
                        }
                        //Log.v(Utils.LOG_TAG, "Set ringtone to " + stringValue);
                    }
            
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
            } else {
        
                Set<String> notificationDays = new HashSet<String>((Set<String>) value);
                String summary = "";
                Context context = preference.getContext();
        
                if (notificationDays.equals(Utils.DEFAULT_NOTIFICATION_DAYS)) {
                    // All days selected
                    summary = context.getString(R.string.everyday);
                } else if (notificationDays.equals(Utils.WEEKDAY_VALUES)) {
                    // Only Weekdays selected
                    summary = context.getString(R.string.weekdays_only);
                } else if (notificationDays.equals(Utils.WEEKEND_VALUES)) {
                    // Only Weekends selected
                    summary = context.getString(R.string.weekends_only);
                    
                } else {
            
                    if (notificationDays.containsAll(Utils.WEEKDAY_VALUES)) {
                        // Check if all week days selected
                        notificationDays.removeAll(Utils.WEEKDAY_VALUES);
                        summary = context.getString(R.string.weekdays) + " ";
                
                    } else if (notificationDays.containsAll(Utils.WEEKEND_VALUES)) {
                        // Check if all weekend days selected
                        notificationDays.removeAll(Utils.WEEKEND_VALUES);
                        summary = context.getString(R.string.weekends) + " ";
                    }
            
                    MultiSelectListPreference daysPreference = (MultiSelectListPreference) preference;
                    CharSequence[] entryValues = daysPreference.getEntries();
                    String[] selectedDaysString = notificationDays.toArray(new String[notificationDays.size()]);
                    Arrays.sort(selectedDaysString);
            
                    for (String index : selectedDaysString) {
                        summary += entryValues[Integer.parseInt(index) - 1] + ", ";
                    }
                    summary = summary.substring(0, summary.length() - 2);
                }
                preference.setSummary(summary);
            }
            return true;
        }
    };
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private Tracker mTracker;
    private boolean mSyncList = false;              // Flag indicating that settings changed may impact task list, so sync it when exiting activity
    
    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
    
        // Trigger the listener immediately with the preference's
        // current value.
        switch (preference.getKey()) {
            case Utils.PREF_NOTIFICATION_DAYS:
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager
                                .getDefaultSharedPreferences(preference.getContext())
                                .getStringSet(preference.getKey(), Utils.DEFAULT_NOTIFICATION_DAYS));
                break;
        
            default:
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager
                                .getDefaultSharedPreferences(preference.getContext())
                                .getString(preference.getKey(), ""));
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    
        // Analytics Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new MainPreferenceFragment())
                .commit();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    
        Utils.sendScreen(mTracker, getClass().getSimpleName());
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    protected void onStop() {
        if (mSyncList)
            IDTSyncAdapter.syncImmediately(getApplicationContext());
        
        super.onStop();
    }
    
    /**
    * Implementing method to handle change in preferences, and test for valid authToken
    * */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    
        switch (key) {
            
            case Utils.PREF_DEFAULT_TEAM: {
                Log.v(LOG_TAG, key + " changed to: " + sharedPreferences.getString(key, ""));
                // Nothing to do
                break;
            }
        
            case Utils.PREF_SYNC_FREQUENCY: {
                int syncFrequency = Integer.parseInt(
                        sharedPreferences.getString(key, String.valueOf(Utils.SYNC_DEFAULT_INTERVAL))
                );
    
                Log.v(LOG_TAG, key + " changed to: " + syncFrequency);
    
                if (syncFrequency > 0)
                    IDTSyncAdapter.configurePeriodicSync(getApplicationContext(), syncFrequency);
                else
                    IDTSyncAdapter.stopPeriodicSync(getApplicationContext());
                
                break;
            }
        
            case Utils.PREF_SHOW_NOTIFICATION: {
                if (sharedPreferences.getBoolean(key, Utils.DEFAULT_SHOW_NOTIFICATION)) {
                    Utils.setNotificationAlarm(this, null);
                } else {
                    Utils.cancelNotificationAlarm(this);
                }
                break;
            }
        
            case Utils.PREF_NOTIFICATION_DAYS: {
                Set<String> notificationDays = sharedPreferences.getStringSet(key, null);
            
                // Get previous notification days
                SharedPreferences userPreferences = this.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, MODE_PRIVATE);
                Set<String> prevNotificationDays = userPreferences.getStringSet(key, null);
            
                if (notificationDays == null || notificationDays.isEmpty()) {
                
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                
                    // If previous notification days are available, set to that, else set to all
                    if (prevNotificationDays != null && !prevNotificationDays.isEmpty())
                        editor.putStringSet(key, prevNotificationDays);
                    else
                        editor.putStringSet(key, Utils.DEFAULT_NOTIFICATION_DAYS);
                
                    editor.apply();
                
                    Snackbar.make(getCurrentFocus(), "Please select at least one day.", Snackbar.LENGTH_SHORT).show();
                } else {
                    // Update previous notification days
                    SharedPreferences.Editor editor = userPreferences.edit();
                    editor.putStringSet(key, notificationDays);
                    editor.apply();
                }
                break;
            }
        
            case Utils.PREF_NOTIFICATION_TIME: {
                Utils.setNotificationAlarm(SettingsActivity.this, null);
                break;
            }
        
            case Utils.PREF_DAYS_TO_FETCH:
            case Utils.PREF_COUNT_TO_FETCH: {
                mSyncList = true;
                break;
            }
        
        
            default:
                Log.w(LOG_TAG, "Unidentified pref change: " + key);
                break;
        }
    }
    
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    
    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || MainPreferenceFragment.class.getName().equals(fragmentName);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * This fragment shows all preferences. 
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            
            setHasOptionsMenu(true);
    
            setupTeamsInSelector();
    
            String daysToFetch = Utils.getFetchValue(getActivity().getApplicationContext(), Utils.PREF_DAYS_TO_FETCH);
            String countToFetch = Utils.getFetchValue(getActivity().getApplicationContext(), Utils.PREF_COUNT_TO_FETCH);
            
            EditTextPreference countToFetchPreference = (EditTextPreference) findPreference(Utils.PREF_COUNT_TO_FETCH);
            countToFetchPreference.setSummary(countToFetch);
    
            ListPreference daysToFetchPreference = (ListPreference) findPreference(Utils.PREF_DAYS_TO_FETCH);
            String daysToFetchTitle = getString(R.string.PREF_TITLE_DAYS_TO_FETCH)
                    .replaceFirst("NUMCOUNT", countToFetch);
            daysToFetchPreference.setTitle(daysToFetchTitle);
            int index = daysToFetchPreference.findIndexOfValue(daysToFetch);
            if (index >= 0)
                daysToFetchPreference.setSummary(daysToFetchPreference.getEntries()[index]);
            
            bindPreferenceSummaryToValue(findPreference(Utils.PREF_DEFAULT_TEAM));
            bindPreferenceSummaryToValue(findPreference(Utils.PREF_DEFAULT_VIEW));
            bindPreferenceSummaryToValue(findPreference(Utils.PREF_NOTIFICATION_SOUND));
            bindPreferenceSummaryToValue(findPreference(Utils.PREF_SNOOZE_DURATION));
            bindPreferenceSummaryToValue(findPreference(Utils.PREF_NOTIFICATION_DAYS));
            bindPreferenceSummaryToValue(findPreference(Utils.PREF_SYNC_FREQUENCY));
    
            TaskCountPreferenceChangeListener taskCountPreferenceChangeListener = new TaskCountPreferenceChangeListener();
    
            findPreference(Utils.PREF_DAYS_TO_FETCH).setOnPreferenceChangeListener(taskCountPreferenceChangeListener);
            findPreference(Utils.PREF_COUNT_TO_FETCH).setOnPreferenceChangeListener(taskCountPreferenceChangeListener);
            
        }
        
        /**
         * 
         */
        private void setupTeamsInSelector() {
            
            ListPreference defaultTeamListPreference = (ListPreference) findPreference(Utils.PREF_DEFAULT_TEAM);
            ListPreference defaultViewListPreference = (ListPreference) findPreference(Utils.PREF_DEFAULT_VIEW);
            
            if (Utils.getAccessToken(getActivity().getApplicationContext()) != null &&
                    defaultTeamListPreference != null && defaultViewListPreference != null) {
                
                // Fill up team names in listPreference in General Preferences
                defaultTeamListPreference.setEnabled(true);
                defaultViewListPreference.setEnabled(true);
                
                Cursor cursor = getActivity().getContentResolver().query(
                        DoneListContract.TeamEntry.CONTENT_URI,
                        new String[]{
                                DoneListContract.TeamEntry.COLUMN_NAME_ID,
                                DoneListContract.TeamEntry.COLUMN_NAME_NAME,
                                DoneListContract.TeamEntry.COLUMN_NAME_URL
                        },
                        null,
                        null,
                        null
                );
        
                if (cursor != null && cursor.getCount() > 0) {
                    
                    List<String> teamNames = new ArrayList<>();
                    List<String> teamURLs = new ArrayList<>();
                    List<String> viewNames = new ArrayList<>();
                    List<String> viewURLs = new ArrayList<>();
    
                    viewNames.add(getString(R.string.all_tasks));
                    viewURLs.add("");
                    
                    int columnTeamName = cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_NAME);
                    int columnTeamURL = cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_URL);
    
                    while (cursor.moveToNext()) {
                        teamNames.add(cursor.getString(columnTeamName));
                        teamURLs.add(cursor.getString(columnTeamURL));
                        viewNames.add(cursor.getString(columnTeamName));
                        viewURLs.add(cursor.getString(columnTeamURL));
                    }
    
                    cursor.close();
    
                    defaultTeamListPreference.setEntries(teamNames.toArray(new String[teamNames.size()]));
                    defaultTeamListPreference.setEntryValues(teamURLs.toArray(new String[teamURLs.size()]));
                    defaultViewListPreference.setEntries(viewNames.toArray(new String[viewNames.size()]));
                    defaultViewListPreference.setEntryValues(viewURLs.toArray(new String[viewURLs.size()]));
                    
                } else {
    
                    defaultTeamListPreference.setEntries(new String[]{});
                    defaultTeamListPreference.setEntryValues(new String[]{});
                    defaultViewListPreference.setEntries(new String[]{getString(R.string.all_tasks)});
                    defaultViewListPreference.setEntryValues(new String[]{""});
                    
                }
            } else
                Log.w(Utils.LOG_TAG + getClass().getSimpleName(), "defaultTeamListPreference is null");
        }
        
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    
        private class TaskCountPreferenceChangeListener implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
    
                switch (preference.getKey()) {
                    case Utils.PREF_DAYS_TO_FETCH: {
                        ListPreference daysToFetchPreference = (ListPreference) preference;
                        int index = daysToFetchPreference.findIndexOfValue((String) newValue);
                        if (index >= 0)
                            daysToFetchPreference.setSummary(daysToFetchPreference.getEntries()[index]);
            
                        break;
                    }
        
                    case Utils.PREF_COUNT_TO_FETCH: {
                        EditTextPreference countToFetchPreference = (EditTextPreference) preference;
                        String newValueString = (String) newValue;
    
                        boolean isModified = false;
    
                        if (newValueString.isEmpty() || !Utils.isInteger(newValueString)) {
                            newValueString = String.valueOf(Utils.DEFAULT_PREF_VALUE_COUNT_TO_FETCH);
                            isModified = true;
                        } else {
                            try {
                                int newValueInt = Integer.parseInt(newValueString);
            
                                if (newValueInt > Utils.MAX_PREF_VALUE_COUNT_TO_FETCH) {
                                    newValueString = "" + Utils.MAX_PREF_VALUE_COUNT_TO_FETCH;
                                    isModified = true;
                                } else if (newValueInt < Utils.MIN_PREF_VALUE_COUNT_TO_FETCH) {
                                    newValueString = "" + Utils.MIN_PREF_VALUE_COUNT_TO_FETCH;
                                    isModified = true;
                                }
            
                            } catch (NumberFormatException e) {
                                newValueString = String.valueOf(Utils.DEFAULT_PREF_VALUE_COUNT_TO_FETCH);
                                isModified = true;
                            }
                        }
    
                        if (isModified) {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(Utils.PREF_COUNT_TO_FETCH, newValueString);
                            editor.commit();
        
                            countToFetchPreference.setText(newValueString);
                        }
                        
                        countToFetchPreference.setSummary(newValueString);
    
                        ListPreference daysToFetchPreference = (ListPreference) findPreference(Utils.PREF_DAYS_TO_FETCH);
                        String daysToFetchTitle = getString(R.string.PREF_TITLE_DAYS_TO_FETCH)
                                .replaceFirst("NUMCOUNT", newValueString);
                        daysToFetchPreference.setTitle(daysToFetchTitle);
    
                        return !isModified;
                    }
                }
                return true;
            }
        }
    }
    
}
