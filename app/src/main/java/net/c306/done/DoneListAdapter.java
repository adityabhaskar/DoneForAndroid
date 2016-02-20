package net.c306.done;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.ResourceCursorAdapter;
import android.view.View;
import android.widget.TextView;

public class DoneListAdapter extends ResourceCursorAdapter{
    
    public DoneListAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView content = (TextView) view.findViewById(R.id.firstLine);
        //content.setText(cursor.getString(cursor.getColumnIndex(Table.CONTENT)));
        content.setText(cursor.getString(cursor.getColumnIndexOrThrow("body")));
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
