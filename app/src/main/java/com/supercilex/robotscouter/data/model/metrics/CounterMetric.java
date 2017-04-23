package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import com.google.firebase.database.Exclude;
import com.supercilex.robotscouter.util.Constants;

public class CounterMetric extends ScoutMetric<Integer> {
    @Exclude
    @Nullable
    private String mUnit;

    @RestrictTo(RestrictTo.Scope.TESTS)
    public CounterMetric() { // Needed for Firebase
        super();
    }

    public CounterMetric(String name, int value, @Nullable String unit) {
        super(name, value, MetricType.NUMBER);
        mUnit = unit;
    }

    @Keep
    @Nullable
    public String getUnit() {
        return mUnit;
    }

    @Keep
    public void setUnit(@Nullable String unit) {
        mUnit = unit;
    }

    public void updateUnit(String unit) {
        setUnit(unit);
        mRef.child(Constants.FIREBASE_UNIT).setValue(mUnit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        CounterMetric metric = (CounterMetric) o;

        return TextUtils.equals(mUnit, metric.mUnit);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (mUnit == null ? 0 : mUnit.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return super.toString() + "\nUnit = " + mUnit;
    }
}
