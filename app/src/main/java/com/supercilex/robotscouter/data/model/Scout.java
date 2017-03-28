package com.supercilex.robotscouter.data.model;

import android.support.annotation.Keep;
import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.supercilex.robotscouter.data.model.metrics.ScoutMetric;

import java.util.ArrayList;
import java.util.List;

public class Scout {
    @Exclude private String mName;

    @Exclude private List<ScoutMetric> mMetrics = new ArrayList<>();

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Scout scout = (Scout) o;

        return TextUtils.equals(mName, scout.mName) && mMetrics.equals(scout.mMetrics);
    }

    @Override
    public int hashCode() {
        int result = mName == null ? 0 : mName.hashCode();
        result = 31 * result + mMetrics.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "Scout{" +
                "mName='" + mName + '\'' +
                ", mMetrics=" + mMetrics +
                '}';
    }
}
