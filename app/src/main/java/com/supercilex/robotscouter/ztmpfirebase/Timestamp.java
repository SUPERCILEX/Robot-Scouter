package com.supercilex.robotscouter.ztmpfirebase;

import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import com.google.firebase.database.ServerValue;

import java.util.Map;

public class Timestamp {
    private long mTimestamp;

    @PropertyName("timestamp")
    public Map<String, String> getServerValue() {
        return ServerValue.TIMESTAMP;
    }

    @Exclude
    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long time) {
        mTimestamp = time;
        Log.d("time", "setTimestamp: " + time);
    }
}
