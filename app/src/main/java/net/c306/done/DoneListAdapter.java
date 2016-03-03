package net.c306.done;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.c306.done.db.DoneListContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DoneListAdapter extends ResourceCursorAdapter{
    
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private SimpleDateFormat sdf2 = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT);
    private Calendar calendar = Calendar.getInstance();
    private String LOG_TAG;
    private int colorArray[] = {
            //R.color.team3,
            //R.color.team4,
            R.color.team5,
            //R.color.team6,
            //R.color.team8
    };
    
    public DoneListAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
        LOG_TAG = context.getString(R.string.app_log_identifier) + " " + DoneListAdapter.class.getSimpleName();
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
        // Set team colour
        View teamSpace = view.findViewById(R.id.team_color_patch);
        int teamColor = (int) Math.round(Math.random() * (colorArray.length - 1));
        teamSpace.setBackgroundResource(colorArray[teamColor]);
        
        //// TODO: 20/02/16 Set colour based on team of the task 
    
        // Set text
        TextView rawText = (TextView) view.findViewById(R.id.item_raw_text);
        rawText.setText(cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)));
    
        if (cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL)).equals("true"))
            rawText.setTextColor(ContextCompat.getColor(mContext, R.color.secondary_text));
        else
            rawText.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text));
        
        
        // Set date
        TextView dateTextView = (TextView) view.findViewById(R.id.item_date);
        String doneDate = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE));
    
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -1);
        String yesterday = sdf.format(calendar.getTime());
        String today = sdf.format(new Date());
    
        if (doneDate.equals(yesterday))
            doneDate = "Yesterday";
        else if (doneDate.equals(today))
            doneDate = "Today";
        else {
            calendar.set(Calendar.DATE, Integer.parseInt(doneDate.substring(8, 10)));
            calendar.set(Calendar.MONTH, Integer.parseInt(doneDate.substring(5, 7)) - 1);
            calendar.set(Calendar.YEAR, Integer.parseInt(doneDate.substring(0, 4)));
            doneDate = sdf2.format(calendar.getTime());
        }
    
        dateTextView.setText(doneDate);
    }
}
