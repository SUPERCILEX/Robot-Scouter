package com.supercilex.robotscouter.data.model.metrics;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class StopwatchMetric extends ScoutMetric<List<Long>> {
    public StopwatchMetric(String name, List<Long> value) {
        super(name, value, MetricType.STOPWATCH);
    }

    /**
     * {@inheritDoc}
     *
     * @return a list of all stopwatch laps in milliseconds
     */
    @Override
    @NonNull
    public List<Long> getValue() {
        if (super.getValue() == null) setValue(new ArrayList<>());
        return super.getValue();
    }
}
