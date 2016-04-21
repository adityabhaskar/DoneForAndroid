package net.c306.done;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import net.c306.done.sync.IDTAccountManager;

public class SplashScreenActivity extends AppCompatActivity {
    
    private final String LOG_TAG = Utils.LOG_TAG + getClass().getSimpleName();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Account syncAccount = IDTAccountManager.getSyncAccount(SplashScreenActivity.this);
        
        if (syncAccount != null) {
            // Logged in - go to Main Activity
            
            Intent nextActivity = new Intent(SplashScreenActivity.this, MainActivity.class);
            startActivity(nextActivity);
            finish();
            
        } else {
            // Not logged in - inflate Layout to show Login Screen
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //Do something after 100ms
                    setContentView(R.layout.activity_splash_screen);
                }
            }, 500);
        }
    }
    
    public void openLoginActivity(View view) {
        Intent loginActivityIntent = new Intent(SplashScreenActivity.this, LoginActivity.class);
        loginActivityIntent.putExtra("fromSplash", true);
        startActivity(loginActivityIntent);
        finish();
    }
}
