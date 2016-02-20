package net.c306.done;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import net.c306.done.db.DoneListContract;

public class DoneListAdapter extends ResourceCursorAdapter{
    
    public DoneListAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }
    
    /*
    *    Remember that these views are reused as needed.
    * */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_row_layout, parent, false);
        
        return view;
    }
    
    /*
    *    This is where we fill-in the views with the contents of the cursor.
    * */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView rawText = (TextView) view.findViewById(R.id.rawText);
        rawText.setText(cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)));
        
        //// TODO: 20/02/16 Set colour based on team of the task 
        RelativeLayout rl = (RelativeLayout) view.findViewById(R.id.list_item);
        rl.setBackgroundResource(android.R.color.holo_blue_dark);

        //cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE)) + 
    }
}    
