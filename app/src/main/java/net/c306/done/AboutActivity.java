package net.c306.done;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import com.google.android.gms.analytics.Tracker;

public class AboutActivity extends AppCompatActivity {
    private Tracker mTracker;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
    
        TextView developerName = (TextView) findViewById(R.id.about_dialog_developer_name);
        if (developerName != null) {
            developerName.setMovementMethod(LinkMovementMethod.getInstance());
            developerName.setLinksClickable(true);
            developerName.setText(Html.fromHtml(getString(R.string.developer_string)));
        }
        
        TextView versionName = (TextView) findViewById(R.id.about_dialog_version);
        if (versionName != null)
            versionName.setText("version " + BuildConfig.VERSION_NAME);
    
        // Analytics Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        Utils.sendScreen(mTracker, getClass().getSimpleName());
    }
}
