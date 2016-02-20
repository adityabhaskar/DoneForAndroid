package net.c306.done;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        
        TextView rawText = (TextView) view.findViewById(R.id.firstLine);
        rawText.setText(cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_RAW_TEXT)));
        
        TextView teamName = (TextView) view.findViewById(R.id.secondLine);
        teamName.setText(
                cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE)) + 
                mContext.getString(R.string.space) +
                cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_TEAM_SHORT_NAME))
        );
        
    }
    
    //@Override
    //public View getView(int position, View convertView, ViewGroup parent) {
        //LayoutInflater inflater = (LayoutInflater) context
        //        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //View rowView = inflater.inflate(R.layout.list_row_layout, parent, false);
        //TextView textView = (TextView) rowView.findViewById(R.id.firstLine);
        //ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        //
        //// Set text in Row's textView
        //textView.setText("Done number .. " + position);
        //textView.setText(values[position]);
        
        // change the icon for Windows and iPhone
        //String s = values[position];
        //if (s.startsWith("iPhone")) {
        //    imageView.setImageResource(R.drawable.ic_done_black_24dp);
        //} else {
        //    imageView.setImageResource(R.drawable.ok);
        //}
        
        //return rowView;
    //}
    
}
