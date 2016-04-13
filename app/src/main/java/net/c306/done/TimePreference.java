package net.c306.done;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TimePreference extends DialogPreference {
    private final String LOG_TAG = Utils.LOG_TAG + this.getClass().getSimpleName();
    private int mHour;
    private int mMinute;
    private TimePicker picker = null;
    
    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public static int getHour(String time) {
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[0]);
    }
    
    public static int getMinute(String time) {
        String[] pieces = time.split(":");
        return Integer.parseInt(pieces[1]);
    }
    
    public static String time24to12(String inTime, boolean in12hourFormat) {
        Date inDate = toDate(inTime);
        if (inDate != null) {
            DateFormat outTimeFormat;
            if (in12hourFormat)
                outTimeFormat = new SimpleDateFormat("hh:mm a", Locale.UK);
            else
                outTimeFormat = new SimpleDateFormat("HH:mm", Locale.UK);
            
            return outTimeFormat.format(inDate);
        } else {
            return inTime;
        }
    }
    
    public static Date toDate(String inTime) {
        try {
            DateFormat inTimeFormat = new SimpleDateFormat("HH:mm", Locale.US);
            return inTimeFormat.parse(inTime);
        } catch (ParseException e) {
            return null;
        }
    }
    
    public void setTime(int hour, int minute) {
        mHour = hour;
        mMinute = minute;
        String time = toTimeString(mHour, mMinute);
        persistString(time);
        notifyDependencyChange(shouldDisableDependents());
        notifyChanged();
    }
    
    public String toTimeString(int hour, int minute) {
        String mn = minute < 10 ? "0" + minute : String.valueOf(minute);
        String hr = hour < 10 ? "0" + hour : String.valueOf(hour);
        return hr + ":" + mn;
    }
    
    public void updateSummary() {
        String time = toTimeString(mHour, mMinute);
        setSummary(time24to12(time, !android.text.format.DateFormat.is24HourFormat(getContext())));
    }
    
    @Override
    protected View onCreateDialogView() {
        picker = new TimePicker(getContext());
        picker.setIs24HourView(android.text.format.DateFormat.is24HourFormat(getContext()));
        return picker;
    }
    
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        if (Build.VERSION.SDK_INT >= 23) {
            picker.setHour(mHour);
            picker.setMinute(mMinute);
        } else {
            picker.setCurrentHour(mHour);
            picker.setCurrentMinute(mMinute);
        }
    }
    
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        
        if (positiveResult) {
            int currHour;
            int currMinute;
            if (Build.VERSION.SDK_INT >= 23) {
                currHour = picker.getHour();
                currMinute = picker.getMinute();
            } else {
                currHour = picker.getCurrentHour();
                currMinute = picker.getCurrentMinute();
            }
            
            if (!callChangeListener(toTimeString(currHour, currMinute))) {
                return;
            }
            
            SharedPreferences preferences = getSharedPreferences();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getKey(), toTimeString(currHour, currMinute))
                    .apply();
            
            // persist
            setTime(currHour, currMinute);
            
            updateSummary();
        }
    }
    
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }
    
    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        String time = null;
        
        if (restorePersistedValue) {
            SharedPreferences preferences = getSharedPreferences();
            String DEFAULT_TIME = "00:00";
            time = preferences.getString(getKey(), DEFAULT_TIME);
        } else {
            time = defaultValue.toString();
        }
        
        int currHour = getHour(time);
        int currMinute = getMinute(time);
        // need to persist here for default value to work
        setTime(currHour, currMinute);
        updateSummary();
    }
}
