package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
                MetricType.BOOLEAN,
                MetricType.NUMBER,
                MetricType.LIST,
                MetricType.TEXT,
                MetricType.STOPWATCH,
                MetricType.HEADER
        })
@Retention(RetentionPolicy.SOURCE)
public @interface MetricType {
    int BOOLEAN = 0, NUMBER = 1, LIST = 2, TEXT = 3, STOPWATCH = 4, HEADER = 5;
}
