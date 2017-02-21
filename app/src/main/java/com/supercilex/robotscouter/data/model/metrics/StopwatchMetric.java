package com.supercilex.robotscouter.data.model.metrics;

import java.util.ArrayList;
import java.util.List;

public class StopwatchMetric extends ScoutMetric<List<Long>> {
    public StopwatchMetric(String name, List<Long> value) {
        super(name, value, MetricType.STOPWATCH);
    }

    @Override
    public List<Long> getValue() {
        if (super.getValue() == null) setValue(new ArrayList<Long>());
        return super.getValue();
    }
}
