package net.c306.done;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ResourceCursorAdapter;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.c306.done.db.DoneListContract;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TaskListAdapter extends ResourceCursorAdapter {
    
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private SimpleDateFormat idtDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
    private SimpleDateFormat userDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT);
    private Calendar calendar = Calendar.getInstance();
    
    public TaskListAdapter(Context context, int layout, Cursor c, int flags) {
        super(context, layout, c, flags);
    }
    
    /**
    *    Remember that these views are reused as needed.
    * */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.tasks_list_row_layout, parent, false);
    }
    
    /**
    *    This is where we fill-in the views with the contents of the cursor.
    * */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
    
        // Set team colour
        View teamSpace = view.findViewById(R.id.team_color_patch);
    
        int teamColor = Utils.findTeam(context, cursor.getString(
                cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_TEAM)
        ));
    
        teamSpace.setBackgroundResource(Utils.colorArray[teamColor == -1 ? 0 : teamColor % Utils.colorArray.length]);
        
        // Set text
        TextView rawTextTextView = (TextView) view.findViewById(R.id.text_view_task_text);
        
        Spannable rawTextWithUnderlines = (Spannable) Html.fromHtml(cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT)));
        SpannableString formattedText = new SpannableString(formatForTextView(rawTextWithUnderlines));
        rawTextTextView.setText(formattedText);
    
        String isEdited = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_EDITED_FIELDS));
        String isLocal = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_IS_LOCAL));
    
        // If task is locally created or edited, set text colour to secondary_text
        if ("TRUE".equals(isLocal) || (isEdited != null && !isEdited.trim().isEmpty()))
            rawTextTextView.setTextColor(ContextCompat.getColor(mContext, R.color.secondary_text));
        else
            rawTextTextView.setTextColor(ContextCompat.getColor(mContext, R.color.primary_text));
    
    
        // Set date
        TextView dateTextView = (TextView) view.findViewById(R.id.item_date);
        String doneDate = cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE));
    
        calendar.setTime(new Date());
        calendar.add(Calendar.DATE, -1);
        String yesterday = idtDateFormat.format(calendar.getTime());
        String today = idtDateFormat.format(new Date());
    
        if (doneDate.equals(yesterday))
            doneDate = "Yesterday";
        else if (doneDate.equals(today))
            doneDate = "Today";
        else {
            calendar.set(Calendar.DATE, Integer.parseInt(doneDate.substring(8, 10)));
            calendar.set(Calendar.MONTH, Integer.parseInt(doneDate.substring(5, 7)) - 1);
            calendar.set(Calendar.YEAR, Integer.parseInt(doneDate.substring(0, 4)));
            doneDate = userDateFormat.format(calendar.getTime());
        }
    
        dateTextView.setText(doneDate);
    }
    
    
    private Spannable formatForTextView(Spannable p_Text) {
        URLSpan[] spans = p_Text.getSpans(0, p_Text.length(), URLSpan.class);
        Pattern hashTagURLPattern = Pattern.compile(".*\\/\\#tags\\/.*", Pattern.CASE_INSENSITIVE);
        Matcher hashtagMatcher = null;
        for (URLSpan span : spans) {
            hashtagMatcher = hashTagURLPattern.matcher(span.getURL());
            // Only format if it's a hashtag link
            if (hashtagMatcher.find()) {
                int start = p_Text.getSpanStart(span);
                int end = p_Text.getSpanEnd(span);
                p_Text.removeSpan(span);
                span = new URLSpanNoUnderline(span.getURL());
                p_Text.setSpan(span, start, end, 0);
            }
        }
        return p_Text;
    }
    
    private class URLSpanNoUnderline extends URLSpan {
        public URLSpanNoUnderline(String url) {
            super(url);
        }
    
        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            //ds.setFakeBoldText(true);
            ds.setColor(ContextCompat.getColor(mContext, R.color.secondary_text));
        }
    }
    
}
