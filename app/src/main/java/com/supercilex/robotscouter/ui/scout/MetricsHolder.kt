package com.supercilex.robotscouter.ui.scout

import android.app.Application
import android.os.Bundle
import com.firebase.ui.database.FirebaseArray
import com.firebase.ui.database.ObservableSnapshotArray
import com.supercilex.robotscouter.data.model.Metric
import com.supercilex.robotscouter.data.util.METRIC_PARSER
import com.supercilex.robotscouter.data.util.getScoutKey
import com.supercilex.robotscouter.data.util.getScoutMetricsRef
import com.supercilex.robotscouter.ui.ViewModelBase
import com.supercilex.robotscouter.util.ChangeEventListenerBase

class MetricsHolder(app: Application) : ViewModelBase<Bundle>(app) {
    lateinit var metrics: ObservableSnapshotArray<Metric<*>>

    private val listener = object : ChangeEventListenerBase() {}

    override fun onCreate(args: Bundle) {
        metrics = FirebaseArray(getScoutMetricsRef(getScoutKey(args)!!), METRIC_PARSER)
        metrics.addChangeEventListener(listener)
    }

    override fun onCleared() = metrics.removeChangeEventListener(listener)
}
