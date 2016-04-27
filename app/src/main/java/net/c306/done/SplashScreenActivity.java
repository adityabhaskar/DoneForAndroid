package net.c306.done;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AnimationUtils;

import com.google.android.gms.analytics.Tracker;

import net.c306.done.sync.IDTAccountManager;
import net.c306.done.sync.IDTSyncAdapter;

public class SplashScreenActivity extends AppCompatActivity {
    
    private final String LOG_TAG = Utils.LOG_TAG + getClass().getSimpleName();
    private Snackbar mSnackbar;
    private Tracker mTracker = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Account syncAccount = IDTAccountManager.getSyncAccount(SplashScreenActivity.this);
        
        if (syncAccount != null) {
            // Logged in - go to Main Activity
    
            // Sync on launch if setting is set to true
            if (Utils.getSyncOnStartup(this))
                IDTSyncAdapter.syncImmediately(getApplicationContext());
            
            Intent nextActivity = new Intent(SplashScreenActivity.this, MainActivity.class);
            startActivity(nextActivity);
            finish();
            
        } else {
            getWindow().setBackgroundDrawableResource(R.drawable.login_screen);
            
            // Not logged in - inflate Layout to show Login Screen
            View view = getLayoutInflater().inflate(R.layout.activity_splash_screen, null, false);
            view.startAnimation(AnimationUtils.loadAnimation(SplashScreenActivity.this, android.R.anim.fade_in));
    
            setContentView(view);
    
            // Analytics Obtain the shared Tracker instance.
            AnalyticsApplication application = (AnalyticsApplication) getApplication();
            mTracker = application.getDefaultTracker();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Utils.sendScreen(mTracker, getClass().getSimpleName());
    }
    
    public void openLoginActivity(View view) {
        Intent loginActivityIntent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        loginActivityIntent.putExtra("fromSplash", true);
        startActivityForResult(loginActivityIntent, Utils.LOGIN_ACTIVITY_IDENTIFIER);
        finish();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        switch (requestCode) {
            case Utils.LOGIN_ACTIVITY_IDENTIFIER: {
                
                if (mSnackbar != null && mSnackbar.isShown())
                    mSnackbar.dismiss();
                
                if (resultCode == RESULT_CANCELED) {
                    // If error, show toast/snackbar.
                    mSnackbar = Snackbar.make(findViewById(R.id.fab), R.string.LOGIN_ERROR_OR_CANCELLED, Snackbar.LENGTH_LONG);
                    mSnackbar.setAction("Dismiss", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mSnackbar != null && mSnackbar.isShown())
                                mSnackbar.dismiss();
                        }
                    })
                            .setActionTextColor(ContextCompat.getColor(this, R.color.link_colour))
                            .show();
                    
                } else if (resultCode == RESULT_OK) {
                    // Successfully logged in
                    
                    // Setup alarm
                    Utils.setNotificationAlarm(SplashScreenActivity.this, null);
                    
                    // Setup sync
                    IDTSyncAdapter.initializeSyncAdapter(getApplicationContext());
                    
                    Utils.sendEvent(mTracker, "action", "loginSuccessful");
                    
                    Intent mainActivity = new Intent(SplashScreenActivity.this, MainActivity.class);
                    mainActivity.putExtra(
                            Utils.INTENT_FROM_ACTIVITY_IDENTIFIER,
                            Utils.LOGIN_ACTIVITY_IDENTIFIER
                    );
                    startActivity(mainActivity);
                }
            }
        }
    }
    
}
