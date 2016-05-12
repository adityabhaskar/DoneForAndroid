package net.c306.done;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.c306.done.db.DoneListContract;

public class NavTagListAdapter extends ResourceCursorAdapter {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    
    public NavTagListAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }
    
    /**
     * Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.nav_list_row_layout, parent, false);
    }
    
    /**
     * This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        
        // Set image resource for tag icon
        ImageView tagIconImageView = (ImageView) view.findViewById(R.id.nav_team_color_patch);
        BitmapDrawable settingsIcon = (BitmapDrawable) ContextCompat.getDrawable(context, R.drawable.ic_label_black_24dp).mutate();
        settingsIcon.setAlpha(0x8A);
        if (tagIconImageView != null)
            tagIconImageView.setImageDrawable(settingsIcon);
        
        // Set tag name
        TextView tagNameTextView = (TextView) view.findViewById(R.id.team_name_text_view);
        
        tagNameTextView.setText(
                cursor.getString(
                        cursor.getColumnIndex(DoneListContract.TagEntry.COLUMN_NAME_NAME))
        );
    }
}
