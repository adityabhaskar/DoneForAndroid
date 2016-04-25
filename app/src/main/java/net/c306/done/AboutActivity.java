package net.c306.done;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {
    
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
    }
}
