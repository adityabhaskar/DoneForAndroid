package net.c306.done;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import net.c306.done.db.DoneListContract;
import net.c306.done.idonethis.CheckTokenTask;

import java.util.ArrayList;
import java.util.List;

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
                }
                
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };
    Snackbar mSnackbar = null;
    private String LOG_TAG;
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    // Src: http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            // Who is sending message?
            String sender = intent.getStringExtra("sender");
            int action = intent.getIntExtra("action", -1);
            String message = intent.getStringExtra("message");
    
            if (sender.equals(CheckTokenTask.class.getSimpleName())) {
                switch (action) {
                    case R.string.CHECK_TOKEN_STARTED: {
                        // Do nothing for now
                        if (mSnackbar != null && mSnackbar.isShownOrQueued())
                            mSnackbar.dismiss();
                        mSnackbar = Snackbar.make(getListView(), "Checking token...", Snackbar.LENGTH_INDEFINITE);
                        mSnackbar.setAction("", null);
                        mSnackbar.show();
                        break;
                    }
    
                    case R.string.CHECK_TOKEN_SUCCESSFUL: {
                        if (mSnackbar != null && mSnackbar.isShownOrQueued())
                            mSnackbar.dismiss();
                        mSnackbar = Snackbar.make(getListView(), "User " + message + " authenticated.", Snackbar.LENGTH_SHORT);
                        mSnackbar.setAction("", null);
                        mSnackbar.show();
                        //Toast.makeText(getApplicationContext(), "User " + message + " authenticated. Thank you!", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - User " + message + " authenticated.");
                        // TODO: 14/03/16 Enable default team preference
                        
                        break;
                    }
    
                    case R.string.CHECK_TOKEN_FAILED: {
                        if (mSnackbar != null && mSnackbar.isShownOrQueued())
                            mSnackbar.dismiss();
                        mSnackbar = Snackbar.make(getListView(), "Authentication failed!", Snackbar.LENGTH_LONG);
                        mSnackbar.setAction("", null);
                        mSnackbar.show();
                        //Toast.makeText(getApplicationContext(), "Authentication failed! Please check auth token provided.", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - Authentication failed! " + message);
                        break;
                    }
    
                    case R.string.CHECK_TOKEN_CANCELLED_OFFLINE: {
                        if (mSnackbar != null && mSnackbar.isShownOrQueued())
                            mSnackbar.dismiss();
                        Toast.makeText(getApplicationContext(), "Offline! Will check authentication when online.", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - Offline. Check again later. " + message);
                        break;
                    }
    
                    case R.string.CHECK_TOKEN_OTHER_ERROR: {
                        Toast.makeText(getApplicationContext(), "Server/Network error. Will try again later.", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - Some other error happened while checking auth: " + message);
                    }
            
                }
            }
            Log.v(LOG_TAG, "Original message from:" + sender + ",\nwith action:" + getString(action) + ", and\nmessage:" + message);
        }
    };
    
    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }
    
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
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();
    
        LOG_TAG = getString(R.string.APP_LOG_IDENTIFIER) + " " + this.getClass().getSimpleName();
    
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(getString(R.string.DONE_LOCAL_BROADCAST_LISTENER_INTENT)));
        
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }
    
    /*
    * Implementing method to handle change in preferences, and test for valid authToken
    * */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.AUTH_TOKEN))) {
            Log.wtf(LOG_TAG, key + " changed.");
            new CheckTokenTask(this).execute();
        } else if (key.equals(getString(R.string.DEFAULT_TEAM))) {
            Log.v(LOG_TAG, key + " changed to: " +
                    sharedPreferences.getString(
                            getString(R.string.DEFAULT_TEAM),
                            ""
                    )
            );
        } else
            Log.wtf(LOG_TAG, "Unidentified pref change: " + key);
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
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }
    
    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName)
                || DataSyncPreferenceFragment.class.getName().equals(fragmentName)
                || NotificationPreferenceFragment.class.getName().equals(fragmentName);
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
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);
    
            setupTeamsSelector();
            
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference(getString(R.string.AUTH_TOKEN)));
            bindPreferenceSummaryToValue(findPreference(getString(R.string.DEFAULT_TEAM)));
    
    
        }
    
        private void setupTeamsSelector() {
        
            ListPreference defaultTeamListPreference = (ListPreference) findPreference(getString(R.string.DEFAULT_TEAM));
        
            if (Utils.haveValidToken(getActivity().getApplicationContext())) {
            
                // Fill up team names in listPreference in General Preferences
                defaultTeamListPreference.setEnabled(true);
            
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
                
                    int columnTeamName = cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_NAME);
                    int columnTeamURL = cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_URL);
                
                    while (cursor.moveToNext()) {
                        teamNames.add(cursor.getString(columnTeamName));
                        teamURLs.add(cursor.getString(columnTeamURL));
                    }
                
                    cursor.close();
                
                    defaultTeamListPreference.setEntries(teamNames.toArray(new String[teamNames.size()]));
                    defaultTeamListPreference.setEntryValues(teamURLs.toArray(new String[teamURLs.size()]));
                } else {
                    // TODO: 10/03/16 When database is updated, schedule an automatic team fetch 
                    defaultTeamListPreference.setEntries(new String[]{});
                    defaultTeamListPreference.setEntryValues(new String[]{});
                }
            } else
                defaultTeamListPreference.setEnabled(false);
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
    }
    
    /**
     * This fragment shows notification preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class NotificationPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_notification);
            setHasOptionsMenu(true);
    
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
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
    }
    
    /**
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);
            setHasOptionsMenu(true);
    
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
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
    }
    
}
