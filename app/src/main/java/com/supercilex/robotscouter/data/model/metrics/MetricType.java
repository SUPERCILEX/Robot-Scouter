package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
                MetricType.CHECKBOX,
                MetricType.COUNTER,
                MetricType.SPINNER,
                MetricType.NOTE,
                MetricType.STOPWATCH,
                MetricType.HEADER
        })
@Retention(RetentionPolicy.SOURCE)
public @interface MetricType {
    int CHECKBOX = 0, COUNTER = 1, SPINNER = 2, NOTE = 3, STOPWATCH = 4, HEADER = 5;
}
