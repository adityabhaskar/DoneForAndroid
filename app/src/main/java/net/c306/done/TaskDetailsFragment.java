package net.c306.done;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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
import net.c306.done.db.DoneListDbHelper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link TaskDetailsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link TaskDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TaskDetailsFragment extends Fragment {
    
    private final String LOG_TAG = Utils.LOG_TAG + getClass().getSimpleName();
    // TODO: Rename and change types of parameters
    private long mTaskId; // Used to fetch & display task details
    private OnFragmentInteractionListener mListener;
    
    public TaskDetailsFragment() {
        // Required empty public constructor
    }
    
    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param taskId       Id of task to be displayed.
     * @return A new instance of fragment TaskDetailsFragment.
     */
    public static TaskDetailsFragment newInstance(long taskId) {
        TaskDetailsFragment fragment = new TaskDetailsFragment();
        Bundle args = new Bundle();
        args.putLong(Utils.TASK_DETAILS_TASK_ID, taskId);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mTaskId = getArguments().getLong(Utils.TASK_DETAILS_TASK_ID);
        }
        
    }
    
    @Nullable
    private Bundle getTaskDetails() {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(DoneListContract.DoneEntry.TABLE_NAME + " INNER JOIN " +
                DoneListContract.TeamEntry.TABLE_NAME +
                " ON " + DoneListContract.DoneEntry.TABLE_NAME +
                "." + DoneListContract.DoneEntry.COLUMN_NAME_TEAM +
                " = " + DoneListContract.TeamEntry.TABLE_NAME +
                "." + DoneListContract.TeamEntry.COLUMN_NAME_URL);
        
        Cursor cursor = qb.query(
                DoneListDbHelper.getInstance(getContext()).getReadableDatabase(),
                new String[]{
                        DoneListContract.DoneEntry.TABLE_NAME + "." + DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT + " as " + DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT,
                        DoneListContract.DoneEntry.TABLE_NAME + "." + DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE + " as " + DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        DoneListContract.DoneEntry.TABLE_NAME + "." + DoneListContract.DoneEntry.COLUMN_NAME_OWNER + " as " + DoneListContract.DoneEntry.COLUMN_NAME_OWNER,
                        DoneListContract.DoneEntry.TABLE_NAME + "." + DoneListContract.DoneEntry.COLUMN_NAME_LIKES + " as " + DoneListContract.DoneEntry.COLUMN_NAME_LIKES,
                        DoneListContract.DoneEntry.TABLE_NAME + "." + DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS + " as " + DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS,
                        DoneListContract.TeamEntry.TABLE_NAME + "." + DoneListContract.TeamEntry.COLUMN_NAME_NAME + " as " + DoneListContract.TeamEntry.COLUMN_NAME_NAME,
                        DoneListContract.TeamEntry.TABLE_NAME + "." + DoneListContract.TeamEntry.COLUMN_NAME_URL + " as " + DoneListContract.TeamEntry.COLUMN_NAME_URL
                },
                DoneListContract.DoneEntry.COLUMN_NAME_ID + " is ?",
                new String[]{String.valueOf(mTaskId)},
                null,
                null,
                null
        );
        
        if (cursor != null) {
            Bundle taskDetails = new Bundle();
            
            if (cursor.getCount() > 0) {
                cursor.moveToNext();
                
                taskDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT)));
                
                taskDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE)));
                
                taskDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_OWNER,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_OWNER)));
                
                taskDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_LIKES,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_LIKES)));
                
                taskDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS)));
                
                taskDetails.putString(DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS)));
                
                taskDetails.putString(DoneListContract.TeamEntry.COLUMN_NAME_NAME,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_NAME)));
                
                taskDetails.putString(DoneListContract.TeamEntry.COLUMN_NAME_URL,
                        cursor.getString(cursor.getColumnIndex(DoneListContract.TeamEntry.COLUMN_NAME_URL)));
                
            }
            cursor.close();
            
            return taskDetails;
        }
        
        return null;
    }
    
    private void populateCard(Bundle taskDetails, View view) {
        if (view == null)
            view = getView();
        
        // Format text with HTML
        TextView taskTextTextView = (TextView) view.findViewById(R.id.task_text);
        String taskText = taskDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_MARKEDUP_TEXT);
        if (taskTextTextView != null && taskText != null) {
            Spannable rawTextWithUnderlines = (Spannable) Html.fromHtml(taskText);
            SpannableString formattedText = new SpannableString(formatForTextView(rawTextWithUnderlines));
        
            taskTextTextView.setText(formattedText);
        }
        
        
        // Set task date
        TextView taskDate = (TextView) view.findViewById(R.id.task_done_date);
        if (taskDate != null) {
            // Format date for locale
            String dateString = taskDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_DONE_DATE);
            String[] dateParts = dateString.split("\\-");
        
            SimpleDateFormat idtDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.UK);
            SimpleDateFormat userDateFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.DEFAULT);
        
            Calendar c = Calendar.getInstance();
            c.setTime(new Date());
            c.add(Calendar.DATE, -1);
            String yesterday = idtDateFormat.format(c.getTime());
        
            String today = idtDateFormat.format(new Date());
        
            if (dateString.equals(yesterday))
                dateString = "Yesterday";
            else if (dateString.equals(today))
                dateString = "Today";
            else {
                c.set(
                        Integer.parseInt(dateParts[0]),
                        Integer.parseInt(dateParts[1]) - 1,
                        Integer.parseInt(dateParts[2])
                );
                dateString = userDateFormat.format(c.getTime());
            }
        
            taskDate.setText(dateString);
        }
        
        // Set task owner
        TextView taskOwner = (TextView) view.findViewById(R.id.task_owner);
        if (taskOwner != null) {
            // Set grey drawable icon
            BitmapDrawable ownerIcon = (BitmapDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ic_person_black_24dp).mutate();
            ownerIcon.setAlpha(0x8A);
            taskOwner.setCompoundDrawablesWithIntrinsicBounds(ownerIcon, null, null, null);
            
            taskOwner.setText(taskDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_OWNER));
        }
        
        
        // Set like icon and count
        TextView taskLikes = (TextView) view.findViewById(R.id.task_likes);
        if (taskLikes != null) {
            // Set grey drawable icon
            BitmapDrawable likesIcon = (BitmapDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ic_thumb_up_black_24dp).mutate();
            likesIcon.setAlpha(0x8A);
            taskLikes.setCompoundDrawablesWithIntrinsicBounds(likesIcon, null, null, null);
            
            // Set like count if not empty
            String likeCount = taskDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_LIKES);
            if (likeCount != null && !likeCount.isEmpty())
                taskLikes.setText(likeCount);
        }
        
        
        // Set comment icon & count
        TextView taskComments = (TextView) view.findViewById(R.id.task_comments);
        if (taskComments != null) {
            // Set grey drawable icon
            BitmapDrawable commentsIcon = (BitmapDrawable) ContextCompat.getDrawable(getContext(), R.drawable.ic_mode_comment_black_24dp).mutate();
            commentsIcon.setAlpha(0x8A);
            taskComments.setCompoundDrawablesWithIntrinsicBounds(commentsIcon, null, null, null);
            
            // Set comment count if not empty
            String commentCount = taskDetails.getString(DoneListContract.DoneEntry.COLUMN_NAME_COMMENTS);
            if (commentCount != null && !commentCount.isEmpty())
                taskComments.setText(commentCount);
        }
        
        // Set task team
        TextView taskTeam = (TextView) view.findViewById(R.id.task_team);
        if (taskTeam != null) {
            // Set team colour
            int teamColor = Utils.findTeam(getActivity().getApplicationContext(),
                    taskDetails.getString(DoneListContract.TeamEntry.COLUMN_NAME_URL));
            
            GradientDrawable teamCircle = (GradientDrawable) ContextCompat.getDrawable(getContext(), R.drawable.task_details_team_circle).mutate();
            teamCircle.setColor(ContextCompat.getColor(getActivity().getApplicationContext(), Utils.colorArray[teamColor == -1 ? 0 : teamColor]));
            teamCircle.setBounds(0, 0, 48, 48);
            taskTeam.setCompoundDrawables(teamCircle, null, null, null);
            
            taskTeam.setText(taskDetails.getString(DoneListContract.TeamEntry.COLUMN_NAME_NAME));
        }
        
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_details, container, false);
        
        Bundle taskDetails = getTaskDetails();
        if (taskDetails != null)
            populateCard(taskDetails, view);
        
        // Inflate the layout for this fragment
        return view;
    }
    
    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }
    
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    
    public void showByTeam(View view) {
        // TODO: 05/05/16 return to MainActivity, with team name in intent 
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
    
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
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
            ds.setColor(ContextCompat.getColor(getContext(), R.color.link_colour));
        }
    }
    
}
