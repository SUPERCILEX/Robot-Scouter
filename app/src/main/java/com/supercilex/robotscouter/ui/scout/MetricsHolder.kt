package com.supercilex.robotscouter.ui.scout

import android.app.Application
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.ObservableSnapshotArray
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.util.METRIC_PARSER
import com.supercilex.robotscouter.data.util.getScoutMetricsRef
import com.supercilex.robotscouter.ui.ViewModelBase
import com.supercilex.robotscouter.util.ChangeEventListenerBase

class MetricsHolder(app: Application) : ViewModelBase<String>(app) {
    lateinit var metrics: ObservableSnapshotArray<Metric<*>>

    private val listener = object : ChangeEventListenerBase() {}

    override fun onCreate(args: String) {
        metrics = FirebaseArray(getScoutMetricsRef(args), METRIC_PARSER)
        metrics.addChangeEventListener(listener)
    }

    override fun onCleared() {
        super.onCleared()
        metrics.removeChangeEventListener(listener)
    }
}
