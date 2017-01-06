package com.supercilex.robotscouter.data.model;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@IntDef({
        MetricType.CHECKBOX,
        MetricType.COUNTER,
        MetricType.SPINNER,
        MetricType.EDIT_TEXT,
})
@Retention(RetentionPolicy.SOURCE)
public @interface MetricType {
    int CHECKBOX = 0;
    int COUNTER = 1;
    int SPINNER = 2;
    int EDIT_TEXT = 3;
    int SLIDER = 4;
}
