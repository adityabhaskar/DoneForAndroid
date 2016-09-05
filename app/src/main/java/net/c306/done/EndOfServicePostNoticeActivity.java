package net.c306.done;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.google.android.gms.analytics.Tracker;

public class EndOfServicePostNoticeActivity extends AppCompatActivity {
    private Tracker mTracker;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_endofservicepostnotice);
        
        // Analytics Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Utils.sendScreen(mTracker, getClass().getSimpleName());
    }
    
    public void openPost(View view) {
        Utils.setNotificationSeen(EndOfServicePostNoticeActivity.this, true);
        
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(Utils.EOS_POST_URL));
        startActivity(i);
        
        setResult(RESULT_OK);
        finish();
    }
}
