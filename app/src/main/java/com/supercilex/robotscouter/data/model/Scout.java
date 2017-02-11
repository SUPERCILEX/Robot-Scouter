package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;

import com.google.firebase.database.Exclude;

import java.util.ArrayList;
import java.util.List;

public class Scout {
    @Exclude private String mName;

    @Exclude private List<ScoutMetric> mMetrics = new ArrayList<>();

    public Scout(String name) {
        mName = name;
    }

    @Keep
    public String getName() {
        return mName;
    }

    @Keep
    public void setName(String name) {
        mName = name;
    }

    @Exclude
    public List<ScoutMetric> getMetrics() {
        return new ArrayList<>(mMetrics);
    }

    public void add(ScoutMetric metric) {
        mMetrics.add(metric);
    }
}
