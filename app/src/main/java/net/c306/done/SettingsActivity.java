package net.c306.done;


import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import net.c306.done.idonethis.CheckTokenTask;

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
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
        
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
    private String LOG_TAG;
    private Snackbar settingsSnackbar;
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    // Src: http://stackoverflow.com/questions/8802157/how-to-use-localbroadcastmanager    
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            // Who is sending message?
            String sender = intent.getStringExtra("sender");
            String action = intent.getStringExtra("action");
            String message = intent.getStringExtra("message");
            
            switch (sender) {
                
                case "CheckTokenTask":
                    if (action.equals(getString(R.string.check_token_started))) {
                        // Do nothing for now
                        
                    } else if (action.equals(getString(R.string.check_token_successful))) {
                        
                        Toast.makeText(getApplicationContext(), "User " + message + " authenticated. Thank you!", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - User " + message + " authenticated.");
                        
                    } else if (action.equals(getString(R.string.check_token_failed))) {
                        
                        Toast.makeText(getApplicationContext(), "Authentication failed! Please check auth token provided.", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - Authentication failed! " + message);
                        
                    } else if (action.equals(getString(R.string.check_token_cancelled_offline))) {
                        
                        Toast.makeText(getApplicationContext(), "Offline! Will check authentication when online.", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - Offline. Check again later. " + message);
                        
                    } else if (action.equals(getString(R.string.check_token_other_error))) {
                        
                        Toast.makeText(getApplicationContext(), "Server error. Will try again later.", Toast.LENGTH_LONG).show();
                        Log.v(LOG_TAG, "Broadcast Receiver - Some other error happened while checking auth: " + message);
                        
                    } else {
                        
                        Log.v(LOG_TAG, "Broadcast Receiver - Unknown response: " + message);
                        
                    }
                    
                    break;
                
                
                case "FetchTeamsTask":
                    if (action.equals(getString(R.string.fetch_teams_started))) {
                        // Do nothing for now
                        
                    } else if (action.equals(getString(R.string.fetch_teams_finished))) {
                        
                        Log.v(LOG_TAG, "Broadcast Receiver - Teams fetched" + message);
                        
                    } else if (action.equals(getString(R.string.fetch_teams_unauth))) {
                        
                        Log.v(LOG_TAG, "Broadcast Receiver - Authentication failed in teamFetch! " + message);
                        
                    } else if (action.equals(getString(R.string.fetch_teams_cancelled_offline))) {
                        
                        Log.v(LOG_TAG, "Broadcast Receiver - Offline. Will Check again later. " + message);
                        
                    } else if (action.equals(getString(R.string.fetch_teams_other_error))) {
                        
                        Log.v(LOG_TAG, "Broadcast Receiver - Some other error happened while checking auth: " + message);
                        
                    } else {
                        
                        Log.v(LOG_TAG, "Broadcast Receiver - Unknown response: " + message);
                        
                    }
                    
                    break;
                
                default:
                    Log.w(LOG_TAG, "Broadcast receiver got message from unknown sender: " +
                            intent.getStringExtra("message") +
                            " - " +
                            intent.getStringExtra("action"));
            }
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
    
        LOG_TAG = getString(R.string.app_log_identifier) + " " + SettingsActivity.class.getSimpleName();
    
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        
        // Register to receive messages.
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(getString(R.string.settings_activity_listener_intent)));
        
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
        Log.wtf(LOG_TAG, "Got pref change: " + key);
        if (key.equals(getString(R.string.auth_token))) {
            new CheckTokenTask(this).execute();
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
            
            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("authToken"));
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
